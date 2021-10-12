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
 * Enum indicating the result of a data persistence operation.
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
public enum DataStatus {

    /** Data was successfully stored to datastore */
    SUCCESS(true),

    /**
     * Data already existed so it was not stored and the rest of the data
     * storage route was stopped
     */
    DUPLICATE_SYNC(true),

    /**
     * Data failed to store in a synchronous way that should stop the data
     * storage route from proceeding
     */
    FAILURE_SYNC(false),

    /**
     * Data failed to store in an asynchronous way - the rest of the data
     * storage route should not be affected by this
     */
    FAILURE_ASYNC(false),

    /** Auditer deleted the data since the metadata didn't exist */
    DELETED(false);

    private final boolean exists;

    private DataStatus(boolean exists) {
        this.exists = exists;
    }

    /**
     * Get whether or not the data exists in the datastore based on this status.
     * Note that ignite (which provides this status) double checks that the data
     * doesn't exist when a failed store occurs by trying to load the data, so
     * this should be pretty fully trusted as is.
     *
     * @return true if data exists, false if it doesn't
     */
    public boolean exists() {
        return exists;
    }
}