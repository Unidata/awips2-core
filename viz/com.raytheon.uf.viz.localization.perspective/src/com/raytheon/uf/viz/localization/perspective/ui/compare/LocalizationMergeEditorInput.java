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

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.structuremergeviewer.DiffNode;
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

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
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
 * Jun 13, 2016  4907     mapeters  Update remote tmp file from server without
 *                                  overwriting local file.
 * Jun 22, 2017  4818     mapeters  Changed setCloseCallback to addCloseCallback
 * Jun 12, 2020  8061     bsteffen  Use LocalizationResourceNode to prevent NPE.
 *
 * </pre>
 *
 * @author mapeters
 */

public class LocalizationMergeEditorInput extends CompareEditorInput
        implements ISaveablesSource {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationMergeEditorInput.class);

    private LocalizationEditorInput input;

    private ResourceNode leftLocalNode;

    private ResourceNode rightRemoteNode;

    private Saveable[] saveables;

    private String serverFileChecksum;

    private long timestamp;

    private long timestampAtLastSyncPrompt = -1;

    private boolean isRefreshing = false;

    private boolean isClean = false;

    /**
     * Everything is on UI thread, this is only for async changes to nodes and
     * refreshing
     */
    private final Object LOCK = new Object();

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

        this.leftLocalNode = new LocalizationResourceNode(local.getFile());
        this.rightRemoteNode = new LocalizationResourceNode(remote.getFile());

        this.serverFileChecksum = serverFileChecksum;

        this.input = input;

        timestamp = getTimestamp();

        this.saveables = new Saveable[] { new LocalizationMergeSaveable(this) };
    }

    @Override
    protected Object prepareInput(IProgressMonitor pm)
            throws InvocationTargetException, InterruptedException {
        Object diffs = new Differencer().findDifferences(false, pm, null, null,
                leftLocalNode, rightRemoteNode);
        if (diffs == null) {
            // Returning null causes diff to not update when refreshing
            DiffNode diffNode = new DiffNode(Differencer.NO_CHANGE);
            diffNode.setLeft(leftLocalNode);
            diffNode.setRight(rightRemoteNode);
            diffs = diffNode;
        }
        return diffs;
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
    public void refreshForNewMergeConflict(IDocument doc,
            IProgressMonitor monitor) {
        synchronized (LOCK) {
            // Update the Local tmp file
            leftLocalNode.setContent(doc.get().getBytes());

            // Update the Remote tmp file
            File tmpRemoteFile = rightRemoteNode.getResource().getLocation()
                    .toFile();
            try {
                LocalizationManager.getInstance().retrieveToFile(
                        input.getLocalizationFile(), tmpRemoteFile);
                rightRemoteNode.discardBuffer();
                serverFileChecksum = Checksum.getMD5Checksum(tmpRemoteFile);
            } catch (IOException | CommunicationException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to update the remote file to the latest version: "
                                + e.getLocalizedMessage(),
                        e);
            }

            // Refresh the editor with the files' new contents
            performRefresh(false);
        }
    }

    /**
     * Refresh the editor using this input with the updated contents of the tmp
     * files for the Local and Remote editors. Note that it is the caller's
     * responsibility to call {@link ResourceNode#discardBuffer()} on the
     * left/right node if it alters its actual file and not just its buffer.
     *
     * @param isRefreshingToSavedState
     *            true if the updated contents have been saved, false otherwise
     *            (i.e. whether or not the editor should be dirty after
     *            refreshing)
     */
    private void performRefresh(boolean isRefreshingToSavedState) {
        isRefreshing = true;
        isClean = isRefreshingToSavedState;

        IEditorPart editor = PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage()
                .findEditor(LocalizationMergeEditorInput.this);
        CompareUI.reuseCompareEditor(LocalizationMergeEditorInput.this,
                (IReusableEditor) editor);

        isRefreshing = false;
        isClean = false;
    }

    /**
     * Handle cases where the file isn't in sync with the local file system when
     * this editor is activated.
     */
    public void handlePartActivated() {
        if (shouldSync()) {
            if (!isDirty()) {
                // Auto-sync if clean
                syncLocalWithFileSystem();
            } else {
                // Prompt to sync if dirty
                promptForSync();
            }
        }
    }

    /**
     * Prompt the user to sync the local file with the local file system.
     */
    private void promptForSync() {
        /*
         * Store that we prompted the user to sync when the file had this
         * timestamp, so that we don't reprompt unless the file changes again
         */
        timestampAtLastSyncPrompt = getTimestamp();

        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        String msg = "The file '" + input.getName()
                + "' has been changed on the file system. Do you want to "
                + "replace the editor contents with these changes?";
        SWTMessageBox messageDialog = new SWTMessageBox(shell, "File Changed",
                msg, SWT.YES | SWT.NO | SWT.ICON_QUESTION);

        messageDialog.addCloseCallback(new ICloseCallback() {

            @Override
            public void dialogClosed(Object returnValue) {
                if (returnValue instanceof Integer
                        && ((int) returnValue == SWT.YES)) {
                    syncLocalWithFileSystem();
                }
            }
        });

        messageDialog.open();
    }

    /**
     * Determine if the local file is out of sync with the local file system and
     * if it should be synced.
     *
     * @return true if the file should be synced, otherwise false
     */
    private boolean shouldSync() {
        if (!isConflictResolved()) {
            // Only support syncing once conflict is resolved
            return false;
        }

        long localFileTimestamp = getTimestamp();
        if (timestampAtLastSyncPrompt == localFileTimestamp) {
            /*
             * Shouldn't sync if nothing has changed since user was last
             * prompted to sync
             */
            return false;
        }
        /*
         * Otherwise return if it's out of sync: isSynchronized checks if local
         * file was modified outside of this CAVE, timestamp comparison checks
         * if file was modified by another editor in this CAVE
         */
        return !input.getFile().isSynchronized(IResource.DEPTH_ZERO)
                || localFileTimestamp != timestamp;
    }

    /**
     * Update the stored timestamp of the local file to the current timestamp of
     * the file on the file system
     */
    private void updateTimestamp() {
        timestamp = getTimestamp();
    }

    /**
     * Get the current timestamp of the localization file on the file system.
     *
     * @return the local file's timestamp
     */
    private long getTimestamp() {
        // Only actual file's timestamp updates correctly for external changes
        return input.getFile().getLocation().toFile().lastModified();
    }

    /**
     * Sync the Local editor's contents with the contents of the local file
     */
    public void syncLocalWithFileSystem() {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    input.refreshLocalizationFile();
                    try (InputStream is = input.getLocalizationFile()
                            .openInputStream()) {
                        IFile file = (IFile) leftLocalNode.getResource();
                        file.setContents(is, IResource.FORCE, null);
                        leftLocalNode.discardBuffer();

                        serverFileChecksum = input.getLocalizationFile()
                                .getCheckSum();

                        performRefresh(true);

                        updateTimestamp();
                    } catch (IOException | LocalizationException
                            | CoreException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Failed to refresh merge editor: "
                                        + e.getLocalizedMessage(),
                                e);
                    }
                }
            }
        });
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
        this.setLeftDirty(!isClean);
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

    private static class LocalizationMergeSaveable
            extends AbstractLocalizationSaveable {

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

            try {
                if (validateSave(monitor)) {
                    synchronized (parent.LOCK) {
                        // Flush changes from the viewer into the node
                        parent.flushLeftViewers(monitor);

                        if (performSave(monitor)) {
                            parent.updateTimestamp();
                            /*
                             * Successfully saved to server, update
                             * serverFileChecksum
                             */
                            parent.serverFileChecksum = getChangesChecksum();
                            mergeResolved = true;
                            // Update diff
                            parent.performRefresh(true);
                        }
                    }
                }
            } catch (LocalizationFileVersionConflictException e) {
                handleAnotherMergeConflict(monitor);
            }
        }

        @Override
        protected boolean validateSave(IProgressMonitor monitor) {
            IPathManager pm = PathManagerFactory.getPathManager();
            LocalizationFile inputLocFile = input.getLocalizationFile();
            String latestServerFileChecksum = pm
                    .getLocalizationFile(inputLocFile.getContext(),
                            inputLocFile.getPath())
                    .getCheckSum();
            if (!latestServerFileChecksum.equals(parent.serverFileChecksum)) {
                // New merge conflict
                mergeResolved = false;
                if (!latestServerFileChecksum.equals(getChangesChecksum())) {
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

        private void handleAnotherMergeConflict(
                final IProgressMonitor monitor) {
            Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                    .getShell();
            String msg = "The file '" + input.getName()
                    + "' has been modified by another user again. The remote "
                    + "file will be updated to the latest version from the server.";
            SWTMessageBox messageDialog = new SWTMessageBox(shell,
                    "File Version Conflict", msg, SWT.OK | SWT.ICON_WARNING);

            messageDialog.addCloseCallback(new ICloseCallback() {

                @Override
                public void dialogClosed(Object returnValue) {
                    parent.refreshForNewMergeConflict(
                            CompareUI.getDocument(node), monitor);
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
