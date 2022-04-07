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
package com.raytheon.uf.common.jms.notification;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

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
 * ------------- -------- ---------   --------------------------------------------
 * May 08, 2008  1127     randerso    Initial Creation
 * Sep 03, 2008  1448     chammack    Refactored notification observer interface
 * Apr 23, 2013  1939     randerso    Add separate connect method to allow
 *                                    application to complete initialization
 *                                    before connecting to JMS.
 * Oct 15, 2013  2389     rjpeter     Updated synchronization to remove session
 *                                    leaks.
 * Jul 21, 2014  3390     bsteffen    Extracted logic from the
 *                                    NotificationManagerJob
 * Oct 23, 2014  3390     bsteffen    Fix concurrency of disconnect and name
 *                                    threads.
 * Apr 06, 2015  3343     rjpeter     Fix deadlock and reconnect.
 * Feb 11, 2016  4630     rjpeter     Fix memory leak.
 * Mar 19, 2018  7141     randerso    Use a single timer for all
 *                                    ReconnectTimerTasks
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 16, 2019  7724     tgurney     Move queue creation to the session
 * Oct 22, 2019  7724     tgurney     Fix topic creation
 * Jan 16, 2020  8008     randerso    Move topic prefix to QpidUFSession
 * Apr 07, 2022  8836     tgurney     Replace synchronize on "this" with a
 *                                    dedicated lock accessible to subclasses
 *
 * </pre>
 *
 * @author randerso
 */
public class JmsNotificationManager
        implements ExceptionListener, AutoCloseable {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(JmsNotificationManager.class);

    private static final int IN_MEM_MESSAGE_LIMIT = 5000;

    private static class ListenerKey {
        private final String topic;

        private final String queryString;

        /**
         * @param topic
         * @param queryString
         */
        public ListenerKey(String topic, String queryString) {
            super();
            this.topic = topic;
            this.queryString = queryString;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + (queryString == null ? 0 : queryString.hashCode());
            result = prime * result + (topic == null ? 0 : topic.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ListenerKey other = (ListenerKey) obj;
            if (queryString == null) {
                if (other.queryString != null) {
                    return false;
                }
            } else if (!queryString.equals(other.queryString)) {
                return false;
            }
            if (topic == null) {
                if (other.topic != null) {
                    return false;
                }
            } else if (!topic.equals(other.topic)) {
                return false;
            }
            return true;
        }
    }

    private enum Type {
        QUEUE, TOPIC;
    }

    protected final ConnectionFactory connectionFactory;

    /** The observer map of topic to listeners */
    protected final Map<ListenerKey, NotificationListener> listeners;

    protected Connection connection;

    protected volatile boolean connected = false;

    protected final ThreadPoolExecutor executorService;

    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private final Timer reconnectTimer = new Timer("JMSReconnectTimer", true);

    private final Object connectionLock = new Object();

    /**
     * Timer task that updates reconnectScheduled and attempts to connect.
     */
    private class ReconnectTimerTask extends TimerTask {
        @Override
        public void run() {
            reconnectScheduled.set(false);
            connect(false);
        }

    }

    /**
     * Construct a JmsNotificationManager with a default thread name prefix
     *
     * @param connectionFactory
     */
    public JmsNotificationManager(ConnectionFactory connectionFactory) {
        this(connectionFactory, "JmsNotificationPool");
    }

    /**
     * Construct a JmsNotificationManager with a specified thread name prefix
     *
     * @param connectionFactory
     * @param notificationThreadNamePrefix
     */
    public JmsNotificationManager(ConnectionFactory connectionFactory,
            String notificationThreadNamePrefix) {
        this.connectionFactory = connectionFactory;
        this.listeners = new HashMap<>();
        executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 0,
                TimeUnit.SECONDS, new SynchronousQueue<Runnable>(),
                new NamedThreadFactory(notificationThreadNamePrefix));
    }

    /**
     * Initiate the jms connection.
     *
     * @param notifyError
     *            whether to report errors(through UFStatus) or ignore them.
     */
    public void connect(boolean notifyError) {
        synchronized (connectionLock) {
            if (connected) {
                return;
            }
            boolean successful = true;
            try {
                disconnect(notifyError);

                // Create a Connection
                connection = connectionFactory.createConnection();
                connection.setExceptionListener(this);
                connection.start();
                /* Enable thread caching. */
                executorService.setKeepAliveTime(60, TimeUnit.SECONDS);
                connected = true;
            } catch (JMSException e) {
                if (notifyError) {
                    statusHandler.handle(Priority.SIGNIFICANT,
                            "NotificationManager failed to connect.", e);
                }
                successful = false;
            }

            synchronized (listeners) {
                for (NotificationListener listener : listeners.values()) {
                    try {
                        listener.setupConnection(this);
                    } catch (JMSException e) {
                        successful = false;
                        if (notifyError) {
                            statusHandler.handle(Priority.SIGNIFICANT,
                                    "NotificationManager failed to setup message listener.",
                                    e);
                        }
                    }
                }
            }

            if (!successful) {
                onException(null);
            }
        }
    }

    /**
     * Creates a new AUTO_ACKNOWLEDGE session if connected to JMS, otherwise
     * returns null.
     *
     * @return the created session or null if not connected
     * @throws JMSException
     */
    protected Session createSession() throws JMSException {
        synchronized (connectionLock) {
            if (connected) {
                return connection.createSession(false,
                        Session.AUTO_ACKNOWLEDGE);
            } else {
                return null;
            }
        }
    }

    /**
     * Disconnect this JmsNotificationManager
     *
     * @param notifyError
     *            true if errors should be logged
     */
    public void disconnect(boolean notifyError) {
        synchronized (connectionLock) {
            if (connection == null) {
                return;
            }
            connected = false;
            synchronized (listeners) {
                for (NotificationListener listener : listeners.values()) {
                    listener.disconnect();
                }
            }
            try {
                connection.stop();
            } catch (Exception e) {
                if (notifyError) {
                    statusHandler.handle(Priority.SIGNIFICANT,
                            "NotificationManager failed to stop a connection.",
                            e);
                }
            }
            try {
                connection.close();
            } catch (Exception e) {
                if (notifyError) {
                    statusHandler.handle(Priority.SIGNIFICANT,
                            "NotificationManager failed to close a connection.",
                            e);
                }
            }
            /* Since threads should not be needed for now, let them finish. */
            executorService.setKeepAliveTime(0, TimeUnit.SECONDS);
            connection = null;
        }
    }

    /*
     * NOTE: cannot synchronize on connectionLock in onException. Often called
     * from within a synchronized on listeners and can end up in a deadlock
     * scenario.
     */
    @Override
    public void onException(JMSException e) {
        connected = false;

        /*
         * Schedule task to attempt to reconnect in 5 seconds.
         */
        if (reconnectScheduled.compareAndSet(false, true)) {
            reconnectTimer.schedule(new ReconnectTimerTask(),
                    Duration.of(5, ChronoUnit.SECONDS).toMillis());
        }

        synchronized (listeners) {
            // disconnect listeners
            for (NotificationListener listener : listeners.values()) {
                listener.disconnect();
            }
        }

        if (e != null) {
            statusHandler.handle(Priority.SIGNIFICANT,
                    "Error in JMS connectivity: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Register for notification messages using a callback.
     *
     * @param queue
     *            message queue
     * @param obs
     *            the notification observer callback
     */
    public void addQueueObserver(String queue, INotificationObserver obs) {
        addQueueObserver(queue, obs, null);
    }

    /**
     * Register for notification messages using a callback.
     *
     * @param queue
     *            message queue
     * @param obs
     *            the notification observer callback
     * @param queryString
     *            filter to only match certain messages
     */
    public void addQueueObserver(String queue, INotificationObserver obs,
            String queryString) {
        ListenerKey key = new ListenerKey(queue, queryString);
        NotificationListener newListener = null;

        synchronized (listeners) {
            NotificationListener listener = listeners.get(key);
            if (listener == null) {
                listener = new NotificationListener(executorService, queue,
                        queryString, Type.QUEUE);
                listeners.put(key, listener);
                listener.addObserver(obs);
                newListener = listener;
            } else {
                listener.addObserver(obs);
            }
        }

        if (newListener != null && connected) {
            try {
                newListener.setupConnection(this);
            } catch (JMSException e) {
                statusHandler.error(
                        "NotificationManager failed to create queue consumer for queue ["
                                + queue + "].",
                        e);
            }
        }
    }

    /**
     * Register for notification messages using a callback.
     *
     * @param topic
     *            message topic
     * @param obs
     *            the notification observer callback
     */
    public void addObserver(String topic, INotificationObserver obs) {
        addObserver(topic, obs, null);
    }

    /**
     * Register for notification messages using a callback.
     *
     * @param topic
     *            message topic
     * @param obs
     *            the notification observer callback
     * @param queryString
     *            filter to only match certain messages
     */
    public void addObserver(String topic, INotificationObserver obs,
            String queryString) {
        ListenerKey key = new ListenerKey(topic, queryString);
        NotificationListener newListener = null;

        synchronized (listeners) {
            NotificationListener listener = listeners.get(key);
            if (listener == null) {
                listener = new NotificationListener(executorService, topic,
                        queryString, Type.TOPIC);
                listeners.put(key, listener);
                listener.addObserver(obs);
                newListener = listener;
            } else {
                listener.addObserver(obs);
            }
        }

        if (newListener != null && connected) {
            try {
                newListener.setupConnection(this);
            } catch (JMSException e) {
                statusHandler.error(
                        "NotificationManager failed to create topic consumer for topic ["
                                + topic + "].",
                        e);
            }
        }
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
    public void removeObserver(String topic, INotificationObserver obs) {
        removeObserver(topic, obs, null);
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
    public void removeObserver(String topic, INotificationObserver obs,
            String queryString) {
        ListenerKey key = new ListenerKey(topic, queryString);
        synchronized (listeners) {
            NotificationListener listener = listeners.get(key);
            if (listener == null) {
                return;
            }
            listener.removeObserver(obs);
            if (listener.size() <= 0) {
                listener.disconnect();
                listeners.remove(key);
            }
        }
    }

    /**
     * Removes an alert message observer that was registered using the
     * addObserver method. This must be called in exactly the same
     *
     * @param queue
     *            message topic
     * @param queryString
     * @param obs
     *            the observer to remove
     */
    public void removeQueueObserver(String queue, String queryString,
            INotificationObserver obs) {
        ListenerKey key = new ListenerKey(queue, queryString);
        synchronized (listeners) {
            NotificationListener listener = listeners.get(key);
            if (listener == null) {
                return;
            }
            listener.removeObserver(obs);
            if (listener.size() <= 0) {
                listener.disconnect();
                listeners.remove(key);
            }
        }
    }

    /**
     * Connect to JMS
     */
    public void connect() {
        connect(true);
    }

    /**
     * Disconnect from JMS
     */
    public void disconnect() {
        disconnect(true);
    }

    private static class NotificationListener implements MessageListener {

        private final ExecutorService executorService;

        private final Type type;

        private final String id;

        private final String queryString;

        /** The list of interested parties */
        protected final List<INotificationObserver> observers;

        /** The map of job threads from observers */
        protected final Map<INotificationObserver, JobWrapper> jobWrappers;

        protected MessageConsumer consumer;

        protected Session session;

        public NotificationListener(ExecutorService executorService, String id,
                String queryString, Type type) {
            this.executorService = executorService;
            this.observers = new ArrayList<>();
            this.jobWrappers = new HashMap<>();
            this.type = type;
            this.id = id;
            this.queryString = queryString;
        }

        private void setupConnection(JmsNotificationManager manager)
                throws JMSException {
            switch (type) {
            case QUEUE: {
                setupQueue(manager);
                break;
            }
            case TOPIC: {
                setupTopic(manager);
                break;
            }
            }
        }

        public synchronized void disconnect() {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (JMSException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error closing consumer connection", e);
                }
                consumer = null;
            }

            if (session != null) {
                try {
                    session.close();
                } catch (JMSException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error closing session", e);
                }
                session = null;
            }
        }

        private synchronized void setupQueue(JmsNotificationManager manager)
                throws JMSException {
            disconnect();
            session = manager.createSession();
            if (session != null) {
                String queueName = id;
                Queue t = session.createQueue(queueName);

                if (queryString != null) {
                    consumer = session.createConsumer(t, queryString);
                } else {
                    consumer = session.createConsumer(t);
                }

                consumer.setMessageListener(this);
            }
        }

        private synchronized void setupTopic(JmsNotificationManager manager)
                throws JMSException {
            disconnect();
            session = manager.createSession();
            if (session != null) {
                Topic t = session.createTopic(id);

                if (queryString != null) {
                    consumer = session.createConsumer(t, queryString);
                } else {
                    consumer = session.createConsumer(t);
                }

                consumer.setMessageListener(this);
            }
        }

        @Override
        public void onMessage(Message msg) {
            synchronized (observers) {
                for (INotificationObserver obs : observers) {
                    // Get the corresponding job, creating the
                    // wrapper if necessary, really on observers lock for sync
                    // purposes
                    JobWrapper wrapper = jobWrappers.get(obs);
                    if (wrapper == null) {
                        wrapper = new JobWrapper(executorService, obs);
                        jobWrappers.put(obs, wrapper);
                    }
                    wrapper.put(msg);
                }
            }
        }

        public void addObserver(INotificationObserver obs) {
            synchronized (observers) {
                observers.add(obs);
            }
        }

        public void removeObserver(INotificationObserver obs) {
            synchronized (observers) {
                observers.remove(obs);
                jobWrappers.remove(obs);
            }
        }

        public int size() {
            synchronized (observers) {
                return observers.size();
            }
        }
    }

    /**
     * Provides internal insulation from slow-running processes by forming a
     * queue
     *
     */
    private static class JobWrapper implements Runnable {

        protected final ExecutorService executorService;

        protected java.util.Queue<Message> messages;

        protected INotificationObserver observer;

        protected long lastErrorPrintTime = 0;

        protected AtomicInteger messageCount;

        protected Future<?> lastRun;

        public JobWrapper(ExecutorService executorService,
                INotificationObserver observer) {
            // super("Alert thread for " + observer);
            this.observer = observer;
            this.messages = new ConcurrentLinkedQueue<>();
            this.messageCount = new AtomicInteger(0);
            this.executorService = executorService;
        }

        @Override
        public void run() {
            List<NotificationMessage> messageList = new ArrayList<>();
            // one error log messages per execution of job
            boolean errorLogged = false;
            while (!this.messages.isEmpty()) {

                while (!this.messages.isEmpty()) {
                    messageCount.decrementAndGet();
                    Message msg = messages.remove();

                    if (!(msg instanceof BytesMessage)) {
                        statusHandler.error(
                                "Incoming message was not a binary message as expected");
                    } else {
                        messageList.add(new NotificationMessage(msg));
                    }
                }

                try {
                    observer.notificationArrived(messageList.toArray(
                            new NotificationMessage[messageList.size()]));
                } catch (Throwable e) {
                    if (!errorLogged) {
                        statusHandler.error(
                                "Error occurred during alert processing", e);
                        errorLogged = true;
                    }
                }
                messageList.clear();
            }
        }

        /**
         * Add the message to the queue
         *
         * @param msg
         *            the msg
         */
        public void put(Message msg) {
            messages.offer(msg);
            if (messageCount.incrementAndGet() > IN_MEM_MESSAGE_LIMIT) {
                messageCount.decrementAndGet();
                messages.remove();
                if (System.currentTimeMillis() - lastErrorPrintTime > Duration
                        .of(10, ChronoUnit.MINUTES).toMillis()) {
                    statusHandler
                            .error("Message queue size exceeded for observer "
                                    + observer
                                    + ", old messages will be replaced by incoming messages.");
                    lastErrorPrintTime = System.currentTimeMillis();
                }
            }
            if (lastRun == null || lastRun.isDone()) {
                lastRun = executorService.submit(this);
            }
        }

    }

    @Override
    public void close() {
        synchronized (listeners) {
            for (NotificationListener listener : listeners.values()) {
                listener.disconnect();
            }
            listeners.clear();
        }
        disconnect(true);
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);

        private final ThreadGroup group;

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        private final String namePrefix;

        public NamedThreadFactory(String namePrefix) {
            SecurityManager s = System.getSecurityManager();
            group = s != null ? s.getThreadGroup()
                    : Thread.currentThread().getThreadGroup();
            this.namePrefix = namePrefix + "-" + poolNumber.getAndIncrement()
                    + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    protected Object getConnectionLock() {
        return connectionLock;
    }
}
