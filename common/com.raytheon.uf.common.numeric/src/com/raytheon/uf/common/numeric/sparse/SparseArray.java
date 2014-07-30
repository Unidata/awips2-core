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

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.numeric.dest.DataDestination;
import com.raytheon.uf.common.numeric.source.DataSource;

/**
 * Abstract parent for sparse array implementations. Sparse arrays are optimized
 * for data that exists in a large logical array, but only a small portion of
 * the array has useful data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 29, 2014 3463       bclement     Initial creation
 * Jul 30, 2014 3463       bclement     changed blockMap to be hashmap
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public abstract class SparseArray<T> implements DataSource, DataDestination {

    public static final int DEFAULT_BLOCK_SIZE = 256;

    protected final int nx;

    protected final int ny;

    protected final int blockSize;

    protected final Map<Integer, T> blockMap = new HashMap<Integer, T>();

    private T cache = null;

    private int cacheIndex = -1;

    /**
     * @param nx
     *            width
     * @param ny
     *            height
     * @param blockSize
     *            size of contiguous chunks of data. lower block sizes reduce
     *            wasted ranges in the block that are no-data values but incurs
     *            performance costs when the array is traversed.
     * @see #DEFAULT_BLOCK_SIZE
     */
    public SparseArray(int nx, int ny, int blockSize) {
        this.nx = nx;
        this.ny = ny;
        this.blockSize = blockSize;
    }

    /**
     * Get block of data that contains element for index. Does not create the
     * block if it does not exist.
     * 
     * @param index
     *            index into logical 1D array
     * @return data block or null if not found
     * @see #getBlockIndex(int)
     */
    protected T getBlockReadOnly(int index) {
        T rval;
        if (index == cacheIndex) {
            rval = cache;
        } else {
            cacheIndex = index / blockSize;
            rval = cache = blockMap.get(cacheIndex);
        }
        return rval;
    }

    /**
     * Get block of data that contains element for index. Creates the block if
     * it does not exist.
     * 
     * @param index
     *            index into logical 1D array
     * @return
     * @see #getBlockIndex(int)
     */
    protected T getBlockReadWrite(int index) {
        T rval;
        if (index == cacheIndex && cache != null) {
            rval = cache;
        } else {
            cacheIndex = index / blockSize;
            rval = blockMap.get(cacheIndex);
            if (rval == null) {
                rval = createBlock(blockSize);
                blockMap.put(cacheIndex, rval);
            }
            cache = rval;
        }
        return rval;
    }

    /**
     * Gets the index into the 1D array that is acting like a 2D array
     * 
     * @param x
     * @param y
     * @return
     */
    protected int getIndex(int x, int y) {
        return (nx * y) + x;
    }

    /**
     * Get the index into data block
     * 
     * @param index
     *            index into logical 1D array
     * @return
     */
    protected int getBlockIndex(int index) {
        return index % blockSize;
    }

    /**
     * Create a new block of provided size
     * 
     * @param blockSize
     * @return
     */
    protected abstract T createBlock(int blockSize);

    /**
     * Increment the value at the given index
     * 
     * @param x
     * @param y
     * @param value
     *            may be negative
     */
    public abstract void add(int x, int y, double value);

}
