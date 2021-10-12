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
package com.raytheon.uf.ignite.core;

import java.util.ArrayList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastore.ignite.IgniteUtils;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;

/**
 * Contains various constants and utilities used with ignite on the server side.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 21, 2021 8450       mapeters    Initial creation
 * Sep 23, 2021 8608       mapeters    Add {@link #sendMessageToQueue},
 *                                     moved from com.raytheon.uf.common.datastorage.ignite
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteServerUtils {

    private static final Logger logger = LoggerFactory
            .getLogger(IgniteServerUtils.class);

    private static final String CLUSTER_INDEX = "IGNITE_CLUSTER_INDEX";

    private static final long JMS_NUM_ATTEMPTS = IgniteUtils
            .getLongProperty("jms.message.send.num.attempts");

    private static final long JMS_RETRY_INTERVAL_MS = IgniteUtils
            .getLongProperty("jms.message.send.retry.interval.ms");

    /**
     * If this ignite cluster's index matches the given cluster index, add
     * clusterAdditions to baseList and return it, otherwise return just
     * baseList.
     *
     * This is intended to be used in spring.
     *
     * @param <T>
     *            the type of objects in the list
     * @param clusterIndex
     *            the cluster index to check
     * @param baseList
     *            the base list
     * @param clusterAdditions
     *            the list of values to add if the cluster matches
     * @return the combined list if the cluster index matches, otherwise
     *         baseList
     */
    public static <T> List<T> addToListIfClusterIndexMatches(int clusterIndex,
            List<T> baseList, List<? extends T> clusterAdditions) {
        List<T> rval = new ArrayList<>(baseList);
        if (Integer.toString(clusterIndex)
                .equals(System.getenv(CLUSTER_INDEX))) {
            rval.addAll(clusterAdditions);
        }
        return rval;
    }

    /**
     * Use the JMS connection factory to send the message to the queue endpoint
     * with the given URI.
     *
     * @param jmsConnectionFactory
     *            JMS connection factory to send the message with
     * @param uri
     *            queue endpoint URI
     * @param message
     *            message to send - must support dynamic serialize
     * @throws JMSException
     * @throws SerializationException
     */
    public static void sendMessageToQueue(
            ConnectionFactory jmsConnectionFactory, String uri, Object message)
            throws SerializationException {
        for (int attemptNum = 1; attemptNum <= JMS_NUM_ATTEMPTS; ++attemptNum) {
            /*
             * Suppressing squid:S2445. Using jmsConnectionFactory for
             * synchronization is correct because the below code block is only
             * not thread-safe if the same factory instance is in it multiple
             * times simultaneously.
             */
            synchronized (jmsConnectionFactory) { // NOSONAR
                /*
                 * Note that the connection factory is currently a
                 * CachingConnectionFactory that caches the connection/sessions
                 * and doesn't actually close them here
                 */
                try (Connection connection = jmsConnectionFactory
                        .createConnection();
                        Session session = connection.createSession()) {
                    Queue destinationQueue = session.createQueue(uri);
                    BytesMessage bytesMessage = session.createBytesMessage();
                    bytesMessage.writeBytes(
                            SerializationUtil.transformToThrift(message));
                    session.createProducer(destinationQueue).send(bytesMessage);
                    // Success, break
                    break;
                } catch (JMSException e) {
                    String msg = "Error sending message to " + uri
                            + " queue on attempt " + attemptNum + "/"
                            + JMS_NUM_ATTEMPTS + ": " + message;
                    logger.error(msg, e);
                    if (attemptNum < JMS_NUM_ATTEMPTS) {
                        try {
                            Thread.sleep(JMS_RETRY_INTERVAL_MS);
                        } catch (InterruptedException e1) {
                            logger.error(
                                    "Interrupted while waiting to re-send JMS message",
                                    e1);
                        }
                    }
                }
            }
        }
    }
}
