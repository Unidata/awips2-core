package com.raytheon.uf.edex.requestsrv.logging;

import javax.xml.bind.annotation.*;

/**
 * Class used to represent a single attribute filter definition.
 * Two filters exist for attributes: maxLength and enabled.  If
 * maxLength is defined for an attribute, string values longer than
 * the specified length will be truncated to that length + "...".
 * Note that the absolute string maximum length will override this
 * value if it's shorter.  All attributes are enabled by default.
 * To disable output of an attribute, set enabled to false in the
 * config file.
 *  
 * @author Brian Rapp
 * @version 1.0
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#    Engineer    Description
 * ------------- ---------- ----------- --------------------------------------------
 * Mar 8, 2020   DCS 21885  brapp       Initial creation
 * </pre>
 */
@XmlRootElement(name="attribute")
@XmlAccessorType(XmlAccessType.NONE)
public class ClassAttribute {
    @XmlAttribute 
    private String name;

    @XmlAttribute
    private boolean enabled = true;

    private int maxLength = -1;

    /**
     * Getter for attribute name
     * @return String
     *     name of attribute
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for attribute logging enabled flag
     * @return boolean
     *     true if enabled; false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Getter for maximum string length
     * @return int
     *     maximum length of the attribute value to write to log
     */
    public int getMaxLength() {
        return maxLength;
    }
}
