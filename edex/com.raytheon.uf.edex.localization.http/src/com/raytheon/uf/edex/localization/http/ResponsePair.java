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
import java.io.OutputStream;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.edex.localization.http.writer.ILocalizationResponseWriter;

/**
 * Pairs a response type with a writer object. This ensures that the writer
 * knows which content type to generate if it supports more than one.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 16, 2015 3978       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class ResponsePair<T extends ILocalizationResponseWriter> {

    private final MimeType responseType;

    private final T writer;

    public ResponsePair(MimeType responseType, T writer) {
        this.responseType = responseType;
        this.writer = writer;
    }

    /**
     * @return the responseType
     */
    public MimeType getResponseType() {
        return responseType;
    }

    /**
     * @return the writer
     */
    public T getWriter() {
        return writer;
    }

    /**
     * @param file
     * @param out
     * @throws IOException
     */
    public void write(LocalizationContext context, String path, OutputStream out)
            throws IOException {
        writer.write(responseType, context, path, out);
    }
}
