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
package com.raytheon.uf.common.localization.checksum;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Utilities to support checkums
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 17, 2015  4834      njensen     Initial creation
 *                                      Extracted from UtilityManager
 *                                      Refactored getFileChecksum(File)
 * Feb 03, 2016  4754      bsteffen    Fix concurrency issue
 * May 03, 2017  6258      tgurney     Set permissions on checksum file
 * Sep  8, 2017  6255      tgurney     Check ownership before setting permissions
 *
 * </pre>
 *
 * @author njensen
 */

public class ChecksumIO {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(ChecksumIO.class);

    private ChecksumIO() {
        // don't allow instantiation
    }

    /**
     * Gets the checksum for a particular file. If one does not exist, creates a
     * checksum file alongside the file.
     *
     * @param file
     * @return
     */
    public static String getFileChecksum(File file) {
        return getFileChecksum(file, true);
    }

    /**
     * Gets the checksum for a particular file. If one does not exist and write
     * is true, creates a checksum file alongside the file.
     *
     * @param file
     * @parm write
     * @return
     */
    public static String getFileChecksum(File file, boolean write) {
        // TODO: Fix FileLocker so it never times out in test driver
        File checksumFile = getChecksumFile(file);
        String chksum = null;
        try {
            if (file.exists()) {
                if (file.isDirectory()) {
                    chksum = ILocalizationFile.DIRECTORY_CHECKSUM;
                } else if (checksumFile.exists()
                        && checksumFile.lastModified() >= file.lastModified()) {
                    try (BufferedReader reader = new BufferedReader(
                            new FileReader(checksumFile))) {
                        /*
                         * This will return null if another thread is in
                         * writeChecksum, make sure to recalculate if this
                         * returns null
                         */
                        chksum = reader.readLine();
                    }
                }

                if (chksum == null) {
                    if (write) {
                        chksum = writeChecksum(file);
                    } else {
                        chksum = Checksum.getMD5Checksum(file);
                    }
                }
            } else {
                chksum = ILocalizationFile.NON_EXISTENT_CHECKSUM;
            }

        } catch (Exception e) {
            // log, no checksum will be provided
            logger.error("Error determing file checksum for: " + file, e);
        }
        return chksum;
    }

    private static File getChecksumFile(File utilityFile) {
        return new File(utilityFile.getParentFile(),
                utilityFile.getName() + Checksum.CHECKSUM_FILE_EXTENSION);
    }

    /**
     * Writes out a checksum file alongside the file. Returns the checksum
     * generated for that file.
     *
     * @param file
     * @return
     * @throws Exception
     */
    public static String writeChecksum(File file) throws Exception {
        String chksum = Checksum.getMD5Checksum(file);
        File checksumFile = getChecksumFile(file);
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(checksumFile))) {
            bw.write(chksum);
        }
        String owner = Files.getOwner(checksumFile.toPath()).getName();
        try {
            if (owner.equals(System.getProperty("user.name"))) {
                Files.setPosixFilePermissions(checksumFile.toPath(),
                        LocalizationFile.FILE_PERMISSIONS);
            } else {
                logger.info("Not changing permissions for " + checksumFile
                        + " because it is owned by user " + owner);
            }
        } catch (IOException e) {
            logger.handle(Priority.PROBLEM,
                    "Failed to set permissions on file: "
                            + checksumFile.getAbsolutePath(),
                    e);
        }
        return chksum;
    }

}
