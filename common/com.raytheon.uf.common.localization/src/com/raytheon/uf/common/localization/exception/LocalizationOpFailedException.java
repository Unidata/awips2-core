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
package com.raytheon.uf.common.localization.exception;

/**
 * 
 * Signifies that a localization operation failed (for reasons other than
 * communication)
 * 
 * @deprecated Please use LocalizationException or any of its non-deprecated
 *             subclasses. This class only continues to exist for backwards
 *             compatibility and will be removed in the near future.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * May 15, 2008 #878        chammack    Initial creation
 * Nov 30, 2015  4834       njensen     Deprecated
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

@Deprecated
public class LocalizationOpFailedException extends LocalizationException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message
     */
    public LocalizationOpFailedException(String message) {
        super(message);
    }

    /**
     * @param message
     * @param t
     */
    public LocalizationOpFailedException(String message, Throwable t) {
        super(message, t);
    }

    /**
     * @param t
     */
    public LocalizationOpFailedException(Throwable t) {
        super(t);
    }

}
