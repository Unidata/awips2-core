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
package com.raytheon.uf.edex.requestsrv.http;

import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.requestsrv.serialization.ISerializingStreamExecutor;
import com.raytheon.uf.edex.requestsrv.serialization.SerializingStreamExecutor;

/**
 * Class that takes an {@link HttpServletRequest} and translates into an
 * {@link IServerRequest} to execute.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class HttpRequestServiceExecutor {

    /** Default instance for convenient sharing of registry. */
    private static final HttpRequestServiceExecutor instance = new HttpRequestServiceExecutor(
            SerializingStreamExecutor.getInstance());

    public static HttpRequestServiceExecutor getInstance() {
        return instance;
    }

    private final ISerializingStreamExecutor executor;

    public HttpRequestServiceExecutor(ISerializingStreamExecutor executor) {
        this.executor = executor;
    }

    /**
     * Executes the {@link HttpServletRequest} by deserializing into an
     * {@link IServerRequest} object and serializing the response to the
     * {@link HttpServletResponse} directly based on the request (Accept: ,
     * Content-Type, url, etc)
     * 
     * @param request
     *            The http request to read from
     * @param response
     *            The http response to write to
     * @throws Exception
     */
    public void execute(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String inFormat = getRequestFormat(request);
        if (inFormat == null) {
            throw new IllegalArgumentException(
                    "Unable to determine HTTP body format from request");
        }
        String outFormat = getResponseFormat(request);
        if (outFormat == null) {
            outFormat = inFormat;
        }

        try (InputStream in = request.getInputStream();
                OutputStream out = response.getOutputStream()) {
            executor.execute(inFormat, in, outFormat, out);
        }
    }

    /**
     * Given the {@link HttpServletRequest}, return the body request format
     * 
     * @param request
     * @return
     */
    private static String getRequestFormat(HttpServletRequest request) {
        // Start with format in path if any
        String responseFormat = getFormatFromRequestPath(request.getPathInfo());
        if (responseFormat == null) {
            // No format specified in path, check content type
            responseFormat = request.getContentType();
        }
        return responseFormat;
    }

    /**
     * Given the {@link HttpServletRequest}, return the desired response format
     * 
     * @param request
     * @return
     */
    private static String getResponseFormat(HttpServletRequest request) {
        // TODO: Use HTTP "Accept:" header to determine best response format,
        // default to request format if none set in accept
        return getRequestFormat(request);
    }

    /**
     * Parses the HTTP request path and returns the desired response format or
     * null if none specified in the path. Format is:
     * 
     * <br>
     * <code>/servicePath[/format[/requestClass]]</code>
     * 
     * @param path
     * @return
     */
    private static String getFormatFromRequestPath(String path) {
        String[] pathParts = StringUtil.split(path, '/');
        if (pathParts.length > 1) {
            return pathParts[1];
        }
        return null;
    }

}
