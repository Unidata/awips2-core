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
package com.raytheon.uf.common.logback.appender;

import java.lang.reflect.Constructor;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * An abstract appender for sending a log event to a JMS queue or topic.
 * Subclasses must implement the method:
 * 
 * <code>protected void append(ILoggingEvent)</code>
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 22, 2015  4473      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public abstract class AbstractJmsAppender extends AppenderBase<ILoggingEvent>
        implements ExceptionListener {

    protected String uri;

    protected String destName;

    protected String connFactoryClass;

    protected ConnectionFactory connFactory;

    protected Connection connection;

    protected Session session;

    protected Destination dest;

    protected MessageProducer producer;

    @SuppressWarnings("unchecked")
    @Override
    public void start() {
        if (connFactoryClass == null) {
            throw new IllegalArgumentException(
                    "Must specify a connection factory class.");
        }
        if (connFactory == null) {
            try {
                Class<ConnectionFactory> clz = (Class<ConnectionFactory>) Class
                        .forName(connFactoryClass, false,
                                AbstractJmsAppender.class.getClassLoader());
                Constructor<ConnectionFactory> cons = clz
                        .getConstructor(String.class);
                connFactory = cons.newInstance(uri);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(
                        "Error initializing JMS connection factory", e);
            }

        }
        connect();
        super.start();
    }

    @Override
    public void stop() {
        disconnect();
        super.stop();
    }

    /**
     * Creates the connection, session, dest, and producer to enable the ability
     * to send JMS messages
     * 
     * @return true if the connection was successful, otherwise false
     */
    protected boolean connect() {
        try {
            connection = connFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            dest = createDestination(session, destName);
            producer = session.createProducer(dest);
            connection.setExceptionListener(this);
            return true;
        } catch (JMSException e) {
            disconnect();
            return false;
        }
    }

    protected abstract Destination createDestination(Session session,
            String name) throws JMSException;

    protected void disconnect() {
        if (producer != null) {
            try {
                producer.close();
            } catch (JMSException e) {
                // ignore
            } finally {
                producer = null;
            }
        }

        if (session != null) {
            try {
                session.close();
            } catch (JMSException e) {
                // ignore
            } finally {
                session = null;
            }
        }

        if (connection != null && super.isStarted()) {
            try {
                connection.stop();
            } catch (JMSException e) {
                // ignore
            }
        }

        if (connection != null) {
            try {
                connection.close();
            } catch (JMSException e) {
                // ignore
            } finally {
                connection = null;
            }
        }
    }

    public boolean isConnected() {
        return connection != null && session != null && dest != null
                && producer != null;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDestName() {
        return destName;
    }

    public void setDestName(String destName) {
        this.destName = destName;
    }

    public String getConnFactoryClass() {
        return connFactoryClass;
    }

    public void setConnFactoryClass(String connFactoryClass) {
        this.connFactoryClass = connFactoryClass;
    }

    @Override
    public void onException(JMSException e) {
        // can't log to slf4j, this is coming from slf4j
        e.printStackTrace();
        disconnect();
    }

}
