/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
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

import org.eclipse.compare.ResourceNode;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;

/**
 * Extend ResourceNode to allow more flexibility in getImage() and avoid
 * NullPointerException.
 *
 * In eclipse 4.12, ResourceNode.getImage() throws a NullPointerException if the
 * resource cannot be adapted to an IWorkbenchAdapter. The Files used as
 * resources in {@link LocalizationCompareEditorInput} and
 * {@link LocalizationMergeEditorInput} cannot be adapted so a subclass of
 * ResourceNode is provided that can get images without the adapting.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Apr 16, 2020  8061     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
class LocalizationResourceNode extends ResourceNode {

    public LocalizationResourceNode(IResource resource) {
        super(resource);
    }

    @Override
    public Image getImage() {
        String path = getResource().getLocation().toPortableString();
        ImageDescriptor desc = LocalizationEditorUtils.getEditorRegistry()
                .getImageDescriptor(path);
        return desc.createImage();
    }

}