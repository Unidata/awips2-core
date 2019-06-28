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
package com.raytheon.uf.common.serialization.thrift;

import com.raytheon.uf.common.serialization.CriticalSerializationException;

/**
 * Thrown when a number is truncated during deserialization (i.e. would overflow
 * or underflow if assigned to the target field)
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul  9, 2019 7888       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class NumberTruncatedException extends CriticalSerializationException {

    private static final long serialVersionUID = 1L;

    public NumberTruncatedException() {
        super();
    }

    public NumberTruncatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NumberTruncatedException(String message) {
        super(message);
    }

    public NumberTruncatedException(Throwable cause) {
        super(cause);
    }

}
