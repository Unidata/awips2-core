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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;

/**
 * Unit tests for {@link ResourceUtil}.
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
class TestResourceUtil {

    private static final String DT_1200 = "2024-01-01_12:00:00.0";

    private static final String DT_1206 = "2024-01-01_12:06:00.0";

    private final DataTime dt1200 = new DataTime(DT_1200);

    private final DataTime dt1206 = new DataTime(DT_1206);

    private DataTime dt1200_1 = buildDt(DT_1200, 1);

    private DataTime dt1200_2 = buildDt(DT_1200, 2);

    private DataTime dt1206_1 = buildDt(DT_1206, 1);

    private DataTime dt1206_2 = buildDt(DT_1206, 2);

    @Test
    void testGetLatestTimes1() {
        // No times -> empty set
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = {};
        when(rsc.getDataTimes()).thenReturn(times);

        Set<DataTime> latestTimes = ResourceUtil.getLatestTimes(rsc);

        assertEquals(Set.of(), latestTimes);
    }

    @Test
    void testGetLatestTimes2() {
        // Non-spatial times -> latest non-spatial time
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = { dt1200, dt1206 };
        when(rsc.getDataTimes()).thenReturn(times);

        Set<DataTime> latestTimes = ResourceUtil.getLatestTimes(rsc);

        assertEquals(Set.of(dt1206), latestTimes);
    }

    @Test
    void testGetLatestTimes3() {
        // Spatial times -> all spatial times for latest time
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = { dt1200_1, dt1200_2, dt1206_1, dt1206_2 };
        when(rsc.getDataTimes()).thenReturn(times);

        Set<DataTime> latestTimes = ResourceUtil.getLatestTimes(rsc);

        assertEquals(Set.of(dt1206_1, dt1206_2), latestTimes);
    }

    @Test
    void testGetLatestTimeNonSpatial1() {
        // No times -> null
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = {};
        when(rsc.getDataTimes()).thenReturn(times);

        DataTime latestTime = ResourceUtil.getLatestTimeNonSpatial(rsc);

        assertEquals(null, latestTime);
    }

    @Test
    void testGetLatestTimeNonSpatial2() {
        // Non-spatial times -> latest non-spatial time
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = { dt1200, dt1206 };
        when(rsc.getDataTimes()).thenReturn(times);

        DataTime latestTime = ResourceUtil.getLatestTimeNonSpatial(rsc);

        assertEquals(dt1206, latestTime);
    }

    @Test
    void testGetLatestTimeNonSpatial3() {
        // Spatial times -> non-spatial version of latest time
        AbstractVizResource<?, ?> rsc = mock(AbstractVizResource.class);
        DataTime[] times = { dt1200_1, dt1200_2, dt1206_1, dt1206_2 };
        when(rsc.getDataTimes()).thenReturn(times);

        DataTime latestTime = ResourceUtil.getLatestTimeNonSpatial(rsc);

        assertEquals(dt1206, latestTime);
    }

    private static DataTime buildDt(String dtStr, double level) {
        DataTime dt = new DataTime(dtStr);
        dt.setLevel(level, "TYPE");
        return dt;
    }
}
