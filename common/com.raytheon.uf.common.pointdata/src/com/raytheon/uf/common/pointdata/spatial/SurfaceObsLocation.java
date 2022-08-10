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
package com.raytheon.uf.common.pointdata.spatial;

import java.text.DecimalFormat;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataplugin.NullUtil;
import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.annotations.NullFloat;
import com.raytheon.uf.common.dataplugin.annotations.NullString;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.adapter.GeometryAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * SurfaceObsLocation represents an observation point on the surface of the
 * earth.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 26, 2007 391        jkorman     Initial Coding.
 * May 17, 2013 1869       bsteffen    Remove DataURI column from sat plot
 *                                     types.
 * Jul 09, 2013 1869       bsteffen    Switch location hibernate type to use
 *                                     hibernate spatial.
 * Jul 16, 2013 2181       bsteffen    Convert geometry types to use hibernate-
 *                                     spatial
 * July 15, 2013 2180      dhladky     Changed to hibernate spatial type (Done in 13.51) not in dev
 * Jul 23, 2014 3410       bclement    changed lat and lon to floats
 * 10/16/2014   3454       bphillip    Upgrading to Hibernate 4
 * Jul 31, 2016 4360       rferrel     Made stationId, latitude and longitude non-nullable.
 * Feb 26, 2019 6140       tgurney     Hibernate 5 GeometryType fix
 * Aug 10, 2022 8892       tjensen     Update indexes for Hibernate 5
 *
 * </pre>
 *
 * @author jkorman
 */
@Embeddable
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class SurfaceObsLocation implements ISpatialObject, Cloneable {

    private static final long serialVersionUID = 1L;

    private static final ThreadLocal<DecimalFormat> LATLON_FORMAT = new ThreadLocal<DecimalFormat>() {

        @Override
        protected DecimalFormat initialValue() {
            return new DecimalFormat("###.###");
        }

    };

    // Elevation of this location in meters.
    @XmlAttribute
    @DynamicSerializeElement
    private Integer elevation = null;

    // Id of the station making this observation.
    @Column(length = 48, nullable = false)
    @DataURI(position = 0)
    @NullString
    @XmlAttribute
    @DynamicSerializeElement
    private String stationId = NullUtil.NULL_STRING;

    // Default to mobile. If defined the base location data has been retrieved
    // from a data base.
    @Column
    @XmlAttribute
    @DynamicSerializeElement
    private Boolean locationDefined = Boolean.FALSE;

    @Column(name = "location", columnDefinition = "geometry")
    @XmlJavaTypeAdapter(value = GeometryAdapter.class)
    @DynamicSerializeElement
    private Point location;

    @DataURI(position = 1)
    @NullFloat
    @Column(nullable = false)
    @XmlAttribute
    @DynamicSerializeElement
    private Float latitude = NullUtil.NULL_FLOAT;

    @DataURI(position = 2)
    @NullFloat
    @Column(nullable = false)
    @XmlAttribute
    @DynamicSerializeElement
    private Float longitude = NullUtil.NULL_FLOAT;

    /**
     * Create an empty instance of this class.
     */
    public SurfaceObsLocation() {
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        SurfaceObsLocation clone = (SurfaceObsLocation) super.clone();
        clone.elevation = elevation;
        clone.latitude = latitude;
        clone.longitude = longitude;
        clone.locationDefined = locationDefined;
        clone.stationId = stationId;
        return clone;
    }

    /**
     * Create an instance of this class using a given station identifier.
     *
     * @param stationIdentifier
     */
    public SurfaceObsLocation(String stationIdentifier) {
        stationId = NullUtil.convertNullToNullString(stationIdentifier);
    }

    public Float getLatitude() {
        return NullUtil.convertNullFloatoNull(this.latitude);
    }

    public void setLatitude(Float latitude) {
        this.latitude = NullUtil.convertNullToNullFloat(latitude);
    }

    public Float getLongitude() {
        return NullUtil.convertNullFloatoNull(this.longitude);
    }

    public void setLongitude(Float longitude) {
        this.longitude = NullUtil.convertNullToNullFloat(longitude);
    }

    /**
     * Get the elevation, in meters, of the observing platform or location.
     *
     * @return The observation elevation, in meters.
     */
    public Integer getElevation() {
        return elevation;
    }

    /**
     * Set the elevation, in meters, of the observing platform or location.
     *
     * @param elevation
     *            The elevation to set
     */
    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    public String getStationId() {
        return NullUtil.convertNullStringToNull(this.stationId);
    }

    public void setStationId(String stationId) {
        this.stationId = NullUtil.convertNullToNullString(stationId);
    }

    /**
     * Generate a stationId from the lat/lon values.
     */
    public void generateCoordinateStationId() {
        DecimalFormat format = LATLON_FORMAT.get();
        this.stationId = format.format(longitude) + ":"
                + format.format(latitude);
    }

    /**
     * Is this location a station lookup.
     *
     * @return the locationDefined
     */
    public Boolean getLocationDefined() {
        return locationDefined;
    }

    public void setLocationDefined(Boolean locationDefined) {
        this.locationDefined = locationDefined;
    }

    @Override
    public CoordinateReferenceSystem getCrs() {
        return null;
    }

    @Override
    public Geometry getGeometry() {
        return location;
    }

    @Override
    public Integer getNx() {
        return null;
    }

    @Override
    public Integer getNy() {
        return null;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public void assignLocation(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.location = new GeometryFactory().createPoint(new Coordinate(
                MapUtil.correctLon(longitude), MapUtil.correctLat(latitude)));
    }

    public void setGeometry(Point point) {
        assignLocation((float) point.getY(), (float) point.getX());
    }

    public void assignLatitude(float latitude) {
        this.latitude = latitude;
        if (!NullUtil.isNull(longitude) && location == null) {
            assignLocation(this.latitude, this.longitude);
        }
    }

    public void assignLongitude(float longitude) {
        this.longitude = longitude;
        if (!NullUtil.isNull(latitude) && location == null) {
            assignLocation(this.latitude, this.longitude);
        }
    }
}
