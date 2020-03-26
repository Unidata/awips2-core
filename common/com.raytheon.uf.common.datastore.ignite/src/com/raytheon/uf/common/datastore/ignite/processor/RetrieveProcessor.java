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
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;

/**
 * 
 * Retrieve some datasets from a group.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jun 03, 2019  7628     bsteffen  Initial creation
 * Mar 26, 2020  8074     bsteffen  Ensure fill value is copied.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class RetrieveProcessor implements
        EntryProcessor<DataStoreKey, DataStoreValue, List<IDataRecord>> {

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
        IDataRecord[] records = entry.getValue().getRecords();
        List<IDataRecord> result = new ArrayList<>();

        if (datasets == null) {
            for (IDataRecord record : records) {
                result.add(applyRequest(record));
            }
        } else {
            for (IDataRecord record : entry.getValue().getRecords()) {
                if (datasets.contains(record.getName())) {
                    result.add(applyRequest(record));
                }
            }
        }
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

        if (dims.length != 2) {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record : " + Arrays.toString(dims));
        }
        int minX = minIndex[0];
        int maxX = maxIndex[0];
        int minY = minIndex[1];
        int maxY = maxIndex[1];

        Object array = record.getDataObject();

        long[] newDims = { maxX - minX, maxY - minY };
        int newLength = (int) (newDims[0] * newDims[1]);
        Object newArray = Array.newInstance(array.getClass().getComponentType(),
                newLength);
        for (int i = minY; i < maxY; i++) {
            int srcIndex = (int) (i * dims[0] + minX);
            int destIndex = (int) ((i - minY) * newDims[0]);
            System.arraycopy(array, srcIndex, newArray, destIndex,
                    (int) newDims[0]);
        }
        String name = record.getName();
        String group = record.getGroup();
        return DataStoreFactory.createStorageRecord(name, group, newArray, 2,
                newDims);
    }

    protected static IDataRecord processYLine(IDataRecord record,
            int[] indices) {
        long[] dims = record.getSizes();
        Object array = record.getDataObject();
        long[] newDims;
        Object newArray;
        if (dims.length == 1) {
            newDims = new long[] { indices.length };
            int newLength = indices.length;
            newArray = Array.newInstance(array.getClass().getComponentType(),
                    newLength);
            for (int i = 0; i < indices.length; i++) {
                System.arraycopy(array, indices[i], newArray, i, 1);
            }
        } else if (dims.length == 2) {
            newDims = new long[] { dims[0], indices.length };
            int newLength = (int) (dims[0] * indices.length);
            newArray = Array.newInstance(array.getClass().getComponentType(),
                    newLength);
            for (int i = 0; i < indices.length; i++) {
                int srcIndex = (int) (indices[i] * dims[0]);
                int destIndex = (int) (i * newDims[0]);
                System.arraycopy(array, srcIndex, newArray, destIndex,
                        (int) newDims[0]);
            }
        } else {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record : " + Arrays.toString(dims));
        }
        String name = record.getName();
        String group = record.getGroup();
        return DataStoreFactory.createStorageRecord(name, group, newArray,
                dims.length, newDims);
    }

    private IDataRecord processPoint(IDataRecord record, Point[] points) {
        long[] dims = record.getSizes();

        if (dims.length != 2) {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record : " + Arrays.toString(dims));
        }
        Object array = record.getDataObject();

        long[] newDims = { 1, points.length };
        int newLength = points.length;
        Object newArray = Array.newInstance(array.getClass().getComponentType(),
                newLength);
        for (int i = 0; i < points.length; i++) {
            int index = (int) (points[i].y * dims[0] + points[i].x);
            System.arraycopy(array, index, newArray, i, 1);
        }

        String name = record.getName();
        String group = record.getGroup();
        return DataStoreFactory.createStorageRecord(name, group, newArray, 1,
                newDims);
    }

    protected static IDataRecord processXLine(IDataRecord record,
            int[] indices) {
        long[] dims = record.getSizes();
        if (dims.length != 2) {
            throw new UnsupportedOperationException(
                    "Unsupported shape of record : " + Arrays.toString(dims));
        }
        Object array = record.getDataObject();

        long[] newDims = new long[] { indices.length, dims[1] };
        int newLength = (int) (dims[1] * indices.length);
        Object newArray = Array.newInstance(array.getClass().getComponentType(),
                newLength);
        for (int i = 0; i < dims[1]; i++) {
            int c = 0;
            for (int j : indices) {
                int index = (int) (i * dims[0] + j);
                int newIndex = (int) (i * newDims[0] + c);
                System.arraycopy(array, index, newArray, newIndex, 1);
                c += 1;
            }
        }
        String name = record.getName();
        String group = record.getGroup();
        return DataStoreFactory.createStorageRecord(name, group, newArray,
                dims.length, newDims);
    }

}
