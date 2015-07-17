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

import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.auth.AuthException;
import com.raytheon.uf.common.auth.resp.AuthServerErrorResponse;
import com.raytheon.uf.common.serialization.ExceptionWrapper;
import com.raytheon.uf.common.serialization.comm.IServerRequest;
import com.raytheon.uf.common.serialization.comm.response.ServerErrorResponse;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.registry.GenericRegistry;
import com.raytheon.uf.common.util.registry.RegistryException;
import com.raytheon.uf.common.util.stream.CountingOutputStream;
import com.raytheon.uf.edex.requestsrv.RequestServiceExecutor;

/**
 * This executor executes the {@link IServerRequest} deserialized from an
 * {@link InputStream}. The serialization format is injected into this class
 * through the registry. Object response of request is then written to the
 * {@link OutputStream} passed in.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 21, 2014 3541       mschenke    Initial creation
 * Jan 06, 2015 3789       bclement    added getContentType(), execute throws UnsupportedFormatException
 * Jun 17, 2015 4561       njensen     Log serialization exception to two logs
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class SerializingStreamExecutor extends
        GenericRegistry<String, StreamSerializer> implements
        ISerializingStreamExecutor {

    private static final IUFStatusHandler requestLog = UFStatus
            .getNamedHandler("ThriftSrvRequestLogger");

    /** Default instance for convenient sharing of registry. */
    private static final SerializingStreamExecutor instance = new SerializingStreamExecutor(
            RequestServiceExecutor.getInstance());

    public static SerializingStreamExecutor getInstance() {
        return instance;
    }

    private final RequestServiceExecutor executor;

    public SerializingStreamExecutor(RequestServiceExecutor executor) {
        this.executor = executor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.requestsrv.serialization.ISerializingStreamExecutor
     * #execute(java.lang.String, java.io.InputStream, java.lang.String,
     * java.io.OutputStream)
     */
    @Override
    public void execute(String inputFormat, InputStream in,
            String outputFormat, OutputStream out)
            throws UnsupportedFormatException {
        long startTime = System.currentTimeMillis();
        boolean success = false;
        IServerRequest request = null;
        Object response;
        try {
            StreamSerializer inputSerializer = getRegisteredObject(inputFormat);
            if (inputSerializer == null) {
                throw new IllegalArgumentException(
                        "No serializer registered for format: " + inputFormat);
            }

            request = (IServerRequest) inputSerializer.deserialize(in);
            response = executor.execute(request);
            success = true;
        } catch (AuthException e) {
            AuthServerErrorResponse resp = new AuthServerErrorResponse();
            resp.setUpdatedData(e.getUpdatedData());
            resp.setException(ExceptionWrapper.wrapThrowable(e));
            response = resp;
            requestLog.error("Authorization issue with service request", e);
        } catch (Throwable t) {
            ServerErrorResponse resp = new ServerErrorResponse();
            resp.setException(ExceptionWrapper.wrapThrowable(t));
            response = resp;
            requestLog.error("Error executing request", t);
        }

        // Get serializer for output format
        StreamSerializer outputSerializer = getRegisteredObject(outputFormat);
        if (outputSerializer == null) {
            String msg = "No serializer registered for format: " + outputFormat;
            requestLog.error("Failed to format response object: " + response
                    + ". " + msg);
            throw new UnsupportedFormatException(msg);
        }
        try {
            // Wrap output stream in counting so we can log size
            CountingOutputStream cout;
            if (out instanceof CountingOutputStream) {
                cout = (CountingOutputStream) out;
            } else {
                cout = new CountingOutputStream(out);
            }

            // Perform serialization to stream
            outputSerializer.serialize(response, cout);

            if (success) {
                // Log response size if request was successful
                long endTime = System.currentTimeMillis();
                StringBuilder sb = new StringBuilder(300);
                sb.append("Handled ").append(request.toString()).append(" in ")
                        .append((endTime - startTime)).append("ms");
                sb.append(", response was size ").append(
                        SizeUtil.prettyByteSize(cout.getBytesWritten()));
                requestLog.info(sb.toString());
            }
        } catch (Throwable t) {
            /*
             * When this happens, the stream is cut off mid-stream, leading to
             * non-helpful error messages on the client. Therefore, we will log
             * this in multiple locations so if someone checks the server logs
             * they will hopefully see the problem.
             */
            LoggerFactory.getLogger("root").error(
                    "Failed to format response object: " + response, t);
            requestLog
                    .error("Failed to format response object: " + response, t);
        }
    }

    public Object registerMultiple(StreamSerializer serializer, String... keys)
            throws RegistryException {
        for (String key : keys) {
            register(key, serializer);
        }
        return new Object();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.util.registry.GenericRegistry#register(java.lang
     * .Object, java.lang.Object)
     */
    @Override
    public Object register(String t, StreamSerializer s)
            throws RegistryException {
        String contentType = s.getContentType();
        if (!t.equals(contentType)) {
            super.register(s.getContentType(), s);
        }
        return super.register(t, s);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.edex.requestsrv.serialization.ISerializingStreamExecutor
     * #getContentType(java.lang.String)
     */
    @Override
    public String getContentType(String format)
            throws UnsupportedFormatException {
        StreamSerializer seralizer = getRegisteredObject(format);
        if (seralizer == null) {
            throw new UnsupportedFormatException(
                    "No serializer registered for format: " + format);
        }
        return seralizer.getContentType();
    }
}
