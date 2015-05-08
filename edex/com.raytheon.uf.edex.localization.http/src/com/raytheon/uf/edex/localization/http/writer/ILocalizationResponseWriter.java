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
package com.raytheon.uf.edex.localization.http.writer;

import java.io.IOException;
import java.io.OutputStream;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.LocalizationContext;

/**
 * Interface for writers that handle output for HTTP localization requests
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 15, 2015 3978       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public interface ILocalizationResponseWriter {

    /**
     * @param contentType
     * @return true if this writer can generate a response of content type
     */
    public boolean generates(MimeType contentType);

    /**
     * Write the file to the output stream in the specified content type
     * 
     * @param contentType
     * @param context
     * @param path
     * @param out
     */
    public void write(MimeType contentType, LocalizationContext context,
            String path, OutputStream out) throws IOException;

}
