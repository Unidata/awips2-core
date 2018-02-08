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
package com.raytheon.viz.ui.jobs;

import java.text.ParseException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IWorkbenchWindow;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.common.util.format.BytesFormat;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 * Monitors the memory usage and notifies the perspective when a configured
 * threshold of memory usage is reached
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 14, 2014 2594       bclement     Initial creation
 * Feb 7, 2018  19577      anilsonm     change threshold to memory units and compute free memory against it
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class MemoryMonitorJob extends Job {

    private static final IUFStatusHandler log = UFStatus
            .getHandler(MemoryMonitorJob.class);

    public static final String THRESHOLD_PROPERTY = "viz.memory.warn.threshold";

    public static final String MONITOR_PERIOD_PROPERTY = "viz.memory.monitor.period";

    private static final long DEFAULT_THRESHOLD = 10 * SizeUtil.BYTES_PER_MB; // approx 10MB

    private static final long DEFAULT_PERIOD = 10 * 1000; // ten seconds

    private final long threshold;

    private final long period;

    private boolean notified = false;

    private static final long MINIMUM_THRESHOLD = 1 * SizeUtil.BYTES_PER_MB; // approx 1MB

    private static long lBytes = 0;

    static{
        String threshold = System.getProperty(THRESHOLD_PROPERTY);

        try {
            lBytes = new BytesFormat().parse(threshold);
        } catch (ParseException e) {
            String msg = "Invalid argument specified for :"+THRESHOLD_PROPERTY;
            log.error(msg, e);
            lBytes = DEFAULT_THRESHOLD;
        }
    }

    /**
     * Default constructor, get configuration from system properties
     */
    public MemoryMonitorJob() {
        this("Low Memory Monitor", lBytes, Long.getLong(MONITOR_PERIOD_PROPERTY, DEFAULT_PERIOD));
    }

    /**
     * @param name
     *            job name
     * @param threshold
     *            memory (in units) used when notification should be triggered
     * @param period
     *            period between memory checks in milliseconds
     */
    public MemoryMonitorJob(String name, long threshold, long period) {
        super(name);
        //log an error and use the DEFAULT_THRESHOLD if threshold < 1M
        if (threshold < MINIMUM_THRESHOLD){
            logInvalidProperty(THRESHOLD_PROPERTY,
                    "Threshold value must be minimum 1MB",
                    DEFAULT_THRESHOLD);
            this.threshold = DEFAULT_THRESHOLD;
        } else {
            this.threshold = threshold;
        }
        if (period < 0){
            logInvalidProperty(MONITOR_PERIOD_PROPERTY,
                    "Monitor period value must be non-negative", DEFAULT_PERIOD);
            this.period = DEFAULT_PERIOD;
        } else {
            this.period = period;
        }
    }

    /**
     * Log invalid configuration message
     * 
     * @param property
     * @param message
     * @param defValue
     */
    private static void logInvalidProperty(String property, String message,
            Object defValue) {
        String msg = "Invalid memory monitor configuration for property: "
                + property + ". " + message + ". Using default value of: "
                + defValue.toString();
        log.error(msg);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            long availMemory = maxMemory - usedMemory;

            if (availMemory < threshold) {
                if (!notified) { // don't pester the user
                    notified = notify(availMemory);
                }
            } else {
                // fell below threshold, reset
                notified = false;
            }
            return Status.OK_STATUS;
        } finally {
            schedule(period);
        }
    }

    /**
     * Notify perspective of low available memory
     * 
     * @param availMemory
     * @return true if notification was displayed
     */
    private boolean notify(long availMemory) {
        boolean rval = false;
        IWorkbenchWindow window = VizWorkbenchManager.getInstance()
                .getCurrentWindow();
        VizPerspectiveListener listener = VizPerspectiveListener
                .getInstance(window);
        if (listener != null) {
            AbstractVizPerspectiveManager manager = listener
                    .getActivePerspectiveManager();
            rval = manager.notifyLowMemory(availMemory);
        }
        return rval;
    }

    /**
     * @return the threshold
     */
    public double getThreshold() {
        return threshold;
    }

    /**
     * @return the period
     */
    public long getPeriod() {
        return period;
    }

}