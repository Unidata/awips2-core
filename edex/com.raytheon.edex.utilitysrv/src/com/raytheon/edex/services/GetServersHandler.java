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
package com.raytheon.edex.services;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.localization.msgs.GetServersRequest;
import com.raytheon.uf.common.localization.msgs.GetServersResponse;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.common.util.registry.GenericRegistry;
import com.raytheon.uf.common.util.registry.RegistryException;

/**
 * Handler class for retrieving the http and jms servers from the
 * environment.xml
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 06, 2009           mschenke     Initial creation
 * Sep 12, 2012 1167      djohnson     Add datadelivery servers.
 * Jan 14, 2013 1469      bkowal       No longer includes the hdf5 data directory
 *                                     in the response.
 * May 28, 2013 1989      njensen      Uses env variables instead of system props
 * Aug 27, 2013 2295      bkowal       Return the entire jms connection url in
 *                                     the response.
 * Dec 17, 2015 5166      kbisanz      Update logging to use SLF4J
 * Feb 02, 2017 6085      bsteffen     Enable ssl in the JMS connection.
 * Feb 09, 2017 6111      njensen      Overrode register to not return this
 * Jul 17, 2019 7724      mrichardson  Upgrade Qpid to Qpid Proton.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class GetServersHandler extends GenericRegistry<String, String>
        implements IRequestHandler<GetServersRequest> {
    
    private static final Logger logger = LoggerFactory
            .getLogger(GetServersHandler.class);

    @Override
    public GetServersResponse handleRequest(GetServersRequest request)
            throws Exception {
        GetServersResponse response = new GetServersResponse();
        String httpServer = System.getenv("HTTP_SERVER");
        String jmsVirtualHost = System.getenv("JMS_VIRTUALHOST");
        String pypiesServer = System.getenv("PYPIES_SERVER");
        String brokerHost = System.getenv("BROKER_HOST");
        String brokerPort = System.getenv("BROKER_PORT");
        String jmsConnectionString = 
                constructJMSConnectionString(brokerHost, brokerPort, jmsVirtualHost);
        Map<String, String> connectionInfo = 
                jmsConnectionInfo(brokerHost, brokerPort, jmsVirtualHost);

        logger.info("http.server=" + httpServer);
        logger.info("broker host=" + brokerHost);
        logger.info("broker port" + brokerPort);
        logger.info("jms.virtualhost=" + jmsVirtualHost);
        logger.info("pypies.server=" + pypiesServer);
        logger.info("server locations=" + registry);

        response.setConnectionInfo(connectionInfo);
        response.setHttpServer(httpServer);
        response.setJmsConnectionString(jmsConnectionString);
        response.setPypiesServer(pypiesServer);
        response.setServerLocations(Collections.unmodifiableMap(this.registry));

        return response;
    }

    private static Map<String, String> jmsConnectionInfo(String hostname,
            String port, String vhost) {
        Map<String, String> connectionInfo = new HashMap<>();
        String connTimeout = System.getProperty("utility.server.connectTimeout", "5000");
        String forceSyncSend = System.getProperty("utility.server.forceSyncSend", "true");

        connectionInfo.put("hostname", hostname);
        connectionInfo.put("port", port);
        connectionInfo.put("vhost", vhost);
        connectionInfo.put("jms.username", "guest");
        connectionInfo.put("jms.prefetchPolicy.all", "0");
        connectionInfo.put("jms.clientID", "__WSID__");
        connectionInfo.put("jms.connectTimeout", connTimeout);
        connectionInfo.put("jms.forceSyncSend", forceSyncSend);
        return connectionInfo;
    }

    // do not enable retry/connectdelay connection and factory will
    // silently reconnect and user will never be notified qpid is down
    // and cave/text workstation will just act like they are hung
    // up to each individual component that opens a connection to handle
    // reconnect
    private static String constructJMSConnectionString(String brokerHost,
            String brokerPort, String jmsVirtualHost) throws URISyntaxException {
        String connTimeout = System.getProperty("utility.server.connectTimeout", "5000");
        String forceSyncSend = System.getProperty("utility.server.forceSyncSend", "true");
        
        // build the connection String that CAVE will use
        URIBuilder uriBuilder = new URIBuilder();
        uriBuilder.setScheme("amqps");
        uriBuilder.setHost(brokerHost);
        uriBuilder.setPort(Integer.parseInt(brokerPort));
        uriBuilder.addParameter("amqp.vhost", jmsVirtualHost);
        uriBuilder.addParameter("jms.username", "guest");
        uriBuilder.addParameter("jms.prefetchPolicy.all", "0");
        uriBuilder.addParameter("jms.clientID", "__WSID__");
        uriBuilder.addParameter("jms.connectTimeout",
                connTimeout);
        uriBuilder.addParameter("jms.forceSyncSend",
                forceSyncSend);
        uriBuilder = JMSConnectionInfo.getInstance().configureSSL(uriBuilder);

        return uriBuilder.toString();
    }

    @Override
    public Object register(String key, String value) throws RegistryException {
        registry.put(key, value);
        return value;
    }
}
