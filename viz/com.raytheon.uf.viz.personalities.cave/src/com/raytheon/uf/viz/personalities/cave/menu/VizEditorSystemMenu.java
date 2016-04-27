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

package com.raytheon.uf.viz.personalities.cave.menu;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.icon.IconUtil;
import com.raytheon.viz.ui.actions.ContributedEditorMenuAction;

/**
 * Enables viz to have custom right click actions on editor tabs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??? ??, ????            ?           Initial creation
 * Mar 02, 2015  4204      njensen     Set the part on the action regardless of visibility
 * Dec 23, 2015  5189      bsteffen    Rewrite for eclipse 4.
 * 
 * </pre>
 * 
 */
public class VizEditorSystemMenu {

    private static final String EDITOR_MENU_EXTENSION_POINT = "com.raytheon.viz.ui.editorMenuAddition";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(VizEditorSystemMenu.class);

    private List<ContributedEditorMenuAction> userContributionActions;

    /**
     * Create the standard view menu
     * 
     * @param site
     *            the site to associate the view with
     */
    public VizEditorSystemMenu() {
        // grab any user contributed items using an extension point
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry
                .getExtensionPoint(EDITOR_MENU_EXTENSION_POINT);
        if (point != null) {
            userContributionActions = new ArrayList<>();
            for (IExtension ext : point.getExtensions()) {
                IConfigurationElement[] element = ext
                        .getConfigurationElements();
                for (final IConfigurationElement el : element) {
                    Object ob;
                    try {
                        ob = el.createExecutableExtension("class");
                        ContributedEditorMenuAction action = (ContributedEditorMenuAction) ob;
                        String icon = el.getAttribute("icon");
                        if (icon != null) {
                            action.setImageDescriptor(
                                    IconUtil.getImageDescriptor(
                                            el.getContributor().getName(),
                                            icon));
                        }
                        action.setId(el.getAttribute("name"));
                        action.setText(el.getAttribute("name"));
                        String perspectiveId = el.getAttribute("perspectiveId");
                        if (perspectiveId != null) {
                            action.setPerspectiveId(perspectiveId);
                        }
                        userContributionActions.add(action);
                    } catch (CoreException e) {
                        statusHandler
                                .error("Error creating custom editor menu action for "
                                        + el.getName(), e);
                    }
                }
            }
        }
    }

    public void show(Menu menu, IWorkbenchPart part) {
        new MenuItem(menu, SWT.SEPARATOR);

        IWorkbenchAction newEditorAction = ActionFactory.NEW_EDITOR
                .create(part.getSite().getWorkbenchWindow());
        new ActionContributionItem(newEditorAction).fill(menu, -1);

        new MenuItem(menu, SWT.SEPARATOR);

        for (ContributedEditorMenuAction action : userContributionActions) {
            action.setPart(part);
            if (!action.shouldBeVisible()) {
                continue;
            }
            new ActionContributionItem(action).fill(menu, -1);
        }
    }

}
