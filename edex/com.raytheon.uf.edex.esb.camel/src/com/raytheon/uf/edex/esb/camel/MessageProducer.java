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
package com.raytheon.uf.edex.esb.camel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.naming.ConfigurationException;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;

import com.raytheon.uf.common.message.IMessage;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.core.IMessageProducer;
import com.raytheon.uf.edex.esb.camel.context.ContextManager;

/**
 * Sends messages to endpoints programmatically.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ---------   --------------------------------------------
 * Nov 14, 2008           njensen     Initial creation.
 * Mar 27, 2014  2726     rjpeter     Modified for graceful shutdown changes,
 *                                    added tracking of endpoints by context.
 * Oct 08, 2014  3684     randerso    Added sendAsyncThriftUri
 * Jul 28, 2017  5570     rjpeter     Fix dependency generation on shutdown
 * Jan 24, 2019  7714     mrichardson Added overloaded sendAsyncUri
 * Mar  4, 2021  8326     tgurney     Fixes for Camel 3 API changes
 * May 12, 2021  8436     tgurney     Change CamelContext detection -- always
 *                                    get the context of the endpoint uri
 * Jun 28, 2022  8865     mapeters    Change determination of default context
 *                                    to use when sending outside JVM
 * Jul 29, 2024  2037700  tgurney     Replace ContextData with shim interface.
 * Sep 26, 2024  2037700  tgurney     Delete unneeded code after Camel 4 upgrade
 *
 * </pre>
 *
 * @author njensen
 */

public class MessageProducer implements IMessageProducer {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MessageProducer.class);

    /**
     * Holds a single producer template to be used throughout EDEX. Creating one
     * producer template and keeping it around is the pattern recommended by
     * Camel documentation:
     * https://camel.apache.org/manual/faq/why-does-camel-use-too-many-threads-with-producertemplate.html
     */
    private final AtomicReference<ProducerTemplate> producerTemplate = new AtomicReference<>();

    /** List of messages waiting to be sent. */
    private final List<WaitingMessage> waitingMessages = new LinkedList<>();

    /** Internal variable for tracking if messages should be queued or not. */
    private volatile boolean started = false;

    /**
     * Constructor that launches an internal thread that will send all async
     * messages that queue up while camel starts up.
     */
    public MessageProducer() {
        Thread t = new Thread() {
            @Override
            public void run() {
                EDEXUtil.waitForRunning();
                started = true;
                sendPendingAsyncMessages();
            }
        };
        t.setName("MessageProducer-pendingMessageSender");
        t.start();
    }

    @Override
    public void sendAsync(String endpoint, Object message)
            throws EdexException {
        if (!started
                && queueWaitingMessage(WaitingType.ID, endpoint, message)) {
            return;
        }

        String uri = ContextManager.getInstance()
                .getEndpointUriForRouteId(endpoint);
        sendAsyncUri(uri, message);
    }

    @Override
    public void sendAsyncUri(String uri, Object message) throws EdexException {
        if (!started && queueWaitingMessage(WaitingType.URI, uri, message)) {
            return;
        }

        try {
            Map<String, Object> headers = getHeaders(message);
            Endpoint ep = getEndpointForUri(uri);
            ProducerTemplate template = getProducerTemplate();

            if (headers != null) {
                template.sendBodyAndHeaders(ep, ExchangePattern.InOnly, message,
                        headers);
            } else {
                template.sendBody(ep, ExchangePattern.InOnly, message);
            }
        } catch (Exception e) {
            throw new EdexException("Error sending asynchronous message: "
                    + message + " to uri: " + uri, e);
        }
    }

    @Override
    public void sendAsyncUri(String uri, Object body,
            Map<String, Object> headers) throws EdexException {
        if (!started && queueWaitingMessage(WaitingType.URI, uri, body)) {
            return;
        }

        try {
            Endpoint ep = getEndpointForUri(uri);
            ProducerTemplate template = getProducerTemplate();

            if (headers != null) {
                template.sendBodyAndHeaders(ep, ExchangePattern.InOnly, body,
                        headers);
            } else {
                template.sendBody(ep, ExchangePattern.InOnly, body);
            }
        } catch (Exception e) {
            throw new EdexException("Error sending asynchronous message: "
                    + body + " to uri: " + uri, e);
        }
    }

    @Override
    public void sendAsyncThriftUri(String uri, Object message)
            throws EdexException, SerializationException {
        if (!started
                && queueWaitingMessage(WaitingType.THRIFT_URI, uri, message)) {
            return;
        }

        try {
            Map<String, Object> headers = getHeaders(message);
            Endpoint ep = getEndpointForUri(uri);
            ProducerTemplate template = getProducerTemplate();

            if (headers != null) {
                template.sendBodyAndHeaders(ep, ExchangePattern.InOnly,
                        SerializationUtil.transformToThrift(message), headers);
            } else {
                template.sendBody(ep, ExchangePattern.InOnly,
                        SerializationUtil.transformToThrift(message));
            }
        } catch (Exception e) {
            throw new EdexException("Error sending asynchronous message: "
                    + message + " to uri: " + uri, e);
        }
    }

    @Override
    public Object sendSync(String endpoint, Object message)
            throws EdexException {
        if (!started) {
            throw new EdexException("Cannot send synchronous message to "
                    + endpoint + " before EDEX has started");
        }

        String uri = ContextManager.getInstance()
                .getEndpointUriForRouteId(endpoint);

        try {
            Map<String, Object> headers = getHeaders(message);
            Endpoint ep = getEndpointForUri(uri);
            ProducerTemplate template = getProducerTemplate();

            if (headers != null) {
                return template.sendBodyAndHeaders(ep, ExchangePattern.InOut,
                        message, headers);
            } else {
                return template.sendBody(ep, ExchangePattern.InOut, message);
            }
        } catch (Exception e) {
            throw new EdexException("Error sending synchronous message: "
                    + message + " to uri: " + uri, e);
        }
    }

    /**
     * Queues up an async message for sending to an endpoint.
     *
     * @param type
     * @param endpoint
     * @param message
     * @return
     */
    private boolean queueWaitingMessage(WaitingType type, String endpoint,
            Object message) {
        synchronized (waitingMessages) {
            // make sure container hasn't started while waiting
            if (!started) {
                WaitingMessage wm = new WaitingMessage();
                wm.type = type;
                wm.dest = endpoint;
                wm.msg = message;
                waitingMessages.add(wm);
                return true;
            }

            return false;
        }
    }

    /**
     * @return the global producer template, creating it if it does not exist.
     */
    private ProducerTemplate getProducerTemplate() {
        ProducerTemplate template = producerTemplate.get();
        if (template == null) {
            CamelContext ctx = ContextManager.getInstance().getCamelContext();
            ProducerTemplate newTemplate = ctx.createProducerTemplate();
            if (producerTemplate.compareAndSet(null, newTemplate)) {
                template = newTemplate;
            } else {
                /* someone else created it first */
                newTemplate.stop();
                template = producerTemplate.get();
            }
        }
        return template;
    }

    private Endpoint getEndpointForUri(String uri)
            throws ConfigurationException, EdexException {
        /*
         * Originally the endpoints retrieved via CamelContext.getEndpoint were
         * cached in a map because some frequently used endpoint types (such as
         * JMS topics) were "non-singleton" and so Camel would create a new
         * Endpoint every time getEndpoint was called. Since Camel 3 this is not
         * the case anymore: https://issues.apache.org/jira/browse/CAMEL-10911
         */
        CamelContext ctx = ContextManager.getInstance().getCamelContext();
        return ctx.getEndpoint(uri);
    }

    private Map<String, Object> getHeaders(Object message) {
        Map<String, Object> headers = null;
        if (message instanceof IMessage) {
            headers = new HashMap<>();
            headers.put("JMSType", message.getClass().getName());
            headers.putAll(((IMessage) message).getHeaders());
        } else if (message instanceof List) {
            List<?> list = (List<?>) message;
            if (!list.isEmpty()) {
                if (list.get(0) instanceof IMessage) {
                    headers = ((IMessage) list.get(0)).getHeaders();
                }
            }
        }
        return headers;
    }

    /**
     * Sends any messages that were queued up while Camel started.
     */
    protected void sendPendingAsyncMessages() {
        synchronized (waitingMessages) {
            for (WaitingMessage wm : waitingMessages) {
                try {
                    switch (wm.type) {
                    case ID:
                        sendAsync(wm.dest, wm.msg);
                        break;
                    case URI:
                        sendAsyncUri(wm.dest, wm.msg);
                        break;
                    case THRIFT_URI:
                        sendAsyncThriftUri(wm.dest, wm.msg);
                        break;
                    }
                } catch (Exception e) {
                    statusHandler.error(
                            "Error occurred sending startup delayed async message",
                            e);
                }
            }
        }
    }

    /**
     * Enum for handling whether the waiting type was uri or msg.
     */
    private enum WaitingType {
        ID, URI, THRIFT_URI
    }

    /**
     * Inner class for handling messages sent before edex is up.
     */
    private class WaitingMessage {
        private WaitingType type;

        private String dest;

        private Object msg;
    }
}
