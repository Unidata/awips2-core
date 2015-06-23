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

import org.apache.thrift.protocol.TField;

import com.raytheon.uf.common.serialization.SerializationException;

/**
 * An exception indicating that a field could not be deserialized from the {code
 * SelfDescribingBinaryProtocol}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2015 4561       njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class FieldDeserializationException extends SerializationException {

    private static final long serialVersionUID = 1L;

    protected final TField field;

    public FieldDeserializationException(TField field) {
        this(field, null, null);
    }

    public FieldDeserializationException(TField field, String msg) {
        this(field, msg, null);
    }

    public FieldDeserializationException(TField field, Throwable t) {
        this(field, null, t);
    }

    public FieldDeserializationException(TField field, String msg, Throwable t) {
        super(msg, t);
        this.field = field;
    }

    public TField getField() {
        return field;
    }

}
