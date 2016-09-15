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

import java.io.BufferedWriter;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.procedures.Bundle;
import com.raytheon.uf.viz.core.procedures.Procedure;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

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
 * Sep 15, 2016           mjames@ucar Save as perspective / multi-display rather
 *                                    single map editor.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class SaveBundle extends AbstractHandler {

	private final String EXT = ".xml";
	
    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands
     * .ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        String fileName = null;
        FileDialog fd = new FileDialog(shell, SWT.SAVE);
        fd.setFileName(fileName);
        fd.setFilterExtensions(new String[] { "*" + EXT });
        fd.setFilterPath(System.getProperty("user.home"));
        String retVal = fd.open();
        if (retVal == null) {
            return null;
        }

        String name = fd.getFileName();
        if (name != null && name.endsWith(EXT) == false) {
            name += EXT;
            fd.setFileName(name);
        }
        fileName = fd.getFilterPath() + File.separator + name;
        if (new File(fileName).exists()) {
            boolean result = MessageDialog.openQuestion(shell,
                    "Confirm Overwrite", "A file named \"" + name
                    + "\" already exists.  Do you want to replace it?");
            if (result == false) {
                fileName = null;
            }
        }
        
        final Path procedurePath = Paths.get(fileName);
        try (BufferedWriter br = Files.newBufferedWriter(procedurePath,
                Charset.defaultCharset())) {
            Procedure procedure = SavePerspectiveHandler.getCurrentProcedure();
            String xml = procedure.toXML();
            br.write(xml);
        } catch (Exception e) {
        	Status status = new Status(Status.ERROR, UiPlugin.PLUGIN_ID, 0,
        			"Failed to write perspective file: " + fileName + ".", e);
        	ErrorDialog.openError(Display.getCurrent().getActiveShell(),
                    "ERROR", "Error occurred during bundle save.", status);
        }
        return null;
    }

    public static Bundle extractCurrentBundle() {
        IEditorPart part = EditorUtil.getActiveEditor();
        if (part instanceof AbstractEditor) {
            AbstractEditor editor = (AbstractEditor) part;
            IRenderableDisplay[] displays = UiUtil
                    .getDisplaysFromContainer(editor);
            List<AbstractRenderableDisplay> absdisplays = new ArrayList<AbstractRenderableDisplay>();
            for (IRenderableDisplay display : displays) {
                if ((display instanceof AbstractRenderableDisplay)) {
                    absdisplays.add((AbstractRenderableDisplay) display);
                }
            }

            Bundle bundle = new Bundle();
            bundle.setName(editor.getPartName());
            bundle.setDisplays(absdisplays
                    .toArray(new AbstractRenderableDisplay[absdisplays.size()]));
            bundle.setLoopProperties(EditorUtil.getActiveVizContainer()
                    .getLoopProperties());
            return bundle;
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

}
