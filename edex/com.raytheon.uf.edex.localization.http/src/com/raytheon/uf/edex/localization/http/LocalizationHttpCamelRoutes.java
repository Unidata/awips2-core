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

package com.raytheon.uf.edex.localization.http;

import com.raytheon.uf.edex.esb.camel.EDEXRouteBuilder;

/**
 * Camel routes converted from file "localization-http-request.xml", context
 * "localization-http-camel"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-11   2037702    aford       Initial creation (from auto-generated)
 *
 * </pre>
 */

public class LocalizationHttpCamelRoutes extends EDEXRouteBuilder {

    private final String httpPort;

    private final String edexLocalizationHttpPath;

    private final String edexLocalizationHttpProperties;

    public LocalizationHttpCamelRoutes(String httpPort,
            String edexLocalizationHttpPath,
            String edexLocalizationHttpProperties) {
        this.httpPort = httpPort;
        this.edexLocalizationHttpPath = edexLocalizationHttpPath;
        this.edexLocalizationHttpProperties = edexLocalizationHttpProperties;
    }

    @Override
    public void configure() throws Exception {
        from("jetty:http://0.0.0.0:" + this.httpPort
                + this.edexLocalizationHttpPath + "?"
                + this.edexLocalizationHttpProperties
                + "&httpMethodRestrict=HEAD,GET,PUT,DELETE&mapHttpMessageBody=false")
                        .noStreamCaching()
                        .setBody(simple("${in.header.CamelHttpServletRequest}"))
                        .bean("localizationHttpSrv",
                                "handle(${in.header.CamelHttpServletRequest}, ${in.header.CamelHttpServletResponse})")
                        .setId("localizationHttpRoute");
    }
}