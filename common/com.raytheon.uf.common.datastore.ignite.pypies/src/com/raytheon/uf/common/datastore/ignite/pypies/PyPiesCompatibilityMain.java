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

import org.apache.ignite.Ignition;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.raytheon.uf.common.datastore.ignite.IgniteDataStore;
import com.raytheon.uf.common.datastore.ignite.IgniteDataStoreFactory;
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
        context.addServlet(new ServletHolder(
                new PyPiesServlet(new IgniteDataStoreFactory())), "/");
        context.addServlet(
                new ServletHolder(new IgniteStatusServlet(Ignition.ignite())),
                "/status");
        server.setHandler(context);

        server.start();
        server.join();
    }
}
