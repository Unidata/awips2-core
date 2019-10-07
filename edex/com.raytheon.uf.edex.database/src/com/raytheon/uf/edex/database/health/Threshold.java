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
package com.raytheon.uf.edex.database.health;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.util.SizeUtil;

/**
 * Minimum table/index size and bloat percent for a given warning / critical
 * threshold.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 10, 2016 4630       rjpeter     Initial creation
 * Jul 18, 2019 7840       mroos       Remade class to be represented in XML
 * 
 * </pre>
 * 
 * @author rjpeter
 */

@XmlAccessorType(XmlAccessType.NONE)
public class Threshold {

    @XmlAttribute(name = "size")
    @DynamicSerializeElement
    // The minimum size of the table to which the threshold applies
    private long size;

    @XmlAttribute(name = "warningPercent")
    @DynamicSerializeElement
    // The bloat percent at which the table bloat is within warning range.
    private double warningPercent = 100;

    @XmlAttribute(name = "criticalPercent")
    @DynamicSerializeElement
    // The bloat percent at which the table bloat is considered critical.
    private double criticalPercent = 100;

    /**
     * @return the size.
     */
    public long getSize() {
        return size;
    }

    /**
     * Sets the size.
     * 
     * @param size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * @return warning bloat percent
     */
    public double getWarningPercent() {
        return warningPercent;
    }

    /**
     * Sets the warning bloat percent.
     * 
     * @param warningPercent
     */
    public void setWarningPercent(double warningPercent) {
        this.warningPercent = warningPercent;
    }

    /**
     * @return critical bloat percent
     */
    public double getCriticalPercent() {
        return criticalPercent;
    }

    /**
     * Sets the critical bloat percent.
     * 
     * @param criticalPercent
     */
    public void setCriticalPercent(double criticalPercent) {
        this.criticalPercent = criticalPercent;
    }

    /**
     * @return the size of the threshold in bytes rather than megabytes
     */
    public double getSizeInBytes() {
        return getSize() * SizeUtil.BYTES_PER_MB;
    }
}
