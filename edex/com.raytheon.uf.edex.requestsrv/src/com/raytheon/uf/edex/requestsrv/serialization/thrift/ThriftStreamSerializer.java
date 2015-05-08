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
package com.raytheon.uf.edex.requestsrv.serialization.thrift;

import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.edex.requestsrv.serialization.StreamSerializer;

/**
 * {@link StreamSerializer} that handles the custom 'thrift' based serialization
 * format used by {@link DynamicSerializationManager}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * Jan 06, 2015 3789       bclement    added getContentType()
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class ThriftStreamSerializer implements StreamSerializer {

    public static final String CONTENT_TYPE = "application/dynamic-serialize";

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.requestsrv.serialization.StreamSerializer#serialize
     * (java.lang.Object, java.io.OutputStream)
     */
    @Override
    public void serialize(Object object, OutputStream out)
            throws SerializationException {
        DynamicSerializationManager.getManager(SerializationType.Thrift)
                .serialize(object, out);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.requestsrv.serialization.StreamSerializer#deserialize
     * (java.io.InputStream)
     */
    @Override
    public Object deserialize(InputStream in) throws SerializationException {
        return DynamicSerializationManager.getManager(SerializationType.Thrift)
                .deserialize(in);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.requestsrv.serialization.StreamSerializer#getContentType
     * ()
     */
    @Override
    public String getContentType() {
        return CONTENT_TYPE;
    }

}
