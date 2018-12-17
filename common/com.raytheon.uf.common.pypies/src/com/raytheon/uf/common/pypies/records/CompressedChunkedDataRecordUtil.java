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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DoubleDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.datastorage.records.LongDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.pypies.records.CompressedChunkedDataRecord.Type;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

/**
 *
 * Various utilities to help with chunking and compression of data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Sep 14, 2018  7435     ksunil  Initial creation. Utilities for new chunked compression.
 *
 * </pre>
 *
 * @author ksunil
 */
@DynamicSerialize
public class CompressedChunkedDataRecordUtil {

    private static final int ARRAY_CHUNK_SIZE = 1024;

    private static final int COMPRESSION_RATIO_ASSUMPTION = 4;

    private static final Logger logger = LoggerFactory
            .getLogger(CompressedChunkedDataRecordUtil.class);

    protected static byte[] compressedBytes(byte[] data) throws IOException {

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
            stream.write(data);
            stream.finish();
            return (byteStream.toByteArray());
        }

    }

    protected static byte[] compressedShorts(short[] data) throws IOException {

        short[] shortArr = data;
        int arrLength = shortArr.length;
        int shortChunkSize = ARRAY_CHUNK_SIZE / 2;
        int remainder = arrLength % shortChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        ShortBuffer shorts = ByteBuffer.wrap(bytes).asShortBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length * 2 / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
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
            return (byteStream.toByteArray());
        }

    }

    protected static byte[] compressedInts(int[] data) throws IOException {

        int[] intArr = data;
        int arrLength = intArr.length;
        int intChunkSize = ARRAY_CHUNK_SIZE / 4;
        int remainder = arrLength % intChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        IntBuffer ints = ByteBuffer.wrap(bytes).asIntBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length * 4 / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
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
            return (byteStream.toByteArray());
        }

    }

    protected static byte[] compressedLongs(long[] data) throws IOException {

        long[] longArr = data;
        int arrLength = longArr.length;
        int longChunkSize = ARRAY_CHUNK_SIZE / 8;
        int remainder = arrLength % longChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        LongBuffer longs = ByteBuffer.wrap(bytes).asLongBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length * 8 / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
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
            return (byteStream.toByteArray());
        }

    }

    protected static byte[] compressedDoubles(double[] data)
            throws IOException {

        double[] doubleArr = data;
        int arrLength = doubleArr.length;
        int doubleChunkSize = ARRAY_CHUNK_SIZE / 8;
        int remainder = arrLength % doubleChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        DoubleBuffer doubles = ByteBuffer.wrap(bytes).asDoubleBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length * 8 / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
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
            return (byteStream.toByteArray());
        }

    }

    protected static byte[] compressedFloats(float[] data) throws IOException {

        float[] floatArr = data;
        int arrLength = floatArr.length;
        int floatChunkSize = ARRAY_CHUNK_SIZE / 4;
        int remainder = arrLength % floatChunkSize;
        int fullChunkSize = arrLength - remainder;
        byte[] bytes = new byte[ARRAY_CHUNK_SIZE];
        FloatBuffer floats = ByteBuffer.wrap(bytes).asFloatBuffer();

        PooledByteArrayOutputStream byteStream = ByteArrayOutputStreamPool
                .getInstance()
                .getStream(data.length * 4 / COMPRESSION_RATIO_ASSUMPTION);
        try (DeflaterOutputStream stream = createDeflaterStream(byteStream)) {
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
            return (byteStream.toByteArray());
        }

    }

    private static DeflaterOutputStream createDeflaterStream(OutputStream out)
            throws IOException {
        /*
         * return new GZIPOutputStream(out) { {
         * this.def.setLevel(Deflater.BEST_SPEED); } };
         */
        return new DeflaterOutputStream(out, new Deflater(Deflater.BEST_SPEED));

    }

    protected static CompressedChunk advanceChunk(int[] dims,
            CompressedChunk previous, int chunk_size) {
        final int numDims = dims.length;
        final CompressedChunk next = new CompressedChunk();
        next.offsets = new int[numDims];
        if (previous != null) {
            for (int i = 0; i < numDims; i++) {
                next.offsets[i] = previous.offsets[i];
            }

            int index = numDims - 1;
            while (index >= 0) {
                next.offsets[index] += previous.sizes[index];
                if (next.offsets[index] >= dims[index]) {
                    next.offsets[index] = 0;
                    index--;
                } else {
                    break;
                }
            }

            // have advanced all the way through
            if (index < 0 && next.offsets[0] == 0) {
                return null;
            }
        }

        next.sizes = new int[numDims];
        for (int i = 0; i < numDims; i++) {
            next.sizes[i] = Math.min(chunk_size, dims[i] - next.offsets[i]);
        }

        return next;
    }

    public static List<CompressedChunk> chunkShort(short[] input, long[] dims)
            throws IOException {
        final int dataSize = 2; // short
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final ShortBuffer shorts = ByteBuffer.wrap(row).asShortBuffer();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkShort(input, revDims, row, shorts, zipStream,
                        chunk, 0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkShort(short[] input, int[] dims,
            byte[] bytes, ShortBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 2);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkShort(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    public static List<CompressedChunk> chunkInt(int[] input, long[] dims)
            throws IOException {
        final int dataSize = 4; // int
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final IntBuffer ints = ByteBuffer.wrap(row).asIntBuffer();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkInt(input, revDims, row, ints, zipStream, chunk,
                        0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkInt(int[] input, int[] dims, byte[] bytes,
            IntBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 4);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkInt(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    public static List<CompressedChunk> chunkFloat(float[] input, long[] dims)
            throws IOException {
        final int dataSize = 4; // float
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final FloatBuffer floats = ByteBuffer.wrap(row).asFloatBuffer();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkFloat(input, revDims, row, floats, zipStream,
                        chunk, 0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkFloat(float[] input, int[] dims,
            byte[] bytes, FloatBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 4);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkFloat(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    public static List<CompressedChunk> chunkLong(long[] input, long[] dims)
            throws IOException {
        final int dataSize = 8; // long
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final LongBuffer longs = ByteBuffer.wrap(row).asLongBuffer();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkLong(input, revDims, row, longs, zipStream, chunk,
                        0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkLong(long[] input, int[] dims,
            byte[] bytes, LongBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 8);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkLong(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    public static List<CompressedChunk> chunkDouble(double[] input, long[] dims)
            throws IOException {
        final int dataSize = 8; // double
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final DoubleBuffer doubles = ByteBuffer.wrap(row).asDoubleBuffer();
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkDouble(input, revDims, row, doubles, zipStream,
                        chunk, 0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkDouble(double[] input, int[] dims,
            byte[] bytes, DoubleBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 8);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkDouble(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    public static List<CompressedChunk> chunkByte(byte[] input, long[] dims)
            throws IOException {
        final int dataSize = 1; // byte
        final byte[] row = new byte[CompressedChunkedDataRecord.CHUNK_SIZE
                * dataSize];
        final ByteBuffer bytes = ByteBuffer.wrap(row);
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final List<CompressedChunk> rval = new ArrayList<>();

        // reverse dimensions to make things look logical and get rid of long
        final int[] revDims = new int[dims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) dims[dims.length - 1 - i];
        }

        CompressedChunk chunk = null;
        while ((chunk = advanceChunk(revDims, chunk,
                CompressedChunkedDataRecord.CHUNK_SIZE)) != null) {
            int bufSize = 1;
            for (final int size : chunk.sizes) {
                bufSize *= size;
            }

            try (DeflaterOutputStream zipStream = new DeflaterOutputStream(
                    byteStream, new Deflater(Deflater.BEST_SPEED), bufSize)) {
                recursiveChunkByte(input, revDims, row, bytes, zipStream, chunk,
                        0, 0);
                zipStream.finish();
                chunk.data = byteStream.toByteArray();
                rval.add(chunk);
            }

            byteStream.reset();
        }
        byteStream.close();
        return rval;
    }

    private static void recursiveChunkByte(byte[] input, int[] dims,
            byte[] bytes, ByteBuffer buffer, DeflaterOutputStream stream,
            CompressedChunk chunk, int dimPosition, int offset)
            throws IOException {
        if (dimPosition == dims.length - 1) {
            buffer.put(input, offset + chunk.offsets[dimPosition],
                    chunk.sizes[dimPosition]);
            stream.write(bytes, 0, chunk.sizes[dimPosition] * 1);
            buffer.rewind();
            return;
        }

        int offsetCost = dims[dims.length - 1];
        for (int i = dims.length - 2; i > dimPosition; i--) {
            offsetCost *= dims[i];
        }

        offset += offsetCost * chunk.offsets[dimPosition];

        for (int i = 0; i < chunk.sizes[dimPosition]; i++) {

            recursiveChunkByte(input, dims, bytes, buffer, stream, chunk,
                    dimPosition + 1, offset);
            offset += offsetCost;
        }
    }

    /**
     * Convert the compressed bytes (coming from HDF5) into its original
     * int/short/byte/double/long/float Data Record.
     *
     * @param sourceRecord
     *            The CompressedChunkedDataRecord that is being converted
     * @param request
     *            original request
     * @param byteStream
     *            ByteArrayOutputStream.
     * @throws IOException
     * @throws DataFormatException
     */
    public static IDataRecord convertCompressedRecToTypeRec(
            CompressedChunkedDataRecord sourceRecord, Request request,
            PooledByteArrayOutputStream byteStream)
            throws StorageException, IOException, DataFormatException {
        Request.Type requestType;
        boolean debug = logger.isDebugEnabled();
        if (request == null) {
            requestType = Request.Type.ALL;
        } else {
            requestType = request.getType();
        }
        // set as a default and then overwrite with whatever is set in the
        // record.
        int chunk_size = CompressedChunkedDataRecord.CHUNK_SIZE;
        Map<String, Object> savedAttr = sourceRecord.getDataAttributes();
        if (savedAttr != null) {
            Integer savedChunkSize = (Integer) savedAttr
                    .get(CompressedChunkedDataRecord.CDR_CHUNK_SIZE);
            if (savedChunkSize != null) {
                chunk_size = savedChunkSize.intValue();
                if (debug) {
                    logger.debug(
                            "Read chunk_size from the record: " + chunk_size);
                }
            }
        }

        /*
         * - The sourceRecord has the required chunks to fulfill you request --
         * They are compressed. So first we need to de-compress -- A request may
         * return chunk 0,5,9,13 for example. Can't assume they are serial. -- A
         * chunk may *not* be a contiguous array. (Meaning, if the chunk
         * uncompressed to short {1, 2, 4, 5}, doesn't mean in the original
         * array, {1, 2, 4, 5} occupied a contiguous space.
         *
         *
         * - Request is a collection of points (XLINE/POINT/YLINE/SLAB). Request
         * could also be ALL, in which case all the chunks are simply merged,
         * making sure the positions match what they used to be in the original
         * array-- We need to map a requested point to a chunk first. Then grab
         * the un-compressed data for that point from the chunk's uncompressed
         * data --
         */

        if (requestType != Request.Type.ALL) {
            Type type = sourceRecord.getType();
            long[] dims = sourceRecord.getSizes();
            List<CompressedChunk> returnedChunks = (List<CompressedChunk>) sourceRecord
                    .getDataObject();
            Map<Integer, CompressedChunk> chunkMap = new HashMap<>(
                    returnedChunks.size());

            for (CompressedChunk chunk : returnedChunks) {
                if (debug) {
                    logger.debug("Chunk: " + chunk.toString());
                }
                chunkMap.put(chunk.getIndex(), chunk);
            }

            if (debug) {
                logger.debug("Record Type:" + type + ". Request Type: "
                        + requestType + ". Original Dims:"
                        + Arrays.toString(dims) + ", #of chunks returned: "
                        + returnedChunks.size());
            }

            if (type == Type.BYTE) {
                Map<Integer, byte[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {

                    byte[] unCompressed = decompressZippedBytes(
                            returnedChunks.get(i).getData(), byteStream);
                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                }
                return ChunkFactory.createRequestedByteTargetRecord(
                        sourceRecord, request, uncompressedData, chunkMap,
                        chunk_size);

            } else if (type == Type.DOUBLE) {
                Map<Integer, double[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {
                    double[] unCompressed = convertToDoubleArray(
                            decompressZippedBytes(
                                    returnedChunks.get(i).getData(),
                                    byteStream));

                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                    if (debug) {
                        logger.debug("length of uncompressed chunk: "
                                + unCompressed.length);
                    }

                }
                return ChunkFactory.createRequestedDoubleTargetRecord(
                        sourceRecord, request, uncompressedData, chunkMap,
                        chunk_size);

            } else if (type == Type.SHORT) {
                Map<Integer, short[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {
                    short[] unCompressed = convertToShortArray(
                            decompressZippedBytes(
                                    returnedChunks.get(i).getData(),
                                    byteStream));
                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                    if (debug) {
                        logger.debug("length of uncompressed chunk: "
                                + unCompressed.length);
                    }
                }

                return ChunkFactory.createRequestedShortTargetRecord(
                        sourceRecord, request, uncompressedData, chunkMap,
                        chunk_size);
            } else if (type == Type.LONG) {
                Map<Integer, long[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {
                    long[] unCompressed = convertToLongArray(
                            decompressZippedBytes(
                                    returnedChunks.get(i).getData(),
                                    byteStream));
                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                    if (debug) {
                        logger.debug("length of uncompressed chunk: "
                                + unCompressed.length);
                    }
                }
                return ChunkFactory.createRequestedLongTargetRecord(
                        sourceRecord, request, uncompressedData, chunkMap,
                        chunk_size);
            } else if (type == Type.INT) {
                Map<Integer, int[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {
                    int[] unCompressed = convertToIntArray(
                            decompressZippedBytes(
                                    returnedChunks.get(i).getData(),
                                    byteStream));
                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                    if (debug) {
                        logger.debug("length of uncompressed chunk: "
                                + unCompressed.length);
                    }
                }
                return ChunkFactory.createRequestedIntTargetRecord(sourceRecord,
                        request, uncompressedData, chunkMap, chunk_size);
            } else if (type == Type.FLOAT) {
                Map<Integer, float[]> uncompressedData = new HashMap<>(
                        returnedChunks.size());
                for (int i = 0; i < returnedChunks.size(); i++) {
                    float[] unCompressed = convertToFloatArray(
                            decompressZippedBytes(
                                    returnedChunks.get(i).getData(),
                                    byteStream));
                    uncompressedData.put(returnedChunks.get(i).getIndex(),
                            unCompressed);
                    if (debug) {
                        logger.debug("length of uncompressed chunk: "
                                + unCompressed.length);
                    }
                }
                return ChunkFactory.createRequestedFloatTargetRecord(
                        sourceRecord, request, uncompressedData, chunkMap,
                        chunk_size);
            } else {
                return sourceRecord;
            }

        } else {
            Type type = sourceRecord.getType();
            if (debug) {
                logger.debug("REQUEST TYPE IS: " + requestType
                        + ", Record Type: " + type + ", DIMS: "
                        + Arrays.toString(sourceRecord.getSizes()));
            }
            List<CompressedChunk> returnedChunks = (List<CompressedChunk>) sourceRecord
                    .getDataObject();
            List<byte[]> listOfUncompressedBytes = new ArrayList<>();
            int totalLength = 0;
            int sizeInDims = 1;
            long[] dims = sourceRecord.getSizes();
            for (int i = 0; i < dims.length; i++) {
                sizeInDims *= dims[i];
            }
            sourceRecord.getSizeInBytes();
            for (CompressedChunk chunk : returnedChunks) {
                byte[] data = chunk.getData();
                int len1 = data.length;
                byte[] deCompressed = decompressZippedBytes(data, byteStream);
                if (debug) {
                    logger.debug("Chunk Size before/after deCompress: " + len1
                            + "/" + deCompressed.length);
                }
                totalLength += deCompressed.length;
                listOfUncompressedBytes.add(deCompressed);
            }
            if (debug) {
                logger.debug("Number of chunks returned: "
                        + returnedChunks.size()
                        + " (primitive type) Size as per dimensions: "
                        + sizeInDims + ", total uncompressed (byte) size: "
                        + totalLength);
            }

            if (type == Type.BYTE) {
                ByteDataRecord targetRecord = new ByteDataRecord();
                List<byte[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(chunk);
                }
                byte[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        byte[].class, chunksOutOfOrder, dims, chunk_size);
                if (debug) {
                    logger.debug("processing byte: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setByteData(orderedData);
                return targetRecord;
            } else if (type == Type.DOUBLE) {
                DoubleDataRecord targetRecord = new DoubleDataRecord();
                List<double[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(convertToDoubleArray(chunk));
                }
                double[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        double[].class, chunksOutOfOrder, dims, chunk_size);

                if (debug) {
                    logger.debug("processing double: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setDoubleData(orderedData);
                return targetRecord;
            } else if (type == Type.FLOAT) {
                FloatDataRecord targetRecord = new FloatDataRecord();
                List<float[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(convertToFloatArray(chunk));
                }
                float[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        float[].class, chunksOutOfOrder, dims, chunk_size);

                if (debug) {
                    logger.debug("processing float: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setFloatData(orderedData);
                return targetRecord;
            } else if (type == Type.INT) {
                IntegerDataRecord targetRecord = new IntegerDataRecord();
                List<int[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(convertToIntArray(chunk));
                }
                int[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        int[].class, chunksOutOfOrder, dims, chunk_size);

                if (debug) {
                    logger.debug("processing int: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setIntData(orderedData);
                return targetRecord;
            } else if (type == Type.LONG) {
                LongDataRecord targetRecord = new LongDataRecord();
                List<long[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(convertToLongArray(chunk));
                }
                long[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        long[].class, chunksOutOfOrder, dims, chunk_size);

                if (debug) {
                    logger.debug("processing long: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setLongData(orderedData);
                return targetRecord;
            } else if (type == Type.SHORT) {
                ShortDataRecord targetRecord = new ShortDataRecord();
                List<short[]> chunksOutOfOrder = new ArrayList<>();
                for (byte[] chunk : listOfUncompressedBytes) {
                    chunksOutOfOrder.add(convertToShortArray(chunk));
                }
                short[] orderedData = ChunkFactory.create1DArrayFromAllChunks(
                        short[].class, chunksOutOfOrder, dims, chunk_size);

                if (debug) {
                    logger.debug("processing short: orderedData.size: "
                            + orderedData.length);
                }
                cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
                targetRecord.setShortData(orderedData);
                return targetRecord;
            } else {
                return sourceRecord;
            }
        }
    }

    private static short[] convertToShortArray(byte[] unCompressed)
            throws IOException {

        ShortBuffer buffer = ByteBuffer.wrap(unCompressed).asShortBuffer();
        short[] array = new short[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private static int[] convertToIntArray(byte[] unCompressed)
            throws IOException {

        IntBuffer buffer = ByteBuffer.wrap(unCompressed).asIntBuffer();
        int[] array = new int[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private static long[] convertToLongArray(byte[] unCompressed)
            throws IOException {

        LongBuffer buffer = ByteBuffer.wrap(unCompressed).asLongBuffer();
        long[] array = new long[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private static float[] convertToFloatArray(byte[] unCompressed)
            throws IOException {

        FloatBuffer buffer = ByteBuffer.wrap(unCompressed).asFloatBuffer();
        float[] array = new float[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private static double[] convertToDoubleArray(byte[] unCompressed)
            throws IOException {

        DoubleBuffer buffer = ByteBuffer.wrap(unCompressed).asDoubleBuffer();
        double[] array = new double[buffer.remaining()];
        buffer.get(array);
        return array;
    }

    private static byte[] decompressZippedBytes(byte[] data,
            PooledByteArrayOutputStream outputStream)
            throws IOException, DataFormatException {

        Inflater inflater = new Inflater();
        inflater.setInput(data);

        byte[] buffer = new byte[ARRAY_CHUNK_SIZE * 4];

        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            if (logger.isDebugEnabled()) {
                logger.debug("Deflated cnt: " + count);
            }
            outputStream.write(buffer, 0, count);
        }
        byte[] ret = outputStream.toByteArray();
        outputStream.reset();
        return ret;
    }

    static IDataRecord cloneMetadataFromCompressedRecord(
            CompressedChunkedDataRecord sourceCompressedRecord,
            IDataRecord targetRecord) {
        targetRecord.setName(sourceCompressedRecord.getName());
        targetRecord.setDimension(sourceCompressedRecord.getDimension());
        targetRecord.setSizes(sourceCompressedRecord.getSizes());
        targetRecord.setMaxSizes(sourceCompressedRecord.getMaxSizes());
        targetRecord.setProperties(sourceCompressedRecord.getProperties());
        targetRecord.setMinIndex(sourceCompressedRecord.getMinIndex());
        targetRecord.setGroup(sourceCompressedRecord.getGroup());
        if (sourceCompressedRecord.getDataAttributes() != null) {
            targetRecord.setDataAttributes(new HashMap<String, Object>(
                    sourceCompressedRecord.getDataAttributes()));
        }
        targetRecord.setFillValue(sourceCompressedRecord.getFillValue());
        targetRecord.setMaxChunkSize(sourceCompressedRecord.getMaxChunkSize());
        targetRecord.setCorrelationObject(
                sourceCompressedRecord.getCorrelationObject());
        return targetRecord;
    }
}
