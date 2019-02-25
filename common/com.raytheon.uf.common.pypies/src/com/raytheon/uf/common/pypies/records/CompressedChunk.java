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

import java.util.Arrays;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 *
 * Record containing gzip compressed version of data chunk. This is intended to
 * reduce the bandwidth usage when communicating with pypies. The record will
 * contain a list of such chunks
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
public class CompressedChunk implements ISerializableObject {

    @DynamicSerializeElement
    protected byte[] data;

    @DynamicSerializeElement
    protected int[] sizes;

    @DynamicSerializeElement
    protected int[] offsets;

    @DynamicSerializeElement
    protected int index;

    /*
     * A shallow copy constructor.
     */
    public CompressedChunk(byte[] data, int[] sizes, int[] offsets) {
        super();
        this.data = data;
        this.sizes = sizes;
        this.offsets = offsets;
    }

    public CompressedChunk(byte[] data, int[] sizes, int[] offsets, int index) {
        super();
        this.data = data;
        this.sizes = sizes;
        this.offsets = offsets;
        this.index = index;
    }

    public CompressedChunk() {

    }

    @Override
    public String toString() {
        return "CompressedChunk sizes=" + Arrays.toString(sizes) + ", offsets="
                + Arrays.toString(offsets) + "]" + ", index=" + index;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int[] getSizes() {
        return sizes;
    }

    public void setSizes(int[] sizes) {
        this.sizes = sizes;
    }

    public int[] getOffsets() {
        return offsets;
    }

    public void setOffsets(int[] offsets) {
        this.offsets = offsets;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

}
