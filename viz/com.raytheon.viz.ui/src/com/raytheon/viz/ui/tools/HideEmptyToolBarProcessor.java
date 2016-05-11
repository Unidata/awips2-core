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
package com.raytheon.viz.ui.tools;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MAddon;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.workbench.modeling.EModelService;

import com.raytheon.viz.ui.UiPlugin;

/**
 * A Processor for use by the org.eclipse.e4.workbench.model extension point
 * that ensures the {@link HideEmptyToolBarAddon} is present in the application
 * model.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 11, 2016  5644     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class HideEmptyToolBarProcessor {

    @Execute
    void addFloatingWindowAddon(MApplication app, EModelService modelService) {
        List<MAddon> addons = app.getAddons();

        // prevent multiple copies
        for (MAddon addon : addons) {
            /*
             * Following the example of the eclipse swt addons we want it to be
             * possible to override this addon if another addon is provided that
             * has the same class name for the contribution.
             */
            if (addon.getContributionURI().contains("HideEmptyToolBarAddon")) {
                return;
            }
        }

        // adds the add-on to the application model
        MAddon addon = modelService.createModelElement(MAddon.class);
        addon.setElementId("HideEmptyToolBarAddon");
        addon.setContributionURI("bundleclass://" + UiPlugin.PLUGIN_ID + "/"
                + HideEmptyToolBarAddon.class.getName());
        addons.add(addon);
    }
}
