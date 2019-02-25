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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

/**
 *
 * Record containing compressed chunked version of data. This is intended to
 * reduce the bandwidth usage when communicating with pypies.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Sep 19, 2018  7435     ksunil    Eliminate compression/decompression on HDF5
 *
 * </pre>
 *
 * @author ksunil
 */
@DynamicSerialize
public class CompressedChunkedDataRecord extends AbstractStorageRecord {

    public static final int CHUNK_SIZE = Integer
            .parseInt(System.getProperty("CDR_CHUNK_SIZE", "256"));

    private static final String COMPRESSED_TYPE = "CDRType";

    private static final String DIMENSION = "CDRDimension";

    private static final String DATA_SIZES = "CDRDataSizes";

    public static final String CDR_CHUNK_SIZE = "CDRChunkSize";

    private static final String MAX_DATA_SIZES = "CDRMaxDataSizes";

    private static final String NUM_CHUNKS = "CDRNumberOfChunks";

    private static final String CHUNK_DIMS = "ChunkDims";

    private static final String CHUNK_OFFSETS = "ChunkOffsets";

    private static final Logger logger = LoggerFactory
            .getLogger(CompressedChunkedDataRecord.class);

    public static enum Type {
        BYTE, SHORT, INT, LONG, FLOAT, DOUBLE;
    }

    @DynamicSerializeElement
    protected List<CompressedChunk> compressedChunks;

    @DynamicSerializeElement
    protected int[] chunkIndices;

    public List<CompressedChunk> getCompressedChunks() {
        return compressedChunks;
    }

    public void setCompressedChunks(List<CompressedChunk> compressedChunks) {
        this.compressedChunks = compressedChunks;
    }

    @DynamicSerializeElement
    protected Type type;

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
        int len = 0;
        if (compressedChunks != null) {
            for (int i = 0; i < compressedChunks.size(); i++) {
                len = len + compressedChunks.get(i).getData().length;
            }
        }
        return len;
    }

    @Override
    public Object getDataObject() {
        return compressedChunks;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    protected CompressedChunkedDataRecord cloneInternal() {
        CompressedChunkedDataRecord record = new CompressedChunkedDataRecord();
        record.type = type;
        if (compressedChunks != null) {
            record.compressedChunks = new ArrayList<>();
            for (CompressedChunk compressedBlock : compressedChunks) {
                record.compressedChunks.add(new CompressedChunk(
                        compressedBlock.getData(), compressedBlock.getSizes(),
                        compressedBlock.getOffsets()));
            }
        }

        if (sizes != null) {
            record.sizes = Arrays.copyOf(sizes, sizes.length);
        }

        if (chunkIndices != null) {
            record.chunkIndices = Arrays.copyOf(chunkIndices,
                    chunkIndices.length);
        }
        return record;
    }

    public int[] getChunkIndices() {
        return chunkIndices;
    }

    public CompressedChunkedDataRecord() {
        super();

    }

    public void setChunkIndices(int[] chunkIndices) {
        this.chunkIndices = chunkIndices;
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
            logger.error("Exception while converting: ", e);
            throw new StorageException("Error compressing Data", sourceRecord);
        }
        return sourceRecord;
    }

    private static CompressedChunkedDataRecord cloneMetadataToCCDR(
            IDataRecord sourceRecord) {
        CompressedChunkedDataRecord compressedRecord = new CompressedChunkedDataRecord();
        compressedRecord.setName(sourceRecord.getName());
        compressedRecord.setDimension(sourceRecord.getDimension());
        compressedRecord.sizes = Arrays.copyOf(sourceRecord.getSizes(),
                sourceRecord.getSizes().length);
        compressedRecord.setMaxSizes(sourceRecord.getMaxSizes());
        compressedRecord.setProperties(sourceRecord.getProperties());
        compressedRecord.setMinIndex(sourceRecord.getMinIndex());
        compressedRecord.setGroup(sourceRecord.getGroup());
        if (sourceRecord.getDataAttributes() != null) {
            compressedRecord.setDataAttributes(new HashMap<String, Object>(
                    sourceRecord.getDataAttributes()));
        }
        compressedRecord.setFillValue(sourceRecord.getFillValue());
        compressedRecord.setMaxChunkSize(sourceRecord.getMaxChunkSize());
        compressedRecord
                .setCorrelationObject(sourceRecord.getCorrelationObject());
        return compressedRecord;
    }

    /*
     * use non chunked compression mode if the flag directs you to do so or the
     * total size of the record is less than the chunk area
     */
    private static CompressedChunkedDataRecord convertShort(
            ShortDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.SHORT);

        short[] orgData = inRecord.getShortData();
        boolean debug = logger.isDebugEnabled();
        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedShorts(orgData);

            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkShort(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    private static CompressedChunkedDataRecord convertInt(
            IntegerDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.INT);

        int[] orgData = inRecord.getIntData();
        boolean debug = logger.isDebugEnabled();

        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedInts(orgData);
            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkInt(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
            if (debug) {
                logger.debug("Number of chunks added: " + chunks.size());
            }
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    private static CompressedChunkedDataRecord convertLong(
            LongDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.LONG);
        boolean debug = logger.isDebugEnabled();

        long[] orgData = inRecord.getLongData();

        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedLongs(orgData);
            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkLong(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
            if (debug) {
                logger.debug("Number of chunks added: " + chunks.size());
            }
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    private static CompressedChunkedDataRecord convertFloat(
            FloatDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.FLOAT);
        boolean debug = logger.isDebugEnabled();

        float[] orgData = inRecord.getFloatData();

        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedFloats(orgData);
            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkFloat(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
            if (debug) {
                logger.debug("Number of chunks added: " + chunks.size());
            }
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    private static CompressedChunkedDataRecord convertDouble(
            DoubleDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.DOUBLE);
        boolean debug = logger.isDebugEnabled();
        double[] orgData = inRecord.getDoubleData();

        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedDoubles(orgData);
            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkDouble(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
            if (debug) {
                logger.debug("Number of chunks added: " + chunks.size());
            }
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    private static CompressedChunkedDataRecord convertByte(
            ByteDataRecord inRecord) throws IOException {
        CompressedChunkedDataRecord compressedRecord = cloneMetadataToCCDR(
                inRecord);
        compressedRecord.setType(Type.BYTE);

        byte[] orgData = inRecord.getByteData();
        boolean debug = logger.isDebugEnabled();
        List<CompressedChunk> chunks = new ArrayList<>();

        // check if the total size is more than the CHUNK*CHUNK
        if (orgData.length <= (CHUNK_SIZE * CHUNK_SIZE)) {
            if (debug) {
                logger.debug("Use Non Chunked mode");
            }
            byte[] ret = CompressedChunkedDataRecordUtil
                    .compressedBytes(orgData);
            long[] dims = inRecord.getSizes();
            int[] offset = new int[dims.length];
            for (int i = 0; i < dims.length; i++) {
                offset[0] = 0;
            }
            chunks.add(new CompressedChunk(ret,
                    ChunkFactory.reverseLongDims(dims), offset));

            compressedRecord.setCompressedChunks(chunks);
        } else {
            chunks = CompressedChunkedDataRecordUtil.chunkByte(orgData,
                    inRecord.getSizes());
            compressedRecord.setCompressedChunks(chunks);
            if (debug) {
                logger.debug("Number of chunks added: " + chunks.size());
            }
        }
        return fillInDataTypeAttribute(compressedRecord, chunks);

    }

    /**
     * Before we write the compressed data into HDF5, we need to leave a marker
     * in the attributes that says this is a "compressed record and the original
     * type is int/float/..." And other markers
     *
     */
    private static CompressedChunkedDataRecord fillInDataTypeAttribute(
            CompressedChunkedDataRecord compressedRecord,
            List<CompressedChunk> compressedChunks) {
        Map<String, Object> attributes = compressedRecord.getDataAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }

        attributes.put(CompressedChunkedDataRecord.COMPRESSED_TYPE,
                compressedRecord.getType());
        attributes.put(CompressedChunkedDataRecord.CDR_CHUNK_SIZE, CHUNK_SIZE);
        attributes.put(CompressedChunkedDataRecord.DIMENSION,
                compressedRecord.getDimension());
        if (compressedRecord.getSizes() != null) {
            attributes.put(CompressedChunkedDataRecord.DATA_SIZES,
                    compressedRecord.getSizes());
        }

        attributes.put(CompressedChunkedDataRecord.NUM_CHUNKS,
                compressedRecord.getCompressedChunks().size());

        if (compressedRecord.getMaxSizes() != null) {
            attributes.put(CompressedChunkedDataRecord.MAX_DATA_SIZES,
                    compressedRecord.getMaxSizes());
        }

        List<int[]> chunkSizes = new ArrayList<>();
        List<int[]> chunkOffsets = new ArrayList<>();
        List<CompressedChunk> chunks = compressedRecord.getCompressedChunks();
        for (CompressedChunk chunk : chunks) {
            chunkSizes.add(chunk.getSizes());
            chunkOffsets.add(chunk.getOffsets());
        }
        attributes.put(CompressedChunkedDataRecord.CHUNK_DIMS, chunkSizes);
        attributes.put(CompressedChunkedDataRecord.CHUNK_OFFSETS, chunkOffsets);
        compressedRecord.setDataAttributes(attributes);

        return compressedRecord;

    }

}
