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

import java.util.ArrayList;
import java.util.Map;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IRefreshListener;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;

/**
 * Group Resource abstract parent class.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------
 * Jan 28, 2011  3298     mpduff    Initial creation
 * Sep 12, 2016  3241     bsteffen  Move to uf.viz.core.rsc plugin
 * 
 * </pre>
 * 
 * @author mpduff
 */
public class VizGroupResource extends
        AbstractVizResource<VizGroupResourceData, MapDescriptor> implements
        IResourceDataChanged, IRefreshListener, IResourceGroup {

    private static final String NO_DATA = "No Data";

    protected VizGroupResource(VizGroupResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        dataTimes = new ArrayList<DataTime>();
    }

    @Override
    protected void disposeInternal() {
        for (ResourcePair rp : this.resourceData.resourceList) {
            if (rp.getResource() != null) {
                rp.getResource().dispose();
            }
        }
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        for (ResourcePair rp : this.resourceData.resourceList) {
            if (rp.getResource() != null) {
                paintProps.setDataTime(descriptor.getTimeForResource(rp
                        .getResource()));
                rp.getResource().paint(target, paintProps);
            }
        }
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        ResourceList rl = resourceData.resourceList;
        String value = "No Data";
        for (ResourcePair pair : rl) {
            if (pair.getResource() != null) {
                AbstractVizResource<?, ?> rsc = pair.getResource();
                value = rsc.inspect(coord);
                if (NO_DATA.equalsIgnoreCase(value) == false) {
                    return value;
                }
            }
        }

        return value;
    }

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        // TODO Auto-generated method stub
        return super.interrogate(coord);
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        for (AbstractVizResource<?, ?> resource : resourceData.getRscs()) {
            if (resource != null) {
                resource.init(target);
                resource.registerListener(this);
            }
        }
        // this.dataTimes = new
        // ArrayList<DataTime>(resourceData.getMap().keySet());

        // If child resources have capabilities that this does not, steal them
        for (AbstractVizResource<?, ?> rcs : getResourceData().getRscs()) {
            for (AbstractCapability capability : rcs.getCapabilities()
                    .getCapabilityClassCollection()) {
                // if (!hasCapability(capability.getClass())) {
                this.getCapabilities().addCapability(capability);
                capability.setResourceData(resourceData);
                // }
            }
        }
        // Spread my master capability set to all my children
        for (AbstractCapability capability : getCapabilities()
                .getCapabilityClassCollection()) {
            for (AbstractVizResource<?, ?> rcs : getResourceData().getRscs()) {
                rcs.getCapabilities().addCapability(capability);
            }
        }
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        // TODO Auto-generated method stub

    }

    @Override
    public ResourceList getResourceList() {
        return this.getResourceData().getResourceList();
    }
}
