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

import javax.servlet.http.HttpServletResponse;

/**
 * Wraps an HTTPServletResponse in an output stream that postpones accessing the
 * response's output stream until it is actually written to. This allows for the
 * response to be sent to libraries that expect an output stream while still
 * being able to report error codes for any errors that may be returned (if the
 * response hasn't already started writing).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 06, 2015 3789       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class ProtectiveHttpOutputStream extends OutputStream {

    private final HttpServletResponse response;

    private final String acceptEncoding;

    private OutputStream out = null;

    /**
     * @param response
     * @param acceptEncoding
     *            HTTP header that specifies which encodings the client can
     *            accept. Example usage would be for GZIP compression.
     */
    public ProtectiveHttpOutputStream(HttpServletResponse response,
            String acceptEncoding) {
        this.response = response;
        this.acceptEncoding = acceptEncoding;
    }

    /**
     * Ensures that the output stream from the response is only retrieved once
     * 
     * @return
     * @throws IOException
     */
    private OutputStream getOutputStream() throws IOException {
        if (this.out == null) {
            this.out = ResponseEncoder.getResponseStream(acceptEncoding,
                    response);
        }
        return this.out;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(int)
     */
    @Override
    public void write(int b) throws IOException {
        getOutputStream().write(b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(byte[])
     */
    @Override
    public void write(byte[] b) throws IOException {
        getOutputStream().write(b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#write(byte[], int, int)
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        getOutputStream().write(b, off, len);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#flush()
     */
    @Override
    public void flush() throws IOException {
        getOutputStream().flush();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.io.OutputStream#close()
     */
    @Override
    public void close() throws IOException {
        getOutputStream().close();
    }

    /**
     * @return true if output stream was used
     */
    public boolean used() {
        return this.out != null;
    }

    /**
     * @return the response
     */
    public HttpServletResponse getResponse() {
        return response;
    }

    /**
     * @return the acceptEncoding
     */
    public String getAcceptEncoding() {
        return acceptEncoding;
    }

}
