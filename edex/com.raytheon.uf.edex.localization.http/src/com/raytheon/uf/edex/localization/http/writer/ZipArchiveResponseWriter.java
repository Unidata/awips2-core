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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.edex.localization.http.LocalizationHttpDataTransfer;

/**
 * Localization response writer that recursively writes a localization directory
 * in a zip archive
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
public class ZipArchiveResponseWriter implements ILocalizationResponseWriter {

    public static final MimeType CONTENT_TYPE = new MimeType("application/zip");

    @Override
    public boolean generates(MimeType contentType) {
        return CONTENT_TYPE.equalsIgnoreParams(contentType);
    }

    @Override
    public void write(MimeType contentType, LocalizationContext context,
            String path, OutputStream out) throws IOException {
        if (!generates(contentType)) {
            throw new IllegalArgumentException(
                    "Unable to generate requested content type: " + contentType);
        }
        IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationFile[] files = pathManager.listFiles(context, path, null,
                true, true);

        Path base = Paths.get(path);

        ZipOutputStream zout = new ZipOutputStream(out);
        try {
            for (LocalizationFile entryFile : files) {
                Path fullPath = Paths.get(entryFile.getName());
                Path entryPath = base.relativize(fullPath);
                ZipEntry zEntry = new ZipEntry(entryPath.toString());
                zout.putNextEntry(zEntry);
                try {
                    LocalizationHttpDataTransfer.copy(entryFile, zout);
                } catch (LocalizationException e) {
                    throw new IOException("Unable to read localization file: "
                            + entryFile, e);
                } finally {
                    zout.closeEntry();
                }
            }
        } finally {
            zout.flush();
            /*
             * shouldn't be closing an output stream that was passed to us, but
             * ZipOutputStream doesn't finalize the archive unless this is
             * called.
             */
            zout.close();
        }
    }

}
