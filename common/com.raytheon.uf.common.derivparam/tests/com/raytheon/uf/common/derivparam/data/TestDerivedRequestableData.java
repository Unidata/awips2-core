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
package com.raytheon.uf.common.derivparam.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.raytheon.uf.common.derivparam.library.DerivedParameterRequest;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.inventory.TimeAndSpace;
import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.time.DataTime;

/**
 * Unit tests for {@link DerivedRequestableData}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 17, 2024 2037624    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
@ExtendWith(MockitoExtension.class)
class TestDerivedRequestableData {

    private final DataTime dt1206 = new DataTime("2024-01-01_12:06:00.0");

    @Mock
    private IGridGeometryProvider space;

    @Test
    void testGetTimeAndSpace1() {
        // Normal scenario -> time/space is created from time/space fields
        DerivedRequestableData data = new DerivedRequestableData(
                new DerivedParameterRequest());
        data.setDataTime(dt1206);
        data.setSpace(space);

        TimeAndSpace tas = data.getTimeAndSpace();

        assertEquals(new TimeAndSpace(dt1206, space), tas);
    }

    @Test
    void testGetTimeAndSpace2() {
        /*
         * Setup data with a dependency that has a virtual time/space for the
         * same time/space as the overall data -> that virtual time/space is
         * returned
         */
        AbstractRequestableData virtualDep = mock(
                AbstractRequestableData.class);
        when(virtualDep.getTimeAndSpace())
                .thenReturn(new TestVirtualTimeAndSpace(dt1206, space));
        DerivedParameterRequest request = new DerivedParameterRequest();
        request.addBaseParam(virtualDep);
        DerivedRequestableData data = new DerivedRequestableData(request);
        data.setDataTime(dt1206);
        data.setSpace(space);

        TimeAndSpace tas = data.getTimeAndSpace();

        assertEquals(new TestVirtualTimeAndSpace(dt1206, space), tas);
    }

    private static class TestVirtualTimeAndSpace extends TimeAndSpace {

        public TestVirtualTimeAndSpace(DataTime time,
                IGridGeometryProvider space) {
            super(time, space);
        }

        @Override
        public boolean isVirtual() {
            return true;
        }
    }
}
