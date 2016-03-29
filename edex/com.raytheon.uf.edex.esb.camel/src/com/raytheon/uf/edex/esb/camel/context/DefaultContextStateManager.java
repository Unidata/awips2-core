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
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class DefaultContextStateManager implements IContextStateManager {
    private static final Set<ServiceStatus> STARTABLE_STATES = EnumSet.of(
            ServiceStatus.Stopped, ServiceStatus.Suspended,
            ServiceStatus.Suspending);

    private static final Set<ServiceStatus> SUSPENDABLE_STATES = EnumSet.of(
            ServiceStatus.Starting, ServiceStatus.Started);

    private static final Set<ServiceStatus> STOPPABLE_STATES = EnumSet.of(
            ServiceStatus.Starting, ServiceStatus.Started,
            ServiceStatus.Suspending, ServiceStatus.Suspended);

    @Override
    public boolean isContextStartable(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        return STARTABLE_STATES.contains(status)
                || (status.isStarted() && !context.isAutoStartup());
    }

    @Override
    public boolean startContext(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();

        boolean rval = status.isStarted();
        if (rval && !context.isAutoStartup()) {
            for (Route route : context.getRoutes()) {
                rval &= context.getRouteStatus(route.getId()).isStarted();
            }
        }

        if (!rval) {
            IContextStateProcessor processor = ContextManager.getInstance()
                    .getStateProcessor(context);
            if (processor != null) {
                processor.preStart();
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

                Collections.sort(routes, new Comparator<Route>() {
                    @Override
                    public int compare(Route r1, Route r2) {
                        int order1 = r1.getRouteContext().getRoute()
                                .getStartupOrder();
                        int order2 = r2.getRouteContext().getRoute()
                                .getStartupOrder();
                        return order1 - order2;
                    }
                });

                for (Route route : routes) {
                    rval &= startRoute(route);
                }

                /*
                 * clear the auto start up flag since its an initial condition
                 * only
                 */
                context.setAutoStartup(true);
            }

            if (processor != null) {
                processor.postStart();
            }
        }

        return rval;
    }

    @Override
    public boolean startRoute(Route route) throws Exception {
        String routeId = route.getId();
        CamelContext ctx = route.getRouteContext().getCamelContext();
        ServiceStatus status = ctx.getRouteStatus(routeId);
        if (STARTABLE_STATES.contains(status)) {
            ctx.startRoute(routeId);
            status = ctx.getRouteStatus(routeId);
        }

        return status.isStarted();
    }

    @Override
    public boolean isContextStoppable(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        boolean shuttingDown = ContextManager.getInstance().isShuttingDown();
        return (shuttingDown && STOPPABLE_STATES.contains(status))
                || (!shuttingDown && SUSPENDABLE_STATES.contains(status));
    }

    @Override
    public boolean stopContext(CamelContext context) throws Exception {
        ServiceStatus status = context.getStatus();
        if (isContextStoppable(context)) {
            IContextStateProcessor processor = ContextManager.getInstance()
                    .getStateProcessor(context);
            if (processor != null) {
                processor.preStop();
            }

            // a context will automatically stop all its routes
            if (ContextManager.getInstance().isShuttingDown()) {
                context.stop();
            } else {
                context.suspend();
            }

            if (processor != null) {
                processor.postStop();
            }

            status = context.getStatus();
        }

        return status.isStopped();
    }

    @Override
    public boolean stopRoute(Route route) throws Exception {
        String routeId = route.getId();
        CamelContext ctx = route.getRouteContext().getCamelContext();
        ServiceStatus status = ctx.getRouteStatus(routeId);
        if (STOPPABLE_STATES.contains(status)) {
            ctx.stopRoute(routeId);
            status = ctx.getRouteStatus(routeId);
        }

        return status.isStopped();
    }

}
