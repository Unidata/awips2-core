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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

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
public class ContourLabelingPreferences extends AbstractLabelingPreferences {

    @XmlElement(name = "values")
    private List<ValuesLabelingPreferences> values = new ArrayList<>();

    @XmlElement(name = "increment")
    private List<IncrementLabelingPreferences> increment = new ArrayList<>();

    public ContourLabelingPreferences() {
    }

    public List<IncrementLabelingPreferences> getIncrement() {
        return increment;
    }

    public void setIncrement(List<IncrementLabelingPreferences> increment) {
        this.increment = increment;
    }

    public ContourLabelingPreferences(ContourLabelingPreferences prefs) {

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

        Iterator<ValuesLabelingPreferences> iterator = prefs.getValues()
                .iterator();

        while (iterator.hasNext()) {
            this.values
                    .add((ValuesLabelingPreferences) iterator.next().clone());
        }

        Iterator<IncrementLabelingPreferences> iterator1 = prefs.getIncrement()
                .iterator();

        while (iterator.hasNext()) {
            this.increment.add(
                    (IncrementLabelingPreferences) iterator1.next().clone());
        }
    }

    @Override
    public ContourLabelingPreferences clone() {
        return new ContourLabelingPreferences(this);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((increment == null) ? 0 : increment.hashCode());
        result = prime * result + ((values == null) ? 0 : values.hashCode());
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
        ContourLabelingPreferences other = (ContourLabelingPreferences) obj;
        if (increment == null) {
            if (other.increment != null)
                return false;
        } else if (!increment.equals(other.increment))
            return false;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }

    public List<ValuesLabelingPreferences> getValues() {
        return values;
    }

    public void setValues(List<ValuesLabelingPreferences> values) {
        this.values = values;
    }

}
