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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.python.PyCacheUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationCompareEditorInput;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationMergeEditorInput;
import com.raytheon.viz.ui.dialogs.SWTMessageBox;

/**
 * Deletes the selected localization file
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 03, 2010           mschenke  Initial creation
 * Feb 18, 2015  4132     mapeters  Fixed issue with deleting overrides.
 * Jun 29, 2015  946      rferrel   Do not allow delete of a protected level
 *                                  file.
 * Nov 13, 2015  4946     mapeters  Use SWTMessageBox instead of MessageDialog.
 * Jan 15, 2016  5242     kbisanz   Replaced LocalizationFile with
 *                                  ILocalizationFile where possible
 * Jan 27, 2016  5054     randerso  Cleaned up SWTMessageBox
 * Mar 25, 2016  5214     mapeters  Support deletion of directories.
 * Apr 12, 2016  4946     mapeters  Fixed issue where action did nothing if
 *                                  prompt == false
 * Jun 02, 2016  4907     mapeters  Close merge/compare editors of deleted files
 * Aug 15, 2016  5834     njensen   Enable delete regardless of protection level
 * Jun 22, 2017  4818     mapeters  Changed setCloseCallback to addCloseCallback
 * Feb 08, 2018  6906     randerso  Fix deletion of pyc files
 * Sep 12, 2019  7917     tgurney   Update handling of pyc files for Python 3
 *
 * </pre>
 *
 * @author mschenke
 */

public class DeleteAction extends Action {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(DeleteAction.class);

    private LocalizationFile[] toDelete;

    private IWorkbenchPage page;

    private boolean prompt;

    public DeleteAction(IWorkbenchPage page, LocalizationFile[] toDelete) {
        this(page, toDelete, true);
    }

    public DeleteAction(IWorkbenchPage page, LocalizationFile[] toDelete,
            boolean prompt) {
        super("Delete");
        this.page = page;
        this.toDelete = toDelete;
        this.prompt = prompt;
    }

    @Override
    public void run() {
        StringBuilder listOfFiles = buildFileListString();

        if (prompt) {
            StringBuilder msg = new StringBuilder();
            msg.append("Are you sure you want to delete ");
            if (toDelete.length > 1) {
                msg.append("these " + toDelete.length + " items");
            } else {
                msg.append("this file");
            }
            msg.append("?\n\n").append(listOfFiles);

            Shell shell = page.getWorkbenchWindow().getShell();
            SWTMessageBox messageDialog = new SWTMessageBox(shell,
                    "Delete Confirmation", msg.toString(),
                    SWT.OK | SWT.CANCEL | SWT.ICON_QUESTION);

            messageDialog.addCloseCallback(returnValue -> {
                if (returnValue instanceof Integer) {
                    if ((int) returnValue == SWT.OK) {
                        deleteFiles();
                    }
                }
            });

            messageDialog.open();
        } else {
            deleteFiles();
        }
    }

    /**
     * Build a StringBuilder containing the list of files that are being
     * deleted. Note that for entire directories being deleted, only the parent
     * directory is listed (note that this requires the parent directory being
     * listed before any of its contents in toDelete).
     *
     * @return the list of files as a StringBuilder
     */
    private StringBuilder buildFileListString() {
        StringBuilder listOfFiles = new StringBuilder();

        String parentDirPath = null;
        for (LocalizationFile fileToDelete : toDelete) {
            String path = fileToDelete.getPath();

            // Skip file if parent directory already listed
            if (parentDirPath != null && path.startsWith(parentDirPath)) {
                continue;
            }

            listOfFiles.append(LocalizationUtil.extractName(path));
            if (fileToDelete.isDirectory()) {
                listOfFiles.append(" (and all contents)");
                parentDirPath = path;
            } else {
                // Reached end of directory specified by parentDirPath
                parentDirPath = null;
            }

            listOfFiles.append("\n");
        }

        return listOfFiles;
    }

    /**
     * Delete the selected files and all associated file extension variations.
     */
    private void deleteFiles() {
        List<LocalizationFile> toDeleteList = Arrays.asList(toDelete);
        List<IEditorReference> toClose = new ArrayList<>();
        // check for open editors and close them
        for (IEditorReference ref : page.getEditorReferences()) {
            IEditorInput input = null;
            try {
                input = ref.getEditorInput();
            } catch (PartInitException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Failed to check if an editor for the deleted "
                                + "file was open (in order to close it)",
                        e);
            }

            LocalizationEditorInput[] editorInputs = new LocalizationEditorInput[0];

            if (input instanceof LocalizationEditorInput) {
                LocalizationEditorInput editorInput = (LocalizationEditorInput) input;
                editorInputs = new LocalizationEditorInput[] { editorInput };

            } else if (input instanceof LocalizationMergeEditorInput) {
                LocalizationMergeEditorInput mergeInput = (LocalizationMergeEditorInput) input;
                editorInputs = new LocalizationEditorInput[] {
                        mergeInput.getLocalizationEditorInput() };

            } else if (input instanceof LocalizationCompareEditorInput) {
                LocalizationCompareEditorInput compareInput = (LocalizationCompareEditorInput) input;
                editorInputs = compareInput.getEditorInputs();
            }

            for (LocalizationEditorInput editorInput : editorInputs) {
                if (toDeleteList.contains(editorInput.getLocalizationFile())) {
                    toClose.add(ref);
                    break;
                }
            }
        }

        if (!toClose.isEmpty()) {
            page.closeEditors(
                    toClose.toArray(new IEditorReference[toClose.size()]),
                    false);
        }

        /*
         * Sort files in reverse order, this ensures files are deleted from the
         * bottom of the file tree up so that all directories are empty when
         * they are deleted.
         */
        Arrays.sort(toDelete, Collections.reverseOrder());
        for (ILocalizationFile file : toDelete) {
            try {
                deleteFile(file);
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error deleting file: " + file.toString(), e);
            }
        }
    }

    /**
     * Delete the file. If the file has a .py extension, also delete any
     * corresponding pycache files.
     *
     * @param file
     *            The file to delete
     * @throws Exception
     */
    private void deleteFile(ILocalizationFile file) throws Exception {
        if (!file.isDirectory() && file.getPath().endsWith(".py")) {
            try {
                PyCacheUtil.clean(file);
            } catch (LocalizationException e) {
                /*
                 * Don't want to fail the whole delete if just the pycache
                 * file(s) failed to delete.
                 */
                statusHandler
                        .info("Error deleting the pycache file for " + file, e);
            }
        }
        file.delete();
    }

    @Override
    public boolean isEnabled() {
        boolean canDelete = true;
        for (LocalizationFile file : toDelete) {
            LocalizationContext ctx = file.getContext();
            LocalizationLevel level = ctx.getLocalizationLevel();
            if (level.isSystemLevel()) {
                canDelete = false;
                break;
            }
        }
        return canDelete;
    }
}
