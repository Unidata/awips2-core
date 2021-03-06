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
package com.raytheon.uf.edex.event;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.google.common.eventbus.EventBus;
import com.raytheon.uf.common.event.Event;
import com.raytheon.uf.common.event.IBaseEventBusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.event.handler.PublishExternalEvent;

/**
 * EDEX implementation of {@link IBaseEventBusHandler}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 28, 2013  1650     djohnson  Simplified and extracted from {@link
 *                                  EdexEventBusHandler}.
 * Jun 20, 2013  1802     djohnson  Thread local is not safe across multiple
 *                                  transaction levels.
 * May 14, 2015  4493     dhladky   External event delivery option.
 * Jun 28, 2016  5670     tjensen   Moved EventTransactionSynchronization to a subclass
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public abstract class BaseEdexEventBusHandler<T> implements
        IBaseEventBusHandler<T> {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(BaseEdexEventBusHandler.class);

    /** publishes external events **/
    private PublishExternalEvent externalPublisher = PublishExternalEvent
            .getInstance();

    private static final String NULL_SUBSCRIBER = "Ignoring a null subscriber.";

    // Set that keeps a reference to all objects which have registered, which
    // simplifies whether or not an object should be unregistered since google
    // eventbus doesn't have a way of knowing who did or did not register
    private final Set<Object> registeredObjects = Collections
            .synchronizedSet(Collections
                    .<Object> newSetFromMap(new IdentityHashMap<Object, Boolean>()));

    /**
     * The actual Google EventBus instances being wrapped.
     */
    protected final List<com.google.common.eventbus.EventBus> googleEventBuses;

    /**
     * Constructor.
     */
    public BaseEdexEventBusHandler() {
        this(new AsynchronousEventBusFactory());
    }

    /**
     * Constructor specifying how to create the EventBus instances.
     * 
     * @param eventBusFactory
     *            the factory
     */
    protected BaseEdexEventBusHandler(GoogleEventBusFactory eventBusFactory) {
        this.googleEventBuses = eventBusFactory.getEventBuses();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void publish(T eventObject) {
        if (eventObject == null) {
            throw new IllegalArgumentException(
                    "Cannot publish a null eventObject");
        }

        if (isTransactionActive()) {

            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager
                        .registerSynchronization(new EventTransactionSynchronization(
                                eventObject));
            }
        } else {
            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                statusHandler
                        .debug("Sending event from non-transactional operation");
            }

            publishEvent(eventObject);
        }
    }

    protected void publishEvent(T eventObject) {
        boolean deliverLocal = true;
        // Publish events marked "external" to JMS external event topic
        if (eventObject instanceof Event) {
            Event event = (Event) eventObject;
            if (event.isExternal()) {
                deliverLocal = false;
                externalPublisher.publish(event);
            }
        }

        // Deliver non-local (and non-events) via Guava by default.
        if (deliverLocal) {
            publishInternal(eventObject);
        }
    }

    /**
     * Publish the actual event object.
     * 
     * @param event
     *            the event
     */
    protected abstract void publishInternal(T event);

    /**
     * Check to see if a transaction is active.
     * 
     * @return true if a transaction is active
     */
    protected boolean isTransactionActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object register(Object subscriber) {
        if (subscriber != null) {
            final boolean registered = registeredObjects.add(subscriber);
            if (registered) {
                for (EventBus eventBus : googleEventBuses) {
                    eventBus.register(subscriber);
                }
            }

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                final String logMsg = (registered) ? "Registered subscriber of type ["
                        + subscriber.getClass().getName()
                        + "] with the event bus."
                        : "Ignoring request to register subscriber of type ["
                                + subscriber.getClass().getName()
                                + "] from the event bus, as it was already registered!";

                statusHandler.handle(Priority.DEBUG, logMsg);
            }
        } else {
            statusHandler.handle(Priority.WARN, NULL_SUBSCRIBER,
                    new IllegalArgumentException(NULL_SUBSCRIBER));
        }

        return subscriber;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregister(Object subscriber) {
        if (subscriber != null) {
            final boolean removed = registeredObjects.remove(subscriber);
            if (removed) {
                for (EventBus eventBus : googleEventBuses) {
                    eventBus.unregister(subscriber);
                }
            }

            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                final String logMsg = (removed) ? "Unregistered subscriber of type ["
                        + subscriber.getClass().getName()
                        + "] from the event bus."
                        : "Ignoring request to unregister subscriber of type ["
                                + subscriber.getClass().getName()
                                + "] from the event bus, as it was never registered!";

                statusHandler.handle(Priority.DEBUG, logMsg);
            }
        } else {
            statusHandler.handle(Priority.WARN, NULL_SUBSCRIBER,
                    new IllegalArgumentException(NULL_SUBSCRIBER));
        }
    }

    class EventTransactionSynchronization implements TransactionSynchronization {

        private final T eventObject;

        public EventTransactionSynchronization(T eventObject) {
            this.eventObject = eventObject;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void suspend() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void resume() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void flush() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeCommit(boolean readOnly) {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void beforeCompletion() {
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void afterCommit() {
        }

        @Override
        public void afterCompletion(int status) {
            if (status == TransactionSynchronization.STATUS_COMMITTED) {
                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    statusHandler.debug("Posting event of type ["
                            + eventObject.getClass().getName()
                            + "] on the event bus");
                }

                publishEvent(eventObject);

            } else if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    statusHandler.debug("Discarding event of type ["
                            + eventObject.getClass().getName()
                            + "] due to transaction rolling back.");
                }
            }
        }

    }

}
