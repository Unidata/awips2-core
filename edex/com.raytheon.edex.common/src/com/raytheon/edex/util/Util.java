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

package com.raytheon.edex.util;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;

import com.raytheon.uf.common.util.FileUtil;

/**
 * Contains utility methods for use in common.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 14Jun2006    TO 3        G Armendariz Initial baseline
 * 02Jul2007    333         MW Fegan    Removed Lucene based methods.
 * 08/10/2007   379         jkorman     Added copyFile method.
 * 10Apr2008    1068        MW Fegan    Remove redundant memory reporting.
 * 15Jul2008    1014        MW Fegan    Improved logging of JiBX marshaling errors.
 * Aug 20, 2008             dglazesk    Added functions for handling JaXB marshalling
 * Nov 09, 2012 1322        djohnson    Add close for Spring context.
 * Feb 15, 2013 1638        mschenke    Deleted unused functions and moved ones used by common/viz
 *                                      code into common projects
 * Jun 25, 2015 4495        njensen     Removed dead code, deprecated near death code
 * Dec 17, 2015 5166        kbisanz     Update logging to use SLF4J
 * 
 * </pre>
 * 
 * @author mfegan
 * @version 1
 */
public final class Util {

    protected static Logger logger = LoggerFactory.getLogger(Util.class);

    /**
     * Easy reference to system-dependent end of line
     * 
     * @deprecated Use FileUtil.EOL instead
     */
    @Deprecated
    public static final String EOL = FileUtil.EOL;

    private Util() {
        // No Instantiation
    }

    /**
     * Converts a string in YYYYMMDDhhmmss format to a {@link Calendar}.
     * Milli-seconds are set to zero. If the input value is not a valid date
     * time string, the current time is returned.
     * 
     * @param date
     *            the formated date string
     * @return the calendar representing the date
     */
    @Deprecated
    public static GregorianCalendar convertStr14ToCal(String date) {
        Pattern p = Pattern
                .compile("(\\d{4})(\\d{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})");
        Matcher m = p.matcher(date);

        // get a calender based on GMT timezone
        // all time stamp strings from the catalog query are in GMT
        GregorianCalendar calendar = new GregorianCalendar(
                TimeZone.getTimeZone("GMT"));

        if (m.matches()) {
            int year = Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2)) - 1;
            int day = Integer.parseInt(m.group(3));
            int hour = Integer.parseInt(m.group(4));
            int min = Integer.parseInt(m.group(5));
            int sec = Integer.parseInt(m.group(6));
            calendar.set(year, month, day, hour, min, sec);
            calendar.set(Calendar.MILLISECOND, 0);
        }
        return calendar;
    }

    /**
     * Creates a new class loaded by adding the specified list of files to the
     * existing class loader.
     * 
     * @param files
     *            the list of files to access with the class loader
     * 
     * @return the class loader for the listed files
     * 
     * @throws MalformedURLException
     */
    @Deprecated
    public static URLClassLoader getLibrariesClassLoader(File[] files)
            throws MalformedURLException {
        URL[] urls = new URL[files.length];

        // iterate through the list of files and get a URL for each
        for (int i = 0; i < files.length; i++) {
            // logger.debug("Utl: adding " + files[i].toString());
            urls[i] = files[i].toURI().toURL();
        }

        // finally, add the urls to the class loader and return
        return URLClassLoader.newInstance(urls, Thread.currentThread()
                .getContextClassLoader());

    }

    /**
     * Create a temporary file from a byte array message inthe OS's temporary
     * directory This file is removed from the OS when the JVM terminates, but
     * you must specify {@link File#deleteOnExit()}
     * 
     * @param message
     *            The byte array to write
     * @return A file with this data
     */
    public static File createTempFile(byte[] message, String filePrefix) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(message.length);
        File file = null;
        FileOutputStream fos = null;
        try {
            file = File.createTempFile(filePrefix, null);
            fos = new FileOutputStream(file);
            bos.write(message);
            bos.writeTo(fos);
        } catch (FileNotFoundException e) {
            logger.error("Unable to find file", e);
        } catch (IOException e) {
            logger.error("Unable to open file", e);
        } finally {
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    logger.error("Unable to close output stream");
                }
            }

            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    logger.error("Unable to close input stream");
                }
            }
        }

        return file;
    }

    /**
     * Converts a ddhhmm time group to a Calendar. Adjusts the calendar as
     * follows: Any time group with a day (dd) in the future is set back one
     * month.
     * 
     * @deprecated Use WMOTimeParser instead
     * 
     * 
     * @param baseTime
     *            the time to convert
     * 
     * @return the converted time
     * 
     * @throws DataFormatException
     *             if an error occurs
     */
    @Deprecated
    public static Calendar findCurrentTime(String baseTime)
            throws DataFormatException {
        Calendar retVal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        try {
            String regexe = "(\\d{2})(\\d{2})(\\d{2})[Zz]?";
            Pattern pattern = Pattern.compile(regexe);
            Matcher matcher = pattern.matcher(baseTime);
            if (matcher.matches()) {
                adjustDayHourMinute(retVal, matcher.group(1), matcher.group(2),
                        matcher.group(3));
            } else {
                throw new ParseException("Invalid format - does not match "
                        + regexe, 0);
            }
        } catch (Exception e) {
            throw new DataFormatException("Unable to find current time for "
                    + baseTime + ", exception was " + e.toString());
        }
        return retVal;
    }

    /**
     * Adjusts the calendar from the current date to the specified date. If the
     * specified date is later than the current date, the calendar is "backed
     * up" one month. In addition, the second and millisecond fields are set to
     * zero.
     * 
     * @param cal
     *            the calendar to adjust
     * @param day
     *            the new day of month
     * @param hour
     *            the new hour of day
     * @param minute
     *            the new minute of the hour
     */
    @Deprecated
    private static void adjustDayHourMinute(Calendar cal, String day,
            String hour, String minute) {
        int iDay = Integer.parseInt(day);
        int iHour = Integer.parseInt(hour);
        int iMinute = Integer.parseInt(minute);
        int iMonth = cal.get(Calendar.MONTH);
        int iYear = cal.get(Calendar.YEAR);
        // adjust the month and year for roll-over situations
        if (iDay > cal.get(Calendar.DAY_OF_MONTH)) {
            iMonth--;
            if (iMonth < 0) {
                iMonth = Calendar.DECEMBER;
                iYear--;
            }
        }
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.YEAR, iYear);
        cal.set(Calendar.MONTH, iMonth);
        cal.set(Calendar.DAY_OF_MONTH, iDay);
        cal.set(Calendar.HOUR_OF_DAY, iHour);
        cal.set(Calendar.MINUTE, iMinute);

    }

    /**
     * Closes any closeable object.
     * 
     * @deprecated Use Java 1.7 try-with-resources
     * 
     * @param c
     *            An closeable target.
     * @return Close status.
     */
    @Deprecated
    public static final String close(Closeable c) {
        String status = null;

        if (c != null) {
            try {
                c.close();
            } catch (IOException ioe) {
                status = ioe.toString();
            }
        }
        return status;
    }

    // TODO move the resize/subgrid methods somewhere better

    /**
     * Resizes a 1-D data array into a 2-D array based on the provided row and
     * column count
     * 
     * @param data
     *            The 1-D array of data
     * @param columnCount
     *            The number of columns to map the data to
     * @param rowCount
     *            The number of rows to map the data to
     * @return The 2-D array of data
     */
    public static float[][] resizeDataTo2D(float[] data, int columnCount,
            int rowCount) {
        float[][] newGrid = new float[rowCount][columnCount];

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                newGrid[row][column] = data[row * columnCount + column];
            }
        }

        return newGrid;
    }

    /**
     * Resizes a 2-D array of data to a 1-D array
     * 
     * @param data
     *            The 2-D array of data
     * @param rowCount
     *            The number of rows in the 2-D data array
     * @param columnCount
     *            The number of columns in the 2-D data array
     * @return The 1-D array of data
     */
    public static float[] resizeDataTo1D(float[][] data, int rowCount,
            int columnCount) {
        float[] newGrid = new float[rowCount * columnCount];

        for (int row = 0; row < rowCount; row++) {
            for (int column = 0; column < columnCount; column++) {
                newGrid[row * columnCount + column] = data[row][column];
            }
        }
        return newGrid;
    }

    public static void insertSubgrid(float[][] data, float[][] subGrid,
            int startColumn, int startRow, int columnCount, int rowCount) {

        for (int row = startRow; row < rowCount + startRow; row++) {
            for (int column = startColumn; column < columnCount + startColumn; column++) {
                // allow data off the end to wrap to the other side this is only
                // makes sense for Lat Lon data that wraps around the world.
                int dataColumn = column % data[row].length;
                data[row][dataColumn] = subGrid[row - startRow][column
                        - startColumn];
            }
        }
    }

    /**
     * Performs a safe-close on a {@link ConfigurableApplicationContext}.
     * 
     * @param ctx
     *            the context
     */
    @Deprecated
    public static String close(final ConfigurableApplicationContext ctx) {
        // Just adapt to a normal Java closeable
        return close(new Closeable() {
            @Override
            public void close() throws IOException {
                if (ctx != null) {
                    ctx.close();
                }
            }
        });
    }

}
