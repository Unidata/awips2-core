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
package com.raytheon.uf.common.serialization.jaxb;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEventHandler;

import com.raytheon.uf.common.serialization.MarshalOptions;
import com.raytheon.uf.common.util.stream.CountingInputStream;
import com.raytheon.uf.common.util.stream.CountingOutputStream;
import com.raytheon.uf.common.util.stream.CountingReader;

/**
 * JAXB Marshaller strategy that uses a pool for marshaller and unmarshaller
 * objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 14, 2014 3373       bclement     Initial creation
 * Jul 25, 2014 3378       bclement     removed uf prefix from system properties
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class PooledJaxbMarshallerStrategy extends JaxbMarshallerStrategy {

    public static final int DEFAULT_POOL_SIZE = Integer.getInteger(
            "jaxb.pool.size", 10);

    public static final int DEFAULT_OBJ_SIZE_LIMIT = Integer.getInteger(
            "jaxb.pool.object.limit", 1024 * 200); // 200KB

    private final int poolSize;

    private final int sizeLimit;

    protected final Queue<Unmarshaller> unmarshallers = new ConcurrentLinkedQueue<Unmarshaller>();

    protected final Queue<Marshaller> marshallers = new ConcurrentLinkedQueue<Marshaller>();

    /**
     * 
     */
    public PooledJaxbMarshallerStrategy() {
        this(DEFAULT_POOL_SIZE, DEFAULT_OBJ_SIZE_LIMIT);
    }


    /**
     * @param poolSize
     * @param pooledObjectSizeLimit
     */
    public PooledJaxbMarshallerStrategy(int poolSize, int pooledObjectSizeLimit) {
        this.poolSize = poolSize;
        this.sizeLimit = pooledObjectSizeLimit;
    }


    /**
     * Gets a marshaller, creating one if one is not currently available.
     * 
     * @return
     * @throws JAXBException
     */
    protected Marshaller getMarshaller(JAXBContext ctx) throws JAXBException {
        Marshaller m = marshallers.poll();
        if (m == null) {
            m = createMarshaller(ctx);
        }

        return m;
    }

    /**
     * Gets an unmarshaller, creating one if one is not currently available.
     * 
     * @return an unmarshaller
     * @throws JAXBException
     */
    protected Unmarshaller getUnmarshaller(JAXBContext ctx)
            throws JAXBException {
        Unmarshaller m = unmarshallers.poll();
        if (m == null) {
            m = createUnmarshaller(ctx);
        } else {
            /*
             * clear events in event handler ( just in case it was missed, don't
             * intentionally rely on this path to clear the events for you, they
             * don't need to live that long )
             */
            ValidationEventHandler h = m.getEventHandler();
            if (h instanceof MaintainEventsValidationHandler) {
                MaintainEventsValidationHandler sh = (MaintainEventsValidationHandler) h;
                sh.clearEvents();
            }
        }

        return m;
    }

    /**
     * attempt to return unmarshaller to pool
     * 
     * @param unmarshaller
     * @param objSize
     */
    protected void returnUnmarshaller(Unmarshaller unmarshaller, long objSize) {
        if (objSize < sizeLimit && unmarshallers.size() < poolSize) {
            unmarshallers.add(unmarshaller);
        }
    }

    /**
     * attempt to return marshaller to pool
     * 
     * @param marshaller
     * @param objSize
     */
    protected void returnMarshaller(Marshaller marshaller, long objSize) {
        if (objSize < sizeLimit && marshallers.size() < poolSize) {
            marshallers.add(marshaller);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.serialization.jaxb.JaxbMarshallerStrategy#
     * unmarshalFromReader(javax.xml.bind.JAXBContext, java.io.Reader)
     */
    @Override
    public Object unmarshalFromReader(JAXBContext context, Reader reader)
            throws JAXBException {
        CountingReader wrappedReader = new CountingReader(reader);
        Unmarshaller unmarshaller = getUnmarshaller(context);
        try {
            return unmarshalFromReader(unmarshaller, wrappedReader);
        } finally {
            long charsRead = wrappedReader.getCharactersRead();
            /* assumes UTF-8 encoded to get approx bytes */
            returnUnmarshaller(unmarshaller, charsRead);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.serialization.jaxb.JaxbMarshallerStrategy#
     * unmarshalFromInputStream(javax.xml.bind.JAXBContext, java.io.InputStream)
     */
    @Override
    public Object unmarshalFromInputStream(JAXBContext context, InputStream is)
            throws JAXBException {
        CountingInputStream wrappedStream = new CountingInputStream(is);
        Unmarshaller unmarshaller = getUnmarshaller(context);
        try {
            return unmarshalFromInputStream(unmarshaller, wrappedStream);
        } finally {
            long bytesRead = wrappedStream.getBytesRead();
            returnUnmarshaller(unmarshaller, bytesRead);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.serialization.jaxb.JaxbMarshallerStrategy#marshalToXml
     * (javax.xml.bind.JAXBContext, java.lang.Object, boolean)
     */
    @Override
    public String marshalToXml(JAXBContext context, Object obj,
            MarshalOptions options) throws JAXBException {
        Marshaller marshaller = getMarshaller(context);
        String rval = marshalToXml(marshaller, obj, options);
        /* assumes UTF-8 encoded to get approx bytes */
        returnMarshaller(marshaller, rval.length());
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.serialization.jaxb.JaxbMarshallerStrategy#
     * marshalToStream(javax.xml.bind.JAXBContext, java.lang.Object,
     * java.io.OutputStream, boolean)
     */
    @Override
    public void marshalToStream(JAXBContext context, Object obj,
            OutputStream out,MarshalOptions options) throws JAXBException {
        Marshaller marshaller = getMarshaller(context);
        CountingOutputStream wrappedOut = new CountingOutputStream(out);
        try {
            super.marshalToStream(context, obj, wrappedOut, options);
        } finally {
            returnMarshaller(marshaller, wrappedOut.getBytesWritten());
        }
    }

}
