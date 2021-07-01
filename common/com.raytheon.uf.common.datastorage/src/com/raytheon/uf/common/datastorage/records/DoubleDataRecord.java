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

package com.raytheon.uf.common.datastorage.records;

import java.util.Arrays;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Provides an interface to datasets of type double
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------
 * Sep 08, 2014           kustert   Initial Creation.
 * Apr 24, 2015  4425     nabowle   Bring in.
 * Jun 10, 2021  8450     mapeters  Add serialVersionUID
 *
 * </pre>
 *
 * @author kustert
 */
@DynamicSerialize
public class DoubleDataRecord extends AbstractStorageRecord {

    private static final long serialVersionUID = -7277425766726168679L;

    @DynamicSerializeElement
    protected double[] doubleData;

    public DoubleDataRecord() {
        super();
    }

    /**
     * Constructor
     *
     * @param name
     *            the name of the data
     * @param group
     *            the group inside the file
     * @param doubleData
     *            the double data as 1d array
     * @param dimension
     *            the dimension of the data
     * @param sizes
     *            the length of each dimension
     */
    public DoubleDataRecord(String name, String group, double[] doubleData,
            int dimension, long[] sizes) {
        this.doubleData = doubleData;
        this.group = group;
        this.dimension = dimension;
        this.sizes = sizes;
        this.name = name;
    }

    /**
     * Convenience constructor for single dimension double data
     *
     * @param name
     *            name of the data
     * @param group
     *            the group inside the file
     * @param doubleData
     *            the one dimensional double data
     */
    public DoubleDataRecord(String name, String group, double[] doubleData) {
        this(name, group, doubleData, 1, new long[] { doubleData.length });
    }

    /**
     * @return the doubleData
     */
    public double[] getDoubleData() {
        return doubleData;
    }

    /**
     * @param doubleData
     *            - the doubleData to set
     */
    public void setDoubleData(double[] doubleData) {
        this.doubleData = doubleData;
    }

    @Override
    public Object getDataObject() {
        return this.doubleData;
    }

    @Override
    public boolean validateDataSet() {

        long size = 1;

        for (int i = 0; i < this.dimension; i++) {
            size *= this.sizes[i];
        }

        if (size == this.doubleData.length) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Override toString method to print dimensions
     */
    @Override
    public String toString() {
        return "[dims,data size]=[" + Arrays.toString(sizes) + ","
                + this.doubleData.length + "]";
    }

    @Override
    public void reduce(int[] indices) {
        double[] reducedData = new double[indices.length];
        for (int i = 0; i < reducedData.length; i++) {
            if (indices[i] >= 0) {
                reducedData[i] = doubleData[indices[i]];
            } else {
                reducedData[i] = -9999;
            }
        }
        this.doubleData = reducedData;
        setDimension(1);
        setSizes(new long[] { indices.length });
    }

    @Override
    protected AbstractStorageRecord cloneInternal() {
        DoubleDataRecord record = new DoubleDataRecord();
        if (doubleData != null) {
            record.doubleData = Arrays.copyOf(doubleData, doubleData.length);
        }
        return record;
    }

    @Override
    public int getSizeInBytes() {
        return doubleData == null ? 0 : doubleData.length * 8;
    }

}
