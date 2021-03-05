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
package com.raytheon.uf.edex.distribution;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.RecipientList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this bean is to load a series of XML files from localization
 * for each plugin registering itself with this bean and route messages based on
 * those XML files. The route method will provide a list of destinations based
 * on a header (or filename) and the associated plugin specified regex patterns.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 16, 2009           brockwoo  Initial creation
 * Jun 08, 2010  4647     bphillip  Added automatic pattern refreshing
 * Sep 01, 2010  4293     cjeanbap  Logging of unknown Weather Products.
 * Feb 27, 2013  1638     mschenke  Cleaned up localization code to fix null
 *                                  pointer when no distribution files present
 * Mar 19, 2013  1794     djohnson  PatternWrapper is immutable, add toString()
 *                                  to it for debugging.
 * Aug 19, 2013  2257     bkowal    edexBridge to qpid 0.18 upgrade
 * Aug 30, 2013  2163     bkowal    edexBridge to qpid 0.18 RHEL6 upgrade
 * Sep 06, 2013  2327     rjpeter   Updated to use DistributionPatterns.
 * May 09, 2014  3151     bclement  changed registration so only required
 *                                  plugins throw exception on error
 * Dec 11, 2015  5166     kbisanz   Update logging to use SLF4J
 * Aug 30, 2017  6381     randerso  Fix ClassCastException when no header is
 *                                  present
 * Oct 10, 2019  7724     randerso  Fix to be compatible with javax.jms
 * Mar  4, 2021  8326     tgurney   Fix for Camel 3 removal of fault API
 *
 * </pre>
 *
 * @author brockwoo
 */
public class DistributionSrv {
    private static final String SUBJECT_HEADER = "JMSType";

    private static final String MESSAGE_HEADER = "header";

    protected Logger logger = LoggerFactory.getLogger("Ingest");

    protected Logger routeFailedLogger = LoggerFactory
            .getLogger("RouteFailedLog");

    private final ConcurrentMap<String, String> pluginRoutes = new ConcurrentHashMap<>();

    /**
     * Allows a plugin to register itself with this bean.
     *
     * @param pluginName
     *            the plugin name
     * @param destination
     *            a destination to send this message to
     * @return an instance of this bean
     * @throws DistributionException
     */
    public DistributionSrv register(String pluginName, String destination)
            throws DistributionException {
        return this.register(pluginName, destination, false);
    }

    /**
     * @see #register(String, String)
     * @param pluginName
     * @param destination
     * @param required
     *            true if an exception should be thrown if plugin does not have
     *            any valid distribution patterns
     * @return
     * @throws DistributionException
     */
    public DistributionSrv register(String pluginName, String destination,
            boolean required) throws DistributionException {
        if (!DistributionPatterns.getInstance()
                .hasPatternsForPlugin(pluginName)) {
            String msg = "Plugin " + pluginName
                    + " does not have a valid distribution patterns file in localization.";
            if (required) {
                throw new DistributionException(msg);
            } else {
                logger.error(msg);
            }
        }
        pluginRoutes.put(pluginName, destination);
        return this;
    }

    /**
     * Generates a list of destinations for this message based on the header (or
     * filename if the header is not available).
     *
     * @param exchange
     *            The exchange object
     * @return a list of destinations
     */
    @RecipientList
    public List<String> route(Exchange exchange) {
        Message in = exchange.getIn();
        // determine if the header is in the qpid subject field?
        String header = (String) in.getHeader(SUBJECT_HEADER);
        if (header != null) {
            // make the qpid subject the header so that everything downstream
            // will be able to read it as the header.
            in.setHeader(MESSAGE_HEADER, header);
        }

        header = (String) in.getHeader(MESSAGE_HEADER);
        Object payload = in.getBody();
        String bodyString = null;
        if (payload instanceof byte[]) {
            bodyString = new String((byte[]) payload);
        } else if (payload instanceof String) {
            bodyString = (String) payload;
        }
        File file = new File(bodyString);
        if (!file.exists()) {
            logger.error("File does not exist : " + bodyString);
            exchange.setRouteStop(true);
        } else {
            in.setHeader("ingestFileName", file.toString());
        }
        boolean unroutedFlag = true;
        if (header == null) {
            // No header entry so will try and use the filename instead
            header = bodyString;
        }

        List<String> plugins = DistributionPatterns.getInstance()
                .getMatchingPlugins(header, pluginRoutes.keySet());
        List<String> routes = new ArrayList<>(plugins.size());
        StringBuilder pluginNames = new StringBuilder(plugins.size() * 8);
        for (String plugin : plugins) {
            String route = pluginRoutes.get(plugin);
            if (route != null) {
                if (pluginNames.length() != 0) {
                    pluginNames.append(",");
                }
                pluginNames.append(plugin);
                routes.add(route);
                unroutedFlag = false;
            } else if (logger.isDebugEnabled()) {
                logger.debug("No route registered for plugin: " + plugin);
            }
        }

        if (unroutedFlag) {
            // append WMO header/filename to failed route logger
            // using warn instead of error; app can continue
            routeFailedLogger.warn(header);
        }

        in.setHeader("pluginName", pluginNames.toString());
        return routes;
    }
}
