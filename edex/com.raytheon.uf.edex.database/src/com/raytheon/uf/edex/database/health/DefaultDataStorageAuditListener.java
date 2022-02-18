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
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.annotations.DataURIUtil;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.audit.DataStatus;
import com.raytheon.uf.common.datastorage.audit.DataStorageInfo;
import com.raytheon.uf.common.datastorage.audit.Hdf5DataIdentifer;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditListener;
import com.raytheon.uf.common.datastorage.audit.MetadataStatus;
import com.raytheon.uf.common.datastorage.records.DataUriMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier.MetadataSpecificity;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.core.IMessageProducer;
import com.raytheon.uf.edex.database.plugin.PluginDao;
import com.raytheon.uf.edex.database.plugin.PluginFactory;

/**
 * Default listener for data storage audit events. If one of the metadata and
 * data fails to store, this deletes the other one to keep things in sync.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 27, 2021 8608       mapeters    Initial creation
 * Feb 17, 2022 8608       mapeters    Ignore non-hdf5 data
 *
 * </pre>
 *
 * @author mapeters
 */
public class DefaultDataStorageAuditListener
        implements IDataStorageAuditListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final String PURGE_TOPIC = "jms-generic:topic:pluginPurged?timeToLive=60000";

    private final IMessageProducer messageProducer;

    public DefaultDataStorageAuditListener(IMessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Override
    public void handleDataStorageResult(DataStorageInfo info,
            Map<String, DataStorageInfo> traceIdToInfo) {
        if (!(info.getMetaId() instanceof DataUriMetadataIdentifier)
                || !(info.getDataId() instanceof Hdf5DataIdentifer)) {
            logger.debug(
                    "Ignoring data storage info with non-dataUri metadata identifier and/or non-hdf5 data identifier: {}",
                    info);
            return;
        }

        DataUriMetadataIdentifier metaId = (DataUriMetadataIdentifier) info
                .getMetaId();
        Hdf5DataIdentifer dataId = (Hdf5DataIdentifer) info.getDataId();

        MetadataStatus metaStatus = info.getMetaStatus();
        DataStatus dataStatus = info.getDataStatus();
        if (metaStatus == MetadataStatus.NA || dataStatus == DataStatus.NA) {
            // nothing to keep in sync if database-only or datastore-only.
            return;
        }
        if (metaStatus.exists() == dataStatus.exists()) {
            // in sync, nothing to do
            return;
        }

        if (dataStatus.exists()) {
            // data exists, metadata doesn't - delete data
            /*
             * Double check that metadata doesn't exist since this should be
             * very rare anyway. Could possibly have failed because of
             * duplicates if a storage route doesn't specially handle that.
             */
            PluginDataObject persistedMetadata = getPersistedMetadata(metaId);
            if (persistedMetadata != null) {
                /*
                 * Check if there are any pending metadata writes for the same
                 * metadata. If so, kick can down the road to when that data
                 * storage route completes.
                 */
                DataStorageInfo otherPending = null;
                for (DataStorageInfo otherInfo : traceIdToInfo.values()) {
                    if (metaId.equalsIgnoreTraceId(otherInfo.getMetaId())) {
                        if (otherInfo.getMetaStatus() == null) {
                            otherPending = otherInfo;
                            break;
                        }
                    }
                }
                if (otherPending == null) {
                    logger.warn(
                            "Data exists and metadata doesn't for data store, deleting metadata: "
                                    + info);
                    try {
                        deleteData(dataId, metaId.getSpecificity());
                        info.setDataStatus(DataStatus.DELETED);
                    } catch (FileNotFoundException | StorageException e) {
                        logger.error("Error deleting data for: " + info, e);
                    }
                } else {
                    logger.info(
                            "Data exists and metadata doesn't for data store, deferring handling to later pending store:\nFailed store: "
                                    + info + "\nPending store: "
                                    + otherPending);
                }
            }
        } else {
            // metadata exists, data doesn't - delete metadata
            /*
             * Check if there are any pending data writes for the same metadata.
             * If so, kick can down the road to when that data storage route
             * completes.
             */
            DataStorageInfo otherPending = null;
            for (DataStorageInfo otherInfo : traceIdToInfo.values()) {
                if (metaId.equalsIgnoreTraceId(otherInfo.getMetaId())) {
                    if (otherInfo.getDataStatus() == null) {
                        otherPending = otherInfo;
                        break;
                    }
                }
            }

            if (otherPending == null) {
                logger.warn(
                        "Metadata exists and data doesn't for data store, deleting metadata: "
                                + info);
                try {
                    deleteMetadata(metaId);
                    info.setMetaStatus(MetadataStatus.DELETED);
                } catch (Exception e) {
                    logger.error("Error deleting metadata for: " + info, e);
                }
            } else {
                logger.info(
                        "Metadata exists and data doesn't for data store, deferring handling to later pending store:\nFailed store: "
                                + info + "\nPending store: " + otherPending);
            }
        }
    }

    private void deleteData(Hdf5DataIdentifer dataId,
            MetadataSpecificity specificity)
            throws FileNotFoundException, StorageException {
        IDataStore store = DataStoreFactory
                .getDataStore(new File(dataId.getFile()));
        switch (specificity) {
        case GROUP:
            store.deleteGroups(dataId.getGroup());
            break;
        case DATASET:
            store.deleteDatasets(dataId.getFullyQualifiedDatasets());
            break;
        case NO_DATA:
        case NO_METADATA:
            break;
        default:
            throw new UnsupportedOperationException(
                    "Unexpected metadata specificity: " + specificity);
        }
    }

    private PluginDataObject getPersistedMetadata(
            DataUriMetadataIdentifier metaId) {
        try {
            String dataUri = metaId.getDataUri();
            String plugin = DataURIUtil.getPluginName(dataUri);
            PluginDao dao = PluginFactory.getInstance().getPluginDao(plugin);
            return dao.getMetadata(dataUri);
        } catch (PluginException e) {
            logger.error("Error getting persisted metadata: " + metaId, e);
            return null;
        }
    }

    private void deleteMetadata(DataUriMetadataIdentifier metaId)
            throws PluginException {
        PluginDataObject persistedMetadata = getPersistedMetadata(metaId);
        if (persistedMetadata == null) {
            /*
             * Should just be caused by a race condition where the data was
             * already deleted due to a different failed data storage operation
             */
            logger.info("No metadata to delete for " + metaId);
        } else {
            String pluginName = persistedMetadata.getPluginName();
            PluginDao dao = PluginFactory.getInstance()
                    .getPluginDao(pluginName);
            dao.delete(Arrays.asList(persistedMetadata));
            logger.info("Metadata deleted for " + metaId);
            sendPurgeNotification(pluginName);
        }
    }

    private void sendPurgeNotification(String plugin) {
        try {
            messageProducer.sendAsyncUri(PURGE_TOPIC, plugin);
        } catch (EdexException e) {
            logger.error("Error sending purge notification for: " + plugin, e);
        }
    }
}
