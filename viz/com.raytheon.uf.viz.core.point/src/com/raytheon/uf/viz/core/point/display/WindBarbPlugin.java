package com.raytheon.uf.viz.core.point.display;

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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * POJO container of WindBarbPlugin configuration.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2018 #57905      E. Debebe   Initial creation
 *
 * </pre>
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class WindBarbPlugin {

    @XmlAttribute
    private String className;

    @XmlElement
    private Double baseSize;

    @XmlElement
    private Double offsetRatio;

    @XmlElement
    private Double minimumMagnitude;

    @XmlElement
    private Double barbRotationDegrees;

    @XmlElement
    private Double barbLengthRatio;

    @XmlElement
    private Double barbSpacingRatio;

    @XmlElement
    private Boolean barbFillFiftyTriangle;

    @XmlElement
    private Double calmCircleMaximumMagnitude;

    @XmlElement
    private Double calmCircleSizeRatio;

    @XmlElement
    private Double arrowHeadSizeRatio;

    @XmlElement
    private Double linearArrowScaleFactor;

    /**
     * @return the className
     */
    public String getClassName() {
        return className;
    }

    /**
     * @param className the className to set
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * @return the baseSize
     */
    public Double getBaseSize() {
        return baseSize;
    }

    /**
     * @param baseSize the baseSize to set
     */
    public void setBaseSize(Double baseSize) {
        this.baseSize = baseSize;
    }

    /**
     * @return the offsetRatio
     */
    public Double getOffsetRatio() {
        return offsetRatio;
    }

    /**
     * @param offsetRatio the offsetRatio to set
     */
    public void setOffsetRatio(Double offsetRatio) {
        this.offsetRatio = offsetRatio;
    }

    /**
     * @return the minimumMagnitude
     */
    public Double getMinimumMagnitude() {
        return minimumMagnitude;
    }

    /**
     * @param minimumMagnitude the minimumMagnitude to set
     */
    public void setMinimumMagnitude(Double minimumMagnitude) {
        this.minimumMagnitude = minimumMagnitude;
    }

    /**
     * @return the barbRotationDegrees
     */
    public Double getBarbRotationDegrees() {
        return barbRotationDegrees;
    }

    /**
     * @param barbRotationDegrees the barbRotationDegrees to set
     */
    public void setBarbRotationDegrees(Double barbRotationDegrees) {
        this.barbRotationDegrees = barbRotationDegrees;
    }

    /**
     * @return the barbLengthRatio
     */
    public Double getBarbLengthRatio() {
        return barbLengthRatio;
    }

    /**
     * @param barbLengthRatio the barbLengthRatio to set
     */
    public void setBarbLengthRatio(Double barbLengthRatio) {
        this.barbLengthRatio = barbLengthRatio;
    }

    /**
     * @return the barbSpacingRatio
     */
    public Double getBarbSpacingRatio() {
        return barbSpacingRatio;
    }

    /**
     * @param barbSpacingRatio the barbSpacingRatio to set
     */
    public void setBarbSpacingRatio(Double barbSpacingRatio) {
        this.barbSpacingRatio = barbSpacingRatio;
    }

    /**
     * @return the barbFillFiftyTriangle
     */
    public Boolean getBarbFillFiftyTriangle() {
        return barbFillFiftyTriangle;
    }

    /**
     * @param barbFillFiftyTriangle the barbFillFiftyTriangle to set
     */
    public void setBarbFillFiftyTriangle(Boolean barbFillFiftyTriangle) {
        this.barbFillFiftyTriangle = barbFillFiftyTriangle;
    }

    /**
     * @return the calmCircleMaximumMagnitude
     */
    public Double getCalmCircleMaximumMagnitude() {
        return calmCircleMaximumMagnitude;
    }

    /**
     * @param calmCircleMaximumMagnitude the calmCircleMaximumMagnitude to set
     */
    public void setCalmCircleMaximumMagnitude(Double calmCircleMaximumMagnitude) {
        this.calmCircleMaximumMagnitude = calmCircleMaximumMagnitude;
    }

    /**
     * @return the calmCircleSizeRatio
     */
    public Double getCalmCircleSizeRatio() {
        return calmCircleSizeRatio;
    }

    /**
     * @param calmCircleSizeRatio the calmCircleSizeRatio to set
     */
    public void setCalmCircleSizeRatio(Double calmCircleSizeRatio) {
        this.calmCircleSizeRatio = calmCircleSizeRatio;
    }

    /**
     * @return the arrowHeadSizeRatio
     */
    public Double getArrowHeadSizeRatio() {
        return arrowHeadSizeRatio;
    }

    /**
     * @param arrowHeadSizeRatio the arrowHeadSizeRatio to set
     */
    public void setArrowHeadSizeRatio(Double arrowHeadSizeRatio) {
        this.arrowHeadSizeRatio = arrowHeadSizeRatio;
    }

    /**
     * @return the linearArrowScaleFactor
     */
    public Double getLinearArrowScaleFactor() {
        return linearArrowScaleFactor;
    }

    /**
     * @param linearArrowScaleFactor the linearArrowScaleFactor to set
     */
    public void setLinearArrowScaleFactor(Double linearArrowScaleFactor) {
        this.linearArrowScaleFactor = linearArrowScaleFactor;
    }
}
