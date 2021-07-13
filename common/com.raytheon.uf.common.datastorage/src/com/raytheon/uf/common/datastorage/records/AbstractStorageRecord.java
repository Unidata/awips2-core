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
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Defines an abstract dataset
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------
 * Feb 08, 2007           chammack  Initial Creation.
 * Dec 31, 2008           chammack  Added correlation object
 * Jun 10, 2021  8450     mapeters  Add serialVersionUID
 *
 * </pre>
 *
 * @author chammack
 */
@DynamicSerialize
public abstract class AbstractStorageRecord implements IDataRecord {

    private static final long serialVersionUID = -8156857671855801312L;

    @DynamicSerializeElement
    protected String name;

    @DynamicSerializeElement
    protected int dimension;

    @DynamicSerializeElement
    protected long[] sizes;

    @DynamicSerializeElement
    protected long[] maxSizes;

    @DynamicSerializeElement
    protected StorageProperties props;

    @DynamicSerializeElement
    protected long[] minIndex;

    @DynamicSerializeElement
    protected String group;

    @DynamicSerializeElement
    protected Map<String, Object> dataAttributes;

    @DynamicSerializeElement
    protected Number fillValue;

    @DynamicSerializeElement
    protected int maxChunkSize;

    /**
     * An arbitrary object that goes along for the ride that can be used for
     * correlation if errors occur
     */
    protected Object correlationObject;

    @Override
    public void setIntSizes(int[] sizes) {
        long[] longSizes = new long[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            longSizes[i] = sizes[i];
        }
        this.setSizes(longSizes);
    }

    @Override
    public StorageProperties getProperties() {
        return this.props;
    }

    @Override
    public void setMinIndex(long[] minIndex) {
        this.minIndex = minIndex;
    }

    @Override
    public long[] getMinIndex() {
        return minIndex;
    }

    @Override
    public void setProperties(StorageProperties props) {
        this.props = props;
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(int dimensions) {
        this.dimension = dimensions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public long[] getSizes() {
        return sizes;
    }

    @Override
    public void setSizes(long[] sizes) {
        this.sizes = sizes;
    }

    @Override
    public abstract Object getDataObject();

    /**
     * @return the group
     */
    @Override
    public String getGroup() {
        return group;
    }

    /**
     * @param group
     *            the group to set
     */
    @Override
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * @return the correlationObject
     */
    @Override
    public Object getCorrelationObject() {
        return correlationObject;
    }

    /**
     * @param correlationObject
     *            the correlationObject to set
     */
    @Override
    public void setCorrelationObject(Object correlationObject) {
        this.correlationObject = correlationObject;
    }

    /**
     * @return the dataAttributes
     */
    @Override
    public Map<String, Object> getDataAttributes() {
        return dataAttributes;
    }

    /**
     * @param dataAttributes
     *            the dataAttributes to set
     */
    @Override
    public void setDataAttributes(Map<String, Object> dataAttributes) {
        this.dataAttributes = dataAttributes;
    }

    /**
     * @return the fillValue
     */
    @Override
    public Number getFillValue() {
        return fillValue;
    }

    /**
     * @param fillValue
     *            the fillValue to set
     */
    @Override
    public void setFillValue(Number fillValue) {
        this.fillValue = fillValue;
    }

    /**
     * @return the maxSizes
     */
    @Override
    public long[] getMaxSizes() {
        return maxSizes;
    }

    /**
     * @param maxSizes
     *            the maxSizes to set
     */
    @Override
    public void setMaxSizes(long[] maxSizes) {
        this.maxSizes = maxSizes;
    }

    /**
     * @return the maxChunkSize
     */
    @Override
    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    /**
     * @param maxChunkSize
     *            the maxChunkSize to set
     */
    @Override
    public void setMaxChunkSize(int maxChunkSize) {
        this.maxChunkSize = maxChunkSize;
    }

    @Override
    public IDataRecord clone() {
        AbstractStorageRecord record = cloneInternal();
        record.name = name;
        record.dimension = dimension;
        if (sizes != null) {
            record.sizes = Arrays.copyOf(sizes, sizes.length);
        }
        if (maxSizes != null) {
            record.maxSizes = Arrays.copyOf(maxSizes, maxSizes.length);
        }
        if (props != null) {
            record.props = props.clone();
        }
        if (minIndex != null) {
            record.minIndex = Arrays.copyOf(minIndex, minIndex.length);
        }
        record.group = group;
        if (dataAttributes != null) {
            record.dataAttributes = new HashMap<>(dataAttributes);
        }
        record.fillValue = fillValue;
        record.maxChunkSize = maxChunkSize;
        record.correlationObject = correlationObject;
        return record;
    }

    /**
     * Create a new Record Object and clone/copy all members of the object where
     * possibly, do not just set references
     *
     * @return cloned record
     */
    protected abstract AbstractStorageRecord cloneInternal();

    public StorageProperties getProps() {
        return props;
    }

    public void setProps(StorageProperties props) {
        this.props = props;
    }

}
