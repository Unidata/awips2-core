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

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.Saveable;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationFileVersionConflictException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;

/**
 * A saveable unit for localization compare and merge editors
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

public abstract class AbstractLocalizationSaveable extends Saveable {

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractLocalizationSaveable.class);

    protected LocalizationEditorInput input;

    protected ResourceNode node;

    protected AbstractLocalizationSaveable(LocalizationEditorInput input,
            ResourceNode node) {
        this.input = input;
        this.node = node;
    }

    /**
     * Validate that we are allowed to save, i.e. that the localization file
     * hasn't been changed by another user on the server. This method also
     * handles the conflict if one does occur.
     * 
     * @param monitor
     * @return true if save is allowed, false otherwise
     */
    protected abstract boolean validateSave(IProgressMonitor monitor);

    /**
     * Validate that we are able to save, and save the editor contents.
     * 
     * @param monitor
     * @return true if the contents are successfully saved, false otherwise
     * @throws LocalizationFileVersionConflictException
     *             if the server file has been changed by another user
     */
    protected boolean validateAndPerformSave(IProgressMonitor monitor)
            throws LocalizationFileVersionConflictException {
        if (!validateSave(monitor)) {
            return false;
        }

        boolean success = false;

        // write node contents to the localization file
        ILocalizationFile lf = input.getLocalizationFile();
        try (SaveableOutputStream os = lf.openOutputStream();
                InputStream is = node.getContents()) {

            byte[] buf = new byte[2048];
            int len = is.read(buf);
            while (len > 0) {
                os.write(buf, 0, len);
                len = is.read(buf);
            }

            os.save();

            success = true;

            // Update the localization file to the newly saved version
            input.refreshLocalizationFile();

            // Force other editors on this file to update
            input.getFile().refreshLocal(IResource.DEPTH_ZERO, monitor);
        } catch (CoreException e) {
            statusHandler.error("Error refreshing local resources for "
                    + input.getFile().getName(), e);
        } catch (IOException e) {
            if (e.getCause() instanceof LocalizationFileVersionConflictException) {
                /*
                 * Only happens if server file changes between validateSave()
                 * and os.save() calls
                 */
                throw (LocalizationFileVersionConflictException) e.getCause();
            } else {
                statusHandler.error("Error saving " + lf, e);
            }
        } catch (Exception e) {
            statusHandler.error("Error saving " + lf, e);
        }

        return success;
    }

    @Override
    public String getName() {
        return input.getName();
    }

    @Override
    public String getToolTipText() {
        return input.getToolTipText();
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return input.getImageDescriptor();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((input == null) ? 0 : input.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AbstractLocalizationSaveable other = (AbstractLocalizationSaveable) obj;
        if (input == null) {
            if (other.input != null) {
                return false;
            }
        } else if (!input.equals(other.input)) {
            return false;
        }
        return true;
    }
}
