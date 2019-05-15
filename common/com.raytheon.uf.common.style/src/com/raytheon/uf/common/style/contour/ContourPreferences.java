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

package com.raytheon.uf.common.style.contour;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.ContourLabelingPreferences;

/**
 * 
 * ContourPreferences
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * Oct 18, 2007           chammack  Initial Creation.
 * Apr 26, 2017  6247     bsteffen  Improve clone
 * 
 * </pre>
 * 
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "contourStyle")
public class ContourPreferences extends AbstractStylePreferences {

    @XmlElement
    private ContourLabelingPreferences contourLabeling;

    @XmlElement
    private String positiveLinePattern;

    @XmlElement
    private String negativeLinePattern;

    @XmlElement
    private Double smoothingDistance;

    public ContourPreferences() {
        super();
    }

    public ContourPreferences(ContourPreferences prefs) {
        super(prefs);
        this.contourLabeling = prefs.contourLabeling.clone();
        this.positiveLinePattern = prefs.positiveLinePattern;
        this.negativeLinePattern = prefs.negativeLinePattern;
        this.smoothingDistance = prefs.smoothingDistance;
    }

    @Override
    public ContourPreferences clone() {
        return new ContourPreferences(this);
    }

    public ContourLabelingPreferences getContourLabeling() {
        return contourLabeling;
    }

    public void setContourLabeling(ContourLabelingPreferences contourLabeling) {
        this.contourLabeling = contourLabeling;
    }

    public String getPositiveLinePattern() {
        return positiveLinePattern;
    }

    public void setPositiveLinePattern(String positiveLinePattern) {
        this.positiveLinePattern = positiveLinePattern;
    }

    public String getNegativeLinePattern() {
        return negativeLinePattern;
    }

    public void setNegativeLinePattern(String negativeLinePattern) {
        this.negativeLinePattern = negativeLinePattern;
    }

    /**
     * @return the smoothingDistance (km)
     */
    public Double getSmoothingDistance() {
        return smoothingDistance;
    }

    /**
     * @param smoothingDistance
     *            (km) the smoothingDistance to set
     */
    public void setSmoothingDistance(Double smoothingDistance) {
        this.smoothingDistance = smoothingDistance;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + ((contourLabeling == null) ? 0 : contourLabeling.hashCode());
        result = prime * result + ((negativeLinePattern == null) ? 0
                : negativeLinePattern.hashCode());
        result = prime * result + ((positiveLinePattern == null) ? 0
                : positiveLinePattern.hashCode());
        result = prime * result + ((smoothingDistance == null) ? 0
                : smoothingDistance.hashCode());
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
        ContourPreferences other = (ContourPreferences) obj;
        if (contourLabeling == null) {
            if (other.contourLabeling != null) {
                return false;
            }
        } else if (!contourLabeling.equals(other.contourLabeling)) {
            return false;
        }
        if (negativeLinePattern == null) {
            if (other.negativeLinePattern != null) {
                return false;
            }
        } else if (!negativeLinePattern.equals(other.negativeLinePattern)) {
            return false;
        }
        if (positiveLinePattern == null) {
            if (other.positiveLinePattern != null) {
                return false;
            }
        } else if (!positiveLinePattern.equals(other.positiveLinePattern)) {
            return false;
        }
        if (smoothingDistance == null) {
            if (other.smoothingDistance != null) {
                return false;
            }
        } else if (!smoothingDistance.equals(other.smoothingDistance)) {
            return false;
        }
        return true;
    }

}
