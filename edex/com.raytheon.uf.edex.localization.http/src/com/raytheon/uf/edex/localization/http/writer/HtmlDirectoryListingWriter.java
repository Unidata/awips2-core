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
import java.io.PrintWriter;
import java.util.List;

import com.raytheon.uf.common.http.MimeType;

/**
 * Localization response writer that generates HTML directory listings.
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
public class HtmlDirectoryListingWriter extends AbstractDirectoryListingWriter {

    public static final MimeType CONTENT_TYPE = new MimeType("text/html");

    @Override
    public boolean generates(MimeType contentType) {
        return CONTENT_TYPE.equalsIgnoreParams(contentType);
    }

    @Override
    public void write(MimeType contentType, List<String> entries,
            OutputStream out) throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: " + contentType);
        }
        // TODO this should be done with a template
        PrintWriter pw = new PrintWriter(out);
        pw.write("<!DOCTYPE html><body><ul>");
        for (String entry : entries) {
            pw.write("<li><a href=\"" + entry + "\">" + entry + "</a></li>");
        }
        pw.write("</ul></body></html>");
        pw.flush();
    }

}
