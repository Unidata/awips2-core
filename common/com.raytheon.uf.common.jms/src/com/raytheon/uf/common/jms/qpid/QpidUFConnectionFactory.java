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

import java.nio.file.Path;
import java.util.Map.Entry;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnection;

import org.apache.http.client.utils.URIBuilder;
import org.apache.qpid.jms.JmsConnectionFactory;

import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.jms.JmsSslConfiguration;

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
 * </pre>
 *
 * @author tgurney
 */

public class QpidUFConnectionFactory implements ConnectionFactory {
    private final IBrokerRestProvider jmsAdmin;

    private final JmsConnectionFactory connectionFactory;

    private static final String JMS_USERNAME = "guest";

    public QpidUFConnectionFactory(JMSConnectionInfo connectionInfo)
            throws JMSConfigurationException {
        String url = QpidUFConnectionFactory.getConnectionURL(connectionInfo);
        this.connectionFactory = new JmsConnectionFactory(url);
        this.jmsAdmin = new QpidBrokerRestImpl(connectionInfo.getHost(),
                connectionInfo.getVhost(), connectionInfo.getServicePort());
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

    public static String getConnectionURL(JMSConnectionInfo connectionInfo) {
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

    public static URIBuilder configureSSL(URIBuilder uriBuilder) {
        JmsSslConfiguration sslConfig = new JmsSslConfiguration(JMS_USERNAME);
        Path trustStorePath = sslConfig.getJavaTrustStoreFile();
        Path keyStorePath = sslConfig.getJavaKeyStoreFile();
        String password = sslConfig.getPassword();

        uriBuilder.addParameter("transport.trustStoreLocation",
                trustStorePath.toString());
        uriBuilder.addParameter("transport.trustStorePassword", password);
        uriBuilder.addParameter("transport.keyStoreLocation",
                keyStorePath.toString());
        uriBuilder.addParameter("transport.keyStorePassword", password);

        return uriBuilder;
    }

    public QueueConnection createQueueConnection() throws JMSException {
        return connectionFactory.createQueueConnection();
    }
}
