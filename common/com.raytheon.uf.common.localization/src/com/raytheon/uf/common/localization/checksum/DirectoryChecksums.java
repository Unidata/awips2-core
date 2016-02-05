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
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * 
 * Provide a mechanism for storing all the checksums for the files in a
 * directory to a single file for more efficient IO. The file format is the same
 * as used by the md5sum command line utility so the utility can be used to
 * create or verify the files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Feb 03, 2016  4754     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class DirectoryChecksums {

    private static final IUFStatusHandler logger = UFStatus
            .getHandler(DirectoryChecksums.class);

    private static final String CHECKSUM_FILE = "checksum"
            + Checksum.CHECKSUM_FILE_EXTENSION;

    private static final Pattern linePattern = Pattern
            .compile("([0-9a-f]*)\\s*(\\S*)");

    private final File directory;

    /**
     * Entries are sorted so that they will be written in alphabetical order to
     * match the output of the md5sum command line utility.
     */
    private final SortedMap<String, String> checksums = new TreeMap<>();

    private boolean dirty = false;

    public DirectoryChecksums(File directory) {
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException(
                    directory + " is not a directory.");
        }
        this.directory = directory;
        read();
    }

    protected File getChecksumFile() {
        return new File(directory, CHECKSUM_FILE);
    }

    private void read() {
        File checksumFile = getChecksumFile();
        if (!checksumFile.exists()) {
            return;
        }
        long lastModified = checksumFile.lastModified();
        try (BufferedReader reader = new BufferedReader(
                new FileReader(checksumFile))) {
            String line = reader.readLine();
            while (line != null) {
                Matcher matcher = linePattern.matcher(line);
                if (matcher.matches()) {
                    String checksum = matcher.group(1);
                    String name = matcher.group(2);
                    File file = new File(directory, name);
                    if (file.lastModified() < lastModified) {
                        checksums.put(name, checksum);
                    }
                } else {
                    logger.warn("Cannot read md5sum file.");
                }
                line = reader.readLine();
            }
        } catch (IOException e) {
            /*
             * This is not fatal, just inefficient. getFileChecksum will fall
             * back to ChecksumIO for every file.
             */
            logger.handle(Priority.WARN,
                    "Error loading directory checksum for " + directory,
                    e);
        }
    }

    /**
     * Save all the file checksums to the checksum file. If the checksum file is
     * already up to date then this will do nothing.
     */
    public void save() {
        if (!dirty) {
            return;
        }
        File checksumFile = getChecksumFile();
        try (BufferedWriter bw = new BufferedWriter(
                new FileWriter(checksumFile))) {
            for (Entry<String, String> entry : checksums.entrySet()) {
                bw.write(entry.getValue());
                bw.write("  ");
                bw.write(entry.getKey());
                bw.write('\n');
            }
        } catch (IOException e) {
            /*
             * This is not fatal, just inefficient. future attempts will not be
             * able to reuse the checksum file.
             */
            logger.handle(Priority.WARN, "Error saving directory checksum for "
                    + directory, e);
        }
        dirty = true;
    }

    /**
     * Get the checksum for a single file that is within the directory. If the
     * checksum in the checksum file is current for this file then no disk IO is
     * necessary and this will be very fast. If this is the first time
     * calculating checksums for this directory or if the file has changed since
     * the checksum file was written then a new checksum will be calculated.
     * 
     * This is intended to be a replacement for ChecksumIO#getFileChecksum(File)
     * that can use a single checksum file for an entire directory.
     */
    public String getFileChecksum(File file) {
        if (!file.getParentFile().equals(directory)) {
            throw new IllegalArgumentException("File has wrong parent.");
        }
        String name = file.getName();
        String checksum = checksums.get(name);
        if (checksum == null) {
            checksum = ChecksumIO.getFileChecksum(file, true);
            checksums.put(name, checksum);
            dirty = true;
        }
        return checksum;
    }

}
