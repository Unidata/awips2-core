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

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * {@link HttpServletResponseWrapper} that discards any body. An HTTP HEAD
 * request is supposed to be identical to a GET request but it excludes the
 * body, so this wrapper allows the handler to behave the same for HEAD and GET.
 * Note: If a handler is doing significant work to generate a body then it may
 * want to handle HEAD specially instead of relying on this class to discard the
 * body.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 07, 2017  5731     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class HeadServletResponse extends HttpServletResponseWrapper {
    public HeadServletResponse(HttpServletResponse response) {
        super(response);
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }

            @Override
            public void write(int b) throws IOException {
                /* discard the contents */
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                /* discard the contents faster than super */
            }

        };
    }
}