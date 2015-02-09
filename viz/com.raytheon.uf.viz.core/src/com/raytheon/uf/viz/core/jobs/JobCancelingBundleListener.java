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
package com.raytheon.uf.viz.core.jobs;

import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.SynchronousBundleListener;

/**
 * A {@link BundleListener} that cancels a job automatically when a bundle is
 * {@link BundleEvent#STOPPING}. This can be used on long running or Daemon type
 * jobs to ensure they stop running when the application is stopped. Jobs that
 * use this listener must respond to cancel requests appropriately.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 09, 2015  4092     bsteffen    Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class JobCancelingBundleListener implements SynchronousBundleListener {

    private final Job job;

    private final Bundle bundle;

    /**
     * Create a listener for the provided job that will automatically stop the
     * job when the bundle that owns the job class is stopping. The listener is
     * automatically added to the bundle context.
     */
    public JobCancelingBundleListener(Job job){
        this(job, FrameworkUtil.getBundle(job.getClass()));
    }

    /**
     * Create a listener for the provided job that will automatically stop the
     * job when the provided bundle is stopping. The listener is automatically
     * added to the bundle context.
     */
    public JobCancelingBundleListener(Job job, Bundle bundle) {
        this.job = job;
        this.bundle = bundle;
        bundle.getBundleContext().addBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getBundle() == bundle
                && event.getType() == BundleEvent.STOPPING) {
            job.cancel();
        }
    }

}
