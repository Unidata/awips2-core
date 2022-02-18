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
package com.raytheon.uf.edex.database.health;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.audit.DataStatus;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditEvent;
import com.raytheon.uf.common.datastorage.audit.DataStorageInfo;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditListener;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditer;
import com.raytheon.uf.common.datastorage.audit.MetadataAndDataId;
import com.raytheon.uf.common.datastorage.audit.MetadataStatus;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier.MetadataSpecificity;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.IContextStateProcessor;
import com.raytheon.uf.edex.core.IMessageProducer;

/**
 * Class for auditing data storage routes. There should be one instance of this
 * per edex cluster. Currently, the primary purpose of this is to detect when
 * the metadata database and the hdf5 datastore are out of sync, and to delete
 * whichever portion does exist to keep them in sync. This is especially needed
 * due to the write behind functionality of ignite.
 *
 * Note that this class should not be accessed directly other than by the spring
 * configuration that forwards data storage events here from JMS endpoints.
 * Everything else should use the proxy implementations of
 * {@link IDataStorageAuditer} to send notifications to those JMS endpoints.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Feb 17, 2022 8608       mapeters    Prevent harmless warnings about duplicate data,
 *                                     persist state across EDEX restarts
 *
 *
 * </pre>
 *
 * @author mapeters
 */
public class DataStorageAuditer
        implements IDataStorageAuditer, IContextStateProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Path to persist state to, so that data storage routes that occur around
     * EDEX restarts are still audited correctly. Note that this must be a
     * mounted/shared file, since only one instance of this auditer exists per
     * EDEX cluster, and it may switch JVMs on restart.
     */
    private static final String PERSISTED_STATE_PATH = EDEXUtil.getEdexShare()
            + File.separator + "dataStorageAuditerState.gz";

    private final AtomicBoolean persistedStateLoaded = new AtomicBoolean(false);

    private final Map<String, DataStorageInfo> traceIdToInfo = new HashMap<>();

    private final long completedRetentionMillis = Long
            .getLong("data.storage.auditer.completed.retention.mins")
            * TimeUtil.MILLIS_PER_MINUTE;

    private final long pendingRetentionMillis = Long
            .getLong("data.storage.auditer.pending.retention.mins")
            * TimeUtil.MILLIS_PER_MINUTE;

    private final List<IDataStorageAuditListener> listeners = new ArrayList<>();

    private final boolean enabled = "ignite"
            .equals(System.getenv("DATASTORE_PROVIDER"));

    public DataStorageAuditer(IMessageProducer messageProducer) {
        registerDataStatusListener(
                new DefaultDataStorageAuditListener(messageProducer));
        logger.info("Data storage auditer {}.",
                enabled ? "enabled" : "disabled");
    }

    @Override
    public void processEvent(DataStorageAuditEvent event) {
        if (!enabled) {
            return;
        }

        MetadataAndDataId[] dataIds = event.getDataIds();
        if (dataIds != null) {
            processDataIds(dataIds);
        }
        Map<String, MetadataStatus> metaStatuses = event.getMetadataStatuses();
        if (metaStatuses != null) {
            processMetadataStatuses(metaStatuses);
        }
        Map<String, DataStatus> dataStatus = event.getDataStatuses();
        if (dataStatus != null) {
            processDataStatuses(dataStatus);
        }
    }

    @Override
    public void processDataIds(MetadataAndDataId[] dataIdsArray) {
        logger.debug("Processing metadata and data IDs: {}",
                (Object) dataIdsArray);
        for (MetadataAndDataId dataIds : dataIdsArray) {
            synchronized (traceIdToInfo) {
                DataStorageInfo info = traceIdToInfo.computeIfAbsent(
                        dataIds.getMetaId().getTraceId(), DataStorageInfo::new);
                if (info.getMetaId() != null || info.getDataId() != null) {
                    logger.error(
                            "Metadata or data IDs already set on info:\nInfo: "
                                    + info + "\nNew metadata and data IDs: "
                                    + dataIds);
                } else {
                    IMetadataIdentifier metaId = dataIds.getMetaId();
                    info.setMetaId(metaId);
                    info.setDataId(dataIds.getDataId());
                    if (metaId
                            .getSpecificity() == MetadataSpecificity.NO_METADATA) {
                        if (info.getMetaStatus() != null) {
                            logger.error(
                                    "Metadata status reported for data storage operation that shouldn't have any metadata: "
                                            + info);
                        } else {
                            info.setMetaStatus(MetadataStatus.NA);
                        }
                    }
                    if (info.isComplete()) {
                        logger.debug("Data store completed: {}", info);
                        handleDataStorageResult(info);
                    }
                }
            }
        }
    }

    @Override
    public void processMetadataStatuses(Map<String, MetadataStatus> statuses) {
        logger.debug("Processing metadata statuses: {}", statuses);
        for (Entry<String, MetadataStatus> traceIdStatusEntry : statuses
                .entrySet()) {
            String traceId = traceIdStatusEntry.getKey();
            MetadataStatus status = traceIdStatusEntry.getValue();
            synchronized (traceIdToInfo) {
                DataStorageInfo info = traceIdToInfo.computeIfAbsent(traceId,
                        t -> new DataStorageInfo(t));
                MetadataStatus prevStatus = info.getMetaStatus();
                info.setMetaStatus(status);
                if (info.isComplete()) {
                    if (prevStatus == null) {
                        logger.debug("Data store completed: {}", info);
                    } else {
                        if (info.getDataStatus() == DataStatus.DUPLICATE_SYNC
                                && prevStatus == MetadataStatus.STORAGE_NOT_REACHED_FOR_DUPLICATE
                                && status == MetadataStatus.DUPLICATE) {
                            /*
                             * Metadata storage shouldn't be reached if data
                             * storage fails due to duplicates. But if it is
                             * reached and complains about duplicates,
                             * everything still matches up, so log as info
                             * instead of warning. This regularly happens for
                             * FFMP.
                             */
                            logger.info(
                                    "Metadata status updated from {} for store info: {}",
                                    prevStatus, info);
                        } else {
                            /*
                             * Warn since this shouldn't really happen, probably
                             * caused by a synchronous data store failure which
                             * should prevent reaching the metadata storing
                             * code, but some data storage routes may not handle
                             * that correctly.
                             */
                            logger.warn(
                                    "Metadata status updated from {} for store info: {}",
                                    prevStatus, info);
                        }
                    }
                    handleDataStorageResult(info);
                }
            }
        }
    }

    @Override
    public void processDataStatuses(Map<String, DataStatus> statuses) {
        logger.debug("Processing data statuses: {}", statuses);
        for (Entry<String, DataStatus> traceIdStatusEntry : statuses
                .entrySet()) {
            String traceId = traceIdStatusEntry.getKey();
            DataStatus status = traceIdStatusEntry.getValue();
            synchronized (traceIdToInfo) {
                DataStorageInfo info = traceIdToInfo.computeIfAbsent(traceId,
                        t -> new DataStorageInfo(t));
                DataStatus prevStatus = info.getDataStatus();
                info.setDataStatus(status);
                if (status == DataStatus.FAILURE_SYNC) {
                    if (info.getMetaStatus() == null) {
                        info.setMetaStatus(
                                MetadataStatus.STORAGE_NOT_REACHED_FOR_FAILURE);
                    } else {
                        logger.warn(
                                "Metadata status reported for data storage operation that should have stopped early due to data failure: "
                                        + info);
                    }
                } else if (status == DataStatus.DUPLICATE_SYNC) {
                    if (info.getMetaStatus() == null) {
                        info.setMetaStatus(
                                MetadataStatus.STORAGE_NOT_REACHED_FOR_DUPLICATE);
                    } else {
                        logger.warn(
                                "Metadata status reported for data storage operation that should have stopped early due to duplicate data: "
                                        + info);
                    }
                }
                if (info.isComplete()) {
                    if (prevStatus == null) {
                        logger.debug("Data store completed: {}", info);
                    } else {
                        /*
                         * This can commonly happen since when ignite stores a
                         * group, it re-stores everything that already was in
                         * the group with a "replace" store op
                         */
                        logger.debug(
                                "Data status updated from {} for store info: {}",
                                prevStatus, info);
                    }
                    handleDataStorageResult(info);
                }
            }
        }
    }

    /**
     * Cleanup the stored data storage information by removing entries that are
     * older than the cutoff times. Should be called on a cron.
     */
    public void cleanup() {
        long currentTime = SimulatedTime.getSystemTime().getMillis();
        long completedCutoff = currentTime - completedRetentionMillis;
        long pendingCutoff = currentTime - pendingRetentionMillis;
        synchronized (traceIdToInfo) {
            logger.info(
                    "Cleaning up expired data storage information from {} total operations...",
                    traceIdToInfo.size());
            Iterator<DataStorageInfo> iter = traceIdToInfo.values().iterator();
            while (iter.hasNext()) {
                DataStorageInfo info = iter.next();
                long startTime = info.getStartTime();
                if (info.isComplete()) {
                    if (startTime < completedCutoff) {
                        iter.remove();
                    }
                } else if (startTime < pendingCutoff) {
                    iter.remove();
                    if (info.hasDataStatusOnly()) {
                        /*
                         * This can happen somewhat commonly if a data storage
                         * operation completes, the completed data storage info
                         * expires from here, and then the data group is stored
                         * again, and the trace ID hung around in the ignite
                         * cache that whole time.
                         */
                        logger.info(
                                "Expired data storage info only has data status: "
                                        + info);
                    } else {
                        logger.warn(
                                "Expired data storage info never completed: "
                                        + info);
                    }
                }
            }
            logger.info("Retained info for {} data storage operations",
                    traceIdToInfo.size());
        }
    }

    public void registerDataStatusListener(IDataStorageAuditListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    private void handleDataStorageResult(DataStorageInfo info) {
        synchronized (listeners) {
            for (IDataStorageAuditListener listener : listeners) {
                listener.handleDataStorageResult(info,
                        Collections.unmodifiableMap(traceIdToInfo));
            }
        }
    }

    @Override
    public void preStart() {
        /*
         * This gets called twice for some reason, only load persisted state the
         * first time.
         */
        if (persistedStateLoaded.getAndSet(true)) {
            return;
        }

        // Load state from disk
        Path persistedStatePath = Paths.get(PERSISTED_STATE_PATH);
        if (Files.isRegularFile(persistedStatePath)) {
            try (FileInputStream fis = new FileInputStream(
                    PERSISTED_STATE_PATH);
                    GZIPInputStream gzis = new GZIPInputStream(fis)) {
                @SuppressWarnings("unchecked")
                Map<String, DataStorageInfo> persistedTraceIdToInfo = SerializationUtil
                        .transformFromThrift(Map.class, gzis);
                traceIdToInfo.putAll(persistedTraceIdToInfo);
                logger.info("Loaded info for {} data storage operations",
                        traceIdToInfo.size());
            } catch (Exception e) {
                logger.error("Error loading persisted state from "
                        + PERSISTED_STATE_PATH, e);
            }
            try {
                Files.delete(persistedStatePath);
            } catch (Exception e) {
                logger.error("Error deleting persisted state: "
                        + PERSISTED_STATE_PATH, e);
            }
        } else if (Files.exists(persistedStatePath)) {
            logger.error(
                    "Unable to load persisted state from non-regular file: "
                            + PERSISTED_STATE_PATH);
        } else {
            logger.info("No persisted state to load");
        }
    }

    @Override
    public void postStart() {
        // no-op
    }

    @Override
    public void preStop() {
        // no-op
    }

    @Override
    public void postStop() {
        // Persist state to disk
        cleanup();
        logger.info("Persisting info for {} data storage operations",
                traceIdToInfo.size());
        try (FileOutputStream fos = new FileOutputStream(PERSISTED_STATE_PATH);
                GZIPOutputStream gzos = new GZIPOutputStream(fos)) {
            SerializationUtil.transformToThriftUsingStream(traceIdToInfo, gzos);
        } catch (Exception e) {
            logger.error("Error persisting state to " + PERSISTED_STATE_PATH,
                    e);
        }
    }
}
