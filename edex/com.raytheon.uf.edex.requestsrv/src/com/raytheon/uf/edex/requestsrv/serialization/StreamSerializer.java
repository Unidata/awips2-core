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
package com.raytheon.uf.edex.requestsrv.serialization;

import java.io.InputStream;
import java.io.OutputStream;

import com.raytheon.uf.common.serialization.SerializationException;

/**
 * Interface for stream-based serialization for the request service. Register
 * implementations with the {@link SerializingStreamExecutor}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public interface StreamSerializer {

    /**
     * Serializes the object passed in to the output stream. Stream should not
     * be closed by serializers since it was not opened by them
     * 
     * @param object
     * @param out
     * @throws SerializationException
     */
    public void serialize(Object object, OutputStream out)
            throws SerializationException;

    /**
     * Deserializes the stream passed in. Stream should not be closed by
     * serializers since it was not opened by them
     * 
     * @param in
     * @return
     * @throws SerializationException
     */
    public Object deserialize(InputStream in) throws SerializationException;

}
