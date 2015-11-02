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

package com.raytheon.uf.common.comm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthProtocolState;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import com.raytheon.uf.common.comm.stream.DynamicSerializeEntity;
import com.raytheon.uf.common.comm.stream.DynamicSerializeStreamHandler;
import com.raytheon.uf.common.comm.stream.OStreamEntity;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

/**
 * 
 * Provides connectivity to HTTP services
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date          Ticket#     Engineer    Description
 *    ------------  ----------  ----------- --------------------------
 *    7/1/06        #1088       chammack    Initial Creation.
 *    5/17/10       #5901       njensen     Moved to common
 *    03/02/11      #8045       rferrel     Add connect reestablished message.
 *    07/17/12      #911        njensen     Refactored significantly
 *    08/09/12      15307       snaples     Added putEntitiy in postStreamingEntity.
 *    01/07/13      DR 15294    D. Friedman Added streaming requests.
 *    Jan 24, 2013  1526        njensen     Added postDynamicSerialize()
 *    Feb 20, 2013  1628        bsteffen    clean up Inflaters used by
 *                                           HttpClient.
 *    Mar 11, 2013  1786        mpduff      Add https capability.
 *    Jun 12, 2013  2102        njensen     Better error handling when using
 *                                           DynamicSerializeStreamHandler
 *    Feb 04, 2014  2704        njensen     Better error message with bad address
 *                                           Https authentication failures notify handler
 *    Feb 17, 2014  2756        bclement    added content type to response object
 *    Feb 28, 2014  2756        bclement    added isSuccess() and isNotExists() to response
 *    6/18/2014     1712        bphillip    Updated Proxy configuration
 *    Aug 15, 2014  3524        njensen     Pass auth credentials on every https request
 *    Aug 20, 2014  3549        njensen     Removed cookie interceptors
 *    Aug 29, 2014  3570        bclement    refactored to configuration builder model using 4.3 API
 *    Nov 15, 2014  3757        dhladky     General HTTPS handler
 *    Jan 07, 2015  3952        bclement    reset auth state on authentication failure
 *    Jan 23, 2015  3952        njensen     Ensure https contexts are thread safe
 *    Feb 17, 2015  3978        njensen     Added executeRequest(HttpUriRequest, IStreamHandler)
 *    Apr 16, 2015  4239        njensen     Better error handling on response != 200
 *    Oct 30, 2015  4710        bclement    ByteArrayOutputStream renamed to PooledByteArrayOutputStream
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class HttpClient {

    public static class HttpClientResponse {
        public final int code;

        public final byte[] data;

        public final String contentType;

        /*
         * TODO contemplate including headers in response object
         */

        private HttpClientResponse(int code, byte[] data, String contentType) {
            this.code = code;
            this.data = data != null ? data : new byte[0];
            this.contentType = contentType;
        }

        /**
         * @return true if code is a 200 level return code
         */
        public boolean isSuccess() {
            return HttpClient.isSuccess(code);
        }

        /**
         * @return true if resource does not exist on server
         */
        public boolean isNotExists() {
            return code == 404 || code == 410;
        }
    }

    private static final String HTTPS = "https";

    private static final String WWW_AUTHENTICATE = "WWW-Authenticate";

    private boolean previousConnectionFailed;

    private static volatile HttpClient instance;

    /**
     * Number of times to retry in the event of a connection exception. Default
     * is 1.
     */
    private int retryCount = 1;

    private static final IUFStatusHandler statusHandler = UFStatus.getHandler(
            HttpClient.class, "DEFAULT");

    private final NetworkStatistics stats = new NetworkStatistics();

    private boolean gzipRequests = false;

    /** number of requests currently in process by the application per host */
    private final Map<String, AtomicInteger> currentRequestsCount = new ConcurrentHashMap<String, AtomicInteger>();

    private volatile CloseableHttpClient sslClient;

    private volatile CloseableHttpClient client;

    private final HttpClientConfig config;

    /**
     * The credentials provider is for https requests only and ensures that a
     * user does not have to enter username/password authentication more than
     * once per application startup.
     */
    private CredentialsProvider credentialsProvider;

    private final ThreadLocal<HttpClientContext> httpsContext = new ThreadLocal<HttpClientContext>() {
        @Override
        protected HttpClientContext initialValue() {
            return HttpClientContext.create();
        }
    };

    /**
     * Checks if the http status code is considered a success
     * 
     * @param statusCode
     * @return
     */
    private static boolean isSuccess(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Public constructor.
     * 
     * @param config
     */
    public HttpClient(HttpClientConfig config) {
        this.config = config;
    }

    /**
     * @return cached SSL client
     */
    private org.apache.http.client.HttpClient getHttpsInstance() {
        if (sslClient == null) {
            synchronized (this) {
                if (sslClient == null) {
                    if (config.getHttpsHandler() == null) {
                        throw new ExceptionInInitializerError(
                                "Https configuration required.");
                    }
                    try {
                        sslClient = ApacheHttpClientCreator.createSslClient(
                                config, stats);
                    } catch (Exception e) {
                        String msg = "Error setting up SSL Client: "
                                + e.getLocalizedMessage();
                        statusHandler.handle(Priority.PROBLEM, msg, e);
                        throw new ExceptionInInitializerError(msg);
                    }
                }
            }
        }
        return sslClient;
    }

    /**
     * @return cached client used for non-https requests
     */
    private org.apache.http.client.HttpClient getHttpInstance() {
        if (client == null) {
            synchronized (this) {
                if (client == null) {
                    try {
                        client = ApacheHttpClientCreator.createClient(config,
                                stats);
                    } catch (Exception e) {
                        String msg = "Error setting up HTTP Client: "
                                + e.getLocalizedMessage();
                        statusHandler.handle(Priority.PROBLEM, msg, e);
                        throw new ExceptionInInitializerError(msg);
                    }
                }
            }
        }
        return client;
    }

    /**
     * Sets whether or not to compress the outgoing requests to reduce bandwidth
     * sent by the client.
     * 
     * @param compress
     */
    public void setCompressRequests(boolean compress) {
        this.gzipRequests = compress;
    }

    /**
     * Get global instance of this class
     * 
     * @return instance
     */
    public static final HttpClient getInstance() {
        if (instance == null) {
            synchronized (HttpClient.class) {
                if (instance == null) {
                    HttpClientConfig config = HttpClientConfigBuilder
                            .defaultConfig();
                    instance = new HttpClient(config);
                }
            }
        }
        return instance;
    }

    /**
     * Set configuration for global HTTP client instance. Overrides any previous
     * configuration. This should only be called by top level configuration code
     * at startup.
     * 
     * @param config
     * @return
     */
    public static final HttpClient configureGlobalInstance(
            HttpClientConfig config) {
        synchronized (HttpClient.class) {
            if (instance != null
                    && (instance.sslClient != null || instance.client != null)) {
                /*
                 * This indicates that the startup configuration order is wrong
                 * or that configureGlobalInstance() is being called after
                 * startup and the issue should be investigated by a developer.
                 */
                statusHandler.warn("HTTP Client global instance was "
                        + " used before global configuration was set.");
            }
            instance = new HttpClient(config);
        }
        return instance;
    }

    /**
     * Post a message to an http address, and return the result as a string.
     * 
     * 
     * @param address
     * @param message
     * @return
     * @throws Exception
     */
    public String post(String address, String message) throws Exception {
        String returnValue = new String(postByteResult(address, message));
        return returnValue;
    }

    /**
     * Post a message to an http address, and return the result as a byte array.
     * 
     * 
     * @param address
     * @param message
     * @return
     * @throws Exception
     */
    public byte[] postByteResult(String address, String message)
            throws Exception {
        HttpPost put = new HttpPost(address);
        put.setEntity(new StringEntity(message, ContentType.TEXT_XML));

        return executePostMethod(put);
    }

    /**
     * Sends the request to the server, checks the status code (in case of 404,
     * 403, etc), and returns the response if there was no error code.
     * 
     * @param put
     *            the request to send
     * @return the response from the server
     * @throws IOException
     * @throws CommunicationException
     */
    private HttpResponse postRequest(HttpUriRequest put) throws IOException,
            CommunicationException {
        HttpResponse resp = null;
        if (put.getURI().getScheme().equalsIgnoreCase(HTTPS)) {
            IHttpsHandler handler = config.getHttpsHandler();
            org.apache.http.client.HttpClient client = getHttpsInstance();
            URI uri = put.getURI();
            String host = uri.getHost();
            int port = uri.getPort();
            HttpClientContext context = getHttpsContext(host, port);
            resp = client.execute(put, context);

            // Check for not authorized, 401
            while (resp.getStatusLine().getStatusCode() == 401) {
                String authValue = null;
                if (resp.containsHeader(WWW_AUTHENTICATE)) {
                    authValue = resp.getFirstHeader(WWW_AUTHENTICATE)
                            .getValue();
                }

                String[] credentials = null;
                if (handler != null) {
                    credentials = handler.getCredentials(host, port, authValue);
                }
                if (credentials == null) {
                    return resp;
                }
                this.setupCredentials(host, port, credentials[0],
                        credentials[1]);
                context = getHttpsContext(host, port);
                /*
                 * The context auth state gets set to FAILED on a 401 which
                 * causes any future requests to abort prematurely. Therefore we
                 * set it to unchallenged so it will try again with new
                 * credentials.
                 */
                AuthState targetAuthState = context.getTargetAuthState();
                targetAuthState.setState(AuthProtocolState.UNCHALLENGED);
                try {
                    resp = client.execute(put, context);
                } catch (Exception e) {
                    statusHandler.handle(Priority.ERROR,
                            "Error retrying http request", e);
                    return resp;
                }

                if (resp.getStatusLine().getStatusCode() == 401) {
                    // obtained credentials and they failed!
                    if (handler != null) {
                        handler.credentialsFailed();
                    }
                }

            }
        } else {
            resp = getHttpInstance().execute(put);
        }

        if (previousConnectionFailed) {
            previousConnectionFailed = false;
            statusHandler.handle(Priority.INFO,
                    "Connection with server reestablished.");
        }
        return resp;
    }

    /**
     * Posts the request to the server and passes the response stream to the
     * handler callback. Will also retry the request if it fails due to a
     * timeout or IO problem.
     * 
     * @param put
     *            the request to post
     * @param handlerCallback
     *            the handler to handle the response stream
     * @return the http status code
     * @throws CommunicationException
     */
    private HttpClientResponse process(HttpUriRequest put,
            IStreamHandler handlerCallback) throws CommunicationException {
        int tries = 0;
        boolean retry = true;
        HttpResponse resp = null;
        AtomicInteger ongoing = null;

        try {
            String host = put.getURI().getHost();
            if (host == null) {
                throw new InvalidURIException("Invalid URI: "
                        + put.getURI().toString());
            }
            ongoing = currentRequestsCount.get(host);
            if (ongoing == null) {
                ongoing = new AtomicInteger();
                currentRequestsCount.put(host, ongoing);
            }
            int currentCount = ongoing.incrementAndGet();
            if (currentCount > getMaxConnectionsPerHost()) {
                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    String msg = currentCount
                            + " ongoing http requests to "
                            + host
                            + ".  Likely waiting for free connection from pool.";
                    statusHandler.debug(msg);
                }
            }
            while (retry) {
                retry = false;
                tries++;

                String errorMsg = null;
                Exception exc = null;
                try {
                    resp = postRequest(put);
                } catch (ConnectionPoolTimeoutException e) {
                    errorMsg = "Timed out waiting for http connection from pool: "
                            + e.getMessage();
                    errorMsg += ".  Currently " + ongoing.get()
                            + " requests ongoing";
                    exc = e;
                } catch (SSLPeerUnverifiedException e) {
                    errorMsg = "Problem with security certificates.\nCannot make a secure connection.\nContact server administrator";
                    throw new CommunicationException(errorMsg, e);
                } catch (IOException e) {
                    errorMsg = "Error occurred communicating with server: "
                            + e.getMessage();
                    exc = e;
                }

                if ((errorMsg != null) && (exc != null)) {
                    if (tries > retryCount) {
                        previousConnectionFailed = true;
                        // close/abort connection
                        if (put != null) {
                            put.abort();
                        }
                        errorMsg += ".  Hit retry limit, aborting connection.";
                        throw new CommunicationException(errorMsg, exc);
                    } else {
                        errorMsg += ".  Retrying...";
                        statusHandler.handle(Priority.INFO, errorMsg);
                        retry = true;
                    }
                }
            }

            int statusCode = resp.getStatusLine().getStatusCode();
            boolean shouldThrow = false;
            if (!isSuccess(statusCode)) {
                /*
                 * In general if we don't get a code 200, then we typically
                 * receive a String message or String HTML, so we will handle
                 * that with the default. However, we only want to throw
                 * exceptions on certain cases because codes like 404 may be
                 * handled well by parts of the system.
                 */
                if (handlerCallback instanceof DynamicSerializeStreamHandler) {
                    shouldThrow = true;
                }
                handlerCallback = new DefaultInternalStreamHandler();
            }

            processResponse(resp, handlerCallback);
            byte[] byteResult = null;
            if (handlerCallback instanceof DefaultInternalStreamHandler) {
                byteResult = ((DefaultInternalStreamHandler) handlerCallback).byteResult;
            }
            if (shouldThrow) {
                throw new HttpServerException(new String(byteResult),
                        statusCode);
            }
            return new HttpClientResponse(statusCode, byteResult,
                    getContentType(resp));
        } finally {
            if (ongoing != null) {
                ongoing.decrementAndGet();
            }
        }
    }

    /**
     * Get content type of response
     * 
     * @param response
     * @return null if none found
     */
    private static String getContentType(HttpResponse response) {
        String rval = null;
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            Header contentType = entity.getContentType();
            if (contentType != null) {
                rval = contentType.getValue();
            }
        }
        return rval;
    }

    /**
     * Streams the response content to the handler callback and closes the http
     * connection once finished.
     * 
     * @param resp
     *            the http response to stream
     * @param handlerCallback
     *            the handler that should process the response stream
     * @throws CommunicationException
     */
    private void processResponse(HttpResponse resp,
            IStreamHandler handlerCallback) throws CommunicationException {
        InputStream is = null;
        if ((resp != null) && (resp.getEntity() != null)) {
            try {
                is = resp.getEntity().getContent();
                handlerCallback.handleStream(is);
            } catch (IOException e) {
                throw new CommunicationException(
                        "IO error processing http response", e);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }

                // Closes the stream if it's still open
                try {
                    EntityUtils.consume(resp.getEntity());
                } catch (IOException e) {
                    // if there was an error reading the input stream,
                    // notify but continue
                    statusHandler.handle(Priority.EVENTB,
                            "Error reading InputStream, assuming closed");
                }
                try {
                    SafeGzipDecompressingEntity.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Posts the request and uses a DefaultInternalStreamHandler to
     * automatically stream the response into a byte[].
     * 
     * @param put
     *            the post to send to the server
     * @return the byte[] of the response
     * @throws CommunicationException
     */
    private byte[] executePostMethod(HttpPost put)
            throws CommunicationException {
        DefaultInternalStreamHandler handlerCallback = new DefaultInternalStreamHandler();
        HttpClientResponse resp = this.process(put, handlerCallback);
        checkStatusCode(resp);
        return resp.data;
    }

    /**
     * Post a message to an http address, and return the result as a byte array.
     * 
     * 
     * @param address
     * @param message
     * @return
     * @throws Exception
     */
    public byte[] postBinary(String address, byte[] message)
            throws CommunicationException, Exception {

        HttpPost put = new HttpPost(address);
        if (gzipRequests) {
            PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                    .getInstance().getStream(message.length);
            GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream);
            gzipStream.write(message);
            gzipStream.finish();
            gzipStream.flush();
            byte[] gzipMessage = byteStream.toByteArray();
            gzipStream.close();
            if (message.length > gzipMessage.length) {
                message = gzipMessage;
                put.setHeader("Content-Encoding", "gzip");
            }
        }

        put.setEntity(new ByteArrayEntity(message));

        return executePostMethod(put);
    }

    /**
     * Transforms the object into bytes and posts it to the server at the
     * address. If gzip requests are enabled the object will be transformed into
     * a byte[] and then gzipped before sending. Streams the response back
     * through DynamicSerialize.
     * 
     * @param address
     *            the address to post to
     * @param obj
     *            the object to transform and send
     * @param stream
     *            if the request should be streamed if possible
     * @return the deserialized object response
     * @throws CommunicationException
     * @throws Exception
     */
    public Object postDynamicSerialize(String address, Object obj,
            boolean stream) throws CommunicationException, Exception {
        HttpPost put = new HttpPost(address);
        DynamicSerializeEntity dse = new DynamicSerializeEntity(obj, stream,
                gzipRequests);

        put.setEntity(dse);
        if (gzipRequests) {
            put.setHeader("Content-Encoding", "gzip");
        }
        // always stream the response for memory efficiency
        DynamicSerializeStreamHandler handlerCallback = new DynamicSerializeStreamHandler();
        HttpClientResponse resp = this.process(put, handlerCallback);
        checkStatusCode(resp);
        return handlerCallback.getResponseObject();
    }

    /**
     * Post a message to an http address, and return the result as a byte array.
     * <p>
     * Implementation note: The given stream handler will be used at least
     * twice: Once to determine the length, another to actually send the
     * content. This is done because pypies does not accept chunked requests
     * bodies.
     * 
     * @param address
     * @param handler
     *            the handler responsible for generating the message to be
     *            posted
     * @return
     * @throws CommunicationException
     */
    public byte[] postBinary(String address, OStreamHandler handler)
            throws CommunicationException {
        OStreamEntity entity = new OStreamEntity(handler);
        HttpPost put = new HttpPost(address);
        put.setEntity(entity);

        return executePostMethod(put);
    }

    /**
     * Post a string to an endpoint and stream the result back.
     * 
     * The result should be handled inside of the handlerCallback
     * 
     * @param address
     *            the http address
     * @param message
     *            the message to send
     * @param handlerCallback
     *            the handler callback
     * @throws CommunicationException
     *             if an error occurred during transmission
     */
    public void postStreamingByteArray(String address, byte[] message,
            IStreamHandler handlerCallback) throws CommunicationException {
        postStreamingEntity(address, new ByteArrayEntity(message),
                handlerCallback);
    }

    /**
     * Executes an HttpUriRequest and returns a response with the byte[] and
     * http status code.
     * 
     * @param request
     *            the request to execute
     * @return the byte[] result and status code
     * @throws CommunicationException
     */
    public HttpClientResponse executeRequest(HttpUriRequest request)
            throws CommunicationException {
        return executeRequest(request, new DefaultInternalStreamHandler());
    }

    /**
     * Executes an HttpUriRequest and returns a response with a status code
     * AFTER the IStreamHandler has processed the response body. Therefore, it
     * is unlikely that the response will contain the actual response body, with
     * the response body being consumed by the IStreamHandler.
     * 
     * @param request
     *            the request to execute
     * @return a response with a status code
     * @throws CommunicationException
     */
    public HttpClientResponse executeRequest(HttpUriRequest request,
            IStreamHandler handlerCallback) throws CommunicationException {
        return process(request, handlerCallback);
    }

    /**
     * Post a string to an endpoint and stream the result back.
     * 
     * The result should be handled inside of the handlerCallback
     * 
     * @param address
     *            the http address
     * @param message
     *            the message to send
     * @param handlerCallback
     *            the handler callback
     * @throws UnsupportedEncodingException
     * @throws CommunicationException
     */
    @Deprecated
    public void postStreamingString(String address, String message,
            IStreamHandler handlerCallback) throws CommunicationException,
            UnsupportedEncodingException {
        postStreamingEntity(address, new StringEntity(message), handlerCallback);
    }

    /**
     * @param response
     * @throws CommunicationException
     *             if status code is not {@link #SUCCESS_CODE}
     */
    private void checkStatusCode(HttpClientResponse response)
            throws CommunicationException {
        if (!isSuccess(response.code)) {
            throw new CommunicationException(
                    "Error reading server response.  Got error message: "
                            + response.data != null ? new String(response.data)
                            : null);
        }
    }

    /**
     * Posts an entity to the address and stream the result back.
     * 
     * @param address
     *            the http address to post to
     * @param entity
     *            an entity containing the message to send
     * @param handlerCallback
     *            the handler callback
     * @throws CommunicationException
     */
    private void postStreamingEntity(String address, AbstractHttpEntity entity,
            IStreamHandler handlerCallback) throws CommunicationException {
        HttpPost put = new HttpPost(address);
        put.setEntity(entity);
        HttpClientResponse resp = process(put, handlerCallback);
        checkStatusCode(resp);
    }

    /**
     * Get the maximum number of connections.
     * 
     * @return the max connections
     */
    public int getMaxConnectionsPerHost() {
        return this.config.getMaxConnections();
    }

    /**
     * Get the socket timeout
     * 
     * @return the socket timeout
     */
    public int getSocketTimeout() {
        return this.config.getSocketTimeout();
    }

    /**
     * Get the connection timeout
     * 
     * @return the connection timeout
     */
    public int getConnectionTimeout() {
        return this.config.getConnectionTimeout();
    }

    /**
     * Number of times to retry in the event of a socket exception. Default is
     * 1.
     */
    public int getRetryCount() {
        return this.retryCount;
    }

    /**
     * Number of times to retry in the event of a socket exception. Default is
     * 1.
     * 
     * @param retryCount
     */
    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    /**
     * Gets the network statistics for http traffic.
     * 
     * @return network stats
     */
    public NetworkStatistics getStats() {
        return this.stats;
    }

    /**
     * Provides a safe interface callback for implementing stream behavior with
     * http
     * 
     * The lifetime of the stream is only guaranteed inside the scope of the
     * handleScope method. A user should not close the stream, it will be closed
     * for them after the method completes.
     * 
     * @author chammack
     * @version 1.0
     */
    public static interface IStreamHandler {

        /**
         * Implementation method for stream callbacks
         * 
         * A user should NOT close the stream, it will be done for them after
         * the method terminates. A user should NOT store off copies of the
         * input stream for later use.
         * 
         * @param is
         * @throws CommunicationException
         */
        public abstract void handleStream(InputStream is)
                throws CommunicationException;
    }

    /**
     * Responsible for writing HTTP content to a stream. May be called more than
     * once for a given entity. See postBinary(String, OStreamHandler) for
     * details.
     */
    public static interface OStreamHandler {
        public void writeToStream(OutputStream os)
                throws CommunicationException;
    }

    /**
     * Automatically reads a stream into a byte array and stores the byte array
     * in byteResult. Should only be used internally in HttpClient with
     * convenience methods that do not take an IStreamHandler as an argument.
     * 
     */
    private static class DefaultInternalStreamHandler implements IStreamHandler {

        private byte[] byteResult;

        @Override
        public void handleStream(InputStream is) throws CommunicationException {
            PooledByteArrayOutputStream baos = ByteArrayOutputStreamPool
                    .getInstance().getStream();
            try {
                byte[] underlyingArray = baos.getUnderlyingArray();
                int read = 0;
                int index = 0;
                do {
                    try {
                        read = is.read(underlyingArray, index,
                                underlyingArray.length - index);
                    } catch (IOException e) {
                        throw new CommunicationException(
                                "Error reading byte response", e);
                    }

                    if (read > 0) {
                        index += read;
                        if (index == underlyingArray.length) {
                            baos.setCapacity(underlyingArray.length << 1);
                            underlyingArray = baos.getUnderlyingArray();
                        }
                    }
                } while (read > 0);

                baos.setCount(index);
                byteResult = new byte[index];
                System.arraycopy(underlyingArray, 0, byteResult, 0, index);
            } finally {
                if (baos != null) {
                    try {
                        baos.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        }

    }

    /**
     * Setup the credentials for SSL.
     * 
     * @param host
     *            The host
     * @param port
     *            The port
     * @param username
     *            The username
     * @param password
     *            The password
     */
    public synchronized void setupCredentials(String host, int port,
            String username, String password) {
        if (credentialsProvider == null) {
            credentialsProvider = new BasicCredentialsProvider();
        }
        credentialsProvider.setCredentials(new AuthScope(host, port,
                AuthScope.ANY_REALM, AuthSchemes.BASIC),
                new UsernamePasswordCredentials(username, password));
    }

    /**
     * Gets a thread local HttpContext to use for an https request.
     * 
     * @return a safe context containing https credential and auth info
     */
    private HttpClientContext getHttpsContext(String host, int port) {
        HttpClientContext context = httpsContext.get();
        if (context.getCredentialsProvider() != credentialsProvider) {
            context.setCredentialsProvider(credentialsProvider);
        }

        /*
         * HttpContext, BasicAuthCache, BasicScheme, and the Base64 instance
         * inside BasicScheme are not thread safe! Therefore we need one for
         * each thread. (BasicCredentialsProvider is thread safe).
         */
        AuthCache authCache = context.getAuthCache();
        if (authCache == null) {
            authCache = new BasicAuthCache();
            context.setAuthCache(authCache);
        }
        HttpHost hostObj = new HttpHost(host, port, HTTPS);
        if (authCache.get(hostObj) == null) {
            authCache.put(hostObj, new BasicScheme());
        }

        return context;
    }

    /**
     * @return the config
     */
    public HttpClientConfig getConfig() {
        return config;
    }

}
