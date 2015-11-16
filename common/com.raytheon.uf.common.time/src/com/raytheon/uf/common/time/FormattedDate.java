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

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.common.time.FormattedDate.FormattedDateSerializer;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Extension of Date to provide a toString compatible with DataTime.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 14, 2015 4486       rjpeter     Initial creation
 * Nov 16, 2015 5132       bsteffen    Backport and change to extend Timestamp.
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = FormattedDateSerializer.class)
public class FormattedDate extends Timestamp {

    private static final long serialVersionUID = 1L;

    public FormattedDate() {
        super(System.currentTimeMillis());
    }

    public FormattedDate(long date) {
        super(date);
    }

    /**
     * 
     * Serialization support for {@link java.util.Date}
     * 
     * @author chammack
     * @version 1.0
     */
    public static class FormattedDateSerializer implements
            ISerializationTypeAdapter<FormattedDate> {

        @Override
        public FormattedDate deserialize(IDeserializationContext deserializer)
                throws SerializationException {
            long t = deserializer.readI64();
            return new FormattedDate(t);
        }

        @Override
        public void serialize(ISerializationContext serializer,
                FormattedDate object) throws SerializationException {
            serializer.writeI64(object.getTime());
        }

    }
}
