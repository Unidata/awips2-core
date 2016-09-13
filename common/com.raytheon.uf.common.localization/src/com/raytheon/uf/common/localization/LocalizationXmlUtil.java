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
package com.raytheon.uf.common.localization;

import java.io.IOException;
import java.io.InputStream;

import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * Utility methods for dealing with ILocalizationFiles where the content is XML.
 * 
 * These methods were originally extracted from LocalizationFile.
 * 
 * @deprecated Please handle the marshalling/unmarshalling in your own code.
 *             This doesn't do that much and hides the underlying exceptions
 *             from you.
 * 
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 18, 2015 3806       njensen     Initial creation
 * Dec 03, 2015 4834       njensen     Use getPath() instead of getName()
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

@Deprecated
public class LocalizationXmlUtil {

    private LocalizationXmlUtil() {
        // don't allow construction
    }

    /**
     * Marshal the specified object into the file.
     * 
     * @param file
     *            the file to marshal to
     * @param obj
     *            the object to marshal
     * @param jaxbManager
     *            the jaxbManager
     * @throws LocalizationException
     */
    public static void jaxbMarshal(ILocalizationFile file, Object obj,
            JAXBManager jaxbManager) throws LocalizationException {
        try (SaveableOutputStream sos = file.openOutputStream()) {
            jaxbManager.marshalToStream(obj, sos);
            sos.save();
        } catch (IOException | SerializationException e) {
            throw new LocalizationException(
                    "Unable to marshal the object to the file "
                            + file.getPath(), e);
        }
    }

    /**
     * Returns the object version of this jaxb serialized file. Returns null if
     * the file does not exist or is empty.
     * 
     * @param file
     * @param resultClass
     * @param manager
     * @return the object representation umarshaled from this file
     * @throws LocalizationException
     */
    public static <T> T jaxbUnmarshal(ILocalizationFile file,
            Class<T> resultClass, JAXBManager manager)
            throws LocalizationException {
        if (file.exists()) {
            try (InputStream is = file.openInputStream()) {
                T object = resultClass.cast(manager
                        .unmarshalFromInputStream(is));
                return object;
            } catch (Exception e) {
                throw new LocalizationException("Could not unmarshal file "
                        + file.getPath(), e);
            }
        }

        return null;
    }

}
