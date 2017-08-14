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
package com.raytheon.uf.edex.localization.http.writer.html;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.edex.localization.http.writer.IDirectoryListingWriter;

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
 * Aug 07, 2017 5731       bsteffen     Handle Recursion
 * 
 * </pre>
 * 
 * @author bclement
 */
public class HtmlDirectoryListingWriter implements IDirectoryListingWriter {

    public static final MimeType CONTENT_TYPE = new MimeType("text/html");

    @Override
    public boolean generates(MimeType contentType) {
        return contentType.accept(CONTENT_TYPE);
    }

    @Override
    public void write(HttpServletRequest request, MimeType contentType,
            LocalizationContext context, String path, OutputStream out)
            throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: "
                            + contentType);
        }
        int depth = 1;
        String depthStr = request.getParameter("depth");
        if (depthStr != null) {
            depth = Integer.parseInt(depthStr);
        }
        PrintWriter pw = new PrintWriter(out);
        pw.write("<!DOCTYPE html><html><body>");
        generateList(context, path, depth, pw);
        pw.write("</body></html>");
        pw.flush();
    }

    private void generateList(LocalizationContext context, String path,
            int depth, PrintWriter pw) {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        ILocalizationFile[] files = pathManager.listFiles(context, path, null,
                false, false);
        Arrays.sort(files,
                Comparator.comparing(IDirectoryListingWriter::getBaseName));

        pw.write("<ul>");
        for (ILocalizationFile file : files) {
            String name = IDirectoryListingWriter.getBaseName(file);
            pw.write("<li><a href=\"" + name + "\">" + name + "</a>");
            if (file.isDirectory() && depth > 1) {
                generateList(context, file.getPath(), depth - 1, pw);
            }
            pw.write("</li>");
        }
        pw.write("</ul>");

    }

    @Override
    public void write(MimeType contentType, List<String> entries,
            OutputStream out) throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: "
                            + contentType);
        }
        PrintWriter pw = new PrintWriter(out);
        pw.write("<!DOCTYPE html><html><body><ul>");
        for (String entry : entries) {
            pw.write("<li><a href=\"" + entry + "\">" + entry + "</a></li>");
        }
        pw.write("</ul></body></html>");
        pw.flush();
    }

}
