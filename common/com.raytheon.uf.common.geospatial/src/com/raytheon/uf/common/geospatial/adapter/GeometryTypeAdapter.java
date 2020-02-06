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
package com.raytheon.uf.common.geospatial.adapter;

import java.io.IOException;

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.OutputStreamOutStream;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

/**
 * Serializes a geometry in a binary format
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Aug 11, 2008             chammack    Initial creation
 * Aug 08, 2014  3503       bclement    moved from common.serialization to common.geospatial
 * Jun 17, 2015  4561       njensen     Use ByteArrayOutputStreamPool for efficiency
 * Oct 30, 2015  4710       bclement    ByteArrayOutputStream renamed to PooledByteArrayOutputStream
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
public class GeometryTypeAdapter implements ISerializationTypeAdapter<Geometry> {

    @Override
    public Geometry deserialize(IDeserializationContext serializer)
            throws SerializationException {

        byte[] data = serializer.readBinary();
        if (data.length == 0) {
            return null;
        }

        try {
            WKBReader parser = new WKBReader();
            Geometry geom = parser.read(data);
            return geom;
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.out.println("Bad data: " + data);
            return null;
        } catch (ParseException e) {
            System.out.println("Bad data, unable to parse: " + data);
            return null;
        }
    }

    @Override
    public void serialize(ISerializationContext serializer, Geometry object)
            throws SerializationException {
        byte[] data = null;
        if (object == null) {
            data = new byte[0];
        } else {
            WKBWriter writer = new WKBWriter();
            /*
             * We use one of our pooled streams because the default behavior of
             * WKBWriter is to inefficiently start with a byte[32] and then grow
             * it repeatedly as needed.
             */
            try (PooledByteArrayOutputStream baos = ByteArrayOutputStreamPool
                    .getInstance().getStream()) {
                // seriously JTS, you couldn't use java.io.OutputStream?
                OutputStreamOutStream out = new OutputStreamOutStream(baos);
                writer.write(object, out);
                data = baos.toByteArray();
            } catch (IOException e) {
                throw new SerializationException(
                        "Error writing geometry to WKB format", e);
            }
        }

        serializer.writeBinary(data);
    }

}
