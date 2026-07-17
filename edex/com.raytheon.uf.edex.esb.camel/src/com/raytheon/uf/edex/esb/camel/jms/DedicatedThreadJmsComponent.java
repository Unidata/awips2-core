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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.component.jms.JmsEndpoint;

import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;
import com.raytheon.uf.edex.esb.camel.spring.JmsThreadPoolTaskExecutor;

/**
 * Custom JMS component that makes dedicated thread pools for each JmsEndpoint
 * based on the concurrent consumers needed. Each pool is named based on the JMS
 * endpoint. Each endpoint also overrides the message listener container factory
 * to monitor the created containers to see if they need to be restarted in a
 * disconnect scenario.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 07, 2014 2357       rjpeter     Initial creation.
 * Sep 15, 2024 2037700    tgurney     Remove pool size cap for topics, to
 *                                     accomodate multiple consumers on same
 *                                     topic same JVM
 * Jun 01, 2026 2042123    mapeters    Set deserialization filter
 * </pre>
 *
 * @author rjpeter
 */
public class DedicatedThreadJmsComponent extends JmsComponent {

    public DedicatedThreadJmsComponent(
            org.apache.camel.component.jms.JmsConfiguration jmsconfig) {
        super(jmsconfig);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining,
            Map<String, Object> parameters) throws Exception {
        String threadName = (String) parameters.remove("threadName");
        JmsEndpoint jmsE = (JmsEndpoint) super.createEndpoint(uri, remaining,
                parameters);
        if (threadName != null && threadName.length() > 0
                && !threadName.endsWith("-")) {
            threadName += "-";
        } else {
            threadName = jmsE.getDestinationName() + "-";
        }
        JmsThreadPoolTaskExecutor executor = new JmsThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(threadName);

        String endpointName = EDEXRouteContext.getEndpointTypeAndName(uri)
                .getSecond();
        if (!endpointName.startsWith("topic:")) {
            /*
             * The concurrentConsumers parameter only makes sense for queues.
             * The purpose is to allow multiple worker threads to process queue
             * items in parallel, in which case we want to cap the number of
             * threads and not allow an explosion of threads if there are many
             * messages in queue.
             *
             * This limits the maximum number of consumers for the given JMS
             * endpoint throughout the entire CamelContext (that is, the entire
             * JVM). This is okay for queues because it doesn't make sense to
             * have more than one route within the CamelContext consuming from a
             * given queue.
             *
             * For topics it doesn't make sense to do this. The JmsEndpoint
             * object is shared among all routes in the CamelContext that
             * consume from it, and it's a publish/subscribe mechanism so we
             * want to allow an arbitrary number of different routes to be
             * subscribed to the same topic at once.
             */
            executor.setCorePoolSize(jmsE.getConcurrentConsumers());
            executor.setMaxPoolSize(Math.max(jmsE.getConcurrentConsumers(),
                    jmsE.getMaxConcurrentConsumers()));
        }
        executor.setQueueCapacity(0);

        jmsE.setTaskExecutor(executor);
        jmsE.setMessageListenerContainerFactory(
                MonitoredDefaultMessageListenerContainerFactory.getInstance());

        /*
         * Add com.raytheon (needed for GribDecodeMessage) and jakarta/gov.noaa
         * (unknown if needed) to camel's default filter
         * (JmsBinding.DEFAULT_DESERIALIZATION_FILTER).
         */
        jmsE.getConfiguration().setDeserializationFilter(
                "java.**;javax.**;jakarta.**;org.apache.camel.**;com.raytheon.**;gov.noaa.**;!*");

        return jmsE;
    }
}
