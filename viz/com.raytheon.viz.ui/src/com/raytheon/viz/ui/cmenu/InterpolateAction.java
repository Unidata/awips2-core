package com.raytheon.viz.ui.cmenu;

import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;

/**
 * Right-click image interpolation toggle
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Apr 30, 2016             mjames    Initial Creation.
 * 
 * </pre>
 * 
 * @author mjames
 * 
 */
public class InterpolateAction extends AbstractRightClickAction {

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run() {
		ImagingCapability imgCap = getTopMostSelectedResourcePair()
				.getResource().getCapability(ImagingCapability.class);
		boolean isEnabled = imgCap.isInterpolationState();
		imgCap.setInterpolationState(!isEnabled);
        this.setChecked(isEnabled);
	}
	
    /* (non-Javadoc)
     * @see com.raytheon.viz.ui.cmenu.AbstractRightClickAction#setSelectedRsc(com.raytheon.uf.viz.core.drawables.ResourcePair)
     */
    @Override
    public void setSelectedRsc(ResourcePair selectedRsc) {
        super.setSelectedRsc(selectedRsc);
        ImagingCapability imgCap = selectedRsc.getResource()
        		.getCapability(ImagingCapability.class);
        boolean isEnabled = imgCap.isInterpolationState();
        this.setChecked(isEnabled);
    }
    
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#getText()
	 */
	@Override
	public String getText() {
		return "Interpolate";
	}
}
