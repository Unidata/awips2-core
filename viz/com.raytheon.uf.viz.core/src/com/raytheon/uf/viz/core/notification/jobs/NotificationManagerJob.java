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
package com.raytheon.uf.viz.core.notification.jobs;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;

import org.apache.qpid.jms.exceptions.JmsExceptionSupport;
import org.eclipse.ui.services.IDisposable;

import com.raytheon.uf.common.jms.notification.JmsNotificationManager;
import com.raytheon.uf.viz.core.Activator;
import com.raytheon.uf.viz.core.comm.JMSConnection;

/**
 * Job to monitor the JMS topic containing notification messages, delegating the
 * messages to the appropriate listeners. Listener execution is performed in
 * isolated threads, so users may perform appropriate operations inside the
 * listener method.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * May 08, 2008  1127     randerso    Initial Creation
 * Sep 03, 2008  1448     chammack    Refactored notification observer interface
 * Apr 23, 2013  1939     randerso    Add separate connect method to allow application
 *                                    to complete initialization before connecting to JMS.
 * Oct 15, 2013  2389     rjpeter     Updated synchronization to remove session leaks.
 * Jul 21, 2014  3390     bsteffen    Extracted logic to the JmsNotificationLogic
 * Nov 08, 2016  5976     bsteffen    Remove deprecated methods
 * Feb 02, 2017  6085     bsteffen    Do not NPE after problems in JMSConnection
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 16, 2019  7724     tgurney     Remove references to broker REST API
 *
 * </pre>
 *
 * @author randerso
 */
public class NotificationManagerJob implements IDisposable {

    /** The singleton instance */
    private static NotificationManagerJob instance;

    private final JmsNotificationManager manager;

    /**
     * Get the active subscription manager job. If one does not exist, start an
     * instance as a system job.
     *
     * @return the subscription manager
     */
    protected static synchronized NotificationManagerJob getInstance() {
        if (instance == null) {
            instance = new NotificationManagerJob();
        }
        return instance;
    }

    protected static synchronized void setCustomInstance(
            NotificationManagerJob notificationManagerJob) {
        instance = notificationManagerJob;
    }

    /**
     * @param name
     */
    protected NotificationManagerJob() {
        /*
         * Must create a delegate factory so it is possible to add listeners
         * before the jms url is set. in JMSConnection.
         */
        ConnectionFactory delegateFactory = new ConnectionFactory() {
            @Override
            public Connection createConnection(String arg0, String arg1)
                    throws JMSException {
                ConnectionFactory factory = JMSConnection.getInstance()
                        .getFactory();
                if (factory == null) {
                    throw new JMSException("Failed to load ConnectionFactory");
                }
                return factory.createConnection(arg0, arg1);
            }

            @Override
            public Connection createConnection() throws JMSException {
                ConnectionFactory factory = JMSConnection.getInstance()
                        .getFactory();
                if (factory == null) {
                    throw new JMSException("Failed to load ConnectionFactory");
                }
                return factory.createConnection();
            }

            @Override
            public JMSContext createContext() {
                try {
                    return JMSConnection.getInstance().getFactory()
                            .createContext();
                } catch (JMSException jmse) {
                    throw JmsExceptionSupport.createRuntimeException(jmse);
                }
            }

            @Override
            public JMSContext createContext(int sessionMode) {
                try {
                    return JMSConnection.getInstance().getFactory()
                            .createContext(sessionMode);
                } catch (JMSException jmse) {
                    throw JmsExceptionSupport.createRuntimeException(jmse);
                }
            }

            @Override
            public JMSContext createContext(String userName, String password) {
                throw new UnsupportedOperationException(
                        "NotificationManagerJob not implemented for username/password connections");
            }

            @Override
            public JMSContext createContext(String userName, String password,
                    int sessionMode) {
                throw new UnsupportedOperationException(
                        "NotificationManagerJob not implemented for username/password connections");
            }
        };
        manager = new JmsNotificationManager(delegateFactory);
        Activator.getDefault().registerDisposable(this);
    }

    protected JmsNotificationManager getManager() {
        return manager;
    }

    protected void connect(boolean notifyError) {
        manager.connect(notifyError);
    }

    protected void disconnect(boolean notifyError) {
        manager.disconnect(notifyError);
    }

    public static void addQueueObserver(String queue,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs) {
        getInstance().getManager().addQueueObserver(queue, obs);
    }

    public static void addQueueObserver(String queue,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs,
            String queryString) {
        getInstance().getManager().addQueueObserver(queue, obs, queryString);
    }

    /**
     * Register for notification messages using a callback.
     *
     * @param topic
     *            message topic
     * @param obs
     *            the alert observer callback
     */
    public static void addObserver(String topic,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs) {
        getInstance().getManager().addObserver(topic, obs);
    }

    public static void addObserver(String topic,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs,
            String queryString) {
        getInstance().getManager().addObserver(topic, obs, queryString);
    }

    /**
     * Removes an alert message observer that was registered using the
     * addObserver method. This must be called in exactly the same
     *
     * @param topic
     *            message topic
     * @param obs
     *            the observer to remove
     */
    public static void removeObserver(String topic,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs) {
        getInstance().getManager().removeObserver(topic, obs);
    }

    /**
     * Removes an alert message observer that was registered using the
     * addObserver method. This must be called in exactly the same
     *
     * @param topic
     *            message topic
     * @param obs
     *            the observer to remove
     * @param queryString
     */
    public static void removeObserver(String topic,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs,
            String queryString) {
        getInstance().getManager().removeObserver(topic, obs, queryString);
    }

    /**
     * Removes an alert message observer that was registered using the
     * addObserver method. This must be called in exactly the same
     *
     * @param topic
     *            message topic
     * @param obs
     *            the observer to remove
     */
    public static void removeQueueObserver(String queue, String queryString,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs) {
        getInstance().getManager().removeQueueObserver(queue, queryString, obs);
    }

    /**
     * Connect to JMS
     */
    public static void connect() {
        getInstance().connect(true);
    }

    /**
     * Disconnect from JMS
     */
    public static void disconnect() {
        getInstance().disconnect(true);
    }

    @Override
    public void dispose() {
        manager.close();
    }
}
