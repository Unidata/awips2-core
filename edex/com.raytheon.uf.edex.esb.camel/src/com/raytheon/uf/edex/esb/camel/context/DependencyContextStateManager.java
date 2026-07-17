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

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;

/**
 * Implementation of IContextStateManager that handles dependencies between
 * contexts so that contexts start/stop in the correct order. Can be given an
 * ExecutorService to use to start/stop dependent contexts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2014 2726       rjpeter     Initial creation
 * Mar  4, 2021 8326       tgurney     Fixes for Camel 3 API changes
 * Jul 31, 2024 2037700    tgurney     Replace CamelContext with EDEXRouteContext
 *
 * </pre>
 *
 * @author rjpeter
 */
public class DependencyContextStateManager extends DefaultContextStateManager {

    /**
     * Service to use to start/stop dependent contexts. If null, context
     * processing will happen on current thread.
     */
    protected final ExecutorService service;

    public DependencyContextStateManager() {
        this(null);
    }

    public DependencyContextStateManager(ExecutorService service) {
        this.service = service;
    }

    @Override
    public boolean isContextStartable(EDEXRouteContext context)
            throws Exception {
        if (!super.isContextStartable(context)) {
            return false;
        }

        Set<EDEXRouteContext> requiredContexts = ContextManager.getInstance()
                .getDependencyMapping(false).getRequiredContexts(context);

        if (requiredContexts != null) {
            for (EDEXRouteContext rContext : requiredContexts) {
                if (!rContext.getStatus().isStarted()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean startContext(EDEXRouteContext context) throws Exception {
        boolean rval = super.startContext(context);
        ContextManager ctxMgr = ContextManager.getInstance();

        if (rval) {
            Set<EDEXRouteContext> dContexts = ctxMgr.getDependencyMapping(false)
                    .getDependentContexts(context);
            if (dContexts != null) {
                List<Future<Boolean>> callbacks = null;

                for (final EDEXRouteContext dCtx : dContexts) {
                    final IContextStateManager stateMgr = ctxMgr
                            .getStateManager(dCtx);
                    if (stateMgr.isContextStartable(dCtx)) {
                        if (service != null) {
                            if (callbacks == null) {
                                callbacks = new LinkedList<>();
                            }

                            callbacks.add(service
                                    .submit(() -> stateMgr.startContext(dCtx)));
                        } else {
                            stateMgr.startContext(dCtx);
                        }
                    }
                }

                if (callbacks != null) {
                    for (Future<Boolean> callback : callbacks) {
                        rval &= callback.get().booleanValue();
                    }
                }
            }
        }

        return rval;
    }

    @Override
    public boolean isContextStoppable(EDEXRouteContext context)
            throws Exception {
        if (!super.isContextStoppable(context)) {
            return false;
        }

        Set<EDEXRouteContext> dContexts = ContextManager.getInstance()
                .getDependencyMapping(true).getDependentContexts(context);

        if (dContexts != null) {
            for (EDEXRouteContext dContext : dContexts) {
                /*
                 * only need to check if the context has stopped, can't have a
                 * stopped context with started routes.
                 */
                if (!dContext.getStatus().isStopped()) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean stopContext(EDEXRouteContext context) throws Exception {
        boolean rval = super.stopContext(context);
        ContextManager ctxMgr = ContextManager.getInstance();

        if (rval) {
            Set<EDEXRouteContext> rContexts = ctxMgr.getDependencyMapping(true)
                    .getRequiredContexts(context);
            if (rContexts != null) {
                List<Future<Boolean>> callbacks = null;

                for (final EDEXRouteContext rCtx : rContexts) {
                    final IContextStateManager stateMgr = ctxMgr
                            .getStateManager(rCtx);
                    if (stateMgr.isContextStoppable(rCtx)) {
                        if (service != null) {
                            if (callbacks == null) {
                                callbacks = new LinkedList<>();
                            }

                            callbacks.add(service
                                    .submit(() -> stateMgr.stopContext(rCtx)));
                        } else {
                            stateMgr.stopContext(rCtx);
                        }
                    }
                }

                if (callbacks != null) {
                    for (Future<Boolean> callback : callbacks) {
                        rval &= callback.get().booleanValue();
                    }
                }
            }
        }

        return rval;
    }
}
