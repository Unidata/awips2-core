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
import java.util.List;

import com.raytheon.uf.common.http.MimeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;

/**
 * Interface for localization response writers that generate directory listings
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 15, 2015 3978       bclement     Initial creation
 * Aug 07, 2017 5731       bsteffen     Add getRelativeName
 * 
 * </pre>
 * 
 * @author bclement
 */
public interface IDirectoryListingWriter extends ILocalizationResponseWriter {

    /**
     * Write the directory entries to the output stream in the specified content
     * type
     * 
     * @param contentType
     * @param entries
     *            List of directory entries. Entries that are themselves a
     *            directory should have a trailing slash
     * @param out
     * @throws IOException
     */
    public void write(MimeType contentType, List<String> entries,
            OutputStream out) throws IOException;

    /**
     * Return the name of the file with any leading directory components
     * removed.
     */
    public static String getBaseName(ILocalizationFile file) {
        String name = file.getPath();
        int index = name.lastIndexOf(IPathManager.SEPARATOR);
        if (index >= 0) {
            name = name.substring(index + 1);
        }
        if (file.isDirectory()) {
            name = name + IPathManager.SEPARATOR;
        }
        return name;
    }

}
