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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataplugin.annotations.DataURI;
import com.raytheon.uf.common.dataplugin.annotations.NullFloat;
import com.raytheon.uf.common.dataplugin.annotations.NullString;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.adapter.GeometryAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * AircraftObsLocation represents an observation point above the surface of the
 * earth.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 26, 2007 384        jkorman     Initial Coding.
 * Apr 08, 2009 952        jsanchez    Added @DynamicSerializeElement tags.
 * Jul 16, 2013 2181       bsteffen    Convert geometry types to use hibernate-
 *                                     spatial
 * Jul 23, 2014 3410       bclement    changed lat and lon to floats
 * 10/16/2014   3454       bphillip    Upgrading to Hibernate 4
 * Jul 20, 2015 4360       rferrel     Made flightLevel, stationId, latitude and longitude non-nullable.
 * Feb 26, 2019 6140       tgurney     Hibernate 5 GeometryType fix
 * Aug 11, 2022 8892       tjensen     Update indexes for Hibernate 5
 *
 * </pre>
 *
 * @author jkorman
 */
@Embeddable
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class AircraftObsLocation implements ISpatialObject {

    private static final long serialVersionUID = 1L;

    // Elevation of this location in meters.
    @Column(nullable = false)
    @DataURI(position = 3)
    @DynamicSerializeElement
    private Integer flightLevel = null;

    // Id of the station making this observation.
    @Column(length = 16, nullable = false)
    @DataURI(position = 0)
    @NullString
    @DynamicSerializeElement
    private String stationId;

    // Default to mobile. If defined the base location data has been retrieved
    // from a data base.
    @Column
    @DynamicSerializeElement
    private Boolean locationDefined = Boolean.FALSE;

    @Column(nullable = false)
    @DataURI(position = 1)
    @NullFloat
    @DynamicSerializeElement
    private float latitude;

    @Column(nullable = false)
    @DataURI(position = 2)
    @NullFloat
    @DynamicSerializeElement
    private float longitude;

    @Column(name = "location", columnDefinition = "geometry")
    @XmlJavaTypeAdapter(value = GeometryAdapter.class)
    @DynamicSerializeElement
    private Point location;

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    /**
     * Create an empty instance of this class.
     */
    public AircraftObsLocation() {
    }

    /**
     * Create an instance of this class using a given station identifier.
     *
     * @param stationIdentifier
     */
    public AircraftObsLocation(String stationIdentifier) {
        stationId = stationIdentifier;
    }

    /**
     * Get the elevation, in meters, of the observing platform or location.
     *
     * @return The observation elevation, in meters.
     */
    public Integer getFlightLevel() {
        return flightLevel;
    }

    /**
     * Set the elevation, in meters, of the observing platform or location.
     *
     * @param elevation
     *            The elevation to set
     */
    public void setFlightLevel(Integer flightLevel) {
        this.flightLevel = flightLevel;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
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

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
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
        return 0;
    }

    @Override
    public Integer getNy() {
        return 0;
    }

    public Point getLocation() {
        return location;
    }

    public void setLocation(Point location) {
        this.location = location;
    }

    public void setLocation(double latitude, double longitude) {
        this.location = new GeometryFactory().createPoint(new Coordinate(
                MapUtil.correctLon(longitude), MapUtil.correctLat(latitude)));
    }

    public static long getSerialVersionUID() {
        return serialVersionUID;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + (flightLevel == null ? 0 : flightLevel.hashCode());
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int) (temp ^ temp >>> 32);
        result = prime * result
                + (stationId == null ? 0 : stationId.hashCode());
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
        AircraftObsLocation other = (AircraftObsLocation) obj;
        if (flightLevel == null) {
            if (other.flightLevel != null) {
                return false;
            }
        } else if (!flightLevel.equals(other.flightLevel)) {
            return false;
        }
        if (Double.doubleToLongBits(latitude) != Double
                .doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(longitude) != Double
                .doubleToLongBits(other.longitude)) {
            return false;
        }
        if (stationId == null) {
            if (other.stationId != null) {
                return false;
            }
        } else if (!stationId.equals(other.stationId)) {
            return false;
        }
        return true;
    }

}
