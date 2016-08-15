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

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;

/**
 * A localization exception indicating that the file is protected at a
 * particular level.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 15, 2016  5834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 */

public class LocalizationProtectedFileException extends LocalizationException {

    private static final long serialVersionUID = 1L;

    protected LocalizationLevel protectionLevel;

    public LocalizationProtectedFileException(String message,
            LocalizationLevel protectionLevel) {
        super(message);
        this.protectionLevel = protectionLevel;
    }

    public LocalizationProtectedFileException(String message,
            LocalizationLevel protectionLevel, Throwable t) {
        super(message, t);
        this.protectionLevel = protectionLevel;
    }

    public LocalizationProtectedFileException(
            LocalizationLevel protectionLevel, Throwable t) {
        super(t);
        this.protectionLevel = protectionLevel;
    }

    public LocalizationLevel getProtectionLevel() {
        return protectionLevel;
    }

}
