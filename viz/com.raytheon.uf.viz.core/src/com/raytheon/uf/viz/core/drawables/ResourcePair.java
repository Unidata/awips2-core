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
package com.raytheon.uf.viz.core.drawables;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.commons.lang3.Validate;

import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource.ResourceStatus;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;

/**
 * ResourcePair pairs together a resource and its ResourceProperties
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------------
 * Sep 05, 2007           chammack  Initial Creation.
 * Oct 22, 2013  2491     bsteffen  Remove ISerializableObject
 * Jan 04, 2018  6753     bsteffen  Instantiate resources in a group in order.
 * Mar 09, 2018  6731     bsteffen  Remove resources that fail instantiation from groups.
 * 
 * </pre>
 * 
 * @author chammack
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "pair")
public class ResourcePair {

    protected AbstractVizResource<?, ?> resource;

    protected ResourceProperties properties;

    protected transient AbstractResourceData resourceData;

    protected transient LoadProperties loadProperties;

    public AbstractVizResource<?, ?> getResource() {
        return resource;
    }

    public void setResource(AbstractVizResource<?, ?> resource) {
        this.resource = resource;
        if (this.properties != null) {
            this.properties.setResource(resource);
        }
    }

    @XmlElement
    public AbstractResourceData getResourceData() {
        if (this.resource != null) {
            return this.resource.getResourceData();
        }
        return resourceData;
    }

    @XmlElement
    public LoadProperties getLoadProperties() {
        if (this.resource != null) {
            return this.resource.getLoadProperties();
        }
        return this.loadProperties;
    }

    public void setLoadProperties(LoadProperties loadProperties) {
        this.loadProperties = loadProperties;
    }

    public void setResourceData(AbstractResourceData resourceData) {
        this.resourceData = resourceData;
    }

    @XmlElement
    public ResourceProperties getProperties() {
        return properties;
    }

    public void setProperties(ResourceProperties properties) {
        this.properties = properties;
        if (properties != null) {
            properties.setResource(this.resource);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((properties == null) ? 0 : properties.hashCode());
        result = prime * result
                + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ResourcePair other = (ResourcePair) obj;
        if (properties == null) {
            if (other.properties != null) {
                return false;
            }
        } else if (!properties.equals(other.properties)) {
            return false;
        }

        AbstractResourceData ours = getResourceData(this);
        AbstractResourceData theirs = getResourceData(other);

        if (ours != null && theirs == null) {
            return false;
        } else if (ours == null && theirs != null) {
            return false;
        } else if (ours != null && !ours.equals(theirs)) {
            return false;
        }

        LoadProperties one = getLoadProperties();
        LoadProperties two = other.getLoadProperties();

        if (one != null && two == null) {
            return false;
        } else if (one == null && two != null) {
            return false;
        } else if (one != null && !one.equals(two)) {
            return false;
        }

        return true;
    }

    /**
     * Gets the resource data for the resource pair, checks the resourceData in
     * the pair then checks the resource in the pair
     * 
     * @param pair
     * @return
     */
    private static AbstractResourceData getResourceData(ResourcePair pair) {
        AbstractResourceData rscData = pair.resourceData;
        if (rscData == null) {
            if (pair.resource != null) {
                rscData = pair.getResourceData();
            }
        }
        return rscData;
    }

    public boolean instantiateResource(IDescriptor descriptor)
            throws VizException {
        return instantiateResource(descriptor, true);
    }

    @SuppressWarnings("unchecked")
    public boolean instantiateResource(IDescriptor descriptor,
            boolean fireListeners) throws VizException {
        boolean success;
        Validate.isTrue(
                this.resource == null
                        || this.resource.getStatus() == ResourceStatus.DISPOSED,
                "Resource is already instantiated");
        Validate.notNull(descriptor, "Must provide descriptor");
        Validate.notNull(this.resourceData, "resourceData is null");

        if (this.loadProperties == null) {
            this.loadProperties = new LoadProperties();
        }

        AbstractVizResource rsc = this.resourceData
                .construct(this.loadProperties, descriptor);
        if (rsc == null) {
            success = false;
        } else {
            rsc.setDescriptor(descriptor);
            if (this.resourceData instanceof IResourceGroup) {
                ResourceList rscList = ((IResourceGroup) this.resourceData)
                        .getResourceList();
                AbstractTimeMatcher timeMatcher = descriptor.getTimeMatcher();
                List<ResourcePair> orderedList = rscList;
                if (timeMatcher != null) {
                    orderedList = timeMatcher.getResourceLoadOrder(rscList);
                }
                for (ResourcePair rp : orderedList) {
                    if (rp.getResource() == null) {
                        if (rp.instantiateResource(descriptor, false)) {
                            rscList.firePostAddListeners(rp);
                        } else {
                            rscList.remove(rp);
                        }
                    }
                }
            }
            setResource(rsc);

            if (fireListeners) {
                descriptor.getResourceList().firePreAddListeners(this);
                descriptor.getResourceList().firePostAddListeners(this);
            }

            this.resourceData = null;
            this.loadProperties = null;
            success = true;
        }
        return success;
    }

    @Override
    public String toString() {
        return "Resource: " + resource + " Properties: " + properties;
    }

    /**
     * Constructs a system resource with the resource data using a default
     * LoadProperties and ResourceProperties, ignores errors on construction so
     * resource in pair may be null
     * 
     * @param resourceData
     * @return
     */
    public static ResourcePair constructSystemResourcePair(
            AbstractResourceData resourceData) {
        ResourcePair rp = new ResourcePair();
        rp.setProperties(new ResourceProperties());
        rp.getProperties().setSystemResource(true);
        rp.setLoadProperties(new LoadProperties());
        rp.setResourceData(resourceData);
        return rp;
    }

}
