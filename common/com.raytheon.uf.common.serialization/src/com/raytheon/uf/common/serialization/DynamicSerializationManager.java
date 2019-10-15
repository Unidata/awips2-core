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

package com.raytheon.uf.common.serialization;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.Buffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.raytheon.uf.common.serialization.BuiltInTypeSupport.CalendarSerializer;
import com.raytheon.uf.common.serialization.BuiltInTypeSupport.DateSerializer;
import com.raytheon.uf.common.serialization.BuiltInTypeSupport.TimestampSerializer;
import com.raytheon.uf.common.serialization.adapters.BufferAdapter;
import com.raytheon.uf.common.serialization.adapters.EnumSetAdapter;
import com.raytheon.uf.common.serialization.adapters.PointAdapter;
import com.raytheon.uf.common.serialization.adapters.StackTraceElementAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.common.serialization.thrift.ThriftSerializationContext;
import com.raytheon.uf.common.serialization.thrift.ThriftSerializationContextBuilder;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

import net.sf.cglib.beans.BeanMap;

import net.sf.cglib.beans.BeanMap;

/**
 * Dynamic Serialization Manager provides a serialization capability that runs
 * purely at runtime based on annotations.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Aug 13, 2008 #1448       chammack    Initial creation
 * Mar 27, 2012 #428        dgilling    Add support for built-in
 *                                      classes used by data delivery's
 *                                      registry service.
 * Sep 28, 2012 #1195       djohnson    Add ability to specify adapter at field level.
 * Oct 08, 2012 #1251       dgilling    Ensure type registered with
 *                                      serialization adapter is encoded
 *                                      in serialization stream.
 * Nov 02, 2012 1302        djohnson    Remove field level adapters, they break python serialization.
 * Aug 06, 2013 2228        njensen     Added deserialize(byte[])
 * Aug 08, 2014 3503        bclement    moved registration of spatial serialization adapters to common.geospatial
 * Aug 15, 2014 3541        mschenke    Made getSerializationMetadata(String) static and renamed inspect to match\
 * Aug 27, 2014 3503        bclement    improved error message in registerAdapter()
 * Jun 16, 2015 4561        njensen     Deprecated EnclosureType
 * Oct 30, 2015 4710        bclement    ByteArrayOutputStream renamed to PooledByteArrayOutputStream
 * Jun 11, 2019 7595        tgurney     Replace compile-time reference to
 *                                      XMLGregorianCalendarImpl with loading at
 *                                      runtime (the class is no longer
 *                                      accessible at compile time)
 * Jul  1, 2019 7888        tgurney     deserialize(ctx) changed method signature
 *
 * </pre>
 *
 * @author chammack
 */
public class DynamicSerializationManager {
    private static Map<SerializationType, DynamicSerializationManager> instanceMap = new HashMap<>();

    private ISerializationContextBuilder builder;

    public static class SerializationMetadata {
        public List<String> attributeNames;

        public ISerializationTypeAdapter<?> serializationFactory;

        public Map<String, ISerializationTypeAdapter<?>> attributesWithFactories;

        public String adapterStructName;

    }

    private static Map<String, SerializationMetadata> serializedAttributes = new ConcurrentHashMap<>();

    private static final SerializationMetadata NO_METADATA = new SerializationMetadata();

    static {
        registerAdapter(GregorianCalendar.class, new CalendarSerializer());
        try {
            registerAdapter(
                    (Class<? extends XMLGregorianCalendar>) Class.forName(
                            "com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl"),
                    new BuiltInTypeSupport.XMLGregorianCalendarSerializer());
        } catch (ClassNotFoundException e) {
            // Better not just catch-and-log this if it ever happens
            throw new RuntimeException(
                    "Could not load XMLGregorianCalendarImpl", e);
        }
        registerAdapter(Date.class, new DateSerializer());
        registerAdapter(Timestamp.class, new TimestampSerializer());
        registerAdapter(java.sql.Date.class,
                new BuiltInTypeSupport.SqlDateSerializer());
        registerAdapter(java.awt.Point.class, new PointAdapter());
        registerAdapter(BigDecimal.class,
                new BuiltInTypeSupport.BigDecimalSerializer());
        registerAdapter(BigInteger.class,
                new BuiltInTypeSupport.BigIntegerSerializer());
        registerAdapter(EnumSet.class, new EnumSetAdapter());
        registerAdapter(StackTraceElement.class,
                new StackTraceElementAdapter());
        registerAdapter(Duration.class,
                new BuiltInTypeSupport.DurationSerializer());
        registerAdapter(QName.class, new BuiltInTypeSupport.QNameSerializer());
        registerAdapter(Throwable.class,
                new BuiltInTypeSupport.ThrowableSerializer());
        registerAdapter(Buffer.class, new BufferAdapter());
    }

    public enum SerializationType {
        Thrift
    }

    /**
     * Serialize an object to a byte array
     *
     * @param obj
     *            the object
     * @return a byte array with a serialized version of the object
     * @throws SerializationException
     */
    public byte[] serialize(Object obj) throws SerializationException {

        PooledByteArrayOutputStream baos = ByteArrayOutputStreamPool
                .getInstance().getStream();

        try {
            ISerializationContext ctx = this.builder
                    .buildSerializationContext(baos, this);
            ctx.writeMessageStart("dynamicSerialize");
            serialize(ctx, obj);
            ctx.writeMessageEnd();
            return baos.toByteArray();
        } finally {
            if (baos != null) {
                try {
                    // return stream to pool
                    baos.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Serialize an object to a byte array
     *
     * @param obj
     *            the object
     * @param os
     *            the output stream
     * @throws SerializationException
     */
    public void serialize(Object obj, OutputStream os)
            throws SerializationException {

        ISerializationContext ctx = this.builder.buildSerializationContext(os,
                this);
        ctx.writeMessageStart("dynamicSerialize");
        serialize(ctx, obj);
        ctx.writeMessageEnd();

    }

    /**
     * Serialize an object using a context
     *
     * This method is not intended to be used by end users.
     *
     * @param ctx
     *            the serialization context
     * @param obj
     *            the object to serialize
     * @throws SerializationException
     */
    public void serialize(ISerializationContext ctx, Object obj)
            throws SerializationException {
        BeanMap beanMap = null;

        if (obj != null && !obj.getClass().isArray()) {
            beanMap = SerializationCache.getBeanMap(obj);
        }
        try {
            SerializationMetadata metadata = null;
            if (obj != null) {
                metadata = getSerializationMetadata(obj.getClass().getName());
            }

            ((ThriftSerializationContext) ctx).serializeMessage(obj, beanMap,
                    metadata);
        } finally {
            if (beanMap != null) {
                SerializationCache.returnBeanMap(beanMap, obj);
            }
        }
    }

    /**
     * Deserialize an object from a stream
     *
     * @param istream
     * @return
     * @throws SerializationException
     */
    public Object deserialize(InputStream istream)
            throws SerializationException {
        IDeserializationContext ctx = this.builder
                .buildDeserializationContext(istream, this);
        ctx.readMessageStart();
        Object obj;
        try {
            obj = deserialize(ctx);
        } catch (CriticalSerializationException e) {
            throw new SerializationException(e);
        }
        ctx.readMessageEnd();
        return obj;
    }

    /**
     * Deserialize from a context
     *
     * Not intended to be used by end users
     *
     * @param ctx
     * @return
     * @throws SerializationException
     */
    public Object deserialize(IDeserializationContext ctx)
            throws SerializationException, CriticalSerializationException {
        return ((ThriftSerializationContext) ctx).deserializeMessage();
    }

    /**
     * Deserialize an object from a byte[]
     *
     * @param data
     * @return
     * @throws SerializationException
     */
    public Object deserialize(byte[] data) throws SerializationException {
        IDeserializationContext ctx = this.builder
                .buildDeserializationContext(data, this);
        ctx.readMessageStart();
        Object obj;
        try {
            obj = deserialize(ctx);
        } catch (CriticalSerializationException e) {
            throw new SerializationException(e);
        }
        ctx.readMessageEnd();
        return obj;
    }

    /**
     * Register a new dynamic serialize adapter
     *
     * @param clazz
     * @param adapter
     * @return the adapter argument (needed for spring configuration)
     * @throws RuntimeException
     *             if the class already has an adapter registered
     */
    public static <T> ISerializationTypeAdapter<T> registerAdapter(
            Class<? extends T> clazz, ISerializationTypeAdapter<T> adapter) {
        String className = clazz.getName();
        SerializationMetadata md = serializedAttributes.get(className);
        if (md != null) {
            String reason = "Metadata already exists";
            if (md.serializationFactory != null) {
                reason = "Adapter conflict, attempted to register adapter '"
                        + adapter.getClass()
                        + "' when type already has a registered adapter '"
                        + md.serializationFactory.getClass() + "'";
            }
            throw new RuntimeException(
                    "Could not create serialization metadata for class: "
                            + clazz + ". " + reason);
        }
        md = new SerializationMetadata();
        md.serializationFactory = adapter;
        md.adapterStructName = className;
        serializedAttributes.put(md.adapterStructName, md);
        return adapter;
    }

    /**
     * Get the serialization metadata. Build it if not found
     *
     * @param name
     * @return
     */
    public static SerializationMetadata getSerializationMetadata(String name) {
        /*
         * we can't synchronize on this because it's possible the
         * Class.forName() will trigger code that comes back into here and then
         * deadlocks
         */
        SerializationMetadata sm = serializedAttributes.get(name);
        if (sm == null) {
            try {
                sm = getSerializationMetadata(Class.forName(name, true,
                        DynamicSerializationManager.class.getClassLoader()));
                if (sm == null) {
                    serializedAttributes.put(name, NO_METADATA);
                }
            } catch (ClassNotFoundException e) {
                /*
                 * ignore, it will return null and if it's an issue it will be
                 * thrown as an exception later
                 */
                // e.printStackTrace();
            }
        }

        if (sm == NO_METADATA) {
            return null;
        }
        return sm;

    }

    /**
     * Inspect a class and return the metadata for the object
     *
     * If the class has not been annotated, this will return null
     *
     * The metadata is cached for performance
     *
     * @param c
     *            the class
     * @return the metadata
     */
    public static SerializationMetadata getSerializationMetadata(Class<?> c) {

        // Check for base types

        SerializationMetadata attribs = serializedAttributes.get(c.getName());
        if (attribs != null) {
            return attribs;
        }

        attribs = new SerializationMetadata();
        attribs.attributeNames = new ArrayList<>();
        attribs.attributesWithFactories = new HashMap<>();

        DynamicSerializeTypeAdapter serializeAdapterTag = c
                .getAnnotation(DynamicSerializeTypeAdapter.class);

        // Check to see if there is an adapter
        if (serializeAdapterTag != null) {
            Class<?> factoryTag = serializeAdapterTag.factory();
            try {
                attribs.serializationFactory = (ISerializationTypeAdapter<?>) factoryTag
                        .newInstance();
                attribs.adapterStructName = c.getName();
            } catch (Exception e) {
                throw new RuntimeException(
                        "Factory could not be constructed: " + factoryTag, e);
            }
        }

        // check to see if superclass has an adapter
        if (attribs.serializationFactory == null) {
            Class<?> superClazz = c.getSuperclass();
            while (superClazz != null && attribs.serializationFactory == null) {
                SerializationMetadata superMd = getSerializationMetadata(
                        superClazz);
                if (superMd != null && superMd.serializationFactory != null) {
                    attribs.serializationFactory = superMd.serializationFactory;
                    attribs.adapterStructName = superMd.adapterStructName;
                }
                superClazz = superClazz.getSuperclass();
            }
        }

        // Make sure the object is annotated or has an adapter. If not, return
        // null
        DynamicSerialize serializeTag = c.getAnnotation(DynamicSerialize.class);
        if (serializeTag == null && attribs.serializationFactory == null) {
            return null;
        }

        if (attribs.serializationFactory == null) {
            // Go through the class and find the fields with annotations
            Class<?> clazz = c;
            Set<String> getters = new HashSet<>();
            Set<String> setters = new HashSet<>();
            while (clazz != null && clazz != Object.class) {

                // Make sure a getter and setter has been defined, and throw an
                // exception if they haven't been

                getters.clear();
                setters.clear();
                Method[] methods = c.getMethods();
                for (Method m : methods) {
                    String name = m.getName();
                    if (name.startsWith("get")) {
                        name = name.substring(3);
                        getters.add(name.toLowerCase());
                    } else if (name.startsWith("is")) {
                        name = name.substring(2);
                        getters.add(name.toLowerCase());
                    } else if (name.startsWith("set")) {
                        name = name.substring(3);
                        setters.add(name.toLowerCase());
                    }
                }

                java.lang.reflect.Field[] fields = clazz.getDeclaredFields();
                for (java.lang.reflect.Field field : fields) {

                    int modifier = field.getModifiers();
                    if (Modifier.isFinal(modifier)) {
                        continue;
                    }

                    DynamicSerializeElement annotation = field
                            .getAnnotation(DynamicSerializeElement.class);
                    if (annotation != null) {
                        String fieldName = field.getName();

                        attribs.attributeNames.add(field.getName());
                        if (serializeAdapterTag == null) {
                            serializeAdapterTag = field.getType().getAnnotation(
                                    DynamicSerializeTypeAdapter.class);
                        }
                        if (serializeAdapterTag != null) {
                            try {
                                attribs.attributesWithFactories.put(fieldName,
                                        serializeAdapterTag.factory()
                                                .newInstance());
                            } catch (Exception e) {
                                throw new RuntimeException(
                                        "Factory could not be instantiated", e);
                            }
                        }
                        // Throw a validation exception if necessary
                        boolean foundGetter = false;
                        boolean foundSetter = false;
                        String lower = fieldName.toLowerCase();

                        if (getters.contains(lower)) {
                            foundGetter = true;
                        }

                        if (setters.contains(lower)) {
                            foundSetter = true;
                        }

                        if (!foundGetter || !foundSetter) {
                            String missing = "";
                            if (!foundGetter && !foundSetter) {
                                missing = "Getter and Setter";
                            } else if (!foundGetter) {
                                missing = "Getter";
                            } else if (!foundSetter) {
                                missing = "Setter";
                            }

                            throw new RuntimeException("Required " + missing
                                    + " on " + clazz.getName() + ":"
                                    + field.getName() + " is missing");
                        }

                    }
                }
                clazz = clazz.getSuperclass();
            }
        }

        // Sort to guarantee universal ordering
        Collections.sort(attribs.attributeNames);
        serializedAttributes.put(c.getName(), attribs);

        // inspect inner classes
        Class<?>[] innerClzs = c.getClasses();
        for (Class<?> innerClz : innerClzs) {
            getSerializationMetadata(innerClz);
        }

        return attribs;
    }

    public static synchronized DynamicSerializationManager getManager(
            SerializationType type) {
        DynamicSerializationManager mgr = instanceMap.get(type);
        if (mgr == null) {
            mgr = new DynamicSerializationManager(type);
            instanceMap.put(type, mgr);
        }

        return mgr;
    }

    private DynamicSerializationManager(SerializationType type) {
        if (type == SerializationType.Thrift) {
            builder = new ThriftSerializationContextBuilder();
        }
    }

}
