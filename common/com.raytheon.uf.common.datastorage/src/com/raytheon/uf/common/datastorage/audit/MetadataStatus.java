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
package com.raytheon.uf.common.datastorage.audit;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;

/**
 * Enum indicating the result of a metadata persistence operation.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@DynamicSerialize
public enum MetadataStatus {

    /** Metadata was successfully stored to database */
    SUCCESS(true),

    /** Metadata was not stored since it already existed */
    DUPLICATE(true),

    /** Metadata failed to store */
    FAILURE(false),

    /**
     * Data storage route stopped before reaching the metadata storage (or
     * should have at least) - should be due to a
     * {@link DataStatus#FAILURE_SYNC} data status
     */
    STORAGE_NOT_REACHED_FOR_FAILURE(false),

    /**
     * Data storage route stopped before reaching the metadata storage (or
     * should have at least) due to the data being duplicate - should be due to
     * a {@link DataStatus#DUPLICATE} data status and the metadata should
     * already exist for the duplicate
     */
    STORAGE_NOT_REACHED_FOR_DUPLICATE(true),

    /** Auditer deleted the metadata since the data didn't exist */
    DELETED(false),

    /**
     * Not applicable, indicating that the data being stored is not referenced
     * by any metadata. Treat this as though the metadata exists since it
     * technically has all the metadata it needs and so we don't delete the
     * data.
     */
    NA(true);

    private final boolean exists;

    private MetadataStatus(boolean exists) {
        this.exists = exists;
    }

    /**
     * Get whether or not the metadata exists in the database based on this
     * status. It may be worthwhile to double check that the metadata doesn't
     * exist before taking any action on this. An example is that a duplicate
     * could be reported as a failure if a data storage route does not
     * specifically check for that (no known occurrences of that).
     *
     * @return whether or not the metadata exists
     */
    public boolean exists() {
        return exists;
    }
}