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
import java.util.stream.Collectors;

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
 *
 * </pre>
 *
 * @author mapeters
 */
public class DataStorageAuditUtils {

    /**
     * Private constructor to prevent instantiation since everything is static.
     */
    private DataStorageAuditUtils() {
    }

    /**
     * Get a mapping of all the given PDO trace IDs to the given data status.
     *
     * @param status
     *            the data status to use for all given PDOs
     * @param pdos
     *            the plugin data objects
     * @return map of PDO trace ID -> status
     */
    public static Map<String, DataStatus> getDataStatusMap(DataStatus status,
            Collection<? extends PluginDataObject> pdos) {
        return pdos.stream().collect(
                Collectors.toMap(pdo -> pdo.getTraceId(), pdo -> status));
    }

    /**
     * Get a mapping of all the given PDO trace IDs to the given data status.
     *
     * @param status
     *            the data status to use for all given PDOs
     * @param pdos
     *            the plugin data objects
     * @return map of PDO trace ID -> status
     */
    public static Map<String, DataStatus> getDataStatusMap(DataStatus status,
            PluginDataObject... pdos) {
        return Arrays.stream(pdos).collect(
                Collectors.toMap(pdo -> pdo.getTraceId(), pdo -> status));
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
            Map<String, MetadataStatus> traceIdsToStatus = pdos.stream()
                    .collect(Collectors.toMap(pdo -> pdo.getTraceId(),
                            pdo -> status));
            DataStorageAuditerContainer.getInstance().getAuditer()
                    .processMetadataStatuses(traceIdsToStatus);
        }
    }
}
