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
package com.raytheon.uf.common.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.time.DataTime;

/**
 * Unit tests for {@link TimeAndSpace}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2024 2037624    mapeters    Initial creation
 * Sep 17, 2024 2037943    mapeters    Test compareTo()
 *
 * </pre>
 *
 * @author mapeters
 */
class TestTimeAndSpace {

    private static final DataTime dt1200 = new DataTime(
            "2024-01-01_12:00:00.0");

    private static final DataTime dt1206 = new DataTime(
            "2024-01-01_12:06:00.0");

    private static IGridGeometryProvider space1 = mock(
            IGridGeometryProvider.class);

    private static IGridGeometryProvider space2 = mock(
            IGridGeometryProvider.class);

    static Stream<Arguments> provideParamsForMatches() {
        TimeAndSpace tas_1200_space1 = new TimeAndSpace(dt1200, space1);

        return Stream.of(
                // Same time/space
                Arguments.of(tas_1200_space1, new TimeAndSpace(dt1200, space1),
                        true),
                // Different time
                Arguments.of(tas_1200_space1, new TimeAndSpace(dt1206, space1),
                        false),
                // Different space
                Arguments.of(tas_1200_space1, new TimeAndSpace(dt1200, space2),
                        false),

                // Null arg
                Arguments.of(tas_1200_space1, null, false));
    }

    @ParameterizedTest
    @MethodSource("provideParamsForMatches")
    void testMatches(TimeAndSpace tas1, TimeAndSpace tas2,
            boolean expectedResult) {
        boolean actualResult = tas1.matches(tas2);
        if (tas2 != null) {
            boolean actualResultReversed = tas2.matches(tas1);

            assertEquals(expectedResult, actualResultReversed);
        }

        assertEquals(expectedResult, actualResult);
    }

    static Stream<Arguments> provideParamsForCompareTo() {
        /*
         * Setup 3 times with same valid time (18Z) but differing
         * reference/forecast time combos
         */
        TimeAndSpace tas18NonFcst = new TimeAndSpace(
                new DataTime("2024-01-01_18:00:00.0"));
        TimeAndSpace tas18OldFcst = new TimeAndSpace(
                new DataTime("2024-01-01_06:00:00.0 (12)"));
        TimeAndSpace tas18NewFcst = new TimeAndSpace(
                new DataTime("2024-01-01_12:00:00.0 (6)"));
        TimeAndSpace tasAgnostic = new TimeAndSpace();

        /*
         * Newer/better times should be considered less than worse times so that
         * they appear earlier in sorted collections
         */
        return Stream.of(
                // Non-forecast time better than forecast time
                Arguments.of(tas18NonFcst, tas18NewFcst, -1),
                // Newer forecast better than older forecast
                Arguments.of(tas18NewFcst, tas18OldFcst, -1),
                // Non-forecast time better than agnostic
                Arguments.of(tas18NonFcst, tasAgnostic, -1),
                // Forecast time better than agnostic
                Arguments.of(tas18NewFcst, tasAgnostic, -1),
                // Agnostic times are equals
                Arguments.of(tasAgnostic, tasAgnostic, 0),
                // Matching non-forecast times are equal
                Arguments.of(tas18NonFcst, tas18NonFcst, 0),
                // Matching forecast times are equal
                Arguments.of(tas18NewFcst, tas18NewFcst, 0));
    }

    @ParameterizedTest
    @MethodSource("provideParamsForCompareTo")
    void testCompareTo(TimeAndSpace tas1, TimeAndSpace tas2,
            int expectedResult) {
        int actualResult = tas1.compareTo(tas2);
        int actualResultReversed = tas2.compareTo(tas1);

        assertEquals(expectedResult, actualResult);
        assertEquals(-expectedResult, actualResultReversed);
    }
}
