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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import com.raytheon.uf.edex.esb.camel.EDEXRouteBuilder;

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
 *
 * </pre>
 */

public class RequestServiceCamelRoutes extends EDEXRouteBuilder {

    private static final String FORMAT_HEADER = "format";

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
                .process(new RequestFormatParser())
                .to("bean:httpServiceExecutor?method=execute("
                        + "${body}, "
                        + "${in.header." + FORMAT_HEADER + "}, "
                        + "${in.header.accept-encoding}, "
                        + "${in.header.CamelHttpServletResponse})");
        // @formatter:on
    }

    /**
     * This class is a custom camel processor that is used to parse out the
     * format from the path parameter used in the request. This is used with the
     * camel jetty component instead of using the camel REST component because
     * disabling stream caching on the REST component does not seem to be
     * possible as of camel 4.4.
     */
    private class RequestFormatParser implements Processor {

        /**
         * Sets the format header on the exchange object by parsing out the path
         * parameter in the request or using the content-type header if the path
         * parameter is not provided.
         *
         * @param exchange
         *            The camel Exchange object to process.
         */
        @Override
        public void process(Exchange exchange) throws Exception {
            String format = null;
            String uri = exchange.getIn().getHeader("CamelHttpUri",
                    String.class);
            String[] uriParts = uri.split("/");
            if (uriParts.length > 2) {
                format = uriParts[uriParts.length - 1];
            } else {
                format = exchange.getIn().getHeader("content-type",
                        String.class);
            }
            exchange.getIn().setHeader(FORMAT_HEADER, format);
        }

    }

}
