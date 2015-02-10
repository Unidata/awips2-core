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
package com.raytheon.viz.ui.input;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.capabilities.EditableCapability;

/**
 * Class for managing editableness of resources on a display container. This
 * class ensures that only one resource is editable at a time.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??/??/????              mschenke    Initial creation
 * Feb 02, 2015  3974      njensen     Improved javadoc
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class EditableManager {

    private static Map<IDisplayPaneContainer, EditableManager> managerMap = new HashMap<IDisplayPaneContainer, EditableManager>();

    private final IDisplayPaneContainer container;

    private EditableManager(IDisplayPaneContainer container) {
        this.container = container;
    }

    public static void makeEditable(AbstractVizResource<?, ?> rsc,
            boolean editable) {
        IDisplayPaneContainer container = rsc.getResourceContainer();
        if (container != null) {
            EditableManager mgr = managerMap.get(rsc.getResourceContainer());
            if (mgr == null) {
                mgr = new EditableManager(container);
                managerMap.put(container, mgr);
            }
            mgr.makeEditableInternal(rsc, editable);
        }
    }

    private void makeEditableInternal(AbstractVizResource<?, ?> rsc,
            boolean editable) {
        if (rsc.getCapability(EditableCapability.class).isEditable() == editable
                && editable == false) {
            return;
        }

        for (IDisplayPane pane : container.getDisplayPanes()) {
            for (ResourcePair pair : pane.getDescriptor().getResourceList()) {
                if (pair.getResource() != null
                        && pair.getResource().hasCapability(
                                EditableCapability.class)) {
                    if (pair.getResource().getResourceData()
                            .equals(rsc.getResourceData()) == false) {
                        pair.getResource()
                                .getCapability(EditableCapability.class)
                                .setEditable(false);
                    } else {
                        pair.getResource()
                                .getCapability(EditableCapability.class)
                                .setEditable(editable);
                    }
                }
            }
        }
    }

}
