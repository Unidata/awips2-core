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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TList;
import org.apache.thrift.protocol.TMap;
import org.apache.thrift.protocol.TMessage;
import org.apache.thrift.protocol.TSet;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.serialization.BaseSerializationContext;
import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationMetadata;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationCache;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.thrift.exception.FieldDeserializationException;
import com.raytheon.uf.common.serialization.thrift.exception.ListDeserializationException;
import com.raytheon.uf.common.serialization.thrift.exception.MapDeserializationException;
import com.raytheon.uf.common.serialization.thrift.exception.SetDeserializationException;

import net.sf.cglib.beans.BeanMap;
import net.sf.cglib.reflect.FastClass;

/**
 * Provides a serialization/deserialization capability built on top of the
 * Thrift Binary Serialization Format.
 * 
 * The serialization encoding is self-describing, using {code TType} bytes to
 * signify the type and also encoding class and field names.
 * 
 * The deserialization decoding uses reflection in an attempt to match up field
 * types based on class name and field name. Where possible it also has
 * tolerance for divergences between the reflectively inspected class and the
 * self-described encoding. For example, if the deserializer receives a float
 * but was expecting a double, it will transform the float into a double. If the
 * deserializer receives an unknown field on a class, it will skip that field
 * and toss out those bytes.
 * 
 * Note that the divergence tolerance does not yet account for changes when a
 * {code DynamicSerializeTypeAdapter} is used or an enum value does not exist.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Aug 12, 2008  1448     chammack    Initial creation
 * Jun 17, 2010  5091     njensen     Optimized primitive arrays
 * Mar 01, 2011           njensen     Restructured deserializeArray()
 * Sep 14, 2012  1169     djohnson    Add ability to write another object into
 *                                    the stream directly.
 * Sep 28, 2012  1195     djohnson    Add ability to specify adapter at field
 *                                    level.
 * Nov 02, 2012  1302     djohnson    No more field level adapters.
 * Apr 25, 2013  1954     bsteffen    Size Collections better.
 * Jul 23, 2013  2215     njensen     Updated for thrift 0.9.0
 * Nov 26, 2013  2537     bsteffen    Add support for void type lists which are
 *                                    sometimes created by python.
 * Jun 24, 2014  3271     njensen     Better safety checks and error msgs
 * Jul 25, 2014  3445     bclement    added castNumber()
 * Jun 15, 2015  4561     njensen     Major cleanup, added read and ignore methods
 * Jun 17, 2015  4564     njensen     Added date/time conversion in deserializeField()
 * Jul 16, 2015  4561     njensen     Improved read and ignore of collection types
 * Oct 19, 2017  6316     njensen     Improved serialization error message
 * 
 * </pre>
 * 
 * @author chammack
 */
// Warnings are suppressed in this class because generics cause issues with the
// extensive use of reflection. The erased objects are used instead.
@SuppressWarnings({ "unchecked", "rawtypes" })
public class ThriftSerializationContext extends BaseSerializationContext {

    /** The tag that is used to indicate the value of an enumeration */
    protected static final String ENUM_VALUE_TAG = "__enumValue__";

    protected static final Logger log = LoggerFactory
            .getLogger(ThriftSerializationContext.class);

    /**
     * An integer that indicates no entries have been read from a map, list, or
     * set type. Useful when trying to determine which index in a collection
     * type failed to deserialize correctly.
     */
    protected static final int NO_ENTRIES_READ_YET = -1;

    protected final SelfDescribingBinaryProtocol protocol;

    protected static Map<Class<?>, Byte> types;

    protected static Map<String, Class<?>> fieldClass = new ConcurrentHashMap<>();

    /** Mapping of built in java types to thift types */
    static {
        types = new HashMap<>();
        types.put(String.class, TType.STRING);
        types.put(Integer.class, TType.I32);
        types.put(Integer.TYPE, TType.I32);
        types.put(Long.class, TType.I64);
        types.put(Long.TYPE, TType.I64);
        types.put(Short.class, TType.I16);
        types.put(Short.TYPE, TType.I16);
        types.put(Byte.class, TType.BYTE);
        types.put(Byte.TYPE, TType.BYTE);
        types.put(Float.class, SelfDescribingBinaryProtocol.FLOAT);
        types.put(Float.TYPE, SelfDescribingBinaryProtocol.FLOAT);
        types.put(Double.class, TType.DOUBLE);
        types.put(Double.TYPE, TType.DOUBLE);
        types.put(Boolean.class, TType.BOOL);
        types.put(Boolean.TYPE, TType.BOOL);
    }

    /**
     * Constructor
     * 
     * @param protocol
     * @param serializationManager
     */
    public ThriftSerializationContext(SelfDescribingBinaryProtocol protocol,
            DynamicSerializationManager serializationManager) {
        super(serializationManager);
        this.protocol = protocol;
    }

    @Override
    public byte[] readBinary() throws SerializationException {
        try {
            return this.protocol.readBinary().array();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public boolean readBool() throws SerializationException {
        try {
            return this.protocol.readBool();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public byte readByte() throws SerializationException {
        try {
            return this.protocol.readByte();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public double readDouble() throws SerializationException {
        try {
            return this.protocol.readDouble();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public float readFloat() throws SerializationException {
        try {
            return this.protocol.readFloat();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public short readI16() throws SerializationException {
        try {
            return this.protocol.readI16();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public int readI32() throws SerializationException {
        try {
            return this.protocol.readI32();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public long readI64() throws SerializationException {
        try {
            return this.protocol.readI64();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public String readString() throws SerializationException {
        try {
            return this.protocol.readString();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeBinary(byte[] arg0) throws SerializationException {
        try {
            this.protocol.writeBinary(ByteBuffer.wrap(arg0));
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeBool(boolean arg0) throws SerializationException {
        try {
            this.protocol.writeBool(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeByte(byte arg0) throws SerializationException {
        try {
            this.protocol.writeByte(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeDouble(double arg0) throws SerializationException {
        try {
            this.protocol.writeDouble(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeFloat(float arg0) throws SerializationException {
        try {
            this.protocol.writeFloat(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeI16(short arg0) throws SerializationException {
        try {
            this.protocol.writeI16(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeI32(int arg0) throws SerializationException {
        try {
            this.protocol.writeI32(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeI64(long arg0) throws SerializationException {
        try {
            this.protocol.writeI64(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeString(String arg0) throws SerializationException {
        try {
            this.protocol.writeString(arg0);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Serializes a specific type
     * 
     * @param val
     *            the value to serialize
     * @param type
     *            the type
     * @throws SerializationException
     */
    protected void serializeType(Object val, byte type)
            throws SerializationException {
        try {
            switch (type) {
            case TType.BYTE:
                protocol.writeByte((Byte) val);
                return;
            case TType.STRING:
                protocol.writeString((String) val);
                return;
            case TType.I32:
                protocol.writeI32((Integer) val);
                return;
            case TType.I16:
                protocol.writeI16((Short) val);
                return;
            case TType.I64:
                protocol.writeI64((Long) val);
                return;
            case TType.BOOL:
                protocol.writeBool((Boolean) val);
                return;
            case SelfDescribingBinaryProtocol.FLOAT:
                protocol.writeFloat(((Number) val).floatValue());
                return;
            case TType.DOUBLE:
                protocol.writeDouble(((Number) val).doubleValue());
                return;
            case TType.STRUCT:
                this.serializationManager.serialize(this, val);
                return;
            case TType.SET:
                Set<?> set = ((Set<?>) val);
                Iterator<?> iterator = set.iterator();
                TSet setObj = new TSet(TType.VOID, set.size());
                protocol.writeSetBegin(setObj);
                while (iterator.hasNext()) {
                    Object v = iterator.next();
                    this.serializationManager.serialize(this, v);
                }
                protocol.writeSetEnd();
                return;
            case TType.MAP:
                Map<?, ?> map = ((Map<?, ?>) val);
                TMap tmap = new TMap(TType.VOID, TType.VOID, map.size());
                protocol.writeMapBegin(tmap);
                for (Entry<?, ?> entry : map.entrySet()) {
                    this.serializationManager.serialize(this, entry.getKey());
                    this.serializationManager.serialize(this, entry.getValue());
                }
                protocol.writeMapEnd();
                return;
            case TType.LIST:
                serializeArray(val);
                return;
            case TType.VOID:
                return;
            default:
                throw new SerializationException("Unknown type! " + val);
            }
        } catch (TException e) {
            throw new SerializationException(
                    "Error occurred during serialization of base type, object was "
                            + val,
                    e);
        }

    }

    /**
     * Serialize an array or List
     * 
     * @param val
     *            the object
     * @throws TException
     *             if writing error occurs
     * @throws SerializationException
     */
    protected void serializeArray(Object val) throws TException,
            SerializationException {
        Iterator<?> iterator;
        Class<?> valClass = val.getClass();
        if (valClass.isArray()) {
            Class<?> c = valClass.getComponentType();
            Byte b = lookupType(c);
            int arrayLength = Array.getLength(val);

            if (b == null) {
                /*
                 * Assume they are objects... throw exceptions if one of the
                 * components isn't serializable when it comes time to serialize
                 * it
                 */
                b = TType.STRUCT;
            }

            TList list = new TList(b, arrayLength);
            protocol.writeListBegin(list);

            /*
             * For speed, write the primitive types and Strings in the most
             * optimized way
             */
            if (c.equals(Float.TYPE)) {
                float[] d = (float[]) val;
                protocol.writeF32List(d);
            } else if (c.equals(Double.TYPE)) {
                double[] d = (double[]) val;
                protocol.writeD64List(d);
            } else if (c.equals(Byte.TYPE)) {
                byte[] d = (byte[]) val;
                protocol.writeI8List(d);
            } else if (c.equals(Integer.TYPE)) {
                int[] d = (int[]) val;
                protocol.writeI32List(d);
            } else if (c.equals(Short.TYPE)) {
                short[] d = (short[]) val;
                protocol.writeI16List(d);
            } else if (c.equals(Long.TYPE)) {
                long[] d = (long[]) val;
                protocol.writeI64List(d);
            } else if (c.equals(String.class)) {
                String[] d = (String[]) val;
                for (int z = 0; z < arrayLength; z++) {
                    protocol.writeString(d[z]);
                }
            } else {
                // Do it using reflection for objects
                for (int k = 0; k < arrayLength; k++) {
                    serializeType(Array.get(val, k), b);
                }
            }

            protocol.writeListEnd();
        } else {
            // it's a List, not an array
            iterator = ((List<?>) val).iterator();
            TList list = new TList(TType.STRUCT, ((List<?>) val).size());
            protocol.writeListBegin(list);
            while (iterator.hasNext()) {
                Object v = iterator.next();
                this.serializationManager.serialize(this, v);
            }
            protocol.writeListEnd();
        }
    }

    /**
     * Looks up the corresponding TType byte for the class
     * 
     * @param clazz
     * @return
     */
    protected Byte lookupType(Class<?> clazz) {
        Byte b = types.get(clazz);
        if (b == null) {
            if (clazz.isArray()) {
                b = TType.LIST;
            } else {
                SerializationMetadata md = DynamicSerializationManager
                        .getSerializationMetadata(clazz.getName());
                if (md != null || clazz.isEnum()) {
                    b = TType.STRUCT;
                } else {
                    Class<?> superClazz = clazz.getSuperclass();
                    while (superClazz != null && md == null) {
                        md = DynamicSerializationManager
                                .getSerializationMetadata(superClazz.getName());
                        if (md == null) {
                            superClazz = superClazz.getSuperclass();
                        }
                    }
                    if (md != null) {
                        b = TType.STRUCT;
                    } else if (Set.class.isAssignableFrom(clazz)) {
                        b = TType.SET;
                    } else if (List.class.isAssignableFrom(clazz)) {
                        b = TType.LIST;
                    } else if (Map.class.isAssignableFrom(clazz)) {
                        b = TType.MAP;
                    }
                }
            }
        }

        return b;
    }

    /**
     * Serialize a message
     * 
     * @param obj
     *            the object
     * @param beanMap
     *            the beanmap of the object
     * @param metadata
     *            the object's metadata
     * @throws SerializationException
     */
    public void serializeMessage(Object obj, BeanMap beanMap,
            SerializationMetadata metadata) throws SerializationException {
        try {
            // Determine the type of the message
            Byte b = null;
            if (obj == null) {
                b = TType.VOID;
            } else {
                b = lookupType(obj.getClass());
            }
            if (b == null
                    || (b == TType.STRUCT && metadata == null && !obj
                            .getClass().isEnum())) {
                throw new SerializationException(
                        "Don't know how to serialize class: " + obj.getClass());
            }

            /*
             * If it is a struct, determine if we know enough about the class
             * (it is properly annotated or it has a factory) for it to be
             * serialized
             */
            if (b == TType.STRUCT) {
                if (metadata != null && metadata.serializationFactory != null) {
                    /*
                     * we need to encode the struct name to something
                     * deserialization can recognize, for instance
                     * java.nio.FloatBuffer instead of
                     * java.nio.DirectFloatBufferS
                     */
                    String structName = metadata.adapterStructName.replace('.',
                            '_');
                    TStruct struct = new TStruct(structName);
                    protocol.writeStructBegin(struct);

                    Object o = obj;
                    ISerializationTypeAdapter fact = metadata.serializationFactory;
                    fact.serialize(this, o);
                    return;
                }

                // Must remove "." to be cross platform
                String structName = obj.getClass().getName().replace('.', '_');

                TStruct struct = new TStruct(structName);
                protocol.writeStructBegin(struct);

                // Determine if the class is really an enum, if so, serialize it
                // in a simple way
                if (obj.getClass().isEnum()) {
                    TField enumValueField = new TField(ENUM_VALUE_TAG,
                            TType.STRING, (short) 0);
                    protocol.writeFieldBegin(enumValueField);
                    protocol.writeString(((Enum) obj).name());
                    protocol.writeFieldEnd();
                } else {
                    // Otherwise it is a class

                    // Look at all the fields available
                    // Serialize all of the remaining fields
                    short id = 1;
                    for (String keyStr : metadata.attributeNames) {

                        Object val = beanMap.get(keyStr);

                        Byte type = null;
                        ISerializationTypeAdapter attributeFactory = null;
                        // Determine if we know how to serialize this field
                        if (val != null) {
                            Class<?> valClass = val.getClass();
                            type = lookupType(valClass);
                            attributeFactory = metadata.attributesWithFactories
                                    .get(keyStr);
                            if (type == null && attributeFactory == null) {

                                throw new SerializationException(
                                        "Unable to find serialization for "
                                                + valClass.getName());
                            }

                            /*
                             * If it's not a first class type or has a
                             * serialization factory, assume struct for now, if
                             * there are no tags we'll find out soon
                             */
                            if (type == null) {
                                type = TType.STRUCT;
                            }
                        } else {
                            // Data is null
                            type = TType.VOID;
                        }

                        // Perform actual serialization
                        serializeField(val, type, keyStr, attributeFactory, id);
                        id++;

                    }
                    protocol.writeFieldStop();
                }
                protocol.writeStructEnd();
            } else {
                /*
                 * Wrap basic types with a struct with the typeId as the name.
                 * This guarantees you know what you're getting even if they
                 * declare their list as List<Object>
                 */
                TStruct tstruct = new TStruct("" + b);
                protocol.writeStructBegin(tstruct);
                serializeType(obj, b);
                protocol.writeStructEnd();
            }
        } catch (TException e) {
            throw new SerializationException("Serialization failed", e);
        }
    }

    /**
     * Serialize a field
     * 
     * @param val
     * @param type
     * @param keyStr
     * @param adapter
     * @throws TException
     * @throws SerializationException
     */
    protected void serializeField(Object val, byte type, String keyStr,
            ISerializationTypeAdapter adapter, short id) throws TException,
            SerializationException {
        TField field = new TField(keyStr, type, id);
        protocol.writeFieldBegin(field);

        if (type != TType.VOID) {
            // Otherwise, as long as it's not void, use basic type serialization
            serializeType(val, type);
        }
        protocol.writeFieldEnd();
    }

    /**
     * Deserialize a message (with headers)
     * 
     * @return the deserialized object
     * @throws SerializationException
     */
    public Object deserializeMessage() throws SerializationException {
        Object retObj = null;
        TStruct struct = protocol.readStructBegin();
        String structName = struct.name.replace('_', '.');

        char c0 = structName.charAt(0);
        if (Character.isDigit(c0)) {
            // since fields/methods in java can't start with numeric, this means
            // that this struct contains a numerical value indicating the type
            byte b = Byte.parseByte(structName);
            Object obj = deserializeType(b, null, "");
            return obj;
        }
        SerializationMetadata md = DynamicSerializationManager
                .getSerializationMetadata(structName);

        FastClass fc;
        try {
            fc = SerializationCache.getFastClass(structName);
        } catch (ClassNotFoundException e) {
            throw new SerializationException("Unable to load class: "
                    + structName, e);
        }

        if (md == null) {
            // check to see if superclass has an adapter
            Class<?> superClazz = fc.getJavaClass().getSuperclass();
            while (superClazz != null && md == null) {
                md = DynamicSerializationManager
                        .getSerializationMetadata(superClazz.getName());
                superClazz = superClazz.getSuperclass();
            }
        }

        if (md != null) {
            if (md.serializationFactory != null) {
                ISerializationTypeAdapter factory = md.serializationFactory;
                return factory.deserialize(this);
            }
        } else if (!fc.getJavaClass().isEnum()) {
            throw new SerializationException("metadata is null for "
                    + structName);
        }

        Object o = null;
        BeanMap bm = null;

        try {
            if (fc.getJavaClass().isEnum()) {
                // an enum
                try {
                    TField enumField = protocol.readFieldBegin();
                    if (!enumField.name.equals(ENUM_VALUE_TAG)) {
                        throw new SerializationException(
                                "Expected to find enum payload.  Found: "
                                        + enumField.name);
                    }
                    Object retVal = Enum.valueOf(fc.getJavaClass(),
                            protocol.readString());
                    protocol.readFieldEnd();
                    return retVal;
                } catch (Exception e) {
                    throw new SerializationException(
                            "Error constructing enum enum", e);
                }
            } else {
                // a "regular" class
                try {
                    o = fc.newInstance();
                    bm = SerializationCache.getBeanMap(o);
                } catch (Exception e) {
                    throw new SerializationException(
                            "Error instantiating class: " + struct.name, e);
                }

                boolean moreFields = true;
                while (moreFields) {
                    try {
                        moreFields = deserializeField(fc, bm);
                    } catch (FieldDeserializationException e) {
                        TField failure = e.getField();
                        log.debug("Skipping deserialization of "
                                + o.getClass().getSimpleName() + "."
                                + failure.name, e);

                        /*
                         * At this point readFieldBegin() was called so we're
                         * past those bytes in the stream. If the field was a
                         * tstruct, tmap, tlist, or tset then those bytes at the
                         * front have also been read.
                         */
                        switch (failure.type) {
                        case TType.STRUCT:
                            readAndIgnoreStructFields();
                            break;
                        case TType.LIST:
                            ListDeserializationException lde = (ListDeserializationException) e
                                    .getCause();
                            readAndIgnoreList(lde.getTlist(), lde.getIndex());
                            break;
                        case TType.MAP:
                            MapDeserializationException mde = (MapDeserializationException) e
                                    .getCause();
                            readAndIgnoreMap(mde.getTmap(), mde.getIndex());
                            break;
                        case TType.SET:
                            SetDeserializationException sde = (SetDeserializationException) e
                                    .getCause();
                            readAndIgnoreSet(sde.getTset(), sde.getIndex());
                            break;
                        default:
                            readAndIgnoreField(failure);
                            break;
                        }

                    }
                }
            }

            protocol.readStructEnd();
        } catch (TException e) {
            throw new SerializationException("Error deserializing class "
                    + structName, e);
        } finally {
            if (bm != null && o != null) {
                retObj = bm.getBean();
                SerializationCache.returnBeanMap(bm, o);
            }
        }

        return retObj;
    }

    /*
     * All of the read and ignore methods below emulate the same protocol read
     * steps as standard reading/deserializing but diverge in that they don't
     * care about what they read. This way the position in the stream can be
     * advanced even if there's errors with a particular field.
     */

    /**
     * Reads a field off the stream, presuming that the field begin bytes have
     * already been read. Then just drops the field's data. Should be used to
     * fast-forward the stream past irrelevant, misunderstood, or unknown
     * fields.
     * 
     * @param fieldToSkip
     *            the field to ignore
     * @throws SerializationException
     * @throws TException
     */
    protected void readAndIgnoreField(TField fieldToSkip)
            throws SerializationException, TException {
        switch (fieldToSkip.type) {
        case TType.BOOL:
        case TType.BYTE:
        case TType.I16:
        case TType.I32:
        case TType.I64:
        case TType.STRING:
        case SelfDescribingBinaryProtocol.FLOAT:
        case TType.DOUBLE:
        case TType.VOID:
            /*
             * we can safely use deserializeType here because we know that all
             * of these types are in every JVM
             */
            deserializeType(fieldToSkip.type, null, fieldToSkip.name);
            break;
        /*
         * handle the types below differently to ensure the entire
         * structure/object is read off the stream regardless of if we know
         * about and understand its types
         */
        case TType.STRUCT:
            TStruct tstruct = protocol.readStructBegin();
            readAndIgnoreStruct(tstruct);
            break;
        case TType.MAP:
            TMap tmap = protocol.readMapBegin();
            readAndIgnoreMap(tmap, NO_ENTRIES_READ_YET);
            break;
        case TType.LIST:
            TList tlist = protocol.readListBegin();
            readAndIgnoreList(tlist, NO_ENTRIES_READ_YET);
            break;
        case TType.SET:
            TSet tset = protocol.readSetBegin();
            readAndIgnoreSet(tset, NO_ENTRIES_READ_YET);
            break;
        default:
            // do nothing
            break;
        }
        protocol.readFieldEnd();
    }

    /**
     * Reads a struct off the stream, then just drops the struct's data.
     * 
     * @param struct
     *            the struct to ignore
     * 
     * @throws SerializationException
     * @throws TException
     */
    protected void readAndIgnoreStruct(TStruct struct)
            throws SerializationException, TException {
        char c0 = struct.name.charAt(0);

        if (Character.isDigit(c0)) {
            byte b = Byte.parseByte(struct.name);
            switch (b) {
            case TType.LIST:
                TList tlist = protocol.readListBegin();
                readAndIgnoreList(tlist, NO_ENTRIES_READ_YET);
                break;
            case TType.MAP:
                TMap tmap = protocol.readMapBegin();
                readAndIgnoreMap(tmap, NO_ENTRIES_READ_YET);
                break;
            case TType.SET:
                TSet tset = protocol.readSetBegin();
                readAndIgnoreSet(tset, NO_ENTRIES_READ_YET);
                break;
            default:
                deserializeType(b, null, "");
                break;
            }
        } else {
            readAndIgnoreStructFields();
        }
    }

    /**
     * Reads a struct off the stream, presuming that the struct begin bytes have
     * already been read. Then just drops the struct's data.
     * 
     * @throws TException
     * @throws SerializationException
     */
    protected void readAndIgnoreStructFields() throws TException,
            SerializationException {
        boolean moreFields = true;
        while (moreFields) {
            // read a field off the structure
            TField field = protocol.readFieldBegin();
            moreFields = (field.type != TType.STOP);
            if (moreFields) {
                readAndIgnoreField(field);
                protocol.readFieldEnd();
            }
        }
    }

    /**
     * Reads a map off the stream, then just drops the map's data.
     * 
     * @param tmap
     *            the tmap to ignore
     * @param entriesRead
     *            the number of entries in the map already read off the stream
     * 
     * @throws TException
     * @throws SerializationException
     */
    protected void readAndIgnoreMap(TMap tmap, int entriesRead)
            throws TException, SerializationException {
        for (int i = entriesRead + 1; i < tmap.size; i++) {
            // key followed by value
            TStruct key = protocol.readStructBegin();
            readAndIgnoreStruct(key);
            TStruct value = protocol.readStructBegin();
            readAndIgnoreStruct(value);
        }
        protocol.readMapEnd();
    }

    /**
     * Reads a set off the stream, then just drops the set's data.
     * 
     * @param tset
     *            the tset to ignore
     * @param entriesRead
     *            the number of entries in the set already read off the stream
     * 
     * @throws TException
     * @throws SerializationException
     */
    protected void readAndIgnoreSet(TSet tset, int entriesRead)
            throws TException, SerializationException {
        for (int i = entriesRead + 1; i < tset.size; i++) {
            TStruct tstruct = protocol.readStructBegin();
            readAndIgnoreStruct(tstruct);
        }
        protocol.readSetEnd();
    }

    /**
     * Reads a list or array off the stream, then just drops the list/array's
     * data.
     * 
     * @param tlist
     *            the tlist to ignore
     * @param entriesRead
     *            the number of entries in the list already read off the stream
     * 
     * @throws TException
     * @throws SerializationException
     */
    protected void readAndIgnoreList(TList tlist, int entriesRead)
            throws TException, SerializationException {
        switch (tlist.elemType) {
        case TType.BYTE:
            protocol.readI8List(tlist.size);
            break;
        case TType.BOOL:
            for (int i = entriesRead + 1; i < tlist.size; i++) {
                readBool();
            }
            break;
        case TType.I16:
            protocol.readI16List(tlist.size);
            break;
        case TType.I32:
            protocol.readI32List(tlist.size);
            break;
        case TType.I64:
            protocol.readI64List(tlist.size);
            break;
        case SelfDescribingBinaryProtocol.FLOAT:
            protocol.readF32List(tlist.size);
            break;
        case TType.DOUBLE:
            protocol.readD64List(tlist.size);
            break;
        case TType.STRING:
            for (int i = entriesRead + 1; i < tlist.size; i++) {
                readString();
            }
            break;
        case TType.VOID:
            break;
        case TType.STRUCT:
            for (int i = entriesRead + 1; i < tlist.size; i++) {
                TStruct tstruct = protocol.readStructBegin();
                readAndIgnoreStruct(tstruct);
            }
        }
        protocol.readListEnd();
    }

    /*
     * End of read and ignore methods
     */

    /**
     * Deserialize a field
     * 
     * @param fc
     * @param bm
     * @throws TException
     * @throws SerializationException
     */
    protected boolean deserializeField(FastClass fc, BeanMap bm)
            throws TException, SerializationException {
        TField field = protocol.readFieldBegin();
        Object obj = null;

        /*
         * TType.STOP indicates we've reached the end of serialized fields on
         * the parent object
         */
        if (field.type == TType.STOP) {
            return false;
        }

        if (field.type != TType.VOID) {
            try {
                obj = deserializeType(field.type, fc, field.name);
            } catch (SerializationException e) {
                throw new FieldDeserializationException(field, e);
            }
            if (field.type == TType.STRING) {
                Class<?> fieldClass = findFieldClass(fc.getJavaClass(),
                        field.name);
                if (fieldClass != null && fieldClass.isEnum()) {
                    /*
                     * special case to handle Strings sent from python and
                     * transform them into enums, since python had no knowledge
                     * of whether a string should translate to a string or enum
                     * in java
                     */
                    obj = Enum.valueOf((Class<Enum>) fieldClass, (String) obj);
                }
            }
            try {
                /*
                 * cglib doesn't seem to mind if you put in extra fields that
                 * don't exist in your version of the object
                 */
                bm.put(field.name, obj);
            } catch (ClassCastException e) {
                /*
                 * should we continue to add special handling in here, we should
                 * break this out to a separate method
                 */

                /* attempt to recover if both types are numbers or times */
                Class<?> fieldClass = findFieldClass(fc.getJavaClass(),
                        field.name);
                if (obj instanceof Number) {
                    /*
                     * we can't easily determine if fieldClass is a number yet
                     * due to primitive number classes, castNumber() will check
                     */
                    obj = castNumber((Number) obj, fieldClass);
                    bm.put(field.name, obj);
                } else if (obj instanceof Date
                        && Calendar.class.isAssignableFrom(fieldClass)) {
                    Calendar c = Calendar.getInstance(TimeZone
                            .getTimeZone("GMT"));
                    c.setTime((Date) obj);
                    obj = c;
                    bm.put(field.name, obj);
                } else if (obj instanceof Calendar
                        && Date.class.isAssignableFrom(fieldClass)) {
                    obj = ((Calendar) obj).getTime();
                    bm.put(field.name, obj);
                } else {
                    throw e;
                }
            }
        }
        protocol.readFieldEnd();

        return true;
    }

    /**
     * Convert source Number to Number compatible with target number class
     * 
     * @param source
     * @param targetClass
     * @return
     * @throws ClassCastException
     *             if targetClass is not supported
     */
    protected static Number castNumber(Number source, Class<?> targetClass)
            throws ClassCastException {
        Number rval;
        if (targetClass.equals(Byte.class) || targetClass.equals(byte.class)) {
            rval = source.byteValue();
        } else if (targetClass.equals(Short.class)
                || targetClass.equals(short.class)) {
            rval = source.shortValue();
        } else if (targetClass.equals(Integer.class)
                || targetClass.equals(int.class)) {
            rval = source.intValue();
        } else if (targetClass.equals(Long.class)
                || targetClass.equals(long.class)) {
            rval = source.longValue();
        } else if (targetClass.equals(Float.class)
                || targetClass.equals(float.class)) {
            rval = source.floatValue();
        } else if (targetClass.equals(Double.class)
                || targetClass.equals(double.class)) {
            rval = source.doubleValue();
        } else if (targetClass.equals(BigInteger.class)) {
            rval = BigInteger.valueOf(source.longValue());
        } else if (targetClass.equals(BigDecimal.class)) {
            rval = BigDecimal.valueOf(source.doubleValue());
        } else {
            throw new ClassCastException("Unable to cast " + source.getClass()
                    + " to " + targetClass);
        }
        return rval;
    }

    /**
     * Deserialize a type
     * 
     * @param type
     * @param fclazz
     * @param fieldName
     * @param enclosureType
     * @return
     * @throws SerializationException
     */
    protected Object deserializeType(byte type, FastClass fclazz,
            String fieldName) throws SerializationException {
        switch (type) {
        case TType.STRING: {
            try {
                return protocol.readString();
            } catch (TException e) {
                throw new SerializationException(
                        "Error reading string of field " + fieldName, e);
            }
        }
        case TType.I16: {
            try {
                return protocol.readI16();
            } catch (TException e) {
                throw new SerializationException(
                        "Error reading short of field " + fieldName, e);
            }
        }
        case TType.I32: {
            try {
                return protocol.readI32();
            } catch (TException e) {
                throw new SerializationException("Error reading int of field "
                        + fieldName, e);
            }
        }
        case TType.LIST: {
            return deserializeArray(fclazz, fieldName);
        }
        case TType.MAP: {
            Map map = null;
            TMap tmap = null;
            int i = NO_ENTRIES_READ_YET;
            try {
                tmap = protocol.readMapBegin();

                /*
                 * Attempt to get the exact implementation of Map as specified
                 * in the class if available
                 */
                if (fclazz != null) {
                    Class<?> fieldClazz = findFieldClass(fclazz.getJavaClass(),
                            fieldName);
                    if (fieldClazz == null) {
                        throw new MapDeserializationException(tmap, i,
                                new NoSuchFieldException(fieldName));
                    }

                    if (!fieldClazz.isInterface()
                            && Map.class.isAssignableFrom(fieldClazz)) {
                        map = (Map) fieldClazz.newInstance();
                    }
                }

                if (map == null) {
                    // assume hashmap if nothing else available
                    map = new HashMap((int) (tmap.size / 0.75) + 1, 0.75f);
                }

                /*
                 * Since Java 1.5+ did not expose generics information at
                 * runtime,we must assume an erased type, and do reflection on
                 * every component of the key and value pair.
                 */
                for (i = 0; i < tmap.size; i++) {
                    Object key = this.serializationManager.deserialize(this);
                    Object val = this.serializationManager.deserialize(this);
                    map.put(key, val);
                }
            } catch (Exception e) {
                throw new MapDeserializationException(tmap, i,
                        "Error deserializing map of field " + fieldName, e);
            }
            protocol.readMapEnd();
            return map;
        }
        case TType.SET: {
            Set set = null;
            TSet tset = null;
            int i = NO_ENTRIES_READ_YET;
            try {
                tset = protocol.readSetBegin();

                /*
                 * Attempt to get the exact implementation of Set as specified
                 * in the class if available
                 */
                if (fclazz != null) {
                    Class<?> fieldClazz = findFieldClass(fclazz.getJavaClass(),
                            fieldName);
                    if (fieldClazz == null) {
                        throw new SetDeserializationException(tset, i,
                                new NoSuchFieldException(fieldName));
                    }
                    if (!fieldClazz.isInterface()
                            && Set.class.isAssignableFrom(fieldClazz)) {
                        set = (Set) fieldClazz.newInstance();
                    }
                }

                if (set == null) {
                    // assume hashset if nothing else available
                    set = new HashSet((int) (tset.size / 0.75) + 1, 0.75f);
                }

                /*
                 * Since Java 1.5+ did not expose generics information at
                 * runtime,we must assume an erased type, and do reflection on
                 * every component of the set.
                 */
                for (i = 0; i < tset.size; i++) {
                    set.add(this.serializationManager.deserialize(this));
                }

                protocol.readSetEnd();
                return set;
            } catch (Exception e) {
                throw new SetDeserializationException(tset, i,
                        "Error deserializing set of field " + fieldName, e);
            }
        }
        case SelfDescribingBinaryProtocol.FLOAT: {
            try {
                return protocol.readFloat();
            } catch (TException e) {
                throw new SerializationException(
                        "Error reading float of field " + fieldName, e);
            }
        }
        case TType.BYTE: {
            try {
                return protocol.readByte();
            } catch (TException e) {
                throw new SerializationException("Error reading byte of field "
                        + fieldName, e);
            }
        }
        case TType.I64: {
            try {
                return protocol.readI64();
            } catch (TException e) {
                throw new SerializationException("Error reading long of field "
                        + fieldName, e);
            }
        }
        case TType.DOUBLE: {
            try {
                return protocol.readDouble();
            } catch (TException e) {
                throw new SerializationException(
                        "Error reading double of field " + fieldName, e);
            }
        }
        case TType.BOOL: {
            try {
                return protocol.readBool();
            } catch (TException e) {
                throw new SerializationException(
                        "Error reading boolean of field " + fieldName, e);
            }
        }
        case TType.STRUCT: {
            return this.serializationManager.deserialize(this);
        }
        case TType.VOID: {
            return null;
        }
        default:
            throw new SerializationException("Unhandled type: " + type);
        }

    }

    protected Class<?> findFieldClass(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "." + fieldName;
        Class<?> rval = fieldClass.get(key);

        if (rval != null) {
            return rval;
        }

        rval = clazz;

        while (rval != null) {
            Field field = null;
            try {
                field = rval.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                // try super class
                rval = rval.getSuperclass();
                continue;
            }
            rval = field.getType();
            fieldClass.put(key, rval);
            return rval;
        }

        return null;
    }

    /**
     * Deserialize an array
     * 
     * @param fclazz
     * @param fieldName
     * @return
     * @throws SerializationException
     */
    protected Object deserializeArray(FastClass fclazz, String fieldName)
            throws SerializationException {
        TList innerList = null;
        int i = NO_ENTRIES_READ_YET;
        try {
            innerList = protocol.readListBegin();

            // Determine whether the list is really an array or if it is a list.
            Class<?> fieldClazz = null;
            if (fclazz != null) {
                fieldClazz = findFieldClass(fclazz.getJavaClass(), fieldName);
                if (fieldClazz == null) {
                    throw new ListDeserializationException(innerList, i,
                            new NoSuchFieldException(fieldName));
                }
            }

            /*
             * The type is an array or List. If the inner type matches a
             * primitive, it's guaranteed be homogeneous in type, we can just
             * read the data quickly as one giant block. If the inner type is a
             * String, we can quickly read block one right after another without
             * a header.
             */
            switch (innerList.elemType) {
            case SelfDescribingBinaryProtocol.FLOAT:
                float[] fa = protocol.readF32List(innerList.size);
                protocol.readListEnd();
                return fa;
            case TType.DOUBLE:
                double[] doubleArray = protocol.readD64List(innerList.size);
                protocol.readListEnd();
                return doubleArray;
            case TType.I32:
                int[] intArray = protocol.readI32List(innerList.size);
                protocol.readListEnd();
                return intArray;
            case TType.BYTE:
                byte[] byteArray = protocol.readI8List(innerList.size);
                protocol.readListEnd();
                return byteArray;
            case TType.BOOL:
                boolean[] boolArray = new boolean[innerList.size];
                for (i = 0; i < boolArray.length; i++) {
                    boolArray[i] = readBool();
                }
                protocol.readListEnd();
                return boolArray;
            case TType.I64:
                long[] longArray = protocol.readI64List(innerList.size);
                protocol.readListEnd();
                return longArray;
            case TType.I16:
                short[] shortArray = protocol.readI16List(innerList.size);
                protocol.readListEnd();
                return shortArray;
            case TType.STRING:
                if (fieldClazz == null || fieldClazz.isArray()) {
                    String[] stringArray = new String[innerList.size];
                    for (i = 0; i < stringArray.length; i++) {
                        stringArray[i] = readString();
                    }
                    protocol.readListEnd();
                    return stringArray;
                } else {
                    // this is a List but due to the encoded element type
                    // we can safely assume it's all Strings
                    List<String> list = null;
                    if (fieldClazz != null) {
                        if (!fieldClazz.isInterface()
                                && List.class.isAssignableFrom(fieldClazz)) {
                            list = (List<String>) fieldClazz.newInstance();
                        }
                    }
                    if (list == null) {
                        list = new ArrayList<>(innerList.size);
                    }
                    for (i = 0; i < innerList.size; i++) {
                        list.add(readString());
                    }
                    return list;
                }
            case TType.VOID:
                if (fieldClazz == null || fieldClazz.isArray()) {
                    Object[] array = new Object[innerList.size];
                    protocol.readListEnd();
                    return array;
                } else {
                    // this is a List but due to the encoded element type
                    // we can safely assume it's all nulls
                    List<String> list = null;
                    if (fieldClazz != null) {
                        if (!fieldClazz.isInterface()
                                && List.class.isAssignableFrom(fieldClazz)) {
                            list = (List<String>) fieldClazz.newInstance();
                        }
                    }
                    if (list == null) {
                        list = new ArrayList<>(innerList.size);
                    }
                    for (i = 0; i < innerList.size; i++) {
                        list.add(null);
                    }
                    return list;
                }
            default:
                if (fieldClazz != null && fieldClazz.isArray()) {
                    // Slower catch-all implementation
                    Class<?> arrayComponent = fieldClazz.getComponentType();
                    Byte serializedType = innerList.elemType;
                    Object array = Array.newInstance(arrayComponent,
                            innerList.size);

                    for (i = 0; i < innerList.size; i++) {
                        Array.set(array, i,
                                deserializeType(serializedType, null, ""));
                    }
                    protocol.readListEnd();
                    return array;
                } else {
                    /*
                     * This type is an actual List. Since Java 1.5+ did not
                     * expose generics information at runtime, we must assume an
                     * erased type, and do reflection on every component of the
                     * list.
                     */
                    List list = null;
                    if (fieldClazz != null) {
                        if (!fieldClazz.isInterface()
                                && List.class.isAssignableFrom(fieldClazz)) {
                            list = (List) fieldClazz.newInstance();
                        }
                    }

                    if (list == null) {
                        list = new ArrayList(innerList.size);
                    }
                    for (i = 0; i < innerList.size; i++) {
                        list.add(this.serializationManager.deserialize(this));
                    }
                    protocol.readListEnd();
                    return list;
                }
            }
        } catch (Exception e) {
            throw new ListDeserializationException(innerList, i,
                    "Error deserializing list/array of field " + fieldName, e);
        }
    }

    @Override
    public void writeMessageEnd() throws SerializationException {
        this.protocol.writeMessageEnd();
    }

    @Override
    public void writeMessageStart(String messageName)
            throws SerializationException {
        try {
            this.protocol.writeMessageBegin(new TMessage(messageName,
                    TType.VOID, 0));
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void readMessageEnd() throws SerializationException {
        this.protocol.readMessageEnd();

    }

    @Override
    public String readMessageStart() throws SerializationException {
        try {
            TMessage msg = this.protocol.readMessageBegin();
            return msg.name;
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public float[] readFloatArray() throws SerializationException {
        try {
            int sz = this.protocol.readI32();
            return this.protocol.readF32List(sz);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public double[] readDoubleArray() throws SerializationException {
        try {
            int sz = this.protocol.readI32();
            return this.protocol.readD64List(sz);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeFloatArray(float[] floats) throws SerializationException {
        try {
            this.protocol.writeI32(floats.length);
            this.protocol.writeF32List(floats);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeDoubleArray(double[] dubs) throws SerializationException {
        try {
            this.protocol.writeI32(dubs.length);
            this.protocol.writeD64List(dubs);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void writeBuffer(ByteBuffer buffer) throws SerializationException {
        try {
            this.protocol.writeBinary(buffer);
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public ByteBuffer readBuffer() throws SerializationException {
        try {
            return this.protocol.readBinary();
        } catch (TException e) {
            throw new SerializationException(e);
        }
    }
}
