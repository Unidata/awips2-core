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

import com.raytheon.uf.common.datastore.ignite.IgniteDataStore;
import com.raytheon.uf.common.datastore.ignite.IgniteDataStoreFactory;
import com.raytheon.uf.common.datastore.ignite.IgniteServerManager;
import com.raytheon.uf.common.datastore.ignite.plugin.CachePluginRegistry;
import com.raytheon.uf.common.datastore.pypies.servlet.PyPiesServlet;

/**
 * Ignite {@link Service} which uses Jetty and {@link PyPiesServlet} to handle
 * http requests that are normally handled by PyPies and handle them with an
 * {@link IgniteDataStore} instead.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 14, 2019  7628     bsteffen  Initial creation
 * Mar 27, 2020  8071     bsteffen  Add handling for /status
 * Jun 25, 2021  8450     mapeters  Updated for centralized ignite instance management
 *
 * </pre>
 *
 * @author bsteffen
 */
public class PyPiesCompatibilityService implements Service {

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
        log.info("Starting PyPies compatibility service on port " + port
                + "...");
        server = new Server(port);

        IgniteServerManager igniteManager = new IgniteServerManager(ignite);
        IgniteDataStoreFactory factory = new IgniteDataStoreFactory(
                igniteManager, new CachePluginRegistry());

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new PyPiesServlet(factory)), "/");
        context.addServlet(
                new ServletHolder(new IgniteStatusServlet(igniteManager)),
                "/status");
        server.setHandler(context);

        server.start();
        log.info("Successfully started PyPies compatibility service");
    }

    @Override
    public void init(ServiceContext arg0) throws Exception {
        String prop = System.getProperty("thrift.stream.maxsize");
        if (prop == null) {
            System.setProperty("thrift.stream.maxsize", "200");
        }
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
