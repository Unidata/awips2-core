package com.raytheon.uf.edex.requestsrv.logging;

import java.util.Map;
import java.util.HashMap;

/**
 * Class for storing logging configuration for a request.
 * Attributes are transformed from a list to a map keyed on the attribute name.
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
public class RequestFilter {
    private String className;

    private boolean enabled = true;    // Logging for each request type defaults to true

    private Map<String, ClassAttribute> attributeMap = new HashMap<>();

    /**
     * Constructor for RequestFilter.
     * @param req
     *     Raw request filter to transform into RequestFilter.  
     */
    public RequestFilter(RawRequestFilter req) {
        className = req.getClassName();
        enabled = req.isEnabled();
        for (ClassAttribute attr : req.getAttributes()) {
            addAttribute(attr);
        }
    }

    /**
     * Getter for class name
     * @return String containing fully qualified request class name
     */
    public String getClassName() {
        return className;
    }

    /**
     * Setter for class name
     * @param className
     *     String representing fully qualified request class name.
     */
    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * Getter for request enabled flag
     * @return true if enabled; false if disabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Setter for request enabled flag
     * @param enabled true to enable request; false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 
     * @return Map with Attribute names as keys, ClassAttribute object as value
     */
    public Map<String, ClassAttribute> getAttributeMap() {
        return attributeMap;
    }

    /**
     * Add an attribute object to the request object
     * @param attribute ClassAttribute object to be added to the map
     */
    public void addAttribute(ClassAttribute attribute) {
        attributeMap.put(attribute.getName(), attribute);
    }

    /**
     * Return the named attribute
     * @param attrName
     *     attribute name
     * @return ClassAttribute
     *     attribute object
     */
    public ClassAttribute getAttribute(String attrName) {
        return attributeMap.get(attrName);
    }
}
