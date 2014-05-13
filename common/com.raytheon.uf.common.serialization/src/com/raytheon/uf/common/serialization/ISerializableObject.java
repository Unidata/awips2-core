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

import javax.xml.bind.JAXB;

/**
 * DEPRECATED! Former usage (no longer applies):
 * 
 * Empty interface that should be implemented by any class that uses Hibernate,
 * JaxB, or DynamicSerialize annotations so it is detected at runtime.
 * 
 * Implementing this interface in conjunction with adding the class to the
 * com.raytheon.uf.common.serialization.ISerializableObject file in the
 * META-INF/services directory will ensure it is detected at runtime.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Aug 11, 2008             njensen     Initial creation
 * Oct 02, 2013 2361        njensen     Deprecated
 * May 13, 2014 3165        njensen     Updated javadoc
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 * @deprecated This interface is deprecated but may still be required until it
 *             is completely removed from the system. DynamicSerialize no longer
 *             requires ISerializableObjects, just use the DynamicSerialize
 *             annotations. JAXB/XML only requires it if you use the global JAXB
 *             context available from {@link SerializationUtil}, however that is
 *             a performance hit and deprecated and you should instead create
 *             your own {@link JAXBManager} for thread safe operations against a
 *             smaller set of classes. If working with a single type on one
 *             thread, you can use {@link JAXB} directly. Hibernate no longer
 *             uses ISerializableObject, EDEX will automatically detect classes
 *             with javax.persistence.Entity or javax.persistence.Embeddable
 *             annotations that exist in jars in the edex/lib/plugins directory.
 *             See DatabaseClassAnnotationFinder for more details.
 * 
 */

@Deprecated
public interface ISerializableObject {

}
