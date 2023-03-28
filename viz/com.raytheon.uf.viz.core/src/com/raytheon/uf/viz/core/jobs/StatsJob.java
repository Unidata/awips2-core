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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.comm.NetworkStatistics;
import com.raytheon.uf.common.comm.NetworkStatistics.NetworkTraffic;

/**
 * Stats job for thin client. Logs network stats every minute to console
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 2, 2011             mschenke    Initial creation
 * Jan 27, 2016 5170       tjensen     Changed to only display byte stats when configured
 * Dec 11, 2017            mjames      Less logging (re-implemented 3/15/23)
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class StatsJob extends Job {

    private NetworkStatistics stats;

    private long lastSent = 0, lastReceived = 0, lastRequestCount = 0;

    private boolean run = false;

    private Thread runningThread;

    /**
     * @param name
     */
    public StatsJob(String name, NetworkStatistics stats) {
        super(name);
        setSystem(true);
        this.stats = stats;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        runningThread = Thread.currentThread();
        run = true;
        while (run) {
            NetworkTraffic total = stats.getTotalTrafficStats();
            long sentInLastMinute = total.getBytesSent() - lastSent;
            long receivedInLastMinute = total.getBytesReceived() - lastReceived;
            long requestCountInLastMinute = total.getRequestCount()
                    - lastRequestCount;
            boolean doBytes = total.isDoByteStats();

            boolean printed = false;
            if (sentInLastMinute != 0.0 || receivedInLastMinute != 0.0
                    || requestCountInLastMinute != 0.0) {
                printed = true;
                String bytesMsg = "";
                if (doBytes) {
                    bytesMsg = " for a total of "
                            + NetworkStatistics.toString(sentInLastMinute)
                            + " sent and "
                            + NetworkStatistics.toString(receivedInLastMinute)
                            + " received";
                }
            }
            if (doBytes) {
                lastSent = total.getBytesSent();
                lastReceived = total.getBytesReceived();
            }
            lastRequestCount = total.getRequestCount();
            if (printed) {
                String bytesTotalMsg = "";
                if (doBytes) {
                    bytesTotalMsg = " for a total of "
                            + NetworkStatistics.toString(lastSent)
                            + " sent and "
                            + NetworkStatistics.toString(lastReceived)
                            + " received";
                }
                NetworkTraffic[] mapped = stats.getMappedTrafficStats();
            }

            try {
                Thread.sleep(60 * 1000);
            } catch (InterruptedException e) {
            }
        }
        return Status.OK_STATUS;
    }

    public void shutdown() {
        if (run) {
            run = false;
            runningThread.interrupt();
        }
    }
}
