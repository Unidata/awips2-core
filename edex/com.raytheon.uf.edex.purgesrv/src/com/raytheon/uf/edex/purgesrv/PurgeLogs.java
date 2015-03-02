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
package com.raytheon.uf.edex.purgesrv;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.edex.database.purge.PurgeLogger;

/**
 * PurgeLogs compresses or removes log files ( and archives ) from logDirectory
 * based on the YYYYMMDD timestamp in the filename.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket   Engineer    Description
 * ------------ -------- ----------- --------------------------
 * May 4, 2011           ekladstrup  Initial creation.
 * Feb 14, 2013 1638     mschenke    Moved class to purgesrv project from edex.logs.
 * Feb 26, 2015 4165     rjpeter     Make purge configurable.
 * </pre>
 * 
 * @author ekladstrup
 * @version 1.0
 */
public class PurgeLogs {
    private static final int DEFAULT_UNCOMPRESSED_DAYS = 6;

    private static final int DEFAULT_COMPRESSED_DAYS = 14;

    private enum FILE_OPERATION {
        COMPRESS, DELETE
    }

    private static final Pattern TIME_PATTERN = Pattern
            .compile("^.*(\\d{4}\\d{2}\\d{2})\\.(?:log|zip)(?:\\.\\d+)?(?:\\.lck)?$");

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");

    private static final String plugin = "Purge Logs";

    private String logDirectory;

    private int compressedDays = DEFAULT_COMPRESSED_DAYS;

    private int uncompressedDays = DEFAULT_UNCOMPRESSED_DAYS;

    public synchronized void purge() {
        // System.out.println("start purge logs");
        PurgeLogger.logInfo("---------START LOG PURGE---------", plugin);
        int skipped = 0;
        // get log directory
        if (logDirectory != null) {
            File logDir = new File(logDirectory);
            if (logDir.exists()) {
                // from edex log directory get age of logs and archives per day
                Map<Date, List<String>> filesByDay = new HashMap<>();
                String[] fileNames = logDir.list();
                for (String fileName : fileNames) {
                    // check that it is a log file
                    Matcher m = TIME_PATTERN.matcher(fileName);
                    if (m.matches() == false) {
                        skipped++;
                        PurgeLogger.logInfo(
                                "Skipped unknown file: " + fileName, plugin);
                        continue;
                    }

                    // found match
                    try {
                        Date day = sdf.parse(m.group(1));
                        List<String> files = filesByDay.get(day);

                        if (files == null) {
                            files = new ArrayList<>(50);
                            filesByDay.put(day, files);
                        }

                        files.add(fileName);
                    } catch (ParseException e) {
                        skipped++;
                        // improper date format, just skip this file.
                        PurgeLogger.logError(
                                "Invalid date format encountered in filename: "
                                        + fileName, plugin);
                    }
                }

                // delete any files over COMPRESSED_DAYS
                int count = process(filesByDay, compressedDays,
                        FILE_OPERATION.DELETE);
                PurgeLogger.logInfo("Removed " + count + " old files", plugin);

                // compress and remove any files over UNCOMPRESSED_DAYS
                count = process(filesByDay, uncompressedDays,
                        FILE_OPERATION.COMPRESS);
                PurgeLogger.logInfo("Archived " + count + " files", plugin);
            }
        }

        PurgeLogger.logInfo("Skipped processing " + skipped + " files", plugin);
        PurgeLogger.logInfo("---------END LOG PURGE-----------", plugin);
    }

    /**
     * Processes the filesByDay before the cut off by performing the given
     * operation. Entries that are processed are removed from the filesByDay
     * map.
     * 
     * @param filesByDay
     * @param dayCutOff
     * @param op
     * @return
     */
    private int process(Map<Date, List<String>> filesByDay, int dayCutOff,
            FILE_OPERATION op) {
        // generate cut off time
        Calendar cutoffTime = TimeUtil.newGmtCalendar();
        TimeUtil.minCalendarFields(cutoffTime, Calendar.HOUR_OF_DAY,
                Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND);
        cutoffTime.add(Calendar.DAY_OF_MONTH, -dayCutOff);

        int count = 0;
        Iterator<Entry<Date, List<String>>> iter = filesByDay.entrySet()
                .iterator();
        while (iter.hasNext()) {
            Entry<Date, List<String>> entry = iter.next();
            Date key = entry.getKey();
            if (key.getTime() < cutoffTime.getTimeInMillis()) {
                List<String> files = entry.getValue();
                iter.remove();

                if (files == null || files.isEmpty()) {
                    // safety check
                    continue;
                }

                switch (op) {
                case COMPRESS:
                    // add all files in the list into YYYYMMDD.zip
                    String name = logDirectory + "/" + sdf.format(key) + ".zip";
                    count += compressFiles(name, files);
                    break;
                case DELETE:
                    count += removeFiles(files);
                    break;
                }
            }
        }

        return count;
    }

    /**
     * Deletes files.
     * 
     * @param files
     * @return
     */
    private int removeFiles(List<String> files) {
        int count = 0;

        for (String file : files) {
            // delete the file
            String fullPath = logDirectory + "/" + file;
            File tmp = new File(fullPath);
            if (tmp.exists()) {
                count++;
                tmp.delete();
            }
        }

        return count;
    }

    /**
     * Add files to archive and deletes original file.
     * 
     * @param zipName
     * @param files
     * @return
     */
    private int compressFiles(String zipName, List<String> files) {
        int count = 0;

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
                zipName))) {
            for (String file : files) {
                String fullPath = logDirectory + "/" + file;
                File tmpFile = new File(fullPath);
                if (tmpFile.exists()) {
                    count++;
                    try (InputStream in = new FileInputStream(fullPath)) {
                        zos.putNextEntry(new ZipEntry(file));

                        int len;
                        byte[] buffer = new byte[4096];
                        while ((len = in.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }

                        tmpFile.delete();
                    } finally {
                        zos.closeEntry();
                    }
                }
            }
        } catch (FileNotFoundException e) {
            /*
             * we check if the file exists before opening, this should never
             * happen
             */
            PurgeLogger.logError("Unexpected exception caught", plugin, e);

        } catch (IOException e) {
            /*
             * This should not happen either, could be caused by attempting to
             * write in a folder where the user does not have proper permissions
             */
            PurgeLogger.logError("Unexpected excetion caught", plugin, e);
        }

        return count;
    }

    /**
     * set the log directory
     * 
     * @param logDirectory
     */
    public void setLogDirectory(String logDirectory) {
        this.logDirectory = logDirectory;
    }

    /**
     * get the log directory
     * 
     * @return
     */
    public String getLogDirectory() {
        return logDirectory;
    }

    /**
     * @return the compressedDays
     */
    public int getCompressedDays() {
        return compressedDays;
    }

    /**
     * @param compressedDays
     *            the compressedDays to set
     */
    public void setCompressedDays(String compressedDays) {
        try {
            this.compressedDays = Integer.parseInt(compressedDays);
        } catch (NumberFormatException e) {
            PurgeLogger.logError(
                    "compressedDays not a valid integer.  Setting to default of "
                            + DEFAULT_COMPRESSED_DAYS, plugin, e);
            this.compressedDays = DEFAULT_COMPRESSED_DAYS;
        }

        if (this.compressedDays < 0) {
            PurgeLogger.logError(
                    "compressedDays cannot be negative, setting to 0", plugin);
        }
    }

    /**
     * @return the uncompressedDays
     */
    public int getUncompressedDays() {
        return uncompressedDays;
    }

    /**
     * @param uncompressedDays
     *            the uncompressedDays to set
     */
    public void setUncompressedDays(String uncompressedDays) {
        try {
            this.uncompressedDays = Integer.parseInt(uncompressedDays);
        } catch (NumberFormatException e) {
            PurgeLogger.logError(
                    "uncompressedDays not a valid integer.  Setting to default of "
                            + DEFAULT_UNCOMPRESSED_DAYS, plugin, e);
            this.uncompressedDays = DEFAULT_UNCOMPRESSED_DAYS;
        }

        if (this.compressedDays < 0) {
            PurgeLogger.logError(
                    "compressedDays cannot be negative, setting to 0", plugin);
        }
    }
}
