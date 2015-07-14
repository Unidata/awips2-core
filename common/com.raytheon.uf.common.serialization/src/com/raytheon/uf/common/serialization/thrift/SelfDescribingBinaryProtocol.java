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
import org.apache.thrift.protocol.TStruct;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TTransport;

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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Aug 07, 2008           chammack  Initial creation
 * Jun 17, 2010  5091     njensen   Added primitive list methods
 * Jun 12, 2013  2102     njensen   Added max read length to prevent out of
 *                                  memory errors due to bad stream
 * Jul 23, 2013  2215     njensen   Updated for thrift 0.9.0
 * Aug 06, 2013  2228     njensen   Overrode readBinary() to ensure it doesn't
 *                                  read too much
 * Jul 13, 2015  4589     bsteffen  Copy arrays in chunks to save memory.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class SelfDescribingBinaryProtocol extends TBinaryProtocol {

    /**
     * The size(in bytes) of the intermediate buffer to use when converting
     * primitive arrays to bytes.
     * 
     * When copying a primitive array to a {@link TTransport} each item in the
     * array must be converted to bytes. There are three basic approaches we
     * have used to do this conversion:
     * <p>
     * <ol>
     * <li><b>Naive Loop</b> This is simply looping over each value, converting
     * to bytes, and writing those bytes to the transport. When the code is run
     * with JIT compilation disabled this is actually the fastest. In the real
     * world, with JIT compilation enabled this method is the slowest. One
     * possible explanation is that the JIT compiler cannot do much optimization
     * within the loop because the transport is an interface so the JIT compiler
     * cannot inline the method call efficiently.
     * <li><b>Copy It All</b> This approach uses
     * ByteBuffer.as&lt;Type&gt;Buffer().put(array) to copy all the data into a
     * single byte[] and then passes this array into the transport. This method
     * is much faster than the Naive Loop, presumably because the JIT can
     * optimize the Buffer operation and because most streams can work more
     * efficiently with larger arrays, this is especially true if the underlying
     * transport uses System.arrayCopy, which works faster on large arrays. The
     * down side to this approach is that it requires double the memory because
     * it copies the whole thing.
     * <li><b>Copying Chunks</b> This approach uses ByteBuffers to copy the
     * data, similar to copying all the data, but the data is copied in many
     * fixed sized chunks. This approach significantly reduces the memory
     * necessary and in most cases it has been observed that this is actually
     * faster than copying all of the data. One possible explanation for why it
     * is faster is because the JVM takes longer to do large allocations so the
     * smaller allocation size of the chunk speeds things up more than the slow
     * down from multiple writes into the transport. The current chunk size was
     * chosen after sampling a variety of sizes. In general smaller sizes tend
     * to slow down and larger sizes offer only negligible benefit.
     * </ol>
     * <p>
     * All comments about performance and JIT are based off observations about
     * the Oracle JDK for java 1.7 running on a 64-bit x86 linux machine. In
     * general the buffering approaches performed as much as 3x faster than the
     * naive approach. Chunking offered performance gains as high as 20% over
     * copying the entire array, although the major selling point is still the
     * memory savings.
     */
    private static int ARRAY_CHUNK_SIZE = 1024;

    public static final byte FLOAT = 64;

    /**
     * This is to ensure a safety check because if the stream has bad bytes at
     * the start, thrift may try to allocate something huge, such as GBs of
     * data, and then the JVM will blow up about OutOfMemory errors.
     **/
    private static int MAX_READ_LENGTH;

    static {
        try {
            int sizeInMB = Integer.parseInt(System
                    .getProperty("thrift.stream.maxsize"));
            MAX_READ_LENGTH = sizeInMB * 1024 * 1024;
        } catch (Throwable t) {
            System.err
                    .println("Error reading property thrift.stream.maxsize - falling back to default of 200 MB");
            t.printStackTrace();
            MAX_READ_LENGTH = 200 * 1024 * 1024;
        }
    }

    public SelfDescribingBinaryProtocol(TTransport trans) {
        this(trans, false, true);
    }

    public SelfDescribingBinaryProtocol(TTransport trans, boolean strictRead,
            boolean strictWrite) {
        super(trans, strictRead, strictWrite);
        this.setReadLength(MAX_READ_LENGTH);
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        int size = readI32();
        checkReadLength(size);
        byte[] buf = new byte[size];
        trans_.readAll(buf, 0, size);
        return ByteBuffer.wrap(buf);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.facebook.thrift.protocol.TBinaryProtocol#readFieldBegin()
     */
    @Override
    public TField readFieldBegin() throws TException {
        // This method was overriden to make the structs more self describing
        Byte type = readByte();
        String name = "";
        short id = (short) 0;
        if (type != TType.STOP) {
            name = readString();
            id = readI16();
        }
        return new TField(name, type, id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.facebook.thrift.protocol.TBinaryProtocol#writeFieldBegin(com.facebook
     * .thrift.protocol.TField)
     */
    @Override
    public void writeFieldBegin(TField field) throws TException {
        // This method was overriden to make the structs more self describing
        writeByte(field.type);
        writeString(field.name);
        writeI16(field.id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.facebook.thrift.protocol.TBinaryProtocol#readStructBegin()
     */
    @Override
    public TStruct readStructBegin() {
        // This method was overriden to make the structs more self describing
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.facebook.thrift.protocol.TBinaryProtocol#writeStructBegin(com.facebook
     * .thrift.protocol.TStruct)
     */
    @Override
    public void writeStructBegin(TStruct struct) {
        // This method was overriden to make the structs more self describing
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
     * Reads a set number of bytes into a buffer
     * 
     * @param length
     * @return
     * @throws TException
     */
    private ByteBuffer readBytes(int length) throws TException {
        byte[] b = new byte[length];
        int n = this.trans_.readAll(b, 0, length);
        if (n != length) {
            throw new TException("Bytes read does not match indicated size");
        }
        ByteBuffer buf = ByteBuffer.wrap(b);
        return buf;
    }

    /**
     * Read a list of floats
     * 
     * @param sz
     * @return data as floats
     * @throws TException
     */
    public float[] readF32List(int sz) throws TException {
        ByteBuffer buf = readBytes(sz * 4);
        FloatBuffer fbuf = buf.asFloatBuffer();
        float[] f = new float[sz];
        fbuf.get(f);
        return f;
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
        ByteBuffer buf = readBytes(sz * 4);
        IntBuffer ibuf = buf.asIntBuffer();
        int[] i = new int[sz];
        ibuf.get(i);
        return i;
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
                ints.put(arr, i, intChunkSize );
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
        ByteBuffer buf = readBytes(sz * 8);
        DoubleBuffer pbuf = buf.asDoubleBuffer();
        double[] arr = new double[sz];
        pbuf.get(arr);
        return arr;
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
        ByteBuffer buf = readBytes(sz * 8);
        LongBuffer pbuf = buf.asLongBuffer();
        long[] arr = new long[sz];
        pbuf.get(arr);
        return arr;
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
        ByteBuffer buf = readBytes(sz * 2);
        ShortBuffer pbuf = buf.asShortBuffer();
        short[] arr = new short[sz];
        pbuf.get(arr);
        return arr;
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
        ByteBuffer buf = readBytes(sz);
        return buf.array();
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

}
