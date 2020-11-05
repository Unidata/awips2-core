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

package com.raytheon.uf.common.time;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import com.raytheon.uf.common.time.adapter.TimeRangeTypeAdapter;
import com.raytheon.uf.common.time.util.TimeUtil;

/**
 *
 * Ported from AWIPS I Common.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jun 19, 2007           chammack  Port from AWIPS Common
 * Feb 27, 2008  879      rbell     Added compareTo(TimeRange)
 * Mar 20, 2013  1774     randerso  Changed toString to display times even when
 *                                  duration is 0, use TimeUtil constants.
 * Apr 04, 2013  1787     randerso  Removed a bunch of isValid checks to the
 *                                  logic works as intended by the original A1
 *                                  implementation.
 * Apr 24, 2013  1949     rjpeter   Updated clone to deep copy by millis.
 * Jun 19, 2013  2125     njensen   Removed javax.persistence.Access annotations
 * Jan 02, 2018  7178     randerso  Added MIN_TIME and made MAX_TIME public
 *
 * </pre>
 *
 * <B>Original Documentation:</B>
 *
 * The TimeRange class provides the notion of a span of time defined by a
 * starting and ending time. The constructors allow the specification of
 * starting/ending time, or a time and a duration. If the duration is positive
 * (or zero), then the specified time is the start time. If the duration is
 * negative, then the specified time is the end time as shown in the design
 * document.
 *
 * TimeRanges are generally used to define a valid time range for data files.
 * TimeRange components (start time, end time, and duration) may be retrieved.
 * TimeRanges may not be modified once created. A new TimeRange object must be
 * created from an previous one to emulate modification. Comparison routines
 * allow the user to determine the relationships between two TimeRanges.
 *
 * A time range is exclusive of the ending time. For example, a start time of
 * February 9, 1993 01:00:00 UTC and a duration of 1 hour indicates that the
 * time range is from February 9, 1993 01:00:00 UTC up to but not including
 * February 9, 1993 01:00:00 UTC.
 *
 * Routines are provided to calculate intersections, unions, spans, and gaps
 * between two time ranges. An invalid time range object is used to indicate a
 * that no intersection or gap is present. A routine is provided to check
 * whether a time range is valid or not.
 *
 * TimeRanges with a zero duration are defined differently than a TimeRange with
 * a duration. A TimeRange with a zero duration is defined as the starting time
 * including the ending time which is analogous to a Date. A zero duration time
 * range only can contain another time range that is a zero duration time range
 * with the same starting time. Zero duration time ranges only contain Dates
 * that are equal to the starting time of the zero duration time range.
 *
 *
 *
 *
 *
 * @author chammack
 */
@Embeddable
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = TimeRangeTypeAdapter.class)
public class TimeRange implements Serializable, Comparable<TimeRange> {

    /**
     * Minimum time for TimeRange, corresponds to 1970-01-01 00:00:00Z
     */
    public static final long MIN_TIME = 0L;

    /**
     * Maximum time for TimeRange, corresponds to 2038-01-19 03:14:07Z
     *
     * This constant gives a value similar to GFEs AbsTime.MaxFutureValue() and
     * doesn't break Calendar like Long.MAX_VALUE does
     */
    public static final long MAX_TIME = Integer.MAX_VALUE
            * TimeUtil.MILLIS_PER_SECOND;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    @Column(name = "rangeStart")
    @XmlAttribute
    @DynamicSerializeElement
    private Date start;

    @Column(name = "rangeEnd")
    @XmlAttribute
    @DynamicSerializeElement
    private Date end;

    /**
     * Default time range constructor
     */
    public TimeRange() {
        this(MIN_TIME, MIN_TIME);
    }

    /**
     * Constructor for creating a time range based on a base time and duration.
     *
     * The other time for the time range is calculated by adding the duration to
     * the end time. The start time is determined from the minimum of the two
     * times and the ending time from the maximum of the two times
     *
     * @param baseTime
     *            the base time
     * @param duration
     *            the duration of the time range in milliseconds
     */
    public TimeRange(Date baseTime, long duration) {
        this(baseTime.getTime(), baseTime.getTime() + duration);
    }

    /**
     * Constructor for creating a time range based on two dates.
     *
     * @param time1
     *            one date
     * @param time2
     *            a second date
     */
    public TimeRange(Date time1, Date time2) {
        this(time1.getTime(), time2.getTime());
    }

    /**
     * Constructor using two calendars
     *
     * @param time1
     *            calendar 1
     * @param time2
     *            calendar 2
     */
    public TimeRange(Calendar time1, Calendar time2) {
        this(time1.getTimeInMillis(), time2.getTimeInMillis());
    }

    /**
     * Constructor for creating a time range based on two longs interpreted as
     * milliseconds since January 1, 1970, 00:00:00 GMT.
     *
     * @param time1
     *            one date
     * @param time2
     *            a second date
     */
    public TimeRange(long time1, long time2) {
        long min = Math.min(time1, time2);
        long max = Math.max(time1, time2);
        start = new Date(min);
        end = new Date(max);
    }

    /**
     * Joins two time ranges and returns a new TimeRange. Returns an invalid
     * TimeRange if there is gap between the specified time ranges and this
     * TimeRange which can be checked with the isValid() routine. An invalid
     * TimeRange is returned if an invalid time range is accessed.
     *
     * @param timeRange
     *            the time range
     * @return the joined time range
     */
    public TimeRange join(TimeRange timeRange) {
        if (isValid() && timeRange.isValid()
                && (overlaps(timeRange) || isAdjacentTo(timeRange))) {
            return new TimeRange(
                    Math.min(start.getTime(), timeRange.getStart().getTime()),
                    Math.max(end.getTime(), timeRange.getEnd().getTime()));
        } else {
            // return an invalid time range
            return new TimeRange();
        }
    }

    /**
     * Returns the gap between two time ranges. If there is no gap, then an
     * invalid time range is returned which can be checked with the isValid()
     * routine.
     *
     * @param timeRange
     *            the time range to gap
     * @return the gap of the time range
     */
    public TimeRange gap(TimeRange timeRange) {
        if (!isValid() || !timeRange.isValid() || overlaps(timeRange)
                || isAdjacentTo(timeRange)) {
            // return an invalid time range
            return new TimeRange();
        } else {
            if (start.getTime() > timeRange.getStart().getTime()) {
                return new TimeRange(timeRange.getEnd(), start);
            } else {
                return new TimeRange(end, timeRange.getStart());
            }
        }
    }

    /**
     * Returns a time range that spans all possible times
     *
     * @return the time range for all possible times
     */
    public static TimeRange allTimes() {
        return new TimeRange(MIN_TIME, MAX_TIME);
    }

    /**
     * Returns the duration of this TimeRange as a String
     *
     * @return the duration as a string
     */
    public String durationAsPrettyString() {
        long dur = getDuration();
        long days = dur / TimeUtil.SECONDS_PER_DAY;

        dur -= days * TimeUtil.SECONDS_PER_DAY;
        long hours = dur / TimeUtil.SECONDS_PER_HOUR;

        dur -= hours * TimeUtil.SECONDS_PER_HOUR;
        long min = dur / TimeUtil.SECONDS_PER_MINUTE;

        long sec = dur - (min * TimeUtil.SECONDS_PER_MINUTE);

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days + "d ");
        }
        if ((hours > 0) || (min > 0) || (sec > 0)) {
            sb.append(hours + "h ");
        }
        if ((min > 0) || (sec > 0)) {
            sb.append(min + "m ");
        }
        if (sec > 0) {
            sb.append(sec + "s ");
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    /**
     * @return the start
     */
    public Date getStart() {
        return start;
    }

    /**
     * @return the end
     */
    public Date getEnd() {
        return end;
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return !start.equals(end);
    }

    /**
     * Returns the center time for the time range. Undefined results are
     * returned for invalid time ranges. The user should use isValid() to ensure
     * that this is a valid time range.
     *
     * @return the center time range
     */
    public Date getCenterTime() {
        long t = end.getTime() - start.getTime();
        return new Date(start.getTime() + (t / 2));
    }

    /**
     * Returns the duration for the time range. Duration is the starting time
     * subtracted from the ending time. Undefined results are returned for
     * invalid time ranges. The user should use isValid() to ensure that this is
     * a valid time range.
     *
     * @return the duration
     */
    public long getDuration() {
        return end.getTime() - start.getTime();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((end == null) ? 0 : end.hashCode());
        result = (prime * result) + ((start == null) ? 0 : start.hashCode());
        return result;
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
        TimeRange other = (TimeRange) obj;
        if (end == null) {
            if (other.end != null) {
                return false;
            }
        } else if (!end.equals(other.end)) {
            return false;
        }
        if (start == null) {
            if (other.start != null) {
                return false;
            }
        } else if (!start.equals(other.start)) {
            return false;
        }
        return true;
    }

    /**
     * Returns true if an absolute time is contained within this time range.
     * Undefined results are returned for invalid time ranges. The user should
     * use isValid() to ensure that this is a valid time range.
     *
     * @param time
     *            the time to compare
     * @return true if time is >= start and < end of this time range
     */
    public boolean contains(Date time) {
        if (getDuration() != 0) {
            // the end time is not part of the time range (hence the < operator)
            return (time.getTime() >= start.getTime())
                    && (time.getTime() < end.getTime());
        } else {
            // Special case for zero duration time range
            return time.equals(start);

        }
    }

    /**
     * Returns true if a time range is contained within this time range.
     * Undefined results are returned for invalid time ranges. The user should
     * use isValid() to ensure that this is a valid time range.
     *
     * @param timeRange
     * @return true if a time range is contained within the range
     */
    public boolean contains(TimeRange timeRange) {
        if (getDuration() == 0) {
            return this.equals(timeRange);
        } else if (timeRange.getDuration() == 0) {
            return contains(timeRange.getStart());
        } else {
            return (timeRange.start.compareTo(start) >= 0)
                    && (timeRange.end.compareTo(end) <= 0);
        }
    }

    /**
     * Returns true if another time range is adjacent to this time range.
     * Undefined results are returned for invalid time ranges. The user should
     * use isValid() to ensure that this is a valid time range.
     *
     * @param timeRange
     *            the time range to compare
     * @return true if the time ranges are adjacent
     */
    public boolean isAdjacentTo(TimeRange timeRange) {
        return start.equals(timeRange.end) || end.equals(timeRange.start);
    }

    /**
     * Checks if this time range overlaps in any way another time range. Returns
     * true if any part of this time range is part of the specified time range.
     *
     * Undefined results are returned for invalid time ranges. The user should
     * use isValid() to ensure that this is a valid time range.
     *
     * @param timeRange
     *            the time range to compare
     * @return true if the time range overlaps
     */
    public boolean overlaps(TimeRange timeRange) {
        if (timeRange.contains(start) || contains(timeRange.getStart())) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Returns the intersection of two time ranges. If there is no intersection,
     * then an invalid TimeRange is returned which can be checked with the
     * isValid() routine.
     *
     * @param timeRange
     *            the time range to use in intersection
     * @return the intersection of the time ranges
     */
    public TimeRange intersection(TimeRange timeRange) {
        if (isValid() && timeRange.isValid() && overlaps(timeRange)) {
            return new TimeRange(
                    Math.max(start.getTime(), timeRange.getStart().getTime()),
                    Math.min(end.getTime(), timeRange.getEnd().getTime()));
        } else {
            // return an invalid time range
            return new TimeRange();
        }

    }

    /**
     * Returns the span for two time ranges.
     *
     * An invalid TimeRange is returned if an invalid time range is accessed.
     *
     * @param timeRange
     *            the time range to span with
     * @return the spanned time range
     */
    public TimeRange span(TimeRange timeRange) {
        if (isValid() && timeRange.isValid()) {
            return new TimeRange(
                    Math.min(start.getTime(), timeRange.getStart().getTime()),
                    Math.max(end.getTime(), timeRange.getEnd().getTime()));
        } else {
            // return an invalid time range
            return new TimeRange();
        }

    }

    /**
     * Returns the combined span for two time range. If either of the time
     * ranges are invalid, then this function returns simply the other's valid
     * time.
     *
     * @param timeRange
     *            the time range to combine with
     * @return a combined time range
     */
    public TimeRange combineWith(TimeRange timeRange) {
        if (isValid() && timeRange.isValid()) {
            return new TimeRange(
                    Math.min(start.getTime(), timeRange.getStart().getTime()),
                    Math.max(end.getTime(), timeRange.getEnd().getTime()));
        } else if (isValid()) {
            return this;
        } else {
            return timeRange;
        }

    }

    @Override
    public String toString() {
        final DateFormat GMTFormat = new SimpleDateFormat(
                "MMM dd yy HH:mm:ss zzz");
        GMTFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(GMTFormat.format(getStart()));
        sb.append(", ");
        sb.append(GMTFormat.format(getEnd()));
        if (!isValid()) {
            sb.append(", Invalid");
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public TimeRange clone() {
        return new TimeRange(this.start.getTime(), this.end.getTime());
    }

    @Override
    public int compareTo(TimeRange rhs) {
        return start.compareTo(rhs.start);
    }

    /**
     * Setter intended for dynamic serialization only
     *
     * @param start
     */
    public void setStart(Date start) {
        if (start.after(this.end)) {
            this.start = this.end;
            this.end = start;
        } else {
            this.start = start;
        }
    }

    /**
     * Setter intended for dynamic serialization only
     *
     * @param end
     */
    public void setEnd(Date end) {
        if (end.before(this.start)) {
            this.end = this.start;
            this.start = end;
        } else {
            this.end = end;
        }
    }

}
