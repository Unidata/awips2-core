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
package com.raytheon.uf.ignite.core;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import com.raytheon.uf.common.datastorage.audit.AbstractDataStorageAuditerProxy;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditEvent;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditer;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * {@link IDataStorageAuditer} proxy implementation for sending data storage
 * events from an Ignite server process.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Jun 22, 2022 8865       mapeters    Let exceptions propagate in send()
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteDataStorageAuditerProxy
        extends AbstractDataStorageAuditerProxy {

    private final ConnectionFactory jmsConnectionFactory;

    public IgniteDataStorageAuditerProxy(
            ConnectionFactory jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    @Override
    protected void send(DataStorageAuditEvent event)
            throws SerializationException, JMSException {
        IgniteServerUtils.sendMessageToQueue(jmsConnectionFactory, URI, event);
    }
}
