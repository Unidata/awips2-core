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

import java.nio.file.Path;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFile;

/**
 * Handles DELETE http method for localization files.
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
public class LocalizationHttpDeleteDelegate
        extends LocalizationHttpModificationDelegate {

    public LocalizationHttpDeleteDelegate(Path basePath) {
        super(basePath);
    }

    @Override
    public void performModification(HttpServletRequest request,
            HttpServletResponse response, LocalizationFile lfile)
            throws Exception {
        lfile.delete();

        sendFileUpdateMessage(
                new FileUpdatedMessage(lfile.getContext(), lfile.getPath(),
                        FileChangeType.DELETED, System.currentTimeMillis(),
                        ILocalizationFile.NON_EXISTENT_CHECKSUM));
    }

    @Override
    protected String getOperation() {
        return "delete";
    }

}
