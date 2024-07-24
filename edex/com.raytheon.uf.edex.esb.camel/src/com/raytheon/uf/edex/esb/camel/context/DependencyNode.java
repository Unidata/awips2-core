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

import java.util.HashSet;
import java.util.Set;

import org.apache.camel.CamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to map a context to its required and dependent contexts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2014 2726       rjpeter     Initial creation
 * Jul 24, 2024 2037700    tgurney     Kill EDEX if circular dependency
 *
 * </pre>
 *
 * @author rjpeter
 */
public class DependencyNode {
    private static final Logger logger = LoggerFactory
            .getLogger(DependencyNode.class);

    private final CamelContext context;

    /**
     * Contexts required by this context.
     */
    private final Set<CamelContext> requiredContexts = new HashSet<>();

    /**
     * Contexts that depend on this context.
     */
    private final Set<CamelContext> dependentContexts = new HashSet<>();

    public DependencyNode(CamelContext context) {
        this.context = context;
    }

    /**
     * Add a node who is dependent on this node. Applies linking in both
     * directions.
     *
     * @param dNode
     */
    public void addDependentNode(DependencyNode dNode) {
        if (!requiredContexts.contains(dNode.context)) {
            dependentContexts.add(dNode.context);
            dNode.requiredContexts.add(context);
        } else {
            logger.error("Circular dependency detected between "
                    + context.getName() + " and " + dNode.context.getName());
            System.exit(1);
        }
    }

    public CamelContext getContext() {
        return context;
    }

    /**
     * @return all contexts that this context requires to be running.
     */
    public Set<CamelContext> getRequiredContexts() {
        return requiredContexts;
    }

    /**
     * @return all contexts that depend on this context to be running.
     */
    public Set<CamelContext> getDependentContexts() {
        return dependentContexts;
    }
}