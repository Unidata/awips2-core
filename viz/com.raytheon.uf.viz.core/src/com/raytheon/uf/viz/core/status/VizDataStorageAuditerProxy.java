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
package com.raytheon.uf.viz.core.status;

import javax.jms.JMSException;

import com.raytheon.uf.common.datastorage.audit.AbstractDataStorageAuditerProxy;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditEvent;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditer;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * {@link IDataStorageAuditer} proxy implementation for sending data storage
 * events from a non-EDEX process (e.g. CAVE or ignite cache server).
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
public class VizDataStorageAuditerProxy
        extends AbstractDataStorageAuditerProxy {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(VizDataStorageAuditerProxy.class);

    @Override
    protected void send(String uri, DataStorageAuditEvent event) {
        try {
            MessageSender.sendToQueue(uri, event);
        } catch (JMSException | SerializationException e) {
            statusHandler.error("Error sending event to " + uri + ": " + event,
                    e);
        }
    }
}
