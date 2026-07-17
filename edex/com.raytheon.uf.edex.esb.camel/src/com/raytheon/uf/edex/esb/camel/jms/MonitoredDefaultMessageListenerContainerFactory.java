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
package com.raytheon.uf.edex.esb.camel.jms;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.camel.component.jms.DefaultJmsMessageListenerContainer;
import org.apache.camel.component.jms.JmsEndpoint;
import org.apache.camel.component.jms.MessageListenerContainerFactory;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Creates DefaultMessageListenerContainer instances that are then monitored
 * once a minute for paused tasks. If a paused task is found the container is
 * restarted. This is necessary in broker restart scenarios.
 *
 * This class also sets each container to have a very low receive timeout when
 * EDEX shuts down so that EDEX does not have to wait seconds before shutting
 * down the JMS consumer.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 8, 2014  2357       rjpeter     Initial creation.
 * Sep 17, 2024 2037700    tgurney     Lower the receive timeout at shutdown
 *                                     to make shutdown much faster.
 * </pre>
 *
 * @author rjpeter
 */
public class MonitoredDefaultMessageListenerContainerFactory
        implements MessageListenerContainerFactory {
    private static final AtomicInteger threadCount = new AtomicInteger(1);

    private final Collection<DefaultJmsMessageListenerContainer> containers = new ConcurrentLinkedQueue<>();

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MonitoredDefaultMessageListenerContainerFactory.class);

    private static final MonitoredDefaultMessageListenerContainerFactory instance = new MonitoredDefaultMessageListenerContainerFactory();

    public static MonitoredDefaultMessageListenerContainerFactory getInstance() {
        return instance;
    }

    private class MonitorTask implements Runnable {

        private void checkContainers() {
            for (DefaultMessageListenerContainer container : containers) {
                if (EDEXUtil.isShuttingDown()) {
                    return;
                } else if (container.getPausedTaskCount() > 0) {
                    StringBuilder msg = new StringBuilder(160);
                    msg.append("Container[")
                            .append(container.getDestinationName())
                            .append("] has paused tasks.  Container is ");
                    if (!container.isRunning()) {
                        msg.append("not ");
                    }
                    msg.append("running.  Container is ");
                    if (container.isActive()) {
                        msg.append("not ");
                    }
                    msg.append("active.  Restarting container.");
                    statusHandler.warn(msg.toString());
                    container.start();
                }
            }
        }

        @Override
        public void run() {
            try {
                EDEXUtil.waitForRunning();
                while (true) {
                    try {
                        try {
                            /*
                             * go to sleep for a minute, wake up immediately if
                             * EDEX begins shutting down so that shutdown work
                             * can be completed
                             */
                            EDEXUtil.awaitShutdown(1, TimeUnit.MINUTES);
                        } catch (InterruptedException e) {
                            // stop waiting
                        }
                        if (EDEXUtil.isShuttingDown()) {
                            break;
                        }
                        checkContainers();
                    } catch (Throwable e) {
                        statusHandler.error(
                                "Error occurred in checking message listener containers",
                                e);
                    }
                }
            } finally {
                for (DefaultMessageListenerContainer container : containers) {
                    /*
                     * This speeds up EDEX shutdown by a lot.
                     *
                     * We have to set this here at this relatively low level,
                     * rather than at the level of Camel JmsEndpoints, because
                     * the receiveTimeout configured on the endpoint is used
                     * only when the endpoint is created and is not referred to
                     * after that.
                     *
                     * The value of 50 ms was chosen for no particular reason
                     * other than it is fast, plus a vague intuition that going
                     * fully non-blocking (argument of -1) might carry some risk
                     * of dropping messages.
                     */
                    container.setReceiveTimeout(50);
                }
            }
        }
    };

    private MonitoredDefaultMessageListenerContainerFactory() {
        Thread containerChecker = new Thread(new MonitorTask(),
                "MessageListenerContainerMonitor-"
                        + threadCount.getAndIncrement());
        containerChecker.start();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.camel.component.jms.MessageListenerContainerFactory#
     * createMessageListenerContainer
     * (org.apache.camel.component.jms.JmsEndpoint)
     */
    @Override
    public AbstractMessageListenerContainer createMessageListenerContainer(
            JmsEndpoint endpoint) {
        // track the container for monitoring in the case of a provider
        // reconnect
        DefaultJmsMessageListenerContainer container = new DefaultJmsMessageListenerContainer(
                endpoint);
        containers.add(container);
        return container;
    }

}
