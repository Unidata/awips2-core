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

import java.util.Arrays;

import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.raytheon.uf.common.datastore.ignite.IIgniteConfigGenerator;
import com.raytheon.uf.common.datastore.ignite.IgniteClientManager;
import com.raytheon.uf.common.datastore.ignite.IgniteDataStore;
import com.raytheon.uf.common.datastore.ignite.IgniteDataStoreFactory;
import com.raytheon.uf.common.datastore.ignite.plugin.CachePluginRegistry;
import com.raytheon.uf.common.datastore.pypies.servlet.PyPiesServlet;

/**
 * Simple main for running a Jetty {@link Server} that uses a
 * {@link PyPiesServlet} to handle pypies requests and forwards them to an
 * {@link IgniteDataStore}
 *
 * This class is used for debugging so it doesn't have any proper configuration.
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
public class PyPiesCompatibilityMain {

    public static void main(String[] args) throws Exception {
        int port = 9586;

        Server server = new Server(port);
        ServletContextHandler context = new ServletContextHandler(
                ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        IgniteClientManager igniteManager = new IgniteClientManager(
                new IIgniteConfigGenerator() {

                    @Override
                    public IgniteConfiguration getNewConfig() {
                        return getDefaultConfig();
                    }
                });
        context.addServlet(new ServletHolder(
                new PyPiesServlet(new IgniteDataStoreFactory(igniteManager,
                        new CachePluginRegistry()))),
                "/");
        context.addServlet(
                new ServletHolder(new IgniteStatusServlet(igniteManager)),
                "/status");
        server.setHandler(context);

        server.start();
        server.join();
    }

    private static IgniteConfiguration getDefaultConfig() {
        IgniteConfiguration config = new IgniteConfiguration();
        config.setClassLoader(IgniteDataStoreFactory.class.getClassLoader());
        config.setClientMode(true);
        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
        discoSpi.setJoinTimeout(5000);
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Arrays.asList("localhost"));
        discoSpi.setIpFinder(ipFinder);
        config.setDiscoverySpi(discoSpi);

        return config;
    }
}
