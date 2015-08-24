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
package com.raytheon.uf.viz.productbrowser.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.TreeItem;

import com.raytheon.uf.viz.productbrowser.ProductBrowserDataDefinition;
import com.raytheon.uf.viz.productbrowser.ProductBrowserView;

/**
 * 
 * Job for adding/removing data types for the {@link ProductBrowserView} based
 * on their availability. The tree item (corresponding to a data type) is
 * assumed to be initially in a loading state and this job asynchronously calls
 * {@link ProductBrowserDataDefinition#checkAvailability()} to determine if the
 * item should be expandable and then removes or configures the item on the UI
 * thread.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 13, 2014  3135     bsteffen  Initial creation
 * Jun 02, 2015  4153     bsteffen  Access data definition through an interface.
 * Aug 12, 2015  4717     mapeters  Renamed from ProductBrowserInitializeJob, only 
 *                                  add "fake" tree item to items with no children.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class ProductBrowserUpdateDataTypeJob extends Job implements Runnable {

    protected final TreeItem item;

    protected final ProductBrowserDataDefinition def;

    protected boolean available = false;

    /**
     * Create a new Job for the provided item, this constructor must be called
     * on the UI thread.
     * 
     * @param item
     *            a TreeItem from the {@link ProductBrowserView}
     */
    public ProductBrowserUpdateDataTypeJob(TreeItem item) {
        super(item.getText());
        this.item = item;
        /*
         * Def must be pulled out of item on the UI thread so it can be accessed
         * off the UI thread.
         */
        this.def = ProductBrowserView.getDataDef(item);
    }

    /**
     * Runnable method inherited from {@link Job} for running in the background.
     */
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        available = def.checkAvailability();
        if (!item.isDisposed()) {
            item.getDisplay().syncExec(this);
        }
        return Status.OK_STATUS;

    }

    /**
     * Runnable method inherited from {@link Runnable}. This will be run on the
     * UI thread.
     */
    @Override
    public void run() {
        if (item.isDisposed()) {
            return;
        }
        if (!available) {
            item.dispose();
        } else {
            String displayName = ProductBrowserView.getLabel(item).getName();
            item.setText(displayName);
            if (!ProductBrowserView.getLabel(item).isProduct()
                    && item.getItemCount() == 0) {
                /*
                 * gives the tree the ability to be opened by adding a "fake"
                 * tree item that will be disposed of later
                 */
                TreeItem fake = new TreeItem(item, SWT.NONE);
                fake.setText("Loading...");
            }
        }
    }
}
