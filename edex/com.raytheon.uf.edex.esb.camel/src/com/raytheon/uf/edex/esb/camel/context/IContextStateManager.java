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

import com.raytheon.uf.edex.esb.camel.EDEXRouteContext;

/**
 * Represents a way for managing a context for starting and stopping. Allows for
 * Context with different purposes to be handled independently of each other.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2014 2726       rjpeter     Initial creation.
 * Jul 31, 2024 2037700    tgurney     Replace CamelContext with EDEXRouteContext
 *
 * </pre>
 *
 * @author rjpeter
 */
public interface IContextStateManager {
    /**
     * Is the {@code EDEXRouteContext} startable?
     *
     * @param context
     * @return
     * @throws Exception
     */
    public boolean isContextStartable(EDEXRouteContext context)
            throws Exception;

    /**
     * Start the {@code EDEXRouteContext}.
     *
     * @param context
     * @return
     * @throws Exception
     */
    public boolean startContext(EDEXRouteContext context) throws Exception;

    /**
     * Is the {@code EDEXRouteContext} stoppable?
     *
     * @param context
     * @return
     * @throws Exception
     */
    public boolean isContextStoppable(EDEXRouteContext context)
            throws Exception;

    /**
     * Stop the {@code EDEXRouteContext}.
     *
     * @param context
     * @return
     * @throws Exception
     */
    public boolean stopContext(EDEXRouteContext context) throws Exception;
}