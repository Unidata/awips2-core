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

import java.io.Serializable;
import java.util.Map;

import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.serialization.ISerializableObject;

/**
 * Data Record Interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Feb 08, 2007           chammack     Initial Check-in
 * Nov 24, 2007  555      garmendariz  Added method to check dataset dimensions
 * Jun 10, 2021  8450     mapeters     Implement {@link Serializable}
 *
 * </pre>
 *
 * @author bphillip
 */
public interface IDataRecord extends ISerializableObject, Serializable {

    /**
     * @return the storage properties
     */
    StorageProperties getProperties();

    /**
     * @param props
     *            the properties to set
     */
    void setProperties(StorageProperties props);

    /**
     * @return the dimension
     */
    int getDimension();

    /**
     * @param dimension
     *            the dimension to set
     */
    void setDimension(int dimensions);

    /**
     * @return the minIndexes
     */
    long[] getMinIndex();

    /**
     * @param dims
     *            start Indexes of sub area
     */
    void setMinIndex(long[] minIndex);

    /**
     * @return the name
     */
    String getName();

    /**
     * @param name
     *            the name to set
     */
    void setName(String name);

    /**
     * @return the sizes
     */
    long[] getSizes();

    /**
     * @param sizes
     *            the sizes to set
     */
    void setSizes(long[] sizes);

    /**
     * Generic type interface to the data
     *
     * Subinterfaces will also likely implement type-safe equivalents of this
     * method
     *
     * @return the data object
     */
    Object getDataObject();

    /**
     * Data type specific check to verify that dimensions are appropriate for
     * data object content.
     *
     * @return
     */
    boolean validateDataSet();

    /**
     * Get the group
     *
     * @return
     */
    String getGroup();

    /**
     * Set the group
     *
     * @param group
     */
    void setGroup(String group);

    /**
     * @return the correlationObject
     */
    Object getCorrelationObject();

    /**
     * @param correlationObject
     *            the correlationObject to set
     */
    void setCorrelationObject(Object correlationObject);

    /**
     * @return the dataAttributes
     */
    Map<String, Object> getDataAttributes();

    /**
     * @param dataAttributes
     *            the dataAttributes to set
     */
    void setDataAttributes(Map<String, Object> dataAttributes);

    /**
     * Reduces the dataset into a smaller dataset with only the indices
     * specified. All other indices are dropped from the data.
     *
     * The data is converted to a one dimensional array
     *
     * Indices should be expressed in 1d notation.
     *
     * @param indices
     *            the indices
     */
    void reduce(int[] indices);

    /**
     * @return the fillValue
     */
    Number getFillValue();

    /**
     * @param fillValue
     *            the fillValue to set
     */
    void setFillValue(Number fillValue);

    void setIntSizes(int[] size);

    /**
     *
     * Return the maximum size (the size that the dataset can maximally be
     * expanded to)
     *
     * @return the maxSizes
     */
    long[] getMaxSizes();

    /**
     * Set the maximum size (the size that the dataset can maximally be expanded
     * to)
     *
     * @param maxSizes
     *            the maxSizes to set
     */
    void setMaxSizes(long[] maxSizes);

    /**
     * @return the maxChunkSize
     */
    int getMaxChunkSize();

    /**
     * @param maxChunkSize
     *            the maxChunkSize to set
     */
    void setMaxChunkSize(int maxChunkSize);

    /**
     * Get the size of the data record given the dimensions, sizes of each
     * dimension and the type of data it is (float,byte,int,etc)
     *
     * @return size of record in bytes
     */
    int getSizeInBytes();

    /**
     * Clone the record
     *
     * @return
     */
    IDataRecord clone();
}