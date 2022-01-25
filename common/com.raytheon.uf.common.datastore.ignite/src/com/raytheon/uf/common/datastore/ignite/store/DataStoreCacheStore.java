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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import javax.cache.Cache.Entry;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.ignite.Ignite;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.lang.IgniteBiInClosure;
import org.apache.ignite.resources.CacheNameResource;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.audit.DataStatus;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditerContainer;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.Pair;

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
 * Sep 23, 2021  8608     mapeters  Add metadata id handling and auditing
 * Jan 25, 2022  8608     mapeters  Support write-through appends better
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

    @IgniteInstanceResource
    private Ignite ignite;

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
            return DataStoreValue.createWithoutMetadata(records);
        } catch (@SuppressWarnings("squid:S1166")
                FileNotFoundException | StorageException e) {
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
            result.put(entry.getKey(),
                    DataStoreValue.createWithoutMetadata((entry.getValue())));
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
        MetadataMap metadataMap = getMetadataMap(List.of(entry));
        try {
            long totalSizeInBytes = 0L;
            DataStoreValue value = entry.getValue();
            RecordAndMetadata[] lastAppendRms = value
                    .getLastAppendRecordsAndMetadata();
            StoreOp storeOp;
            RecordAndMetadata[] rms;
            if (!ArrayUtils.isEmpty(lastAppendRms)) {
                /*
                 * This method is only used by write-through so we can just
                 * append the records from the last append operation. The data
                 * store key is locked the whole time the StoreProcessor runs
                 * and this write is done for write-through, so this is
                 * thread-safe.
                 */
                rms = lastAppendRms;
                storeOp = StoreOp.APPEND;
            } else {
                rms = value.getRecordsAndMetadata();
                storeOp = StoreOp.REPLACE;
            }

            for (RecordAndMetadata rm : rms) {
                store.addDataRecord(rm.getRecord(), rm.getMetadata());
                totalSizeInBytes += rm.getRecord().getSizeInBytes();
            }

            logger.info("Writing " + cacheName + " entry: " + path + " (size="
                    + totalSizeInBytes + "B)");

            StorageStatus ss;
            synchronized (lock) {
                ss = store.store(storeOp);
            }
            if (ss.hasExceptions()) {
                throw ss.getExceptions()[0];
            }
            auditDataStorage(List.of(), List.of(metadataMap), true);
        } catch (Exception e) {
            logger.error(
                    "Error occurred writing " + cacheName + " entry: " + path,
                    e);
            auditDataStorage(List.of(metadataMap), List.of(), true);
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

        int numPaths = entriesByPath.size();
        logger.info("Writing " + numCacheEntries + " " + cacheName
                + " entries across " + numPaths + " paths");

        List<MetadataMap> successfulStores = new ArrayList<>();
        List<MetadataMap> failedStores = new ArrayList<>();
        int i = 1;
        for (Map.Entry<String, List<Entry<? extends DataStoreKey, ? extends DataStoreValue>>> mapEntry : entriesByPath
                .entrySet()) {
            String path = mapEntry.getKey();
            List<Entry<? extends DataStoreKey, ? extends DataStoreValue>> cacheEntries = mapEntry
                    .getValue();

            long totalSizeInBytes = 0L;
            MetadataMap metadataMap = getMetadataMap(cacheEntries);
            for (Entry<? extends DataStoreKey, ? extends DataStoreValue> cacheEntry : cacheEntries) {
                DataStoreValue value = cacheEntry.getValue();
                if (!ArrayUtils
                        .isEmpty(value.getLastAppendRecordsAndMetadata())) {
                    /*
                     * We don't currently support multiple appends going into a
                     * single write
                     */
                    logger.warn(
                            "Write behind append operation must be performed as a replace: "
                                    + cacheEntry.getKey());
                }
                for (RecordAndMetadata rm : value.getRecordsAndMetadata()) {
                    totalSizeInBytes += rm.getRecord().getSizeInBytes();
                }
            }

            try {
                IDataStore store = factory.getDataStore(new File(path),
                        useLocking);

                for (Entry<? extends DataStoreKey, ? extends DataStoreValue> cacheEntry : cacheEntries) {
                    for (RecordAndMetadata rm : cacheEntry.getValue()
                            .getRecordsAndMetadata()) {
                        store.addDataRecord(rm.getRecord(), rm.getMetadata());
                    }
                }

                logger.info("Writing " + i + "/" + numPaths + ": " + path
                        + " (size=" + totalSizeInBytes + "B)");
                Object lock = getWriteLock(path);
                StorageStatus ss;
                synchronized (lock) {
                    ss = store.store(StoreOp.REPLACE);
                }
                if (ss.hasExceptions()) {
                    throw ss.getExceptions()[0];
                }
                successfulStores.add(metadataMap);
            } catch (Exception e) {
                logger.error("Error occurred writing " + cacheName + " entries",
                        e);

                failedStores.add(metadataMap);
            }
            ++i;
        }

        /*
         * This writeAll method can be called in a synchronous way if write
         * behind falls behind enough, but it still doesn't propagate errors
         * back to edex, so it is still considered async for our purposes here
         * since it doesn't affect how the data storage route proceeds.
         */
        auditDataStorage(failedStores, successfulStores, false);

        entries.clear();

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

    private void auditDataStorage(List<MetadataMap> failedStores,
            List<MetadataMap> successfulStores, boolean synchronous) {
        // Audit illegal write behinds
        if (!synchronous) {
            List<MetadataMap> illegalWriteBehinds = new ArrayList<>();
            for (MetadataMap metadataMap : failedStores) {
                if (!metadataMap.isWriteBehindSupported()) {
                    illegalWriteBehinds.add(metadataMap);
                }
            }
            for (MetadataMap metadataMap : successfulStores) {
                if (!metadataMap.isWriteBehindSupported()) {
                    illegalWriteBehinds.add(metadataMap);
                }
            }
            if (!illegalWriteBehinds.isEmpty()) {
                logger.error("Illegal write behinds: " + illegalWriteBehinds);
            }
        }

        // Audit metadata specificity
        failedStores.forEach(MetadataMap::validateMetadataSpecificity);
        successfulStores.forEach(MetadataMap::validateMetadataSpecificity);

        // Audit data storage statuses
        Pair<Set<String>, Set<String>> failedAndOkayTraceIds = processFailedStores(
                failedStores);
        Set<String> failedTraceIds = failedAndOkayTraceIds.getFirst();
        Set<String> successfulTraceIds = new HashSet<>();
        successfulTraceIds.addAll(failedAndOkayTraceIds.getSecond());
        successfulStores.forEach(metadataMap -> successfulTraceIds
                .addAll(metadataMap.extractTraceIds()));

        if (!Collections.disjoint(failedTraceIds, successfulTraceIds)) {
            logger.error(
                    "Some trace IDs reported as both failures and successes:\nFailures: "
                            + failedTraceIds + "\nSuccesses: "
                            + successfulTraceIds);
        }

        Map<String, DataStatus> traceIdToStatuses = new HashMap<>();
        DataStatus failureStatus = synchronous ? DataStatus.FAILURE_SYNC
                : DataStatus.FAILURE_ASYNC;
        failedTraceIds.forEach(id -> traceIdToStatuses.put(id, failureStatus));
        successfulTraceIds
                .forEach(id -> traceIdToStatuses.put(id, DataStatus.SUCCESS));
        DataStorageAuditerContainer.getInstance().getAuditer()
                .processDataStatuses(traceIdToStatuses);
    }

    /**
     * Process the metadata maps of the failed stores. This attempts to load
     * data in order to double check that the data does not exist.
     *
     * @param metadataMaps
     *            maps of data store keys to metadata identifiers to data record
     *            names
     *
     * @return pair of the failed trace IDs and the okay trace IDs (the data
     *         existed even though the store said it failed)
     */
    private Pair<Set<String>, Set<String>> processFailedStores(
            List<MetadataMap> metadataMaps) {
        Set<String> failedTraceIds = new HashSet<>();
        Set<String> okayTraceIds = new HashSet<>();
        for (MetadataMap metadataMap : metadataMaps) {
            for (Map.Entry<DataStoreKey, MetadataRecordNamesMap> metadataMapEntry : metadataMap
                    .getEntries()) {
                DataStoreKey key = metadataMapEntry.getKey();
                Set<String> storedRecordNames = loadRecordNames(key);
                MetadataRecordNamesMap metadataRecordNamesMap = metadataMapEntry
                        .getValue();
                for (Map.Entry<IMetadataIdentifier, Set<String>> metadataRecordNamesEntry : metadataRecordNamesMap
                        .getEntries()) {
                    IMetadataIdentifier metaId = metadataRecordNamesEntry
                            .getKey();
                    String traceId = metaId.getTraceId();
                    switch (metaId.getSpecificity()) {
                    case GROUP:
                        /*
                         * Metadata identifies a group, just check if there is
                         * any data for the group
                         */
                        if (storedRecordNames.isEmpty()) {
                            failedTraceIds.add(traceId);
                        } else {
                            okayTraceIds.add(traceId);
                        }
                        break;
                    case DATASET:
                        /*
                         * Metadata identifies a dataset, check if metadata
                         * record names and dataset record names overlap
                         */
                        Set<String> metadataRecordNames = metadataRecordNamesEntry
                                .getValue();
                        if (Collections.disjoint(storedRecordNames,
                                metadataRecordNames)) {
                            failedTraceIds.add(traceId);
                        } else {
                            okayTraceIds.add(traceId);
                        }
                        break;
                    case NONE:
                        break;
                    default:
                        throw new UnsupportedOperationException(
                                "Unexpected metadata specificity value: "
                                        + metaId.getSpecificity());
                    }
                }
            }
        }
        return new Pair<>(failedTraceIds, okayTraceIds);
    }

    /**
     * Get map of data store keys to metadata identifiers to data record names.
     *
     * @param cacheEntries
     *            cache entries to get metadata map for
     * @return metadata map
     */
    private static MetadataMap getMetadataMap(
            List<Entry<? extends DataStoreKey, ? extends DataStoreValue>> cacheEntries) {
        MetadataMap metadataMap = new MetadataMap();
        for (Entry<? extends DataStoreKey, ? extends DataStoreValue> cacheEntry : cacheEntries) {
            DataStoreKey key = cacheEntry.getKey();
            DataStoreValue value = cacheEntry.getValue();
            RecordAndMetadata[] recordsAndMetadata = value
                    .getRecordsAndMetadata();
            if (recordsAndMetadata != null) {
                for (RecordAndMetadata rm : recordsAndMetadata) {
                    String recordName = rm.getRecord().getName();
                    Set<IMetadataIdentifier> metaIds = rm.getMetadata();
                    for (IMetadataIdentifier metaId : metaIds) {
                        metadataMap.getMetadataRecordNamesMap(key)
                                .getRecordNames(metaId).add(recordName);
                    }
                }
            }
        }
        return metadataMap;
    }

    private Set<String> loadRecordNames(DataStoreKey key) {
        String[] datasets;
        try {
            IDataStore store = factory.getDataStore(new File(key.getPath()),
                    useLocking);
            datasets = store.getDatasets(key.getGroup());
        } catch (Exception e) {
            logger.error("Error loading datasets for: " + key, e);
            datasets = null;
        }

        Set<String> recordNames;
        if (datasets != null && datasets.length > 0) {
            recordNames = Arrays.stream(datasets)
                    .collect(Collectors.toUnmodifiableSet());
        } else {
            recordNames = Set.of();
        }
        return recordNames;
    }

    /**
     * Maps data store keys to metadata identifiers to data record names
     */
    private static class MetadataMap {

        private final Map<DataStoreKey, MetadataRecordNamesMap> map = new HashMap<>();

        public Collection<Map.Entry<DataStoreKey, MetadataRecordNamesMap>> getEntries() {
            return map.entrySet();
        }

        /**
         * Get metadata record names maps for the given data store key,
         * creating/storing a new empty map if none exists.
         *
         * @param key
         *            data store key to get metadata and record names for
         * @return the metadata record names map (will not be null)
         */
        public MetadataRecordNamesMap getMetadataRecordNamesMap(
                DataStoreKey key) {
            return map.computeIfAbsent(key, k -> new MetadataRecordNamesMap());
        }

        public Collection<MetadataRecordNamesMap> getMetadataRecordNamesMaps() {
            return map.values();
        }

        /**
         * @return true if all contained metadata supports write behind, false
         *         otherwise
         */
        public boolean isWriteBehindSupported() {
            return getMetadataRecordNamesMaps().stream()
                    .allMatch(MetadataRecordNamesMap::isWriteBehindSupported);
        }

        /**
         * @return all trace IDs contained in this metadata map
         */
        public Set<String> extractTraceIds() {
            Set<String> traceIds = new HashSet<>();
            for (MetadataRecordNamesMap recordNamesMap : getMetadataRecordNamesMaps()) {
                traceIds.addAll(recordNamesMap.extractTraceIds());
            }
            return traceIds;
        }

        /**
         * Verify that the metadata specificity and the number of record names
         * match up for all contained metadata record names maps. This logs
         * warnings for any mismatches.
         */
        public void validateMetadataSpecificity() {
            for (Map.Entry<DataStoreKey, MetadataRecordNamesMap> entry : getEntries()) {
                DataStoreKey key = entry.getKey();
                MetadataRecordNamesMap recordNamesMap = entry.getValue();
                recordNamesMap.validateMetadataSpecificity(key);
            }
        }

        @Override
        public String toString() {
            return "MetadataMap [map=" + map + "]";
        }
    }

    /**
     * Maps metadata identifiers to data record names
     */
    private static class MetadataRecordNamesMap {

        private final Map<IMetadataIdentifier, Set<String>> map = new HashMap<>();

        public Collection<Map.Entry<IMetadataIdentifier, Set<String>>> getEntries() {
            return map.entrySet();
        }

        public Collection<IMetadataIdentifier> getMetadataIds() {
            return map.keySet();
        }

        /**
         * Get record names for the given metadata ID, creating/storing a new
         * empty set if none exists.
         *
         * @param metaId
         *            metadata identifier to get record names for
         * @return the record names (will not be null)
         */
        public Set<String> getRecordNames(IMetadataIdentifier metaId) {
            return map.computeIfAbsent(metaId, m -> new HashSet<>());
        }

        /**
         * @return true if all contained metadata supports write behind, false
         *         otherwise
         */
        public boolean isWriteBehindSupported() {
            return getMetadataIds().stream()
                    .allMatch(IMetadataIdentifier::isWriteBehindSupported);
        }

        /**
         * @return all trace IDs contained in this metadata map
         */
        public Set<String> extractTraceIds() {
            return getMetadataIds().stream()
                    .map(IMetadataIdentifier::getTraceId)
                    .collect(Collectors.toSet());
        }

        /**
         * Verify that the metadata specificity and the number of record names
         * match up. This logs warnings for any mismatches.
         *
         * @param key
         *            the data store key that this metadata record names map is
         *            for
         */
        public void validateMetadataSpecificity(DataStoreKey key) {
            /*
             * Convert the entries into a list of metadata IDs and record names.
             * This merges metadata identifiers that are the same except for
             * trace ID.
             */
            List<MetaIdsAndRecordNames> metaIdsAndRecordNamesList = new ArrayList<>();
            for (Map.Entry<IMetadataIdentifier, Set<String>> entry : getEntries()) {
                IMetadataIdentifier metaId = entry.getKey();
                Set<String> recordNames = entry.getValue();
                boolean merged = false;
                for (MetaIdsAndRecordNames metaIdsAndRecordNames : metaIdsAndRecordNamesList) {
                    if (metaIdsAndRecordNames.merge(metaId, recordNames)) {
                        merged = true;
                        break;
                    }
                }
                if (!merged) {
                    metaIdsAndRecordNamesList.add(
                            new MetaIdsAndRecordNames(metaId, recordNames));
                }
            }

            boolean firstGroupSpecificity = true;
            for (MetaIdsAndRecordNames metaIdsAndRecordNames : metaIdsAndRecordNamesList) {
                Set<IMetadataIdentifier> metaIds = metaIdsAndRecordNames
                        .getMetaIds();
                IMetadataIdentifier sampleMetaId = metaIds.iterator().next();
                switch (sampleMetaId.getSpecificity()) {
                case GROUP:
                    /*
                     * Everything in this map is for the same data store
                     * key/group, so there should only be one set of metadata
                     * identifiers with group specificity.
                     */
                    if (!firstGroupSpecificity) {
                        logger.warn(
                                "Multiple metadata for single group that claims to have one metadata per group: {}, {}",
                                key, map);
                    }
                    firstGroupSpecificity = false;
                    break;
                case DATASET:
                    if (metaIdsAndRecordNames.getRecordNames().size() > 1) {
                        logger.warn(
                                "Multiple datasets for single metadata that claims to identify a single dataset: {}, {}",
                                key, metaIdsAndRecordNames);
                    }

                    break;
                case NONE:
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Unexpected metadata specificity value: "
                                    + sampleMetaId.getSpecificity());
                }

            }
        }

        @Override
        public String toString() {
            return "MetadataRecordNamesMap [map=" + map + "]";
        }
    }

    /**
     * Contains metadata identifiers that are all the same except for their
     * trace IDs. Also contains all the data record names that those metadata
     * identifiers reference.
     */
    private static class MetaIdsAndRecordNames {

        private final Set<IMetadataIdentifier> metaIds = new HashSet<>();

        private final Set<String> recordNames = new HashSet<>();

        public MetaIdsAndRecordNames(IMetadataIdentifier metaId,
                Set<String> recordNames) {
            metaIds.add(metaId);
            this.recordNames.addAll(recordNames);
        }

        public boolean merge(IMetadataIdentifier metaId,
                Set<String> recordNames) {
            IMetadataIdentifier sampleMetaId = metaIds.iterator().next();
            if (sampleMetaId.equalsIgnoreTraceId(metaId)) {
                metaIds.add(metaId);
                this.recordNames.addAll(recordNames);
                return true;
            }
            return false;
        }

        public Set<IMetadataIdentifier> getMetaIds() {
            return Collections.unmodifiableSet(metaIds);
        }

        public Set<String> getRecordNames() {
            return Collections.unmodifiableSet(recordNames);
        }

        @Override
        public String toString() {
            return "MetaIdsAndRecordNames [metaIds=" + metaIds
                    + ", recordNames=" + recordNames + "]";
        }
    }
}
