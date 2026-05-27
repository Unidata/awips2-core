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
package com.raytheon.uf.edex.routes;

import org.apache.camel.builder.RouteBuilder;

/**
 * <p>
 * Camel {@link RouteBuilder} to be used by EDEX plugins.
 * </p>
 *
 * <p>
 * To add new Camel routes to EDEX, extend this class and override the
 * {@link #configure()} method. Routes are defined in this method using the
 * Camel Java DSL: https://camel.apache.org/manual/java-dsl.html
 * </p>
 *
 * <p>
 * For EDEX to actually be aware of the new routes, you have to wrap your new
 * route builder class in a {@link EDEXRouteContext} from within the Spring
 * application context. Refer to that class for details on how that is done.
 * </p>
 *
 * <p>
 * This class does not add any extra functionality on top of RouteBuilder. It
 * exists only for future-proofing and to make it easier to find all Camel
 * routes created by EDEX plugins. Any functionality for controlling or querying
 * the status of routes at runtime should be implemented not in this class but
 * in EDEXRouteContext.
 * </p>
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-09   2037227    tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public abstract class EDEXRouteBuilder extends RouteBuilder {
}
