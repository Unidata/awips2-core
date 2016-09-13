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
package com.raytheon.uf.viz.core.rsc.groups;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IDescriptor.FramesInfo;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory.ResourceOrder;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.BlendableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.BlendedCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.rsc.interrogation.Interrogatable;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogateMap;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogationKey;

/**
 * A Blended resource is a combination of two resources displayed together with
 * an option to blend them by having inverse alpha values.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 31, 2014  3461     bsteffen  Recycle properly.
 * Sep 12, 2016  3241     bsteffen  Move to uf.viz.core.rsc plugin, implement
 *                                  Interrogatable
 * 
 * </pre>
 * 
 * @author randerso
 */
public class BlendedResource
        extends AbstractVizResource<BlendedResourceData, IDescriptor>
        implements IResourceGroup, Interrogatable {

    private ResourceOrder highestResourceOrder = null;

    public BlendedResource(BlendedResourceData data, LoadProperties props) {
        super(data, props);
        getCapability(BlendableCapability.class);
    }

    protected void addResourceInternal(AbstractVizResource<?, ?> res) {
        if (this.resourceData.getResourceList().size() < 2) {
            ResourcePair pair = new ResourcePair();
            pair.setResource(res);
            ResourceProperties rp = new ResourceProperties();
            rp.setVisible(true);
            pair.setProperties(rp);
            res.getCapabilities()
                    .addCapability(getCapability(ImagingCapability.class));
            this.resourceData.getResourceList().add(pair);
        } else {
            throw new UnsupportedOperationException(
                    "Cannot add more than two resources to a BlendedResource.");
        }
    }

    public void addResource(AbstractVizResource<?, ?> res) {
        addResourceInternal(res);
    }

    @Override
    protected void disposeInternal() {
        for (ResourcePair rp : this.resourceData.getResourceList()) {
            rp.getResource().dispose();
        }
    }

    @Override
    protected void recycleInternal() {
        for (ResourcePair rp : this.resourceData.getResourceList()) {
            rp.getResource().recycle();
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        ResourcePair me = new ResourcePair();
        me.setLoadProperties(getLoadProperties());
        me.setProperties(descriptor.getResourceList().getProperties(this));
        me.setResource(this);
        me.setResourceData(resourceData);
        int i = 0;
        for (ResourcePair rp : this.getResourceList()) {
            if (rp.getResource() != null) {
                BlendedCapability blendCap = rp.getResource()
                        .getCapability(BlendedCapability.class);
                blendCap.setBlendableResource(me);
                blendCap.setResourceIndex(i++);
                rp.getResource().init(target);
            } else {
                BlendedCapability blendCap = rp.getLoadProperties()
                        .getCapabilities().getCapability(rp.getResourceData(),
                                BlendedCapability.class);
                blendCap.setBlendableResource(me);
                blendCap.setResourceIndex(i++);
            }
        }
    }

    @Override
    public void setDescriptor(IDescriptor descriptor) {
        super.setDescriptor(descriptor);
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        PaintProperties newProps = new PaintProperties(paintProps);

        BlendableCapability bCap = getCapability(BlendableCapability.class);
        ImagingCapability iCap = getCapability(ImagingCapability.class);
        float alpha = bCap.getAlphaStep()
                / (float) BlendableCapability.BLEND_MAX;

        paintResource(target, bCap.getResourceIndex(), 1.0f - alpha,
                iCap.getBrightness(), iCap.getContrast(), newProps);
        newProps.setAlpha(1.0f - alpha);
        paintResource(target, 1 - bCap.getResourceIndex(), alpha,
                iCap.getBrightness(), iCap.getContrast(), newProps);

    }

    private void paintResource(IGraphicsTarget target, int index, float alpha,
            float brightness, float contrast, PaintProperties pProps)
                    throws VizException {
        if (index < 0 || index >= this.resourceData.getResourceList().size()) {
            return;
        }
        ResourcePair rp = this.resourceData.getResourceList().get(index);
        AbstractVizResource<?, ?> rsc = rp.getResource();
        if (rsc == null) {
            return;
        }
        ImagingCapability rscIcap = rsc.getCapability(ImagingCapability.class);
        rsc.getCapability(ColorableCapability.class)
                .setColor(getCapability(ColorableCapability.class).getColor());
        pProps.setAlpha(alpha);
        rscIcap.setAlpha(alpha, false);
        rscIcap.setBrightness(brightness, false);
        rscIcap.setContrast(contrast, false);
        pProps.setDataTime(this.descriptor.getTimeForResource(rsc));
        if (rp.getProperties().isVisible()) {
            rsc.paint(target, pProps);
        }
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        for (ResourcePair rp : this.resourceData.resourceList) {
            AbstractVizResource<?, ?> rsc = rp.getResource();
            rsc.project(mapData);
        }
    }

    @Override
    public ResourceList getResourceList() {
        return this.resourceData.getResourceList();
    }

    @Override
    public String inspect(ReferencedCoordinate latLon) throws VizException {
        ResourceList rl = this.resourceData.getResourceList();
        StringBuffer displayedData = new StringBuffer();
        for (int i = rl.size() - 1; i >= 0; --i) {
            ResourcePair rp = rl.get(i);
            if (rp.getResource() != null) {
                displayedData.append(rp.getResource().inspect(latLon) + "\n");
            }
        }
        return displayedData.toString();
    }

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        ResourceList rl = this.resourceData.getResourceList();
        Map<String, Object> resultMap = new HashMap<>();

        for (int i = 0; i < rl.size(); ++i) {
            ResourcePair rp = rl.get(i);
            if (rp.getProperties().isVisible()) {
                Map<String, Object> rscMap = rp.getResource()
                        .interrogate(coord);
                if (rscMap != null) {
                    resultMap.putAll(rp.getResource().interrogate(coord));
                }
            }
        }
        return resultMap;
    }

    @Override
    public Set<InterrogationKey<?>> getInterrogationKeys() {
        Set<InterrogationKey<?>> resultSet = new HashSet<>();

        for (ResourcePair pair : getResourceList()) {
            AbstractVizResource<?, ?> resource = pair.getResource();
            if (resource instanceof Interrogatable) {
                resultSet.addAll(
                        ((Interrogatable) resource).getInterrogationKeys());
            }
        }
        return resultSet;
    }

    @Override
    public InterrogateMap interrogate(ReferencedCoordinate coordinate,
            DataTime time, InterrogationKey<?>... keys) {
        FramesInfo framesInfo = descriptor.getFramesInfo();
        DataTime[] frameTimes = framesInfo.getFrameTimes();
        int timeIndex = -1;
        for (int i = 0; i < frameTimes.length; i += 1) {
            if (time.equals(frameTimes[i])) {
                timeIndex = i;
                break;
            }
        }

        InterrogateMap resultMap = new InterrogateMap();
        for (ResourcePair pair : getResourceList()) {
            AbstractVizResource<?, ?> resource = pair.getResource();
            if (resource instanceof Interrogatable) {
                DataTime rscTime = time;
                if (timeIndex >= 0) {
                    rscTime = framesInfo.getTimeForResource(resource,
                            timeIndex);
                }
                resultMap.putAll(((Interrogatable) resource)
                        .interrogate(coordinate, rscTime, keys));
            }
        }
        return resultMap;
    }

    @Override
    public ResourceOrder getResourceOrder() {
        if (highestResourceOrder == null) {
            String orderId = descriptor.getResourceList().getProperties(this)
                    .getRenderingOrderId();
            if (orderId != null) {
                highestResourceOrder = RenderingOrderFactory
                        .getRenderingOrder(orderId);
            } else {
                for (ResourcePair rp : getResourceList()) {
                    if (rp.getResource() != null) {
                        ResourceOrder order = rp.getResource()
                                .getResourceOrder();
                        if (highestResourceOrder == null
                                || highestResourceOrder.value < order.value) {
                            highestResourceOrder = order;
                        }
                    }
                }
            }
            if (highestResourceOrder == null) {
                highestResourceOrder = ResourceOrder.UNKNOWN;
            }
        }
        return highestResourceOrder;
    }

}
