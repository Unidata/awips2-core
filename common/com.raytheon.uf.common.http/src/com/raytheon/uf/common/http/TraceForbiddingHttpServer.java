/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.http;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.Callback;

import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * Jetty HTTP server that returns a 405 Method Not Allowed status in response to
 * TRACE/TRACK requests.
 *
 * Allowing those methods is a potential security vulnerability:
 * https://www.tenable.com/plugins/nessus/11213
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- -------------------------------------------
 * Apr 05, 2022 8837       mapeters    Initial creation (extracted anonymous
 *                                     class from collaboration's WebServerRunner)
 * 2024-05-09   2037228    tgurney     Jetty 12 API changes
 *
 * </pre>
 *
 * @author mapeters
 */
public class TraceForbiddingHttpServer extends Server {

    public TraceForbiddingHttpServer(int port) {
        super(port);
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback)
            throws Exception {
        if ("TRACE".equals(request.getMethod().toUpperCase())
                || "TRACK".equals(request.getMethod().toUpperCase())) {
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            callback.succeeded();
            return true;
        } else {
            return super.handle(request, response, callback);
        }
    }
}
