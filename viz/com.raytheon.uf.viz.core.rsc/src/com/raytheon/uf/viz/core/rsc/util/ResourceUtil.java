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
package com.raytheon.uf.viz.core.rsc.util;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;

/**
 * Utilities for working with CAVE resources.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2024 2037624    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class ResourceUtil {

    /**
     * Private constructor to prevent instantiation since everything's static.
     */
    private ResourceUtil() {
    }

    /**
     * Get the latest frame times for the given resource. This will only return
     * multiple times if there are frames for multiple levels for the latest
     * time.
     *
     * @param rsc
     *            resource to get latest times from
     * @return the resource's latest frame times
     */
    public static Set<DataTime> getLatestTimes(AbstractVizResource<?, ?> rsc) {
        DataTime latestTime = getLatestTimeNonSpatial(rsc);
        if (latestTime == null) {
            return Set.of();
        }
        return Arrays.stream(rsc.getDataTimes())
                .filter(dt -> dt.equals(latestTime, true))
                .collect(Collectors.toSet());
    }

    /**
     * Get a non-spatial/level-less version of the latest frame time for the
     * given resource.
     *
     * @param rsc
     *            the resource to get the latest time from
     * @return a non-spatial version of the resource's latest time (may be null)
     */
    public static DataTime getLatestTimeNonSpatial(
            AbstractVizResource<?, ?> rsc) {
        DataTime[] times = rsc.getDataTimes();
        if (times.length == 0) {
            return null;
        }
        // Resource's times are sorted in ascending order
        DataTime latest = times[times.length - 1];
        latest = latest.clone();
        latest.clearLevel();
        return latest;
    }
}
