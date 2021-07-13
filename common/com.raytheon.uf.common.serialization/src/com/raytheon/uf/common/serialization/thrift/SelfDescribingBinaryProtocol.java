package com.raytheon.uf.common.serialization.thrift;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TField;
import org.apache.thrift.protocol.TProtocolException;
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 * Implement serialization using a self-describing version of the Thrift binary
 * protocol. <BR>
 * <BR>
 * <B>Differences from standard thrift:</B>
 * <UL>
 * <LI>Structs have their types (class names) encoded in the header
 * <LI>Fields have their names encoded in the header
 * <LI>float types are supported
 * </UL>
 *
 *
 * <BR>
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 07, 2008           chammack  Initial creation
 * Jun 17, 2010  5091     njensen   Added primitive list methods
 * Jun 12, 2013  2102     njensen   Added max read length to prevent out of
 *                                  memory errors due to bad stream
 * Jul 23, 2013  2215     njensen   Updated for thrift 0.9.0
 * Aug 06, 2013  2228     njensen   Overrode readBinary() to ensure it doesn't
 *                                  read too much
 * Jul 13, 2015  4589     bsteffen  Copy arrays in chunks to save memory.
 * Mar 08, 2017  6167     nabowle   Updated for thrift 0.10.0
 * Jun 28, 2021  8470     lsingh    Updated for thrift 0.14.1, overrode
 *                                  getMinSerializedSize() to add support for
 *                                  FLOAT.
 *
 * </pre>
 *
 * @author chammack
 */

public class SelfDescribingBinaryProtocol extends TBinaryProtocol {

    /**
     * The size(in bytes) of the intermediate buffer to use when converting
     * primitive arrays to or from bytes.
     * <p>
     * To interact with a {@link TTransport} arrays of primitives must be
     * represented as byte arrays. It is possible to convert each primitive
     * object individually or to use buffers to convert all the data at once,
     * but the best performance has been observed when large arrays are copied
     * in fixed sized chunks. In testing a chunk size of 1024 bytes yielded the
     * best performance.
     */
    private static int ARRAY_CHUNK_SIZE = 1024;

    public static final byte FLOAT = 64;

    protected static final Logger log = LoggerFactory
            .getLogger(SelfDescribingBinaryProtocol.class);

    /**
     * This is to ensure a safety check because if the stream has bad bytes at
     * the start, thrift may try to allocate something huge, such as GBs of
     * data, and then the JVM will blow up about OutOfMemory errors.
     **/
    private static int MAX_READ_LENGTH;

    static {
        try {
            int sizeInMB = Integer
                    .parseInt(System.getProperty("thrift.stream.maxsize"));
            MAX_READ_LENGTH = sizeInMB * 1024 * 1024;
        } catch (Throwable t) {
            log.error(
                    "Error reading property thrift.stream.maxsize - falling back to default of 200 MB",
                    t);
            MAX_READ_LENGTH = 200 * 1024 * 1024;
        }
    }

    public SelfDescribingBinaryProtocol(TTransport trans) {
        this(trans, false, true);
    }

    public SelfDescribingBinaryProtocol(TTransport trans, boolean strictRead,
            boolean strictWrite) {
        super(trans, MAX_READ_LENGTH, MAX_READ_LENGTH, strictRead, strictWrite);
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        int size = readI32();
        checkReadLength(size);
        byte[] buf = new byte[size];
        trans_.readAll(buf, 0, size);
        return ByteBuffer.wrap(buf);
    }

    @Override
    public TField readFieldBegin() throws TException {
        // This method was overridden to make the structs more self describing
        Byte type = readByte();
        String name = "";
        short id = (short) 0;
        if (type != TType.STOP) {
            name = readString();
            id = readI16();
        }
        return new TField(name, type, id);
    }

    @Override
    public void writeFieldBegin(TField field) throws TException {
        // This method was overridden to make the structs more self describing
        writeByte(field.type);
        writeString(field.name);
        writeI16(field.id);
    }

    @Override
    public TStruct readStructBegin() {
        // This method was overridden to make the structs more self describing
        String name;
        try {
            name = readString();
        } catch (TException e) {
            // TODO: unfortunately incompatible signatures prevent this from
            // being thrown up as a TException
            throw new RuntimeException(e);
        }
        return new TStruct(name);
    }

    @Override
    public void writeStructBegin(TStruct struct) {
        // This method was overridden to make the structs more self describing
        try {
            writeString(struct.name);
        } catch (TException e) {
            // TODO: unfortunately incompatible signatures prevent this from
            // being thrown up as a TException
            throw new RuntimeException(e);
        }
    }

    /**
     * Write a float
     *
     * @param flt
     * @throws TException
     */
    public void writeFloat(float flt) throws TException {
        writeI32(Float.floatToIntBits(flt));
    }

    /**
     * Read a float
     *
     * @return float
     * @throws TException
     */
    public float readFloat() throws TException {
        return Float.intBitsToFloat(readI32());
    }

    /**
     * Read a list of floats
     *
     * @param sz
     * @return data as floats
     * @throws TException
     */
    public float[] readF32List(int sz) throws TException {
        FloatBuffer result = FloatBuffer.allocate(sz);
        int arrByteLength = sz * 4;
        int bufferSize = Math.min(ARRAY_CHUNK_SIZE, arrByteLength);
        byte[] buffer = new byte[bufferSize];
        FloatBuffer floatBuffer = ByteBuffer.wrap(buffer).asFloatBuffer();
        int offset = 0;
        int len = bufferSize;
        while (result.remaining() > 0) {
            int bytesRead = offset + this.trans_.read(buffer, offset, len);
            int floatsRead = bytesRead / 4;
            if (floatsRead > 0) {
                floatBuffer.limit(floatsRead);
                result.put(floatBuffer);
                floatBuffer.rewind();
                offset = bytesRead % 4;
                if (offset > 0) {
                    System.arraycopy(buffer, floatsRead * 4, buffer, 0, offset);
                }
            } else if (bytesRead <= offset) {
                throw new TException("Failed to read any data.");
            } else {
                offset = bytesRead;
            }
            len = Math.min(bufferSize, result.remaining() * 4) - offset;
        }
        return result.array();
    }

    /**
     * Write a list of floats
     *
     * @param arr
     * @throws TException
     */
    public void writeF32List(float[] arr) throws TException {
        int arrLength = arr.length;
        int arrByteLength = arrLength * 4;
        if (ARRAY_CHUNK_SIZE > arrByteLength) {
            byte[] bytes = new byte[arrByteLength];
            ByteBuffer.wrap(bytes).asFloatBuffer().put(arr);
            this.trans_.write(bytes);
        } else {
            int floatChunkSize = ARRAY_CHUNK_SIZE / 4;
            int remainder = arrLength % floatChunkSize;
            int fullChunkSize = arrLength - remainder;

            byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
            FloatBuffer floats = ByteBuffer.wrap(bytes).asFloatBuffer();
            for (int i = 0; i < fullChunkSize; i += floatChunkSize) {
                floats.put(arr, i, floatChunkSize);
                trans_.write(bytes, 0, ARRAY_CHUNK_SIZE);
                floats.rewind();
            }
            if (remainder > 0) {
                floats.put(arr, fullChunkSize, remainder);
                trans_.write(bytes, 0, remainder * 4);
            }
        }
    }

    /**
     * Read a list of ints
     *
     * @param sz
     * @return data as ints
     * @throws TException
     */
    public int[] readI32List(int sz) throws TException {
        IntBuffer result = IntBuffer.allocate(sz);
        int arrByteLength = sz * 4;
        int bufferSize = Math.min(ARRAY_CHUNK_SIZE, arrByteLength);
        byte[] buffer = new byte[bufferSize];
        IntBuffer intBuffer = ByteBuffer.wrap(buffer).asIntBuffer();
        int offset = 0;
        int len = bufferSize;
        while (result.remaining() > 0) {
            int bytesRead = offset + this.trans_.read(buffer, offset, len);
            int intsRead = bytesRead / 4;
            if (intsRead > 0) {
                intBuffer.limit(intsRead);
                result.put(intBuffer);
                intBuffer.rewind();
                offset = bytesRead % 4;
                if (offset > 0) {
                    System.arraycopy(buffer, intsRead * 4, buffer, 0, offset);
                }
            } else if (bytesRead <= offset) {
                throw new TException("Failed to read any data.");
            } else {
                offset = bytesRead;
            }
            len = Math.min(bufferSize, result.remaining() * 4) - offset;
        }
        return result.array();
    }

    /**
     * Write a list of ints
     *
     * @param arr
     * @throws TException
     */
    public void writeI32List(int[] arr) throws TException {
        int arrLength = arr.length;
        int arrByteLength = arrLength * 4;
        if (ARRAY_CHUNK_SIZE > arrByteLength) {
            byte[] bytes = new byte[arrByteLength];
            ByteBuffer.wrap(bytes).asIntBuffer().put(arr);
            this.trans_.write(bytes);
        } else {
            int intChunkSize = ARRAY_CHUNK_SIZE / 4;
            int remainder = arrLength % intChunkSize;
            int fullChunkSize = arrLength - remainder;

            byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
            IntBuffer ints = ByteBuffer.wrap(bytes).asIntBuffer();
            for (int i = 0; i < fullChunkSize; i += intChunkSize) {
                ints.put(arr, i, intChunkSize);
                trans_.write(bytes, 0, ARRAY_CHUNK_SIZE);
                ints.rewind();
            }
            if (remainder > 0) {
                ints.put(arr, fullChunkSize, remainder);
                trans_.write(bytes, 0, remainder * 4);
            }
        }
    }

    /**
     * Read a list of doubles
     *
     * @param sz
     * @return data as doubles
     * @throws TException
     */
    public double[] readD64List(int sz) throws TException {
        DoubleBuffer result = DoubleBuffer.allocate(sz);
        int arrByteLength = sz * 8;
        int bufferSize = Math.min(ARRAY_CHUNK_SIZE, arrByteLength);
        byte[] buffer = new byte[bufferSize];
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(buffer).asDoubleBuffer();
        int offset = 0;
        int len = bufferSize;
        while (result.remaining() > 0) {
            int bytesRead = offset + this.trans_.read(buffer, offset, len);
            int doublesRead = bytesRead / 8;
            if (doublesRead > 0) {
                doubleBuffer.limit(doublesRead);
                result.put(doubleBuffer);
                doubleBuffer.rewind();
                offset = bytesRead % 8;
                if (offset > 0) {
                    System.arraycopy(buffer, doublesRead * 8, buffer, 0,
                            offset);
                }
            } else if (bytesRead <= offset) {
                throw new TException("Failed to read any data.");
            } else {
                offset = bytesRead;
            }
            len = Math.min(bufferSize, result.remaining() * 8) - offset;
        }
        return result.array();
    }

    /**
     * Write a list of doubles
     *
     * @param arr
     * @throws TException
     */
    public void writeD64List(double[] arr) throws TException {
        int arrLength = arr.length;
        int arrByteLength = arrLength * 8;
        if (ARRAY_CHUNK_SIZE > arrByteLength) {
            byte[] bytes = new byte[arrByteLength];
            ByteBuffer.wrap(bytes).asDoubleBuffer().put(arr);
            this.trans_.write(bytes);
        } else {
            int doubleChunkSize = ARRAY_CHUNK_SIZE / 8;
            int remainder = arrLength % doubleChunkSize;
            int fullChunkSize = arrLength - remainder;

            byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
            DoubleBuffer doubles = ByteBuffer.wrap(bytes).asDoubleBuffer();
            for (int i = 0; i < fullChunkSize; i += doubleChunkSize) {
                doubles.put(arr, i, doubleChunkSize);
                trans_.write(bytes, 0, ARRAY_CHUNK_SIZE);
                doubles.rewind();
            }
            if (remainder > 0) {
                doubles.put(arr, fullChunkSize, remainder);
                trans_.write(bytes, 0, remainder * 8);
            }
        }
    }

    /**
     * Read a list of longs
     *
     * @param sz
     * @return data as longs
     * @throws TException
     */
    public long[] readI64List(int sz) throws TException {
        LongBuffer result = LongBuffer.allocate(sz);
        int arrByteLength = sz * 8;
        int bufferSize = Math.min(ARRAY_CHUNK_SIZE, arrByteLength);
        byte[] buffer = new byte[bufferSize];
        LongBuffer longBuffer = ByteBuffer.wrap(buffer).asLongBuffer();
        int offset = 0;
        int len = bufferSize;
        while (result.remaining() > 0) {
            int bytesRead = offset + this.trans_.read(buffer, offset, len);
            int longsRead = bytesRead / 8;
            if (longsRead > 0) {
                longBuffer.limit(longsRead);
                result.put(longBuffer);
                longBuffer.rewind();
                offset = bytesRead % 8;
                if (offset > 0) {
                    System.arraycopy(buffer, longsRead * 8, buffer, 0, offset);
                }
            } else if (bytesRead <= offset) {
                throw new TException("Failed to read any data.");
            } else {
                offset = bytesRead;
            }
            len = Math.min(bufferSize, result.remaining() * 8) - offset;
        }
        return result.array();
    }

    /**
     * Write a list of longs
     *
     * @param arr
     * @throws TException
     */
    public void writeI64List(long[] arr) throws TException {
        int arrLength = arr.length;
        int arrByteLength = arrLength * 8;
        if (ARRAY_CHUNK_SIZE > arrByteLength) {
            byte[] bytes = new byte[arrByteLength];
            ByteBuffer.wrap(bytes).asLongBuffer().put(arr);
            this.trans_.write(bytes);
        } else {
            int longChunkSize = ARRAY_CHUNK_SIZE / 8;
            int remainder = arrLength % longChunkSize;
            int fullChunkSize = arrLength - remainder;

            byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
            LongBuffer longs = ByteBuffer.wrap(bytes).asLongBuffer();
            for (int i = 0; i < fullChunkSize; i += longChunkSize) {
                longs.put(arr, i, longChunkSize);
                trans_.write(bytes, 0, ARRAY_CHUNK_SIZE);
                longs.rewind();
            }
            if (remainder > 0) {
                longs.put(arr, fullChunkSize, remainder);
                trans_.write(bytes, 0, remainder * 8);
            }
        }
    }

    /**
     * Read a list of shorts
     *
     * @param sz
     * @return data as shorts
     * @throws TException
     */
    public short[] readI16List(int sz) throws TException {
        ShortBuffer result = ShortBuffer.allocate(sz);
        int arrByteLength = sz * 2;
        int bufferSize = Math.min(ARRAY_CHUNK_SIZE, arrByteLength);
        byte[] buffer = new byte[bufferSize];
        ShortBuffer shortBuffer = ByteBuffer.wrap(buffer).asShortBuffer();
        int offset = 0;
        int len = bufferSize;
        while (result.remaining() > 0) {
            int bytesRead = offset + this.trans_.read(buffer, offset, len);
            int shortsRead = bytesRead / 2;
            if (shortsRead > 0) {
                shortBuffer.limit(shortsRead);
                result.put(shortBuffer);
                shortBuffer.rewind();
                offset = bytesRead % 2;
                if (offset > 0) {
                    System.arraycopy(buffer, shortsRead * 2, buffer, 0, offset);
                }
            } else if (bytesRead <= offset) {
                throw new TException("Failed to read any data.");
            } else {
                offset = bytesRead;
            }
            len = Math.min(bufferSize, result.remaining() * 2) - offset;
        }
        return result.array();
    }

    /**
     * Write a list of doubles
     *
     * @param arr
     * @throws TException
     */
    public void writeI16List(short[] arr) throws TException {
        int arrLength = arr.length;
        int arrByteLength = arrLength * 2;
        if (ARRAY_CHUNK_SIZE > arrByteLength) {
            byte[] bytes = new byte[arrByteLength];
            ByteBuffer.wrap(bytes).asShortBuffer().put(arr);
            this.trans_.write(bytes);
        } else {
            int shortChunkSize = ARRAY_CHUNK_SIZE / 2;
            int remainder = arrLength % shortChunkSize;
            int fullChunkSize = arrLength - remainder;

            byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
            ShortBuffer shorts = ByteBuffer.wrap(bytes).asShortBuffer();
            for (int i = 0; i < fullChunkSize; i += shortChunkSize) {
                shorts.put(arr, i, shortChunkSize);
                trans_.write(bytes, 0, ARRAY_CHUNK_SIZE);
                shorts.rewind();
            }
            if (remainder > 0) {
                shorts.put(arr, fullChunkSize, remainder);
                trans_.write(bytes, 0, remainder * 2);
            }
        }
    }

    /**
     * Read a list of bytes
     *
     * @param sz
     * @return data as bytes
     * @throws TException
     */
    public byte[] readI8List(int sz) throws TException {
        byte[] b = new byte[sz];
        int n = this.trans_.readAll(b, 0, sz);
        if (n != sz) {
            throw new TException("Bytes read does not match indicated size");
        }
        return b;
    }

    /**
     * Write a list of bytes
     *
     * @param arr
     * @throws TException
     */
    public void writeI8List(byte[] arr) throws TException {
        this.trans_.write(arr);
    }

    /**
     * Verifies that the given length is non-negative and less than
     * {@link #MAX_READ_LENGTH}.
     *
     * @param length
     *            The incoming length.
     * @throws TException
     *             if the length is negative or exceeds {@link #MAX_READ_LENGTH}
     */
    private void checkReadLength(int length) throws TException {
        if (length < 0) {
            throw new TProtocolException(TProtocolException.NEGATIVE_SIZE,
                    "Negative length: " + length);
        }

        if (length > MAX_READ_LENGTH) {
            throw new TProtocolException(TProtocolException.SIZE_LIMIT,
                    "Incoming length " + length + " exceeds limit of "
                            + MAX_READ_LENGTH);
        }
    }

    @Override
    public int getMinSerializedSize(byte type) throws TTransportException {
        switch (type) {
        // AWIPS FLOAT
        case FLOAT:
            return 4; 
        default:
            return super.getMinSerializedSize(type);
        }
    }

}
