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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.naming.ConfigurationException;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.Route;
import org.apache.camel.spi.InterceptStrategy;

import com.raytheon.uf.common.message.IMessage;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.uf.common.util.collections.BoundedMap;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.core.IMessageProducer;
import com.raytheon.uf.edex.esb.camel.context.ContextData;
import com.raytheon.uf.edex.esb.camel.context.ContextManager;

/**
 * Sends message to endpoints programmatically. Implements the camel
 * {@link InterceptStrategy} to allow for tracking of camel dependencies where
 * possible so that the ProducerTemplate is created from the correct context.
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
 *
 * </pre>
 *
 * @author njensen
 */

public class MessageProducer implements IMessageProducer {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(MessageProducer.class);

    private static final String URI_CACHE_SIZE_PROPERTY = "MessageProducer.uriCacheSize";

    private static final int URI_CACHE_SIZE = Integer
            .getInteger(URI_CACHE_SIZE_PROPERTY, 256);

    private final ConcurrentMap<CamelContext, ProducerTemplate> contextProducerMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<CamelContext, Map<String, Endpoint>> contextUriEndpointMap = new ConcurrentHashMap<>();

    /**
     * List of messages waiting to be sent.
     */
    private final List<WaitingMessage> waitingMessages = new LinkedList<>();

    /**
     * Internal variable for tracking if messages should be queued or not.
     */
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

    /**
     * Returns the ContextData
     *
     * @return
     * @throws EdexException
     */
    protected ContextData getContextData() throws EdexException {
        try {
            return ContextManager.getInstance().getContextData();
        } catch (ConfigurationException e) {
            throw new EdexException("Unable to look up camel context data", e);
        }
    }

    @Override
    public void sendAsync(String endpoint, Object message)
            throws EdexException {
        if (!started
                && queueWaitingMessage(WaitingType.ID, endpoint, message)) {
            return;
        }

        String uri = getContextData().getEndpointUriForRouteId(endpoint);
        sendAsyncUri(uri, message);
    }

    @Override
    public void sendAsyncUri(String uri, Object message) throws EdexException {
        if (!started && queueWaitingMessage(WaitingType.URI, uri, message)) {
            return;
        }

        try {
            Pair<ProducerTemplate, Endpoint> ctxAndTemplate = getProducerTemplateAndEndpointForUri(
                    uri);
            Map<String, Object> headers = getHeaders(message);
            ProducerTemplate template = ctxAndTemplate.getFirst();
            Endpoint ep = ctxAndTemplate.getSecond();

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
            Pair<ProducerTemplate, Endpoint> ctxAndTemplate = getProducerTemplateAndEndpointForUri(
                    uri);
            ProducerTemplate template = ctxAndTemplate.getFirst();
            Endpoint ep = ctxAndTemplate.getSecond();

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
            Pair<ProducerTemplate, Endpoint> ctxAndTemplate = getProducerTemplateAndEndpointForUri(
                    uri);
            Map<String, Object> headers = getHeaders(message);
            ProducerTemplate template = ctxAndTemplate.getFirst();
            Endpoint ep = ctxAndTemplate.getSecond();

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

        String uri = getContextData().getEndpointUriForRouteId(endpoint);

        try {
            Pair<ProducerTemplate, Endpoint> ctxAndTemplate = getProducerTemplateAndEndpointForUri(
                    uri);
            Map<String, Object> headers = getHeaders(message);
            ProducerTemplate template = ctxAndTemplate.getFirst();
            Endpoint ep = ctxAndTemplate.getSecond();

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
     * Returns the a producer template for the CamelContext of the uri. If the
     * uri is not registered in this jvm, will use the first context available.
     *
     * @return
     */
    protected Pair<ProducerTemplate, Endpoint> getProducerTemplateAndEndpointForUri(
            String uri) throws ConfigurationException, EdexException {
        CamelContext ctx = null;
        ContextData contextData = getContextData();
        Pair<String, String> typeAndName = ContextData
                .getEndpointTypeAndName(uri);
        if (typeAndName != null) {
            Route route = contextData
                    .getRouteForEndpointName(typeAndName.getSecond());
            if (route != null) {
                ctx = route.getCamelContext();
            }
        }

        if (ctx == null) {
            // this jvm does not consume from this route, use default context
            ctx = contextData.getDefaultContext();
        }

        if (ctx != null) {
            ProducerTemplate tmp = contextProducerMap.get(ctx);
            if (tmp == null) {
                tmp = ctx.createProducerTemplate();
                ProducerTemplate prev = contextProducerMap.putIfAbsent(ctx,
                        tmp);
                if ((prev != null) && (prev != tmp)) {
                    try {
                        tmp.stop();
                    } catch (Exception e) {
                        statusHandler.error(
                                "Error occurred stopping temporary ProducerTemplate. Consider synchronizing producer creation.",
                                e);
                    }
                    tmp = prev;
                }
            }

            /*
             * Caching endpoint for the uri ourselves. Camel considers various
             * endpoints non singleton. So for things like jms-topic, a new
             * endpoint is created every time a message is sent to the URI
             * instead of reusing one that was already created. This is in part
             * due to the lack of tracking per route. We are ok with caching per
             * context as we don't operate on routes individually only contexts
             * as a whole.
             */
            Map<String, Endpoint> endpointMap = contextUriEndpointMap.get(ctx);
            if (endpointMap == null) {
                /*
                 * Use bounded map to prevent leaking cached endpoints. If
                 * mapping size is an issue, we may need to consider using just
                 * the base part of the URI as the key
                 */
                endpointMap = new BoundedMap<>(URI_CACHE_SIZE);
                Map<String, Endpoint> prev = contextUriEndpointMap
                        .putIfAbsent(ctx, endpointMap);
                if (prev != null) {
                    endpointMap = prev;
                }
            }

            Endpoint ep = null;
            synchronized (endpointMap) {
                ep = endpointMap.get(uri);
                if (ep == null) {
                    if (endpointMap.size() == URI_CACHE_SIZE) {
                        statusHandler.error(
                                "Context URI mapping has exceeded number of URIs limit ["
                                        + URI_CACHE_SIZE
                                        + "]. Possible Endpoint leak in Camel Context. Consider increasing System property ["
                                        + URI_CACHE_SIZE_PROPERTY + "]");
                    }

                    ContextManager.getInstance().clearDependencyMapping();
                    ep = ctx.getEndpoint(uri);
                    endpointMap.put(uri, ep);
                }
            }

            return new Pair<>(tmp, ep);
        }

        throw new ConfigurationException(
                "Could not find a CamelContext for routing to uri [" + uri
                        + "].  Check loaded spring configurations.");
    }

    private Map<String, Object> getHeaders(Object message) {
        Map<String, Object> headers = null;
        if (message instanceof IMessage) {
            headers = new HashMap<>();
            headers.put("JMSType", message.getClass().getName());
            headers.putAll(((IMessage) message).getHeaders());
        } else if (message instanceof List) {
            List<?> list = ((List<?>) message);
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
