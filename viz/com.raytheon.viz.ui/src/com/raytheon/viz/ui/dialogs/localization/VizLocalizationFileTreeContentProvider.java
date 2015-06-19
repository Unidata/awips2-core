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
package com.raytheon.viz.ui.dialogs.localization;

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * 
 * Displays a list of localization files in a tree view. This class was formerly
 * com.raytheon.uf.viz.d2d.ui.dialogs.procedures.ProcedureTreeContentProvider.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ???                                 Initial creation
 * 02 Jun 2015  4401       bkowal      Re-factored for reuse.
 * 
 * </pre>
 * 
 * @author unknown
 * @version 1.0
 */
public class VizLocalizationFileTreeContentProvider implements
        ITreeContentProvider {

    private VizLocalizationFileTree rootNode = null;

    public VizLocalizationFileTreeContentProvider(VizLocalizationFileTree tree) {
        rootNode = tree;
    }

    @Override
    public void dispose() {
        // nothing to do here
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        // nothing to do here
    }

    @Override
    public Object[] getElements(Object inputElement) {
        if (rootNode != null) {
            if (rootNode.getChildren() != null) {
                return rootNode.getChildren().toArray();
            }
        }
        return new Object[0];
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        if (parentElement instanceof VizLocalizationFileTree) {
            VizLocalizationFileTree parent = (VizLocalizationFileTree) parentElement;
            List<VizLocalizationFileTree> children = parent.getChildren();
            if (children != null) {
                return children.toArray();
            } else {
                return new Object[0];
            }
        }
        return null;
    }

    @Override
    public Object getParent(Object element) {
        return null;
    }

    @Override
    public boolean hasChildren(Object element) {
        if (element instanceof VizLocalizationFileTree) {
            VizLocalizationFileTree elem = (VizLocalizationFileTree) element;
            return elem.hasChildren();
        }
        return false;
    }

    public Object findItem(String text) {
        VizLocalizationFileTree item = rootNode.findChildByText(text);
        return item;
    }

}
