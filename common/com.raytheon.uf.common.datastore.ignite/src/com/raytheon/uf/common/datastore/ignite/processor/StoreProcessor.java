/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite.processor;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import org.apache.ignite.resources.CacheNameResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.DuplicateRecordStorageException;
import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.datastorage.records.StringDataRecord;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;
import com.raytheon.uf.common.datastore.ignite.IgniteUtils;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 *
 * Store data in a group. The arguments are the new {@link IDataRecord}s which
 * should be added to the entry.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 03, 2019  7628     bsteffen  Initial creation
 * Mar 27, 2020  8099     bsteffen  Throw DuplicateRecordStorageException for
 *                                  duplicate records.
 * Apr 02, 2020  8075     bsteffen  Extract merge for reuse elsewhere.
 * Jun 10, 2021  8450     mapeters  Add logging
 * Sep 23, 2021  8608     mapeters  Add metadata handling
 * Jan 25, 2022  8608     mapeters  Support write-through appends better
 *
 *
 * </pre>
 *
 * @author bsteffen
 */
public class StoreProcessor
        implements EntryProcessor<DataStoreKey, DataStoreValue, StorageStatus> {

    private static final Logger logger = LoggerFactory
            .getLogger(StoreProcessor.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(StoreProcessor.class.getSimpleName() + ":");

    @CacheNameResource
    private String cacheName;

    private StoreOp op = StoreOp.STORE_ONLY;

    public StoreProcessor() {

    }

    public StoreProcessor(StoreOp op) {
        this.op = op;
    }

    public StoreOp getOp() {
        return op;
    }

    public void setOp(StoreOp op) {
        this.op = op;
    }

    @Override
    public StorageStatus process(
            MutableEntry<DataStoreKey, DataStoreValue> entry, Object... args)
            throws EntryProcessorException {
        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        DataStoreKey key = entry.getKey();
        String msg = "Processing " + cacheName + " key: " + key;
        logger.info(msg);

        StorageStatus status = new StorageStatus();
        status.setOperationPerformed(op);
        RecordAndMetadata[] recordsAndMetadata = new RecordAndMetadata[args.length];
        for (int i = 0; i < args.length; i += 1) {
            recordsAndMetadata[i] = (RecordAndMetadata) args[i];
        }
        if (op == StoreOp.APPEND) {
            status.setIndexOfAppend(new long[recordsAndMetadata.length]);
        }
        try {
            if (entry.exists()) {
                DataStoreValue newValue = merge(
                        Arrays.asList(entry.getValue().getRecordsAndMetadata()),
                        Arrays.asList(recordsAndMetadata), op, status);
                entry.setValue(newValue);
            } else {
                for (RecordAndMetadata rm : recordsAndMetadata) {
                    IDataRecord record = rm.getRecord();
                    if (isPartial(record)) {
                        rm.setRecord(expandPartial(record));
                    }
                }
                entry.setValue(new DataStoreValue(recordsAndMetadata));
            }
        } catch (StorageException e) {
            status.setExceptions(new StorageException[] { e });
        }

        if (op == StoreOp.APPEND) {
            entry.getValue()
                    .setLastAppendRecordsAndMetadata(recordsAndMetadata);
        }

        timer.stop();
        perfLog.logDuration(msg, timer.getElapsedTime());
        return status;
    }

    public static DataStoreValue merge(List<RecordAndMetadata> oldValues,
            List<RecordAndMetadata> newValues, StoreOp op, StorageStatus status)
            throws StorageException {
        Map<String, IDataRecord> byName = new HashMap<>();
        for (RecordAndMetadata rm : oldValues) {
            IDataRecord record = rm.getRecord();
            byName.put(record.getName(), record);
        }

        for (int i = 0; i < newValues.size(); i++) {
            IDataRecord record = newValues.get(i).getRecord();
            String name = record.getName();
            boolean partial = isPartial(record);
            IDataRecord previous;
            if (partial) {
                previous = byName.get(name);
            } else {
                previous = byName.put(name, record);
            }
            if (previous != null) {
                if (op == StoreOp.STORE_ONLY) {
                    throw new DuplicateRecordStorageException(
                            "Duplicate record: " + name, record);
                } else if (op == StoreOp.APPEND) {
                    IDataRecord merged = append(status, i, previous, record);
                    byName.put(name, merged);
                } else if (partial) {
                    insertPartial(previous, record);
                }
            } else if (partial) {
                byName.put(name, expandPartial(record));
            }
        }

        DataStoreValue value = DataStoreValue
                .createWithoutMetadata(byName.values());
        IgniteUtils.updateMetadata(value, oldValues);
        IgniteUtils.updateMetadata(value, newValues);
        return value;
    }

    protected static IDataRecord append(StorageStatus status, int recordIndex,
            IDataRecord record, IDataRecord appendRecord) {
        Object array = record.getDataObject();
        Object appendArray = appendRecord.getDataObject();
        int length = Array.getLength(array);
        int appendLength = Array.getLength(appendArray);
        int newLength = length + appendLength;
        Object newArray = Array.newInstance(array.getClass().getComponentType(),
                newLength);
        System.arraycopy(array, 0, newArray, 0, length);
        System.arraycopy(appendArray, 0, newArray, length, appendLength);

        long[] sizes = record.getSizes();
        long[] appendSizes = appendRecord.getSizes();
        long[] newSizes = Arrays.copyOf(sizes, sizes.length);
        newSizes[newSizes.length - 1] += appendSizes[appendSizes.length - 1];

        String name = record.getName();
        String group = record.getGroup();
        int dimension = record.getDimension();

        status.getIndexOfAppend()[recordIndex] = sizes[sizes.length - 1];

        IDataRecord result = DataStoreFactory.createStorageRecord(name, group,
                newArray, dimension, newSizes);
        result.setMaxChunkSize(record.getMaxChunkSize());
        result.setMaxSizes(record.getMaxSizes());
        result.setDataAttributes(record.getDataAttributes());
        if (result instanceof StringDataRecord) {
            StringDataRecord recordStr = (StringDataRecord) record;
            StringDataRecord appendRecordStr = (StringDataRecord) appendRecord;
            StringDataRecord resultStr = ((StringDataRecord) result);
            /*
             * In theory the lengths should always be the same, but when data is
             * read back form pypies the length is omitted and defaults to 0.
             */
            resultStr.setMaxLength(Math.max(recordStr.getMaxLength(),
                    appendRecordStr.getMaxLength()));
        }
        return result;
    }

    public static boolean isPartial(IDataRecord record) {
        long[] minIndex = record.getMinIndex();
        return (minIndex != null && minIndex.length > 0);
    }

    protected static IDataRecord expandPartial(IDataRecord partial) {
        return insertPartial(null, partial);
    }

    /**
     * Insert a partial record into a larger full record.
     *
     * @param full
     *            - The full record to inert into, may be null which creates a
     *            new record.
     * @param partial
     *            - The partial record to insert into the full record
     * @return a full record, if full was null this will be a new record.
     */
    protected static IDataRecord insertPartial(IDataRecord full,
            IDataRecord partial) {
        long[] partialDims = partial.getSizes();
        if (partialDims.length != 2) {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record : "
                            + Arrays.toString(partialDims));
        }
        long[] minIndex = partial.getMinIndex();
        long[] fullDims = partial.getMaxSizes();

        Object partialArray = partial.getDataObject();

        Object fullArray = null;
        if (full == null) {
            int newLength = (int) (fullDims[0] * fullDims[1]);
            fullArray = Array.newInstance(
                    partialArray.getClass().getComponentType(), newLength);
            arrayFill(fullArray, partial.getFillValue());
        } else {
            fullArray = full.getDataObject();
        }
        for (int i = 0; i < partialDims[1]; i++) {
            int srcIndex = (int) (i * partialDims[0]);
            int destIndex = (int) (((i + minIndex[1]) * fullDims[0])
                    + minIndex[0]);
            System.arraycopy(partialArray, srcIndex, fullArray, destIndex,
                    (int) partialDims[0]);
        }
        if (full == null) {
            String name = partial.getName();
            String group = partial.getGroup();
            return DataStoreFactory.createStorageRecord(name, group, fullArray,
                    2, fullDims);
        } else {
            return full;
        }
    }

    protected static void arrayFill(Object array, Number fill) {
        if (array instanceof byte[]) {
            Arrays.fill((byte[]) array, fill.byteValue());
        } else if (array instanceof short[]) {
            Arrays.fill((short[]) array, fill.shortValue());
        } else if (array instanceof int[]) {
            Arrays.fill((int[]) array, fill.intValue());
        } else if (array instanceof long[]) {
            Arrays.fill((long[]) array, fill.longValue());
        } else if (array instanceof float[]) {
            Arrays.fill((float[]) array, fill.floatValue());
        } else if (array instanceof double[]) {
            Arrays.fill((double[]) array, fill.doubleValue());
        } else if (array instanceof Number[]) {
            Arrays.fill((Number[]) array, fill);
        } else {
            throw new IllegalArgumentException(
                    "Cannot fill array [" + array + "] with " + fill);
        }
    }

}
