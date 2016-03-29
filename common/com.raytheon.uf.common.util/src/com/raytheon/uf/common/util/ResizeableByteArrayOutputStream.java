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
package com.raytheon.uf.common.util;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Output stream that is backed by a resizeable byte array
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 30, 2015 4710       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class ResizeableByteArrayOutputStream extends ByteArrayOutputStream {

    public ResizeableByteArrayOutputStream(int size) {
        super(size);
    }

    public int getCapacity() {
        return this.buf.length;
    }

    public void setCapacity(int length) {
        this.buf = Arrays.copyOf(this.buf, length);
        if (this.buf.length < this.count) {
            this.count = this.buf.length - 1;
        }

    }

    /**
     * This returns the underlying array of the output stream. Care should be
     * taken to keep count in sync if the underlying array is changed.
     * 
     * @return The underlying array.
     */
    public byte[] getUnderlyingArray() {
        return this.buf;
    }

    /**
     * Sets the current count. This should only be used if the underlyingArray
     * was changed outside of this classes knowledge.
     * 
     * @param count
     */
    public void setCount(int count) {
        if (count < this.buf.length) {
            this.count = count;
        } else {
            this.count = this.buf.length - 1;
        }
    }

}
