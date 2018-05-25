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
package com.raytheon.uf.viz.core.maps.actions;

import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.MapManager;
import com.raytheon.uf.viz.core.maps.rsc.AbstractMapResource;
import com.raytheon.uf.viz.core.maps.rsc.AbstractMapResourceData;
import com.raytheon.uf.viz.core.rsc.capabilities.Capabilities;
import com.raytheon.viz.ui.cmenu.AbstractRightClickAction;

/**
 * Action to save map style preferences
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 10, 2010            randerso     Initial creation
 * May 31, 2018 6562       tgurney      AbstractMapResource type signature change
 *
 * </pre>
 *
 * @author randerso
 */

public class SaveMapStyleAction extends AbstractRightClickAction {

    public SaveMapStyleAction() {
        super("Save Map Style");
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        AbstractMapResource<AbstractMapResourceData, IMapDescriptor> rsc = (AbstractMapResource<AbstractMapResourceData, IMapDescriptor>) getSelectedRsc();

        // save map styles
        Capabilities capabilities = rsc.getCapabilities();

        MapManager.getInstance(rsc.getDescriptor())
                .saveStylePreferences(rsc.getName(), capabilities);
    }
}
