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

package com.raytheon.uf.common.style;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * Contains the style preferences related to labeling. Both increment and values
 * arguments are extended from this base class.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * May 07, 2019  64008    ksunil  Initial Creation.
 *
 *
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BaseLabelingPreferences {

    private static final String SEPARATOR = " ";

    @XmlValue
    protected float[] values;

    @XmlAttribute
    protected String color;

    @XmlAttribute
    protected String lineStyle;

    @XmlAttribute
    protected int thickness = -1;

    public BaseLabelingPreferences() {

    }

    public BaseLabelingPreferences(BaseLabelingPreferences prefs) {
        if (prefs.values != null) {
            this.values = new float[prefs.values.length];
            System.arraycopy(prefs.values, 0, this.values, 0,
                    this.values.length);
        }
        this.thickness = prefs.thickness;
        this.lineStyle = prefs.lineStyle;
        this.color = prefs.color;
    }

    @Override
    public BaseLabelingPreferences clone() {
        return new BaseLabelingPreferences(this);
    }

    public float[] getValues() {
        return values;
    }

    public void setValues(float[] values) {
        this.values = values;
    }

    /**
     * Gets the values interpreted as a String
     * 
     * @return a space separated list of values
     */
    public String getValuesString() {
        String returnString = null;
        if (values != null) {
            StringBuilder sb = new StringBuilder();
            for (Float f : values) {
                sb.append(f);
                sb.append(SEPARATOR);
            }
            returnString = sb.toString().trim();
        }

        return returnString;
    }

    /**
     * Sets the values from an interpretation of a String
     * 
     * @param aValues
     *            a space separated list of values
     */
    public void setValuesString(String aValues) {
        if (aValues != null) {
            String[] floats = aValues.split(SEPARATOR);
            values = new float[floats.length];
            for (int i = 0; i < floats.length; i++) {
                values[i] = Float.parseFloat(floats[i]);
            }
        }
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getLinePattern() {
        return lineStyle;
    }

    public void setLinePattern(String lineStyle) {
        this.lineStyle = lineStyle;
    }

    public int getThickness() {
        return thickness;
    }

    public void setThickness(int thickness) {
        this.thickness = thickness;
    }

    public boolean noStylesSet() {
        return ((lineStyle == null) && (color == null) && (thickness == -1));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result
                + ((lineStyle == null) ? 0 : lineStyle.hashCode());
        result = prime * result + Float.floatToIntBits(thickness);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BaseLabelingPreferences other = (BaseLabelingPreferences) obj;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (lineStyle == null) {
            if (other.lineStyle != null)
                return false;
        } else if (!lineStyle.equals(other.lineStyle))
            return false;
        if (Float.floatToIntBits(thickness) != Float
                .floatToIntBits(other.thickness))
            return false;
        return true;
    }

}
