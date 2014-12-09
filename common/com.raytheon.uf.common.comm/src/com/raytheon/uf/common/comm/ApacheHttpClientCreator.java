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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Utility for constructing apache http client instances
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 4, 2014  3570      bclement     Initial creation
 * Nov 15, 2014 3757      dhladky      Added general certificate checks.
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class ApacheHttpClientCreator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ApacheHttpClientCreator.class);

    private ApacheHttpClientCreator() {
    }

    /**
     * @param config
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @see {@link #createSslClient(HttpClientConfig, NetworkStatistics)}
     */
    public static CloseableHttpClient createSslClient(HttpClientConfig config)
            throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {
        return createSslClient(config, null);
    }

    /**
     * Construct a new HTTP client that is configured for HTTPS connections
     * 
     * @param config
     * @param stats
     * @return
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     */
    public static CloseableHttpClient createSslClient(HttpClientConfig config, NetworkStatistics stats)
            throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException {

        SSLContextBuilder sslCtxBuilder = new SSLContextBuilder();

        /**
         * TODO Need to validate whether this method of validation works
         * correctly. It predicates that if this returns false, Java will
         * automatically, (According to documentation) then validate using the
         * loaded truststore(KeyStore) this should allow for "self" signed certs
         * used by Data Delivery and such.
         */
        if (config.getHttpsHandler().isValidateCertificates()) {
            
            final KeyStore truststore = config.getHttpsHandler().getTruststore();
            // Load a local TrustStrategy for first check
            TrustStrategy trustStrategy = new LocalTrustStrategy(truststore);
            sslCtxBuilder.loadTrustMaterial(truststore, trustStrategy);
            
        } else {
            /*
             * No comparison is done, just returns a blind "true" with no loaded
             * truststore. Original implementation.
             */
            sslCtxBuilder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {
                    // Do no validation what so ever
                    statusHandler.handle(Priority.DEBUG,
                            "Proceeding with No Validation of Certificates!");
                    return true;
                }
            });
        }

        SSLContext sslCtx = sslCtxBuilder.build();
        SSLConnectionSocketFactory ssf = new SSLConnectionSocketFactory(sslCtx,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        RegistryBuilder<ConnectionSocketFactory> registryBuilder = RegistryBuilder
                .create();
        registryBuilder.register("https", ssf);
        registryBuilder.register("http", new PlainConnectionSocketFactory());
        Registry<ConnectionSocketFactory> registry = registryBuilder.build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        clientBuilder.setSSLSocketFactory(ssf);
        addLoggingInterceptors(clientBuilder, stats);
        RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
        SocketConfig.Builder soConfigBuilder = SocketConfig.custom();

        // Set the proxy info
        if (ProxyConfiguration.HTTPS_PROXY_DEFINED) {
            HttpHost proxy = new HttpHost(
                    ProxyConfiguration.getHttpsProxyHost(),
                    ProxyConfiguration.getHttpsProxyPort());
            reqConfigBuilder.setProxy(proxy);
        }

        reqConfigBuilder.setSocketTimeout(config.getSocketTimeout());
        soConfigBuilder.setSoTimeout(config.getSocketTimeout());
        reqConfigBuilder.setConnectTimeout(config.getConnectionTimeout());
        soConfigBuilder.setTcpNoDelay(config.isTcpNoDelay());
        reqConfigBuilder.setExpectContinueEnabled(config
                .isExpectContinueEnabled());

        clientBuilder.setDefaultRequestConfig(reqConfigBuilder.build());
        clientBuilder.setDefaultSocketConfig(soConfigBuilder.build());

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                registry);
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnections());
        clientBuilder.setConnectionManager(connectionManager);

        configureGzip(clientBuilder, config);

        return clientBuilder.build();
    }

    /**
     * Adds GZIP (compression) interceptors to client builder
     * 
     * @param clientBuilder
     * @param config
     */
    private static void configureGzip(HttpClientBuilder clientBuilder,
            HttpClientConfig config) {
        if (config.isHandlingGzipResponses()) {
            // Add gzip compression handlers
            // advertise we accept gzip
            clientBuilder.addInterceptorLast(new GzipRequestInterceptor());
            // handle gzip contents
            clientBuilder.addInterceptorLast(new GzipResponseInterceptor());
        }
    }

    /**
     * Adds statistics logging interceptors to client builder
     * 
     * @param clientBuilder
     * @param stats
     */
    private static void addLoggingInterceptors(HttpClientBuilder clientBuilder,
            final NetworkStatistics stats) {
        if (stats != null) {
            clientBuilder.addInterceptorLast(new HttpRequestInterceptor() {
                @Override
                public void process(final HttpRequest request,
                        final HttpContext context) throws HttpException,
                        IOException {
                    try {
                        if (request != null
                                && request.getFirstHeader("Content-Length") != null) {
                            Header contentLenHeader = request
                                    .getFirstHeader("Content-Length");
                            long len = Long.valueOf(contentLenHeader.getValue());
                            stats.log(len, 0);
                        }
                    } catch (Throwable t) {
                        statusHandler.handle(Priority.DEBUG,
                                "Error in httpClient request interceptor", t);
                    }
                }
            });
            clientBuilder.addInterceptorLast(new HttpResponseInterceptor() {
                @Override
                public void process(final HttpResponse response,
                        final HttpContext context) throws HttpException,
                        IOException {
                    try {
                        if (response != null && response.getEntity() != null) {

                            stats.log(0, response.getEntity()
                                    .getContentLength());
                        }
                    } catch (Throwable t) {
                        statusHandler.handle(Priority.DEBUG,
                                "Error in httpsClient response interceptor", t);
                    }
                }
            });
        }
    }

    /**
     * @param config
     * @return
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @see #createClient(HttpClientConfig, NetworkStatistics)
     */
    public static CloseableHttpClient createClient(HttpClientConfig config) {
        return createClient(config, null);
    }

    /**
     * Creates a new HTTP client according to the provided configuration
     * 
     * @param config
     * @param stats
     * @return
     */
    public static CloseableHttpClient createClient(HttpClientConfig config,
            NetworkStatistics stats) {

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        /*
         * we don't support cookies at this time, so don't waste any time in the
         * interceptors
         */
        clientBuilder.disableCookieManagement();
        addLoggingInterceptors(clientBuilder, stats);
        RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
        SocketConfig.Builder soConfigBuilder = SocketConfig.custom();

        reqConfigBuilder.setSocketTimeout(config.getSocketTimeout());
        soConfigBuilder.setSoTimeout(config.getSocketTimeout());
        reqConfigBuilder.setConnectTimeout(config.getConnectionTimeout());
        soConfigBuilder.setTcpNoDelay(config.isTcpNoDelay());
        reqConfigBuilder.setExpectContinueEnabled(config
                .isExpectContinueEnabled());

        clientBuilder.setDefaultRequestConfig(reqConfigBuilder.build());
        clientBuilder.setDefaultSocketConfig(soConfigBuilder.build());

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(config.getMaxConnections());
        clientBuilder.setConnectionManager(connectionManager);

        configureGzip(clientBuilder, config);

        return clientBuilder.build();
    }

    /**
     * Adds Accept-Encoding: gzip to every outgoing request
     */
    private static class GzipRequestInterceptor implements
            HttpRequestInterceptor {

        @Override
        public void process(HttpRequest request, HttpContext context)
                throws HttpException, IOException {
            if (!request.containsHeader("Accept-Encoding")) {
                request.addHeader("Accept-Encoding", "gzip");
            }
        }
    }

    /**
     * Decompresses any responses that arrive with Content-Encoding: gzip
     */
    private static class GzipResponseInterceptor implements
            HttpResponseInterceptor {

        @Override
        public void process(HttpResponse response, HttpContext context)
                throws HttpException, IOException {
            HttpEntity entity = response.getEntity();
            Header ceheader = entity.getContentEncoding();
            if (ceheader != null) {
                HeaderElement[] codecs = ceheader.getElements();
                for (HeaderElement codec : codecs) {
                    if (codec.getName().equalsIgnoreCase("gzip")) {
                        response.setEntity(new SafeGzipDecompressingEntity(
                                response.getEntity()));
                        return;
                    }
                }
            }
        }
    }

}
