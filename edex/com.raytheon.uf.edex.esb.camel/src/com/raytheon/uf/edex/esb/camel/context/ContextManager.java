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
import java.util.Arrays;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.ConfigurationException;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextAware;
import org.apache.camel.CamelContextLifecycle;
import org.apache.camel.Route;
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
 * running. This should mainly be used for reading from topics so that only box
 * is processing the topic data in the cluster for singleton type events.
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
 *
 * </pre>
 *
 * @author rjpeter
 */
public class ContextManager implements CamelContextAware {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ContextManager.class);

    private static ContextManager instance = new ContextManager();

    /** The one application-wide CamelContext */
    private CamelContext theCamelContext;

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

    /*
     * TODO Camel 4 - remove because it is now always empty. Instead we will
     * call EDEXRouteContext.isClustered
     */
    private final Set<CamelContext> clusteredContexts = new HashSet<>();

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
     * Used to allow contexts to do custom worn on startup/shutdown.
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
     * Dependency mappings for all camel contexts in the spring container. This
     * should only be changed in a sync block. Otherwise mark as volatile.
     */
    private ContextDependencyMapping dependencyMapping = null;

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

    public ContextData getContextData() {
        return this::getCamelContext;
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
     * Get the {@link IContextStateManager} for the passed {@code CamelContext}.
     *
     * @param context
     * @return
     */
    protected IContextStateManager getStateManager(CamelContext context) {
        if (clusteredContexts.contains(context)) {
            return clusteredStateManager;
        }

        return defaultStateManager;
    }

    /**
     * Get the list of {@link IContextStateProcessor} for the specified
     * {@code CamelContext}.
     *
     * @param context
     *            the CamelContext
     * @return this list of IContextStateProcessors
     */
    public List<IContextStateProcessor> getStateProcessor(
            CamelContextLifecycle context) {
        /*
         * TODO Camel 4 - take EDEXRouteContext as argument. CamelContexts will
         * not be passed into this method anymore.
         */
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
        synchronized (this) {
            if (dependencyMapping == null) {
                long t0 = System.currentTimeMillis();
                dependencyMapping = new ContextDependencyMapping(
                        getContextData(), suppressExceptions);
                long t1 = System.currentTimeMillis();
                statusHandler.info("Took " + (t1 - t0)
                        + "ms to generate depedency mapping.");
            }
        }

        return dependencyMapping;

    }

    /**
     * Force clear the generated dependency mapping. Should be called when new
     * routes are dynamically added to the system.
     */
    public void clearDependencyMapping() {
        synchronized (this) {
            dependencyMapping = null;
        }
    }

    /**
     * Starts all routes for all contexts. If a route fails to start the entire
     * jvm will be shutdown.
     */
    public void startContexts() {
        statusHandler.info("Context Manager starting contexts");

        try {
            ContextData cxtData = getContextData();

            for (final CamelContext context : cxtData.getContexts()) {
                /*
                 * Enforce startup order so that internal endpoints start first
                 * and shutdown last. Each route must have a unique number under
                 * 1000. Camel documentation doesn't state if numbers can be
                 * negative or not. Order is reverse of how they are found in
                 * the file with internal types going first followed by external
                 * types.
                 */
                int externalCount = 999;
                int internalCount = externalCount - context.getRoutes().size();

                for (Route route : context.getRoutes()) {
                    if (EDEXRouteContext.routeIsInternal(route)) {
                        route.setStartupOrder(internalCount);
                        internalCount--;
                    } else {
                        route.setStartupOrder(externalCount);
                        externalCount--;
                    }
                }
            }

            List<Future<?>> callbacks = new LinkedList<>();
            for (final CamelContext context : cxtData.getContexts()) {
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
        }
    }

    private class StartContext implements Runnable {

        private CamelContext context;

        public StartContext(CamelContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            try {
                IContextStateManager stateManager = getStateManager(context);
                if (!stateManager.startContext(context)) {
                    statusHandler.error("Context [" + context.getName()
                            + "] failed to start, shutting down");
                    System.exit(1);
                }
            } catch (Throwable e) {
                statusHandler.fatal(
                        "Error occurred starting context: " + context.getName(),
                        e);
                System.exit(1);
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

            try {
                ContextData ctxData = getContextData();
                List<CamelContext> contexts = ctxData.getContexts();
                List<Future<Pair<CamelContext, Boolean>>> callbacks = new LinkedList<>();

                /*
                 * Shut down all contexts except default, then shut down
                 * default. Default is used for generic actions like sending
                 * messages outside JVM in MessageProducer, which other contexts
                 * may want to do during shutdown.
                 */
                CamelContext defaultContext = ctxData.getDefaultContext();
                for (final CamelContext context : contexts) {
                    if (context != defaultContext) {
                        callbacks.add(service.submit(new StopContext(context)));
                    }
                }

                List<CamelContext> failures = waitForCallbacks(callbacks,
                        "Waiting for contexts to shutdown: ", 1000);

                if (defaultContext != null) {
                    Future<Pair<CamelContext, Boolean>> callback = service
                            .submit(new StopContext(defaultContext));

                    List<Future<Pair<CamelContext, Boolean>>> defaultCallbacks = new ArrayList<>(
                            Arrays.asList(callback));
                    failures.addAll(waitForCallbacks(defaultCallbacks,
                            "Waiting for default contexts to shutdown: ",
                            1000));
                }

                for (CamelContext failure : failures) {
                    statusHandler.error("Context [" + failure.getName()
                            + "] had a failure trying to stop");
                }
            } catch (Throwable e) {
                statusHandler.error("Error occurred during shutdown", e);
            }
        }
    }

    /**
     * Private Callable for stopping a context.
     */
    private class StopContext implements Callable<Pair<CamelContext, Boolean>> {
        private final CamelContext context;

        private StopContext(CamelContext context) {
            this.context = context;
        }

        @Override
        public Pair<CamelContext, Boolean> call() throws Exception {
            boolean rval = false;
            IContextStateManager stateManager = getStateManager(context);

            if (stateManager.isContextStoppable(context)) {
                try {
                    statusHandler.info(
                            "Stopping context [" + context.getName() + "]");
                    rval = stateManager.stopContext(context);

                    if (!rval) {
                        statusHandler.error("Context [" + context.getName()
                                + "] failed to stop");
                    }
                } catch (Throwable e) {
                    statusHandler.fatal("Error occurred stopping context: "
                            + context.getName(), e);
                }
            } else {
                /*
                 * dependency context that will be called by a future shutdown
                 * after its dependencies have shut down
                 */
                rval = true;
            }

            return new Pair<>(context, rval);
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
    private static List<CamelContext> waitForCallbacks(
            List<Future<Pair<CamelContext, Boolean>>> callbacks, String message,
            long sleepInterval) {
        statusHandler.info(message + callbacks.size() + " remaining");
        List<CamelContext> failures = new LinkedList<>();

        while (!callbacks.isEmpty()) {
            boolean foundOne = false;

            Iterator<Future<Pair<CamelContext, Boolean>>> callbackIter = callbacks
                    .iterator();
            while (callbackIter.hasNext()) {
                Future<Pair<CamelContext, Boolean>> callback = callbackIter
                        .next();
                if (callback.isDone()) {
                    foundOne = true;
                    callbackIter.remove();
                    try {
                        Pair<CamelContext, Boolean> val = callback.get();
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

    /**
     * Checks the clustered contexts. If context is not running in the cluster
     * the context will be started.
     */
    public void checkClusteredContexts() {
        if (!shuttingDown.get()) {
            for (CamelContext camelContext : clusteredContexts) {
                boolean activateRoute = true;
                try {
                    IContextStateManager stateManager = getStateManager(
                            camelContext);

                    if (stateManager.isContextStartable(camelContext)) {
                        stateManager.startContext(camelContext);
                    } else if (stateManager.isContextStoppable(camelContext)) {
                        activateRoute = false;
                        stateManager.stopContext(camelContext);
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
                    msg.append(camelContext.getName());
                    statusHandler.handle(Priority.ERROR, msg.toString(), e);
                }
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

    @Override
    public CamelContext getCamelContext() {
        return theCamelContext;
    }

    @Override
    public void setCamelContext(CamelContext camelContext) {
        this.theCamelContext = camelContext;
    }
}
