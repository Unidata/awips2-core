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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.http.ProtectiveHttpOutputStream;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.edex.requestsrv.serialization.ISerializingStreamExecutor;
import com.raytheon.uf.edex.requestsrv.serialization.SerializingStreamExecutor;
import com.raytheon.uf.edex.requestsrv.serialization.UnsupportedFormatException;

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
 * Jan 05, 2015 3789       bclement    modified for camel rest implementation
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
     * Executes the request in the input stream by deserializing into an
     * {@link IServerRequest} object and serializing the response to the
     * {@link HttpServletResponse} directly based on the request (Accept: ,
     * Content-Type, url, etc)
     * 
     * @param requestStream
     *            The http request to read from
     * @param requestFormat
     *            request body format
     * @param response
     *            The http response to write to
     * @throws Exception
     */
    public void execute(InputStream requestStream,
            String requestFormat, String acceptEncoding,
            HttpServletResponse response) throws Exception {
        if (requestFormat == null) {
            throw new IllegalArgumentException(
                    "Unable to determine HTTP body format from request");
        }
        /*
         * TODO: Use HTTP "Accept:" header to determine best response format,
         * default to request format if none set in accept
         */
        String responseFormat = requestFormat;

        ProtectiveHttpOutputStream out = new ProtectiveHttpOutputStream(
                response, acceptEncoding);
        try {
            String responseContentType = executor
                    .getContentType(responseFormat);
            response.setContentType(responseContentType);
            executor.execute(requestFormat, requestStream, responseFormat, out);
        } catch (UnsupportedFormatException e) {
            /* unsupported format was specified on the path, report as 404 */
            if (!out.used()) {
                /* output stream wasn't used, we can report the error */
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain");
                out.write(("No handlers for specified format: " + requestFormat)
                        .getBytes());
            }
        } finally {
            requestStream.close();
            if (out != null) {
                /* flushed needed or HttpGenerator warns of 'extra content' */
                out.flush();
                out.close();
            }
        }
    }

}
