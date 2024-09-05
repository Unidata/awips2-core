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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.ConfigurationException;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.component.quartz.QuartzEndpoint;
import org.apache.camel.component.timer.TimerEndpoint;
import org.apache.camel.model.RouteDefinition;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.uf.edex.core.EdexAsyncStartupBean;
import com.raytheon.uf.edex.core.EdexException;
import com.raytheon.uf.edex.core.IContextStateProcessor;
import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;

/**
 * Tracks all contexts and is used to auto determine context dependencies and
 * start/stop them in the right order. Dynamically starts/stops a clustered
 * context and its associated routes so that only one context in the cluster is
 * running. This should mainly be used for reading from topics so that only one
 * box is processing the topic data in the cluster for singleton type events.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 10, 2010  5050     rjpeter   Initial creation.
 * May 13, 2013  1989     njensen   Camel 2.11 compatibility.
 * Mar 11, 2014  2726     rjpeter   Implemented graceful shutdown.
 * Oct 27, 2016  5860     njensen   Contexts setAllowOriginalMessage to false
 * Jan 26, 2017  6092     randerso  Allow multiple context state processors per
 *                                  context
 * Jul 17, 2017  5570     tgurney   Move external route stopping to
 *                                  DefaultContextStateManager
 * Jul 28, 2017  5570     rjpeter   Fix dependency generation on shutdown
 * Mar  4, 2021  8326     tgurney   Fixes for Camel 3 API changes
 * Jun 28, 2022  8865     mapeters  Shut down default context after all others
 * Sep 26, 2022  8920     smoorthy  Add method to register multiple processors at once.
 * Jul  9, 2024  2037227  tgurney   First round of Camel 4 changes, minimum
 *                                  necessary to allow EDEX startup.
 * Jul 24, 2024  2037700  tgurney   General refactoring to support EDEXRouteContext
 * Jul 29, 2024  2037700  tgurney   Replace ContextData with shim interface (Camel 4)
 * Jul 31, 2024, 2037700  tgurney   Perform startup/shutdown on EDEXRouteContexts
 * Aug  2, 2024, 2037700  tgurney   Clustered context checking for EDEXRouteContexts
 * Aug  8, 2024  2037700  tgurney   Set readable thread names. Stop timers before
 *                                  stopping contexts.
 * Sep  5, 2024  2037700  tgurney   Manually inject the Camel context via spring
 *                                  instead of using CamelContextAware interface
 *
 *
 * </pre>
 *
 * @author rjpeter
 */
public class ContextManager {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ContextManager.class);

    /** Thread number for context start/stop jobs */
    private static final AtomicInteger threadNum = new AtomicInteger();

    private static ContextManager instance = new ContextManager();

    /** The one application-wide CamelContext */
    private CamelContext camelContext;

    /** All route contexts known to this instance of EDEX */
    private final Set<EDEXRouteContext> routeContexts = new HashSet<>();

    /**
     * Map of route IDs to endpoint URIs. This map exists for fast lookup of
     * endpoint URIs and to guarantee that no routes with duplicate names are
     * created.
     */
    private final Map<String, String> routeIdURIMap = new HashMap<>();

    /**
     * Must hold this lock while accessing the routeContexts or routeIDURIMap
     * fields. Only take the write lock when adding or removing items. Otherwise
     * take the read lock.
     */
    private final ReadWriteLock routesLock = new ReentrantReadWriteLock(true);

    /**
     * Service used for start up and shut down threading.
     */
    private final ExecutorService service = Executors.newCachedThreadPool();

    /**
     * State Manager for all contexts that are not clustered.
     */
    private final IContextStateManager defaultStateManager = new DependencyContextStateManager(
            service);

    /**
     * State Manager used for all clustered contexts.
     */
    private final IContextStateManager clusteredStateManager = new ClusteredContextStateManager(
            service);

    /**
     * Map of context processors that have been registered for a given context.
     * Used to allow contexts to do custom work on startup/shutdown.
     */
    private final Map<EDEXRouteContext, List<IContextStateProcessor>> contextProcessors = new HashMap<>();

    /**
     * Cluster lock timeout for clustered contexts.
     */
    private int timeOutMillis;

    /**
     * Flag to control shutting down the jvm. This handles shutdown being called
     * during startup to short circuit startup.
     */
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);

    /**
     * Dependency mappings for all camel contexts in the spring container.
     */
    private volatile ContextDependencyMapping dependencyMapping = null;

    /**
     * Collection of beans required for startup that can be initialized off the
     * main thread. EDEX will not call startContexts until all these beans have
     * completed their initialization.
     */
    private final Set<EdexAsyncStartupBean> asyncStartupBeans = new HashSet<>();

    /**
     * @return the singleton ContextManager instance
     */
    public static ContextManager getInstance() {
        return instance;
    }

    private ContextManager() {
    }

    /**
     * @param routeId
     * @return the uri for the consumer endpoint of the route with the specified
     *         routeId.
     * @throws EdexException
     */
    public String getEndpointUriForRouteId(String routeId)
            throws EdexException {
        routesLock.readLock().lock();
        String uri = null;
        try {
            uri = routeIdURIMap.get(routeId);
        } finally {
            routesLock.readLock().unlock();
        }
        if (uri == null) {
            throw new EdexException("Route id " + routeId
                    + " not found.  Check loaded spring configurations.");
        }
        return uri;
    }

    /**
     * Get the {@link IContextStateManager} for the passed
     * {@code EDEXRouteContext}.
     *
     * @param context
     * @return
     */
    protected IContextStateManager getStateManager(EDEXRouteContext context) {
        if (context.isClustered()) {
            return clusteredStateManager;
        }

        return defaultStateManager;
    }

    /**
     * Get the list of {@link IContextStateProcessor} for the specified
     * {@code EDEXRouteContext}.
     *
     * @param context
     *            the EDEXRouteContext
     * @return this list of IContextStateProcessors
     */
    public List<IContextStateProcessor> getStateProcessor(
            EDEXRouteContext context) {
        return contextProcessors.get(context);
    }

    /**
     * Get the {@link ContextDependencyMapping} for all contexts.
     *
     * @param suppressExceptions
     * @return the ContextDependencyMapping
     * @throws ConfigurationException
     */
    public ContextDependencyMapping getDependencyMapping(
            boolean suppressExceptions) throws ConfigurationException {
        if (dependencyMapping == null) {
            routesLock.readLock().lock();
            try {
                if (dependencyMapping == null) {
                    /*
                     * TODO race condition is still possible, not a big deal in
                     * this case, but we would prefer to avoid it.
                     */
                    long t0 = System.currentTimeMillis();
                    dependencyMapping = new ContextDependencyMapping(
                            routeContexts, suppressExceptions);
                    long t1 = System.currentTimeMillis();
                    statusHandler.info("Took " + (t1 - t0)
                            + "ms to generate depedency mapping.");
                }
            } finally {
                routesLock.readLock().unlock();
            }
        }
        return dependencyMapping;

    }

    /**
     * Force clear the generated dependency mapping. Should be called when new
     * routes are dynamically added to the system.
     */
    public void clearDependencyMapping() {
        dependencyMapping = null;
    }

    /**
     * Starts all routes for all contexts. If a route fails to start the entire
     * jvm will be shutdown.
     */
    public void startContexts() {
        statusHandler.info("Context Manager starting contexts");

        camelContext.start();
        routesLock.readLock().lock();
        try {
            List<Future<?>> callbacks = new LinkedList<>();
            for (final EDEXRouteContext context : routeContexts) {
                final IContextStateManager stateManager = getStateManager(
                        context);
                if (stateManager.isContextStartable(context)) {
                    /*
                     * Have the ExecutorService start the context to allow for
                     * quicker startup. Only the contexts with no dependencies
                     * are started from here. The state manager is responsible
                     * for starting any contexts that depend on a context after
                     * that context is started.
                     */
                    callbacks.add(service.submit(new StartContext(context)));
                }
            }

            /*
             * Wait for contexts to start. It is not necessary to check any
             * statuses since a thread that fails to start its context will
             * cause the whole JVM to exit.
             */
            for (Future<?> callback : callbacks) {
                callback.get();
            }

        } catch (Throwable e) {
            statusHandler.fatal(
                    "Error occurred starting contexts, shutting down", e);
            System.exit(1);
        } finally {
            routesLock.readLock().unlock();
        }
    }

    private class StartContext implements Runnable {

        private EDEXRouteContext context;

        public StartContext(EDEXRouteContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            int id = threadNum.getAndIncrement();
            try {
                Thread.currentThread().setName(
                        "EDEXContext-start-" + context.getName() + "-" + id);
                IContextStateManager stateManager = getStateManager(context);
                if (!stateManager.startContext(context)) {
                    statusHandler
                            .error(context + " failed to start, shutting down");
                    System.exit(1);
                }
            } catch (Throwable e) {
                statusHandler.fatal("Error occurred starting " + context, e);
                System.exit(1);
            } finally {
                try {
                    Thread.currentThread().setName("EDEXContext-idle-" + id);
                } catch (Exception e) {
                    statusHandler.debug(e.getLocalizedMessage(), e);
                }
            }
        }

    }

    /**
     * Register a context state processor to be called on start/stop of the
     * context.
     *
     * @param context
     * @param processor
     * @return this ContextManager
     */
    public ContextManager registerContextStateProcessor(
            final EDEXRouteContext context,
            final IContextStateProcessor processor) {

        List<IContextStateProcessor> processorList = contextProcessors
                .get(context);

        if (processorList == null) {
            processorList = new LinkedList<>();
            contextProcessors.put(context, processorList);
        }

        processorList.add(processor);

        return this;
    }

    /**
     * Register multiple context state processors to be called on start/stop of
     * the context.
     *
     * @param context
     * @param processors
     * @return this ContextManager
     */
    public ContextManager registerContextStateProcessor(
            final EDEXRouteContext context,
            final IContextStateProcessor... processors) {

        List<IContextStateProcessor> processorList = contextProcessors
                .get(context);

        if (processorList == null) {
            processorList = new LinkedList<>();
            contextProcessors.put(context, processorList);
        }

        for (IContextStateProcessor processor : processors) {
            processorList.add(processor);
        }
        return this;
    }

    /**
     * Stops all contexts. Note this method can only be called once for the life
     * of the jvm and will gracefully shut down all of camel.
     */
    public void stopContexts() {
        /*
         * flag to ensure no one else runs shutdown also stops
         * checkClusteredContext from starting contexts once shutdown has been
         * initiated
         */
        if (shuttingDown.compareAndSet(false, true)) {
            /*
             * clear the dependency mapping to force a fresh mapping of any
             * runtime dependencies.
             */
            clearDependencyMapping();

            statusHandler.info("Context Manager stopping contexts");

            routesLock.readLock().lock();
            try {
                /*
                 * Stopping a route does not stop any timer that triggers it so
                 * we have to stop all timers separately.
                 */
                for (Endpoint e : camelContext.getEndpoints()) {
                    if (e instanceof QuartzEndpoint
                            || e instanceof TimerEndpoint) {
                        e.stop();
                    }
                }
                List<Future<Pair<EDEXRouteContext, Boolean>>> callbacks = new LinkedList<>();

                for (EDEXRouteContext context : routeContexts) {
                    callbacks.add(service.submit(new StopContext(context)));
                }

                List<EDEXRouteContext> failures = waitForCallbacks(callbacks,
                        "Waiting for contexts to shutdown: ", 1000);

                for (EDEXRouteContext failure : failures) {
                    statusHandler.error("Context [" + failure.getName()
                            + "] had a failure trying to stop");
                }
            } catch (Throwable e) {
                statusHandler.error("Error occurred during shutdown", e);
            } finally {
                routesLock.readLock().unlock();
                camelContext.stop();
            }
        }
    }

    /**
     * Private Callable for stopping a context.
     */
    private class StopContext
            implements Callable<Pair<EDEXRouteContext, Boolean>> {
        private final EDEXRouteContext context;

        private StopContext(EDEXRouteContext context) {
            this.context = context;
        }

        @Override
        public Pair<EDEXRouteContext, Boolean> call() throws Exception {
            int id = threadNum.getAndIncrement();
            try {
                Thread.currentThread().setName(
                        "EDEXContext-stop-" + context.getName() + "-" + id);
                boolean rval = false;
                IContextStateManager stateManager = getStateManager(context);

                if (stateManager.isContextStoppable(context)) {
                    try {
                        statusHandler.info("Stopping context " + context);
                        rval = stateManager.stopContext(context);

                        if (!rval) {
                            statusHandler.error(context + " failed to stop");
                        }
                    } catch (Throwable e) {
                        statusHandler
                                .fatal("Error occurred stopping " + context, e);
                    }
                } else {
                    /*
                     * dependency context that will be called by a future
                     * shutdown after its dependencies have shut down
                     */
                    rval = true;
                }

                return new Pair<>(context, rval);
            } finally {
                try {
                    Thread.currentThread().setName("EDEXContext-idle-" + id);
                } catch (Exception e) {
                    statusHandler.debug(e.getLocalizedMessage(), e);
                }
            }
        }
    }

    /**
     * Waits for all callbacks to finish printing a periodic message with number
     * of remaining callbacks. Returns a list of contexts that had a failure
     * status.
     *
     * @param callbacks
     * @param message
     * @param sleepInterval
     * @return
     */
    private static List<EDEXRouteContext> waitForCallbacks(
            List<Future<Pair<EDEXRouteContext, Boolean>>> callbacks,
            String message, long sleepInterval) {
        statusHandler.info(message + callbacks.size() + " remaining");
        List<EDEXRouteContext> failures = new LinkedList<>();

        while (!callbacks.isEmpty()) {
            boolean foundOne = false;

            Iterator<Future<Pair<EDEXRouteContext, Boolean>>> callbackIter = callbacks
                    .iterator();
            while (callbackIter.hasNext()) {
                Future<Pair<EDEXRouteContext, Boolean>> callback = callbackIter
                        .next();
                if (callback.isDone()) {
                    foundOne = true;
                    callbackIter.remove();
                    try {
                        Pair<EDEXRouteContext, Boolean> val = callback.get();
                        if (!val.getSecond().booleanValue()) {
                            failures.add(val.getFirst());
                        }
                    } catch (Exception e) {
                        statusHandler.error("Failure in callback task", e);
                    }
                }
            }

            if (!foundOne) {
                statusHandler.info(message + callbacks.size() + " remaining");
                try {
                    Thread.sleep(sleepInterval);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }

        return failures;
    }

    private Set<EDEXRouteContext> getClusteredContexts() {
        Set<EDEXRouteContext> rval = new HashSet<>();
        routesLock.readLock().lock();
        try {
            for (EDEXRouteContext ctx : routeContexts) {
                if (ctx.isClustered()) {
                    rval.add(ctx);
                }
            }
        } finally {
            routesLock.readLock().unlock();
        }
        return rval;
    }

    /**
     * Checks the clustered contexts. If context is not running in the cluster
     * the context will be started.
     */
    public void checkClusteredContexts() {
        if (shuttingDown.get()) {
            return;
        }
        Set<EDEXRouteContext> clusteredContexts = getClusteredContexts();
        for (EDEXRouteContext context : clusteredContexts) {
            if (shuttingDown.get()) {
                return;
            }
            boolean activateRoute = true;
            try {
                IContextStateManager stateManager = getStateManager(context);

                if (stateManager.isContextStartable(context)) {
                    stateManager.startContext(context);
                } else if (stateManager.isContextStoppable(context)) {
                    activateRoute = false;
                    stateManager.stopContext(context);
                }
            } catch (Exception e) {
                StringBuilder msg = new StringBuilder();
                msg.append("Failed to ");
                if (activateRoute) {
                    msg.append("start ");
                } else {
                    msg.append("stop ");
                }
                msg.append("context ");
                msg.append(context.getName());
                statusHandler.handle(Priority.ERROR, msg.toString(), e);
            }
        }
    }

    /**
     * @return the timeout in milliseconds
     */
    public int getTimeOutMillis() {
        return timeOutMillis;
    }

    /**
     * Sets the time out
     *
     * @param timeOutMillis
     *            the time out in milliseconds
     */
    public void setTimeOutMillis(int timeOutMillis) {
        this.timeOutMillis = timeOutMillis;
    }

    /**
     * @return true if shutting down
     */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * Register the provided bean as an {@link EdexAsyncStartupBean}. EDEX will
     * not start its contexts until all registered async startup beans have
     * completed initialization.
     *
     * @param asyncBean
     *            {@code EdexAsyncStartupBean} instance to register
     * @return Reference to this {@code ContextManager} instance.
     */
    public ContextManager registerAsyncStartupBean(
            final EdexAsyncStartupBean asyncBean) {
        asyncStartupBeans.add(asyncBean);
        return this;
    }

    /**
     * Poll all the async startup beans and determine if they've all completed
     * their initialization or not.
     *
     * @return {@code true} if all beans have completed initialization,
     *         {@code false} if they have not.
     */
    public boolean readyToStartContexts() {
        for (EdexAsyncStartupBean bean : asyncStartupBeans) {
            if (!bean.isDone()) {
                return false;
            }
        }

        return true;
    }

    public void registerRouteContext(EDEXRouteContext routeContext) {
        routesLock.writeLock().lock();
        try {
            if (routeContexts.add(routeContext)) {
                for (RouteDefinition r : routeContext.getRouteDefs()) {
                    String prev = routeIdURIMap.put(r.getId(),
                            r.getEndpointUrl());
                    if (prev != null) {
                        throw new RuntimeException("Duplicate route ID '"
                                + r.getId()
                                + "'. Route IDs must be globally unique.");
                    }
                }
            }
        } finally {
            routesLock.writeLock().unlock();
        }
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    /**
     * Only for Spring to inject the CamelContext. This makes Spring aware that
     * ContextManager depends on the CamelContext.
     *
     * If instead we were to use the CamelContextAware interface, Spring would
     * not be aware of this dependency relationship, which can lead to
     * situations where ContextManager is being accessed before the CamelContext
     * exists, which is potentially catastrophic.
     */
    public void setCamelContext(CamelContext camelContext) {
        this.camelContext = camelContext;
    }
}
