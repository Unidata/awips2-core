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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.CamelContext;

import com.raytheon.uf.common.util.Pair;

/**
 * Contains all known contexts and parsed data about the contexts.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 10, 2014 2726       rjpeter     Initial creation.
 * Mar  4, 2021 8326       tgurney     Fixes for Camel 3 API changes
 * Jun 28, 2022 8865       mapeters    Add getDefaultContext()
 * Jul 29, 2024 2037700    tgurney     Replace with shim interface (Camel 4).
 *                                     Correctly parse URI without "//".
 *
 * </pre>
 *
 * @author rjpeter
 */
public interface ContextData {
    /**
     * Pulls the direct-vm:name, vm:name, queue:name, topic:name section from
     * the endpoint URI.
     */
    public static final Pattern endpointUriParsePattern = Pattern
            .compile("([^:]+):(?://)?([^?]+)");

    @Deprecated
    public default List<CamelContext> getContexts() {
        return List.of(getDefaultContext());
    }

    /**
     * @param uri
     * @return component type and endpoint name
     */
    public static Pair<String, String> getEndpointTypeAndName(String uri) {
        Pair<String, String> rval = null;
        Matcher m = endpointUriParsePattern.matcher(uri);
        if (m.find()) {
            String endpointType = m.group(1);
            String endpointName = m.group(2);
            rval = new Pair<>(endpointType, endpointName);
        }
        return rval;
    }

    @Deprecated
    public CamelContext getDefaultContext();

}
