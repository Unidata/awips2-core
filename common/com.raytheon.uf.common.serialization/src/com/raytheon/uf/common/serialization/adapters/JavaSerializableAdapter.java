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

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

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
        serializer.writeBinary(byteStream.toByteArray());

    }

    @Override
    public Serializable deserialize(IDeserializationContext deserializer)
            throws SerializationException {
        try (ObjectInputStream objectStream = new ObjectInputStream(
                new ByteArrayInputStream(deserializer.readBinary()))) {
            return (Serializable) objectStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializationException("Unable to deserialize Object", e);
        }
    }

}
