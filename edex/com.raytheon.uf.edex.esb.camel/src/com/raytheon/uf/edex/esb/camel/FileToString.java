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
package com.raytheon.uf.edex.esb.camel;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Set;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.file.Files;
import com.raytheon.uf.common.util.file.IOPermissionsHelper;

/**
 * Provides a capability to transform java.io.File to Strings. This is necessary
 * because camel transforms java.io.Files to byte[] on JMS.
 * <p>
 * To ensure proper processing, this file will need to be stored on a file
 * system accessible to all EDEX cluster members.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 03, 2008            chammack    Initial creation
 * Jun 12, 2012 00609      djohnson    Use EDEXUtil for EDEX_HOME.
 * Oct 16, 2018 #7522      dgilling    Move file to
 *                                     ${data.archive.root}/manual/edex_processing.
 * Mar  4, 2021  8326      tgurney     Fix for Camel 3 removal of fault API
 *
 * </pre>
 *
 * @author chammack
 */

@Deprecated
public class FileToString implements Processor {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private static final String DIR = System.getProperty("data.archive.root")
            + File.separator + "manual" + File.separator + "edex_processing";

    private static final Set<PosixFilePermission> POSIX_FILE_PERMISSIONS = EnumSet
            .of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE);

    private static final FileAttribute<Set<PosixFilePermission>> POSIX_DIRECTORY_ATTRIBUTES = PosixFilePermissions
            .asFileAttribute(EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_WRITE,
                    PosixFilePermission.GROUP_EXECUTE));

    private static final DateTimeFormatter SUB_DIR_FORMATTER = DateTimeFormatter
            .ofPattern("yyyyMMdd" + File.separatorChar + "HH")
            .withZone(ZoneOffset.UTC);

    @Override
    public void process(Exchange arg0) throws Exception {
        File file = (File) ((GenericFile<?>) arg0.getIn().getBody()).getFile();
        File newFile = moveFileToDataStore(file);
        if (newFile != null) {
            arg0.getIn().setBody(newFile.toString());
            arg0.getIn().setHeader("enqueueTime", System.currentTimeMillis());
        } else {
            arg0.setRouteStop(true);
        }
    }

    public File moveFileToDataStore(File inFile) throws IOException {
        if (!inFile.exists()) {
            statusHandler
                    .warn("Attempting to process non-existent file: " + inFile);
            return null;
        }

        String dateTimeString = ZonedDateTime.now(ZoneOffset.UTC)
                .format(SUB_DIR_FORMATTER);
        String path = FileUtil.join(DIR, dateTimeString);
        File dir = new File(path);
        File newFile = new File(dir, inFile.getName());

        try {
            if (!dir.exists()) {
                Files.createDirectories(dir.toPath(),
                        POSIX_DIRECTORY_ATTRIBUTES);
            }

            java.nio.file.Files.move(inFile.toPath(), newFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            /*
             * Attempt to adjust the file permissions to fulfill the security
             * requirements. As of May 2017, all of the files that will be
             * processed are provided by an external source.
             */
            try {
                IOPermissionsHelper.applyFilePermissions(newFile.toPath(),
                        POSIX_FILE_PERMISSIONS);
            } catch (Exception e) {
                /*
                 * Permission updates have failed. However, we still probably
                 * want to keep the file so that it can successfully be ingested
                 * and used?
                 */
                statusHandler.warn(e.getMessage(), e);
            }

            statusHandler.info("edex_processing: " + inFile.getAbsolutePath());
        } catch (IOException e) {
            statusHandler.error("Failed to move file ["
                    + inFile.getAbsolutePath()
                    + "] to edex_processing dir. File will be discarded.", e);
            inFile.delete();
            return null;
        }

        return newFile;
    }
}
