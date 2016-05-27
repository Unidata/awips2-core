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
package com.raytheon.uf.viz.localization.perspective;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.team.FileModificationValidationContext;
import org.eclipse.core.resources.team.FileModificationValidator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationMergeEditorInput;
import com.raytheon.uf.viz.localization.perspective.view.actions.ResolveFileVersionConflictAction;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;

/**
 * Class used to verify we can modify localization files. Used so user is not
 * prompted to make file writable when set to read only, and to handle merge
 * conflicts when users try to save a file that has been changed on the server.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 03, 2011           mschenke  Initial creation
 * May 23, 2016  4907     mapeters  Added save validation
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class LocalizationFileModificationValidator extends
        FileModificationValidator {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationFileModificationValidator.class);

    @Override
    public IStatus validateEdit(IFile[] files,
            FileModificationValidationContext context) {
        boolean allOk = true;
        for (IFile file : files) {
            if (file.isReadOnly()) {
                allOk = false;
                break;
            }
        }
        return allOk ? Status.OK_STATUS : Status.CANCEL_STATUS;
    }

    @Override
    public IStatus validateSave(IFile file) {
        /*
         * This method is automatically hooked into by save operations from text
         * editors (not compare editors), so find the text editor for the file
         */
        IEditorReference[] refs = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage()
                .getEditorReferences();
        for (IEditorReference ref : refs) {
            IEditorPart part = ref.getEditor(false);
            IEditorInput editorInput = part.getEditorInput();
            if (editorInput instanceof LocalizationEditorInput) {
                LocalizationEditorInput input = (LocalizationEditorInput) editorInput;
                if (input.getFile().equals(file)) {
                    ITextEditor editor = (ITextEditor) part;
                    IDocument doc = editor.getDocumentProvider().getDocument(
                            editorInput);
                    return validateSave(input, doc);
                }
            }
        }

        /*
         * Saving a file that isn't in an editor (e.g. a tmp file), no
         * validation needed
         */
        return Status.OK_STATUS;
    }

    /**
     * Performs the actual validation of the save, allowing for compare editors
     * to manually hook into this. This validates that the file is being saved
     * on top of the latest version of the file from the localization server, or
     * that the local changes match the latest version of the file. If not, an
     * {@link ResolveFileVersionConflictAction} is started and the save
     * operation is canceled.
     * 
     * @param input
     *            the editor input
     * @param doc
     *            the editor document containing the current unsaved changes
     * @return {@link Status#OK_STATUS } or {@link Status#CANCEL_STATUS }
     *         depending on validation result
     */
    public static IStatus validateSave(LocalizationEditorInput input,
            IDocument doc) {
        // Handle an unresolved merge conflict for this file
        LocalizationMergeEditorInput mergeInput = LocalizationEditorUtils
                .getOpenMergeInputForLocalizationInput(input);
        if (mergeInput != null && !mergeInput.isConflictResolved()) {
            handleUnresolvedMergeConflict(mergeInput);
            return Status.CANCEL_STATUS;
        }

        // OK if saving on top of the latest version of the file
        LocalizationFile inputLocFile = input.getLocalizationFile();
        IPathManager pm = PathManagerFactory.getPathManager();
        String serverChecksum = pm.getLocalizationFile(
                inputLocFile.getContext(), inputLocFile.getPath())
                .getCheckSum();
        String localChecksum = inputLocFile.getCheckSum();

        if (localChecksum.equals(serverChecksum)) {
            return Status.OK_STATUS;
        }

        // OK if file version conflict but local changes match server version
        String text = doc.get();
        InputStream is = new ByteArrayInputStream(text.getBytes());
        String changesChecksum = null;
        try {
            changesChecksum = Checksum.getMD5Checksum(is);
            if (changesChecksum.equals(serverChecksum)) {
                // Refresh so we are able to save without a conflict
                input.refreshLocalizationFile();
                return Status.OK_STATUS;
            }
        } catch (IOException e) {
            statusHandler
                    .handle(Priority.PROBLEM, "Error calculating checksum: "
                            + e.getLocalizedMessage(), e);
        }

        // Handle file version conflict
        new ResolveFileVersionConflictAction(input, doc, serverChecksum).run();
        return Status.CANCEL_STATUS;
    }

    private static void handleUnresolvedMergeConflict(
            final LocalizationMergeEditorInput mergeInput) {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        String msg = "A file version conflict occurred when previously attempting to save this file. The conflict must be resolved before it can be saved.\n\nSelect OK to go to the open merge editor.";
        SWTMessageBox messageDialog = new SWTMessageBox(shell,
                "Unresolved File Version Conflict", msg, SWT.OK | SWT.CANCEL
                        | SWT.ICON_WARNING);

        messageDialog.setCloseCallback(new ICloseCallback() {

            @Override
            public void dialogClosed(Object returnValue) {
                if (returnValue instanceof Integer
                        && ((int) returnValue == SWT.OK)) {
                    IWorkbenchPage page = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage();
                    page.activate(page.findEditor(mergeInput));
                }
            }
        });

        messageDialog.open();
    }
}
