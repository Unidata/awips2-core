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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;

/**
 * Base class for localization response writers that generate directory
 * listings.
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
public abstract class AbstractDirectoryListingWriter implements
        IDirectoryListingWriter {

    @Override
    public void write(MimeType contentType, LocalizationContext context,
            String path, OutputStream out) throws IOException {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationFile[] files = pathManager.listFiles(context, path, null,
                false, false);
        List<String> entries = new ArrayList<>(files.length);
        for (LocalizationFile subLocalFile : files) {
            File f = subLocalFile.getFile();
            if (f.isDirectory()) {
                entries.add(f.getName() + "/");
            } else {
                entries.add(f.getName());
            }
        }
        write(contentType, entries, out);
    }

}
