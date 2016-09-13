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
package com.raytheon.uf.edex.database.purge;

import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

/**
 * POJO defining the text and/or regex (when the {@link #regex} flag is set)
 * that the data purger should match on to determine which rules apply.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 19, 2016 5262       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

@XmlAccessorType(XmlAccessType.NONE)
public class PurgeKeyValue {

    @XmlAttribute(required = false)
    private boolean regex = false;

    @XmlValue
    private String keyValue;

    private Pattern keyValuePattern;

    public PurgeKeyValue() {
    }

    public PurgeKeyValue(final String keyValue) {
        this.keyValue = keyValue;
    }

    public void initRegex() throws Exception {
        this.keyValuePattern = Pattern.compile(keyValue);
    }

    /**
     * @return the regex
     */
    public boolean isRegex() {
        return regex;
    }

    /**
     * @param regex
     *            the regex to set
     */
    public void setRegex(boolean regex) {
        this.regex = regex;
    }

    /**
     * @return the keyValue
     */
    public String getKeyValue() {
        return keyValue;
    }

    /**
     * @param keyValue
     *            the keyValue to set
     */
    public void setKeyValue(String keyValue) {
        this.keyValue = keyValue;
    }

    /**
     * @return the keyValuePattern
     */
    public Pattern getKeyValuePattern() {
        return keyValuePattern;
    }

    /**
     * @param keyValuePattern
     *            the keyValuePattern to set
     */
    public void setKeyValuePattern(Pattern keyValuePattern) {
        this.keyValuePattern = keyValuePattern;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("PurgeKeyValue [");
        sb.append("regex=").append(this.regex);
        sb.append(", keyValue=").append(this.keyValue).append("]");

        return sb.toString();
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((keyValue == null) ? 0 : keyValue.hashCode());
        result = prime * result + (regex ? 1231 : 1237);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PurgeKeyValue other = (PurgeKeyValue) obj;
        if (keyValue == null) {
            if (other.keyValue != null)
                return false;
        } else if (!keyValue.equals(other.keyValue))
            return false;
        if (regex != other.regex)
            return false;
        return true;
    }
}