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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.audit.AbstractDataStorageAuditerProxy;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditEvent;
import com.raytheon.uf.common.datastorage.audit.IDataStorageAuditer;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.core.IMessageProducer;

/**
 * {@link IDataStorageAuditer} proxy implementation for sending data storage
 * events from an EDEX process.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Feb 23, 2022 8608       mapeters    Change queue to durable
 *
 * </pre>
 *
 * @author mapeters
 */
public class EdexDataStorageAuditerProxy
        extends AbstractDataStorageAuditerProxy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IMessageProducer messageProducer;

    private final boolean enabled = "ignite"
            .equals(System.getenv("DATASTORE_PROVIDER"));

    public EdexDataStorageAuditerProxy(IMessageProducer messageProducer) {
        this.messageProducer = messageProducer;
    }

    @Override
    public void send(String uri, DataStorageAuditEvent event) {
        if (!enabled) {
            return;
        }

        uri = "jms-durable:queue:" + uri;
        try {
            messageProducer.sendAsyncThriftUri(uri, event);
        } catch (EdexException | SerializationException e) {
            logger.error("Error sending event to " + uri + ": " + event, e);
        }
    }
}
