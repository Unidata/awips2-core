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
package com.raytheon.uf.viz.localization.perspective.editor;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationMergeEditorInput;

/**
 * Provides utility class for interacting with editors in the Localization
 * perspective
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 27, 2013            mschenke    Methods extracted from LocalizationPerspectiveUtils
 * Jan 11, 2016 5242       kbisanz     Replaced calls to deprecated LocalizationFile methods
 * May 23, 2016 4907       mapeters    Added method for getting open merge editors
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class LocalizationEditorUtils {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationEditorUtils.class);

    private static final String DEFAULT_TEXT_EDITOR = "org.eclipse.ui.DefaultTextEditor";

    /**
     * Get the editor descriptors for the localization file input
     * 
     * @param input
     * @return the editor descriptors
     */
    public static IEditorDescriptor[] getEditorsForInput(
            LocalizationEditorInput input) {
        IEditorRegistry reg = getEditorRegistry();
        if (reg == null) {
            return new IEditorDescriptor[0];
        }
        return reg.getEditors(input.getLocalizationFile().getPath());
    }

    /**
     * Get the default editor for the localization editor input
     * 
     * @param input
     * @return the default editor descriptor or null if none
     */
    public static IEditorDescriptor getDefaultEditorForInput(
            LocalizationEditorInput input) {
        IEditorRegistry reg = getEditorRegistry();
        if (reg == null) {
            return null;
        }
        return reg.getDefaultEditor(input.getLocalizationFile().getPath());
    }

    /**
     * Get the text editor descriptor
     * 
     * @return the text editor descriptor or null if none
     */
    public static IEditorDescriptor getTextEditorDescriptor() {
        IEditorRegistry reg = getEditorRegistry();
        if (reg == null) {
            return null;
        }
        return reg.findEditor(DEFAULT_TEXT_EDITOR);
    }

    /**
     * Opens an editor using the editor input and default editor associated with
     * file extension
     * 
     * @param page
     *            page to open in
     * @param input
     *            input to open with
     * @return null the opened editor or null no editor opened
     */
    public static IEditorPart openInEditor(IWorkbenchPage page,
            LocalizationEditorInput input) {
        IEditorRegistry reg = getEditorRegistry();
        if (reg != null) {
            IEditorDescriptor desc = reg.getDefaultEditor(input
                    .getLocalizationFile().getPath());
            String id = DEFAULT_TEXT_EDITOR;
            if (desc != null) {
                id = desc.getId();
            }
            return openInEditor(page, input, id);
        }
        return null;
    }

    /**
     * Open an editor given the input and editor id, activate editor if already
     * opened
     * 
     * @param page
     * @param input
     * @param editorId
     * @return the opened/activated editor
     */
    public static IEditorPart openInEditor(IWorkbenchPage page,
            LocalizationEditorInput input, String editorId) {
        IEditorPart rval = null;
        IEditorPart existingPart = null;
        try {
            input.getFile().refreshLocal(IResource.DEPTH_ZERO, null);
            existingPart = getEditorForFile(page, input.getLocalizationFile());
            if ((existingPart != null)
                    && existingPart.getEditorSite().getId().equals(editorId)) {
                page.activate(existingPart);
                rval = existingPart;
            } else {
                rval = page.openEditor(input, editorId, true);
            }
        } catch (PartInitException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error opening editor for file: " + input.getName(), e);
        } catch (CoreException e) {
            // Ignore exception from refreshing the file
        }

        return rval;
    }

    /**
     * Get the workbench editor registry
     * 
     * @return the workbench editor registry
     */
    public static IEditorRegistry getEditorRegistry() {
        return PlatformUI.getWorkbench().getEditorRegistry();
    }

    /**
     * Get the open editor editing the file
     * 
     * @param file
     * @return the open editor part or null if not being edited
     */
    public static IEditorPart getEditorForFile(IWorkbenchPage page,
            ILocalizationFile file) {
        for (IEditorReference ref : page.getEditorReferences()) {
            IEditorPart part = ref.getEditor(false);
            if (part != null) {
                IEditorInput input = part.getEditorInput();
                if (input instanceof LocalizationEditorInput) {
                    ILocalizationFile editedFile = ((LocalizationEditorInput) input)
                            .getLocalizationFile();
                    if (editedFile.getContext().equals(file.getContext())
                            && editedFile.getPath().equals(file.getPath())) {
                        return part;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Get the open merge editor input handling a file version conflict for the
     * given localization editor input.
     * 
     * @param input
     * @return the merge editor input, or null if none is open
     */
    public static LocalizationMergeEditorInput getOpenMergeInputForLocalizationInput(
            LocalizationEditorInput input) {
        for (IEditorReference ref : PlatformUI.getWorkbench()
                .getActiveWorkbenchWindow().getActivePage()
                .getEditorReferences()) {
            IEditorPart part = ref.getEditor(false);
            if (part != null) {
                IEditorInput editorInput = part.getEditorInput();
                if (editorInput instanceof LocalizationMergeEditorInput) {
                    LocalizationMergeEditorInput mergeInput = (LocalizationMergeEditorInput) editorInput;
                    LocalizationEditorInput locInput = mergeInput
                            .getLocalizationEditorInput();
                    if (locInput.equals(input)) {
                        return mergeInput;
                    }
                }
            }
        }

        return null;
    }
}
