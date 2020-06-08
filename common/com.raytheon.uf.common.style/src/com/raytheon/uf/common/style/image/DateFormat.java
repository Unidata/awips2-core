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
package com.raytheon.uf.common.style.image;

import java.text.ParsePosition;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.zone.ZoneRulesException;

import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.UnitConverter;
import javax.measure.format.ParserException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import si.uom.SI;
import tec.uom.se.format.SimpleUnitFormat;
import tec.uom.se.unit.MetricPrefix;

/**
 * SampleFormat for date formats (e.g. seconds since epoch)
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 02, 2020  8145     randerso  Initial creation
 * Jun 08, 2020  7983     randerso  Updated units framework to JSR-363.
 *
 * </pre>
 *
 * @author randerso
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class DateFormat extends SampleFormat {
    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(DateFormat.class);

    /**
     * Date format pattern. {@link DateTimeFormatter#ofPattern(String)} for
     * details.
     */
    @XmlElement
    private String pattern = "YYYY-MM-dd HHz";

    /**
     * Time zone for date format. {@link ZoneId#of(String)} for details.
     */
    @XmlElement
    private String zoneId = "Z";

    @XmlElement
    private String epoch;

    private Instant epochDate;

    private DateTimeFormatter dtf;

    /**
     * Nullary Constructor for jaxb serialization use
     */
    @SuppressWarnings("unused")
    private DateFormat() {

    }

    /**
     * Create a DateFormat object using the standard epoch of January 1, 1970
     * 00:00:00Z
     *
     * @param pattern
     *            date time format patter. See
     *            {@link DateTimeFormatter#ofPattern(String)} for details.
     * @param zoneId
     *            - time zone ID (e.g. Z, UTC, GMT, etc.). See
     *            {@link ZoneId#of(String)} for details.
     * @throws DateTimeException
     * @throws ZoneRulesException
     */
    public DateFormat(String pattern, String zoneId)
            throws DateTimeException, ZoneRulesException {
        this(pattern, zoneId, null);
    }

    /**
     * Create a DateFormat using an alternate epoch date/time. The epoch should
     * be formatted to match the supplied pattern.
     *
     * @param pattern
     *            date time format patter. See
     *            {@link DateTimeFormatter#ofPattern(String)} for details.
     * @param zoneId
     *            - time zone ID (e.g. Z, UTC, GMT, etc.). See
     *            {@link ZoneId#of(String)} for details.
     * @param epoch
     *            alternate epoch date/time.
     * @throws DateTimeException
     * @throws ZoneRulesException
     */
    public DateFormat(String pattern, String zoneId, String epoch)
            throws DateTimeException, ZoneRulesException {
        this.pattern = pattern;
        this.zoneId = zoneId;
        this.epoch = epoch;
        getEpochDate();
    }

    /**
     * Copy constructor
     *
     * @param other
     */
    private DateFormat(DateFormat other) {
        this.pattern = other.pattern;
        this.zoneId = other.zoneId;
        this.epoch = other.epoch;
    }

    @Override
    public DateFormat clone() {
        return new DateFormat(this);
    }

    @Override
    public String format(Object value, String unitString) {

        Duration millisSinceEpoch = null;
        if (value instanceof Number) {
            Unit<?> unit = null;
            try {
                unit = (Unit<?>) SimpleUnitFormat
                        .getInstance(SimpleUnitFormat.Flavor.ASCII)
                        .parseObject(unitString, new ParsePosition(0));
                UnitConverter converter = unit
                        .getConverterToAny(MetricPrefix.MILLI(SI.SECOND));
                millisSinceEpoch = Duration.ofMillis((long) converter
                        .convert(((Number) value).doubleValue()));
            } catch (ParserException e) {
                statusHandler.error("Unrecognized unit string: " + unitString,
                        e);
            } catch (ClassCastException | IncommensurableException
                    | UnconvertibleException e) {
                statusHandler.error(
                        "Unit " + unit + " is not compatible with " + SI.SECOND,
                        e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Received value object of unexpected type: "
                            + value.getClass().getName());
        }

        if (millisSinceEpoch == null) {
            return "Invalid Date";
        } else {
            // add epoch
            return getDtf().format(getEpochDate().plus(millisSinceEpoch));
        }
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern
     *            the pattern to set
     */
    public synchronized void setPattern(String pattern) {
        this.pattern = pattern;
        this.dtf = null;
    }

    /**
     * @return the zoneId
     */
    public String getZoneId() {
        return zoneId;
    }

    /**
     * @param zoneId
     *            the zoneId to set
     */
    public synchronized void setZoneId(String zoneId) {
        this.zoneId = zoneId;
        this.dtf = null;
    }

    /**
     * @return the epoch
     */
    public String getEpoch() {
        return epoch;
    }

    /**
     * @param epoch
     *            the epoch to set
     */
    public void setEpoch(String epoch) {
        this.epoch = epoch;
        this.epochDate = null;
    }

    /**
     * @return the dtf
     */
    private synchronized DateTimeFormatter getDtf() {
        if (dtf == null) {
            dtf = DateTimeFormatter.ofPattern(pattern)
                    .withZone(ZoneId.of(zoneId));
        }
        return dtf;
    }

    private synchronized Instant getEpochDate() {
        if (epochDate == null) {
            if (epoch == null || epoch.isEmpty()) {
                /* Standard epoch: January 1, 1970, 00:00:00 GMT */
                epochDate = Instant.ofEpochMilli(0);
            } else {
                DateTimeFormatter dtf = getDtf();
                epochDate = dtf.parse(epoch, Instant::from);
            }
        }
        return epochDate;
    }

}
