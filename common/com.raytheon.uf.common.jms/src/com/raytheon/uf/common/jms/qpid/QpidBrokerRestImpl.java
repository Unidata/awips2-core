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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
import com.raytheon.uf.common.comm.HttpClientConfigBuilder;
import com.raytheon.uf.common.comm.HttpServerException;
import com.raytheon.uf.common.json.BasicJsonService;
import com.raytheon.uf.common.json.JsonException;

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
 * Oct 22, 2019  7724     tgurney     More cleanup. Use PUT instead of POST
 *                                    for queue creation. Set JSON content type
 *
 * </pre>
 *
 * @author randerso
 */
public class QpidBrokerRestImpl implements IBrokerRestProvider {

    private static final String HTTP_SCHEME = "https";

    private static final String BROKER_REST_PORT = "8180";

    private HttpClient httpClient;

    private ReadWriteLock httpClientLock = new ReentrantReadWriteLock();

    private String jmsConnUrl = null;

    private String jmsQueueUrl = null;

    private String jmsExcUrl = null;

    public QpidBrokerRestImpl(String hostname, String vhost)
            throws JMSConfigurationException {
        HttpClientConfigBuilder configBuilder = new HttpClientConfigBuilder();
        configBuilder.setHttpAuthHandler(new QpidCertificateAuthHandler());
        configBuilder.setMaxConnections(1);
        httpClient = new HttpClient(configBuilder.build());
        jmsConnUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/connection";
        jmsExcUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/exchange/" + vhost + "/" + vhost;
        jmsQueueUrl = HTTP_SCHEME + "://" + hostname + ":" + BROKER_REST_PORT
                + "/api/latest/queue/" + vhost + "/" + vhost;
    }

    @Override
    public void createQueue(String queue)
            throws CommunicationException, HttpServerException {
        String queueUrl = String.join("/", jmsQueueUrl, queue);
        String json = "{ \"name\" : \"" + queue + "\" }";

        HttpClientResponse resp = putJson(queueUrl, json);

        if (!isSuccess(resp)) {
            String responseBody = null;
            if (resp.data != null && resp.data.length != 0) {
                responseBody = new String(resp.data, StandardCharsets.UTF_8);
            }
            throw new HttpServerException(
                    "Failed to create the queue " + queue + "; broker returned "
                            + resp.code + ". Response body: " + responseBody,
                    resp.code);
        }

        // create binding for the queue

        String bindUrl = String.join("/", jmsExcUrl, "amq.direct", "bind");
        json = "{ \"bindingKey\" : \"" + queue + "\", \"destination\" : \""
                + queue + "\", \"replaceExistingArguments\": true }";

        resp = postJson(bindUrl, json);

        if (!isSuccess(resp)) {
            String responseBody = null;
            if (resp.data != null && resp.data.length != 0) {
                responseBody = new String(resp.data, StandardCharsets.UTF_8);
            }
            throw new HttpServerException("Failed to create the binding "
                    + "amq.direct/" + queue + "; broker returned " + resp.code
                    + ". Response body: " + responseBody, resp.code);
        }
    }

    @Override
    public List<String> getConnections()
            throws CommunicationException, HttpServerException {

        HttpClientResponse resp = get(jmsConnUrl);

        if (!isSuccess(resp)) {
            String responseBody = null;
            if (resp.data != null && resp.data.length != 0) {
                responseBody = new String(resp.data, StandardCharsets.UTF_8);
            }
            throw new HttpServerException(
                    "Failed to get connections list; broker returned "
                            + resp.code + ". Response body: " + responseBody,
                    resp.code);
        }

        List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) getJsonObject(
                resp);
        List<String> resultSet = new ArrayList<>();
        for (Map<String, Object> statDict : jsonObjList) {
            String clientId = (String) statDict.get("clientId");
            if (clientId != null) {
                resultSet.add(clientId);
            }
        }
        return resultSet;
    }

    @Override
    public boolean queueReady(String queue)
            throws CommunicationException, HttpServerException {
        String url = String.join("/", jmsQueueUrl, queue);
        HttpClientResponse resp = get(url);

        if (!isSuccess(resp)) {
            if (resp.code == 404) {
                return false;
            }
            String responseBody = null;
            if (resp.data != null && resp.data.length != 0) {
                responseBody = new String(resp.data, StandardCharsets.UTF_8);
            }
            throw new HttpServerException(
                    "Failed to get queue information; broker returned "
                            + resp.code + ". Response body: " + responseBody,
                    resp.code);
        }

        Map<String, Object> jsonObj = (Map<String, Object>) getJsonObject(resp);
        @SuppressWarnings("unchecked")
        Map<String, Integer> statistics = (Map<String, Integer>) jsonObj
                .get("statistics");
        int bindingCount = statistics.get("bindingCount");
        int consumerCount = statistics.get("consumerCount");
        return bindingCount > 0 && consumerCount > 0;
    }

    @Override
    public void deleteQueue(String queue)
            throws CommunicationException, HttpServerException {
        String url = String.join("/", jmsQueueUrl, queue);

        HttpClientResponse resp = delete(url);

        if (!isSuccess(resp)) {
            if (resp.code == 404) {
                // this is okay if we are deleting
                return;
            }
            String responseBody = null;
            if (resp.data != null && resp.data.length != 0) {
                responseBody = new String(resp.data, StandardCharsets.UTF_8);
            }
            throw new HttpServerException(
                    "Failed to get delete queue " + queue + "; broker returned "
                            + resp.code + ". Response body: " + responseBody,
                    resp.code);
        }
    }

    private HttpClientResponse putJson(String url, String jsonBody)
            throws CommunicationException {
        HttpPut put = new HttpPut(url);
        put.setEntity(new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        httpClientLock.writeLock().lock();
        try {
            return httpClient.executeRequest(put);
        } finally {
            httpClientLock.writeLock().unlock();
        }
    }

    private HttpClientResponse postJson(String url, String jsonBody)
            throws CommunicationException {
        HttpPost post = new HttpPost(url);
        post.setEntity(
                new StringEntity(jsonBody, ContentType.APPLICATION_JSON));
        httpClientLock.writeLock().lock();
        try {
            return httpClient.executeRequest(post);
        } finally {
            httpClientLock.writeLock().unlock();
        }
    }

    private HttpClientResponse get(String url) throws CommunicationException {
        HttpGet get = new HttpGet(url);
        httpClientLock.readLock().lock();
        try {
            return httpClient.executeRequest(get);
        } finally {
            httpClientLock.readLock().unlock();
        }
    }

    private HttpClientResponse delete(String url)
            throws CommunicationException {
        HttpDelete delete = new HttpDelete(url);
        httpClientLock.writeLock().lock();
        try {
            return httpClient.executeRequest(delete);
        } finally {
            httpClientLock.writeLock().unlock();
        }
    }

    private Object getJsonObject(HttpClientResponse resp)
            throws CommunicationException {
        String responseBody = new String(resp.data, StandardCharsets.UTF_8);
        try {
            BasicJsonService json = new BasicJsonService();
            return json.deserialize(responseBody, Object.class);

        } catch (JsonException e) {
            throw new CommunicationException(
                    "Unable to parse JSON response. Response body: "
                            + responseBody,
                    e);
        }
    }

    private static boolean isSuccess(HttpClientResponse response) {
        return response.code >= 200 && response.code < 300;
    }
}
