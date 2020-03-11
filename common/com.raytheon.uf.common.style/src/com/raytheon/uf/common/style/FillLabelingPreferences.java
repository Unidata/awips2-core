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

/**
 * Contains the style preferences related to labeling
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * Jun 27, 2019  65510    ksunil     initial creation
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.NONE)
public class FillLabelingPreferences {

    @XmlAttribute
    private float min = Float.MIN_VALUE;

    @XmlAttribute
    private float max = Float.MAX_VALUE;

    @XmlAttribute
    private String color;

    public FillLabelingPreferences() {
        super();
    }

    public FillLabelingPreferences(FillLabelingPreferences prefs) {

        this.color = prefs.color;
        this.max = prefs.max;
        this.min = prefs.min;
    }

    @Override
    public FillLabelingPreferences clone() {
        return new FillLabelingPreferences(this);
    }

    public float getMin() {
        return min;
    }

    public void setMin(float minLabel) {
        this.min = minLabel;
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + Float.floatToIntBits(max);
        result = prime * result + Float.floatToIntBits(min);
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
        FillLabelingPreferences other = (FillLabelingPreferences) obj;
        if (color == null) {
            if (other.color != null)
                return false;
        } else if (!color.equals(other.color))
            return false;
        if (Float.floatToIntBits(max) != Float.floatToIntBits(other.max))
            return false;
        if (Float.floatToIntBits(min) != Float.floatToIntBits(other.min))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "FillLabelingPreferences [min=" + min + ", max=" + max
                + ", color=" + color + "]";
    }
}
