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

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.camel.ServiceStatus;

import com.raytheon.uf.edex.core.IContextStateProcessor;
import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;

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
 * Jul 25, 2024 2037700    tgurney     Use EDEXRouteContext to identify internal routes
 * Jul 31, 2024 2037700    tgurney     Replace CamelContext with EDEXRouteContext
 *
 * </pre>
 *
 * @author rjpeter
 */
public class DefaultContextStateManager implements IContextStateManager {

    private static final Set<ServiceStatus> STARTABLE_STATES = EnumSet.of(
            ServiceStatus.Stopped, ServiceStatus.Suspended,
            ServiceStatus.Suspending);

    private static final Set<ServiceStatus> SUSPENDABLE_STATES = EnumSet
            .of(ServiceStatus.Starting, ServiceStatus.Started);

    private static final Set<ServiceStatus> STOPPABLE_STATES = EnumSet.of(
            ServiceStatus.Starting, ServiceStatus.Started,
            ServiceStatus.Suspending, ServiceStatus.Suspended);

    @Override
    public boolean isContextStartable(EDEXRouteContext context)
            throws Exception {
        ServiceStatus status = context.getStatus();
        return STARTABLE_STATES.contains(status);
    }

    @Override
    public boolean startContext(EDEXRouteContext context) throws Exception {
        ServiceStatus status = context.getStatus();

        boolean rval = status.isStarted();
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

            if (processorList != null) {
                for (IContextStateProcessor processor : processorList) {
                    processor.postStart();
                }
            }
        }

        return rval;
    }

    @Override
    public boolean isContextStoppable(EDEXRouteContext context)
            throws Exception {
        ServiceStatus status = context.getStatus();
        boolean shuttingDown = ContextManager.getInstance().isShuttingDown();
        return shuttingDown && STOPPABLE_STATES.contains(status)
                || !shuttingDown && SUSPENDABLE_STATES.contains(status);
    }

    @Override
    public boolean stopContext(EDEXRouteContext context) throws Exception {
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

}
