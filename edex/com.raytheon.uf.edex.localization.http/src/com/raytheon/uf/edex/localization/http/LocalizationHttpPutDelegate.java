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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.text.SimpleDateFormat;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.util.file.Files;

/**
 * Handles PUT http method for localization files.
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
public class LocalizationHttpPutDelegate
        extends LocalizationHttpModificationDelegate {

    public LocalizationHttpPutDelegate(Path basePath) {
        super(basePath);
    }

    /**
     * Handle HTTP PUT requests for localization files
     *
     * @param request
     * @param response
     * @throws IOException
     */
    @Override
    public void performModification(HttpServletRequest request,
            HttpServletResponse response, LocalizationFile lfile)
            throws Exception {
        File file = lfile.getFile();
        FileChangeType changeType = FileChangeType.UPDATED;
        if (!file.exists()) {
            changeType = FileChangeType.ADDED;
        }

        /*
         * Proceed with the put by writing it to a temporary file and then
         * replacing the original file.
         */
        Path parentPath = file.toPath().getParent();
        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile(parentPath, file.getName(), ".tmp",
                    PosixFilePermissions.asFileAttribute(
                            LocalizationFile.FILE_PERMISSIONS));
            try (FileOutputStream fos = new FileOutputStream(
                    tmpFile.toFile())) {
                try (InputStream is = request.getInputStream()) {
                    LocalizationHttpDataTransfer.copy(is, fos);
                }
            }
            java.nio.file.Files.move(tmpFile, file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (Throwable t) {
            if (tmpFile != null) {
                java.nio.file.Files.deleteIfExists(tmpFile);
            }
            throw t;
        }

        // generate the new checksum after the change
        String checksum = ChecksumIO.writeChecksum(file);
        long timeStamp = file.lastModified();

        // notify topic the file has changed
        sendFileUpdateMessage(new FileUpdatedMessage(lfile.getContext(),
                lfile.getPath(), changeType, timeStamp, checksum));

        response.setStatus(HttpServletResponse.SC_OK);
        SimpleDateFormat format = TIME_HEADER_FORMAT.get();
        response.setHeader(LAST_MODIFIED_HEADER, format.format(timeStamp));
        response.setHeader(CONTENT_MD5_HEADER, checksum);
    }

    @Override
    protected String getOperation() {
        return "write";
    }

}
