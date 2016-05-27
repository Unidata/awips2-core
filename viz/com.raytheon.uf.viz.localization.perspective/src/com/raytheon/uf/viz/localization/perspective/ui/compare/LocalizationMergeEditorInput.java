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
package com.raytheon.uf.viz.localization.perspective.ui.compare;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.part.FileEditorInput;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;

/**
 * Editor input for merging the remote version of a localization file into a
 * user's local version when a file version conflict occurs.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------
 * May 23, 2016  4907     mapeters  Initial creation.
 * 
 * </pre>
 * 
 * @author mapeters
 */

public class LocalizationMergeEditorInput extends CompareEditorInput implements
        ISaveablesSource {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationMergeEditorInput.class);

    private LocalizationEditorInput input;

    private ResourceNode leftLocalNode;

    private ResourceNode rightRemoteNode;

    private Saveable[] saveables;

    private String serverFileChecksum;

    private boolean isRefreshing = false;

    public LocalizationMergeEditorInput(LocalizationEditorInput input,
            FileEditorInput local, FileEditorInput remote,
            String serverFileChecksum) {
        super(new CompareConfiguration());
        this.setTitle("Merge (" + input.getName() + ")");

        CompareConfiguration config = getCompareConfiguration();
        config.setLeftEditable(true);
        config.setRightEditable(false);
        config.setLeftLabel("Local: " + input.getName());
        config.setRightLabel("Remote: " + input.getName());

        this.leftLocalNode = new ResourceNode(local.getFile());
        this.rightRemoteNode = new ResourceNode(remote.getFile());

        this.serverFileChecksum = serverFileChecksum;

        this.input = input;

        this.saveables = new Saveable[] { new LocalizationMergeSaveable(this) };
    }

    @Override
    protected Object prepareInput(IProgressMonitor pm)
            throws InvocationTargetException, InterruptedException {
        return new Differencer().findDifferences(false, pm, null, null,
                leftLocalNode, rightRemoteNode);
    }

    /**
     * Refresh the Local editor's contents with the given doc's contents, and
     * refresh the Remote editor's contents with the latest file from the
     * localization server.
     * 
     * @param doc
     *            the document containing the contents to put in the Local
     *            editor
     * @param monitor
     */
    public void refresh(IDocument doc, IProgressMonitor monitor) {
        // Update the Local tmp file
        byte[] bytes = doc.get().getBytes();
        IFile file = (IFile) leftLocalNode.getResource();
        try {
            file.setContents(new ByteArrayInputStream(bytes), IResource.FORCE,
                    null);
        } catch (CoreException e) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Failed to set contents of local file "
                            + file.getFullPath().toString() + ": "
                            + e.getLocalizedMessage(), e);
        }

        // Update the Remote tmp file
        File tmpRemoteFile = rightRemoteNode.getResource().getLocation()
                .toFile();
        IPathManager pm = PathManagerFactory.getPathManager();
        ILocalizationFile inputLocFile = input.getLocalizationFile();
        ILocalizationFile latestLocFile = pm.getLocalizationFile(
                inputLocFile.getContext(), inputLocFile.getPath());
        try (InputStream is = latestLocFile.openInputStream()) {
            Files.copy(is, tmpRemoteFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            serverFileChecksum = Checksum.getMD5Checksum(tmpRemoteFile);
        } catch (IOException | LocalizationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Failed to update the remote file to the latest version: "
                            + e.getLocalizedMessage(), e);
        }

        // Refresh the editor with the files' new contents
        performRefresh();
    }

    /**
     * Refresh the editor using this input with the updated contents of the tmp
     * files for the Local and Remote editors.
     */
    private void performRefresh() {
        isRefreshing = true;

        leftLocalNode.discardBuffer();
        rightRemoteNode.discardBuffer();
        IEditorPart editor = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage()
                .findEditor(LocalizationMergeEditorInput.this);
        CompareUI.reuseCompareEditor(LocalizationMergeEditorInput.this,
                (IReusableEditor) editor);

        isRefreshing = false;
    }

    @Override
    protected void contentsCreated() {
        super.contentsCreated();
        if (isRefreshing) {
            // When refreshing, we need to re-register the saveable manually
            SaveablesLifecycleEvent event = new SaveablesLifecycleEvent(this,
                    SaveablesLifecycleEvent.POST_OPEN, getSaveables(), false);
            IWorkbenchPage page = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage();
            IEditorPart editor = page.findEditor(this);
            ((CompareEditor) editor).handleLifecycleEvent(event);
        }
        this.setLeftDirty(true);
    }

    public boolean isConflictResolved() {
        return ((LocalizationMergeSaveable) saveables[0]).mergeResolved;
    }

    @Override
    public Saveable[] getSaveables() {
        return this.saveables;
    }

    @Override
    public Saveable[] getActiveSaveables() {
        return getSaveables();
    }

    public LocalizationEditorInput getLocalizationEditorInput() {
        return input;
    }

    @Override
    protected void handleDispose() {
        super.handleDispose();

        if (!isRefreshing) {
            // If we aren't just refreshing then we are done with the tmp files
            try {
                Files.delete(leftLocalNode.getResource().getLocation().toFile()
                        .toPath());
                Files.delete(rightRemoteNode.getResource().getLocation()
                        .toFile().toPath());
            } catch (IOException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
    }

    private static class LocalizationMergeSaveable extends
            AbstractLocalizationSaveable {

        private LocalizationMergeEditorInput parent;

        private boolean mergeResolved = false;

        private String changesChecksum = null;

        public LocalizationMergeSaveable(LocalizationMergeEditorInput parent) {
            super(parent.input, parent.leftLocalNode);
            this.parent = parent;
        }

        @Override
        public void doSave(IProgressMonitor monitor) throws CoreException {
            // Clear value so it's recalculated
            changesChecksum = null;

            // Flush changes from the viewer into the node
            parent.flushLeftViewers(monitor);

            try {
                if (validateAndPerformSave(monitor)) {
                    // Successfully saved to server, update serverFileChecksum
                    parent.serverFileChecksum = getChangesChecksum();
                    mergeResolved = true;
                    /*
                     * This is normally automatically handled, but if save is
                     * selected before changing anything when the merge editor
                     * is opened, it will incorrectly stay dirty
                     */
                    parent.setLeftDirty(false);
                }
            } catch (LocalizationFileVersionConflictException e) {
                handleAnotherMergeConflict(monitor);
            }
        }

        @Override
        protected boolean validateSave(IProgressMonitor monitor) {
            IPathManager pm = PathManagerFactory.getPathManager();
            LocalizationFile inputLocFile = input.getLocalizationFile();
            String serverChecksum = pm.getLocalizationFile(
                    inputLocFile.getContext(), inputLocFile.getPath())
                    .getCheckSum();
            if (!serverChecksum.equals(parent.serverFileChecksum)) {
                // New merge conflict
                mergeResolved = false;
                if (!serverChecksum.equals(getChangesChecksum())) {
                    handleAnotherMergeConflict(monitor);
                    return false;
                }
            }

            if (!mergeResolved) {
                // Refresh so we are able to save without another conflict
                input.refreshLocalizationFile();
            }

            return true;
        }

        private void handleAnotherMergeConflict(final IProgressMonitor monitor) {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            String msg = "The file has been modified by another user again. "
                    + "The remote file will be updated to the latest version from the server.";
            SWTMessageBox messageDialog = new SWTMessageBox(shell,
                    "File Version Conflict", msg, SWT.OK | SWT.ICON_WARNING);

            messageDialog.setCloseCallback(new ICloseCallback() {

                @Override
                public void dialogClosed(Object returnValue) {
                    parent.refresh(CompareUI.getDocument(node), monitor);
                }
            });

            messageDialog.open();
        }

        /**
         * Get the checksum value of the changes made by the user (the current
         * contents of the Local editor).
         * 
         * @return the checksum
         */
        private String getChangesChecksum() {
            if (changesChecksum == null) {
                try {
                    byte[] docBytes = CompareUI.getDocument(node).get()
                            .getBytes();
                    changesChecksum = Checksum
                            .getMD5Checksum(new ByteArrayInputStream(docBytes));
                } catch (IOException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }
            }
            return changesChecksum;
        }

        @Override
        public boolean isDirty() {
            return parent.isLeftSaveNeeded();
        }
    }
}
