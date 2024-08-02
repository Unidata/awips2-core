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
package com.raytheon.uf.common.datastorage.audit;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import com.raytheon.uf.common.dataplugin.PluginDataObject;

/**
 *
 * Utility constants and methods for data storage auditing.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2022 8608       mapeters    Initial creation
 * Aug 24, 2022 8920       mapeters    Optimizations; Swap key/values for statuses.
 * Feb 03, 2023 9019       mapeters    Adjust for configurable number of audit threads.
 *
 * </pre>
 *
 * @author mapeters
 */
public class DataStorageAuditUtils {

    public static final int NUM_QUEUES = Integer
            .getInteger("data.storage.auditer.num.queues");

    public static final String QUEUE_ROOT_NAME = "data.storage.audit.event";

    public static final String QUEUE_JMS_PREFIX = "jms-durable:queue:";

    /**
     * Private constructor to prevent instantiation since everything is static.
     */
    private DataStorageAuditUtils() {
    }

    /**
     * Get a mapping of the given data status to all the given PDO trace IDs.
     *
     * @param status
     *            the data status to use for all given PDOs
     * @param pdos
     *            the plugin data objects
     * @return map of status -> all PDO trace IDs per status
     */
    public static Map<DataStatus, String[]> getDataStatusMap(DataStatus status,
            Collection<? extends PluginDataObject> pdos) {
        String[] traceIds = pdos.stream().map(PluginDataObject::getTraceId)
                .toArray(String[]::new);
        return Map.of(status, traceIds);
    }

    /**
     * Get a mapping of the given data status to all the given PDO trace IDs.
     *
     * @param status
     *            the data status to use for all given PDOs
     * @param pdos
     *            the plugin data objects
     * @return map of status -> all PDO trace IDs per status
     */
    public static Map<DataStatus, String[]> getDataStatusMap(DataStatus status,
            PluginDataObject... pdos) {
        String[] traceIds = Arrays.stream(pdos)
                .map(PluginDataObject::getTraceId).toArray(String[]::new);
        return Map.of(status, traceIds);
    }

    /**
     * Audit that all the given PDOs' metadata storage completed with the given
     * status.
     *
     * @param status
     *            the metadata status to use for all given PDOs
     * @param pdos
     *            the plugin data objects
     */
    public static void auditMetadataStatuses(MetadataStatus status,
            Collection<? extends PluginDataObject> pdos) {
        if (!pdos.isEmpty()) {
            String[] traceIds = pdos.stream().map(PluginDataObject::getTraceId)
                    .toArray(String[]::new);
            Map<MetadataStatus, String[]> traceIdsToStatus = Map.of(status,
                    traceIds);
            DataStorageAuditerContainer.getInstance().getAuditer()
                    .processMetadataStatuses(traceIdsToStatus);
        }
    }
}
