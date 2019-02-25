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

import java.awt.Point;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.Request.Type;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DoubleDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.datastorage.records.LongDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 *
 * Various methods to help with chunking and compression of data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Sep 14, 2018  7435     ksunil  Initial creation. Methods for new chunked compression.
 *
 * </pre>
 *
 * @author ksunil
 */
@DynamicSerialize
public class ChunkFactory {

    private static final Logger logger = LoggerFactory
            .getLogger(ChunkFactory.class);

    /**
     * Calculate the correct ByteRecord to return from the parameters
     *
     * @param sourceRecord
     *            Original source record
     * @param request
     *            Original request
     * @param uncompressedData
     *            Map of chunk index and the uncompressed data
     * @param chunkMap
     *            Map of chunk index and CompressedChunks
     * @param chunk_size
     *            chunk size from the record
     * @return IDataRecord the correct type of data record with changed sizes
     *         and the correct data
     */

    public static IDataRecord createRequestedByteTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, byte[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        ByteDataRecord targetRecord = new ByteDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        byte[] returnedData = createRequestedDataArray(byte[].class,
                sourceRecord, request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setByteData(returnedData);
        return targetRecord;
    }

    public static IDataRecord createRequestedShortTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, short[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        ShortDataRecord targetRecord = new ShortDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        short[] returnedData = createRequestedDataArray(short[].class,
                sourceRecord, request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setShortData(returnedData);

        return targetRecord;
    }

    public static IDataRecord createRequestedIntTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, int[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        IntegerDataRecord targetRecord = new IntegerDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        int[] returnedData = createRequestedDataArray(int[].class, sourceRecord,
                request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setIntData(returnedData);
        return targetRecord;
    }

    public static IDataRecord createRequestedFloatTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, float[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        FloatDataRecord targetRecord = new FloatDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        float[] returnedData = createRequestedDataArray(float[].class,
                sourceRecord, request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setFloatData(returnedData);
        return targetRecord;
    }

    public static IDataRecord createRequestedDoubleTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, double[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        DoubleDataRecord targetRecord = new DoubleDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        double[] returnedData = createRequestedDataArray(double[].class,
                sourceRecord, request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setDoubleData(returnedData);
        return targetRecord;
    }

    public static IDataRecord createRequestedLongTargetRecord(
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, long[]> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {
        LongDataRecord targetRecord = new LongDataRecord();
        CompressedChunkedDataRecordUtil
                .cloneMetadataFromCompressedRecord(sourceRecord, targetRecord);
        long newDims[] = getNewDimensions(sourceRecord, request);
        targetRecord.setSizes(newDims);

        long[] returnedData = createRequestedDataArray(long[].class,
                sourceRecord, request, uncompressedData, chunkMap, chunk_size);

        targetRecord.setLongData(returnedData);
        return targetRecord;
    }

    public static long findChunkIndex(long x, long y, long chunkSize,
            long xChunks) {
        long ret = ((y / chunkSize) * xChunks) + (x / chunkSize);
        return ret;
    }

    // See com.raytheon.uf.common.dataplugin.grid.derivparam.data.SliceUtil.java
    // for questions on the x,y orientation. If x,y is reversed, so will the
    // final map! (Note: YLINE is incorrect in SliceUtil.java)
    private static List<int[]> getRequestedPoints(
            CompressedChunkedDataRecord sourceRecord, Request request) {
        Request.Type requestType = request.getType();
        List<int[]> requestedPoints = new ArrayList<>();
        boolean debug = logger.isDebugEnabled();
        if (requestType == Request.Type.SLAB) {

            if (debug) {
                logger.debug("SLAB request: min/max: "
                        + Arrays.toString(request.getMinIndexForSlab()) + "/"
                        + Arrays.toString(request.getMaxIndexForSlab()));

            }
            for (int i = request.getMinIndexForSlab()[1]; i < request
                    .getMaxIndexForSlab()[1]; i++) {
                for (int j = request.getMinIndexForSlab()[0]; j < request
                        .getMaxIndexForSlab()[0]; j++) {
                    requestedPoints.add(new int[] { j, i });
                }
            }

        } else if (requestType == Request.Type.XLINE) {

            int[] reqIndices = request.getIndices();
            long xDim = sourceRecord.getSizes()[0];

            if (debug) {
                logger.debug("XLINE: reqIndices: " + Arrays.toString(reqIndices)
                        + ", xDim: " + xDim);

            }
            for (int i = 0; i < reqIndices.length; i++) {
                for (int j = 0; j < xDim; j++) {
                    requestedPoints.add(new int[] { j, reqIndices[i] });

                }
            }

        } else if (requestType == Request.Type.YLINE) {

            int[] reqIndices = request.getIndices();
            long yDim = sourceRecord.getSizes()[1];
            if (debug) {
                logger.debug("YLINE: reqIndices: " + Arrays.toString(reqIndices)
                        + ", yDim: " + yDim);

            }
            for (int i = 0; i < reqIndices.length; i++) {
                for (int j = 0; j < yDim; j++) {
                    requestedPoints.add(new int[] { reqIndices[i], j });
                }
            }
        } else if (requestType == Request.Type.POINT) {

            Point[] points = request.getPoints();
            if (debug) {
                logger.debug("POINT: length: " + points.length);

            }
            for (Point pt : points) {
                requestedPoints.add(new int[] { pt.x, pt.y });
            }

        }

        return requestedPoints;

    }

    /**
     * Given a request SLAB/XLINE/YLINE/POINT, calculates the new dimensions of
     * the (eventually) returned data record object
     *
     * @param sourceRecord
     *            The source record
     * @param request
     *            The request
     * @return long[] The dimensions for the data record that will be returned
     *         by the caller.
     */

    private static long[] getNewDimensions(
            CompressedChunkedDataRecord sourceRecord, Request request) {
        Request.Type requestType = request.getType();
        long[] newDims = new long[2];
        long[] orgDims = sourceRecord.getSizes();
        if (requestType == Request.Type.SLAB) {
            int[] minIndex = request.getMinIndexForSlab();
            int[] maxIndex = request.getMaxIndexForSlab();

            newDims[0] = maxIndex[0] - minIndex[0];
            newDims[1] = maxIndex[1] - minIndex[1];
        } else if (requestType == Request.Type.XLINE) {

            int[] reqIndices = request.getIndices();

            newDims[0] = orgDims[0];
            newDims[1] = reqIndices.length;

        } else if (requestType == Request.Type.YLINE) {

            int[] reqIndices = request.getIndices();

            newDims[0] = reqIndices.length;
            newDims[1] = orgDims[1];

        } else if (requestType == Request.Type.POINT) {

            Point[] points = request.getPoints();

            newDims[0] = 1;
            newDims[1] = points.length;
        }

        return newDims;
    }

    /**
     * Finds the position index of the position identified by "point" within an
     * original dimension of dims. General purpose function. 1 2 3 4 5 6 7 8 9
     * 10 11 12 dimension [3,4]. point [1,2] will have an index of 7.
     *
     * @param point
     *            The point for which we are calculating the index
     * @param dims
     *            Dimensions of the record/array
     */

    public static int calculateIndexInArray(int[] point, int[] dims) {

        final int[] revPoint = reverseDims(point);

        int offset = 0;
        for (int i = 0; i < revPoint.length - 1; i++) {
            int mult = 1;
            for (int j = i; j < dims.length - 1; j++) {
                mult = mult * dims[j];
            }
            offset = offset + mult * revPoint[i];
        }
        offset = offset + revPoint[revPoint.length - 1];
        return offset;
    }

    // utility
    public static int[] reverseDims(int[] inDims) {

        final int[] revDims = new int[inDims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = inDims[inDims.length - 1 - i];
        }

        return revDims;
    }

    // utility
    public static int[] reverseLongDims(long[] inDims) {

        final int[] revDims = new int[inDims.length];
        for (int i = 0; i < revDims.length; i++) {
            revDims[i] = (int) inDims[inDims.length - 1 - i];
        }

        return revDims;
    }

    /**
     * subtracts the chunk's (reversed) off set from the required point's
     * offset. This resulting off set is later used to index into the chunk's
     * de-compressed array.
     *
     * @param offset
     *            the point for which we are adjusting the offset
     * @param chunk
     *            compressed chunk
     */

    private static int[] adjustOffsets(int[] offset, CompressedChunk chunk) {

        final int[] newOffsets = new int[chunk.offsets.length];
        int[] chkOffsets = chunk.offsets;
        final int[] revOffSets = new int[chkOffsets.length];
        for (int i = 0; i < revOffSets.length; i++) {
            revOffSets[i] = chkOffsets[revOffSets.length - 1 - i];
        }

        for (int i = 0; i < newOffsets.length; i++) {
            newOffsets[i] = offset[i] - revOffSets[i];
        }
        return newOffsets;
    }

    /**
     * Calculate the right data array to return, given the request (contains the
     * requested point(s) information), the list of CompressedChunk etc
     *
     * @param returnClazz
     *            the class object of the array (could be float[].class for
     *            example)
     * @param sourceRecord
     *            the source record
     * @param request
     *            original request used to calculate the requested points etc
     * @param uncompressedData
     *            Map of chunk index and the uncompressed data
     * @param chunkMap
     *            Map of chunk index and CompressedChunks
     * @param chunk_size
     *            chunk size used when the record was created
     * @return T the correct type of primitive array containing the correct data
     */

    public static <T> T createRequestedDataArray(Class<T> returnClazz,
            CompressedChunkedDataRecord sourceRecord, Request request,
            Map<Integer, T> uncompressedData,
            Map<Integer, CompressedChunk> chunkMap, int chunk_size) {

        long[] orgDims = sourceRecord.getSizes();
        int[] orgDimsInt = new int[orgDims.length];
        for (int i = 0; i < orgDims.length; i++) {
            orgDimsInt[i] = (int) orgDims[i];
        }
        long xChunks = (long) Math.ceil((float) orgDims[0] / chunk_size);

        int totalChunks = uncompressedData.size();
        long newDims[] = getNewDimensions(sourceRecord, request);

        int size = 1;
        for (int i = 0; i < newDims.length; i++) {
            size = (int) (size * newDims[i]);
        }
        T returnedData = returnClazz
                .cast(Array.newInstance(returnClazz.getComponentType(), size));

        List<int[]> points = getRequestedPoints(sourceRecord, request);
        int[] chunkIndices = null;
        Type type = request.getType();
        if (type == Type.POINT || type == Type.XLINE || type == Type.YLINE) {
            chunkIndices = sourceRecord.getChunkIndices();
        }
        int i = 0;

        // Initially,assume there is only 1 chunk. If not, we will adjust 5
        // lines down.
        int chunkIndex = chunkMap.keySet().stream().findFirst().get();

        for (int[] pt : points) {

            if (totalChunks != 1) {
                // If not a SLAB, the chunkIndex is already passed back from
                // Pypies.
                if (type == Type.SLAB) {
                    chunkIndex = (int) findChunkIndex(pt[0], pt[1], chunk_size,
                            xChunks);
                } else {
                    chunkIndex = chunkIndices[i];
                }
            }

            CompressedChunk chunk = chunkMap.get(chunkIndex);
            T decompressed = uncompressedData.get(chunkIndex);
            int indexInChunkData = calculateIndexInArray(
                    adjustOffsets(pt, chunk), reverseDims(chunk.getSizes()));
            System.arraycopy(decompressed, indexInChunkData, returnedData, i,
                    1);
            i++;
        }

        return returnedData;
    }

    /**
     * a 3x4 array of numbers from 1...12 with a chunk size of 2. 1st of all,
     * the row/column concept is reversed. It is not 3 rows, 4 columns. We have
     * 4 rows of 3 columns each [1, 2, 3] [4, 5, 6] [7, 8, 9] [10, 11, 12] And
     * the data is chunked as follows. 4 "blocks"/"chunks"
     *
     * [1, 2, 4, 5] [3,6] [7, 8, 10, 11] [9, 12]
     *
     * The following code recreates the original array, given that the
     * individual chunks give you data that is "out of order". This code is
     * "opposite of" the chunk creation, used the same algorithm. * @param
     * returnClazz the class object of the array (could be float[].class for
     * example)
     *
     * @param uncompressedChunks
     *            uncompressed data chunks. Type is generic.
     * @param dims
     *            original dimensions
     * @param chunk_size
     *            chunk size used when the record was created
     * @return T the correct type of primitive array containing the data from
     *         all chunks in the right order
     */
    public static <T> T create1DArrayFromAllChunks(Class<T> returnClazz,
            List<T> uncompressedChunks, long[] dims, int chunk_size) {
        boolean debug = logger.isDebugEnabled();

        // If there is only 1 slab, there is no need to re-order anything. This
        // was never "chunked". This is plain, ordered data.

        if (uncompressedChunks.size() == 1) {
            return uncompressedChunks.get(0);
        }
        long xDim = dims[0];
        long yDim;
        if (dims.length == 1) {
            yDim = 1;
        } else {
            yDim = dims[1];
        }

        long yChunks = (long) Math.ceil((float) yDim / chunk_size);
        long xChunks = (long) Math.ceil((float) xDim / chunk_size);
        T returnedData = returnClazz.cast(Array.newInstance(
                returnClazz.getComponentType(), (int) (xDim * yDim)));

        if (debug) {
            long cnt = 0;
            for (T i : uncompressedChunks) {
                cnt = cnt + Array.getLength(i);
            }
            logger.debug("xChunks/yChunk: " + xChunks + "/" + yChunks
                    + " xDim/yDim: " + xDim + "/" + yDim);
            logger.debug("SLABS length : " + uncompressedChunks.size()
                    + ", new 1D array size : " + xDim * yDim);
            logger.debug("total data in the slabs: " + cnt);
        }
        for (int y = 0; y < yChunks; y++) {
            for (int x = 0; x < xChunks; x++) {
                long numRows = Math.min(chunk_size, yDim - (y * chunk_size));
                int slabIndex = (int) (y * xChunks + x);
                for (int row = 0; row < numRows; row++) {
                    int index = (int) ((y * chunk_size + row) * xDim
                            + x * chunk_size);
                    int copySize = (int) Math.min(chunk_size,
                            xDim - (x * chunk_size));
                    System.arraycopy(uncompressedChunks.get(slabIndex),
                            row * copySize, returnedData, index, copySize);
                }
            }
        }
        return returnedData;
    }

}
