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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.hibernate.annotations.Index;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.geospatial.ISpatialObject;
import com.raytheon.uf.common.geospatial.adapter.GeometryAdapter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import org.locationtech.jts.geom.Point;

/**
 * Class representing a observation station. Mapped to stations_spatial table in
 * the database
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Jul 24, 2007 353         bphillip    Initial Check in
 * Jul 16, 2013 2181        bsteffen    Convert geometry types to use hibernate-
 *                                      spatial
 * 10/16/2014   3454        bphillip    Upgrading to Hibernate 4
 * Oct 12, 2015 4911        rjpeter     Removed unused columns.
 * Feb 26, 2019 6140        tgurney     Hibernate 5 GeometryType fix
 * </pre>
 *
 * @author bphillip
 */
@Entity
@Table(name = "common_obs_spatial")
@XmlAccessorType(XmlAccessType.NONE)
@DynamicSerialize
public class ObStation extends PersistableDataObject<String>
        implements ISpatialObject {

    private static final long serialVersionUID = 1L;

    public static final Integer CAT_TYPE_ICAO = 1;

    // Legacy SAO identifiers - Still some around
    public static final Integer CAT_TYPE_SAO = 2;

    // National Weather Service Weather Forecast Office (WFO) locations.
    public static final Integer CAT_TYPE_WFOID = 10;

    // National Weather Service NEXRAD radar sites.
    public static final Integer CAT_TYPE_NEXRAD = 11;

    // Profiler site locations
    public static final Integer CAT_TYPE_PROF = 12;

    // WMO fixed surface synoptic locations
    public static final Integer CAT_TYPE_SFC_FXD = 20;

    // WMO mobil surface synoptic locations
    public static final Integer CAT_TYPE_SFC_MOB = 21;

    // WMO fixed upperair locations
    public static final Integer CAT_TYPE_SFC_RAOB = 22;

    // Known ship identifications - Mobile no lat/lon
    public static final Integer CAT_TYPE_SHIP_MOB = 30;

    // Drifting buoy locations
    public static final Integer CAT_TYPE_BUOY_MOB = 31;

    // Moored (Fixed) buoy locations
    public static final Integer CAT_TYPE_BUOY_FXD = 32;

    // Coastal Marine (CMAN) locations.
    public static final Integer CAT_TYPE_CMAN = 33;

    // Aircraft waypoint locations
    public static final Integer CAT_TYPE_ACFT_WAYPT = 100;

    // Aircraft pirep locations
    public static final Integer CAT_TYPE_ACFT_PIREP = 101;

    // Base catagory type for US mesonet stations.
    // Subnets may be defined in the range 1000-1999 by ading this value
    // to the Mesonet id;
    // Example Iowa DOT, IADOT = 91 so the subnet id = 1091
    //
    public static final Integer CAT_TYPE_MESONET = 1000;

    public static final Integer MESONET_NWSFAA = 1001;

    @Id
    @Column(length = 32)
    @Index(name = "gidIndex")
    @DynamicSerializeElement
    private String gid;

    /** The icao */
    @XmlAttribute
    @DynamicSerializeElement
    @Column(length = 16)
    @Index(name = "icaoIndex")
    private String icao;

    /** The WMO index */
    @XmlAttribute
    @DynamicSerializeElement
    @Column
    private Integer wmoIndex;

    @XmlAttribute
    @DynamicSerializeElement
    @Column(length = 16, nullable = false)
    private String stationId;

    @XmlAttribute
    @DynamicSerializeElement
    @Column(nullable = false)
    private Integer catalogType;

    /** Descriptive name of the station */
    @XmlAttribute
    @DynamicSerializeElement
    @Column
    private String name;

    /** Country location */
    @XmlAttribute
    @DynamicSerializeElement
    @Column(length = 32)
    private String country;

    /** State location (if applicable) */
    @XmlAttribute
    @DynamicSerializeElement
    @Column(length = 32)
    private String state;

    /** The WMO region */
    @XmlAttribute
    @DynamicSerializeElement
    @Column
    private Integer wmoRegion;

    /** The elevation of the station */
    @XmlAttribute
    @DynamicSerializeElement
    @Column
    private Integer elevation;

    /** The upper air elevation */
    @XmlAttribute
    @DynamicSerializeElement
    @Column
    private Integer upperAirElevation;

    /** The upper air geometry information */
    @Column(name = "upperairgeom", columnDefinition = "geometry")
    @XmlJavaTypeAdapter(value = GeometryAdapter.class)
    @DynamicSerializeElement
    private Point upperAirGeometry;

    /** The station location */
    @Column(name = "the_geom", columnDefinition = "geometry")
    @XmlJavaTypeAdapter(value = GeometryAdapter.class)
    @DynamicSerializeElement
    private Point location;

    public Integer getCatalogType() {
        return catalogType;
    }

    public void setCatalogType(Integer catalogType) {
        this.catalogType = catalogType;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    @Override
    public Point getGeometry() {
        return location;
    }

    @Override
    public CoordinateReferenceSystem getCrs() {
        return null;
    }

    public String getGid() {
        return gid;
    }

    public void setGid(String gid) {
        this.gid = gid;
    }

    public String getIcao() {
        return icao;
    }

    public void setIcao(String icao) {
        this.icao = icao;
    }

    public Integer getWmoIndex() {
        return wmoIndex;
    }

    public void setWmoIndex(Integer wmoIndex) {
        this.wmoIndex = wmoIndex;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Integer getWmoRegion() {
        return wmoRegion;
    }

    public void setWmoRegion(Integer wmoRegion) {
        this.wmoRegion = wmoRegion;
    }

    public Integer getElevation() {
        return elevation;
    }

    public void setElevation(Integer elevation) {
        this.elevation = elevation;
    }

    public Integer getUpperAirElevation() {
        return upperAirElevation;
    }

    public void setUpperAirElevation(Integer upperAirElevation) {
        this.upperAirElevation = upperAirElevation;
    }

    public Point getUpperAirGeometry() {
        return upperAirGeometry;
    }

    public void setUpperAirGeometry(Point upperAirGeometry) {
        this.upperAirGeometry = upperAirGeometry;
    }

    public Point getStationGeom() {
        return location;
    }

    public void setStationGeom(Point stationGeom) {
        this.location = stationGeom;
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

    public static final String createGID(Integer catalogType,
            String stationId) {
        String gid = null;
        if (catalogType != null && stationId != null) {
            gid = String.format("%04d-%s", catalogType, stationId);
        }
        return gid;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (gid == null ? 0 : gid.hashCode());
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
        ObStation other = (ObStation) obj;
        if (gid == null) {
            if (other.gid != null) {
                return false;
            }
        } else if (!gid.equals(other.gid)) {
            return false;
        }
        return true;
    }
}
