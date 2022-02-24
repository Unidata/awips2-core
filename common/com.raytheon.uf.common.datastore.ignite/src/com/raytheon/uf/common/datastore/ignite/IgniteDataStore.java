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
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.cache.processor.EntryProcessorException;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.DuplicateRecordStorageException;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.StorageProperties.Compression;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.audit.DataStatus;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditerContainer;
import com.raytheon.uf.common.datastorage.audit.Hdf5DataIdentifer;
import com.raytheon.uf.common.datastorage.audit.MetadataAndDataId;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.datastore.ignite.processor.FastStoreCallable;
import com.raytheon.uf.common.datastore.ignite.processor.GetDatasetNamesProcessor;
import com.raytheon.uf.common.datastore.ignite.processor.RetrieveProcessor;
import com.raytheon.uf.common.datastore.ignite.processor.StoreProcessor;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;

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
 * ------------- -------- --------- --------------------------------------------
 * May 29, 2019  7628     bsteffen  Initial creation
 * Mar 27, 2020  8099     bsteffen  Throw DuplicateRecordStorageException for
 *                                  duplicate records.
 * Mar 30, 2020  8073     bsteffen  Make deleteFiles query lazy to prevent OOM.
 * Apr 02, 2020  8075     bsteffen  Handle updates in fastStore.
 * Dec 08, 2020  8299     tgurney   Receive the through-datastore in constructor
 * Jun 10, 2021  8450     mapeters  Make cache ops retry on error/timeout and be
 *                                  effectively synchronous, close query
 *                                  cursors, add extra logging
 * Sep 23, 2021  8608     mapeters  Add auditing to prevent metadata/data from
 *                                  getting out of sync
 * Feb 17, 2022  8608     mapeters  Update FastReplaceCallable to be used for all
 *                                  fast stores, extract to FastStoreCallable file
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteDataStore implements IDataStore {

    private static final Logger logger = LoggerFactory
            .getLogger(IgniteDataStore.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(IgniteDataStore.class.getSimpleName() + ":");

    /* matches the date formats used in hdf5 filenames. */
    private static final Pattern ORPHAN_REGEX = Pattern.compile(
            "(19|20)(\\d\\d)-?(0[1-9]|1[012])-?(0[1-9]|[12][0-9]|3[01])");

    private boolean fastStore = true;

    private final String path;

    protected final IgniteClientManager igniteClientManager;

    protected final IgniteCacheAccessor<DataStoreKey, DataStoreValue> igniteCacheAccessor;

    protected Map<String, List<RecordAndMetadata>> recordsByGroup = new HashMap<>();

    private IDataStore throughDataStore;

    public IgniteDataStore(File file, IgniteClientManager igniteClientManager,
            IgniteCacheAccessor<DataStoreKey, DataStoreValue> igniteCacheAccessor,
            IDataStore throughDataStore) {
        path = file.getPath();
        this.igniteClientManager = igniteClientManager;
        this.igniteCacheAccessor = igniteCacheAccessor;
        this.throughDataStore = throughDataStore;
    }

    /**
     * get dataStore being used for read-through and write-through. Useful for
     * delete operations so we don't have to load data into cache before
     * deletion.
     */
    private IDataStore getThroughDataStore() {
        return throughDataStore;
    }

    @Override
    public void addDataRecord(IDataRecord dataset,
            IMetadataIdentifier metadataIdentifier,
            StorageProperties properties) {
        addDataRecord(dataset, Set.of(metadataIdentifier), properties);
    }

    @Override
    public void addDataRecord(IDataRecord dataset,
            IMetadataIdentifier metadataIdentifier) {
        addDataRecord(dataset, Set.of(metadataIdentifier));
    }

    @Override
    public void addDataRecord(IDataRecord dataset,
            Collection<IMetadataIdentifier> metadataIdentifiers,
            StorageProperties properties) {
        dataset.setProperties(properties);
        addDataRecord(dataset, metadataIdentifiers);
    }

    @Override
    public void addDataRecord(IDataRecord dataset,
            Collection<IMetadataIdentifier> metadataIdentifiers) {
        if (StoreProcessor.isPartial(dataset)) {
            fastStore = false;
        }
        String group = dataset.getGroup();
        List<RecordAndMetadata> records = recordsByGroup.get(group);
        if (records == null) {
            records = new ArrayList<>();
            recordsByGroup.put(group, records);
        }
        records.add(new RecordAndMetadata(dataset, metadataIdentifiers));
    }

    @Override
    public StorageStatus store() throws StorageException {
        return store(StoreOp.STORE_ONLY);
    }

    @Override
    public StorageStatus store(StoreOp storeOp) {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        Map<String, MetadataAndDataId> traceIdToDataInfo = new HashMap<>();

        long totalSizeInBytes = 0L;
        for (Entry<String, List<RecordAndMetadata>> groupRecordsEntry : recordsByGroup
                .entrySet()) {
            String group = groupRecordsEntry.getKey();
            for (RecordAndMetadata rm : groupRecordsEntry.getValue()) {
                IDataRecord record = rm.getRecord();
                Set<IMetadataIdentifier> metaIds = rm.getMetadata();
                for (IMetadataIdentifier metaId : metaIds) {
                    MetadataAndDataId metaAndDataId = traceIdToDataInfo
                            .computeIfAbsent(metaId.getTraceId(),
                                    traceId -> new MetadataAndDataId(metaId,
                                            new Hdf5DataIdentifer(traceId, path,
                                                    group)));
                    ((Hdf5DataIdentifer) metaAndDataId.getDataId())
                            .addDataset(record.getName());
                }
                totalSizeInBytes += record.getSizeInBytes();
            }
        }
        String logMsg = "Storing " + path + " groups: "
                + recordsByGroup.keySet() + " (fastStore=" + fastStore
                + ", storeOp=" + storeOp + ", size=" + totalSizeInBytes + "B)";
        logger.info(logMsg);

        if (!traceIdToDataInfo.isEmpty()) {
            DataStorageAuditerContainer.getInstance().getAuditer()
                    .processDataIds(traceIdToDataInfo.values()
                            .toArray(MetadataAndDataId[]::new));
        }

        StorageStatus storageStatus;
        boolean doingFastStore = fastStore && storeOp != StoreOp.APPEND;
        if (doingFastStore) {
            storageStatus = fastStore(storeOp);
        } else {
            /*
             * We are not doing a fast store, but the value of fastStore isn't
             * used in the rest of this store operation so reset it back to its
             * starting value of true for the next store operation
             */
            fastStore = true;
            List<StorageException> exceptions = new ArrayList<>();
            long[] indexOfAppend = null;

            StoreProcessor processor = new StoreProcessor(storeOp);
            Set<String> successfulGroups = new HashSet<>();
            Set<String> duplicateGroups = new HashSet<>();
            try {
                for (Entry<String, List<RecordAndMetadata>> entry : recordsByGroup
                        .entrySet()) {
                    String group = entry.getKey();
                    DataStoreKey key = new DataStoreKey(path, group);
                    Map<String, Object> corrObjs = unsetCorrelationObjects2(
                            entry.getValue());
                    Object[] records = entry.getValue().toArray();
                    StorageStatus status;
                    try {
                        status = igniteCacheAccessor.doAsyncCacheOp(
                                c -> c.invokeAsync(key, processor, records));
                        if (status.hasExceptions()) {
                            for (StorageException e : status.getExceptions()) {
                                resetCorrelationObjects(corrObjs, e);
                                exceptions.add(e);
                                if (e instanceof DuplicateRecordStorageException) {
                                    duplicateGroups.add(group);
                                }
                            }
                        } else {
                            successfulGroups.add(group);
                        }
                        long[] moreIndices = status.getIndexOfAppend();
                        if (moreIndices != null) {
                            if (indexOfAppend == null) {
                                indexOfAppend = moreIndices;
                            } else {
                                int oldLength = indexOfAppend.length;
                                indexOfAppend = Arrays.copyOf(indexOfAppend,
                                        oldLength + moreIndices.length);
                                System.arraycopy(moreIndices, 0, indexOfAppend,
                                        oldLength, moreIndices.length);
                            }
                        }
                    } catch (DuplicateRecordStorageException e) {
                        resetCorrelationObjects(corrObjs, e);
                        exceptions.add(e);
                        duplicateGroups.add(group);
                    } catch (StorageException e) {
                        resetCorrelationObjects(corrObjs, e);
                        exceptions.add(e);
                    }
                }
            } finally {
                auditDataStatuses(successfulGroups, duplicateGroups);
            }
            recordsByGroup.clear();

            storageStatus = new StorageStatus();
            storageStatus.setOperationPerformed(storeOp);
            storageStatus
                    .setExceptions(exceptions.toArray(new StorageException[0]));
            storageStatus.setIndexOfAppend(indexOfAppend);
        }

        timer.stop();
        long time = timer.getElapsedTime();
        perfLog.logDuration(logMsg, time);
        if (time > 10_000) {
            logger.warn("Storing " + path + " took " + time + " ms");
        }

        return storageStatus;
    }

    /**
     * See {@link #unsetCorrelationObjects}.
     */
    protected Map<String, Object> unsetCorrelationObjects2(
            List<RecordAndMetadata> records) {
        return unsetCorrelationObjects(
                records.stream().map(RecordAndMetadata::getRecord)
                        .collect(Collectors.toList()));
    }

    /**
     * Ignite will serialize and store correlation objects which means it must
     * have every object on the classpath which is bad, this clears them and
     * returns a mapping of dataset name to correlation object so they can be
     * reset in error processing if necessary.
     *
     * @return map of dataset name to correlation object
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
     * @see FastStoreCallable's javadoc
     *
     * @param storeOp
     * @return the storage status
     * @throws StorageException
     */
    protected StorageStatus fastStore(StoreOp storeOp) {
        StorageStatus result = new StorageStatus();
        List<StorageException> exceptions = new ArrayList<>();
        result.setOperationPerformed(storeOp);

        Set<String> successfulGroups = new HashSet<>();
        Set<String> duplicateGroups = new HashSet<>();
        try {
            for (Entry<String, List<RecordAndMetadata>> entry : recordsByGroup
                    .entrySet()) {
                String group = entry.getKey();
                Map<String, Object> corrObjs = unsetCorrelationObjects2(
                        entry.getValue());
                DataStoreKey key = new DataStoreKey(path, group);
                DataStoreValue value = new DataStoreValue(entry.getValue());
                try {
                    String cacheName = igniteCacheAccessor.getCacheName();
                    StorageStatus status = igniteClientManager.doIgniteOp(
                            ignite -> ignite.compute().affinityCall(cacheName,
                                    key, new FastStoreCallable(cacheName, key,
                                            value, storeOp)));
                    if (status.hasExceptions()) {
                        for (StorageException e : status.getExceptions()) {
                            resetCorrelationObjects(corrObjs, e);
                            exceptions.add(e);
                            if (e instanceof DuplicateRecordStorageException) {
                                duplicateGroups.add(group);
                            }
                        }
                    } else {
                        successfulGroups.add(group);
                    }
                } catch (DuplicateRecordStorageException e) {
                    resetCorrelationObjects(corrObjs, e);
                    exceptions.add(e);
                    duplicateGroups.add(group);
                } catch (StorageException e) {
                    resetCorrelationObjects(corrObjs, e);
                    exceptions.add(e);
                }
            }
        } finally {
            auditDataStatuses(successfulGroups, duplicateGroups);
        }

        result.setExceptions(exceptions.toArray(new StorageException[0]));
        recordsByGroup.clear();
        return result;
    }

    private void auditDataStatuses(Set<String> successfulGroups,
            Set<String> duplicateGroups) {
        Map<String, DataStatus> traceIdsToStatus = new HashMap<>();
        for (Entry<String, List<RecordAndMetadata>> groupRecordsEntry : recordsByGroup
                .entrySet()) {
            String group = groupRecordsEntry.getKey();
            if (successfulGroups.contains(group)) {
                continue;
            }

            DataStatus status = duplicateGroups.contains(group)
                    ? DataStatus.DUPLICATE_SYNC
                    : DataStatus.FAILURE_SYNC;
            for (RecordAndMetadata rm : groupRecordsEntry.getValue()) {
                for (IMetadataIdentifier metaId : rm.getMetadata()) {
                    traceIdsToStatus.put(metaId.getTraceId(), status);
                }
            }
        }
        DataStorageAuditerContainer.getInstance().getAuditer()
                .processDataStatuses(traceIdsToStatus);
    }

    @Override
    public void deleteDatasets(String... datasets)
            throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        String msg = "Deleting " + path + " datasets: " + datasets;
        logger.info(msg);

        DataStoreKey key = new DataStoreKey(path, "");
        IDataStore through = getThroughDataStore();
        igniteCacheAccessor.doAsyncCacheOp(c -> c.clearAsync(key));
        through.deleteDatasets(datasets);

        timer.stop();
        perfLog.logDuration(msg, timer.getElapsedTime());
    }

    @Override
    public void deleteGroups(String... groups)
            throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        String msg = "Deleting " + path + " groups: " + groups;
        logger.info(msg);

        Set<DataStoreKey> keys = new HashSet<>();
        for (String group : groups) {
            keys.add(new DataStoreKey(path, group));
        }
        DataStoreKey key = new DataStoreKey(path, "");
        IDataStore through = getThroughDataStore();
        igniteCacheAccessor.doAsyncCacheOp(c -> c.clearAsync(key));
        /*
         * The cache store is not writing removal of records so manually pass it
         * through.
         */
        through.deleteGroups(groups);

        timer.stop();
        perfLog.logDuration(msg, timer.getElapsedTime());
    }

    @Override
    public IDataRecord[] retrieve(String group)
            throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        DataStoreKey key = new DataStoreKey(path, group);
        try {
            List<IDataRecord> result = igniteCacheAccessor.doAsyncCacheOp(
                    c -> c.invokeAsync(key, new RetrieveProcessor()));

            timer.stop();
            perfLog.logDuration("Retrieving records for " + group,
                    timer.getElapsedTime());

            return result.toArray(new IDataRecord[0]);
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }
    }

    @Override
    public IDataRecord retrieve(String group, String dataset, Request request)
            throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

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
            final String finalDataset = dataset;
            List<IDataRecord> result = igniteCacheAccessor
                    .doAsyncCacheOp(c -> c.invokeAsync(key,
                            new RetrieveProcessor(finalDataset, request)));
            if (result == null || result.isEmpty()) {
                throw new StorageException("No data found for " + group + " "
                        + dataset + " in " + path, null);
            } else if (result.size() > 1) {
                throw new IllegalStateException("Too many records found for "
                        + group + " " + dataset + " in " + path);
            }

            timer.stop();
            perfLog.logDuration("Retrieving record for " + group + " and "
                    + dataset + " and " + request, timer.getElapsedTime());

            return result.get(0);
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }

    }

    @Override
    public IDataRecord[] retrieveDatasets(String[] datasetGroupPath,
            Request request) throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

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
        List<IDataRecord> records = new ArrayList<>();
        try {
            for (Entry<String, Set<String>> entry : dataSetsByGroup
                    .entrySet()) {
                DataStoreKey key = new DataStoreKey(this.path, entry.getKey());
                RetrieveProcessor processor = new RetrieveProcessor(
                        entry.getValue(), request);
                records.addAll(igniteCacheAccessor
                        .doAsyncCacheOp(c -> c.invokeAsync(key, processor)));
            }
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }

        timer.stop();
        perfLog.logDuration("Retrieving records for " + datasetGroupPath
                + " and " + request, timer.getElapsedTime());

        return records.toArray(new IDataRecord[0]);

    }

    @Override
    public IDataRecord[] retrieveGroups(String[] groups, Request request)
            throws StorageException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        RetrieveProcessor processor = new RetrieveProcessor(request);
        List<IDataRecord> records = new ArrayList<>();
        try {
            for (String group : groups) {
                DataStoreKey key = new DataStoreKey(path, group);
                records.addAll(igniteCacheAccessor
                        .doAsyncCacheOp(c -> c.invokeAsync(key, processor)));
            }
        } catch (EntryProcessorException e) {
            throw new StorageException(e.getLocalizedMessage(), null, e);
        }

        timer.stop();
        perfLog.logDuration(
                "Retrieving records for " + groups + " and " + request,
                timer.getElapsedTime());

        return records.toArray(new IDataRecord[0]);
    }

    @Override
    public String[] getDatasets(String group)
            throws StorageException, FileNotFoundException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        DataStoreKey key = new DataStoreKey(path, group);
        Set<String> names = igniteCacheAccessor.doAsyncCacheOp(
                c -> c.invokeAsync(key, new GetDatasetNamesProcessor()));

        timer.stop();
        perfLog.logDuration("Getting datasets for " + group,
                timer.getElapsedTime());

        return names.toArray(new String[0]);
    }

    @Override
    public void createLinks(Map<String, LinkLocation> links)
            throws StorageException, FileNotFoundException {
        throw new UnsupportedOperationException(
                "Ignite does not support this yet!");
    }

    @Override
    public void createDataset(IDataRecord rec) throws StorageException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        DataStoreKey key = new DataStoreKey(path, rec.getGroup());
        String msg = "Creating dataset for " + key;
        logger.info(msg);

        Map<String, Object> corrObjs = unsetCorrelationObjects(List.of(rec));

        final IDataRecord finalRec = rec;
        StorageStatus result = igniteCacheAccessor.doAsyncCacheOp(
                c -> c.invokeAsync(key, new StoreProcessor(), finalRec));
        if (result.hasExceptions()) {
            StorageException e = result.getExceptions()[0];
            resetCorrelationObjects(corrObjs, e);
            throw e;
        }

        timer.stop();
        perfLog.logDuration(msg, timer.getElapsedTime());
    }

    @Override
    public void repack(Compression compression) throws StorageException {
        IDataStore dataStore = getThroughDataStore();
        dataStore.repack(compression);
    }

    @Override
    public void copy(String outputDir, Compression compression,
            int minMillisSinceLastChange, int maxMillisSinceLastChange)
            throws StorageException {
        IDataStore dataStore = getThroughDataStore();
        dataStore.copy(outputDir, compression, minMillisSinceLastChange,
                maxMillisSinceLastChange);
    }

    @Override
    public void deleteOrphanData(Map<String, Date> dateMap)
            throws StorageException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        logger.info("Deleting " + path + " orphan data: " + dateMap);

        SqlFieldsQuery selectQuery = new SqlFieldsQuery(
                "select distinct path from DataStoreValue;");

        List<String> pathsToPurge = new ArrayList<>();
        try (QueryCursor<List<?>> cursor = igniteCacheAccessor
                .doSyncCacheOp(c -> c.query(selectQuery))) {
            for (List<?> row : cursor) {
                String path = (String) row.get(0);
                for (java.util.Map.Entry<String, Date> entry : dateMap
                        .entrySet()) {
                    Pattern p = Pattern.compile(entry.getKey());
                    if (p.matcher(path).find()) {
                        Matcher dateM = ORPHAN_REGEX.matcher(path);
                        if (dateM.find()) {
                            int year = Integer
                                    .parseInt(dateM.group(1) + dateM.group(2));
                            int month = Integer.parseInt(dateM.group(3));
                            int day = Integer.parseInt(dateM.group(4));
                            OffsetDateTime pathTime = OffsetDateTime.of(year,
                                    month, day, 0, 0, 0, 0, ZoneOffset.UTC);
                            OffsetDateTime purgeTime = OffsetDateTime.ofInstant(
                                    entry.getValue().toInstant(),
                                    ZoneOffset.UTC);
                            if (pathTime.isBefore(purgeTime)) {
                                pathsToPurge.add(path);
                            }
                        }
                        break;
                    }

                }
            }
        }
        if (!pathsToPurge.isEmpty()) {
            StringJoiner pathsSql = new StringJoiner("', '", "('", "')");
            for (String path : pathsToPurge) {
                pathsSql.add(path);
            }
            SqlFieldsQuery deleteQuery = new SqlFieldsQuery(
                    "delete from DataStoreValue where path in "
                            + pathsSql.toString());
            try (QueryCursor<List<?>> cursor = igniteCacheAccessor
                    .doSyncCacheOp(c -> c.query(deleteQuery))) {
                // do nothing
            }
        }

        IDataStore dataStore = getThroughDataStore();
        dataStore.deleteOrphanData(dateMap);

        timer.stop();
        perfLog.logDuration("Deleting " + path + " orphan data",
                timer.getElapsedTime());
    }

    @Override
    public void deleteFiles(String[] datesToDelete)
            throws StorageException, FileNotFoundException {
        if (datesToDelete != null) {
            throw new UnsupportedOperationException(
                    "Ignite does not support deleting dates yet.");
        }

        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        String msg = "Deleting " + path + " for dates: " + datesToDelete;
        logger.info(msg);

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
            Set<DataStoreKey> keysToDelete = new TreeSet<>();
            try (QueryCursor<List<?>> cursor = igniteCacheAccessor
                    .doSyncCacheOp(c -> c.query(query))) {
                for (List<?> row : cursor) {
                    String group = (String) row.get(0);
                    keysToDelete.add(new DataStoreKey(path, group));
                }
            }
            logger.info("Deleting " + keysToDelete.size() + " keys for path: "
                    + path);
            igniteCacheAccessor
                    .doAsyncCacheOp(c -> c.removeAllAsync(keysToDelete));
        } else {
            SqlFieldsQuery query = new SqlFieldsQuery(
                    "delete from DataStoreValue where path = ?");
            query.setArgs(path);
            try (QueryCursor<List<?>> cursor = igniteCacheAccessor
                    .doSyncCacheOp(c -> c.query(query))) {
                // do nothing
            }
        }
        IDataStore dataStore = getThroughDataStore();
        dataStore.deleteFiles(datesToDelete);

        timer.stop();
        perfLog.logDuration(msg, timer.getElapsedTime());
    }
}
