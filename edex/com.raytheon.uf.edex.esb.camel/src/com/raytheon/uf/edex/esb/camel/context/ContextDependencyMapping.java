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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.ConfigurationException;

import org.apache.camel.model.RouteDefinition;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.Pair;
import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;

/**
 * Contains context dependency mappings.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 26, 2014 2726       rjpeter     Initial creation
 * Jul 24, 2024 2037700    tgurney     Update list of endpoint types for Camel 4
 * Jul 31, 2024 2037700    tgurney     Replace CamelContext with EDEXRouteContext
 *
 * </pre>
 *
 * @author rjpeter
 */
public class ContextDependencyMapping {
    /**
     * Endpoint types that should be tracked for dependency mapping. These are
     * used for communication between contexts
     */
    protected static final Set<String> DEPENDENCY_ENDPOINT_TYPES = Set
            .of("direct", "seda");

    /**
     * The dependency mappings.
     */
    protected final Map<EDEXRouteContext, DependencyNode> dependencyMapping;

    /**
     * Populates the dependency mappings for all camel contexts.
     * {@code suppressExceptions} can be used to differentiate between
     * startup/shutdown conditions to allow the map to be populated regardless
     * of detected issues.
     *
     * @param contextData
     * @param suppressExceptions
     * @throws ConfigurationException
     */
    public ContextDependencyMapping(Collection<EDEXRouteContext> contexts,
            boolean suppressExceptions) throws ConfigurationException {
        dependencyMapping = Collections.unmodifiableMap(
                populateDependencyMapping(contexts, suppressExceptions));
    }

    /**
     * Returns a {@code IUFStatusHandler}. Not cached as rarely used.
     *
     * @return
     */
    private static IUFStatusHandler getHandler() {
        return UFStatus.getHandler(ContextDependencyMapping.class);
    }

    /**
     * Dependency mappings per context. The dependency mapping is only for
     * internal vm types that have a direct dependency. Indirect dependency via
     * a JMS queue for example is not returned/enforced.
     *
     * @param contexts
     * @param suppressExceptions
     *            Done in a shutdown scenario to get the dependencyMapping as
     *            close as possible.
     */
    protected static Map<EDEXRouteContext, DependencyNode> populateDependencyMapping(
            Collection<EDEXRouteContext> contexts, boolean suppressExceptions)
            throws ConfigurationException {
        Map<EDEXRouteContext, DependencyNode> dependencyMapping = new LinkedHashMap<>(
                contexts.size());

        // set up dependency nodes for internal types
        Map<String, EDEXRouteContext> consumesFrom = new HashMap<>();
        Map<String, List<EDEXRouteContext>> producesTo = new HashMap<>();
        Set<String> consumers = new HashSet<>();

        // scan for consuming and producing internal endpoints
        for (EDEXRouteContext context : contexts) {
            dependencyMapping.put(context, new DependencyNode(context));
            consumers.clear();
            List<RouteDefinition> routes = context.getRouteDefs();
            if (routes != null) {
                for (RouteDefinition route : routes) {
                    String uri = route.getEndpointUrl();
                    Pair<String, String> typeAndName = ContextData
                            .getEndpointTypeAndName(uri);
                    if (typeAndName != null && DEPENDENCY_ENDPOINT_TYPES
                            .contains(typeAndName.getFirst())) {
                        String endpointName = typeAndName.getSecond();
                        consumers.add(endpointName);

                        /*
                         * Internal types don't support a fanout type policy
                         * where multiple routes can listen to the same
                         * endpoint.
                         */
                        EDEXRouteContext prev = consumesFrom.put(endpointName,
                                context);
                        if (prev != null) {
                            String msg = "Two contexts listen to the same internal endpoint ["
                                    + endpointName
                                    + "].  ContextManager cannot handle this situation.  Double check configuration.  Conflicting contexts ["
                                    + prev.getName() + "] and ["
                                    + context.getName() + "]";
                            if (suppressExceptions) {
                                getHandler().error(msg);
                            } else {
                                throw new ConfigurationException(msg);
                            }
                        }
                    }
                }
            }

            Collection<String> endpointUris = context.getToEndpoints();
            if (endpointUris != null) {
                for (String uri : endpointUris) {
                    Pair<String, String> typeAndName = ContextData
                            .getEndpointTypeAndName(uri);
                    if (typeAndName != null && DEPENDENCY_ENDPOINT_TYPES
                            .contains(typeAndName.getFirst())) {
                        String endpointName = typeAndName.getSecond();
                        if (!consumers.contains(endpointName)) {
                            List<EDEXRouteContext> producerCtxs = producesTo
                                    .get(endpointName);
                            if (producerCtxs == null) {
                                producerCtxs = new LinkedList<>();
                                producesTo.put(endpointName, producerCtxs);
                            }
                            producerCtxs.add(context);
                        }
                    }
                }
            }
        }

        // setup dependencies for internal routes
        for (Map.Entry<String, List<EDEXRouteContext>> producersEntry : producesTo
                .entrySet()) {
            String endpoint = producersEntry.getKey();
            EDEXRouteContext consumer = consumesFrom.get(endpoint);
            List<EDEXRouteContext> producers = producersEntry.getValue();

            if (consumer == null) {
                StringBuilder msg = new StringBuilder(200);
                msg.append("Internal Routing Endpoint [").append(endpoint)
                        .append("] has no defined consumers.  This is endpoint is used in contexts [");
                Iterator<EDEXRouteContext> producerIter = producers.iterator();

                while (producerIter.hasNext()) {
                    EDEXRouteContext producer = producerIter.next();
                    msg.append(producer.getName());

                    if (producerIter.hasNext()) {
                        msg.append(", ");
                    }
                }

                msg.append("]");
                if (suppressExceptions) {
                    getHandler().error(msg.toString());
                } else {
                    throw new ConfigurationException(msg.toString());
                }
            } else {
                DependencyNode consumerNode = dependencyMapping.get(consumer);
                for (EDEXRouteContext producer : producers) {
                    DependencyNode producerNode = dependencyMapping
                            .get(producer);
                    consumerNode.addDependentNode(producerNode);
                }
            }
        }
        return dependencyMapping;
    }

    /**
     * Get the contexts that depend upon the passed context to work. If the
     * passed context is unknown null will be returned.
     *
     * @param context
     * @return
     */
    public Set<EDEXRouteContext> getDependentContexts(
            EDEXRouteContext context) {
        DependencyNode dNode = dependencyMapping.get(context);
        if (dNode == null) {
            return null;
        }

        return dNode.getDependentContexts();
    }

    /**
     * Get the contexts that the passed context requires to be running to work.
     * If the passed context is unknown null will be returned.
     *
     * @param context
     * @return
     */
    public Set<EDEXRouteContext> getRequiredContexts(EDEXRouteContext context) {
        DependencyNode dNode = dependencyMapping.get(context);
        if (dNode == null) {
            return null;
        }

        return dNode.getRequiredContexts();
    }
}
