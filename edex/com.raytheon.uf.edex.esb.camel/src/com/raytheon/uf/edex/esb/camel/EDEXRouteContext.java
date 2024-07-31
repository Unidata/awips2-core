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
package com.raytheon.uf.edex.esb.camel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.CamelContext;
import org.apache.camel.CamelContextLifecycle;
import org.apache.camel.Route;
import org.apache.camel.RuntimeCamelException;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.spi.RouteController;
import org.apache.camel.support.service.ServiceSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;

import com.raytheon.uf.edex.core.modes.EdexMode;
import com.raytheon.uf.edex.esb.camel.context.ContextData;
import com.raytheon.uf.edex.esb.camel.context.ContextManager;

/**
 * <p>
 * Container for a {@link EDEXRouteBuilder} that defines one or more Camel
 * routes.
 * </p>
 *
 * <p>
 * IMPORTANT USAGE NOTE: This class must only be instantiated in Spring XML and
 * only via {@link EDEXRouteContextFactory}. Otherwise it does not get properly
 * initialized.
 * </p>
 *
 * <p>
 * This class is the replacement for {@link CamelContext} as it was previously
 * used in EDEX. Before Camel 4, EDEX plugins that wanted to create Camel routes
 * would define one or more CamelContexts in their Spring XML files and then
 * define routes inside the CamelContext(s) using XML.
 * </p>
 *
 * <p>
 * Starting with Camel 4, there can be only one CamelContext in the entire JVM,
 * so this pattern is no longer possible. Instead we define the routes using
 * EDEXRouteBuilder and the Camel Java DSL, and then the EDEXRouteBuilder has to
 * be encapsulated in an EDEXRouteContext in order to be used by EDEX. The
 * EDEXRouteContext provides capabilities that were originally provided by
 * CamelContext. Specifically, it enables querying and controlling the state of
 * multiple routes as a group.
 * </p>
 *
 * <p>
 * Though this class has "RouteContext" in the name, it is unrelated to the
 * routeContext tag that is offered by the Camel XML schema.
 * </p>
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-09   2037227    tgurney     Initial creation
 * 2024-07-24   2037700    tgurney     Add logging of state changes.
 *                                     Start internal routes first and stop
 *                                     them last.
 * 2024-07-31   2037700    tgurney     Fix initialization
 *
 * </pre>
 *
 * @author tgurney
 */
/*
 * The interfaces implemented by this class demand justification:
 *
 * ServiceSupport: This class, part of Camel, is generically useful in that it
 * provides thread-safe start()/stop()/getStatus() methods for free.
 *
 * CamelContextLifecycle: It is not strictly necessary to implement this
 * interface, but we can do so for free when extending ServiceSupport, and it is
 * also implemented by CamelContext, so it makes it a bit easier to adapt code
 * that once operated on CamelContexts to now work on EDEXRouteContexts.
 *
 * BeanNameAware: The many CamelContexts previously used in EDEX were always
 * uniquely identified by their Spring bean name, which was often visible in log
 * files. EDEXRouteContext continues that practice for backward compatibility
 * and to avoid the annoyance of declaring a name for the context separately.
 * For the bean to access its own name, we have to implement this interface,
 * which makes Spring inject the bean name into the object.
 *
 * InitializingBean: Provides the afterPropertiesSet method that Spring calls
 * after the bean has been initialized in every other respect. We need this
 * method to be able to register the EDEXRouteContext with EDEX (via
 * ContextManager) after the bean name has been set. (In the constructor the
 * bean name has not been set yet.)
 */
public class EDEXRouteContext extends ServiceSupport
        implements CamelContextLifecycle, BeanNameAware, InitializingBean {

    private static final Logger logger = LoggerFactory
            .getLogger(EDEXRouteContext.class);

    /**
     * Endpoint types that are visible only within the EDEX JVM they were
     * created in. We want to start these routes first and stop them last.
     */
    private static final Set<String> INTERNAL_ENDPOINT_TYPES = Set.of("direct",
            "seda", "timer", "quartz");

    /**
     * Name of this object as a Spring bean. Don't set this manually. The only
     * reason this field is not final is that it has to be set after the
     * constructor has already ran.
     */
    private String name = null;

    /** true if this is a clustered context, false if not */
    private final boolean clustered;

    private final EDEXRouteBuilder routeBuilder;

    private static final ContextManager contextManager = ContextManager
            .getInstance();

    /**
     * Route definitions created by the routeBuilder. Access only through
     * getRouteDefs() to prevent access before the list has been populated
     */
    private volatile List<RouteDefinition> routeDefs = null;

    public EDEXRouteContext(EDEXRouteBuilder routeBuilder) {
        this(routeBuilder, false);
    }

    public EDEXRouteContext(EDEXRouteBuilder routeBuilder, boolean clustered) {
        this.routeBuilder = routeBuilder;
        this.clustered = clustered;
    }

    private void logInfo(String msg) {
        logger.info(getName() + ": " + msg);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        build();
    }

    @Override
    protected void doBuild() throws Exception {
        /*
         * CamelContext.addRoutes has to be called before doing anything else.
         * The routes do not exist until then;
         * RouteBuilder.getRoutes().getRoutes() would return empty list
         */
        contextManager.getCamelContext().addRoutes(routeBuilder);
        routeDefs = List.copyOf(routeBuilder.getRoutes().getRoutes());
        contextManager.registerRouteContext(this);
    }

    /**
     * @return true if the route is internal to this JVM, false if the route
     *         receives messages from outside the JVM
     */
    private static boolean routeIsInternal(RouteDefinition r) {
        return INTERNAL_ENDPOINT_TYPES.contains(ContextData
                .getEndpointTypeAndName(r.getEndpointUrl()).getFirst());
    }

    /**
     * TODO Camel 4 - remove this method after implementing new startup/shutdown
     * code
     *
     * @return true if the route is internal to this JVM, false if the route
     *         receives messages from outside the JVM
     * @deprecated Outside classes should no longer need to know about the
     *             concept of internal vs external routes.
     */
    @Deprecated
    public static boolean routeIsInternal(Route r) {
        return INTERNAL_ENDPOINT_TYPES.contains(ContextData
                .getEndpointTypeAndName(r.getEndpoint().getEndpointUri())
                .getFirst());
    }

    /** Comparison key for sorting internal routes before external ones */
    private static int internalFirst(RouteDefinition r) {
        if (EDEXRouteContext.routeIsInternal(r)) {
            return 0;
        }
        return 1;
    }

    /**
     * @return unmodifiable list of all routes contained in this context. The
     *         order of the routes is unspecified.
     */
    public List<RouteDefinition> getRouteDefs() {
        if (routeDefs == null) {
            // Shouldn't be possible. But just in case
            throw new RuntimeException(
                    "Tried to get routes from uninitialized " + this);
        }
        return routeDefs;
    }

    /**
     * @return unmodifiable list of all routes contained in this context. Routes
     *         that should be started first are ordered first.
     */
    public List<RouteDefinition> getRouteDefsInStartupOrder() {
        List<RouteDefinition> routeDefsTmp = new ArrayList<>(getRouteDefs());
        routeDefsTmp
                .sort(Comparator.comparingInt(EDEXRouteContext::internalFirst));
        return Collections.unmodifiableList(routeDefsTmp);
    }

    /**
     * @return unmodifiable list of all routes contained in this context. Routes
     *         that should be shut down first (according to inter-context
     *         relationships) are ordered first.
     */
    public List<RouteDefinition> getRouteDefsInShutdownOrder() {
        List<RouteDefinition> routeDefsTmp = new ArrayList<>(getRouteDefs());
        routeDefsTmp.sort(Comparator
                .comparingInt(r -> -EDEXRouteContext.internalFirst(r)));
        return Collections.unmodifiableList(routeDefsTmp);
    }

    /** @return set of endpoint URLs for all routes in this context */
    public Set<String> getEndpointUrls() {
        return getRouteDefs().stream().map(RouteDefinition::getEndpointUrl)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** @return the bean name */
    public String getName() {
        return name;
    }

    /**
     * @return true if this is a clustered context, meaning that it runs on no
     *         more than one EDEX process at a time within the cluster. If
     *         false, the context will run on all EDEX processes in the cluster
     *         that include the context in their {@link EdexMode}.
     */
    public boolean isClustered() {
        return clustered;
    }

    /* The name is assumed to uniquely identify the context */
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        EDEXRouteContext other = (EDEXRouteContext) obj;
        return Objects.equals(name, other.name);
    }

    private RouteController getRouteController() {
        return contextManager.getCamelContext().getRouteController();
    }

    @Override
    protected void doStart() {
        logInfo("starting");
        for (RouteDefinition r : getRouteDefsInStartupOrder()) {
            try {
                getRouteController().startRoute(r.getRouteId());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
        logInfo("started");
    }

    @Override
    protected void doStop() {
        logInfo("stopping");
        for (RouteDefinition r : getRouteDefsInShutdownOrder()) {
            try {
                getRouteController().stopRoute(r.getRouteId());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
        logInfo("stopped");
    }

    @Override
    protected void doSuspend() {
        logInfo("suspending");
        for (RouteDefinition r : getRouteDefsInStartupOrder()) {
            try {
                getRouteController().suspendRoute(r.getRouteId());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
        logInfo("suspended");
    }

    @Override
    protected void doResume() {
        logInfo("resuming");
        for (RouteDefinition r : getRouteDefsInShutdownOrder()) {
            try {
                getRouteController().resumeRoute(r.getRouteId());
            } catch (Exception e) {
                throw new RuntimeCamelException(e);
            }
        }
        logInfo("resumed");
    }

    /* Only for Spring to inject the bean name */
    @Override
    public void setBeanName(String name) {
        if (this.name != null) {
            throw new RuntimeException("name already set to '" + this.name
                    + "', cannot set again to '" + name + "'");
        }
        this.name = name;
    }

    @Override
    public String toString() {
        return "EDEXRouteContext [" + name + "]";
    }

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }
}
