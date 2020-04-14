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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;

/**
 * Contains the style preferences related to labeling
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * May 07, 2019  64008    ksunil    Initial creation/refactoring to absorb LabelingPreferences changes
 *
 * </pre>
 *
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ImageryLabelingPreferences extends AbstractLabelingPreferences {

    @XmlElement(name = "values")
    @XmlList
    private float[] values;

    @XmlElement(name = "increment")
    private float increment;

    public ImageryLabelingPreferences() {

    }

    public ImageryLabelingPreferences(ImageryLabelingPreferences prefs) {
        if (prefs.values != null) {
            this.values = new float[prefs.values.length];
            System.arraycopy(prefs.values, 0, this.values, 0,
                    this.values.length);
        }
        this.increment = prefs.increment;
        this.createNegativeValues = prefs.createNegativeValues;
        this.labelFormat = prefs.labelFormat;
        this.labelSpacing = prefs.labelSpacing;
        this.labelTrimLeft = prefs.labelTrimLeft;
        this.maxLabel = prefs.maxLabel;
        this.maxMinTrimLeft = prefs.maxMinTrimLeft;
        this.minLabel = prefs.minLabel;
        this.minMaxLabelFormat = prefs.minMaxLabelFormat;
        this.numberOfContours = prefs.numberOfContours;
    }

    public float[] getValues() {
        return values;
    }

    public float getIncrement() {
        return increment;
    }

    public void setValues(float[] values) {
        this.values = values;
    }

    public void setIncrement(float increment) {
        this.increment = increment;
    }

    @Override
    public ImageryLabelingPreferences clone() {
        return new ImageryLabelingPreferences(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Float.floatToIntBits(increment);
        result = prime * result + Arrays.hashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImageryLabelingPreferences other = (ImageryLabelingPreferences) obj;
        if (Float.floatToIntBits(increment) != Float
                .floatToIntBits(other.increment))
            return false;
        if (!Arrays.equals(values, other.values))
            return false;
        return true;
    }

}
