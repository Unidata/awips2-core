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

import java.io.Serializable;

/**
 * Interface for a class that identifies where the data in a data storage
 * operation is going in the data store.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- ----------------------------------------
 * Feb 04, 2022 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public interface IDataIdentifier extends Serializable {

    /**
     * Get the trace ID that uniquely identifies the combination of this data
     * and its current data storage route.
     *
     * @return the trace ID
     */
    String getTraceId();
}
