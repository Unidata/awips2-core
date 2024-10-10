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
package com.raytheon.viz.core.contours;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;

/**
 * ContourManagerJob
 *
 * Provides a job that can create contours asynchronously
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 24, 2007           chammack  Initial Creation.
 * Feb 27, 2014  2791     bsteffen  Switch from IDataRecord to DataSource
 * Dec 06, 2021  8341     randerso  Added use of getResourceId for contour
 *                                  logging
 *
 * </pre>
 *
 * @author chammack
 */
public class ContourManagerJob extends Job {

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("ContourManagerJob");

    private static ContourManagerJob instance;

    private ConcurrentLinkedQueue<ContourCreateRequest> requestQueue;

    private ContourManagerJob() {
        super("Contouring");
        this.requestQueue = new ConcurrentLinkedQueue<>();
    }

    /**
     * Get instance
     *
     * @return the singleton instance of ControurManagerJob
     */
    public static synchronized ContourManagerJob getInstance() {
        if (instance == null) {
            instance = new ContourManagerJob();
            instance.setSystem(false);
            instance.schedule();
        }

        return instance;
    }

    /**
     * Request a contour group
     *
     * @param request
     *
     */
    public void request(ContourCreateRequest request) {
        this.requestQueue.add(request);
        this.schedule();
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {

        ContourCreateRequest req;
        while ((req = this.requestQueue.poll()) != null) {
            try {
                if (req.isCanceled() || req.getContourGroup() != null) {
                    // request has been canceled or contours exist
                } else {
                    perfLog.log(String.format("createContours called for [%s]",
                            req.getResourceId()));
                    long t0 = System.currentTimeMillis();
                    ContourGroup cg;
                    cg = ContourSupport.createContours(req.getResourceId(),
                            req.getSource(), req.getLevel(),
                            req.getPixelExtent(), req.getCurrentDensity(),
                            req.getCurrentMagnification(),
                            req.getImageGridGeometry(), req.getTarget(),
                            req.getDescriptor(), req.getPrefs(), req.getZoom());
                    // setContourGroup will check if cg needs to be disposed
                    req.setContourGroup(cg);
                    perfLog.logDuration(
                            String.format("createContours for [%s]",
                                    req.getResourceId()),
                            (System.currentTimeMillis() - t0));
                }
            } catch (Throwable e) {
                return new Status(Status.ERROR,
                        ContourManagerJob.class.getPackage().getName(),
                        "Error creating contours", e);

            }
        }

        return Status.OK_STATUS;
    }
}
