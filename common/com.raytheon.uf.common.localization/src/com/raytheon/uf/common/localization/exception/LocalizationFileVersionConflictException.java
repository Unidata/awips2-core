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
 * A localization exception for when different versions of the same file
 * conflict with one another.
 * 
 * A simple example is when two different clients start with the same base
 * version of a file and then both change the file.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 1, 2015  4834       njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class LocalizationFileVersionConflictException extends
        LocalizationException {

    private static final long serialVersionUID = 1L;

    public LocalizationFileVersionConflictException(String message) {
        super(message);
    }

    public LocalizationFileVersionConflictException(String message, Throwable t) {
        super(message, t);
    }

    public LocalizationFileVersionConflictException(Throwable t) {
        super(t);
    }

}
