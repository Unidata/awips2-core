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
import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * Handles HEAD http method for localization files. This simply extends the
 * {@link LocalizationHttpGetDelegate} but uses a {@link HeadServletResponse} to
 * discard the body.
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
public class LocalizationHttpHeadDelegate extends LocalizationHttpGetDelegate {

    public LocalizationHttpHeadDelegate(Path basePath) {
        super(basePath);
    }

    /**
     * Handle HTTP HEAD requests for localization files and directories
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        super.handle(request, new HeadServletResponse(response));
    }

    @Override
    protected void copy(LocalizationFile lfile, OutputStream out)
            throws LocalizationException, IOException {
        // Save time, don't copy
    }

}
