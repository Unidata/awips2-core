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
package com.raytheon.uf.viz.core.maps.jobs;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;

/**
 * Base class for map database query jobs
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2014  #3459     randerso     Initial creation
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public abstract class AbstractMapQueryJob<REQUEST extends AbstractMapRequest<?>, RESULT extends AbstractMapResult>
        extends Job {
    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("MapQueryJob:");

    private IProgressMonitor monitor;

    private REQUEST pendingRequest;

    private RESULT latestResult;

    public AbstractMapQueryJob() {
        super("Retrieving map...");
    }

    public void queueRequest(REQUEST request) {
        request.setQueuedTime(System.currentTimeMillis());
        synchronized (this) {
            pendingRequest = request;
        }

        this.cancel();
        this.schedule();
    }

    public void stop() {
        System.out.println("Stopping queryJob");
        synchronized (this) {
            pendingRequest = null;
        }

        this.cancel();
        try {
            this.join();
        } catch (InterruptedException e) {
            // do nothing, just waiting for job to die
        }

        synchronized (this) {
            if (latestResult != null) {
                latestResult.dispose();
                latestResult = null;
            }
        }
    }

    public RESULT getLatestResult() {
        synchronized (this) {
            RESULT result = latestResult;
            latestResult = null;
            return result;
        }
    }

    protected boolean checkCanceled(RESULT result) {
        if (monitor.isCanceled()) {
            result.cancel();
        }
        return result.isCanceled();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.
     * IProgressMonitor)
     */
    @Override
    protected IStatus run(IProgressMonitor monitor) {
        this.monitor = monitor;
        REQUEST req = null;
        synchronized (this) {
            req = pendingRequest;
            pendingRequest = null;
        }

        IStatus status = Status.OK_STATUS;
        if ((req != null) && !this.monitor.isCanceled()) {
            String mapName = req.getResource().getName();
            this.setName("Retrieving " + mapName + " map...");
            System.out.println("Processing " + mapName + " request: "
                    + req.getNumber());
            RESULT result = getNewResult(req);
            try {
                if (checkCanceled(result)) {
                    result.cancel();
                } else {
                    processRequest(req, result);
                }
            } catch (Throwable e) {
                result.setCause(e);
            } finally {
                if (result.isCanceled()) {
                    System.out.println("Canceling " + mapName + " request: "
                            + req.getNumber());
                    result.dispose();
                    result = null;
                    status = Status.CANCEL_STATUS;
                } else {
                    if (result.isFailed()) {
                        System.out.println("Failed " + mapName + " request: "
                                + req.getNumber());
                    } else {
                        System.out.println("Completed " + mapName
                                + " request: " + req.getNumber());
                    }

                    synchronized (this) {
                        latestResult = result;
                    }
                    req.getResource().issueRefresh();
                }
                perfLog.logDuration("Loading map " + mapName,
                        System.currentTimeMillis() - req.getQueuedTime());
            }
        }
        return status;
    }

    /**
     * @param req
     * @return
     */
    abstract protected RESULT getNewResult(REQUEST req);

    /**
     * Process request and place results in result.
     * 
     * This method should call checkCanceled(result) during any long running
     * processes and if it returns true, terminate processing
     * 
     * @param request
     * @param result
     * @throws Exception
     */
    abstract protected void processRequest(REQUEST request, final RESULT result)
            throws Exception;

}
