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

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;

import com.raytheon.uf.common.comm.ApacheHttpClientCreator;
import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.HttpServerException;
import com.raytheon.uf.common.json.BasicJsonService;
import com.raytheon.uf.common.json.JsonException;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Qpid implementation of IBrokerRestProvider
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Apr 04, 2014  2694     randerso  Converted python implementation to Java
 * Jan 25, 2017  6092     randerso  Renamed and added queueReady method
 * Jan 31, 2017  6083     bsteffen  Add https support
 * Feb 02, 2017  6104     randerso  Add checking for empty client list which
 *                                  indicates an issue with the
 *                                  JMS_CONNECTIONS_URL
 *
 * </pre>
 *
 * @author randerso
 */
public class QpidBrokerRestImpl implements IBrokerRestProvider {
    private static final String JMS_CONNECTIONS_URL = "JMS_CONNECTIONS_URL";

    private static final String JMS_QUEUE_URL = "JMS_QUEUE_URL";

    private CloseableHttpClient client = null;

    @Override
    public List<String> getConnections() throws CommunicationException,
            JMSConfigurationException, HttpServerException {
        // Use rest services to pull connection clientId
        // http://brokerHost:port/rest/connection/edex
        // port needs to be passed as a parameter
        // parse json response for clientId, recommend using a hash of some kind

        String url = System.getenv(JMS_CONNECTIONS_URL);
        if (url == null) {
            throw new JMSConfigurationException(
                    JMS_CONNECTIONS_URL + " is not set in setup.env");
        }

        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (!isSuccess(response)) {
                String msg = String.format("Broker returned %d",
                        response.getStatusLine().getStatusCode());

                throw new HttpServerException(msg,
                        response.getStatusLine().getStatusCode());
            }

            List<String> resultSet = new ArrayList<>();
            try (InputStream content = response.getEntity().getContent()) {
                BasicJsonService json = new BasicJsonService();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) json
                        .deserialize(content, Object.class);

                /*
                 * if there are no connections then we are probably not using
                 * the correct REST URL
                 */
                if (jsonObjList.isEmpty()) {
                    throw new JMSConfigurationException(
                            "Connection list is empty. Check "
                                    + JMS_CONNECTIONS_URL + " in "
                                    + FileUtil.join(EDEXUtil.getEdexBin(),
                                            "setup.env"));
                }

                for (Map<String, Object> statDict : jsonObjList) {
                    String clientId = (String) statDict.get("clientId");
                    if (clientId != null) {
                        resultSet.add(clientId);
                    }
                }
            } catch (JsonException e) {
                throw new CommunicationException("Unable to parse response", e);
            }
            return resultSet;

        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    @Override
    public boolean queueReady(String queue)
            throws CommunicationException, JMSConfigurationException {
        // Use the Qpid rest service to determine if the specified queue exists
        // and is ready to receive messages

        String urlPrefix = System.getenv(JMS_QUEUE_URL);
        if (urlPrefix == null) {
            throw new JMSConfigurationException(
                    JMS_QUEUE_URL + " is not set in setup.env");
        }

        String url = String.join("/", urlPrefix, queue);
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (isSuccess(response)) {
                try (InputStream content = response.getEntity().getContent()) {
                    BasicJsonService json = new BasicJsonService();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) json
                            .deserialize(content, Object.class);

                    @SuppressWarnings("unchecked")
                    Map<String, Integer> statistics = (Map<String, Integer>) jsonObjList
                            .get(0).get("statistics");
                    int bindingCount = statistics.get("bindingCount");
                    int consumerCount = statistics.get("consumerCount");
                    return (bindingCount > 0) && (consumerCount > 0);

                } catch (JsonException | IOException e) {
                    throw new CommunicationException("Unable to parse response",
                            e);
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
        return false;
    }

    @Override
    public void deleteQueue(String queue) throws CommunicationException,
            JMSConfigurationException, HttpServerException {
        // Use the Qpid rest service to delete the queue

        String urlPrefix = System.getenv(JMS_QUEUE_URL);
        if (urlPrefix == null) {
            throw new JMSConfigurationException(
                    JMS_QUEUE_URL + " is not set in setup.env");
        }

        String url = String.join("/", urlPrefix, queue);
        HttpDelete request = new HttpDelete(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (!isSuccess(response)) {
                String msg = String.format("Broker returned %d",
                        response.getStatusLine().getStatusCode());

                throw new HttpServerException(msg,
                        response.getStatusLine().getStatusCode());
            }
        } catch (IOException e) {
            throw new CommunicationException(e);
        }
    }

    private boolean isSuccess(CloseableHttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    private synchronized CloseableHttpClient getHttpClient()
            throws JMSConfigurationException {

        if (client == null) {
            boolean https = false;
            String connectionUrl = System.getenv(JMS_CONNECTIONS_URL);
            String queueUrl = System.getenv(JMS_QUEUE_URL);
            if (connectionUrl != null) {
                https = connectionUrl.startsWith("https://");
                if (queueUrl != null
                        && queueUrl.startsWith("https://") != https) {
                    throw new JMSConfigurationException(
                            JMS_QUEUE_URL + " and " + JMS_CONNECTIONS_URL
                                    + " must use the same protocol.");
                }
            } else if (queueUrl != null) {
                https = queueUrl.startsWith("https://");
            }
            HttpClientConfigBuilder config = new HttpClientConfigBuilder();
            config.setMaxConnections(1);
            if (https) {
                config.setHttpAuthHandler(new QpidCertificateAuthHandler());
                try {
                    client = ApacheHttpClientCreator
                            .createSslClient(config.build());
                } catch (GeneralSecurityException e) {
                    throw new JMSConfigurationException(
                            "Failed to load ssl configuration.", e);
                }
            } else {
                client = ApacheHttpClientCreator.createClient(config.build());
            }
        }
        return client;
    }

}
