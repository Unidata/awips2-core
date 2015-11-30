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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import com.raytheon.edex.utility.ProtectedFiles;
import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.localization.FileLocker;
import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.stream.AbstractLocalizationStreamRequest;
import com.raytheon.uf.common.localization.stream.LocalizationStreamGetRequest;
import com.raytheon.uf.common.localization.stream.LocalizationStreamPutRequest;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Base handler for localization streaming requests. Delegates work off to a
 * get/put handler
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2010            mschenke     Initial creation
 * Jul 14, 2014 3372       njensen      fileMap is ConcurrentHashMap for thread safety
 * Jul 16, 2014 3378       bclement     removed cache
 * Feb 17, 2015 4137       reblum       fixed timestamp on put requests
 * Nov 16, 2015 4834       njensen      Send updated checksum on put
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class LocalizationStreamHandler
        extends
        AbstractPrivilegedLocalizationRequestHandler<AbstractLocalizationStreamRequest> {

    @Override
    public Object handleRequest(AbstractLocalizationStreamRequest request)
            throws Exception {
        File file = PathManagerFactory.getPathManager().getFile(
                request.getContext(), request.getFileName());
        if (file == null) {
            throw new LocalizationException("File with name, "
                    + request.getFileName() + ", and context, "
                    + String.valueOf(request.getContext())
                    + ", could not be found");
        }

        if (request instanceof LocalizationStreamGetRequest) {
            return handleStreamingGet((LocalizationStreamGetRequest) request,
                    file);
        } else if (request instanceof LocalizationStreamPutRequest) {
            LocalizationStreamPutRequest req = (LocalizationStreamPutRequest) request;
            LocalizationLevel protectedLevel = ProtectedFiles
                    .getProtectedLevel(req.getLocalizedSite(), request
                            .getContext().getLocalizationType(), request
                            .getFileName());
            if (protectedLevel != null
                    && protectedLevel != request.getContext()
                            .getLocalizationLevel()) {
                throw new LocalizationException("File: "
                        + request.getContext().getLocalizationType().toString()
                                .toLowerCase() + File.separator
                        + request.getFileName()
                        + " is protected and cannot be overridden");
            }
            return handleStreamingPut((LocalizationStreamPutRequest) request,
                    file);
        } else {
            throw new LocalizationException("Request type of "
                    + request.getClass() + " is not recognized");
        }
    }

    private Object handleStreamingGet(LocalizationStreamGetRequest request,
            File file) throws Exception {
        // TODO: Copy file to tmp location named from request unique id and
        // stream that file for the request to avoid put/delete/read issues
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            int bytesSkipped = 0;
            int toSkip = request.getOffset();

            // we are done if we the toSkip is 0, or toSkip increased in size
            while ((toSkip != 0) && (toSkip <= request.getOffset())) {
                bytesSkipped += inputStream.skip(toSkip);
                toSkip = request.getOffset() - bytesSkipped;
            }

            if (toSkip != 0) {
                throw new LocalizationException("Error skipping through file");
            }

            LocalizationStreamPutRequest response = new LocalizationStreamPutRequest();
            byte[] bytes = new byte[request.getNumBytes()];
            int bytesRead = inputStream.read(bytes, 0, request.getNumBytes());

            if (bytesRead == -1) {
                response.setBytes(new byte[0]);
                response.setEnd(true);
            } else {
                if (bytesRead != bytes.length) {
                    bytes = Arrays.copyOf(bytes, bytesRead);
                }

                response.setBytes(bytes);
                response.setEnd(request.getOffset() + bytesRead == file
                        .length());
            }
            return response;
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private Object handleStreamingPut(LocalizationStreamPutRequest request,
            File file) throws Exception {
        if (file.getParentFile().exists() == false) {
            file.getParentFile().mkdirs();
        }

        /*
         * TODO Verify file's pre-modification checksum is the non-existent file
         * checksum or matches the server file's current checksum. If not, throw
         * LocalizationFileChangedOutFromUnderYouException.
         */

        File tmpFile = new File(file.getParentFile(), "." + file.getName()
                + "." + request.getId());
        if ((tmpFile.exists() == false) && (request.getOffset() != 0)) {
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

        // if (
        if (tmpFile.exists() == false) {
            tmpFile.createNewFile();
        }
        FileOutputStream outputStream = null;

        try {
            outputStream = new FileOutputStream(tmpFile, true);
            byte[] bytes = request.getBytes();
            outputStream.write(bytes);
        } finally {
            if (outputStream != null) {
                outputStream.flush();
                outputStream.close();
            }
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

                EDEXUtil.getMessageProducer()
                        .sendAsync(
                                UtilityManager.NOTIFY_ID,
                                new FileUpdatedMessage(request.getContext(),
                                        request.getFileName(), changeType,
                                        timeStamp, checksum));
                return timeStamp;
            } finally {
                FileLocker.unlock(this, file);
            }
        }

        return tmpFile.lastModified();
    }

    @Override
    public AuthorizationResponse authorized(IUser user,
            AbstractLocalizationStreamRequest request)
            throws AuthorizationException {
        if (request instanceof LocalizationStreamGetRequest) {
            // All gets are authorized
            return new AuthorizationResponse(true);
        } else if (request instanceof LocalizationStreamPutRequest) {
            LocalizationContext context = request.getContext();
            String fileName = request.getFileName();
            return getAuthorizationResponse(user, context, fileName,
                    request.getMyContextName());
        }
        return new AuthorizationResponse(true);
    }

}
