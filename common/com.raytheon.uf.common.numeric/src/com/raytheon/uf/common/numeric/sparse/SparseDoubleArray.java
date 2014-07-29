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
package com.raytheon.uf.common.numeric.sparse;

import java.util.Arrays;

/**
 * SparseArray implementation for arrays of double primitives
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 29, 2014 3463       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class SparseDoubleArray extends SparseArray<double[]> {

    public static final double DEFAULT_FILL_VALUE = 0;

    private final double fillValue;

    /**
     * @param nx
     * @param ny
     * @see SparseDoubleArray(int, int, double, int)
     * @see SparseDoubleArray#DEFAULT_FILL_VALUE
     * @see SparseArray#DEFAULT_BLOCK_SIZE
     */
    public SparseDoubleArray(int nx, int ny) {
        this(nx, ny, DEFAULT_FILL_VALUE, DEFAULT_BLOCK_SIZE);
    }

    /**
     * @param nx
     * @param ny
     * @param fillValue
     *            no-data value
     * @param blockSize
     * @see SparseArray#SparseArray(int, int, int)
     */
    public SparseDoubleArray(int nx, int ny, double fillValue, int blockSize) {
        super(nx, ny, blockSize);
        this.fillValue = fillValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.numeric.source.DataSource#getDataValue(int,
     * int)
     */
    @Override
    public double getDataValue(int x, int y) {
        int index = getIndex(x, y);
        double[] block = getBlockReadOnly(index);
        double rval = fillValue;
        if (block != null) {
            rval = block[getBlockIndex(index)];
        }
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.numeric.dest.DataDestination#setDataValue(double,
     * int, int)
     */
    @Override
    public void setDataValue(double dataValue, int x, int y) {
        int index = getIndex(x, y);
        double[] block = getBlockReadWrite(index);
        block[getBlockIndex(index)] = (double) dataValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.numeric.sparse.SparseArray#createBlock(int)
     */
    @Override
    protected double[] createBlock(int blockSize) {
        double[] rval = new double[blockSize];
        if (fillValue != 0) {
            Arrays.fill(rval, fillValue);
        }
        return rval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.numeric.sparse.SparseArray#add(int, int,
     * double)
     */
    @Override
    public void add(int x, int y, double value) {
        int index = getIndex(x, y);
        double[] block = getBlockReadWrite(index);
        block[getBlockIndex(index)] += (double) value;
    }

}
