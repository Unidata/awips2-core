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
package com.raytheon.viz.ui.simulatedtime;

/**
 * Exception to be thrown when an operation is not allowed to be performed when
 * in CAVE's OPERATIONAL or TEST mode with SimulatedTime enabled.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 11, 2015  #4858     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

public final class SimulatedTimeProhibitedOpException extends Exception {

    private static final long serialVersionUID = -2901613775849790177L;

    /**
     * 
     */
    public SimulatedTimeProhibitedOpException() {
        super();
    }

    /**
     * @param message
     */
    public SimulatedTimeProhibitedOpException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public SimulatedTimeProhibitedOpException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public SimulatedTimeProhibitedOpException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public SimulatedTimeProhibitedOpException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
