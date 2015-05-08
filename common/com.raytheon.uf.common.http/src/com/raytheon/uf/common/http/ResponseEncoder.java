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
package com.raytheon.uf.common.http;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;

/**
 * Utility to handle response encoding for servlet response objects (eg gzip)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 05, 2015 3789       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class ResponseEncoder {

    public static final String GZIP_ENCODING = "gzip";

    /**
     * Gets response output stream from http servlet object. If accept encoding
     * includes gzip, the response stream will be wrapped in a gzip output
     * stream and the content encoding header of the response will be set to
     * gzip
     * 
     * @param acceptEncoding
     * @param response
     * @return
     * @throws IOException
     */
    public static OutputStream getResponseStream(String acceptEncoding,
            HttpServletResponse response) throws IOException {
        OutputStream rval;
        /*
         * TODO allow for registration of different response encoders and do
         * matching based on accept encoding q values
         */
        if (acceptsGzip(acceptEncoding)) {
            response.setHeader(HttpHeaders.CONTENT_ENCODING, GZIP_ENCODING);
            rval = new GZIPOutputStream(response.getOutputStream());
        } else {
            rval = response.getOutputStream();
        }
        return rval;
    }

    /**
     * @param acceptEncoding
     * @return true if accept encoding includes gzip
     */
    public static boolean acceptsGzip(String acceptEncoding) {
        boolean rval = false;
        if (acceptEncoding != null) {
            AcceptHeaderParser parser = new AcceptHeaderParser(acceptEncoding);
            for (AcceptHeaderValue value : parser) {
                String encoding = value.getEncoding();
                if (encoding.equalsIgnoreCase(GZIP_ENCODING)
                        && value.isAcceptable()) {
                    rval = true;
                    break;
                }
            }
        }
        return rval;
    }

}
