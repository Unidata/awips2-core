package com.raytheon.uf.ignite.core;

import javax.jms.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.audit.AbstractDataStorageAuditerProxy;
import com.raytheon.uf.common.datastorage.audit.DataStorageAuditEvent;
import com.raytheon.uf.common.serialization.SerializationException;

public class IgniteDataStorageAuditerProxy
        extends AbstractDataStorageAuditerProxy {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConnectionFactory jmsConnectionFactory;

    public IgniteDataStorageAuditerProxy(
            ConnectionFactory jmsConnectionFactory) {
        this.jmsConnectionFactory = jmsConnectionFactory;
    }

    @Override
    protected void send(String uri, DataStorageAuditEvent event) {
        try {
            IgniteServerUtils.sendMessageToQueue(jmsConnectionFactory, uri,
                    event);
        } catch (SerializationException e) {
            logger.error("Error sending event to " + uri + ": " + event, e);
        }
    }
}
