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
package com.raytheon.viz.ui.actions;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.internal.WorkbenchWindow;
import org.eclipse.ui.services.IEvaluationService;

import com.raytheon.viz.ui.VizWorkbenchManager;

/**
 * Menu item to toggle the global toolbar (the one at the top of the screen on
 * all perspectives)
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 23, 2018 7298       tgurney     Rename menu item from "Toolbar" to
 *                                     "Show Toolbar"
 * Dec 09, 2019 7991       randerso    Fix NPE in isChecked()
 * </pre>
 *
 * @author unknown
 */

public class ToggleToolbarContributionItem extends CompoundContributionItem {

    @Override
    protected IContributionItem[] getContributionItems() {
        ActionContributionItem toggleContributionItem = new ActionContributionItem(
                new Action("Show Toolbar", IAction.AS_CHECK_BOX) {
                    @Override
                    public boolean isChecked() {
                        IWorkbenchWindow activeWorkbenchWindow = VizWorkbenchManager
                                .getInstance().getCurrentWindow();
                        if (activeWorkbenchWindow != null) {
                            IEvaluationService service = activeWorkbenchWindow
                                    .getService(IEvaluationService.class);
                            IEvaluationContext appState = service
                                    .getCurrentState();
                            Boolean visible = (Boolean) appState.getVariable(
                                    ISources.ACTIVE_WORKBENCH_WINDOW_IS_COOLBAR_VISIBLE_NAME);
                            return (visible != null ? visible : false);
                        }

                        return false;
                    }

                    @Override
                    public void run() {
                        IWorkbenchWindow activeWorkbenchWindow = VizWorkbenchManager
                                .getInstance().getCurrentWindow();

                        if (activeWorkbenchWindow instanceof WorkbenchWindow) {
                            WorkbenchWindow window = (WorkbenchWindow) activeWorkbenchWindow;
                            window.toggleToolbarVisibility();
                        }
                    }

                });
        return new IContributionItem[] { toggleContributionItem };
    }

}
