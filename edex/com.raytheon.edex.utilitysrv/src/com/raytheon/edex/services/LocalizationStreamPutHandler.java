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
package com.raytheon.edex.services;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.raytheon.uf.common.localization.FileLocker;
import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.stream.LocalizationStreamPutRequest;
import com.raytheon.uf.common.protectedfiles.ProtectedFiles;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * A request handler for LocalizationStreamPutRequests. Supports receiving
 * chunks of files.
 * 
 * @deprecated Continues to exist to support older clients. Newer clients should
 *             use the Localization REST service.
 * 
 *             <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 14, 2017 6111       njensen     Extracted from LocalizationStreamHandler
 * Aug 08, 2017 6379       njensen     Updated import of ProtectedFiles
 *
 *             </pre>
 *
 * @author njensen
 */

@Deprecated
public class LocalizationStreamPutHandler
        extends LocalizationStreamHandler<LocalizationStreamPutRequest> {

    @Override
    public Object handleRequest(LocalizationStreamPutRequest request)
            throws Exception {
        File file = super.validate(request);

        LocalizationLevel protectedLevel = ProtectedFiles.getProtectedLevel(
                request.getLocalizedSite(),
                request.getContext().getLocalizationType(),
                request.getFileName());
        if (protectedLevel != null && protectedLevel != request.getContext()
                .getLocalizationLevel()) {
            throw new LocalizationException("File: "
                    + request.getContext().getLocalizationType().toString()
                            .toLowerCase()
                    + File.separator + request.getFileName()
                    + " is protected and cannot be overridden");
        }
        return handleStreamingPut(request, file);
    }

    private Object handleStreamingPut(LocalizationStreamPutRequest request,
            File file) throws Exception {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        /*
         * TODO Verify file's pre-modification checksum is the non-existent file
         * checksum or matches the server file's current checksum. If not, throw
         * LocalizationFileChangedOutFromUnderYouException.
         */

        File tmpFile = new File(file.getParentFile(),
                "." + file.getName() + "." + request.getId());
        if (!tmpFile.exists() && (request.getOffset() != 0)) {
            throw new LocalizationException(
                    "Illegal state, request has offset set but file "
                            + "has not begun being written to yet.");
        } else if (tmpFile.exists()
                && (tmpFile.length() != request.getOffset())) {
            throw new LocalizationException(
                    "Illegal state, request's offset does not match size of file, size = "
                            + tmpFile.length() + " offset = "
                            + request.getOffset());
        }

        // if start of request, delete existing temporary file
        if (request.getOffset() == 0) {
            tmpFile.delete();
        }

        if (!tmpFile.exists()) {
            tmpFile.createNewFile();
        }

        try (FileOutputStream outputStream = new FileOutputStream(tmpFile,
                true)) {
            byte[] bytes = request.getBytes();
            outputStream.write(bytes);
        }

        if (request.isEnd()) {
            try {
                FileLocker.lock(this, file, Type.WRITE);
                FileChangeType changeType = FileChangeType.UPDATED;
                if (!file.exists()) {
                    changeType = FileChangeType.ADDED;
                }

                Files.move(tmpFile.toPath(), file.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);

                // generate checksum after change
                String checksum = ChecksumIO.writeChecksum(file);
                long timeStamp = file.lastModified();

                EDEXUtil.getMessageProducer().sendAsync(
                        UtilityManager.NOTIFY_ID,
                        new FileUpdatedMessage(request.getContext(),
                                request.getFileName(), changeType, timeStamp,
                                checksum));
                return timeStamp;
            } finally {
                FileLocker.unlock(this, file);
            }
        }

        return tmpFile.lastModified();
    }
    
    @Override
    public Class<?> getRequestType() {
        return LocalizationStreamPutRequest.class;
    }

}
