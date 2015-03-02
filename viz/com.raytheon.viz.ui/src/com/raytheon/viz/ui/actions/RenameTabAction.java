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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import com.raytheon.viz.ui.IRenameablePart;

/**
 * Action for renaming an IRenameablePart, typically tabs.
 * 
 * Note that this action will not be shown unless the perspectiveId is specified
 * in the plugin.xml of the plugin contributing an instance of this action.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 25, 2015  4204      njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class RenameTabAction extends ContributedEditorMenuAction {

    public RenameTabAction() {
        super("Rename Tab", IAction.AS_PUSH_BUTTON);
    }

    @Override
    public boolean shouldBeVisible() {
        boolean visible = false;
        IWorkbenchPart wbPart = getWorkbenchPart();
        if (wbPart instanceof IRenameablePart) {
            String currentPersp = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .getPerspective().getId();
            visible = currentPersp.equals(getPerspectiveId());
        }
        return visible;
    }

    @Override
    public void run() {
        IWorkbenchPart wbPart = getWorkbenchPart();
        if (wbPart instanceof IRenameablePart) {
            IRenameablePart partToRename = (IRenameablePart) wbPart;
            String currentName = partToRename.getPartName();
            // TODO add validation of user input
            InputDialog userInput = new InputDialog(PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getShell(), "Rename Tab", null,
                    currentName, null);
            if (userInput.open() == Window.OK) {
                String newName = userInput.getValue();
                partToRename.setPartName(newName);
            }
        }
    }

}
