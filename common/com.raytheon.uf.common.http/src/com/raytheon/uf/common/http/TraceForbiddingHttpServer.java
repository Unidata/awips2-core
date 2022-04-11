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

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.HttpChannel;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.Server;

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
    public void handle(HttpChannel channel)
            throws IOException, ServletException {
        Request request = channel.getRequest();
        Response response = channel.getResponse();

        if ("TRACE".equals(request.getMethod().toUpperCase())
                || "TRACK".equals(request.getMethod().toUpperCase())) {
            request.setHandled(true);
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else {
            super.handle(channel);
        }
    }
}
