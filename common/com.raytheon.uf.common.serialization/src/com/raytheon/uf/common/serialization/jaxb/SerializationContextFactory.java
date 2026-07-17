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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.glassfish.jaxb.runtime.api.TypeReference;
import org.glassfish.jaxb.runtime.v2.model.annotation.RuntimeInlineAnnotationReader;
import org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl;
import org.glassfish.jaxb.runtime.v2.runtime.JAXBContextImpl.JAXBContextBuilder;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;

/**
 * JAXBContext factory, creates the jaxb context. This classes is used to create
 * a custom JAXBContext and is only used if a JAXBManager is created with the
 * useCustomJaxbContextFactory flag set to true. 
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 12, 2011            mschenke     Initial creation
 * June 24, 2013 #2126     bkowal       Update for Java 7 compatibility
 * Oct 25, 2024  2037223   aford        Updated class javadoc to reflect changes
 *                                      made to how this class is instantiated.
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class SerializationContextFactory {

    @SuppressWarnings("rawtypes")
    public static JAXBContext createContext(Class[] classes, Map props) {
        // Construct delegate implementation
        System.setProperty(JAXBContextImpl.class.getName() + ".fastBoot",
                "true");
        JAXBContextBuilder builder = new JAXBContextBuilder();
        builder.setAnnotationReader(new RuntimeInlineAnnotationReader());
        builder.setClasses(classes);
        builder.setSubclassReplacements(new HashMap<Class, Class>());
        builder.setDefaultNsUri("");
        builder.setTypeRefs(new ArrayList<TypeReference>());
        try {
            // TODO: Can we override/extend some part of the context
            // implementation to allow for ignoring of xsi:type fields that we
            // don't have classes for?
            return new CustomJAXBContext(builder.build());
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

}
