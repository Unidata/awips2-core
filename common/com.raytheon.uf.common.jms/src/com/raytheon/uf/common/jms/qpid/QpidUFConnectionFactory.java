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

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.apache.hc.core5.net.URIBuilder;
import org.apache.qpid.jms.JmsConnectionExtensions;
import org.apache.qpid.jms.JmsConnectionFactory;

import com.raytheon.uf.common.jms.HttpProxyHandlerSslExt;
import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.jms.JmsSslConfiguration;

import io.netty.handler.proxy.ProxyHandler;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;

/**
 * Qpid JMS connection factory
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 11, 2019 7724       tgurney     Initial creation
 * May 27, 2021 8469       dgilling    Pass broker REST service port through
 *                                     JMSConnectionInfo.
 * Aug 06, 2021 22528      smoorthy    Add proxy handler extension to ConnectionFactory
 * Apr 12, 2022 8677       tgurney     Minor changes to SSL configuration API.
 *                                     Minor refactoring
 * Jan 03, 2023 8982       thuggins    RCM Process fills /tmp inode count with
 *                                     keystore files. Deleted temp store
 *                                     files on exception when URI building
 *                                     fails
 * Jan 17, 2023 22528      smoorthy    Pass empty credentials for proxy handler if proxy server
 *                                     doesn't require authentication. Add default ssl port 443.
 * Apr 15, 2026 2038243    mapeters    Apache httpclient 5 upgrade, remove username/password that
 *                                     were only for obsolete Windows IMET thin clients
 * </pre>
 *
 * @author tgurney
 */

public class QpidUFConnectionFactory implements ConnectionFactory {
    private final IBrokerRestProvider jmsAdmin;

    private final JmsConnectionFactory connectionFactory;

    private static final String JMS_USERNAME = "guest";

    public static final String KEY_STORE_LOCATION = "transport.keyStoreLocation";

    public static final String TRUST_STORE_LOCATION = "transport.trustStoreLocation";

    public QpidUFConnectionFactory(JMSConnectionInfo connectionInfo)
            throws JMSConfigurationException {
        String url = QpidUFConnectionFactory.getConnectionURL(connectionInfo);
        this.connectionFactory = new JmsConnectionFactory(url);

        String proxyAddr = connectionInfo.getProxyAddress();

        if (proxyAddr != null) {
            addProxyExtension(proxyAddr);
        }

        this.jmsAdmin = new QpidBrokerRestImpl(connectionInfo.getHost(),
                connectionInfo.getVhost(), connectionInfo.getServicePort());
    }

    private void addProxyExtension(String proxyAddr)
            throws JMSConfigurationException {
        // add proxy extension to the Connection Factory
        String proxyHost = null;
        int proxyPort = 0;
        try {
            URI proxyURI = new URI(proxyAddr);
            proxyHost = proxyURI.getHost();
            proxyPort = proxyURI.getPort();

            if (proxyPort < 0) {
                // default ssl port if no port entered
                proxyPort = 443;
            }

        } catch (URISyntaxException e) {
            throw new JMSConfigurationException(
                    "Problem processing proxy address string", e);
        }

        // add the proxy handler extension
        String host = proxyHost;
        int port = proxyPort;
        this.connectionFactory.setExtension(
                JmsConnectionExtensions.PROXY_HANDLER_SUPPLIER.toString(),
                (connection, remote) -> {
                    SocketAddress proxyAddress = new InetSocketAddress(host,
                            port); // 443
                    Supplier<ProxyHandler> proxyHandlerFactory = () -> {
                        return new HttpProxyHandlerSslExt(proxyAddress, "", "");
                    };
                    return proxyHandlerFactory;
                });
    }

    @Override
    public Connection createConnection(String userName, String password)
            throws JMSException {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public JMSContext createContext(String userName, String password) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public JMSContext createContext(String userName, String password,
            int sessionMode) {
        throw new UnsupportedOperationException(getClass().getSimpleName() + " "
                + "does not support username/password connections");
    }

    @Override
    public Connection createConnection() throws JMSException {
        Connection connection = connectionFactory.createConnection();
        return new QpidUFConnection(connection, jmsAdmin);
    }

    @Override
    public JMSContext createContext() {
        return connectionFactory.createContext();
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        return connectionFactory.createContext(sessionMode);
    }

    public static String getConnectionURL(JMSConnectionInfo connectionInfo)
            throws JMSConfigurationException {
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("amqps");
        uriBuilder.setHost(connectionInfo.getHost());
        uriBuilder.setPort(Integer.parseInt(connectionInfo.getPort()));
        uriBuilder.addParameter("amqp.vhost", connectionInfo.getVhost());
        uriBuilder.addParameter("jms.username", JMS_USERNAME);
        for (Entry<String, String> e : connectionInfo.getParameters()
                .entrySet()) {
            uriBuilder.addParameter(e.getKey(), e.getValue());
        }
        uriBuilder = configureSSL(uriBuilder);

        return uriBuilder.toString();
    }

    public static URIBuilder configureSSL(URIBuilder uriBuilder)
            throws JMSConfigurationException {
        JmsSslConfiguration sslConfig = new JmsSslConfiguration(JMS_USERNAME);
        Path trustStorePath = sslConfig.getJavaTrustStoreFile();
        Path keyStorePath = sslConfig.getJavaKeyStoreFile();
        try {
            String password = sslConfig.getStorePassword();

            uriBuilder.addParameter(TRUST_STORE_LOCATION,
                    trustStorePath.toString());
            uriBuilder.addParameter("transport.trustStorePassword", password);
            uriBuilder.addParameter(KEY_STORE_LOCATION,
                    keyStorePath.toString());
            uriBuilder.addParameter("transport.keyStorePassword", password);

            return uriBuilder;
        } catch (Exception e) {
            deleteTempStores(sslConfig);
            throw new JMSConfigurationException(
                    "Could not decrypt JMS password.", e);
        }
    }

    public QueueConnection createQueueConnection() throws JMSException {
        return connectionFactory.createQueueConnection();
    }

    public static boolean deleteTempStores(JmsSslConfiguration sslConfig)
            throws JMSConfigurationException {
        boolean retVal = false;
        if (sslConfig != null) {
            Path trustStorePath = sslConfig.getJavaTrustStoreFile();
            Path keyStorePath = sslConfig.getJavaKeyStoreFile();
            try {
                retVal = Files.deleteIfExists(trustStorePath)
                        && Files.deleteIfExists(keyStorePath);
            } catch (Exception e) {
                throw new JMSConfigurationException(
                        "Could not delete temporary keystore: " + keyStorePath
                                + " and truststore: " + trustStorePath,
                        e);
            }
        }

        return retVal;
    }

}
