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
package com.raytheon.uf.common.pypies.records;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;

import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.AbstractStorageRecord;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DoubleDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.datastorage.records.LongDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

/**
 *
 * Record containing gzip compressed version of data. This is intended to reduce
 * the bandwidth usage when communicating with pypies.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------
 * Nov 15, 2016  5992     bsteffen  Initial creation
 * Jun 10, 2021  8450     mapeters  Add serialVersionUID
 *
 * </pre>
 *
 * @author bsteffen
 */
@DynamicSerialize
public class CompressedDataRecord extends AbstractStorageRecord {

    private static final long serialVersionUID = -1535722233997351161L;

    private static final int ARRAY_CHUNK_SIZE = 1024;

    private static final int COMPRESSION_RATIO_ASSUMPTION = 4;

    public enum Type {
        BYTE, SHORT, INT, LONG, FLOAT, DOUBLE;
    }

    @DynamicSerializeElement
    private byte[] compressedData;

    @DynamicSerializeElement
    private Type type;

    @Override
    public boolean validateDataSet() {
        return true;
    }

    @Override
    public void reduce(int[] indices) {
        throw new UnsupportedOperationException(
                "Compressed record cannot be modified.");
    }

    @Override
    public int getSizeInBytes() {
        return compressedData.length;
    }

    @Override
    public Object getDataObject() {
        return compressedData;
    }

    public byte[] getCompressedData() {
        return compressedData;
    }

    public void setCompressedData(byte[] compressedData) {
        this.compressedData = compressedData;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    protected CompressedDataRecord cloneInternal() {
        CompressedDataRecord record = new CompressedDataRecord();
        record.type = type;
        if (compressedData != null) {
            record.compressedData = Arrays.copyOf(compressedData,
                    compressedData.length);
        }
        return record;
    }

    /**
     * Convert to a compressed record only if the type of the record supports
     * compression. Otherwise the original record is returned.
     */
    public static IDataRecord convert(IDataRecord sourceRecord)
            throws StorageException {
        try {
            if (sourceRecord instanceof ByteDataRecord) {
                return convertByte((ByteDataRecord) sourceRecord);
            } else if (sourceRecord instanceof ShortDataRecord) {
                return convertShort((ShortDataRecord) sourceRecord);
            } else if (sourceRecord instanceof IntegerDataRecord) {
                return convertInt((IntegerDataRecord) sourceRecord);
            } else if (sourceRecord instanceof LongDataRecord) {
                return convertLong((LongDataRecord) sourceRecord);
            } else if (sourceRecord instanceof FloatDataRecord) {
                return convertFloat((FloatDataRecord) sourceRecord);
            } else if (sourceRecord instanceof DoubleDataRecord) {
                return convertDouble((DoubleDataRecord) sourceRecord);
            }
        } catch (IOException e) {
            throw new StorageException("Error compressing Data", sourceRecord,
                    e);
        }
        return sourceRecord;
    }

    private static CompressedDataRecord cloneMetadata(
            IDataRecord sourceRecord) {
        CompressedDataRecord compressedRecord = new CompressedDataRecord();
        compressedRecord.setName(sourceRecord.getName());
        compressedRecord.setDimension(sourceRecord.getDimension());
        compressedRecord.setSizes(sourceRecord.getSizes());
        compressedRecord.setMaxSizes(sourceRecord.getMaxSizes());
        compressedRecord.setProperties(sourceRecord.getProperties());
        compressedRecord.setMinIndex(sourceRecord.getMinIndex());
        compressedRecord.setGroup(sourceRecord.getGroup());
        compressedRecord.setDataAttributes(sourceRecord.getDataAttributes());
        compressedRecord.setFillValue(sourceRecord.getFillValue());
        compressedRecord.setMaxChunkSize(sourceRecord.getMaxChunkSize());
        compressedRecord
                .setCorrelationObject(sourceRecord.getCorrelationObject());
        return compressedRecord;
    }

    private static CompressedDataRecord convertByte(ByteDataRecord byteRecord)
            throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(byteRecord);

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(byteRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            stream.write(byteRecord.getByteData());
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.BYTE);

        return compressedRecord;
    }

    private static CompressedDataRecord convertShort(
            ShortDataRecord shortRecord) throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(shortRecord);

        short[] shortArr = shortRecord.getShortData();
        int arrLength = shortArr.length;
        int shortChunkSize = ARRAY_CHUNK_SIZE / 2;
        int remainder = arrLength % shortChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        ShortBuffer shorts = ByteBuffer.wrap(bytes).asShortBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(shortRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            for (int i = 0; i < fullChunkSize; i += shortChunkSize) {
                shorts.put(shortArr, i, shortChunkSize);
                stream.write(bytes, 0, ARRAY_CHUNK_SIZE);
                shorts.rewind();
            }
            if (remainder > 0) {
                shorts.put(shortArr, fullChunkSize, remainder);
                stream.write(bytes, 0, remainder * 2);
            }
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.SHORT);

        return compressedRecord;
    }

    private static CompressedDataRecord convertInt(IntegerDataRecord intRecord)
            throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(intRecord);

        int[] intArr = intRecord.getIntData();
        int arrLength = intArr.length;
        int intChunkSize = ARRAY_CHUNK_SIZE / 4;
        int remainder = arrLength % intChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        IntBuffer ints = ByteBuffer.wrap(bytes).asIntBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(intRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            for (int i = 0; i < fullChunkSize; i += intChunkSize) {
                ints.put(intArr, i, intChunkSize);
                stream.write(bytes, 0, ARRAY_CHUNK_SIZE);
                ints.rewind();
            }
            if (remainder > 0) {
                ints.put(intArr, fullChunkSize, remainder);
                stream.write(bytes, 0, remainder * 4);
            }
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.INT);

        return compressedRecord;
    }

    private static CompressedDataRecord convertLong(LongDataRecord longRecord)
            throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(longRecord);

        long[] longArr = longRecord.getLongData();
        int arrLength = longArr.length;
        int longChunkSize = ARRAY_CHUNK_SIZE / 8;
        int remainder = arrLength % longChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        LongBuffer longs = ByteBuffer.wrap(bytes).asLongBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(longRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            for (int i = 0; i < fullChunkSize; i += longChunkSize) {
                longs.put(longArr, i, longChunkSize);
                stream.write(bytes, 0, ARRAY_CHUNK_SIZE);
                longs.rewind();
            }
            if (remainder > 0) {
                longs.put(longArr, fullChunkSize, remainder);
                stream.write(bytes, 0, remainder * 8);
            }
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.LONG);

        return compressedRecord;
    }

    private static CompressedDataRecord convertFloat(
            FloatDataRecord floatRecord) throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(floatRecord);

        float[] floatArr = floatRecord.getFloatData();
        int arrLength = floatArr.length;
        int floatChunkSize = ARRAY_CHUNK_SIZE / 4;
        int remainder = arrLength % floatChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        FloatBuffer floats = ByteBuffer.wrap(bytes).asFloatBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(floatRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            for (int i = 0; i < fullChunkSize; i += floatChunkSize) {
                floats.put(floatArr, i, floatChunkSize);
                stream.write(bytes, 0, ARRAY_CHUNK_SIZE);
                floats.rewind();
            }
            if (remainder > 0) {
                floats.put(floatArr, fullChunkSize, remainder);
                stream.write(bytes, 0, remainder * 4);
            }
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.FLOAT);

        return compressedRecord;
    }

    private static CompressedDataRecord convertDouble(
            DoubleDataRecord doubleRecord) throws IOException {
        CompressedDataRecord compressedRecord = cloneMetadata(doubleRecord);

        double[] doubleArr = doubleRecord.getDoubleData();
        int arrLength = doubleArr.length;
        int doubleChunkSize = ARRAY_CHUNK_SIZE / 8;
        int remainder = arrLength % doubleChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        DoubleBuffer doubles = ByteBuffer.wrap(bytes).asDoubleBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance().getStream(doubleRecord.getSizeInBytes()
                        / COMPRESSION_RATIO_ASSUMPTION);
        try (GZIPOutputStream stream = createGZIPStream(byteStream)) {
            for (int i = 0; i < fullChunkSize; i += doubleChunkSize) {
                doubles.put(doubleArr, i, doubleChunkSize);
                stream.write(bytes, 0, ARRAY_CHUNK_SIZE);
                doubles.rewind();
            }
            if (remainder > 0) {
                doubles.put(doubleArr, fullChunkSize, remainder);
                stream.write(bytes, 0, remainder * 8);
            }
            stream.finish();
            compressedRecord.setCompressedData(byteStream.toByteArray());
        }
        compressedRecord.setType(Type.FLOAT);

        return compressedRecord;
    }

    private static GZIPOutputStream createGZIPStream(OutputStream out)
            throws IOException {
        return new GZIPOutputStream(out) {
            {
                this.def.setLevel(Deflater.BEST_SPEED);
            }
        };

    }

}
