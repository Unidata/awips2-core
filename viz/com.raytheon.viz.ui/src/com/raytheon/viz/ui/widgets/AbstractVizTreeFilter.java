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
package com.raytheon.viz.ui.widgets;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

/**
 * Filters a tree view.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 15, 2015 4401       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public abstract class AbstractVizTreeFilter extends ViewerFilter {

    private String currentText;

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        if (currentText == null) {
            return true;
        }

        boolean retVal = true;
        String labelText = ((ILabelProvider) ((StructuredViewer) viewer)
                .getLabelProvider()).getText(element);
        if (labelText.equals(currentText)) {
            viewer.setSelection(new StructuredSelection(element));
        }
        if (this.shouldFilter(element)) {
            String[] words = getWords(currentText);
            for (String word : words) {
                if (!labelText.toUpperCase().contains(word.toUpperCase())) {
                    retVal = false;
                    break;
                }
            }
        }

        return retVal;
    }

    /**
     * Determines if the filter should be applied to the specified
     * {@link Object}.
     * 
     * @param element
     *            the specified {@link Object}.
     * @return true, if the filter should be applied; false, otherwise
     */
    protected abstract boolean shouldFilter(Object element);

    private String[] getWords(String text) {
        return text.trim().split("\\s+");
    }

    /**
     * @param currentText
     *            the currentText to set
     */
    public void setCurrentText(String currentText) {
        this.currentText = currentText;
    }
}