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
package com.raytheon.uf.common.message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBException;

import com.raytheon.uf.common.serialization.JAXBManager;

/**
 * Handles registration for JAXB classes that are used in XML message
 * serialization
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 11, 2014 3583       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class JAXBMessageRegistry {

    private static final boolean POOLING = Boolean
            .getBoolean("message.jaxb.marshaller.pooling");

    private static final Set<Class<?>> contextClasses = new HashSet<Class<?>>();

    private static volatile JAXBManager manager;

    private static final JAXBMessageRegistry instance = new JAXBMessageRegistry();

    /**
     * 
     */
    private JAXBMessageRegistry() {
    }

    /**
     * @return static instance
     */
    public static JAXBMessageRegistry getInstance() {
        return instance;
    }

    /**
     * Register JAXB classes for XML Message serialization
     * 
     * @param classes
     * @return static instance for Spring compatibility
     */
    public static JAXBMessageRegistry register(List<Class<?>> classes) {
        boolean changed = false;
        synchronized (JAXBMessageRegistry.class) {
            for (Class<?> c : classes) {
                if (contextClasses.add(c)) {
                    /* set did not already contain c, invalidate current context */
                    changed = true;
                }
            }
            if (changed) {
                manager = null;
            }
        }
        return instance;
    }

    /**
     * The result of this method should not be cached as late registrations will
     * cause the underlying JAXB manager to be re-instantiated.
     * 
     * @return static JAXB manager for messages
     * @throws JAXBException
     */
    public static JAXBManager getJAXBManager() throws JAXBException {
        if (manager == null) {
            synchronized (JAXBMessageRegistry.class) {
                if (manager == null) {
                    Class<?>[] classes = contextClasses
                            .toArray(new Class<?>[contextClasses.size()]);
                    manager = new JAXBManager(POOLING, classes);
                }
            }
        }
        return manager;
    }

    /**
     * Convert an instance of a class to an XML representation in a string. Uses
     * JAXB.
     * 
     * @param obj
     *            Object being marshalled
     * @return XML string representation of the object
     * @throws JAXBException
     */
    public static String marshalToXml(Object obj) throws JAXBException {
        return getJAXBManager().marshalToXml(obj);
    }

}
