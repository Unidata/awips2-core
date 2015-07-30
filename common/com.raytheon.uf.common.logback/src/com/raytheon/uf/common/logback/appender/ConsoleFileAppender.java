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
package com.raytheon.uf.common.logback.appender;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.io.output.TeeOutputStream;

import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Appender that modifies System.out and System.err PrintStreams to print to
 * their normal streams while also outputting to a file. The file is specified
 * by the setFile method.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 10, 2014 3675       njensen     Initial creation
 * Jun 09, 2015 4473       njensen     Moved from status to logback plugin
 * Jun 22, 2015 4148       rferrel     Refactored from EnvConfigSysStreamFileAppender.
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class ConsoleFileAppender<E> extends OutputStreamAppender<E> {
    private static final PrintStream originalOut = System.out;

    private static final PrintStream originalErr = System.err;

    private FileOutputStream fileStream;

    /**
     * Create an output stream base on the file name.
     * 
     * @param filename
     */
    public void setFile(String filename) {
        if (!OptionHelper.isEmpty(filename)) {
            configureStream(filename);
        }
    }

    private void configureStream(String filename) {
        try {
            fileStream = new FileOutputStream(filename);
            System.setOut(new PrintStream(new TeeOutputStream(originalOut,
                    fileStream)));
            System.setErr(new PrintStream(new TeeOutputStream(originalErr,
                    fileStream)));
            super.setOutputStream(fileStream);
        } catch (Throwable t) {
            reset();
            System.err.println("Error configuring FileOutputStream(" + filename
                    + ") for sysout and syserr");
            t.printStackTrace();
        }
    }

    private void reset() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        /*
         * Setting the output stream will close the FileOutputStream and attempt
         * to return this to normal. But if somehow reset() was called twice, we
         * want to make sure we don't close the original System.out stream.
         */
        if (getOutputStream() != originalOut) {
            super.setOutputStream(originalOut);
        }

        if (fileStream != null) {
            try {
                fileStream.close();
            } catch (IOException e) {
                // ignore
            }
            fileStream = null;
        }
    }

    @Override
    public void stop() {
        // reset will clean up the streams
        this.reset();
        started = false;
    }
}
