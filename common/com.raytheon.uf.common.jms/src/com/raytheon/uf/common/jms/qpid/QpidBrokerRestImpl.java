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
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.HttpServerException;
import com.raytheon.uf.common.json.BasicJsonService;
import com.raytheon.uf.common.json.JsonException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Qpid implementation of IBrokerRestProvider
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ----------------------------------------
 * Apr 04, 2014  2694     randerso    Converted python implementation to Java
 * Jan 25, 2017  6092     randerso    Renamed and added queueReady method
 * Jan 31, 2017  6083     bsteffen    Add https support
 * Feb 02, 2017  6104     randerso    Add checking for empty client list which
 *                                    indicates an issue with the
 *                                    JMS_CONNECTIONS_URL
 * Feb 08, 2017  6092     randerso    Add additional error checking in
 *                                    queueReady.
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 17, 2019  7724     tgurney     Minor fixes in {@link #createBinding(String, String)}
 * Oct 22, 2019  7724     tgurney     Additional cleanup and fixes
 *
 * </pre>
 *
 * @author randerso
 */
public class QpidBrokerRestImpl implements IBrokerRestProvider {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(QpidBrokerRestImpl.class);

    private static final String HTTP_SCHEME = "https";

    private static final String BROKER_REST_PORT = "8180";

    private HttpClient httpClient;

    private String jmsConnUrl = null;

    private String jmsQueueUrl = null;

    private String jmsExcUrl = null;

    private CloseableHttpClient client = null;

    public QpidBrokerRestImpl() throws JMSConfigurationException {
        httpClient = new HttpClient(getSSLClientConfigBuilder().build());
    }

    public QpidBrokerRestImpl(String hostname, String vhost)
            throws JMSConfigurationException {
        this();
        setConnectionUrl(hostname);
        setExchangeUrl(hostname, vhost);
        setQueueUrl(hostname, vhost);
    }

    @Override
    public boolean createQueue(String queue)
            throws JMSConfigurationException, CommunicationException {
        boolean queueCreatedSuccessfully = false;

        if (jmsQueueUrl == null) {
            throw new JMSConfigurationException(
                    "The queue REST url has not been defined");
        }

        String queueUrl = String.join("/", jmsQueueUrl, queue);

        if (!queueExists(queueUrl)) {
            String attributes = "{ \"name\" : \"" + queue + "\" }";

            try {
                queueCreatedSuccessfully = postSucceeded(new String(
                        httpClient.postByteResult(jmsQueueUrl, attributes)));
            } catch (Exception e) {
                if (queueExists(queueUrl)) {
                    queueCreatedSuccessfully = true;
                } else {
                    statusHandler.error(
                            "An error occurred while trying to create the queue "
                                    + queue,
                            e);
                }
            }
        }

        return queueCreatedSuccessfully;
    }

    @Override
    public boolean createBinding(String queue, String bindingKey,
            String exchange)
            throws JMSConfigurationException, CommunicationException {
        boolean success = false;

        if (jmsExcUrl == null) {
            throw new JMSConfigurationException(
                    "The exchange REST url has not been defined");
        }

        String exchangeUrl = String.join("/", jmsExcUrl, exchange);

        if (!bindingExists(exchangeUrl, bindingKey)) {
            try {
                String operationUrl = String.join("/", exchangeUrl, "bind");
                String attributes = "{ \"bindingKey\" : \"" + bindingKey
                        + "\", \"destination\" : \"" + queue + "\" }";
                httpClient.postByteResult(operationUrl, attributes);
                success = true;
            } catch (Exception e) {
                statusHandler.error(
                        "An error occurred while trying to create the binding: "
                                + exchange + "/" + bindingKey + " -> " + queue,
                        e);
            }
        }

        return success;
    }

    @Override
    public List<String> getConnections() throws CommunicationException,
            JMSConfigurationException, HttpServerException {
        // Use rest services to pull connection clientId
        // http://brokerHost:port/rest/connection/edex
        // port needs to be passed as a parameter
        // parse json response for clientId, recommend using a hash of some kind

        HttpGet request = new HttpGet(jmsConnUrl);
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
                                    + "that the hostname and vhost "
                                    + "have been set.");
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

        if (jmsQueueUrl == null) {
            throw new JMSConfigurationException(
                    "The queue REST url has not been defined.");
        }

        String url = String.join("/", jmsQueueUrl, queue);
        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (isSuccess(response)) {
                try (InputStream content = response.getEntity().getContent()) {
                    BasicJsonService json = new BasicJsonService();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) json
                            .deserialize(content, Object.class);

                    if (!jsonObjList.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Integer> statistics = (Map<String, Integer>) jsonObjList
                                .get(0).get("statistics");
                        int bindingCount = statistics.get("bindingCount");
                        int consumerCount = statistics.get("consumerCount");
                        return bindingCount > 0 && consumerCount > 0;
                    }

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

        if (jmsQueueUrl == null) {
            throw new JMSConfigurationException(
                    "The queue REST url has not been defined.");
        }

        String url = String.join("/", jmsQueueUrl, queue);
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

    @Override
    public boolean queueExists(String url)
            throws CommunicationException, JMSConfigurationException {
        boolean queueExists = false;

        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (isSuccess(response)) {
                try (InputStream content = response.getEntity().getContent()) {
                    BasicJsonService json = new BasicJsonService();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonObjMap = (Map<String, Object>) json
                            .deserialize(content, Object.class);

                    if (!jsonObjMap.isEmpty()) {
                        queueExists = true;
                    }
                } catch (JsonException e) {
                    throw new CommunicationException(
                            "Unable to parse response from " + url, e);
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(
                    "Error occurred executing request for " + url, e);
        }

        return queueExists;
    }

    @Override
    public boolean bindingExists(String url, String name)
            throws CommunicationException, JMSConfigurationException {
        boolean bindingExists = false;

        HttpGet request = new HttpGet(url);
        try (CloseableHttpResponse response = getHttpClient()
                .execute(request)) {

            if (isSuccess(response)) {
                try (InputStream content = response.getEntity().getContent()) {
                    BasicJsonService json = new BasicJsonService();

                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonObjList = (Map<String, Object>) json
                            .deserialize(content, Object.class);

                    if (!jsonObjList.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> bindings = (List<Map<String, Object>>) jsonObjList
                                .get("bindings");

                        for (Map<String, Object> binding : bindings) {
                            if (binding.containsKey(name)) {
                                bindingExists = true;
                                break;
                            }
                        }
                    }
                } catch (JsonException e) {
                    throw new CommunicationException(
                            "Unable to parse response from " + url, e);
                }
            }
        } catch (IOException e) {
            throw new CommunicationException(
                    "Error occurred executing request for " + url, e);
        }

        return bindingExists;
    }

    private void setConnectionUrl(String hostname) {
        jmsConnUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/connection";
    }

    private void setQueueUrl(String hostname, String vhost) {
        jmsQueueUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/queue/" + vhost + "/" + vhost;
    }

    private void setExchangeUrl(String hostname, String vhost) {
        jmsExcUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/exchange/" + vhost + "/" + vhost;
    }

    private boolean isSuccess(CloseableHttpResponse response) {
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean postSucceeded(String result) throws JsonException {
        BasicJsonService json = new BasicJsonService();
        boolean postSucceeded = false;

        @SuppressWarnings("unchecked")
        Map<String, Object> jsonObjList = (Map<String, Object>) json
                .deserialize(result, Object.class);

        if (jsonObjList.containsKey("id")
                && !jsonObjList.get("id").toString().isEmpty()) {
            postSucceeded = true;
        }

        return postSucceeded;
    }

    private HttpClientConfigBuilder getSSLClientConfigBuilder()
            throws JMSConfigurationException {
        HttpClientConfigBuilder config = new HttpClientConfigBuilder();
        config.setMaxConnections(1);
        config.setHttpAuthHandler(new QpidCertificateAuthHandler());
        return config;
    }

    private synchronized CloseableHttpClient getHttpClient()
            throws JMSConfigurationException {

        if (client == null) {
            boolean https = false;
            if (jmsConnUrl != null) {
                https = jmsConnUrl.startsWith("https://");
                if (jmsQueueUrl != null
                        && jmsQueueUrl.startsWith("https://") != https) {
                    throw new JMSConfigurationException(
                            "The queue and connection REST urls must use the same protocol.");
                }
            } else if (jmsQueueUrl != null) {
                https = jmsQueueUrl.startsWith("https://");
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
