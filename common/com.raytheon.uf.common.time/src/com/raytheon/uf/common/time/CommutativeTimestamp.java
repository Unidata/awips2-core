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
package com.raytheon.uf.common.time;

import java.sql.Timestamp;
import java.util.Date;

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.common.time.CommutativeTimestamp.CommutativeTimestampSerializer;

/**
 * Extension of {@link java.sql.Timestamp} that allows equal comparison with
 * {@link java.util.Date}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 23, 2016  5696     rjpeter   Initial creation
 * 
 * </pre>
 * 
 * @author rjpeter
 */
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = CommutativeTimestampSerializer.class)
public class CommutativeTimestamp extends Timestamp {

    private static final long serialVersionUID = 1L;

    public CommutativeTimestamp() {
        super(System.currentTimeMillis());
    }

    public CommutativeTimestamp(long date) {
        super(date);
    }

    public CommutativeTimestamp(Date date) {
        super(date.getTime());
    }

    public CommutativeTimestamp(Timestamp date) {
        // drop nanos precision, dynamic serialize only supports to millis
        super(date.getTime());
    }

    @Override
    public void setNanos(int nanos) {
        // drop nanos precision down to milliseconds
        super.setNanos((nanos / 1000000) * 1000000);
    }

    /*
     * Support equals for Date and Timestamp
     */
    @Override
    public boolean equals(Object ts) {
        if (ts instanceof Date) {
            return this.getTime() == ((Date) ts).getTime();
        }

        return false;
    }

    /**
     * Serialization support for
     * {@link com.raytheon.uf.common.time.CommutativeTimestamp}
     */
    public static class CommutativeTimestampSerializer implements
            ISerializationTypeAdapter<CommutativeTimestamp> {

        @Override
        public CommutativeTimestamp deserialize(
                IDeserializationContext deserializer)
                throws SerializationException {
            long t = deserializer.readI64();
            CommutativeTimestamp rval = new CommutativeTimestamp(t);
            return rval;
        }

        @Override
        public void serialize(ISerializationContext serializer,
                CommutativeTimestamp object) throws SerializationException {
            serializer.writeI64(object.getTime());
        }
    }
}
