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
package com.raytheon.uf.common.dataaccess.exception;

/**
 * An exception to indicate that a factory does not support this method yet but
 * will probably support it in the future.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 14, 2014 3184       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class MethodNotSupportedYetException extends DataAccessException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public MethodNotSupportedYetException() {
        super();
    }

    /**
     * Constructor
     * 
     * @param message
     *            the error message
     */
    public MethodNotSupportedYetException(String message) {
        super(message);
    }

    /**
     * Constructor
     * 
     * @param message
     *            the error message
     * @param cause
     *            the cause of the error
     */
    public MethodNotSupportedYetException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     * 
     * @param cause
     *            the cause of the error
     */
    public MethodNotSupportedYetException(Throwable cause) {
        super(cause);
    }

}
