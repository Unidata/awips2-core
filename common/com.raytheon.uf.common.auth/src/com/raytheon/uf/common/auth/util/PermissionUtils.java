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
package com.raytheon.uf.common.auth.util;

/**
 * Permission Utilities
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jul 20, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class PermissionUtils {
    /**
     * Delimiter used for permission strings
     */
    public static final String DELIMITER = ":";

    /*
     * Private constructor for class containing only static methods
     */
    private PermissionUtils() {
    }

    /**
     * Build a permission string from tokens
     *
     * @param tokens
     * @return the permission string
     */
    public static String buildPermissionString(String... tokens) {
        return String.join(DELIMITER, tokens);
    }

    /**
     * Build a permission string from a list of tokens
     *
     * @param tokens
     * @return the permission string
     */
    public static String buildPermissionString(
            Iterable<? extends CharSequence> tokens) {
        return String.join(DELIMITER, tokens);
    }
}
