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
package com.raytheon.viz.ui.cmenu;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.ResourceGroup;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;

/**
 * Duplicate a resource but with a different display type. Works only on
 * resources with the {@link DisplayTypeCapability}.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Apr 26, 2010           bsteffen   Initial creation
 * Aug 10, 2011           njensen    Added runWithEvent
 * Oct 22, 2013  2491     bsteffen   Switch serialization to ProcedureXmlManager
 * Apr 06, 2015  17215    dfriedman  Load in Job
 * Jun 11, 2015  17603    dfriedman  Fix 17215: Load to correct pane
 * Mar 14, 2018  6700     bsteffen   Move performLoad to a separate method to
 *                                   allow subclasses to customize.
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public abstract class LoadAsDisplayTypeAction extends AbstractRightClickAction {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LoadAsDisplayTypeAction.class);

    @Override
    public void run() {
        // Capture active pane now, then instantiate in a job.
        final IDescriptor descriptor = getDescriptor();
        Job job = new Job(
                "Loading as " + getDisplayType().toString().toLowerCase()) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                performLoad(descriptor, selectedRsc);
                return Status.OK_STATUS;
            }
        };
        job.schedule();
    }

    /**
     * the {@link #run()} method is on the UI thread so it starts a job which
     * calls this method to perform the actual loading.
     * 
     * @param descriptor
     *            the IDescriptor to load to
     * @param selectedRsc
     *            the resourcePair that was selected
     */
    protected void performLoad(IDescriptor descriptor,
            ResourcePair selectedRsc) {
        try {
            ProcedureXmlManager jaxb = ProcedureXmlManager.getInstance();
            ResourcePair rp = selectedRsc;
            ResourceGroup group = new ResourceGroup();
            group.getResourceList().add(rp);
            String xml = jaxb.marshal(group);
            group = jaxb.unmarshal(ResourceGroup.class, xml);
            rp = group.getResourceList().get(0);
            rp.setProperties(new ResourceProperties());
            rp.getLoadProperties().getCapabilities()
                    .getCapability(rp.getResourceData(),
                            DisplayTypeCapability.class)
                    .setDisplayType(getDisplayType());
            rp.instantiateResource(descriptor);
            descriptor.getResourceList().add(rp);
        } catch (VizException | SerializationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unexpected error cloning resource", e);
        }
    }

    @Override
    public String getText() {
        String typeString = getDisplayType().toString();
        typeString = typeString.substring(0, 1).toUpperCase()
                + typeString.substring(1).toLowerCase();
        return "Load as " + typeString;
    }

    @Override
    public boolean isHidden() {
        AbstractVizResource<?, ?> rsc = getSelectedRsc();
        if (rsc == null) {
            return true;
        }
        DisplayTypeCapability cap = rsc
                .getCapability(DisplayTypeCapability.class);

        return cap.getDisplayType() == getDisplayType()
                || !cap.getAlternativeDisplayTypes().contains(getDisplayType());
    }

    @Override
    public boolean isEnabled() {
        AbstractVizResource<?, ?> rsc = getSelectedRsc();
        AbstractResourceData rrd = null;
        if (rsc != null) {
            rrd = rsc.getResourceData();
        }

        if (rrd != null) {
            for (ResourcePair rp : rsc.getDescriptor().getResourceList()) {
                AbstractVizResource<?, ?> rsc2 = rp.getResource();
                if (rsc2 != null && rsc2 != rsc
                        && rrd.equals(rsc2.getResourceData())
                        && rsc2.getCapability(DisplayTypeCapability.class)
                                .getDisplayType() == getDisplayType()) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    protected abstract DisplayType getDisplayType();

}
