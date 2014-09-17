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
import javax.jms.JMSException;

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
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1
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
                return JMSConnection.getInstance()
                        .getFactory().createConnection(arg0, arg1);
                }
            
            @Override
            public Connection createConnection() throws JMSException {
                return JMSConnection.getInstance()
                        .getFactory().createConnection();
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

    /**
     * @deprecated use
     *             {@link #addQueueObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver)}
     */
    @Deprecated
    public static void addQueueObserver(String queue,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs) {
        addQueueObserver(queue, new DeprecatedObserverAdapter(obs));
    }

    public static void addQueueObserver(String queue,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs,
            String queryString) {
        getInstance().getManager().addQueueObserver(queue, obs, queryString);
    }

    /**
     * @deprecated use
     *             {@link #addQueueObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver, String)}
     */
    @Deprecated
    public static void addQueueObserver(String queue,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs,
            String queryString) {
        addQueueObserver(queue, new DeprecatedObserverAdapter(obs),
                queryString);
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

    /**
     * @deprecated use
     *             {@link #addObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver)}
     */
    @Deprecated
    public static void addObserver(String topic,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs) {
        addObserver(topic, new DeprecatedObserverAdapter(obs));
    }

    public static void addObserver(String topic,
            com.raytheon.uf.common.jms.notification.INotificationObserver obs,
            String queryString) {
        getInstance().getManager().addObserver(topic, obs, queryString);
    }

    /**
     * @deprecated use
     *             {@link #addObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver, String)}
     */
    @Deprecated
    public static void addObserver(String topic,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs,
            String queryString) {
        addObserver(topic, new DeprecatedObserverAdapter(obs), queryString);
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
     * @deprecated use
     *             {@link #removeObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver)}
     */
    @Deprecated
    public static void removeObserver(String topic,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs) {
        removeObserver(topic, new DeprecatedObserverAdapter(obs));
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
     * @deprecated use
     *             {@link #removeObserver(String, com.raytheon.uf.common.jms.notification.INotificationObserver, String)}
     */
    @Deprecated
    public static void removeObserver(String topic,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs,
            String queryString) {
        removeObserver(topic, new DeprecatedObserverAdapter(obs), queryString);
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
     * @deprecated use
     *             {@link #removeQueueObserver(String, String, com.raytheon.uf.common.jms.notification.INotificationObserver)}
     */
    @Deprecated
    public static void removeQueueObserver(String queue, String queryString,
            com.raytheon.uf.viz.core.notification.INotificationObserver obs) {
        removeQueueObserver(queue, queryString, new DeprecatedObserverAdapter(
                obs));
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

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.services.IDisposable#dispose()
     */
    @Override
    public void dispose() {
        manager.close();
    }

    private static class DeprecatedObserverAdapter implements
            com.raytheon.uf.common.jms.notification.INotificationObserver {

        private final com.raytheon.uf.viz.core.notification.INotificationObserver delegate;

        public DeprecatedObserverAdapter(
                com.raytheon.uf.viz.core.notification.INotificationObserver delegate) {
            this.delegate = delegate;
        }

        @Override
        public void notificationArrived(
                com.raytheon.uf.common.jms.notification.NotificationMessage[] messages) {
            com.raytheon.uf.viz.core.notification.NotificationMessage[] deprecatedMessages = new com.raytheon.uf.viz.core.notification.NotificationMessage[messages.length];
            for (int i = 0; i < messages.length; i += 1) {
                deprecatedMessages[i] = new com.raytheon.uf.viz.core.notification.NotificationMessage(
                        messages[i]);
            }
            delegate.notificationArrived(deprecatedMessages);
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            DeprecatedObserverAdapter other = (DeprecatedObserverAdapter) obj;
            if (!delegate.equals(other.delegate))
                return false;
            return true;
        }
        
        
    }
}
