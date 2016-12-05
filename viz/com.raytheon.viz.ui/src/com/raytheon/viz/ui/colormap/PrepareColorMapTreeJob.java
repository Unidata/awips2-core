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
package com.raytheon.viz.ui.colormap;

import java.util.LinkedList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * 
 * Job to call {@link ColorMapTree#prepare()} in a background thread so that the
 * UI is not blocked by use of the trees. This is not intended to be used
 * directly, {@link ColorMapTree#prepareAsync(java.util.Optional)} should be
 * used to schedule preparation.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------
 * Nov 30, 2016  5990     bsteffen  Extracted From ColorMapTreeFactory
 * 
 * </pre>
 *
 * @author bsteffen
 */
class PrepareColorMapTreeJob extends Job {

    private static PrepareColorMapTreeJob instance = new PrepareColorMapTreeJob();

    private final LinkedList<ColorMapTree> queue = new LinkedList<>();

    private PrepareColorMapTreeJob() {
        super("Prepare Color Map Trees");
        setSystem(true);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        ColorMapTree tree = null;

        while (true) {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            synchronized (queue) {
                if (queue.isEmpty()) {
                    return Status.OK_STATUS;
                }
                tree = queue.pop();
            }
            tree.prepare();
        }
    }

    /**
     * Add a tree to the job so it can be prepared.
     * 
     * @param tree
     *            the tree to prepare.
     * @param now
     *            Whether the tree should be scheduled before other waiting
     *            trees. This should be true only if the user is actively
     *            looking at a UI component for this tree and false for
     *            background optimizations.
     */
    public static void add(ColorMapTree tree, boolean now) {
        synchronized (instance.queue) {
            if (now) {
                instance.queue.addFirst(tree);
            } else {
                instance.queue.addLast(tree);
            }
            instance.schedule();
        }
    }

}