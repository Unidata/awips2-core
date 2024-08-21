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

package com.raytheon.uf.edex.requestsrv;

import com.raytheon.uf.edex.esb.camel.EDEXRouteBuilder;
import com.raytheon.uf.edex.routes.EDEXHttpRequestFormatParser;

/**
 * Camel routes converted from file "request-service.xml", context
 * "request-service-camel"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-11   2037702    aford       Initial creation (from auto-generated)
 * 2024-08-20   2037798    tgurney     Extract RequestFormatParser to new class
 *                                     called EDEXHttpRequestFormatParser
 *
 * </pre>
 */

public class RequestServiceCamelRoutes extends EDEXRouteBuilder {

    private final String edexHttpPort;

    private final String edexHttpServerPath;

    private final String edexRequestSrvHttpChunked;

    public RequestServiceCamelRoutes(String edexHttpPort,
            String edexHttpServerPath, String edexRequestSrvHttpChunked) {
        this.edexHttpPort = edexHttpPort;
        this.edexHttpServerPath = edexHttpServerPath;
        this.edexRequestSrvHttpChunked = edexRequestSrvHttpChunked;
    }

    @Override
    public void configure() throws Exception {
        String requestServiceEndpoint = "jetty:http://0.0.0.0:"
                + this.edexHttpPort + "/" + this.edexHttpServerPath
                + "?httpMethodRestrict=POST&matchOnUriPrefix=true&chunked="
                + this.edexRequestSrvHttpChunked;

        // @formatter:off
        from(requestServiceEndpoint)
                .noStreamCaching()
                .process(new EDEXHttpRequestFormatParser())
                .to("bean:httpServiceExecutor?method=execute("
                        + "${body}, "
                        + "${in.header." + EDEXHttpRequestFormatParser.FORMAT_HEADER + "}, "
                        + "${in.header.accept-encoding}, "
                        + "${in.header.CamelHttpServletResponse})");
        // @formatter:on
    }
}
