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
package com.raytheon.uf.common.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DataTime}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 2, 2024  2037091    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class TestDataTime {

    private static final String TIME_STR = "2024-03-25_12:00:00.0";

    @Test
    public void testEquals1() {
        // Same time and level type/value -> equal
        DataTime dt = new DataTime(TIME_STR);
        dt.setLevel(0d, "Type");
        DataTime dt2 = new DataTime(TIME_STR);
        dt2.setLevel(0d, "Type");

        assertEquals(dt, dt2);
    }

    @Test
    public void testEquals2() {
        // Same time and level value but different level type -> not equal
        DataTime dt = new DataTime(TIME_STR);
        dt.setLevel(0d, "Type1");
        DataTime dt2 = new DataTime(TIME_STR);
        dt2.setLevel(0d, "Type2");

        assertNotEquals(dt, dt2);
    }

    @Test
    public void testEquals3() {
        // Same time and level type but different level value -> not equal
        DataTime dt = new DataTime(TIME_STR);
        dt.setLevel(0d, "Type");
        DataTime dt2 = new DataTime(TIME_STR);
        dt2.setLevel(1d, "Type");

        assertNotEquals(dt, dt2);
    }
}
