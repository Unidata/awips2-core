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
package com.raytheon.uf.viz.core.rsc.sampling;

import org.eclipse.swt.widgets.Event;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.rsc.IContainerAwareInputHandler;
import com.raytheon.viz.ui.input.InputAdapter;
import org.locationtech.jts.geom.Coordinate;

/**
 * Default input handler for sampling
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Jun 17, 2011  9730     mschenke  Initial creation
 * Aug 08, 2016  2676     bsteffen  Track container for reliable unregister()
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class SamplingInputAdapter<T extends SamplingResource> extends
        InputAdapter implements IContainerAwareInputHandler {

    private T resource;

    private IDisplayPaneContainer container;

    public SamplingInputAdapter(T resource) {
        this.resource = resource;
    }

    @Override
    public boolean handleMouseMove(int x, int y) {
        IDisplayPaneContainer container = resource.getResourceContainer();
        Coordinate c = container.translateClick(x, y);
        if (c != null) {
            resource.sampleCoord = new ReferencedCoordinate(c);
        } else {
            resource.sampleCoord = null;
        }
        if (resource.isSampling()) {
            resource.issueRefresh();
        }
        return false;
    }

    @Override
    public boolean handleMouseDownMove(int x, int y, int mouseButton) {
        return handleMouseMove(x, y);
    }

    @Override
    public boolean handleMouseExit(Event event) {
        resource.sampleCoord = null;
        if (resource.isSampling()) {
            resource.issueRefresh();
        }
        return false;
    }

    @Override
    public boolean handleMouseEnter(Event event) {
        return handleMouseMove(event.x, event.y);
    }

    public void register(IDisplayPaneContainer container) {
        if (container != null) {
            container.registerMouseHandler(this, InputPriority.SYSTEM_RESOURCE);
            /* This should call setContainer() automatically. */
        }
    }

    public void unregister() {
        if (this.container != null) {
            container.unregisterMouseHandler(this);
        }
    }

    @Override
    public void setContainer(IDisplayPaneContainer container) {
        this.container = container;
    }
}
