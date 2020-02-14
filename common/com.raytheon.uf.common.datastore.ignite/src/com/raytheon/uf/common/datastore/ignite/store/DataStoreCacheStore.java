/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite.store;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import javax.cache.Cache.Entry;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.lang.IgniteBiInClosure;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;

/**
 * 
 * {@link CacheStore} that performs all writes and loads from an
 * {@link IDataStore}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 29, 2019  7628     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DataStoreCacheStore
        implements CacheStore<DataStoreKey, DataStoreValue> {

    private final IDataStoreFactory factory;

    private boolean useLocking = true;

    private Map<String, Object> writeLocks = new LinkedHashMap<String, Object>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > 256;
        }
    };

    public DataStoreCacheStore(IDataStoreFactory factory, boolean useLocking) {
        this.factory = factory;
        this.useLocking = useLocking;
    }

    public Object getWriteLock(String path) {
        synchronized (writeLocks) {
            Object lock = writeLocks.get(path);
            if (lock == null) {
                lock = new Object();
                writeLocks.put(path, lock);
            }
            return lock;

        }
    }

    @Override
    public DataStoreValue load(DataStoreKey key) throws CacheLoaderException {
        IDataStore store = factory.getDataStore(new File(key.getPath()),
                useLocking);

        try {
            IDataRecord[] records = store.retrieve(key.getGroup());
            return new DataStoreValue(records);
        } catch (FileNotFoundException | StorageException e) {
            /*
             * CacheLoader is supposed to return null if something can't be
             * loaded. It would be nice to be able to differentiate "not found"
             * from other errors but that isn't currently possible.
             */
            return null;
        }
    }

    @Override
    public Map<DataStoreKey, DataStoreValue> loadAll(
            Iterable<? extends DataStoreKey> keys) throws CacheLoaderException {
        StringJoiner j = new StringJoiner(",");
        Map<String, List<String>> groupsByFile = new HashMap<>();
        for (DataStoreKey key : keys) {
            j.add(key.getGroup());
            List<String> groups = groupsByFile.get(key.getPath());
            if (groups == null) {
                groups = new ArrayList<>();
                groupsByFile.put(key.getPath(), groups);
            }
            groups.add(key.getGroup());
        }
        Map<DataStoreKey, List<IDataRecord>> recordMap = new HashMap<>();
        try {
            for (java.util.Map.Entry<String, List<String>> entry : groupsByFile
                    .entrySet()) {
                String path = entry.getKey();
                IDataStore store = factory.getDataStore(new File(path),
                        useLocking);
                String[] groups = entry.getValue().toArray(new String[0]);
                IDataRecord[] records = store.retrieveGroups(groups,
                        Request.ALL);
                for (IDataRecord record : records) {
                    DataStoreKey key = new DataStoreKey(path,
                            record.getGroup());
                    List<IDataRecord> recs = recordMap.get(key);
                    if (recs == null) {
                        recs = new ArrayList<>();
                        recordMap.put(key, recs);
                    }
                    recs.add(record);
                }
            }
        } catch (FileNotFoundException | StorageException e) {
            throw new CacheLoaderException(e);
        }
        Map<DataStoreKey, DataStoreValue> result = new HashMap<>();
        for (java.util.Map.Entry<DataStoreKey, List<IDataRecord>> entry : recordMap
                .entrySet()) {
            result.put(entry.getKey(), new DataStoreValue(entry.getValue()));
        }
        return result;
    }

    @Override
    public void delete(Object keyObject) throws CacheWriterException {
        DataStoreKey key = (DataStoreKey) keyObject;
        IDataStore store = factory.getDataStore(new File(key.getPath()),
                useLocking);
        try {
            store.deleteGroups(key.getGroup());
        } catch (FileNotFoundException | StorageException e) {
            /*
             * When a whole file is purged ignite will pass the deletion through
             * to the underlying store so it is already gone.
             */
        }
    }

    @Override
    public void deleteAll(Collection<?> keys) throws CacheWriterException {
        Map<String, List<String>> groupsByFile = new HashMap<>();

        try {
            for (Object keyObject : keys) {
                DataStoreKey key = (DataStoreKey) keyObject;
                List<String> groups = groupsByFile.get(key.getPath());
                if (groups == null) {
                    groups = new ArrayList<>();
                    groupsByFile.put(key.getPath(), groups);
                }
                groups.add(key.getGroup());
            }

            for (java.util.Map.Entry<String, List<String>> entry : groupsByFile
                    .entrySet()) {
                IDataStore store = factory
                        .getDataStore(new File(entry.getKey()), useLocking);
                String[] groups = entry.getValue().toArray(new String[0]);
                store.deleteGroups(groups);
            }
        } catch (FileNotFoundException | StorageException e) {
            /*
             * When a whole file is purged ignite will pass the deletion through
             * to the underlying store so it is already gone.
             */
        }
    }

    @Override
    public void write(
            Entry<? extends DataStoreKey, ? extends DataStoreValue> entry)
            throws CacheWriterException {
        String path = entry.getKey().getPath();
        IDataStore store = factory.getDataStore(new File(path), useLocking);
        Object lock = getWriteLock(path);
        try {
            for (IDataRecord record : entry.getValue().getRecords()) {
                store.addDataRecord(record);
            }
            StorageStatus ss;
            synchronized (lock) {
                ss = store.store(StoreOp.REPLACE);
            }
            if (ss.hasExceptions()) {
                throw ss.getExceptions()[0];
            }
        } catch (StorageException e) {
            throw new CacheWriterException(e);
        }
    }

    @Override
    public void writeAll(
            Collection<Entry<? extends DataStoreKey, ? extends DataStoreValue>> entries)
            throws CacheWriterException {
        Map<String, IDataStore> storeMap = new HashMap<>();
        try {

            for (Entry<? extends DataStoreKey, ? extends DataStoreValue> entry : entries) {
                String path = entry.getKey().getPath();
                IDataStore store = storeMap.get(path);
                if (store == null) {
                    store = factory.getDataStore(new File(path), useLocking);
                    storeMap.put(path, store);
                }
                for (IDataRecord record : entry.getValue().getRecords()) {
                    store.addDataRecord(record);
                }
            }
            for (Map.Entry<String, IDataStore> entry : storeMap.entrySet()) {
                String path = entry.getKey();
                IDataStore store = entry.getValue();
                Object lock = getWriteLock(path);
                StorageStatus ss;
                synchronized (lock) {
                    ss = store.store(StoreOp.REPLACE);
                }
                if (ss.hasExceptions()) {
                    throw ss.getExceptions()[0];
                }

            }
        } catch (StorageException e) {
            throw new CacheWriterException(e);
        }
    }

    @Override
    public void loadCache(
            IgniteBiInClosure<DataStoreKey, DataStoreValue> closure,
            Object... pathAndGroups) throws CacheLoaderException {
        /* This is optional and unused. */
    }

    @Override
    public void sessionEnd(boolean arg0) throws CacheWriterException {
        /* This store is non-transactional. */
    }

}
