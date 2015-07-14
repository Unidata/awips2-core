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
package com.raytheon.uf.viz.core.maps.scales;

import gov.noaa.nws.ncep.viz.common.area.IAreaProviderCapable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.geotools.coverage.grid.GeneralGridGeometry;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.maps.scales.MapScalesManager.ManagedMapScale;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.viz.satellite.rsc.SatResource;
import com.raytheon.viz.ui.EditorUtil;

/**
 * UI populator for map scales
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 7, 2010             mschenke    Initial creation
 * Mar 21, 2013       1638 mschenke    Made map scales not tied to d2d
 * Oct 10, 2013       2104 mschenke    Switched to use MapScalesManager
 * May 17, 2015			   mjames@ucar Added functionality to add loaded 
 * 									   area definitions as bundles
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class MapScalePopulator extends CompoundContributionItem {

	private String GINI_TYPE = "GINI_SECTOR_ID";
	private GeneralGridGeometry gridGeom;
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
     */
    @SuppressWarnings("null")
	@Override
    protected IContributionItem[] getContributionItems() {
    	
        MenuManager menuMgr = new MenuManager("Scales", "mapControls");
        IDisplayPaneContainer cont = EditorUtil.getActiveVizContainer();
        
        if ((cont != null && (cont.getActiveDisplayPane()
                .getRenderableDisplay() instanceof IMapScaleDisplay))
                || EditorUtil.getActiveEditor() == null) {

        	/*
             * Contribute default scales 
             * this method doesn't use event listeners, the commands are inserted to
             * CommandContributionItem/CommandContributionItemParameter
             */
            for (ManagedMapScale scale : MapScalesManager.getInstance()
                    .getScales()) {
            	if (!scale.getAreaScale()) {
	                Map<String, String> parms = new HashMap<String, String>();
	                parms.put(MapScaleHandler.SCALE_NAME_ID, scale.getDisplayName());
	                CommandContributionItem item = new CommandContributionItem(
	                        new CommandContributionItemParameter(
	                                PlatformUI.getWorkbench(), null,
	                                MapScaleHandler.SET_SCALE_COMMAND_ID, parms,
	                                null, null, null, scale.getDisplayName(), null,
	                                null, CommandContributionItem.STYLE_PUSH, null,
	                                true));
	                menuMgr.add(item);		
	        	}
            }

            /*
             * Contribute scales from loaded resources which provide an area (pre-rendered).
             */
            List<IAreaProviderCapable> rscList = new ArrayList<IAreaProviderCapable>();
            IDisplayPane pane = cont.getActiveDisplayPane();
        	IDescriptor desc = pane.getDescriptor();
        	/*
        	 * Query only the loaded Satellite Resources
        	 */
        	List<AbstractVizResource<?, ?>> rList = desc.getResourceList().getResourcesByType(
                    		SatResource.class);
        	for (AbstractVizResource rp : rList) {
        		if( rp.getResourceData() instanceof IAreaProviderCapable ) {
        			rscList.add( (IAreaProviderCapable) rp.getResourceData() );
        			String areaName = ((IAreaProviderCapable) rp.getResourceData()).getAreaName();
        			
        			/*
        			 * Only add item if it's not loaded already
        			 */
        			ManagedMapScale mms = MapScalesManager.getInstance().getScaleByName(areaName);
					
					for (ManagedMapScale scale : MapScalesManager.getInstance().getScales()) {
						if (scale.getDisplayName().equals(areaName) ) {

							if (scale.getAreaScale()) {
				            	
				                Map<String, String> parms = new HashMap<String, String>();
				                parms.put(MapScaleHandler.SCALE_NAME_ID, scale.getDisplayName());
				                CommandContributionItem item = new CommandContributionItem(
				                        new CommandContributionItemParameter(
				                                PlatformUI.getWorkbench(), null,
				                                MapScaleHandler.SET_SCALE_COMMAND_ID, parms,
				                                null, null, null, scale.getDisplayName(), null,
				                                null, CommandContributionItem.STYLE_PUSH, null,
				                                true));
				                menuMgr.add(item);
							}
						}
		            }
					
				}
			}
        	/*
        	 * if rList.size() = 0 (no SatResource loaded to display), clear custom scales.
        	 */
        	if (rList.size() == 0) {
        		MapScalesManager.getInstance().clearCustomScales();
        	}
	    }
        
        return menuMgr.getItems();
    }
    

}
