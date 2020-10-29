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
 * ------------- -------- --------- ------------------------------------
 * May 07, 2019  64008    ksunil    Initial Creation..
 * Oct 28, 2020  83998    tjensen   Corrected default min
 *
 * </pre>
 *
 * @author ksunil
 */
@XmlAccessorType(XmlAccessType.NONE)
public class IncrementLabelingPreferences extends BaseLabelingPreferences {

    @XmlAttribute
    private float min = -Float.MAX_VALUE;

    @XmlAttribute
    private float max = Float.MAX_VALUE;

    public IncrementLabelingPreferences() {
        super();
    }

    public IncrementLabelingPreferences(IncrementLabelingPreferences prefs) {
        if (prefs.values != null) {
            this.values = new float[prefs.values.length];
            System.arraycopy(prefs.values, 0, this.values, 0,
                    this.values.length);
        }
        this.thickness = prefs.thickness;
        this.lineStyle = prefs.lineStyle;
        this.color = prefs.color;
        this.max = prefs.max;
        this.min = prefs.min;
    }

    @Override
    public IncrementLabelingPreferences clone() {
        return new IncrementLabelingPreferences(this);
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

    @Override
    public int hashCode() {
        int result = super.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        return true;
    }

}
