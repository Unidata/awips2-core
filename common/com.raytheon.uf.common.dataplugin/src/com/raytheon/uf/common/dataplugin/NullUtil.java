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
package com.raytheon.uf.common.dataplugin;

/**
 * Utilities for non-nullable values to use in the database.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 28, 2015 4360       rferrel     Initial creation
 * Aug 20, 2015 4360       rferrel     Changed value for NULL_STRING and removed EMPTY_STRING.
 * 
 * </pre>
 * 
 * @author rferrel
 * @version 1.0
 */

public class NullUtil {
    /**
     * String to use to replace null value in char field.
     */
    public static final String NULL_STRING = "";

    /**
     * Value to use to replace null value in float/double fields.
     */
    public static final Float NULL_FLOAT = Float.NaN;

    /**
     * Instance of class not allowed.
     */
    private NullUtil() {
    }

    /**
     * 
     * @param value
     * @return true when value is the NULL_STRING
     */
    public static final boolean isNull(String value) {
        return NULL_STRING.equals(value);
    }

    public static final boolean isNull(Float value) {
        return NULL_FLOAT.equals(value);
    }

    /**
     * Check value and if it is the NULL_STRING return null.
     * 
     * @param value
     * @return null or value
     */
    public static String convertNullStringToNull(String value) {
        return isNull(value) ? null : value;
    }

    /**
     * When value is null return the NULL_STRING
     * 
     * @param value
     * @return NULL_STRING or value
     */
    public static String convertNullToNullString(String value) {
        return (value == null) ? NULL_STRING : value;
    }

    public static Float convertNullFloatoNull(Float value) {
        return NULL_FLOAT.equals(value) ? null : value;
    }

    public static Float convertNullToNullFloat(Float value) {
        return (value == null) ? NULL_FLOAT : value;
    }
}
