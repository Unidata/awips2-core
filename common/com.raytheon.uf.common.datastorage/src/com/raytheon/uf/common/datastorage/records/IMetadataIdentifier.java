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
package com.raytheon.uf.common.datastorage.records;

import java.io.Serializable;

/**
 * Interface for a class that identifies a metadata entry, which in turn is used
 * to identify actual data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 * Feb 17, 2022 8608       mapeters    Updates to fix data storage audit errors
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IMetadataIdentifier extends Serializable {

    /**
     * Get the trace ID that uniquely identifies the combination of this
     * metadata and its current data storage route.
     *
     * @return the trace ID
     */
    String getTraceId();

    /**
     * Get the specificity, or amount/level of data, that this metadata
     * identifies.
     *
     * @return the metadata specificity
     */
    MetadataSpecificity getSpecificity();

    /**
     * Determine if this metadata identifier is equal to the given object,
     * ignoring whether or not their trace IDs match.
     *
     * @param obj
     *            the object to compare against
     * @return true if equal when ignoring trace ID, false otherwise
     */
    boolean equalsIgnoreTraceId(Object obj);

    /**
     * Generate a hash code value for this metadata identifier, ignoring its
     * trace ID.
     *
     * @return the hash code
     */
    int hashCodeIgnoreTraceId();

    public enum MetadataSpecificity {
        /**
         * One metadata entry per hdf5 group
         */
        GROUP,

        /**
         * One metadata entry per hdf5 dataset
         */
        DATASET,

        /**
         * No data is referenced by this metadata
         */
        NO_DATA,

        /**
         * No metadata is used to reference the data being stored
         */
        NO_METADATA
    }
}
