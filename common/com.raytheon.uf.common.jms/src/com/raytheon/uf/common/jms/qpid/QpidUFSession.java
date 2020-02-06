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

import java.io.Serializable;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Wrapper over a JMS session. Adds Qpid-specific logic for queue creation
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 15, 2019 7724       tgurney     Initial creation
 * Oct 22, 2019 7724       tgurney     Fix topic creation. Change broker REST
 *                                     API
 * Oct 23, 2019 7724       tgurney     Log producer creation
 * Jan 16, 2020 8008       randerso    Move topic prefix to QpidUFSession
 *
 * </pre>
 *
 * @author tgurney
 */

public class QpidUFSession implements Session {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(QpidUFSession.class);

    private Session session;

    private IBrokerRestProvider jmsAdmin;

    public QpidUFSession(Session session, IBrokerRestProvider jmsAdmin) {
        this.session = session;
        this.jmsAdmin = jmsAdmin;
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return session.createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        return session.createMapMessage();
    }

    @Override
    public Message createMessage() throws JMSException {
        return session.createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return session.createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object)
            throws JMSException {
        return session.createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        return session.createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return session.createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        return session.createTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return session.getTransacted();
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        return session.getAcknowledgeMode();
    }

    @Override
    public void commit() throws JMSException {
        session.commit();
    }

    @Override
    public void rollback() throws JMSException {
        session.rollback();
    }

    @Override
    public void close() throws JMSException {
        session.close();
    }

    @Override
    public void recover() throws JMSException {
        session.recover();
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return session.getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener listener)
            throws JMSException {
        session.setMessageListener(listener);
    }

    @Override
    public void run() {
        session.run();
    }

    @Override
    public MessageProducer createProducer(Destination destination)
            throws JMSException {
        MessageProducer producer = session.createProducer(destination);
        statusHandler.info("Created producer " + destination);
        return producer;
    }

    @Override
    public MessageConsumer createConsumer(Destination destination)
            throws JMSException {
        MessageConsumer consumer = session.createConsumer(destination);
        statusHandler.info("Created consumer " + destination);
        return consumer;
    }

    @Override
    public MessageConsumer createConsumer(Destination destination,
            String messageSelector) throws JMSException {
        MessageConsumer consumer = session.createConsumer(destination,
                messageSelector);
        statusHandler.info("Created consumer " + destination);
        return consumer;
    }

    @Override
    public MessageConsumer createConsumer(Destination destination,
            String messageSelector, boolean noLocal) throws JMSException {
        MessageConsumer consumer = session.createConsumer(destination,
                messageSelector, noLocal);
        statusHandler.info("Created consumer " + destination);
        return consumer;
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        try {
            /*
             * TODO Dynamically creating queues in Qpid Broker is problematic
             * now that it can only be done via the REST API. Consider
             * alternative solutions, e.g. moving this call somewhere else, or
             * entirely doing away with dynamically creating queues.
             */
            jmsAdmin.createQueue(queueName);
        } catch (CommunicationException e) {
            statusHandler.error(
                    "An error occurred while creating the queue " + queueName,
                    e);
        }
        return session.createQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        return session.createTopic("amq.topic/" + topicName);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name)
            throws JMSException {
        return session.createDurableSubscriber(topic, name);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name,
            String messageSelector, boolean noLocal) throws JMSException {
        return session.createDurableSubscriber(topic, name, messageSelector,
                noLocal);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return session.createBrowser(queue);
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector)
            throws JMSException {
        return session.createBrowser(queue, messageSelector);
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return session.createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return session.createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        session.unsubscribe(name);
    }

    @Override
    public MessageConsumer createSharedConsumer(Topic topic,
            String sharedSubscriptionName) throws JMSException {
        return session.createSharedConsumer(topic, sharedSubscriptionName);
    }

    @Override
    public MessageConsumer createSharedConsumer(Topic topic,
            String sharedSubscriptionName, String messageSelector)
            throws JMSException {
        return session.createSharedConsumer(topic, sharedSubscriptionName,
                messageSelector);
    }

    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name)
            throws JMSException {
        return session.createDurableConsumer(topic, name);
    }

    @Override
    public MessageConsumer createDurableConsumer(Topic topic, String name,
            String messageSelector, boolean noLocal) throws JMSException {
        return session.createDurableConsumer(topic, name, messageSelector,
                noLocal);
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name)
            throws JMSException {
        return session.createSharedDurableConsumer(topic, name);
    }

    @Override
    public MessageConsumer createSharedDurableConsumer(Topic topic, String name,
            String messageSelector) throws JMSException {
        return session.createSharedDurableConsumer(topic, name,
                messageSelector);
    }
}
