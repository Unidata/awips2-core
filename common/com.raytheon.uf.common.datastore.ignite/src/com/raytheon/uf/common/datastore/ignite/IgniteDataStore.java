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
package com.raytheon.uf.common.datastore.ignite;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.cache.integration.CacheLoader;
import javax.cache.processor.EntryProcessorException;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMetrics;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.StorageProperties.Compression;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastore.ignite.processor.DeleteDatasetProcessor;
import com.raytheon.uf.common.datastore.ignite.processor.GetDatasetNamesProcessor;
import com.raytheon.uf.common.datastore.ignite.processor.RetrieveProcessor;
import com.raytheon.uf.common.datastore.ignite.processor.StoreProcessor;
import com.raytheon.uf.common.datastore.ignite.store.DataStoreCacheStoreFactory;

/**
 * 
 * {@link IDataStore} implementation that stores data in an {@link IgniteCache}.
 * This supports using ignite persistent store functionality or simply acting as
 * a caching layer in front of another IDataStore.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 29, 2019  7628     bsteffen  Initial creation
 * Mar 27, 2020  8099     bsteffen  Throw DuplicateRecordStorageException for
 *                                  duplicate records.
 * Mar 30, 2020  8073     bsteffen  Make deleteFiles query lazy to prevent OOM.
 * Apr 02, 2020  8075     bsteffen  Handle updates in fastStore.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteDataStore implements IDataStore {

    private static final Logger logger = LoggerFactory
            .getLogger(IgniteDataStore.class);

    /* matches the date formats used in hdf5 filenames. */
    private static final Pattern ORPHAN_REGEX = Pattern.compile(
            "(19|20)(\\d\\d)-?(0[1-9]|1[012])-?(0[1-9]|[12][0-9]|3[01])");

    private boolean fastStore = true;

    private final String path;

    protected final IgniteCache<DataStoreKey, DataStoreValue> cache;

    protected Map<String, List<IDataRecord>> recordsByGroup = new HashMap<>();

    public IgniteDataStore(File file,
            IgniteCache<DataStoreKey, DataStoreValue> cache) {
        path = file.getPath();
        this.cache = cache;
    }

    /**
     * get dataStore being used for read-through and write-through. Useful for
     * delete operations so we don't have to load data into cache before
     * deletion.
     */
    protected IDataStore getThroughDataStore() {
        if (cache.metrics().isReadThrough()) {
            @SuppressWarnings("unchecked")
            CacheConfiguration<?, ?> config = cache
                    .getConfiguration(CacheConfiguration.class);
            Object factory = config.getCacheStoreFactory();
            if (factory instanceof DataStoreCacheStoreFactory) {
                return ((DataStoreCacheStoreFactory) factory)
                        .getDataStore(new File(path));
            }
        }
        return null;
    }

    @Override
    public void addDataRecord(IDataRecord dataset,
            StorageProperties properties) {
        dataset.setProperties(properties);
        addDataRecord(dataset);
    }

    @Override
    public void addDataRecord(IDataRecord dataset) {
        if (StoreProcessor.isPartial(dataset)) {
            fastStore = false;
        }
        String group = dataset.getGroup();
        List<IDataRecord> records = recordsByGroup.get(group);
        if (records == null) {
            records = new ArrayList<>();
            recordsByGroup.put(group, records);
        }
        records.add(dataset);
    }

    @Override
    public StorageStatus store() throws StorageException {
        long t0 = System.currentTimeMillis();
        StorageStatus ss = store(StoreOp.STORE_ONLY);
        long t1 = System.currentTimeMillis();
        long time = t0 - t1;
        if (time > 10_000) {
            logger.warn("Took " + time + "ms to store in " + path);
            logMetrics();
        }
        return ss;
    }

    protected void logMetrics() {
        CacheMetrics metrics = cache.metrics();
        StringBuilder message = new StringBuilder();
        message.append("Cache metrics for ").append(path).append("{\n");
        message.append("  CacheMissPercentage = ")
                .append(metrics.getCacheMissPercentage()).append('\n');
        message.append("  EntryProcessorMissPercentage = ")
                .append(metrics.getEntryProcessorMissPercentage()).append('\n');
        message.append("  EntryProcessorAverageInvocationTime = ")
                .append(metrics.getEntryProcessorAverageInvocationTime())
                .append('\n');
        message.append("  EntryProcessorMaxInvocationTime = ")
                .append(metrics.getEntryProcessorMaxInvocationTime())
                .append('\n');
        message.append("  WriteBehindTotalCriticalOverflowCount = ")
                .append(metrics.getWriteBehindTotalCriticalOverflowCount())
                .append('\n');
        message.append("}");
        logger.info(message.toString());
    }

    @Override
    public StorageStatus store(StoreOp storeOp) throws StorageException {
        if (fastStore && storeOp != StoreOp.APPEND) {
            return fastStore(storeOp);
        }
        fastStore = true;
        List<IgniteFuture<StorageStatus>> futures = new ArrayList<>();
        List<Map<String, Object>> correlationObjects = new ArrayList<>();

        StoreProcessor processor = new StoreProcessor(storeOp);
        for (Entry<String, List<IDataRecord>> entry : recordsByGroup
                .entrySet()) {
            DataStoreKey key = new DataStoreKey(path, entry.getKey());
            Map<String, Object> corrObjs = unsetCorrelationObjects(
                    entry.getValue());
            Object[] records = entry.getValue().toArray();
            futures.add(cache.invokeAsync(key, processor, records));
            correlationObjects.add(corrObjs);
        }
        recordsByGroup.clear();
        long[] indexOfAppend = null;
        StorageException[] exceptions = new StorageException[0];
        for (int i = 0; i < futures.size(); i += 1) {
            IgniteFuture<StorageStatus> future = futures.get(i);
            StorageStatus status = future.get();
            long[] moreIndices = status.getIndexOfAppend();
            if (moreIndices != null) {
                if (indexOfAppend == null) {
                    indexOfAppend = moreIndices;
                } else {
                    int oldLength = indexOfAppend.length;
                    indexOfAppend = Arrays.copyOf(indexOfAppend,
                            oldLength + moreIndices.length);
                    System.arraycopy(moreIndices, 0, indexOfAppend, oldLength,
                            moreIndices.length);
                }
            }
            StorageException[] moreEx = status.getExceptions();
            if (moreEx != null && moreEx.length > 0) {
                resetCorrelationObjects(correlationObjects.get(i), moreEx);
                int oldLength = exceptions.length;
                exceptions = Arrays.copyOf(exceptions,
                        oldLength + moreEx.length);
                System.arraycopy(moreIndices, 0, exceptions, oldLength,
                        moreEx.length);
            }
        }

        StorageStatus status = new StorageStatus();
        status.setOperationPerformed(storeOp);
        status.setExceptions(exceptions);
        status.setIndexOfAppend(indexOfAppend);

        return status;
    }

    /**
     * Ignite will serialize and store correlation objects which means it must
     * have every object on the classpath which is bad, this clears them and
     * returns a mapping of datasetname to correlation object so they can be
     * reset in error processing if necessary.
     */
    protected Map<String, Object> unsetCorrelationObjects(
            List<IDataRecord> records) {
        Map<String, Object> correlationObjects = new HashMap<>();
        for (IDataRecord record : records) {

            Object obj = record.getCorrelationObject();
            if (obj != null) {
                record.setCorrelationObject(null);
                correlationObjects.put(record.getName(), obj);
            }
        }
        return correlationObjects;
    }

    /**
     * Restore correlation objects for any records referenced in exceptions.
     * 
     * @param exceptions
     * @param correlationObjects
     */
    protected void resetCorrelationObjects(
            Map<String, Object> correlationObjects,
            StorageException... exceptions) {
        for (StorageException ex : exceptions) {
            IDataRecord record = ex.getRecord();
            if (record != null) {
                Object obj = correlationObjects.get(record.getName());
                record.setCorrelationObject(obj);
            }
        }
    }

    /**
     * Alternative implementation of store that is optimized for the common case
     * where all of the data in a group is stored in a single operation.
     * 
     * This implementation is not as good for more complex operations such as
     * append, detecting duplicates, and partial inserts(including inserting
     * different datasets in a group with multiple store operations). For these
     * operations the previous cache value is needed. This method will pull the
     * previous value to the local node and then push the result to the node
     * storing the data. Bringing the data local is more expensive than using a
     * {@link StoreProcessor} to move the new data to the node responsible for
     * storing the data so this method is not recommended if these operations
     * are performed often.
     * 
     * For the complex operations described above the exact behavior of this
     * method is dependent on the value of
     * {@link CacheConfiguration#isLoadPreviousValue()}. When this setting is
     * false the previous value will only be used if it is already loaded in the
     * cache, this is much faster but may overwrite the previous value if it is
     * not already in the cache. When loadPreviousValue is true the previous
     * value is loaded with the {@link CacheLoader} which is slower but
     * guarantees the existing data will not be overwritten unless this is a
     * REPLACE operation.
     * 
     * @param storeOp
     * @return
     * @throws StorageException
     */
    protected StorageStatus fastStore(StoreOp storeOp) throws StorageException {
        Map<String, IgniteFuture<DataStoreValue>> storeResults = new HashMap<>();
        Map<String, Map<String, Object>> storeCorrObjs = new HashMap<>();
        List<IgniteFuture<Void>> replaceResults = new ArrayList<>();
        for (Entry<String, List<IDataRecord>> entry : recordsByGroup
                .entrySet()) {
            Map<String, Object> corrObjs = unsetCorrelationObjects(
                    entry.getValue());
            DataStoreKey key = new DataStoreKey(path, entry.getKey());
            DataStoreValue value = new DataStoreValue(entry.getValue());
            if (storeOp == StoreOp.REPLACE) {
                replaceResults.add(cache.putAsync(key, value));
            } else {
                storeResults.put(entry.getKey(),
                        cache.getAndPutIfAbsentAsync(key, value));
                storeCorrObjs.put(entry.getKey(), corrObjs);
            }
        }

        StorageStatus result = new StorageStatus();
        List<StorageException> exceptions = new ArrayList<>();
        result.setOperationPerformed(storeOp);
        for (Entry<String, IgniteFuture<DataStoreValue>> entry : storeResults
                .entrySet()) {
            DataStoreValue previous = entry.getValue().get();
            if (previous != null) {
                List<IDataRecord> updated = recordsByGroup.get(entry.getKey());
                try {
                    DataStoreValue value = StoreProcessor.merge(previous,
                            updated, storeOp, result);
                    DataStoreKey key = new DataStoreKey(path, entry.getKey());
                    replaceResults.add(cache.putAsync(key, value));
                } catch (StorageException e) {
                    resetCorrelationObjects(storeCorrObjs.get(entry.getKey()),
                            e);
                    exceptions.add(e);
                }

            }
        }
        for (IgniteFuture<Void> future : replaceResults) {
            future.get();
        }

        result.setExceptions(exceptions.toArray(new StorageException[0]));
        recordsByGroup.clear();
        return result;
    }

    @Override
    public void deleteDatasets(String... datasets)
            throws StorageException, FileNotFoundException {
        DataStoreKey key = new DataStoreKey(path, "");
        IDataStore through = getThroughDataStore();
        if (through != null) {
            IgniteFuture<Void> future = cache.clearAsync(key);
            through.deleteDatasets(datasets);
            future.get();
        } else {
            cache.invoke(key, new DeleteDatasetProcessor(),
                    (Object[]) datasets);
        }
    }

    @Override
    public void deleteGroups(String... groups)
            throws StorageException, FileNotFoundException {
        Set<DataStoreKey> keys = new HashSet<>();
        for (String group : groups) {
            keys.add(new DataStoreKey(path, group));
        }
        DataStoreKey key = new DataStoreKey(path, "");
        IDataStore through = getThroughDataStore();
        if (through != null) {
            IgniteFuture<Void> future = cache.clearAsync(key);
            /*
             * The cache store is not writing removal of records so manually
             * pass it through.
             */
            through.deleteGroups(groups);
            future.get();
        } else {
            cache.removeAll(keys);
        }
    }

    @Override
    public IDataRecord[] retrieve(String group)
            throws StorageException, FileNotFoundException {
        DataStoreKey key = new DataStoreKey(path, group);
        try {
            List<IDataRecord> result = cache.invoke(key,
                    new RetrieveProcessor());
            return result.toArray(new IDataRecord[0]);
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }
    }

    @Override
    public IDataRecord retrieve(String group, String dataset, Request request)
            throws StorageException, FileNotFoundException {
        if (DataStoreFactory.DEF_SEPARATOR.equals(group)) {
            group = "";
        }
        /* Just load satellite for all this sanitation testing. */
        if (group.isEmpty()) {
            int index = dataset.lastIndexOf(DataStoreFactory.DEF_SEPARATOR);
            if (index >= 0) {
                group = dataset.substring(0, index);
                dataset = dataset.substring(index + 1);
            }
        }
        group = group.replaceAll(
                DataStoreFactory.DEF_SEPARATOR + DataStoreFactory.DEF_SEPARATOR,
                DataStoreFactory.DEF_SEPARATOR);
        if (group.endsWith(DataStoreFactory.DEF_SEPARATOR)) {
            group = group.substring(0,
                    group.length() - DataStoreFactory.DEF_SEPARATOR.length());
        }

        DataStoreKey key = new DataStoreKey(path, group);
        try {
            List<IDataRecord> result = cache.invoke(key,
                    new RetrieveProcessor(dataset, request));
            if (result == null || result.isEmpty()) {
                throw new StorageException("No data found for " + group + " "
                        + dataset + " in " + path, null);
            } else if (result.size() > 1) {
                throw new IllegalStateException("Too many records found for "
                        + group + " " + dataset + " in " + path);
            }
            return result.get(0);
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }

    }

    @Override
    public IDataRecord[] retrieveDatasets(String[] datasetGroupPath,
            Request request) throws StorageException, FileNotFoundException {
        Map<String, Set<String>> dataSetsByGroup = new LinkedHashMap<>();
        for (String path : datasetGroupPath) {
            String group = "";
            String dataset = path;
            int index = path.lastIndexOf(DataStoreFactory.DEF_SEPARATOR);
            if (index >= 0) {
                group = path.substring(0, index);
                dataset = path.substring(index + 1);
            }
            Set<String> dataSets = dataSetsByGroup.get(group);
            if (dataSets == null) {
                dataSets = new HashSet<>();
                dataSetsByGroup.put(group, dataSets);
            }
            dataSets.add(dataset);
        }
        List<IgniteFuture<List<IDataRecord>>> futures = new ArrayList<>();
        for (Entry<String, Set<String>> entry : dataSetsByGroup.entrySet()) {
            DataStoreKey key = new DataStoreKey(this.path, entry.getKey());
            RetrieveProcessor processor = new RetrieveProcessor(
                    entry.getValue(), request);
            futures.add(cache.invokeAsync(key, processor));
        }
        List<IDataRecord> records = new ArrayList<>();
        try {
            for (IgniteFuture<List<IDataRecord>> future : futures) {
                records.addAll(future.get());
            }
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }
        return records.toArray(new IDataRecord[0]);

    }

    @Override
    public IDataRecord[] retrieveGroups(String[] groups, Request request)
            throws StorageException {
        RetrieveProcessor processor = new RetrieveProcessor(request);
        List<IgniteFuture<List<IDataRecord>>> futures = new ArrayList<>();
        for (String group : groups) {
            DataStoreKey key = new DataStoreKey(path, group);
            futures.add(cache.invokeAsync(key, processor));
        }
        List<IDataRecord> records = new ArrayList<>();
        try {
            for (IgniteFuture<List<IDataRecord>> future : futures) {
                records.addAll(future.get());
            }
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }
        return records.toArray(new IDataRecord[0]);
    }

    @Override
    public String[] getDatasets(String group)
            throws StorageException, FileNotFoundException {
        DataStoreKey key = new DataStoreKey(path, group);
        Set<String> names = cache.invoke(key, new GetDatasetNamesProcessor());
        return names.toArray(new String[0]);
    }

    @Override
    public void createLinks(Map<String, LinkLocation> links)
            throws StorageException, FileNotFoundException {
        throw new UnsupportedOperationException(
                "Ignite does not support this yet!");
    }

    @Override
    public void createDataset(IDataRecord rec)
            throws StorageException, FileNotFoundException {
        if (rec.getCorrelationObject() != null) {
            rec = rec.clone();
            rec.setCorrelationObject(null);
        }
        DataStoreKey key = new DataStoreKey(path, rec.getGroup());
        cache.invoke(key, new StoreProcessor(), rec);
    }

    @Override
    public void repack(Compression compression) throws StorageException {
        IDataStore dataStore = getThroughDataStore();
        if (dataStore == null) {
            throw new UnsupportedOperationException(
                    "Ignite does not support repack yet.");
        }
        dataStore.repack(compression);
    }

    @Override
    public void copy(String outputDir, Compression compression,
            int minMillisSinceLastChange, int maxMillisSinceLastChange)
            throws StorageException {
        IDataStore dataStore = getThroughDataStore();
        if (dataStore == null) {
            throw new UnsupportedOperationException(
                    "Ignite does not support archiving yet.");
        }
        dataStore.copy(outputDir, compression, minMillisSinceLastChange,
                maxMillisSinceLastChange);
    }

    @Override
    public void deleteOrphanData(Map<String, Date> dateMap)
            throws StorageException {
        QueryCursor<List<?>> cursor = null;
        SqlFieldsQuery query = new SqlFieldsQuery(
                "select distinct path from DataStoreValue;");
        cursor = cache.query(query);

        List<String> pathsToPurge = new ArrayList<>();
        for (List<?> row : cursor) {
            String path = (String) row.get(0);
            for (java.util.Map.Entry<String, Date> entry : dateMap.entrySet()) {
                Pattern p = Pattern.compile(entry.getKey());
                if (p.matcher(path).find()) {
                    Matcher dateM = ORPHAN_REGEX.matcher(path);
                    if (dateM.find()) {
                        int year = Integer
                                .parseInt(dateM.group(1) + dateM.group(2));
                        int month = Integer.parseInt(dateM.group(3));
                        int day = Integer.parseInt(dateM.group(4));
                        OffsetDateTime pathTime = OffsetDateTime.of(year, month,
                                day, 0, 0, 0, 0, ZoneOffset.UTC);
                        OffsetDateTime purgeTime = OffsetDateTime.ofInstant(
                                entry.getValue().toInstant(), ZoneOffset.UTC);
                        if (pathTime.isBefore(purgeTime)) {
                            pathsToPurge.add(path);
                        }
                    }
                    break;
                }

            }
        }
        if (!pathsToPurge.isEmpty()) {
            StringJoiner pathsSql = new StringJoiner("', '", "('", "')");
            for (String path : pathsToPurge) {
                pathsSql.add(path);
            }
            query = new SqlFieldsQuery(
                    "delete from DataStoreValue where path in "
                            + pathsSql.toString());
            cache.query(query);
        }

        IDataStore dataStore = getThroughDataStore();
        if (dataStore != null) {
            dataStore.deleteOrphanData(dateMap);
        }
    }

    @Override
    public void deleteFiles(String[] datesToDelete)
            throws StorageException, FileNotFoundException {
        if (datesToDelete != null) {
            throw new UnsupportedOperationException(
                    "Ignite does not support deleting dates yet.");
        }
        /*
         * Delete SQL seems simpler but runs out of memory for large files.
         * https://issues.apache.org/jira/browse/IGNITE-9182
         */
        boolean workAround9182 = true;
        if (workAround9182) {
            SqlFieldsQuery query = new SqlFieldsQuery(
                    "select distinct recgroup from DataStoreValue where path = ?");
            query.setArgs(path);
            query.setLazy(true);
            QueryCursor<List<?>> cursor = cache.query(query);
            Set<DataStoreKey> keysToDelete = new HashSet<>();
            for (List<?> row : cursor) {
                String group = (String) row.get(0);
                keysToDelete.add(new DataStoreKey(path, group));
            }
            cache.removeAll(keysToDelete);
        } else {
            SqlFieldsQuery query = new SqlFieldsQuery(
                    "delete from DataStoreValue where path = ?");
            query.setArgs(path);
            cache.query(query);
        }
        IDataStore dataStore = getThroughDataStore();
        if (dataStore != null) {
            dataStore.deleteFiles(datesToDelete);
        }
    }

}
