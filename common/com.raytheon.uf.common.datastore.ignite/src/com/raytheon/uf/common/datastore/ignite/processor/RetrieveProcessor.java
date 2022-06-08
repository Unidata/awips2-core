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

import java.awt.Point;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.cache.processor.EntryProcessor;
import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.Request.Type;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.time.util.IPerformanceTimer;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Retrieve some datasets from a group.
 *
 * The processing of the various request types (e.g. POINT or XLINE) should all
 * match what is done in HDF5OpManager.py.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------
 * Jun 03, 2019  7628     bsteffen  Initial creation
 * Mar 26, 2020  8074     bsteffen  Ensure fill value is copied.
 * Jun 10, 2021  8450     mapeters  Add performance logging
 * Sep 23, 2021  8608     mapeters  Add metadata handling
 * Apr 13, 2022  8845     njensen   Fix dimension value in processPoint()
 * Jun 08, 2022  8866     mapeters  Update requests to better match pypies
 *
 * </pre>
 *
 * @author bsteffen
 */
public class RetrieveProcessor implements
        EntryProcessor<DataStoreKey, DataStoreValue, List<IDataRecord>> {

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler(RetrieveProcessor.class.getSimpleName() + ":");

    protected Request request = Request.ALL;

    /** Optional, null means all */
    protected Set<String> datasets = null;

    public RetrieveProcessor() {

    }

    public RetrieveProcessor(Request request) {
        this.request = request;
    }

    public RetrieveProcessor(String datasets, Request request) {
        this.request = request;
        this.datasets = Collections.singleton(datasets);
    }

    public RetrieveProcessor(Set<String> datasets, Request request) {
        this.request = request;
        this.datasets = datasets;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    public Set<String> getDatasets() {
        return datasets;
    }

    public void setDatasets(Set<String> datasets) {
        this.datasets = datasets;
    }

    @Override
    public List<IDataRecord> process(
            MutableEntry<DataStoreKey, DataStoreValue> entry, Object... args)
            throws EntryProcessorException {
        if (!entry.exists()) {
            throw new EntryProcessorException(
                    "No data found for " + entry.getKey());
        }

        IPerformanceTimer timer = TimeUtil.getPerformanceTimer();
        timer.start();

        RecordAndMetadata[] rms = entry.getValue().getRecordsAndMetadata();
        List<IDataRecord> result = new ArrayList<>();

        if (datasets == null) {
            for (RecordAndMetadata rm : rms) {
                IDataRecord record = rm.getRecord();
                result.add(applyRequest(record));
            }
        } else {
            for (RecordAndMetadata rm : rms) {
                IDataRecord record = rm.getRecord();
                if (datasets.contains(record.getName())) {
                    result.add(applyRequest(record));
                }
            }
        }

        timer.stop();
        perfLog.logDuration("Processing " + entry.getKey(),
                timer.getElapsedTime());
        return result;
    }

    public IDataRecord applyRequest(IDataRecord record) {
        if (record == null) {
            return null;
        }
        IDataRecord result = null;
        if (request.getType() == Type.ALL) {
            return record;
        } else if (request.getType() == Type.SLAB) {
            result = processSlab(record, request.getMinIndexForSlab(),
                    request.getMaxIndexForSlab());
        } else if (request.getType() == Type.YLINE) {
            result = processYLine(record, request.getIndices());
        } else if (request.getType() == Type.POINT) {
            result = processPoint(record, request.getPoints());
        } else if (request.getType() == Type.XLINE) {
            result = processXLine(record, request.getIndices());
        } else {
            throw new UnsupportedOperationException(
                    "Cannot handle request of type: " + request);
        }
        result.setDataAttributes(record.getDataAttributes());
        result.setFillValue(record.getFillValue());
        return result;

    }

    protected static IDataRecord processSlab(IDataRecord record, int[] minIndex,
            int[] maxIndex) {
        long[] dims = record.getSizes();
        Object array = record.getDataObject();

        if (dims.length < minIndex.length) {
            throw new UnsupportedOperationException(
                    "Unsupported slab request from " + Arrays.toString(minIndex)
                            + " to " + Arrays.toString(maxIndex)
                            + " for shape of record " + record.getGroup() + "/"
                            + record.getName() + ": " + Arrays.toString(dims));
        }

        Object newArray;
        long[] newDims;
        if (dims.length == 1) {
            int min = normalizeSlabIndex(minIndex[0], dims[0]);
            int max = normalizeSlabIndex(maxIndex[0], dims[0]);
            if (max < min) {
                max = min;
            }
            int newLength = max - min;
            newDims = new long[] { newLength };
            newArray = Array.newInstance(array.getClass().getComponentType(),
                    newLength);
            System.arraycopy(array, min, newArray, 0, newLength);
        } else if (dims.length == 2) {
            int minX, maxX, minY, maxY;
            if (minIndex.length == 1) {
                // Apply requested indices to y-values and include all x-values
                minX = 0;
                maxX = (int) dims[0];
                minY = normalizeSlabIndex(minIndex[0], dims[1]);
                maxY = normalizeSlabIndex(maxIndex[0], dims[1]);
            } else {
                minX = normalizeSlabIndex(minIndex[0], dims[0]);
                maxX = normalizeSlabIndex(maxIndex[0], dims[0]);
                minY = normalizeSlabIndex(minIndex[1], dims[1]);
                maxY = normalizeSlabIndex(maxIndex[1], dims[1]);
            }
            if (maxX < minX) {
                maxX = minX;
            }
            if (maxY < minY) {
                maxY = minY;
            }

            newDims = new long[] { maxX - minX, maxY - minY };
            int newLength = (int) (newDims[0] * newDims[1]);
            newArray = Array.newInstance(array.getClass().getComponentType(),
                    newLength);
            for (int i = minY; i < maxY; i++) {
                int srcIndex = (int) (i * dims[0] + minX);
                int destIndex = (int) ((i - minY) * newDims[0]);
                System.arraycopy(array, srcIndex, newArray, destIndex,
                        (int) newDims[0]);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record " + record.getGroup() + "/"
                            + record.getName() + ": " + Arrays.toString(dims));

        }

        return DataStoreFactory.createStorageRecord(record.getName(),
                record.getGroup(), newArray, newDims.length, newDims);
    }

    protected static IDataRecord processYLine(IDataRecord record,
            int[] indices) {
        long[] dims = record.getSizes();

        if (dims.length == 1) {
            return process1DIndexRequest(record, indices);
        } else if (dims.length == 2) {
            normalizeLineIndices(indices, dims[1]);
            long[] newDims = new long[] { dims[0], indices.length };
            int newLength = (int) (dims[0] * indices.length);
            Object array = record.getDataObject();
            Object newArray = Array.newInstance(
                    array.getClass().getComponentType(), newLength);
            for (int i = 0; i < indices.length; i++) {
                int srcIndex = (int) (indices[i] * dims[0]);
                int destIndex = (int) (i * newDims[0]);
                System.arraycopy(array, srcIndex, newArray, destIndex,
                        (int) newDims[0]);
            }

            return DataStoreFactory.createStorageRecord(record.getName(),
                    record.getGroup(), newArray, newDims.length, newDims);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record " + record.getGroup() + "/"
                            + record.getName() + ": " + Arrays.toString(dims));
        }
    }

    private static IDataRecord processPoint(IDataRecord record,
            Point[] points) {
        long[] dims = record.getSizes();

        if (dims.length == 1) {
            int[] indices = new int[points.length];
            for (int i = 0; i < points.length; ++i) {
                indices[i] = points[i].x;
            }

            return process1DIndexRequest(record, indices);
        } else if (dims.length == 2) {
            long[] newDims = new long[] { 1, points.length };
            Object array = record.getDataObject();
            Object newArray = Array.newInstance(
                    array.getClass().getComponentType(), points.length);
            for (int i = 0; i < points.length; i++) {
                Point point = points[i];
                if (point.x < 0 || point.x > dims[0] || point.y < 0
                        || point.y > dims[1]) {
                    /*
                     * We have to specifically check for this because the index
                     * calculated below may incorrectly be in bounds in the 1d
                     * array even if the point isn't in the 2d array, e.g. if we
                     * request (3, 3) from a 1x20 record.
                     */
                    throwPointOutOfBoundsException(point, record);
                } else if (point.x == dims[0] || point.y == dims[1]) {
                    /*
                     * For some reason, pypies lets you request an x and/or y
                     * index that is 1 spot out of bounds and wraps it around to
                     * the first column/row. It's unknown if anything actually
                     * relies on this functionality.
                     */
                    if (record.getMaxSizes() == null) {
                        /*
                         * But pypies only supports wrapping around if the
                         * record is resizable.
                         */
                        throwPointOutOfBoundsException(point, record);
                    } else if (points.length == 1) {
                        /*
                         * And if you only request a single point and it's out
                         * of bounds, then you just get the fill value.
                         */
                        Array.set(newArray, i, record.getFillValue());
                    } else {
                        // Wrap around
                        int x = point.x;
                        int y = point.y;
                        if (x == dims[0]) {
                            // Move to start of next row
                            y += 1;
                            x = 0;
                        }
                        if (y >= dims[1]) {
                            /*
                             * Wrap around to first row (or second if x was also
                             * out of bounds)
                             */
                            y = (int) (y % dims[1]);
                        }

                        int index = (int) (y * dims[0] + x);
                        System.arraycopy(array, index, newArray, i, 1);
                    }
                } else {
                    int index = (int) (point.y * dims[0] + point.x);
                    System.arraycopy(array, index, newArray, i, 1);
                }
            }

            return DataStoreFactory.createStorageRecord(record.getName(),
                    record.getGroup(), newArray, newDims.length, newDims);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record " + record.getGroup() + "/"
                            + record.getName() + ": " + Arrays.toString(dims));
        }
    }

    protected static IDataRecord processXLine(IDataRecord record,
            int[] indices) {
        long[] dims = record.getSizes();

        if (dims.length == 1) {
            return process1DIndexRequest(record, indices);
        } else if (dims.length == 2) {
            normalizeLineIndices(indices, dims[0]);
            long[] newDims = new long[] { indices.length, dims[1] };
            int newLength = (int) (dims[1] * indices.length);
            Object array = record.getDataObject();
            Object newArray = Array.newInstance(
                    array.getClass().getComponentType(), newLength);
            for (int i = 0; i < dims[1]; i++) {
                int c = 0;
                for (int j : indices) {
                    int index = (int) (i * dims[0] + j);
                    int newIndex = (int) (i * newDims[0] + c);
                    System.arraycopy(array, index, newArray, newIndex, 1);
                    c += 1;
                }
            }

            return DataStoreFactory.createStorageRecord(record.getName(),
                    record.getGroup(), newArray, newDims.length, newDims);
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record " + record.getGroup() + "/"
                            + record.getName() + ": " + Arrays.toString(dims));
        }
    }

    private static IDataRecord process1DIndexRequest(IDataRecord record,
            int[] indices) {
        Object array = record.getDataObject();
        int recordLength = Array.getLength(array);
        long[] newDims = new long[] { indices.length };
        Object newArray = Array.newInstance(array.getClass().getComponentType(),
                indices.length);
        for (int i = 0; i < indices.length; ++i) {
            int recordIndex = indices[i];
            if (recordIndex == recordLength) {
                /*
                 * For some reason, pypies lets you request an index that is 1
                 * spot out of bounds and wraps it around to 0. It's unknown if
                 * anything actually relies on this functionality.
                 */
                if (record.getMaxSizes() == null) {
                    /*
                     * But pypies only supports wrapping around if the record is
                     * resizable.
                     */
                    throw new IndexOutOfBoundsException("Index " + recordIndex
                            + " is out of bounds for record: "
                            + record.getGroup() + "/" + record.getName() + ": "
                            + Arrays.toString(record.getSizes()));
                } else if (indices.length == 1) {
                    /*
                     * And if you only request a single index and it's out of
                     * bounds, then you just get the fill value.
                     */
                    Array.set(newArray, i, record.getFillValue());
                } else {
                    // Wrap around to index 0
                    recordIndex = 0;
                    System.arraycopy(array, recordIndex, newArray, i, 1);
                }
            } else {
                System.arraycopy(array, recordIndex, newArray, i, 1);
            }
        }

        return DataStoreFactory.createStorageRecord(record.getName(),
                record.getGroup(), newArray, newDims.length, newDims);
    }

    private static void normalizeLineIndices(int[] indices, long dimSize) {
        for (int i = 0; i < indices.length; ++i) {
            int index = indices[i];
            if (index < 0 && index >= -dimSize) {
                index += dimSize;
                indices[i] = index;
            }
        }
        Arrays.sort(indices);
        for (int i = 1; i < indices.length; ++i) {
            if (indices[i] == indices[i - 1]) {
                throw new UnsupportedOperationException("Index " + indices[i]
                        + " cannot be requested multiple times");
            }
        }
    }

    private static int normalizeSlabIndex(int index, long dimSize) {
        if (index < 0) {
            if (index >= -dimSize) {
                index += dimSize;
            } else {
                index = 0;
            }
        } else if (index > dimSize) {
            index = (int) dimSize;
        }
        return index;
    }

    private static void throwPointOutOfBoundsException(Point point,
            IDataRecord record) throws IndexOutOfBoundsException {
        throw new IndexOutOfBoundsException("Point " + point
                + " is out of bounds for record: " + record.getGroup() + "/"
                + record.getName() + ": " + Arrays.toString(record.getSizes()));
    }
}
