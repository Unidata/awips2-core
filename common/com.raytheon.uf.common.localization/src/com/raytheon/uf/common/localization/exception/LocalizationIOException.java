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
 * An exception for input/output errors related to localization.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2015  4834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class LocalizationIOException extends LocalizationException {

    private static final long serialVersionUID = 1L;

    public LocalizationIOException(String message) {
        super(message);
    }

    public LocalizationIOException(String message, Throwable t) {
        super(message, t);
    }

    public LocalizationIOException(Throwable t) {
        super(t);
    }

}
