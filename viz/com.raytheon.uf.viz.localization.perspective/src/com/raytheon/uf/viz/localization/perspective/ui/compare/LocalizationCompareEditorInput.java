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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ResourceNode;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;

import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.localization.perspective.LocalizationFileModificationValidator;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.view.actions.ResolveFileVersionConflictAction;
import com.raytheon.viz.ui.dialogs.ICloseCallback;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;

/**
 * Comparing editor input for localization files
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 24, 2011           mschenke  Initial creation
 * Jan 22, 2015  4108     randerso  Allow editing in the compare editor
 * Aug 18, 2015  3806     njensen   Use SaveableOutputStream to save
 * May 23, 2016  4907     mapeters  Added save validation and sync handling,
 *                                  fix saving with read-only files, abstracted
 *                                  out most of saveable class
 * Jun 22, 2017  4818     mapeters  Changed setCloseCallback to addCloseCallback
 * Apr 16, 2020  8061     bsteffen  Extend ResourceNode to prevent NPE.
 * Jun 12, 2020  8061     bsteffen  Extract LocalizationResourceNode to its own file.
 *
 * </pre>
 *
 * @author mschenke
 */
public class LocalizationCompareEditorInput extends CompareEditorInput
        implements ISaveablesSource {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationCompareEditorInput.class);

    private LocalizationEditorInput leftInput;

    private LocalizationEditorInput rightInput;

    private ResourceNode leftNode;

    private ResourceNode rightNode;

    private long leftTimestamp;

    private long rightTimestamp;

    private long leftTimestampAtLastSyncPrompt = -1;

    private long rightTimestampAtLastSyncPrompt = -1;

    private Saveable[] saveables;

    private boolean isRefreshing = false;

    /**
     * Everything is on UI thread, this is only for async changes to nodes and
     * refreshing
     */
    private final Object LOCK = new Object();

    public LocalizationCompareEditorInput(LocalizationEditorInput left,
            LocalizationEditorInput right) {
        super(new CompareConfiguration());

        boolean leftReadOnly = left.getFile().isReadOnly();
        boolean rightReadOnly = right.getFile().isReadOnly();

        CompareConfiguration config = getCompareConfiguration();
        config.setLeftEditable(!leftReadOnly);
        config.setRightEditable(!rightReadOnly);
        config.setLeftLabel(left.getName());
        config.setRightLabel(right.getName());

        this.leftInput = left;
        this.rightInput = right;

        this.leftNode = new LocalizationResourceNode(left.getFile());
        this.rightNode = new LocalizationResourceNode(right.getFile());

        leftTimestamp = getTimestamp(leftNode);
        rightTimestamp = getTimestamp(rightNode);

        List<Saveable> saveablesList = new ArrayList<>();
        if (!leftReadOnly) {
            saveablesList.add(new LocalizationCompareSaveable(this, true));
        }
        if (!rightReadOnly) {
            saveablesList.add(new LocalizationCompareSaveable(this, false));
        }
        this.saveables = saveablesList.toArray(new Saveable[0]);
    }

    @Override
    protected Object prepareInput(IProgressMonitor pm)
            throws InvocationTargetException, InterruptedException {
        Object diffs = new Differencer().findDifferences(false, pm, null, null,
                leftNode, rightNode);
        if (diffs == null && isRefreshing) {
            // Returning null causes diff to not update when refreshing
            DiffNode diffNode = new DiffNode(Differencer.NO_CHANGE);
            diffNode.setLeft(leftNode);
            diffNode.setRight(rightNode);
            diffs = diffNode;
        }
        return diffs;
    }

    /**
     * Sync the editor on the given side with the contents of its corresponding
     * local file.
     *
     * @param left
     *            the side to sync
     */
    public void syncWithFileSystem(final boolean left) {
        VizApp.runAsync(new Runnable() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    /*
                     * Discard buffer so we read new contents from file, make
                     * sure other side's contents are flushed so they aren't
                     * lost
                     */
                    if (left) {
                        leftNode.discardBuffer();
                        flushRightViewers(null);
                    } else {
                        rightNode.discardBuffer();
                        flushLeftViewers(null);
                    }

                    // Refresh so new contents are displayed
                    isRefreshing = true;

                    IEditorPart editor = PlatformUI.getWorkbench()
                            .getActiveWorkbenchWindow().getActivePage()
                            .findEditor(LocalizationCompareEditorInput.this);
                    CompareUI.reuseCompareEditor(
                            LocalizationCompareEditorInput.this,
                            (IReusableEditor) editor);

                    isRefreshing = false;

                    updateTimestamp(left);
                    LocalizationEditorInput input = left ? leftInput
                            : rightInput;
                    input.refreshLocalizationFile();
                    if (left) {
                        setLeftDirty(false);
                    } else {
                        setRightDirty(false);
                    }
                }
            }
        });
    }

    /**
     * Handle cases where either file isn't in sync with local file system when
     * this editor is activated.
     */
    public void handlePartActivated() {
        if (shouldSyncLeft()) {
            if (!isLeftSaveNeeded()) {
                // Auto-sync if clean
                syncWithFileSystem(true);
            } else {
                // Prompt to sync if dirty
                promptForSync(true);
            }
        }

        if (shouldSyncRight()) {
            if (!isRightSaveNeeded()) {
                // Auto-sync if clean
                syncWithFileSystem(false);
            } else {
                // Prompt to sync if dirty
                promptForSync(false);
            }
        }
    }

    /**
     * Determine if the given node's resource is out of sync with the local file
     * system and if it should be synced.
     *
     * @param node
     *            the node to check the sync state of
     * @param timestamp
     *            the timestamp of the version of the file in the editor
     * @param lastSyncPromptTimestamp
     *            the timestamp of the file the last time the user was prompted
     *            to sync
     * @return true if the file should be synced, otherwise false
     */
    private static boolean shouldSync(ResourceNode node, long timestamp,
            long lastSyncPromptTimestamp) {
        long localFileTimestamp = getTimestamp(node);

        if (lastSyncPromptTimestamp == localFileTimestamp) {
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
        return !node.getResource().isSynchronized(IResource.DEPTH_ZERO)
                || localFileTimestamp != timestamp;
    }

    private boolean shouldSyncLeft() {
        return shouldSync(leftNode, leftTimestamp,
                leftTimestampAtLastSyncPrompt);
    }

    private boolean shouldSyncRight() {
        return shouldSync(rightNode, rightTimestamp,
                rightTimestampAtLastSyncPrompt);
    }

    /**
     * Prompt the user to sync the file on the given side with the local file
     * system.
     *
     * @param left
     *            the side to prompt for sync
     */
    private void promptForSync(final boolean left) {
        /*
         * Store that we prompted the user to sync when the file had this
         * timestamp, so that we don't reprompt unless the file changes again
         */
        if (left) {
            leftTimestampAtLastSyncPrompt = getTimestamp(leftNode);
        } else {
            rightTimestampAtLastSyncPrompt = getTimestamp(rightNode);
        }
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        LocalizationEditorInput input = left ? leftInput : rightInput;
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
                    syncWithFileSystem(left);
                }
            }
        });

        messageDialog.open();
    }

    /**
     * Update the stored timestamp of the file on the given side to the current
     * timestamp of the local file
     *
     * @param left
     *            the side to update
     */
    private void updateTimestamp(boolean left) {
        if (left) {
            leftTimestamp = getTimestamp(leftNode);
        } else {
            rightTimestamp = getTimestamp(rightNode);
        }
    }

    /**
     * Get the current timestamp of the given node's file on the file system.
     *
     * @param node
     * @return the local file's timestamp
     */
    private static long getTimestamp(ResourceNode node) {
        // Only actual file's timestamp updates correctly for external changes
        return node.getResource().getLocation().toFile().lastModified();
    }

    @Override
    public Saveable[] getSaveables() {
        return this.saveables;
    }

    @Override
    public Saveable[] getActiveSaveables() {
        return getSaveables();
    }

    /**
     * Get the inputs for the left and right editors in this compare editor.
     *
     * @return the editor inputs
     */
    public LocalizationEditorInput[] getEditorInputs() {
        return new LocalizationEditorInput[] { leftInput, rightInput };
    }

    private static class LocalizationCompareSaveable
            extends AbstractLocalizationSaveable {

        private LocalizationCompareEditorInput parent;

        private boolean left;

        public LocalizationCompareSaveable(
                LocalizationCompareEditorInput parent, boolean left) {
            super(left ? parent.leftInput : parent.rightInput,
                    left ? parent.leftNode : parent.rightNode);
            this.parent = parent;
            this.left = left;
        }

        @Override
        public void doSave(IProgressMonitor monitor) throws CoreException {
            try {
                if (validateSave(monitor)) {
                    synchronized (parent.LOCK) {
                        // Flush changes from the viewer into the node
                        if (left) {
                            parent.flushLeftViewers(monitor);
                        } else {
                            parent.flushRightViewers(monitor);
                        }
                        if (performSave(monitor)) {
                            parent.updateTimestamp(left);
                            // Update diff
                            parent.syncWithFileSystem(left);
                        }
                    }
                }
            } catch (LocalizationFileVersionConflictException e) {
                IDocument doc = CompareUI.getDocument(node);
                new ResolveFileVersionConflictAction(input, doc, null).run();
            }
        }

        @Override
        protected boolean validateSave(IProgressMonitor monitor) {
            IStatus status = LocalizationFileModificationValidator
                    .validateSave(input, CompareUI.getDocument(node));
            return status == Status.OK_STATUS;
        }

        @Override
        public boolean isDirty() {
            return (left ? parent.isLeftSaveNeeded()
                    : parent.isRightSaveNeeded());
        }
    }
}
