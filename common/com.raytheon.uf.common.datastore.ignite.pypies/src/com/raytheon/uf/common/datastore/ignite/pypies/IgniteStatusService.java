/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite.pypies;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.raytheon.uf.common.datastore.ignite.IgniteServerManager;

/**
 * Ignite {@link Service} which uses Jetty and {@link IgniteStatusServlet} to
 * report an ignite cache's status over http (http://${host}:${port}/status).
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 14, 2019  7628     bsteffen  Initial creation
 * Mar 27, 2020  8071     bsteffen  Add handling for /status
 * Jul 07, 2021  8450     mapeters  Extracted out from PyPiesCompatibilityService
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteStatusService implements Service {

    private static final long serialVersionUID = 1L;

    @IgniteInstanceResource
    private Ignite ignite;

    @LoggerResource
    private IgniteLogger log;

    private int port = 9582;

    private transient Server server;

    @Override
    public void cancel(ServiceContext arg0) {
        try {
            server.stop();
            server.join();
        } catch (Exception e) {
            log.warning("Unable to stop server.", e);
        }
    }

    @Override
    public void execute(ServiceContext arg0) throws Exception {
        server = new Server(port);

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(
                new IgniteStatusServlet(new IgniteServerManager(ignite))),
                "/status");
        server.setHandler(context);

        server.start();
    }

    @Override
    public void init(ServiceContext arg0) throws Exception {
        // do nothing
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
