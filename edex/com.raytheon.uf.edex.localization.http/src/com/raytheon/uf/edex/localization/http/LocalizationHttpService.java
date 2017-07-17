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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.edex.localization.http.writer.ILocalizationResponseWriter;

/**
 * REST service for handling localization file requests over HTTP.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 12, 2015  3978     bclement  Initial creation
 * Dec 02, 2015  4834     njensen   Added support for PUT requests
 * Jul 01, 2016  5729     bsteffen  Fix restricted permissions on files after
 *                                  PUT.
 * Apr 26, 2017  6258     tgurney   Set permissions on file after PUT
 * May 18, 2017  6242     randerso  Changed to use new roles and permissions
 *                                  framework
 * Aug 07, 2017  5731     bsteffen  Separate logic for each method into it's own class.
 *
 * </pre>
 *
 * @author bclement
 */
public class LocalizationHttpService {

    private final LocalizationHttpGetDelegate get;

    private final LocalizationHttpGetDelegate head;

    private final LocalizationHttpDelegate put;

    private final LocalizationHttpDelegate delete;

    /**
     * @param base
     *            portion of URL that is used for routing to this service
     */
    public LocalizationHttpService(String base) {
        Path basePath = Paths.get(base);
        get = new LocalizationHttpGetDelegate(basePath);
        head = new LocalizationHttpHeadDelegate(basePath);
        put = new LocalizationHttpPutDelegate(basePath);
        delete = new LocalizationHttpDeleteDelegate(basePath);
    }

    /**
     * Handle HTTP PUT requests for localization files
     *
     * @param request
     * @param response
     * @return Always null. Returning null signals to the jetty endpoint that we
     *         handled the response writing ourselves so it will not try to
     *         write to the response.
     * @throws IOException
     */
    public Object handle(HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        switch (request.getMethod()) {
        case "GET":
            get.handle(request, response);
            break;
        case "HEAD":
            head.handle(request, response);
            break;
        case "PUT":
            put.handle(request, response);
            break;
        case "DELETE":
            delete.handle(request, response);
            break;
        default:
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        return null;
    }

    public LocalizationHttpService register(
            ILocalizationResponseWriter... writers) {
        get.register(writers);
        head.register(writers);
        return this;
    }

}
