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
package com.raytheon.uf.common.dataaccess.impl;

import java.util.Set;

import com.raytheon.uf.common.dataaccess.IDataFactory;
import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.common.time.DataTime;

/**
 * Utilities for working with data factories
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#    Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Nov 14, 2012           njensen     Initial creation
 * Feb 14, 2013  1614     bsteffen    Refactor data access framework to use
 *                                    single request.
 * Mar 03, 2014  2673     bsteffen    Add ability to query only ref times.
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class FactoryUtil {

    /**
     * Convenience method that provides the getAvailableTimes(R, BinOffset)
     * functionality by calling getAvailableTimes(R) and then applying the
     * BinOffset to normalize the available times.
     * 
     * @param factory
     *            the factory to retrieve times through
     * @param request
     *            the request to find available times for
     * @param binOffset
     *            the bin offset to apply
     * @return the binned times
     */
    public static DataTime[] getAvailableTimes(IDataFactory factory,
            IDataRequest request, BinOffset binOffset) {
        DataTime[] actualTimes = factory.getAvailableTimes(request, false);
        if (binOffset != null) {
            Set<DataTime> normalized = binOffset
                    .getNormalizedTimes(actualTimes);
            return normalized.toArray(new DataTime[0]);
        } else {
            return actualTimes;
        }
    }

}
