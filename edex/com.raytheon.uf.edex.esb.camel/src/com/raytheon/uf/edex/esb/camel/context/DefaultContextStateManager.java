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
package com.raytheon.uf.edex.esb.camel.context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.ServiceStatus;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.uf.edex.core.IContextStateProcessor;

/**
 * Implementation of IContextStateManager that does basic validation of context
 * status as well as handling IContextStateProcessor for startup/shutdown of
 * contexts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2014 2726       rjpeter     Initial creation
 * Mar 14, 2016 DR 18533   D. Friedman Resume instead of starting suspended contexts.
 * Mar 21, 2016 3290       tgurney     Enforce startup order on manual route
 *                                     startup
 * Jan 26, 2017 6092       randerso    Allow multiple context state processors per context
 * Jul 17, 2017 5570       tgurney     Always stop external routes first
 * Mar  4, 2021 8326       tgurney     Fixes for Camel 3 API changes
 *
 * </pre>
 *
 * @author rjpeter
 */
public class DefaultContextStateManager implements IContextStateManager {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DefaultContextStateManager.class);

    private static final Set<ServiceStatus> STARTABLE_STATES = EnumSet.of(
            ServiceStatus.Stopped, ServiceStatus.Suspended,
            ServiceStatus.Suspending);

    private static final Set<ServiceStatus> SUSPENDABLE_STATES = EnumSet
            .of(ServiceStatus.Starting, ServiceStatus.Started);

    private static final Set<ServiceStatus> STOPPABLE_STATES = EnumSet.of(
            ServiceStatus.Starting, ServiceStatus.Started,
            ServiceStatus.Suspending, ServiceStatus.Suspended);

    @Override
    public boolean isContextStartable(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        return STARTABLE_STATES.contains(status)
                || status.isStarted() && !context.isAutoStartup();
    }

    @Override
    public boolean startContext(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();

        boolean rval = status.isStarted();
        if (rval && !context.isAutoStartup()) {
            for (Route route : context.getRoutes()) {
                rval &= context.getRouteController()
                        .getRouteStatus(route.getId()).isStarted();
            }
        }

        if (!rval) {
            List<IContextStateProcessor> processorList = ContextManager
                    .getInstance().getStateProcessor(context);

            if (processorList != null) {
                for (IContextStateProcessor processor : processorList) {
                    processor.preStart();
                }
            }

            if (status == ServiceStatus.Suspended) {
                context.resume();
            } else {
                context.start();
            }
            rval = context.getStatus().isStarted();

            /*
             * if a context has autoStartup = false, all of its routes are
             * started on the second time context.start is called, adding route
             * check for future proofing just in case.
             */
            if (!context.isAutoStartup()) {
                List<Route> routes = new ArrayList<>();
                routes.addAll(context.getRoutes());

                Collections.sort(routes,
                        Comparator.comparingInt(r -> r.getStartupOrder()));
                for (Route route : routes) {
                    rval &= startRoute(route);
                }

                /*
                 * clear the auto start up flag since its an initial condition
                 * only
                 */
                context.setAutoStartup(true);
            }

            if (processorList != null) {
                for (IContextStateProcessor processor : processorList) {
                    processor.postStart();
                }
            }
        }

        return rval;
    }

    @Override
    public boolean startRoute(Route route) throws Exception {
        String routeId = route.getId();
        CamelContext ctx = route.getCamelContext();
        ServiceStatus status = ctx.getRouteController().getRouteStatus(routeId);
        if (STARTABLE_STATES.contains(status)) {
            ctx.getRouteController().startRoute(routeId);
            status = ctx.getRouteController().getRouteStatus(routeId);
        }

        return status.isStarted();
    }

    @Override
    public boolean isContextStoppable(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        boolean shuttingDown = ContextManager.getInstance().isShuttingDown();
        return shuttingDown && STOPPABLE_STATES.contains(status)
                || !shuttingDown && SUSPENDABLE_STATES.contains(status);
    }

    @Override
    public boolean stopContext(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        boolean rval = true;
        if (isContextStoppable(context)) {
            List<IContextStateProcessor> processorList = ContextManager
                    .getInstance().getStateProcessor(context);

            if (processorList != null) {
                for (IContextStateProcessor processor : processorList) {
                    processor.preStop();
                }
            }

            if (ContextManager.getInstance().isShuttingDown()) {
                // begin shutting down external routes
                List<Route> routes = context.getRoutes();
                for (Route route : routes) {
                    String uri = route.getEndpoint().getEndpointUri();
                    Pair<String, String> typeAndName = ContextData
                            .getEndpointTypeAndName(uri);
                    String type = typeAndName.getFirst();
                    if (!ContextManager.INTERNAL_ENDPOINT_TYPES
                            .contains(type)) {
                        try {
                            statusHandler.info(
                                    "Stopping route [" + route.getId() + "]");
                            rval &= stopRoute(route);
                        } catch (Exception e) {
                            statusHandler
                                    .error("Error occurred Stopping route: "
                                            + route.getId(), e);
                        }
                    }
                }
                context.stop();
            } else {
                context.suspend();
            }

            if (processorList != null) {
                for (IContextStateProcessor processor : processorList) {
                    processor.postStop();
                }
            }

            status = context.getStatus();
        }

        rval &= status.isStopped();
        return rval;
    }

    @Override
    public boolean stopRoute(Route route) throws Exception {
        String routeId = route.getId();
        CamelContext ctx = route.getCamelContext();
        ServiceStatus status = ctx.getRouteController().getRouteStatus(routeId);
        if (STOPPABLE_STATES.contains(status)) {
            ctx.getRouteController().stopRoute(routeId);
            status = ctx.getRouteController().getRouteStatus(routeId);
        }

        return status.isStopped();
    }

}
