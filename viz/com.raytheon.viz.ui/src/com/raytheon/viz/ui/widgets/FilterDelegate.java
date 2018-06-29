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

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

/**
 * Creates within a specified {@link Composite} and handles user interaction
 * with a filtering component.
 *
 * Code was copied from FilteredTree, as FilteredTree did not offer an advanced
 * enough matching capability
 *
 * This creates a nice looking text widget with a button embedded in the widget
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2015 4401       bkowal      Initial creation
 * Aug 26, 2015 4800       bkowal      Prevent NPE if all required properties are
 *                                     not set in time before the first filter.
 * Dec 18, 2015 5216       dgilling    Use ModifyListener instead of KeyListener.
 * Nov 21, 2016 6006       njensen     Eclipse 4.6 Replace gif with png
 * Jul 11, 2017 ----       mjames@ucar Don't both creating images for filetree.
 * 
 * </pre>
 *
 * @author bkowal
 */

@SuppressWarnings("restriction")
public class FilterDelegate {

    private TreeViewer treeViewer;

    private final AbstractVizTreeFilter filter;

    private IFilterInput filterInput;

    private Text text;

    public FilterDelegate(Composite parent, AbstractVizTreeFilter filter) {
        this(parent, null, filter, null);
    }

    public FilterDelegate(Composite parent, TreeViewer treeViewer,
            AbstractVizTreeFilter filter, IFilterInput filterInput) {
        this.treeViewer = treeViewer;
        this.filter = filter;
        this.filterInput = filterInput;

        text = new Text(parent, SWT.SINGLE | SWT.SEARCH | SWT.ICON_CANCEL);
        GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        text.setLayoutData(data);
        text.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                filter();
            }
        });
        text.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
                filter();
            }
        });
    }

    private void filter() {
        if (this.treeViewer == null) {
            return;
        }

        // call refresh on the tree to get the most up-to-date children
        this.treeViewer.refresh(false);

        /*
         * Ensure that the filter has been applied to the tree viewer.
         */
        if (this.treeViewer.getFilters().length == 0) {
            ViewerFilter[] filters = new ViewerFilter[1];
            filters[0] = this.filter;
            this.treeViewer.setFilters(filters);
        }

        /*
         * set the current filter text so that it can be used when refresh is
         * called again.
         */
        final String currentText = this.text.getText();
        this.filter.setCurrentText(currentText);
        final boolean expandedState = (currentText.isEmpty());
        if (this.filterInput != null) {
            for (Object ob : this.filterInput.getObjects()) {
                this.treeViewer.setExpandedState(ob, expandedState);
            }
        }

        // call refresh on the tree after things are expanded
        this.treeViewer.refresh(false);
    }

    /**
     * @param treeViewer
     *            the treeViewer to set
     */
    public void setTreeViewer(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    /**
     * @param filterInput
     *            the filterInput to set
     */
    public void setFilterInput(IFilterInput filterInput) {
        this.filterInput = filterInput;
    }
}
