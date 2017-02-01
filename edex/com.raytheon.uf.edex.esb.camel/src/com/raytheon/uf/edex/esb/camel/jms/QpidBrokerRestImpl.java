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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.comm.HttpClient.HttpClientResponse;
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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Apr 04, 2014  2694     randerso  Converted python implementation to Java
 * Jan 25, 2017  6092     randerso  Renamed and added queueReady method
 *
 * </pre>
 *
 * @author randerso
 */

public class QpidBrokerRestImpl implements IBrokerRestProvider {
    private static final String JMS_CONNECTIONS_URL = "JMS_CONNECTIONS_URL";

    private static final String JMS_QUEUE_URL = "JMS_QUEUE_URL";

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
        HttpClientResponse response = HttpClient.getInstance()
                .executeRequest(request);
        if (!response.isSuccess()) {
            String msg = String.format("Broker returned %d %s", response.code,
                    new String(response.data));

            throw new HttpServerException(msg, response.code);
        }

        List<String> resultSet = new ArrayList<>();
        String jsonStr = new String(response.data);
        try {
            BasicJsonService json = new BasicJsonService();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) json
                    .deserialize(jsonStr, Object.class);

            for (Map<String, Object> statDict : jsonObjList) {
                String clientId = (String) statDict.get("clientId");
                if (clientId != null) {
                    resultSet.add(clientId);
                }
            }
        } catch (JsonException e) {
            throw new CommunicationException(
                    "Unable to parse response " + jsonStr, e);
        }
        return resultSet;
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
        HttpClientResponse response = HttpClient.getInstance()
                .executeRequest(request);

        if (response.isSuccess()) {
            String jsonStr = new String(response.data);
            try {
                BasicJsonService json = new BasicJsonService();

                @SuppressWarnings("unchecked")
                List<Map<String, Object>> jsonObjList = (List<Map<String, Object>>) json
                        .deserialize(jsonStr, Object.class);

                @SuppressWarnings("unchecked")
                Map<String, Integer> statistics = (Map<String, Integer>) jsonObjList
                        .get(0).get("statistics");
                int bindingCount = statistics.get("bindingCount");
                int consumerCount = statistics.get("consumerCount");
                return (bindingCount > 0) && (consumerCount > 0);

            } catch (JsonException e) {
                throw new CommunicationException(
                        "Unable to parse response " + jsonStr, e);
            }
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
        HttpClientResponse response = HttpClient.getInstance()
                .executeRequest(request);
        if (!response.isSuccess()) {
            String msg = String.format("Broker returned %d %s", response.code,
                    new String(response.data));

            throw new HttpServerException(msg, response.code);
        }
    }
}
