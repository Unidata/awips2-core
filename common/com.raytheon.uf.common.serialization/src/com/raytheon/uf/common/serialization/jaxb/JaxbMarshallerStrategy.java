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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.raytheon.uf.common.serialization.MarshalOptions;

/**
 * Manages JAXB marshalling and unmarshalling including resource creation
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 14, 2014 3373       bclement     Initial creation
 * Aug 08, 2014 3503       bclement     removed ufstatus
 * Jun 02, 2015 4496       nabowle      Unmarshal from XMLStreamReader if not
 *                                      using the CustomJAXBUnmarshaller to
 *                                      prevent XEE attacks.
 *
 * </pre>
 *
 * @author bclement
 * @version 1.0
 */
public class JaxbMarshallerStrategy {

    private static final XMLInputFactory xif = XMLInputFactory.newFactory();

    static {
        // disable to prevent XML External Entity attacks.
        xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    }

    private static final Logger log = Logger
            .getLogger(JaxbMarshallerStrategy.class.getName());

    /**
     *
     * Saves all validation events so if an error is caught handlers have an
     * option of getting more accurate information about what happened
     *
     * <pre>
     *
     * SOFTWARE HISTORY
     *
     * Date         Ticket#    Engineer    Description
     * ------------ ---------- ----------- --------------------------
     * Sep 2, 2011            ekladstrup     Initial creation
     *
     * </pre>
     *
     * @author ekladstrup
     * @version 1.0
     */
    protected static class MaintainEventsValidationHandler implements
            ValidationEventHandler {

        private final ArrayList<ValidationEvent> events = new ArrayList<ValidationEvent>(
                0);

        @Override
        public boolean handleEvent(ValidationEvent event) {
            events.add(event);
            return true;
        }

        public ArrayList<ValidationEvent> getEvents() {
            synchronized (events) {
                return new ArrayList<ValidationEvent>(events);
            }
        }

        public void clearEvents() {
            synchronized (events) {
                events.clear();
            }
        }
    }

    /*
     * Note about unmarshalling:
     *
     * Any unmarshallers that are not an instance of CustomJAXBUnmarshaller will
     * use an XMLStreamReader to parse the source to prevent External Entity
     * Attacks.
     *
     * CustomJAXBUnmarshaller is only used when the list of Classes provided to
     * JAXBContext.newInstance(...) contains JaxbDummyObject.class, which causes
     * the jaxb.properties in this package to be loaded and then uses
     * SerializationContextFactory.createContext() for the JAXBContext creation.
     *
     * CustomJAXBUnmarshaller uses CustomEntityResolver to prevent External
     * Entity Attacks.
     */

    /**
     * @see JaxbMarshallerStrategy#unmarshalFromReader(Unmarshaller, Reader)
     * @param context
     * @param reader
     * @return
     * @throws JAXBException
     */
    public Object unmarshalFromReader(JAXBContext context, Reader reader) throws JAXBException {
        Unmarshaller unmarshaller = createUnmarshaller(context);
        return unmarshalFromReader(unmarshaller, reader);
    }

    /**
     * @see Unmarshaller#unmarshal(Reader)
     * @param unmarshaller
     * @param reader
     *            input source which is closed before return
     * @return
     * @throws JAXBException
     */
    protected Object unmarshalFromReader(Unmarshaller unmarshaller,
            Reader reader) throws JAXBException {
        try {
            if (unmarshaller instanceof CustomJAXBUnmarshaller) {
                return unmarshaller.unmarshal(reader);
            } else {
                try {
                    XMLStreamReader xsr = xif.createXMLStreamReader(reader);
                    return unmarshaller.unmarshal(xsr);
                } catch (XMLStreamException e) {
                    throw new JAXBException(e);
                }
            }
        } finally {
            handleEvents(unmarshaller, null);
            try {
                reader.close();
            } catch (IOException e) {
                log.log(Level.WARNING,
                        "Unable to close JAXB reader: "
                                + e.getLocalizedMessage());
            }
        }
    }

    /**
     * @see JaxbMarshallerStrategy#unmarshalFromInputStream(Unmarshaller,
     *      InputStream)
     * @param context
     * @param is
     * @return
     * @throws JAXBException
     */
    public Object unmarshalFromInputStream(JAXBContext context, InputStream is)
            throws JAXBException {
        Unmarshaller unmarshaller = createUnmarshaller(context);
        return unmarshalFromInputStream(unmarshaller, is);
    }

    /**
     * @see Unmarshaller#unmarshal(InputStream)
     * @param msh
     * @param is
     *            input source which is closed before return
     * @return
     * @throws JAXBException
     */
    protected Object unmarshalFromInputStream(Unmarshaller msh, InputStream is)
            throws JAXBException {
        try {
            if (msh instanceof CustomJAXBUnmarshaller) {
                return msh.unmarshal(is);
            } else {
                try {
                    XMLStreamReader xsr = xif.createXMLStreamReader(is);
                    return msh.unmarshal(xsr);
                } catch (XMLStreamException e) {
                    throw new JAXBException(e);
                }
            }
        } finally {
            if (msh != null) {
                handleEvents(msh, null);
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    log.log(Level.WARNING,
                            "Unable to close JAXB stream: "
                                    + e.getLocalizedMessage());
                }
            }
        }
    }

    /**
     * @see JAXBContext#createMarshaller()
     * @param context
     * @return
     * @throws JAXBException
     */
    protected Marshaller createMarshaller(JAXBContext context)
            throws JAXBException {
        return context.createMarshaller();
    }

    /**
     * @see JAXBContext#createUnmarshaller()
     * @param context
     * @return
     * @throws JAXBException
     */
    protected Unmarshaller createUnmarshaller(JAXBContext context)
            throws JAXBException {
        Unmarshaller m = context.createUnmarshaller();
        // set event handler to be able to retrieve ValidationEvents
        m.setEventHandler(new MaintainEventsValidationHandler());
        return m;
    }

    /**
     * @see JaxbMarshallerStrategy#marshalToXml(Marshaller, Object,
     *      MarshalOptions)
     * @param context
     * @param obj
     * @return
     * @throws JAXBException
     */
    public String marshalToXml(JAXBContext context, Object obj,
            MarshalOptions options) throws JAXBException {
        Marshaller marshaller = createMarshaller(context);
        return marshalToXml(marshaller, obj, options);
    }

    /**
     * @see Marshaller#marshal(Object, java.io.Writer)
     * @param msh
     * @param obj
     * @param options
     *            Formatting options applied to marshaller
     * @return
     * @throws JAXBException
     */
    protected String marshalToXml(Marshaller msh, Object obj,
            MarshalOptions options) throws JAXBException {
        StringWriter writer = new StringWriter();
        options.apply(msh);
        msh.marshal(obj, writer);
        return writer.toString();
    }

    /**
     * @see JaxbMarshallerStrategy#marshalToStream(Marshaller, Object,
     *      OutputStream, MarshalOptions)
     * @param context
     * @param obj
     * @param out
     * @param options
     * @throws JAXBException
     */
    public void marshalToStream(JAXBContext context, Object obj,
            OutputStream out, MarshalOptions options) throws JAXBException {
        Marshaller marshaller = createMarshaller(context);
        marshalToStream(marshaller, obj, out, options);
    }

    /**
     * @see Marshaller#marshal(Object, OutputStream)
     * @param msh
     * @param obj
     * @param out
     *            destination which is not closed before returning
     * @param options
     *            formatting options applied to marshaller
     * @throws JAXBException
     */
    protected void marshalToStream(Marshaller msh, Object obj,
            OutputStream out, MarshalOptions options) throws JAXBException {
        options.apply(msh);
        msh.marshal(obj, out);
    }

    /**
     * Processes the events received by an unmarshaller when parsing XML.
     *
     * @param msh
     *            the unmarshaller
     */
    protected void handleEvents(Unmarshaller msh, String name) {
        try {
            ValidationEventHandler h = msh.getEventHandler();
            if (h instanceof MaintainEventsValidationHandler) {
                boolean allInfo = true;
                MaintainEventsValidationHandler mh = (MaintainEventsValidationHandler) h;
                for (ValidationEvent event : mh.getEvents()) {
                    if (event.getSeverity() == ValidationEvent.FATAL_ERROR) {
                        // If we had a fatal error, report events at their
                        // native severity, otherwise use all info as the
                        // unmarshalling didn't fail
                        allInfo = false;
                        break;
                    }
                }
                for (ValidationEvent event : mh.getEvents()) {
                    Level l = Level.INFO;
                    if (!allInfo) {
                        switch (event.getSeverity()) {
                        case ValidationEvent.FATAL_ERROR:
                        case ValidationEvent.ERROR:
                            l = Level.SEVERE;
                            break;
                        case ValidationEvent.WARNING:
                            l = Level.WARNING;
                            break;
                        }
                    }
                    log.log(l,
                            (name != null ? name : "") + ": "
                                    + event.getMessage() + " on line "
                                    + event.getLocator().getLineNumber()
                                    + " column "
                                    + event.getLocator().getColumnNumber(),
                            event.getLinkedException());
                }
                mh.clearEvents();
            }
        } catch (JAXBException e) {
            // Ignore, unable to get handler
        }
    }

}
