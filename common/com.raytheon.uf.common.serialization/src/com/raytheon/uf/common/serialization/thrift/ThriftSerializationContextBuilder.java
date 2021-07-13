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
package com.raytheon.uf.common.serialization.thrift;

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.thrift.TConfiguration;
import org.apache.thrift.transport.TIOStreamTransport;
import org.apache.thrift.transport.TMemoryInputTransport;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationContextBuilder;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * Build a Thrift Serialization context
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Aug 12, 2008             chammack    Initial creation
 * Jul 23, 2013  2215       njensen     Updated for thrift 0.9.0
 * Aug 06, 2013  2228       njensen     Added buildDeserializationContext(byte[], dsm)
 * May 27, 2021  8470       lsingh      Upgraded to Thrift 0.14.1. Added
 *                                      exception handling and TConfiguration support.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class ThriftSerializationContextBuilder
        implements ISerializationContextBuilder {

    protected static final Logger log = LoggerFactory
            .getLogger(ThriftSerializationContextBuilder.class);

    /**
     * Thrift Configuration. This needs to be passed into all Thrift transport
     * objects.
     */
    private static final TConfiguration config;

    /**
     * Set up the Thrift configuration. Get the maxMessageSize, maxFrameDepth
     * and recursionDepth.
     */
    static {
        int maxMessageSize = 0;
        int maxFrameSize = 0;
        int recursionDepth = 0;

        String maxMessageSizeStr = System.getenv("THRIFT_MAX_MESSAGE_SIZE");
        String maxFrameSizeStr = System.getenv("THRIFT_MAX_FRAME_SIZE");
        String recursionDepthStr = System.getenv("THRIFT_RECURSION_DEPTH");

        // Parse MAX_MESSAGE_SIZE
        if (maxMessageSizeStr == null) {
            log.warn("THRIFT_MAX_MESSAGE_SIZE environmental variable not set.");
        } else {
            try {
                maxMessageSize = Integer.parseInt(maxMessageSizeStr);
            } catch (NumberFormatException e) {
                log.warn(
                        "Could not parse value from THRIFT_MAX_MESSAGE_SIZE environmental variable. Value received was "
                                + maxMessageSizeStr + ". "
                                + "Setting THRIFT_MAX_MESSAGE_SIZE to the default value of "
                                + TConfiguration.DEFAULT_MAX_MESSAGE_SIZE
                                + ".");
                maxMessageSize = TConfiguration.DEFAULT_MAX_MESSAGE_SIZE;
            }
        }

        if (maxMessageSize <= 0) {

            log.warn(
                    "THRIFT_MAX_MESSAGE_SIZE is set to zero or less. Setting THRIFT_MAX_MESSAGE_SIZE to default value of "
                            + TConfiguration.DEFAULT_MAX_MESSAGE_SIZE
                            + " bytes.");
            maxMessageSize = TConfiguration.DEFAULT_MAX_MESSAGE_SIZE;
        }
        log.info("THRIFT_MAX_MESSAGE_SIZE is now set to " + maxMessageSize
                + " bytes");

        // Parse MAX_FRAME_SIZE
        if (maxFrameSizeStr == null) {
            log.warn("THRIFT_MAX_FRAME_SIZE environmental variable not set.");
        } else {
            try {
                maxFrameSize = Integer.parseInt(maxFrameSizeStr);
            } catch (NumberFormatException e) {
                log.warn(
                        "Could not parse value from THRIFT_MAX_FRAME_SIZE environmental variable. Value received was "
                                + maxFrameSizeStr + ". "
                                + "Setting THRIFT_MAX_FRAME_SIZE to the default value of "
                                + TConfiguration.DEFAULT_MAX_FRAME_SIZE + ".");
                maxFrameSize = TConfiguration.DEFAULT_MAX_FRAME_SIZE;
            }
        }

        if (maxFrameSize <= 0) {
            log.warn(
                    "THRIFT_MAX_FRAME_SIZE is set to zero or less. Setting THRIFT_MAX_FRAME_SIZE to default value of "
                            + TConfiguration.DEFAULT_MAX_FRAME_SIZE
                            + " bytes.");
            maxFrameSize = TConfiguration.DEFAULT_MAX_FRAME_SIZE;

        }
        log.info("THRIFT_MAX_FRAME_SIZE is now set to " + maxFrameSize
                + " bytes");

        // Parse RECURSION_DEPTH
        if (recursionDepthStr == null) {
            log.warn("THRIFT_RECURSION_DEPTH environmental variable not set.");
        } else {
            try {
                recursionDepth = Integer.parseInt(recursionDepthStr);
            } catch (NumberFormatException e) {
                log.warn(
                        "Could not parse value from THRIFT_RECURSION_DEPTH environmental variable. Value received was "
                                + recursionDepthStr + ". "
                                + "Setting THRIFT_RECURSION_DEPTH to the default value of "
                                + TConfiguration.DEFAULT_RECURSION_DEPTH + ".");
                recursionDepth = TConfiguration.DEFAULT_RECURSION_DEPTH;
            }
        }

        if (recursionDepth <= 0) {
            log.warn(
                    "THRIFT_RECURSION_DEPTH is set to zero or less. Setting THRIFT_RECURSION_DEPTH to default value of "
                            + TConfiguration.DEFAULT_RECURSION_DEPTH);
            recursionDepth = TConfiguration.DEFAULT_RECURSION_DEPTH;

        }
        log.info("THRIFT_RECURSION_DEPTH is now set to " + recursionDepth);

        config = new TConfiguration(maxMessageSize, maxFrameSize,
                recursionDepth);
    }

    public ThriftSerializationContextBuilder() {

    }

    @Override
    public IDeserializationContext buildDeserializationContext(InputStream data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TIOStreamTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public ISerializationContext buildSerializationContext(OutputStream data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TIOStreamTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

    @Override
    public IDeserializationContext buildDeserializationContext(byte[] data,
            DynamicSerializationManager manager) throws SerializationException {
        try {
            TTransport transport = new TMemoryInputTransport(config, data);
            SelfDescribingBinaryProtocol proto = new SelfDescribingBinaryProtocol(
                    transport);

            return new ThriftSerializationContext(proto, manager);
        } catch (TTransportException e) {
            throw new SerializationException(e.getLocalizedMessage(), e);
        }

    }

}
