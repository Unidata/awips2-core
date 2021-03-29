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
package com.raytheon.uf.common.pypies.response;

import java.util.StringJoiner;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Error Response object
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 17, 2010           njensen   Initial creation
 * Mar 18, 2021  8349     randerso  Added constructors for String and Throwable
 *
 * </pre>
 *
 * @author njensen
 */

@DynamicSerialize
public class ErrorResponse {

    @DynamicSerializeElement
    private String error;

    /**
     * Nullary constructor
     */
    public ErrorResponse() {
        this.error = null;
    }

    /**
     * @param error
     *            an error message
     */
    public ErrorResponse(String error) {
        this.error = error;
    }

    /**
     * @param t
     *            Throwable from which to construct the error message
     */
    public ErrorResponse(Throwable t) {
        StringJoiner sj = new StringJoiner("\n\tat ", t.getLocalizedMessage(),
                "\n");
        for (StackTraceElement e : t.getStackTrace()) {
            sj.add(e.toString());
        }
        this.error = sj.toString();
    }

    /**
     * @return the error message
     */
    public String getError() {
        return error;
    }

    /**
     * @param error
     *            the error message
     */
    public void setError(String error) {
        this.error = error;
    }

}
