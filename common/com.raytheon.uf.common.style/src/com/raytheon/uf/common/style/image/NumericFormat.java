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
package com.raytheon.uf.common.style.image;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Sample format for numeric values
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 02, 2020  8145     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement
public class NumericFormat extends SampleFormat {

    /**
     * Default format
     */
    public static final NumericFormat DEFAULT = new NumericFormat();

    @XmlElement
    private Double minValue;

    @XmlElement
    private Double maxValue;

    @XmlElement
    private String pattern = "0.00";

    private java.text.DecimalFormat df;

    /**
     * Nullary constructor for jaxb serialization
     */
    @SuppressWarnings("unused")
    private NumericFormat() {

    }

    /**
     * @param pattern
     */
    public NumericFormat(String pattern) {
        this.pattern = pattern;
        this.df = getDf();
    }

    /**
     * @param minValue
     * @param maxValue
     */
    public NumericFormat(Double minValue, Double maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * Copy constructor
     *
     * @param other
     */
    private NumericFormat(NumericFormat other) {
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
        this.pattern = other.pattern;
    }

    @Override
    public SampleFormat clone() {
        return new NumericFormat(this);
    }

    @Override
    public String format(Object value, String unitString) {
        if (value instanceof Number) {
            double d = ((Number) value).doubleValue();
            if (minValue != null && d < minValue) {
                return "<" + getDf().format(minValue) + unitString;
            } else if (maxValue != null && d > maxValue) {
                return ">" + getDf().format(maxValue) + unitString;
            }
        }
        return getDf().format(value) + unitString;
    }

    /**
     * @return the minValue, null if unset
     */
    public Double getMinValue() {
        return minValue;
    }

    /**
     * @param minValue
     *            the minValue to set
     */
    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }

    /**
     * @return the maxValue, null if unset
     */
    public Double getMaxValue() {
        return maxValue;
    }

    /**
     * @param maxValue
     *            the maxValue to set
     */
    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern
     *            the pattern to set
     */
    public synchronized void setPattern(String pattern) {
        this.pattern = pattern;
        this.df = null;
    }

    private synchronized java.text.DecimalFormat getDf() {
        if (df == null) {
            df = new java.text.DecimalFormat(pattern);
        }
        return df;
    }

}
