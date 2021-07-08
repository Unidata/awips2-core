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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
 * Oct 16, 2019 7724      tgurney      Replace connection string and info map
 *                                     with a single {@link JMSConnectionInfo}
 *                                     object
 * May 27, 2021 8469      dgilling     Read broker REST service port from
 *                                     BROKER_HTTP env. variable.
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
        String brokerServicePort = System.getenv("BROKER_HTTP");
        logger.info("http.server=" + httpServer);
        logger.info("broker host=" + brokerHost);
        logger.info("broker port=" + brokerPort);
        logger.info("jms.virtualhost=" + jmsVirtualHost);
        logger.info("pypies.server=" + pypiesServer);
        logger.info("server locations=" + registry);
        JMSConnectionInfo connectionInfo = createJmsConnectionInfo(brokerHost,
                brokerPort, jmsVirtualHost, brokerServicePort);
        response.setJmsConnectionInfo(connectionInfo);
        response.setHttpServer(httpServer);
        response.setPypiesServer(pypiesServer);
        response.setServerLocations(Collections.unmodifiableMap(this.registry));

        return response;
    }

    private static JMSConnectionInfo createJmsConnectionInfo(String hostname,
            String port, String vhost, String servicePort) {
        /*
         * Do not enable retry/connectdelay connection and factory will silently
         * reconnect and user will never be notified qpid is down and cave/text
         * workstation will just act like they are hung. Up to each individual
         * component that opens a connection to handle reconnect
         */
        String connTimeout = System.getProperty("utility.server.connectTimeout",
                "5000");
        String forceSyncSend = System
                .getProperty("utility.server.forceSyncSend", "true");

        Map<String, String> parameters = new HashMap<>();
        parameters.put("jms.prefetchPolicy.all", "0");
        parameters.put("jms.connectTimeout", connTimeout);
        parameters.put("jms.forceSyncSend", forceSyncSend);
        return new JMSConnectionInfo(hostname, port, vhost, servicePort,
                parameters);
    }

    @Override
    public Object register(String key, String value) throws RegistryException {
        registry.put(key, value);
        return value;
    }
}
