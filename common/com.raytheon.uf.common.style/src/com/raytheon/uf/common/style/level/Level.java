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

package com.raytheon.uf.common.style.level;

import si.uom.NonSI;
import si.uom.SI;
import tec.uom.se.AbstractUnit;
import tec.uom.se.unit.MetricPrefix;

import javax.measure.Unit;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Represents a level on the earth for style rule purposes
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ ---------------------------
 * Sep 24, 2007           njensen      Initial creation
 * Nov 14, 2013  2361     njensen      Remove ISerializableObject
 * May 01, 2014  DCS 027  MPorricelli  Add WBZ level
 * Aug 15, 2016  5821     bsteffen     Add TROP level
 * May 18, 2018  20395    wkwock       Add CBL and CLG level
 * Apr 15, 2019  7596     lsingh       Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author njensen
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class Level {

    public static enum LevelType {
        THETA, HEIGHT_AGL, HEIGHT_MSL, PRESSURE, SURFACE, TILT, MB_AGL, MAXW, TW0, TEMP, FRZ, DEFAULT, WBZ, TROP, CBL, CLG
    }

    protected LevelType type;

    protected Unit<?> units;

    public Level() {

    }

    public Level(String aType) {
        constructFromString(aType);
    }

    public Level(LevelType aType) {
        this.type = aType;
        init();
    }

    private void init() {
        switch (type) {
        case THETA:
        case TEMP:
            this.units = SI.KELVIN;
            break;
        case HEIGHT_AGL:
        case HEIGHT_MSL:
            this.units = SI.METRE;
            break;
        case PRESSURE:
        case MB_AGL:
            this.units = MetricPrefix.MILLI(NonSI.BAR);
            break;
        case DEFAULT:
        case SURFACE:
            this.units = AbstractUnit.ONE;
            break;
        case TILT:
            this.units = NonSI.DEGREE_ANGLE;
            break;
        default:
            this.units = AbstractUnit.ONE;
        }
    }

    private void constructFromString(String aType) {
        if (aType.equals("MB")) {
            aType = "PRESSURE";
        } else if (aType.equals("AGL")) {
            aType = "HEIGHT_AGL";
        } else if (aType.equals("MSL")) {
            aType = "HEIGHT_MSL";
        } else if (aType.equals("BL")) {
            aType = "MB_AGL";
        }
        this.type = LevelType.valueOf(aType);
        init();
    }

    /**
     * @return the type
     */
    public LevelType getType() {
        return type;
    }

    @XmlAttribute(name = "units")
    public void setTypeString(String aType) {
        constructFromString(aType);
    }

    public String getTypeString() {
        String t = type.toString();
        if (t.equals("PRESSURE")) {
            t = "MB";
        } else if (t.equals("HEIGHT_AGL")) {
            t = "AGL";
        } else if (t.equals("HEIGHT_MSL")) {
            t = "MSL";
        } else if (type == LevelType.MB_AGL)
            t = "MB agl"; // TODO: ??
        return t;
    }

    public void setType(LevelType aType) {
        type = aType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((units == null) ? 0 : units.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Level other = (Level) obj;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        if (units == null) {
            if (other.units != null)
                return false;
        } else if (!units.equals(other.units))
            return false;
        return true;
    }

}
