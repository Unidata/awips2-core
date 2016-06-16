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
package com.raytheon.uf.viz.localization.perspective.view.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.localization.perspective.Activator;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationMergeEditorInput;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;

/**
 * Action for resolving a file version conflict when a user tries to save a
 * localization file that has been modified on the server. It prompts the user
 * regarding the conflict and allows them to open a merge editor to resolve it.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 23, 2016  4907     mapeters  Initial creation.
 * Jun 13, 2016  4907     mapeters  Copy file from server without overwriting
 *                                  local file.
 * 
 * </pre>
 * 
 * @author mapeters
 */

public class ResolveFileVersionConflictAction extends Action {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ResolveFileVersionConflictAction.class);

    private final LocalizationEditorInput input;

    private final String serverFileChecksum;

    private final IDocument doc;

    /**
     * Constructor for an action to resolve a file version conflict.
     * 
     * @param input
     *            the editor input whose save operation caused the conflict
     * @param doc
     *            the document containing the user's local changes
     * @param serverFileChecksum
     *            the checksum of the file on the localization server (if null,
     *            it will be retrieved here)
     */
    public ResolveFileVersionConflictAction(LocalizationEditorInput input,
            IDocument doc, String serverFileChecksum) {
        this.input = input;
        this.doc = doc;
        if (serverFileChecksum == null) {
            ILocalizationFile file = input.getLocalizationFile();
            IPathManager pm = PathManagerFactory.getPathManager();
            serverFileChecksum = pm.getLocalizationFile(file.getContext(),
                    file.getPath()).getCheckSum();
        }
        this.serverFileChecksum = serverFileChecksum;
    }

    @Override
    public void run() {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();
        String msg = "The file '" + input.getName()
                + "' has been modified by another user. In order "
                + "to save your changes, you must merge them with the latest "
                + "version of the file.\n\nSelect OK to merge.";
        SWTMessageBox messageDialog = new SWTMessageBox(shell,
                "File Version Conflict", msg, SWT.OK | SWT.CANCEL
                        | SWT.ICON_WARNING);

        messageDialog.setCloseCallback(new ICloseCallback() {

            @Override
            public void dialogClosed(Object returnValue) {
                if (returnValue instanceof Integer
                        && ((int) returnValue == SWT.OK)) {
                    resolveFileVersionConflict();
                }
            }
        });

        messageDialog.open();
    }

    private void resolveFileVersionConflict() {
        try {
            LocalizationMergeEditorInput mergeInput = LocalizationEditorUtils
                    .getOpenMergeInputForLocalizationInput(input);
            if (mergeInput == null) {
                // Copy user's changes to tmp file
                IFile localTmpFile = copyDocToTmpFile();
                FileEditorInput local = new FileEditorInput(localTmpFile);

                // Copy latest version to tmp file
                IFile remoteTmpFile = copyLatestVersionToTmpFile();
                FileEditorInput remote = new FileEditorInput(remoteTmpFile);

                mergeInput = new LocalizationMergeEditorInput(input, local,
                        remote, serverFileChecksum);
                CompareUI.openCompareEditor(mergeInput);
            } else {
                // Refresh the merge editor with the user's changes
                mergeInput.refreshForNewMergeConflict(doc, null);

                IWorkbenchPage page = PlatformUI.getWorkbench()
                        .getActiveWorkbenchWindow().getActivePage();
                page.activate(page.findEditor(mergeInput));
            }
        } catch (IOException | CoreException | CommunicationException e) {
            String msg = "An error occurred while setting up the merge editor. "
                    + "Please consider saving your changes to a local file, updating "
                    + "to the latest version of the file, and manually merging the changes.\n";
            statusHandler.handle(Priority.PROBLEM,
                    msg + e.getLocalizedMessage(), e);
        }
    }

    private IFile copyDocToTmpFile() throws IOException, CoreException {
        java.nio.file.Path tmpPath = createTmpFilePath();
        Files.write(tmpPath, doc.get().getBytes());

        return getTmpFile(tmpPath);
    }

    private IFile copyLatestVersionToTmpFile() throws IOException,
            CoreException, CommunicationException {
        java.nio.file.Path tmpPath = createTmpFilePath();

        LocalizationManager.getInstance().retrieveToFile(
                input.getLocalizationFile(), tmpPath.toFile());

        return getTmpFile(tmpPath);
    }

    private Path createTmpFilePath() throws IOException {
        IProject localizationProject = Activator.getDefault()
                .getLocalizationProject();
        IPath tmpDir = localizationProject.getLocation();
        java.nio.file.Path tmpDirPath = Paths.get(tmpDir.toOSString());

        return Files.createTempFile(tmpDirPath, null, null);
    }

    private IFile getTmpFile(Path tmp) throws CoreException {
        IProject localizationProject = Activator.getDefault()
                .getLocalizationProject();
        String tmpFileName = tmp.getFileName().toString();
        IFile tmpFile = localizationProject.getFile(tmpFileName);
        tmpFile.createLink(tmp.toUri(), IResource.NONE, null);

        return tmpFile;
    }
}
