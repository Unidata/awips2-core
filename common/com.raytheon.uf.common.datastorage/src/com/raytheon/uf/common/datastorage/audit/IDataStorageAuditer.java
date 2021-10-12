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

import java.util.Map;

/**
 * Interface for auditing data storage routes. The appropriate implementation
 * should typically be accessed via DataStorageAuditerContainer.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IDataStorageAuditer {

    String uri = "data.storage.audit.event";

    /**
     * Audit the metadata and data IDs of data being stored.
     *
     * @param dataIds
     *            the metadata and data IDs
     */
    void processDataIds(MetadataAndDataId[] dataIds);

    /**
     * Audit the statuses of metadata being persisted to the database.
     *
     * @param statuses
     *            map of trace IDs to metadata persist status
     */
    void processMetadataStatuses(Map<String, MetadataStatus> statuses);

    /**
     * Audit the statuses of data being persisted to the datastore.
     *
     * @param statuses
     *            map of trace IDs to data persist status
     */
    void processDataStatuses(Map<String, DataStatus> statuses);

    /**
     * Audit a data storage event. Other methods in this interface should
     * generally be used instead as they provide a more convenient way of
     * handling particular events.
     *
     * @param event
     *            the data storage event
     */
    void processEvent(DataStorageAuditEvent event);
}
