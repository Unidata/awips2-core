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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

/**
 * A JAXBManager that only supports a single class (including any classes that
 * are contained within that class). Useful when dealing specifically with an
 * XML file where you know the type that corresponds to it.
 * 
 * Primarily used for convenience to avoid casting.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 01, 2013  2361      njensen     Initial creation
 * Jul 21, 2014  3373      bclement    added pooling boolean constructor
 * Aug 08, 2014  3503      bclement    removed ufstatus
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 * @param <T>
 */

public class SingleTypeJAXBManager<T extends Object> extends JAXBManager {

    protected final Class<T> type;

    /**
     * Constructor. Only accepts a single class.
     * 
     * @param clazz
     *            the class of the object to read/write XML for.
     * @throws JAXBException
     */
    public SingleTypeJAXBManager(Class<T> clazz) throws JAXBException {
        this(false, clazz);

    }

    /**
     * @see JAXBManager#JAXBManager(boolean, Class...)
     * @param pooling
     * @param clazz
     * @throws JAXBException
     */
    public SingleTypeJAXBManager(boolean pooling, Class<T> clazz)
            throws JAXBException {
        super(pooling, clazz);
        this.type = clazz;
    }

    /**
     * Instantiates an object from the XML representation in a File.
     * 
     * @param file
     *            The XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     */
    public T unmarshalFromXmlFile(File file) throws SerializationException {
        return super.unmarshalFromXmlFile(type, file);
    }

    /**
     * Instantiates an object from the XML representation in a File.
     * 
     * @param filePath
     *            The path to the XML file
     * @return A new instance from the XML representation
     * @throws SerializationException
     */
    public T unmarshalFromXmlFile(String filePath)
            throws SerializationException {
        return super.unmarshalFromXmlFile(type, new File(filePath));
    }

    /**
     * Creates a SingleTypeJAXBManager for a specified type, but catches any
     * JAXBExceptions thrown and logs them. If an exception does occur, returns
     * null.
     * 
     * @param clazz
     *            the class of the object to read/write XML for
     * @return the SingleTypeJAXBManager or null if an exception occurred
     */
    public static <A> SingleTypeJAXBManager<A> createWithoutException(
            Class<A> clazz) {
        return createWithoutException(false, clazz);
    }

    /**
     * Creates a SingleTypeJAXBManager for a specified type, but catches any
     * JAXBExceptions thrown and logs them. If an exception does occur, returns
     * null.
     * 
     * @param pooling
     *            true if jaxb manager should pool resources
     * @param clazz
     *            the class of the object to read/write XML for
     * @return the SingleTypeJAXBManager or null if an exception occurred
     */
    public static <A> SingleTypeJAXBManager<A> createWithoutException(
            boolean pooling, Class<A> clazz) {
        SingleTypeJAXBManager<A> retVal = null;
        try {
            retVal = new SingleTypeJAXBManager<A>(clazz);
        } catch (JAXBException e) {
            // technically this should only ever happen if a developer messes
            // up, so we're going to print the stacktrace too as extra warning
            e.printStackTrace();
            Logger.getGlobal().log(
                    Level.SEVERE,
                    "Unable to initialize single type JAXB manager: "
                            + e.getLocalizedMessage());
        }

        return retVal;
    }

}
