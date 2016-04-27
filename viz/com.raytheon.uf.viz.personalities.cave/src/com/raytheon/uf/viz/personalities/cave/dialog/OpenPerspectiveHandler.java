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
package com.raytheon.uf.viz.personalities.cave.dialog;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.handlers.ShowPerspectiveHandler;
import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * A handler intended to override the command
 * org.eclipse.ui.perspectives.showPerspective to use the
 * PerspectiveChooserDialog instead of the SelectPerspectiveDialog. After
 * getting the dialog results, this handler then defers to the original Eclipse
 * handler for actual processing of the event.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 06, 2016  5192      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 * @version 1.0
 */

public class OpenPerspectiveHandler extends AbstractHandler {

    protected AbstractHandler original = new ShowPerspectiveHandler();

    @SuppressWarnings({ "unchecked", "restriction" })
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IWorkbenchWindow window = HandlerUtil
                .getActiveWorkbenchWindowChecked(event);

        Map<String, String> parameters = event.getParameters();
        Object value = parameters.get(
                IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE_PARM_ID);
        String newWindow = parameters.get(
                IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE_PARM_NEWWINDOW);

        if (value == null) {
            // no perspective provided, show a dialog to choose
            PerspectiveChooserDialog dialog = new PerspectiveChooserDialog(window.getShell(),
                    WorkbenchPlugin.getDefault().getPerspectiveRegistry());
            dialog.open();
            if (dialog.getReturnCode() == Window.CANCEL) {
                return null;
            }
            IPerspectiveDescriptor descriptor = dialog.getSelection();
            String perspectiveId = descriptor.getId();
            parameters = new HashMap<>();
            parameters.put(
                    IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE_PARM_ID,
                    perspectiveId);
            if (newWindow != null) {
                parameters.put(
                        IWorkbenchCommandConstants.PERSPECTIVES_SHOW_PERSPECTIVE_PARM_NEWWINDOW,
                        newWindow);
            }
            event = new ExecutionEvent(event.getCommand(), parameters,
                    event.getTrigger(), event.getApplicationContext());
        }

        return original.execute(event);
    }

}
