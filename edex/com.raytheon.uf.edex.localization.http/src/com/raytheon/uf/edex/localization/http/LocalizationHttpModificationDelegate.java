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
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.http.ProtectiveHttpOutputStream;
import com.raytheon.uf.common.localization.FileLocker;
import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

/**
 * Base class for handling HTTP methods that are going to modify localization
 * files. This class handles validation, authorization, and file locking so that
 * subclasses can focus on just performing the required modifications to the
 * localization files.
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
public abstract class LocalizationHttpModificationDelegate
        extends LocalizationHttpDelegate {

    protected static final String IF_MATCH_HEADER = "If-Match";

    public LocalizationHttpModificationDelegate(Path basePath) {
        super(basePath);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        String rawPath = request.getPathInfo();
        Path fullPath = Paths.get(rawPath);

        Path relative = basePath.relativize(fullPath);
        if (!relative.toString().isEmpty()) {
            relative = relative.normalize();
        }
        String acceptEncoding = request.getHeader(ACCEPT_ENC_HEADER);
        ProtectiveHttpOutputStream out = new ProtectiveHttpOutputStream(
                response, acceptEncoding, false);

        try {
            validate(request, relative);
            LocalizationFile lfile = LocalizationResolver.getFile(relative);

            /*
             * Passed all the initial checks, acquire the lock and verify
             * another process did not alter the file in the meantime. getFile()
             * will also create parent directories as necessary.
             */
            File file = lfile.getFile();
            try {
                FileLocker.lock(this, file, Type.WRITE);

                /*
                 * The LocalizationFile instance may have come from an in-memory
                 * cache, in which case we can't be 100% positive that the
                 * checksum in memory is up-to-date. An outside process that
                 * didn't go through normal routes may have modified the file,
                 * so we need to get the checksum from the file on the
                 * filesystem to be safe.
                 */
                String currentChecksum = ChecksumIO.getFileChecksum(file);
                String preModChecksum = request.getHeader(IF_MATCH_HEADER);
                if (!currentChecksum.equals(preModChecksum)) {
                    StringBuilder sb = new StringBuilder(256);
                    sb.append("The file ");
                    sb.append(lfile.getPath());
                    sb.append(" as version ");
                    String newContentMd5 = request
                            .getHeader(CONTENT_MD5_HEADER);
                    if (newContentMd5 == null) {
                        newContentMd5 = "**No Content-MD5 Header Supplied**";
                    }
                    sb.append(newContentMd5);
                    sb.append(
                            " has not been saved because it has been changed by another process. ");
                    sb.append("The client modified the file based on version ");
                    sb.append(preModChecksum);
                    sb.append(
                            " but the localization service's latest version is ");
                    sb.append(currentChecksum);
                    sb.append(
                            ". Please consider updating to the latest version of the ");
                    sb.append("file and merging the changes.");
                    throw new LocalizationHttpException(
                            HttpServletResponse.SC_CONFLICT, sb.toString());
                }

                performModification(request, response, lfile);
            } finally {
                FileLocker.unlock(this, file);
            }
        } catch (LocalizationHttpException e) {
            sendError(e, out);
        } catch (Throwable t) {
            log.error("Problem handling localization put request: " + fullPath,
                    t);
            sendError(new LocalizationHttpException(
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR, SERVER_ERROR),
                    out);
        } finally {
            out.flush();
            out.setAllowClose(true);
            out.close();
        }
    }

    @Override
    protected void validate(HttpServletRequest request, Path relative)
            throws LocalizationHttpException {
        if (LocalizationResolver.isContextQuery(relative)) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    "context information cannot be modified.");
        }
        LocalizationFile lfile = LocalizationResolver.getFile(relative);
        if (lfile.isDirectory()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_METHOD_NOT_ALLOWED,
                    request.getMethod()
                            + " requests not allowed on directories");
        }
        super.validate(request, relative);
    }

    public abstract void performModification(HttpServletRequest request,
            HttpServletResponse response, LocalizationFile lfile)
            throws Exception;

    protected void sendFileUpdateMessage(FileUpdatedMessage message)
            throws EdexException {
        EDEXUtil.getMessageProducer().sendAsync("utilityNotify", message);
    }

}
