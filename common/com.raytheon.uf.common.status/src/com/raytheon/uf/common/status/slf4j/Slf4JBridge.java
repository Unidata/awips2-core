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
package com.raytheon.uf.common.status.slf4j;

import org.slf4j.Logger;
import org.slf4j.Marker;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Sends messages received from the UFStatus API to SLF4J.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 19, 2015            njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class Slf4JBridge {

    private Slf4JBridge() {
        // don't allow instantiation
    }

    /**
     * Logs a UFStatus to SLF4J by using the arguments passed in. Does NOT add
     * to or alter the marker. Markers must be set up/configured before calling
     * this method.
     * 
     * @param logger
     * @param priority
     * @param marker
     * @param msg
     * @param throwable
     */
    public static void logToSLF4J(Logger logger, Priority priority,
            Marker marker, String msg, Throwable throwable) {
        /*
         * priority will have already been added to the marker before this point
         */
        switch (priority) {
        case CRITICAL:
            logger.error(marker, msg, throwable);
            break;
        case SIGNIFICANT:
            logger.error(marker, msg, throwable);
            break;
        case PROBLEM:
            logger.warn(marker, msg, throwable);
            break;
        case EVENTA: // fall through
        case EVENTB:
            logger.info(marker, msg, throwable);
            break;
        case VERBOSE:
            logger.debug(marker, msg, throwable);
            break;
        }
    }

}
