package com.raytheon.uf.edex.requestsrv.logging;

import java.util.List;
import java.util.ArrayList;
import javax.xml.bind.annotation.*;

/**
 * Class used by JAXB to transform request logging configuration from XML to POJOs.
 * This class maps a single request filter containing zero or more attribute filters.
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
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name="request")
public class RawRequestFilter {
    @XmlAttribute(name="class")
    private String className;

    @XmlAttribute
    private boolean enabled = true;    // Logging for each request type defaults to true

    /* 
     * Attributes are loaded from the XML file into this ArrayList.  
     * Call attributesToMap() to copy the attributes to attributeMap HashMap.
     * The HashMap provides efficient access by attribute name.
     */
    @XmlElementWrapper(name="attributes", required=false)
    @XmlElement(name="attribute", required=false)
    private List<ClassAttribute> attributes = new ArrayList<>();

    /**
     * Getter for request class name
     * @return String
     *     fully qualified request class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Getter for enabled flag
     * @return boolean
     *     true if logging for this request type is enabled; false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Getter for configured request attributes
     * @return List
     *     List of configured {@link ClassAttribute}s
     */
    public List<ClassAttribute> getAttributes() {
        return attributes;
    }
}
