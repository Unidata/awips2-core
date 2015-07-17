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
package com.raytheon.uf.common.serialization.thrift.exception;

import com.raytheon.uf.common.serialization.SerializationException;

/**
 * An exception indicating that a collection could not be deserialized from the
 * {code SelfDescribingBinaryProtocol}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 16, 2015  4561      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class CollectionDeserializationException extends SerializationException {

    private static final long serialVersionUID = 1L;

    /**
     * indicates the index of the last read entry/item in the collection that
     * caused the exception, or -1 if no entries have been read yet
     */
    protected final int index;

    public CollectionDeserializationException(int index) {
        this(index, null, null);
    }

    public CollectionDeserializationException(int index, String msg) {
        this(index, msg, null);
    }

    public CollectionDeserializationException(int index, Throwable t) {
        this(index, null, t);
    }

    public CollectionDeserializationException(int index, String msg, Throwable t) {
        super(msg, t);
        this.index = index;
    }

    /**
     * Gets the index that iteration failed on, or put another way, index + 1
     * indicates the number of items in the collection that have been
     * deserialized.
     * 
     * @return
     */
    public int getIndex() {
        return index;
    }

}
