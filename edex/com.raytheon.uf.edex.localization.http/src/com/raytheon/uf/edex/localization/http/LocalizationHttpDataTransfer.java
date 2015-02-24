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
import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * Utility class for sending localization files to output streams
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 16, 2015 3978       bclement     Initial creation
 * Feb 24, 2015 3978       njensen      Changed to use abstract InputStream
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class LocalizationHttpDataTransfer {

    private static final int BUFFER_SIZE = Integer.getInteger(
            "localization.http.buffer.size", 2048 * 4);

    /**
     * 
     */
    private LocalizationHttpDataTransfer() {
    }

    /**
     * @param lfile
     * @param out
     * @throws LocalizationException
     * @throws IOException
     */
    public static void copy(LocalizationFile lfile, OutputStream out)
            throws LocalizationException, IOException {
        try (InputStream in = lfile.openInputStream()) {
            copy(in, out);
        }
    }

    /**
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copy(InputStream in, OutputStream out)
            throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

}
