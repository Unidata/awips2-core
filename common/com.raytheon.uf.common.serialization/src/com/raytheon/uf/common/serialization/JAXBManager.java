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

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Collections;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.serialization.jaxb.JaxbMarshallerStrategy;
import com.raytheon.uf.common.serialization.jaxb.PooledJaxbMarshallerStrategy;

/**
 * Provides an easy and convenient layer to marshal or unmarshal objects to and
 * from XML using JAXB. An instance of this class is thread-safe, it will use
 * separate marshallers and unmarshallers if used simultaneously by different
 * threads.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 24, 2008            chammack     Initial creation
 * Nov 13, 2008            njensen      Added thrift methods
 * May 22, 2013 1917       rjpeter      Added non-pretty print option to jaxb serialize methods.
 * Aug 18, 2013 #2097      dhladky      Allowed extension by OGCJAXBManager
 * Sep 30, 2013 2361       njensen      Refactored for cleanliness
 * Nov 14, 2013 2361       njensen      Added lazy init option, improved unmarshal error message
 * Apr 16, 2014 2928       rjpeter      Updated marshalToStream to not close the stream.
 * Apr 25, 2014 2060       njensen      Improved printout
 * Jul 15, 2014 3373       bclement     moved marshaller management to JaxbMarshallerStrategy
 *                                      added MarshalOptions, no longer pools by default
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class JAXBManager {

    private volatile JAXBContext jaxbContext;

    private Class<?>[] clazz;

    private final JaxbMarshallerStrategy marshStrategy;

    /**
     * Constructor. Clazz should include any classes that this JAXBManager needs
     * to marshal to XML or unmarshal from XML. Does not need to include classes
     * contained as fields or inner classes of other classes already passed to
     * the constructor.
     * 
     * @param clazz
     *            classes that this instance must know about for
     *            marshalling/unmarshalling
     * @throws JAXBException
     */
    public JAXBManager(Class<?>... clazz) throws JAXBException {
        this(false, clazz);
    }

    /**
     * Constructor. Clazz should include any classes that this JAXBManager needs
     * to marshal to XML or unmarshal from XML. Does not need to include classes
     * contained as fields or inner classes of other classes already passed to
     * the constructor.
     * 
     * @param pooling
     *            whether or not to pool (un)marshallers
     * @param clazz
     *            classes that this instance must know about for
     *            marshalling/unmarshalling
     * @throws JAXBException
     */
    public JAXBManager(boolean pooling, Class<?>... clazz)
            throws JAXBException {
        this(pooling ? new PooledJaxbMarshallerStrategy()
                : new JaxbMarshallerStrategy(), clazz);
    }

    /**
     * @see #JAXBManager(boolean, Class...)
     * @param marshStrategy
     * @param clazz
     * @throws JAXBException
     */
    public JAXBManager(JaxbMarshallerStrategy marshStrategy,
            Class<?>... clazz) throws JAXBException {
        this.clazz = clazz;
        getJaxbContext();
        this.marshStrategy = marshStrategy;
    }

    /**
     * Returns the JAXB Context behind this JAXBManager.
     * 
     * @return the JAXBContext
     * @throws JAXBException
     * @Deprecated TODO This method should be protected and the JAXBContext
     *             should be hidden from outside libraries. Any options needing
     *             to be applied to the context or its marshallers/unmarshallers
     *             should either have convenience methods or flags on
     *             JAXBManager to provide that functionality.
     */
    @Deprecated
    public JAXBContext getJaxbContext() throws JAXBException {
        if (jaxbContext == null) {
            synchronized (this) {
                if (jaxbContext == null) {
                    long t0 = System.currentTimeMillis();
                    jaxbContext = JAXBContext.newInstance(clazz,
                            getJaxbConfig());
                    if (clazz.length == 1) {
                        System.out.println("JAXB context for "
                                + clazz[0].getSimpleName() + " inited in: "
                                + (System.currentTimeMillis() - t0) + "ms");
                    }
                    clazz = null;
                }
            }
        }
        return jaxbContext;
    }

    /**
     * @return mapping of JAXB property names to configuration objects
     * @throws JAXBException
     */
    protected Map<String, Object> getJaxbConfig() throws JAXBException {
        return Collections.emptyMap();
    }

    /**
     * Instantiates an object from the XML representation in a string.
     * 
     * @param xml
     *            The XML representation
     * @return A new instance from the XML representation
     * @throws JAXBException
     */
    public Object unmarshalFromXml(String xml) throws JAXBException {
        StringReader reader = new StringReader(xml);
        JAXBContext cxt = getJaxbContext();
        return marshStrategy.unmarshalFromReader(cxt, reader);
    }



    /**
     * Convert an instance of a class to an XML pretty print representation in a
     * string.
     * 
     * @param obj
     *            Object being marshalled
     * @return XML string representation of the object
     * @throws JAXBException
     */
    public String marshalToXml(Object obj) throws JAXBException {
        return marshalToXml(obj, MarshalOptions.FORMATTED);
    }

    /**
     * Convert an instance of a class to an XML representation in a string.
     * 
     * @param obj
     *            Object being marshalled
     * @param options
     *            Formatting options
     * @return XML string representation of the object
     * @throws JAXBException
     */
    public String marshalToXml(Object obj, MarshalOptions options)
            throws JAXBException {
        JAXBContext ctx = getJaxbContext();
        return marshStrategy.marshalToXml(ctx, obj, options);
    }

    /**
     * Convert an instance of a class to an XML representation and writes pretty
     * print formatted XML to file.
     * 
     * @param obj
     *            Object to be marshaled
     * @param filePath
     *            Path to the output file
     * @throws SerializationException
     */
    public void marshalToXmlFile(Object obj, String filePath)
            throws SerializationException {
        marshalToXmlFile(obj, filePath, MarshalOptions.FORMATTED);
    }

    /**
     * Convert an instance of a class to an XML representation and writes XML to
     * file.
     * 
     * @param obj
     *            Object to be marshaled
     * @param filePath
     *            Path to the output file
     * @param options
     *            Formatting options
     * @throws SerializationException
     */
    public void marshalToXmlFile(Object obj, String filePath,
            MarshalOptions options) throws SerializationException {
        OutputStream os = null;
        try {
            os = new FileOutputStream(new File(filePath));
            marshalToStream(obj, os, options);
        } catch (SerializationException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializationException(e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    /**
     * Convert an instance of a class to an XML representation and writes pretty
     * print formatted XML to output stream.
     * 
     * @param obj
     * @param out
     * @throws SerializationException
     */
    public void marshalToStream(Object obj, OutputStream out)
            throws SerializationException {
        marshalToStream(obj, out, MarshalOptions.FORMATTED);
    }

    /**
     * Convert an instance of a class to an XML representation and writes XML to
     * output stream.
     * 
     * @param obj
     * @param out
     * @param options
     * 
     * @throws SerializationException
     */
    public void marshalToStream(Object obj, OutputStream out,
            MarshalOptions options) throws SerializationException {
        try {
            JAXBContext cxt = getJaxbContext();
            marshStrategy.marshalToStream(cxt, obj, out, options);
        } catch (JAXBException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Instantiates an object from the XML representation in a File.
     * 
     * @param filePath
     *            The path to the XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     * @Deprecated Use unmarshalFromXmlFile(Class<?>, String) instead
     */
    @Deprecated
    public Object unmarshalFromXmlFile(String filePath)
            throws SerializationException {
        return unmarshalFromXmlFile(new File(filePath));
    }

    /**
     * Instantiates an object from the XML representation in a File.
     * 
     * @param file
     *            The XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     * @Deprecated Use unmarshalFromXmlFile(Class<?>, File) instead
     */
    @Deprecated
    public Object unmarshalFromXmlFile(File file) throws SerializationException {
        return unmarshalFromXmlFile(Object.class, file);
    }

    /**
     * Instantiates an object of the specified type from the XML representation
     * in a File.
     * 
     * @param clazz
     *            The class to cast the Object in the file to
     * @param filePath
     *            The path to the XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     */
    public <T> T unmarshalFromXmlFile(Class<T> clazz, String filePath)
            throws SerializationException {
        return unmarshalFromXmlFile(clazz, new File(filePath));
    }

    /**
     * Instantiates an object from the XML representation in a File.
     * 
     * @param clazz
     *            The class to cast the Object in the file to
     * @param file
     *            The XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     */
    public <T> T unmarshalFromXmlFile(Class<T> clazz, File file)
            throws SerializationException {
        try {
            return clazz.cast(internalUnmarshalFromXmlFile(file));
        } catch (ClassCastException cce) {
            throw new SerializationException(cce);
        }
    }

    /**
     * Instantiates an object from the XML representation in a stream.
     * 
     * @param is
     *            The input stream. The stream will be closed by this operation.
     * @return A new instance from the XML representation
     * @throws SerializationException
     */
    public Object unmarshalFromInputStream(InputStream is)
            throws SerializationException {
        try {
            JAXBContext ctx = getJaxbContext();
            return marshStrategy.unmarshalFromInputStream(ctx, is);
        } catch (JAXBException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Unmarshals an object from an xml file.
     * 
     * @param file
     *            the file to unmarshal and object from.
     * @return the object from the file
     * @throws SerializationException
     */
    protected Object internalUnmarshalFromXmlFile(File file)
            throws SerializationException {
        try {
            JAXBContext ctx = getJaxbContext();
            FileReader reader = new FileReader(file);
            return marshStrategy.unmarshalFromReader(ctx, reader);
        } catch (Exception e) {
            throw new SerializationException("Error reading " + file.getName()
                    + "\n" + e.getLocalizedMessage(), e);
        } 
    }

}
