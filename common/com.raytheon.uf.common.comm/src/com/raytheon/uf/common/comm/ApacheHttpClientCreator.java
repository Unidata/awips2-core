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

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;

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
import org.apache.http.util.VersionInfo;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.app.AppInfo;

/**
 * Utility for constructing apache http client instances
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 04, 2014  3570     bclement  Initial creation
 * Nov 15, 2014  3757     dhladky   Added general certificate checks.
 * Jan 22, 2015  3952     njensen   Removed gzip handling as apache http client
 *                                  has it built-in
 * May 10, 2015  4435     dhladky   PDA necessitated the loading of keyMaterial
 *                                  as well as trustMaterial.
 * Jul 01, 2015  4021     bsteffen  Set User Agent
 * Jul 06, 2015  4614     njensen   Disable gzip by default
 * Dec 07, 2015  4834     njensen   Changes for rename of IHttpsHandler to
 *                                  HttpAuthHandler
 * Jan 27, 2016  5170     tjensen   Removed log interceptors. Logging moved to
 *                                  methods where message type is known.
 * Jan 31, 2017  6083     bsteffen  Remove local trust strategy
 * Apr 02, 2020  8086     randerso  Use HttpClientBuilder.useSystemProperties() to
 *                                  handle proxy settings
 *
 * </pre>
 *
 * @author bclement
 */
public class ApacheHttpClientCreator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(ApacheHttpClientCreator.class);

    private ApacheHttpClientCreator() {
    }

    /**
     * @param config
     * @return an http client based on the config
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     * @see #createSslClient(HttpClientConfig, NetworkStatistics)
     */
    public static CloseableHttpClient createSslClient(HttpClientConfig config)
            throws NoSuchAlgorithmException, KeyStoreException,
            KeyManagementException, UnrecoverableKeyException {
        return createSslClient(config, null);
    }

    /**
     * Construct a new HTTP client that is configured for HTTPS connections
     *
     * @param config
     * @param stats
     * @return an https client based on the config
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws KeyManagementException
     * @throws UnrecoverableKeyException
     */
    public static CloseableHttpClient createSslClient(HttpClientConfig config,
            NetworkStatistics stats) throws NoSuchAlgorithmException,
    KeyStoreException, KeyManagementException,
    UnrecoverableKeyException {

        SSLContextBuilder sslCtxBuilder = new SSLContextBuilder();
        HttpAuthHandler handler = config.getHttpAuthHandler();

        /**
         * If this returns false, Java will automatically, (According to
         * documentation) then validate using the default java loaded
         * truststore/keyStore. This override method allows for "self" signed
         * certs that are locally verified and or submitted to remote servers.
         * This method is used by Data Delivery and such.
         */
        if (handler.isValidateCertificates()) {

            final KeyStore truststore = handler.getTruststore();
            sslCtxBuilder.loadTrustMaterial(truststore);

            if (handler.getKeystore() != null) {
                /*
                 * Validate certificates and submit key(s). This is the general
                 * situation where sometimes you act as the server and validate
                 * clients. Other times you act as a client and submit your
                 * key(s) to remote servers.
                 */

                final KeyStore keystore = handler.getKeystore();
                sslCtxBuilder.loadKeyMaterial(keystore,
                        handler.getKeystorePassword());
                statusHandler
                .handle(Priority.DEBUG,
                        "Proceeding with validation of certificates.  Presenting key(s) for validation.");

            } else {
                /*
                 * Validate certificates w/o submitting keys. This is only
                 * useful where you are a "server" and you only validate
                 * clients.
                 */
                statusHandler
                .handle(Priority.DEBUG,
                        "Proceeding with validation of certificates.  Not presenting key(s) for validation.");
            }

        } else {
            /*
             * No comparison is done, just returns a blind "true" with no loaded
             * truststore. Original implementation.
             */
            sslCtxBuilder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain,
                        String authType) throws CertificateException {

                    return true;
                }
            });

            // Do no validation what so ever
            statusHandler
            .handle(Priority.DEBUG,
                    "Proceeding with no validation of certificates or key submission.");
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
        RequestConfig.Builder reqConfigBuilder = RequestConfig.custom();
        SocketConfig.Builder soConfigBuilder = SocketConfig.custom();

        // Use system properties to get the proxy info
        clientBuilder.useSystemProperties();

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
        setUserAgent(clientBuilder);

        /*
         * build() call automatically adds gzip interceptors unless we
         * explicitly disable that
         */
        if (!config.isGzipEnabled()) {
            clientBuilder.disableContentCompression();
        }
        return clientBuilder.build();
    }

    /**
     * @param config
     * @return an http client based on the config
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
     * @return an http client based on the config
     */
    public static CloseableHttpClient createClient(HttpClientConfig config,
            NetworkStatistics stats) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        /*
         * we don't support cookies at this time, so don't waste any time in the
         * interceptors
         */
        clientBuilder.disableCookieManagement();
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
        setUserAgent(clientBuilder);

        /*
         * build() call automatically adds gzip interceptors unless we
         * explicitly disable that
         */
        if (!config.isGzipEnabled()) {
            clientBuilder.disableContentCompression();
        }
        return clientBuilder.build();
    }

    private static void setUserAgent(HttpClientBuilder clientBuilder) {
        AppInfo appInfo = AppInfo.getInstance();
        if (appInfo == null || appInfo.getName() == null) {
            return;
        }
        StringBuilder userAgent = new StringBuilder(appInfo.getName());
        if (appInfo.getVersion() != null) {
            userAgent.append("/").append(appInfo.getVersion());
        }
        StringBuilder comments = new StringBuilder();
        if (appInfo.getMode() != null) {
            comments.append(appInfo.getMode());
        }
        String os = System.getProperty("os.name");
        if (os != null) {
            if (comments.length() != 0) {
                comments.append("; ");
            }
            comments.append(os);
        }
        String javaVersion = System.getProperty("java.version");
        if (javaVersion != null) {
            if (comments.length() != 0) {
                comments.append("; ");
            }
            comments.append("java " + javaVersion);
        }
        if (comments.length() != 0) {
            userAgent.append(" (").append(comments).append(')');
        }

        /*
         * We want to include the apache user agent as part of ours but they
         * don't expose it so need to build it here
         */
        userAgent.append(" Apache-HttpClient/");
        VersionInfo vi = VersionInfo.loadVersionInfo("org.apache.http.client",
                HttpClientBuilder.class.getClassLoader());
        String release = (vi != null) ? vi.getRelease()
                : VersionInfo.UNAVAILABLE;
        userAgent.append(release);
        clientBuilder.setUserAgent(userAgent.toString());
    }

}
