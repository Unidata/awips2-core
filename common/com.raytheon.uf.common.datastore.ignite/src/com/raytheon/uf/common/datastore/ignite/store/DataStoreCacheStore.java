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
import java.util.stream.Collectors;

import javax.cache.Cache.Entry;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.resources.CacheNameResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;

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
 * ------------- -------- --------- ---------------------
 * May 29, 2019  7628     bsteffen  Initial creation
 * Jun 10, 2021  8450     mapeters  Add various logging, improve exception handling
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DataStoreCacheStore
        implements CacheStore<DataStoreKey, DataStoreValue> {

    private static final Logger logger = LoggerFactory
            .getLogger(DataStoreCacheStore.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(DataStoreCacheStore.class.getSimpleName() + ":");

    private final IDataStoreFactory factory;

    @CacheNameResource
    private String cacheName;

    private boolean useLocking = true;

    private final Map<String, Object> writeLocks = new LinkedHashMap<String, Object>() {

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
        } catch (Exception e) {
            logger.error(
                    "Error occurred loading " + cacheName + " entry: " + key,
                    e);
            throw new CacheLoaderException(e);
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
        } catch (Exception e) {
            logger.error(
                    "Error occurred loading " + cacheName + " entries: " + keys,
                    e);
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
             * to the underlying store so it is likely already gone. Log as
             * warning in case something else is going on since this happens
             * rarely enough anyway.
             */
            logger.warn("Error occurred deleting " + cacheName + " entry: "
                    + keyObject, e);
        } catch (Exception e) {
            logger.error("Error occurred deleting " + cacheName + " entry: "
                    + keyObject, e);
            throw new CacheWriterException(e);
        }
    }

    @Override
    public void deleteAll(Collection<?> keys) throws CacheWriterException {
        Map<String, List<DataStoreKey>> keysByFile = keys.stream()
                .map(k -> (DataStoreKey) k)
                .collect(Collectors.groupingBy(DataStoreKey::getPath));

        for (Map.Entry<String, List<DataStoreKey>> entry : keysByFile
                .entrySet()) {
            List<DataStoreKey> fileKeys = entry.getValue();
            try {
                IDataStore store = factory
                        .getDataStore(new File(entry.getKey()), useLocking);
                String[] groups = fileKeys.stream().map(DataStoreKey::getGroup)
                        .toArray(String[]::new);
                store.deleteGroups(groups);
            } catch (FileNotFoundException | StorageException e) {
                /*
                 * When a whole file is purged ignite will pass the deletion
                 * through to the underlying store so it is likely already gone.
                 * Log as warning in case something else is going on since this
                 * happens rarely enough anyway.
                 */
                logger.warn("Error occurred deleting " + cacheName + " entry: "
                        + entry, e);
            } catch (Exception e) {
                logger.error("Error occurred deleting " + cacheName + " entry: "
                        + entry, e);
                throw new CacheWriterException(e);
            }

            // Remove from keys list arg to indicate success
            keys.removeAll(fileKeys);
        }
    }

    @Override
    public void write(
            Entry<? extends DataStoreKey, ? extends DataStoreValue> entry)
            throws CacheWriterException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        DataStoreKey key = entry.getKey();
        String path = key.getPath();

        IDataStore store = factory.getDataStore(new File(path), useLocking);
        Object lock = getWriteLock(path);
        try {
            long totalSizeInBytes = 0L;
            for (IDataRecord record : entry.getValue().getRecords()) {
                store.addDataRecord(record);
                totalSizeInBytes += record.getSizeInBytes();
            }

            logger.info("Writing " + cacheName + " entry: " + path + "(size="
                    + totalSizeInBytes + "B)");

            StorageStatus ss;
            synchronized (lock) {
                ss = store.store(StoreOp.REPLACE);
            }
            if (ss.hasExceptions()) {
                throw ss.getExceptions()[0];
            }
        } catch (Exception e) {
            logger.error(
                    "Error occurred writing " + cacheName + " entry: " + path,
                    e);
            throw new CacheWriterException(e);
        }

        timer.stop();
        perfLog.logDuration("Writing " + cacheName + " entry: " + path,
                timer.getElapsedTime());
    }

    @Override
    public void writeAll(
            Collection<Entry<? extends DataStoreKey, ? extends DataStoreValue>> entries)
            throws CacheWriterException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        int numCacheEntries = entries.size();
        Map<String, List<Entry<? extends DataStoreKey, ? extends DataStoreValue>>> entriesByPath = entries
                .stream().collect(Collectors
                        .groupingBy(entry -> entry.getKey().getPath()));

        try {
            int numPaths = entriesByPath.size();
            logger.info("Writing " + numCacheEntries + " " + cacheName
                    + " entries across " + numPaths + " paths");

            int i = 1;
            for (Map.Entry<String, List<Entry<? extends DataStoreKey, ? extends DataStoreValue>>> mapEntry : entriesByPath
                    .entrySet()) {
                String path = mapEntry.getKey();
                IDataStore store = factory.getDataStore(new File(path),
                        useLocking);

                long totalSizeInBytes = 0L;
                List<Entry<? extends DataStoreKey, ? extends DataStoreValue>> cacheEntries = mapEntry
                        .getValue();
                for (Entry<? extends DataStoreKey, ? extends DataStoreValue> cacheEntry : cacheEntries) {
                    for (IDataRecord record : cacheEntry.getValue()
                            .getRecords()) {
                        store.addDataRecord(record);
                        totalSizeInBytes += record.getSizeInBytes();
                    }
                }

                logger.info("Writing " + i + "/" + numPaths + ": " + path
                        + "(size=" + totalSizeInBytes + "B)");
                Object lock = getWriteLock(path);
                StorageStatus ss;
                synchronized (lock) {
                    ss = store.store(StoreOp.REPLACE);
                }
                if (ss.hasExceptions()) {
                    throw ss.getExceptions()[0];
                }

                // Remove from entries list arg to indicate success
                entries.removeAll(cacheEntries);
                ++i;
            }
        } catch (Exception e) {
            logger.error("Error occurred writing " + cacheName + " entries", e);
            throw new CacheWriterException(e);
        }

        timer.stop();
        perfLog.logDuration(
                "Writing " + numCacheEntries + " " + cacheName + " entries",
                timer.getElapsedTime());
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
