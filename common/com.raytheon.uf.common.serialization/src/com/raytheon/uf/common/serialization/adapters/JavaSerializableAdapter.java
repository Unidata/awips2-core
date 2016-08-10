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
package com.raytheon.uf.common.serialization.adapters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TType;

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.thrift.exception.FieldDeserializationException;

/**
 * 
 * Adapter which allows any Object type that implements {@link Serializable} to
 * be used in a field with the {@link DynamicSerializeElement} annotation. This
 * adapter should only be used for third party Objects that do not provide any
 * other means to access the state for serialization. When possible it is better
 * to write a custom adapter that writes the Object state using primitive types
 * and structures rather than using this adapter. Fields using this adapter may
 * not be available for use in libraries for programming languages other than
 * java.
 * 
 * This adapter writes some metadata around the raw java serialized bytes so
 * that if the data is deserialized without this adapter it will generate a
 * struct with a byte[] field named javaSerializedData. This makes it easier for
 * a generic deserializer to safely skip this data if it is does not support
 * java serialization.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------
 * Jul 28, 2016  5738     bsteffen  Initial Implementation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class JavaSerializableAdapter implements
        ISerializationTypeAdapter<Serializable> {

    private static final String FAKE_FIELD_NAME = "javaSerializedData";

    @Override
    public void serialize(ISerializationContext serializer, Serializable object)
            throws SerializationException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        try (ObjectOutputStream objectStream = new ObjectOutputStream(
                byteStream)) {
            objectStream.writeObject(object);
        } catch (IOException e) {
            throw new SerializationException("Unable to serialize " + object, e);
        }
        serializer.writeByte(TType.LIST);
        serializer.writeString(FAKE_FIELD_NAME);
        serializer.writeI16((short) 1);
        serializer.writeByte(TType.BYTE);
        serializer.writeBinary(byteStream.toByteArray());
        serializer.writeByte(TType.STOP);

    }

    @Override
    public Serializable deserialize(IDeserializationContext deserializer)
            throws SerializationException {
        byte fieldType = deserializer.readByte();
        if (fieldType == TType.STOP) {
            throw new SerializationException(
                    "Received no fields for a java.io.Serializable object.");
        }
        String fieldName = deserializer.readString();
        /* fieldId is meaningless so don't need to verify. */
        short fieldId = deserializer.readI16();
        if (fieldType != TType.LIST || !FAKE_FIELD_NAME.equals(fieldName)) {
            TField field = new TField(fieldName, fieldType, fieldId);
            /*
             * Including the field metadata in the exception should allow the
             * framework to skip the field safely and attempt to recover.
             */
            throw new FieldDeserializationException(field,
                    "Unexpected initial field: " + field.toString());
        }

        byte listType = deserializer.readByte();
        if (listType != TType.BYTE) {
            throw new SerializationException("Unexpected list type: "
                    + listType);
        }
        Serializable result = null;
        try (ObjectInputStream objectStream = new ObjectInputStream(
                new ByteArrayInputStream(deserializer.readBinary()))) {
            result = (Serializable) objectStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Unable to deserialize Object", e);
        }
        fieldType = deserializer.readByte();
        if (fieldType != TType.STOP) {

            fieldName = deserializer.readString();
            fieldId = deserializer.readI16();
            TField field = new TField(fieldName, fieldType, fieldId);
            /*
             * Including the field metadata in the exception should allow the
             * framework to skip the field safely and attempt to recover.
             */
            throw new FieldDeserializationException(field,
                    "Unexpected additional field: " + field.toString());
        }
        return result;
    }

}
