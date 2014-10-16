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
package com.raytheon.uf.common.status.logback;

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
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class LogbackUtil {

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
    protected static String replacePid(String filename) {
        return filename.replace("%PID%", Integer.toString(SystemUtil.getPid()));
    }
}
