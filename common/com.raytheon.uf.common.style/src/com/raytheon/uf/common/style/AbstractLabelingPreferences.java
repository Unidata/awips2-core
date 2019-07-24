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
 * Jul 12, 2007           chammack  Initial Creation.
 * Apr 26, 2017  6247     bsteffen  Implement clone
 * Apr 30, 2018  6697     bsteffen  Add zoomLock
 * May 07, 2019  64008    ksunil    Changed the structure, renamed to new abstract base
 *
 * </pre>
 *
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
public abstract class AbstractLabelingPreferences {

    @XmlAttribute
    protected int labelSpacing;

    @XmlAttribute
    protected String minLabel;

    @XmlAttribute
    protected String maxLabel;

    @XmlAttribute
    protected String labelFormat;

    @XmlAttribute
    protected String minMaxLabelFormat;

    @XmlAttribute
    protected int numberOfContours = -1;

    @XmlAttribute
    protected boolean createNegativeValues = false;

    @XmlAttribute
    protected int labelTrimLeft = 0;

    @XmlAttribute
    protected int maxMinTrimLeft = 0;

    @XmlAttribute
    protected boolean zoomLock = false;

    public String getMinLabel() {
        return minLabel;
    }

    public void setMinLabel(String minLabel) {
        this.minLabel = minLabel;
    }

    public String getMaxLabel() {
        return maxLabel;
    }

    public void setMaxLabel(String maxLabel) {
        this.maxLabel = maxLabel;
    }

    public String getLabelFormat() {
        return labelFormat;
    }

    public void setLabelFormat(String labelFormat) {
        this.labelFormat = labelFormat;
    }

    public String getMinMaxLabelFormat() {
        return minMaxLabelFormat;
    }

    public void setMinMaxLabelFormat(String minMaxLabelFormat) {
        this.minMaxLabelFormat = minMaxLabelFormat;
    }

    public int getLabelSpacing() {
        return labelSpacing;
    }

    public void setLabelSpacing(int labelSpacing) {
        this.labelSpacing = labelSpacing;
    }

    public int getNumberOfContours() {
        return numberOfContours;
    }

    public void setNumberOfContours(int numberOfContours) {
        this.numberOfContours = numberOfContours;
    }

    public boolean isCreateNegativeValues() {
        return createNegativeValues;
    }

    public void setCreateNegativeValues(boolean createNegativeValues) {
        this.createNegativeValues = createNegativeValues;
    }

    public int getLabelTrimLeft() {
        return labelTrimLeft;
    }

    public void setLabelTrimLeft(int labelTrimLeft) {
        this.labelTrimLeft = labelTrimLeft;
    }

    public int getMaxMinTrimLeft() {
        return maxMinTrimLeft;
    }

    public void setMaxMinTrimLeft(int maxMinTrimLeft) {
        this.maxMinTrimLeft = maxMinTrimLeft;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (createNegativeValues ? 1231 : 1237);
        result = prime * result
                + ((labelFormat == null) ? 0 : labelFormat.hashCode());
        result = prime * result + labelSpacing;
        result = prime * result + labelTrimLeft;
        result = prime * result
                + ((maxLabel == null) ? 0 : maxLabel.hashCode());
        result = prime * result + maxMinTrimLeft;
        result = prime * result
                + ((minLabel == null) ? 0 : minLabel.hashCode());
        result = prime * result + ((minMaxLabelFormat == null) ? 0
                : minMaxLabelFormat.hashCode());
        result = prime * result + numberOfContours;
        result = prime * result + (zoomLock ? 1231 : 1237);
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
        AbstractLabelingPreferences other = (AbstractLabelingPreferences) obj;
        if (createNegativeValues != other.createNegativeValues)
            return false;
        if (labelFormat == null) {
            if (other.labelFormat != null)
                return false;
        } else if (!labelFormat.equals(other.labelFormat))
            return false;
        if (labelSpacing != other.labelSpacing)
            return false;
        if (labelTrimLeft != other.labelTrimLeft)
            return false;
        if (maxLabel == null) {
            if (other.maxLabel != null)
                return false;
        } else if (!maxLabel.equals(other.maxLabel))
            return false;
        if (maxMinTrimLeft != other.maxMinTrimLeft)
            return false;
        if (minLabel == null) {
            if (other.minLabel != null)
                return false;
        } else if (!minLabel.equals(other.minLabel))
            return false;
        if (minMaxLabelFormat == null) {
            if (other.minMaxLabelFormat != null)
                return false;
        } else if (!minMaxLabelFormat.equals(other.minMaxLabelFormat))
            return false;
        if (numberOfContours != other.numberOfContours)
            return false;
        if (zoomLock != other.zoomLock)
            return false;
        return true;
    }

    /**
     * When zoomLock is true then the provided values or increment should always
     * be used regardless of the zoom level. When it is false then the provided
     * values or increment are used as a base and zooming in or out will
     * increase or decrease the intervals used.
     * 
     * @return the zoom lock status.
     */
    public boolean isZoomLock() {
        return zoomLock;
    }

    /**
     * @see #isZoomLock()
     * 
     * @param zoomLock
     *            the zoomLock status
     */
    public void setZoomLock(boolean zoomLock) {
        this.zoomLock = zoomLock;
    }

}
