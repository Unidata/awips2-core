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
 * An exception for when a request would generate a response size that exceeds a
 * set limit.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 28, 2015 2866       nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 * @version 1.0
 */
public class ResponseTooLargeException extends DataAccessException {

    private static final String DEFAULT_MESSAGE = "The response is too large to"
            + " be returned. Try narrowing the request by removing parameters "
            + "or adding identifiers, location names, or times.";

    private static final long serialVersionUID = 1L;

    /**
     * Uses the default message: {@value #DEFAULT_MESSAGE}.
     */
    public ResponseTooLargeException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * @param message
     */
    public ResponseTooLargeException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param cause
     */
    public ResponseTooLargeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Uses the default message: {@value #DEFAULT_MESSAGE}.
     *
     * @param cause
     */
    public ResponseTooLargeException(Throwable cause) {
        super(DEFAULT_MESSAGE, cause);
    }

}
