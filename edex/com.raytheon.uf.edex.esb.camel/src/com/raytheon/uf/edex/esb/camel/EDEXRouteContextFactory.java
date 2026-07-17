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

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Factory for creating {@link EDEXRouteContext} objects.
 *
 * The purpose of this class is to allow creating route contexts using a
 * reference to a factory bean, rather than directly instantiating the
 * EDEXRouteContext class.
 *
 * The intended pattern is:
 *
 * <pre>
 * {@code
 * <bean id="myCtx" factory-bean="routeContextFactory" factory-method="create">
 *     <constructor-arg>
 *         <bean class="com.raytheon.uf.edex.example.MyEDEXRouteBuilder" />
 *     </constructor-arg>
 * </bean>
 * }
 * </pre>
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

public class EDEXRouteContextFactory {
    public EDEXRouteContextFactory() {
    }

    public EDEXRouteContext create(EDEXRouteBuilder routeBuilder) {
        return new EDEXRouteContext(routeBuilder);
    }

    public EDEXRouteContext createClustered(EDEXRouteBuilder routeBuilder) {
        return new EDEXRouteContext(routeBuilder, true);
    }
}
