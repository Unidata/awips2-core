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
 * Abstract class for {@link IDataStorageAuditer} proxy implementations that
 * just send data storage events to the necessary JMS endpoints, where they'll
 * be picked up by the actual auditer implementation.
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
public abstract class AbstractDataStorageAuditerProxy
        implements IDataStorageAuditer {

    @Override
    public void processDataIds(MetadataAndDataId[] dataIds) {
        if (dataIds != null && dataIds.length > 0) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setDataIds(dataIds);
            processEvent(event);
        }
    }

    @Override
    public void processMetadataStatuses(Map<String, MetadataStatus> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setMetadataStatuses(statuses);
            processEvent(event);
        }
    }

    @Override
    public void processDataStatuses(Map<String, DataStatus> statuses) {
        if (statuses != null && !statuses.isEmpty()) {
            DataStorageAuditEvent event = new DataStorageAuditEvent();
            event.setDataStatuses(statuses);
            processEvent(event);
        }
    }

    @Override
    public void processEvent(DataStorageAuditEvent event) {
        send(uri, event);
    }

    /**
     * Send the given audit event to the given uri.
     *
     * @param uri
     *            the URI to send to
     * @param event
     *            the audit event to send
     */
    protected abstract void send(String uri, DataStorageAuditEvent event);
}
