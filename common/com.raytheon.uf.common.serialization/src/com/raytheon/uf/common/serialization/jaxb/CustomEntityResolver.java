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

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.ext.EntityResolver2;

/**
 * An entity resolver that always resolves all external entities to an empty
 * InputSource. This Prevents external entities such as
 * {@code <!ENTITY xxe SYSTEM "file:///dev/random">} but not
 * exponential/quadratic expansion attacks like the Billion Laughs
 * (http://en.wikipedia.org/wiki/Billion_laughs), though that can be limited by
 * setting {@code -DentityExpansionLimit=N}. The JDK defaults to 64000 if this
 * isn't set.
 *
 * <br/>
 * <br/>
 *
 * A non-null InputSource is always returned so the parser does not attempt to
 * resolve the entities using the default behavior.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 27, 2015 4496       nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 * @version 1.0
 */
public class CustomEntityResolver implements EntityResolver2 {

    private static final byte[] EMPTY = new byte[0];

    /**
     * Constructor.
     */
    public CustomEntityResolver() {
        super();
    }

    /**
     * Returns a non-null, empty InputSource.
     *
     * @return a non-null, empty InputSource.
     */
    @Override
    public InputSource getExternalSubset(String name, String baseURI)
            throws SAXException, IOException {
        return new InputSource(new ByteArrayInputStream(EMPTY));
    }

    /**
     * Returns a non-null, empty InputSource.
     *
     * @return a non-null, empty InputSource.
     */
    @Override
    public InputSource resolveEntity(String name, String publicId,
            String baseURI, String systemId) throws SAXException, IOException {
        return new InputSource(new ByteArrayInputStream(EMPTY));
    }

    /**
     * Returns a non-null, empty InputSource.
     *
     * @return a non-null, empty InputSource.
     */
    @Override
    public InputSource resolveEntity(String publicId, String systemId)
            throws SAXException, IOException {
        return new InputSource(new ByteArrayInputStream(EMPTY));
    }
}
