/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.jms.qpid;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Wrapper over a Qpid connection that exposes the broker's REST API to sessions
 * created from this connection
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 15, 2019 7724       tgurney     Initial creation
 * Oct 23, 2019 7724       tgurney     Log session creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class QpidUFConnection implements Connection {

    private final IBrokerRestProvider jmsAdmin;

    private final Connection connection;

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(QpidUFConnection.class);

    public QpidUFConnection(Connection connection,
            IBrokerRestProvider jmsAdmin) {
        this.connection = connection;
        this.jmsAdmin = jmsAdmin;
    }

    @Override
    public Session createSession(boolean transacted, int acknowledgeMode)
            throws JMSException {
        Session session = connection.createSession(transacted, acknowledgeMode);
        statusHandler.info("Created new Qpid session (transacted = "
                + transacted + ", acknowledgeMode = " + acknowledgeMode + ")");
        return new QpidUFSession(session, jmsAdmin);
    }

    @Override
    public Session createSession(int sessionMode) throws JMSException {
        Session session = connection.createSession(sessionMode);
        statusHandler.info(
                "Created new Qpid session (sessionMode = " + sessionMode + ")");
        return new QpidUFSession(session, jmsAdmin);
    }

    @Override
    public Session createSession() throws JMSException {
        Session session = connection.createSession();
        statusHandler.info("Created new Qpid session");
        return new QpidUFSession(session, jmsAdmin);
    }

    @Override
    public String getClientID() throws JMSException {
        return connection.getClientID();
    }

    @Override
    public void setClientID(String clientID) throws JMSException {
        connection.setClientID(clientID);
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {
        return connection.getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {
        return connection.getExceptionListener();
    }

    @Override
    public void setExceptionListener(ExceptionListener listener)
            throws JMSException {
        connection.setExceptionListener(listener);
    }

    @Override
    public void start() throws JMSException {
        connection.start();
    }

    @Override
    public void stop() throws JMSException {
        connection.stop();
    }

    @Override
    public void close() throws JMSException {
        connection.close();
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(Destination destination,
            String messageSelector, ServerSessionPool sessionPool,
            int maxMessages) throws JMSException {
        return connection.createConnectionConsumer(destination, messageSelector,
                sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(Topic topic,
            String subscriptionName, String messageSelector,
            ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        return connection.createDurableConnectionConsumer(topic,
                subscriptionName, messageSelector, sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic,
            String subscriptionName, String messageSelector,
            ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        return connection.createSharedDurableConnectionConsumer(topic,
                subscriptionName, messageSelector, sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic,
            String subscriptionName, String messageSelector,
            ServerSessionPool sessionPool, int maxMessages)
            throws JMSException {
        return connection.createSharedConnectionConsumer(topic,
                subscriptionName, messageSelector, sessionPool, maxMessages);
    }

}
