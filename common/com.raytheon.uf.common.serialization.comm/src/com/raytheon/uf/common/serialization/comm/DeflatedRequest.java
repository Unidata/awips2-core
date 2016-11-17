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
package com.raytheon.uf.common.serialization.comm;

import java.io.IOException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

/**
 * Request that wraps a compressed request. Useful for sending very large
 * requests when bandwidth is limited.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2016 5937       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

@DynamicSerialize
public class DeflatedRequest implements IServerRequest {
    private static final int GZIP_BUFFER_SIZE = 4096;

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(DeflatedRequest.class);

    /** The compressed IServerRequest */
    @DynamicSerializeElement
    private byte[] compressedData;

    /**
     * Note that this is an expensive operation as it will serialize and
     * compress the specified IServerRequest object.
     *
     * @param request
     * @throws SerializationException
     */
    public void setRequest(IServerRequest request)
            throws SerializationException {
        byte[] uncompressedData = DynamicSerializationManager
                .getManager(SerializationType.Thrift).serialize(request);
        try (PooledByteArrayOutputStream buffer = ByteArrayOutputStreamPool
                .getInstance().getStream()) {
            byte[] tempBuffer = new byte[GZIP_BUFFER_SIZE];
            Deflater deflater = new Deflater();
            deflater.setInput(uncompressedData);
            deflater.finish();
            while (!deflater.finished()) {
                int n = deflater.deflate(tempBuffer);
                buffer.write(tempBuffer, 0, n);
            }
            compressedData = buffer.toByteArray();
        } catch (IOException e) {
            statusHandler.handle(Priority.DEBUG, "Exception on stream close",
                    e);
        }
    }

    /**
     * Note that this is an expensive operation as it must decompress and
     * deserialize the IServerRequest object before returning it.
     *
     * @return
     * @throws DataFormatException
     * @throws SerializationException
     */
    public IServerRequest getRequest()
            throws DataFormatException, SerializationException {

        byte[] bytes = null;
        try (PooledByteArrayOutputStream buffer = ByteArrayOutputStreamPool
                .getInstance().getStream()) {
            byte[] tempBuffer = new byte[GZIP_BUFFER_SIZE];
            Inflater inflater = new Inflater();
            inflater.setInput(compressedData);
            while (!inflater.finished()) {
                int n = inflater.inflate(tempBuffer);
                buffer.write(tempBuffer, 0, n);
            }
            bytes = buffer.toByteArray();
        } catch (IOException e) {
            statusHandler.handle(Priority.DEBUG, "Exception on stream close",
                    e);
        }
        return (IServerRequest) DynamicSerializationManager
                .getManager(SerializationType.Thrift).deserialize(bytes);
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public void setCompressedData(byte[] compressedData) {
        this.compressedData = compressedData;
    }
}
