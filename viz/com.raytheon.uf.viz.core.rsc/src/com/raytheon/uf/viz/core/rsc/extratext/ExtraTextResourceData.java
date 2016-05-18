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
package com.raytheon.uf.viz.core.rsc.extratext;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;

/**
 * Resource data for an {@link ExtraTextResource}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------
 * Jul 29, 2010  5991     mschenke  Initial creation
 * May 18, 2016  3253     bsteffen  Move to core, rename, simplify removal.
 * 
 * </pre>
 * 
 * @author mschenke
 */
@XmlAccessorType(XmlAccessType.NONE)
public class ExtraTextResourceData extends AbstractResourceData {

    /**
     * This method should be called during initialization by any resource
     * implementing {@link IExtraTextGeneratingResource} to ensure that the
     * descriptor contains an {@link ExtraTextResource}. If the descriptor
     * already has an ExtraTextResource than this method does nothing, otherwise
     * it adds an ExtraTextResource to the descriptor.
     * 
     * @param descriptor
     */
    public static void addExtraTextResource(IDescriptor descriptor) {
        List<AbstractVizResource<?, ?>> existing = descriptor.getResourceList()
                .getResourcesByType(ExtraTextResource.class);
        if (!existing.isEmpty()) {
            return;
        }
        ExtraTextResourceData rtrd = new ExtraTextResourceData();
        ResourcePair rp = new ResourcePair();
        ResourceProperties props = new ResourceProperties();
        props.setSystemResource(true);
        rp.setProperties(props);
        rp.setLoadProperties(new LoadProperties());
        rp.setResourceData(rtrd);
        descriptor.getResourceList().add(rp);
        descriptor.getResourceList().instantiateResources(descriptor, true);
    }

    @Override
    public AbstractVizResource<?, ?> construct(LoadProperties loadProperties,
            IDescriptor descriptor) throws VizException {
        return new ExtraTextResource(this, loadProperties);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return true;
    }

    @Override
    public void update(Object updateData) {
        // ignore updates
    }

}
