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
package com.raytheon.uf.common.inventory;

import java.util.Comparator;
import java.util.Objects;

import org.geotools.coverage.grid.GridGeometry2D;

import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.time.DataTime;

/**
 * Represents a time and space(location) where data can exist. This is used in
 * derived parameters for data types to report when and where data can be
 * loaded.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Apr 11, 2012           bsteffen    Initial creation
 * Apr 11, 2014  2947     bsteffen    Switch space to use IGridGeometryProvider
 * Mar 07, 2024  2036814  sharbison   Sort the times in LinkedLists for
 *                                    reliable derived parameter results.
 * Jul 15, 2024  2037624  mapeters    Add matches(), isVirtual()
 * Sep 17, 2024  2037943  mapeters    Prevent NPEs with time-agnostic values
 *
 * </pre>
 *
 * @author bsteffen
 */
public class TimeAndSpace implements Comparable<TimeAndSpace> {

    /**
     * A constant to represent the time for data that does not change over time
     * or that is available for all times, an example would be topography which
     * generally does not change on a day to day basis.
     */
    public static final DataTime TIME_AGNOSTIC = new DataTime() {

        private static final long serialVersionUID = 1L;

        @Override
        public boolean equals(Object obj, boolean ignoreSpatial) {
            return this == obj;
        }

        @Override
        public String toString() {
            return "TIME_AGNOSTIC";
        }

    };

    /**
     * A constant to represent the space for data that does not change over
     * space or that is available for all spaces, an example would be radar
     * elevation angle for a grid coverage, this can be calculated for any
     * gridcoverage.
     */
    public static final IGridGeometryProvider SPACE_AGNOSTIC = new IGridGeometryProvider() {

        @Override
        public String toString() {
            return "SPACE_AGNOSTIC";
        }

        @Override
        public GridGeometry2D getGridGeometry() {
            return null;
        }

    };

    private final DataTime time;

    private final IGridGeometryProvider space;

    public TimeAndSpace() {
        this(TIME_AGNOSTIC, SPACE_AGNOSTIC);
    }

    public TimeAndSpace(DataTime time) {
        this(time, SPACE_AGNOSTIC);

    }

    public TimeAndSpace(IGridGeometryProvider space) {
        this(TIME_AGNOSTIC, space);
    }

    public TimeAndSpace(DataTime time, IGridGeometryProvider space) {
        this.time = time;
        this.space = space;
    }

    public DataTime getTime() {
        return time;
    }

    public IGridGeometryProvider getSpace() {
        return space;
    }

    public boolean isTimeAgnostic() {
        return this.time == TIME_AGNOSTIC;
    }

    public boolean isSpaceAgnostic() {
        return space == SPACE_AGNOSTIC;
    }

    /**
     * @return true if this is representing the data as being available for a
     *         different time than it's actually for, false otherwise
     */
    public boolean isVirtual() {
        return false;
    }

    /**
     * Determine if this and other have the same time and space. This differs
     * from {@link #equals} in that it doesn't check that the classes match, and
     * doesn't check subclass fields.
     *
     * @param other
     *            the other {@link TimeAndSpace} to compare against
     * @return true if this and other match, false otherwise
     */
    public final boolean matches(TimeAndSpace other) {
        if (this == other) {
            return true;
        }
        if (other == null) {
            return false;
        }
        if (!Objects.equals(space, other.space)) {
            return false;
        }
        if (!Objects.equals(time, other.time)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hash(space, time);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TimeAndSpace other = (TimeAndSpace) obj;
        if (!Objects.equals(space, other.space)) {
            return false;
        }
        if (!Objects.equals(time, other.time)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AvailSpT: Time: ");
        sb.append(time);
        sb.append(", Space: ");
        sb.append(space);
        return sb.toString();
    }

    /**
     * Sort the times so that newer/better times come first for reliable derived
     * parameter results.
     */
    @Override
    public int compareTo(TimeAndSpace timeAndSpace) {
        // Compare the reference time and if equal compare the forecast time.
        // Example of sorted TimeAndSpace:
        // fcstTime = 583200 refTime = 2024-02-29 12:00:00.0
        // fcstTime = 604800 refTime = 2024-02-29 12:00:00.0
        // fcstTime = 583200 refTime = 2024-02-29 06:00:00.0
        // fcstTime = 604800 refTime = 2024-02-29 06:00:00.0

        /*
         * Sort reference time in reverse/descending order so that newer/better
         * reference times come first in the results, also putting time-agnostic
         * TimeAndSpace values (null reference time) last.
         */
        int refTimeComparison = Objects.compare(time.getRefTime(),
                timeAndSpace.getTime().getRefTime(),
                Comparator.nullsLast(Comparator.reverseOrder()));
        if (refTimeComparison != 0) {
            return refTimeComparison;
        }
        // If reference time is equal then compare the forecast time.
        return Integer.compare(time.getFcstTime(),
                timeAndSpace.getTime().getFcstTime());
    }

}
