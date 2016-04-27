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
package com.raytheon.uf.viz.personalities.cave.workbench;

import java.util.Collection;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.ISaveHandler;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;

import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * When a "dirty" {@link AbstractEditor} is closed it pops up a confirmation
 * dialog. When multiple AbstractEditors are closed at once then the default
 * behavior is for each one to pop up its own dialog which is really annoying.
 * This class intercepts the save call which is responsible for opening the
 * dialog and it opens up one confirmation dialog for all the editors.
 * 
 * From an RCP perspective the only opportunity to "confirm" close like this is
 * to override the "save" handling. Most editors implement only
 * {@link ISaveablePart} which causes the default {@link ISaveHandler} to
 * consolidate multiple save dialogs into a single group dialog. AbstractEditors
 * must implement {@link ISaveablePart2} so they can use a custom save dialog
 * that is really just a confirmation dialog. The default ISaveHandler does not
 * include ISaveablePart2 in the consolidated save dialog. This class fixes the
 * problem by applying the same type of consolidation to AbstractEditors.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Dec 23, 2015  5189     bsteffen    Initial Creation.
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public final class VizSaveHandler implements ISaveHandler {

    private final ISaveHandler defaultSaveHandler;

    public VizSaveHandler(ISaveHandler defaultSaveHandler) {
        this.defaultSaveHandler = defaultSaveHandler;
    }

    @Override
    public boolean save(MPart dirtyPart, boolean confirm) {
        return defaultSaveHandler.save(dirtyPart, confirm);
    }

    @Override
    @SuppressWarnings("restriction")
    public boolean saveParts(Collection<MPart> dirtyParts, boolean confirm) {
        if (confirm) {
            boolean allAbstract = true;
            for (MPart part : dirtyParts) {
                if (part.getObject() instanceof CompatibilityEditor) {
                    CompatibilityEditor ed = (CompatibilityEditor) part
                            .getObject();
                    if (ed.getEditor() instanceof AbstractEditor) {
                        continue;
                    }
                }
                allAbstract = false;
                break;
            }
            if (allAbstract) {
                Shell shell = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getShell();
                return MessageDialog.openQuestion(shell, "Close Editors?",
                        "Are you sure you want to close multiple editors?");
            }
        }
        return defaultSaveHandler.saveParts(dirtyParts, confirm);

    }

    @Override
    public Save promptToSave(MPart dirtyPart) {
        return defaultSaveHandler.promptToSave(dirtyPart);
    }

    @Override
    public Save[] promptToSave(Collection<MPart> dirtyParts) {
        return defaultSaveHandler.promptToSave(dirtyParts);
    }
}