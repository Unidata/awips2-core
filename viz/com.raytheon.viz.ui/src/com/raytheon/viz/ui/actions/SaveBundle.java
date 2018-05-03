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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.IRenameablePart;
import com.raytheon.viz.ui.UiPlugin;

/**
 * Save a bundle to disk
 * 
 * <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jan 29, 2007           chammack    Initial Creation.
 * Oct 22, 2013  2491     bsteffen    Switch serialization to
 *                                     ProcedureXmlManager
 * Mar 02, 2015  4204     njensen     Extract part name as bundle name
 * May 03, 2018  6622     bsteffen    Support hidden panes.
 * 
 * </pre>
 * 
 * @author chammack
 */
public class SaveBundle extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        String fileName = null;
        FileDialog fd = new FileDialog(shell, SWT.SAVE);
        fd.setOverwrite(true);
        fd.setFileName(fileName);
        fd.setFilterExtensions(new String[] { "*.xml" });
        fd.setFilterPath(System.getProperty("user.home"));
        while (fileName == null) {
            String retVal = fd.open();
            if (retVal == null) {
                return null;
            }

            String name = fd.getFileName();
            fileName = fd.getFilterPath() + File.separator + name;
            if (!name.endsWith(".xml")) {
                name += ".xml";
                fd.setFileName(name);
                fileName = fd.getFilterPath() + File.separator + name;
                if (new File(fileName).exists()) {
                    boolean result = MessageDialog.openQuestion(shell,
                            "Confirm Overwrite", "A file named \"" + name
                                    + "\" already exists.  Do you want to replace it?");
                    if (!result) {
                        fileName = null;
                    }
                }
            }
        }

        try {
            Bundle bundle = extractCurrentBundle();
            ProcedureXmlManager.getInstance().marshalToFile(bundle, fileName);
        } catch (Exception e) {
            Status status = new Status(Status.ERROR, UiPlugin.PLUGIN_ID, 0,
                    "Error occurred during bundle save.", e);
            ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                    "ERROR", "Error occurred during bundle save.", status);
            throw new ExecutionException("Error occurred during bundle save",
                    e);
        }
        return null;
    }

    public static Bundle extractCurrentBundle() {
        IEditorPart part = EditorUtil.getActiveEditor();
        if (part instanceof IDisplayPaneContainer) {
            return extractBundle((IDisplayPaneContainer) part);
        } else {
            String msg = null;
            if (part == null) {
                msg = "No editor selected";
            } else {
                msg = "Cannot save an editor of type: "
                        + part.getClass().getName();
            }
            throw new IllegalStateException(msg);
        }

    }

    public static Bundle extractBundle(IDisplayPaneContainer container) {
        List<AbstractRenderableDisplay> displays = new ArrayList<>();
        List<Integer> hidden = new ArrayList<>();
        int index = 0;
        for (IDisplayPane pane : container.getDisplayPanes()) {
            IRenderableDisplay idisp = pane.getRenderableDisplay();
            if (idisp instanceof AbstractRenderableDisplay) {
                displays.add((AbstractRenderableDisplay) idisp);
                if (!pane.isVisible()) {
                    hidden.add(index);
                }
                index += 1;
            }
        }
        if (displays.isEmpty()) {
            return null;
        }
        Bundle bundle = new Bundle();
        if (container instanceof IRenameablePart) {
            bundle.setName(((IRenameablePart) container).getPartName());
        }
        bundle.setDisplays(displays.toArray(new AbstractRenderableDisplay[0]));
        if (!hidden.isEmpty()) {
            bundle.setHidden(
                    hidden.stream().mapToInt(Integer::intValue).toArray());
        }
        bundle.setLoopProperties(
                EditorUtil.getActiveVizContainer().getLoopProperties());
        return bundle;
    }

}
