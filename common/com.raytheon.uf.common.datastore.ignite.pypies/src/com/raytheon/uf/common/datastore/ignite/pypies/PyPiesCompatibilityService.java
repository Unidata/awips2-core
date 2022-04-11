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

import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastore.ignite.IgniteClusterManager;
import com.raytheon.uf.common.datastore.ignite.IgniteDataStore;
import com.raytheon.uf.common.datastore.ignite.IgniteUtils;
import com.raytheon.uf.common.datastore.pypies.servlet.PyPiesServlet;
import com.raytheon.uf.common.http.TraceForbiddingHttpServer;

/**
 * EDEX service that uses Jetty and {@link PyPiesServlet} to handle http
 * requests that are normally handled by PyPies and handle them with an
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
 * Jun 25, 2021  8450     mapeters  Updated for centralized ignite instance management,
 *                                  moved from ignite server to edex
 * Apr 05, 2022  8837     mapeters  Use TraceForbiddingHttpServer for security
 *
 * </pre>
 *
 * @author bsteffen
 */
public class PyPiesCompatibilityService
        implements ApplicationListener<ContextRefreshedEvent> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final AtomicBoolean initialized = new AtomicBoolean();

    private final IgniteClusterManager clusterManager;

    private final int port;

    public PyPiesCompatibilityService(IgniteClusterManager clusterManager,
            int port) {
        this.clusterManager = clusterManager;
        this.port = port;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (initialized.getAndSet(true)) {
            return;
        }
        if (!IgniteUtils.isIgniteActive()) {
            logger.info("Doing nothing since ignite is not active");
            return;
        }

        logger.info("Starting PyPies compatibility service on port " + port
                + "...");

        Server server = new TraceForbiddingHttpServer(port);

        IDataStoreFactory factory = DataStoreFactory.getInstance()
                .getUnderlyingFactory();

        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        context.addServlet(new ServletHolder(new PyPiesServlet(factory)), "/");
        // Status servlet is needed for IPVS to use for load balancing
        context.addServlet(
                new ServletHolder(new IgniteStatusServlet(clusterManager)),
                "/status");
        server.setHandler(context);

        try {
            server.start();
        } catch (Exception e) {
            logger.error("Error starting PyPies/ignite compatibility service",
                    e);
            return;
        }

        logger.info("Successfully started PyPies/ignite compatibility service");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Stopping server...");
                try {
                    if (server.isRunning()) {
                        server.stop();
                        server.join();
                    }
                } catch (Exception e) {
                    logger.error("Error shutting down server", e);
                }
                logger.info("Server stopped");
            }
        });
    }
}
