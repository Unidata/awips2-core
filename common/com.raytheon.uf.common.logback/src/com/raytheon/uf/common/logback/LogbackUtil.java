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
package com.raytheon.uf.common.logback;

import ch.qos.logback.core.Context;

import com.raytheon.uf.common.util.SystemUtil;

/**
 * Static utility methods for other classes in this package.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 10, 2014 3675       njensen     Initial creation
 * Feb 18, 2015 4015       rferrel     Standard constants and determineLogFilenamePattern.
 * Jun 09, 2015 4473       njensen     Moved from status to logback plugin
 * Jun 07, 2016 5258       bsteffen    Include sequence number in default message
 *                                     pattern to allow log forging detection.
 * 
 * </pre>
 * 
 * @author njensen
 */
public class LogbackUtil {

    /* Context Property names. */
    public final static String LOG_MESSAGE_PATTERN_PROP = "log.message.pattern";

    public final static String LOG_DIR_HOME_PROP = "log.dir.home";

    public final static String LOG_FILE_BASE_PROP = "log.file.base";

    public final static String LOG_FILE_MODE_PROP = "log.file.mode";

    /**
     * Format to get standard pattern for log file names.
     */
    private final static String FILENAME_PATTERN_FORMAT = "%s/logs/%s-%s-%s-%%d{yyyMMdd}.log";

    /**
     * Standard number of history files.
     */
    public final static int STD_HISTORY = 30;

    /**
     * Standard format for log entry.
     */
    private final static String UF_MESSAGE_PATTERN = "%-5p %d %4.4lsn [%t] %c{0}: %m%n";

    private LogbackUtil() {
        // do not allow instantiation
    }

    /**
     * Replaces %PID% in a filename with the actual PID of this process
     * 
     * @param filename
     *            the filename that potentially contains %PID%
     * @return the new filename with %PID% replaced if it was present
     */
    public static String replacePid(String filename) {
        return filename.replace("%PID%", Integer.toString(SystemUtil.getPid()));
    }

    /**
     * The pattern to use for log messages.
     * 
     * @param context
     * @return format
     */
    public static String getUFMessagePattern(Context context) {
        String format = context.getProperty(LOG_MESSAGE_PATTERN_PROP);
        if (format == null) {
            format = UF_MESSAGE_PATTERN;
        }
        return format;
    }

    /**
     * Get standard pattern for log file name.
     * 
     * @param name
     * @return filenamePattern
     */
    public static String determineUFFilenamePattern(Context context, String name)
            throws AssertionError {

        String logDirHome = context.getProperty(LOG_DIR_HOME_PROP);
        String logBase = context.getProperty(LOG_FILE_BASE_PROP);
        String logMode = context.getProperty(LOG_FILE_MODE_PROP);
        StringBuilder msg = new StringBuilder();
        int errCnt = 0;
        if (logDirHome == null) {
            msg.append(LOG_DIR_HOME_PROP).append(", ");
            ++errCnt;
        }
        if (logBase == null) {
            msg.append(LOG_FILE_BASE_PROP).append(", ");
            ++errCnt;
        }
        if (logMode == null) {
            msg.append(LOG_FILE_MODE_PROP).append(", ");
            ++errCnt;
        }
        if (msg.length() > 0) {
            msg.setLength(msg.length() - 2);
            msg.append(".");
            if (errCnt == 1) {
                msg.insert(0, "Property not defined ");
            } else {
                msg.insert(0, "Following properties not defined ");
            }
            throw new AssertionError(msg);
        }
        String filenamePattern = String.format(
                LogbackUtil.FILENAME_PATTERN_FORMAT, logDirHome, logBase,
                logMode, name);
        return filenamePattern;
    }
}
