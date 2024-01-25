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
package com.raytheon.uf.viz.core.grid.rsc.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.rsc.AbstractGridResource;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource.ResourceStatus;

/**
 *
 * Manages asynchronously requesting data for {@link AbstractGridResource}s.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 16, 2011            bsteffen    Initial creation
 * Jun 04, 2013 2041       bsteffen    Improve exception handing in grid
 *                                     resources.
 * Jun 24, 2013 2140       randerso    Moved safe name code into AbstractVizResource
 * Oct 07, 2014 3668       bclement    uses executor instead of eclipse job
 *                                      renamed to GridDataRequestRunner
 * Oct 29, 2014 3668       bsteffen    replace executor with custom job pool.
 * May 14, 2015 4079       bsteffen    Move to core.grid
 * Aug 31, 2021 8651       njensen     Added method isScheduled(DataTime)
 * Jan 26, 2022 8741       njensen     Added performance logging
 * Dec 20, 2023 2036519    mapeters    Prevent requesting data for disposed resources,
 *                                     add fromCacheOnly arg to requestData()
 *
 * </pre>
 *
 * @author bsteffen
 * @see GridDataRequestJobPool
 */
public class GridDataRequestRunner {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(GridDataRequestRunner.class);

    private static final IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("GridDataRequestRunner");

    private static class GridDataRequest {

        public final DataTime time;

        public final List<PluginDataObject> pdos;

        public List<GeneralGridData> gridData;

        public VizException exception;

        // True only if exception has already been handled(i.e. the user has
        // been notified.)
        public boolean exceptionHandled = false;

        public boolean isRunning = false;

        public GridDataRequest(DataTime time, List<PluginDataObject> pdos) {
            this.time = time;
            if (pdos == null) {
                this.pdos = null;
            } else {
                this.pdos = new ArrayList<>(pdos);
            }

        }

        public boolean shouldRequest() {
            return (gridData == null) && (exception == null) && (!isRunning);
        }

    }

    private final AbstractGridResource<?> resource;

    private final List<GridDataRequest> requests = new ArrayList<>();

    public GridDataRequestRunner(AbstractGridResource<?> resource) {
        this.resource = resource;
    }

    /**
     * Attempt to process a request if there are any that need to be processed
     *
     * @return true if a request was processed or false if no requests need to
     *         be processed.
     */
    public boolean processOneRequest() {
        GridDataRequest request = getNext();
        if (request == null) {
            return false;
        }
        try {
            long t0 = System.currentTimeMillis();
            request.gridData = resource.getData(request.time, request.pdos);
            long t1 = System.currentTimeMillis();
            if (request.pdos != null || request.gridData != null) {
                perfLog.logDuration("Getting grid data for " + request.pdos,
                        t1 - t0);
            }
            if (request.gridData == null) {
                /*
                 * need to remove unfulfillable requests to avoid infinite loop.
                 */
                synchronized (requests) {
                    requests.remove(request);
                }
            } else {
                resource.issueRefresh();
            }
        } catch (VizException e) {
            request.exception = e;
            resource.issueRefresh();
        } finally {
            request.isRunning = false;
        }
        return true;
    }

    /**
     * Get the next request that should be processed
     *
     * @return null if no request should be processed
     */
    protected GridDataRequest getNext() {
        synchronized (requests) {
            for (GridDataRequest request : requests) {
                if (request.shouldRequest()) {
                    request.isRunning = true;
                    return request;
                }
            }
        }
        return null;
    }

    public List<GeneralGridData> requestData(DataTime time,
            List<PluginDataObject> pdos) {
        return requestData(time, pdos, false);
    }

    /**
     * Request data for the given time and PDOs.
     *
     * @param time
     * @param pdos
     * @param fromCacheOnly
     *            true to only get the data if it's already cached and to do
     *            nothing otherwise, false to trigger a request for the data if
     *            it's not yet cached
     * @return null if no requests matching time have been fulfilled
     */
    public List<GeneralGridData> requestData(DataTime time,
            List<PluginDataObject> pdos, boolean fromCacheOnly) {
        GridDataRequest request = null;
        synchronized (requests) {
            if (resource.getStatus() == ResourceStatus.DISPOSED) {
                return null;
            }
            Iterator<GridDataRequest> itr = requests.iterator();
            while (itr.hasNext()) {
                GridDataRequest r = itr.next();
                if (r.time.equals(time)) {
                    if (r.gridData != null) {
                        itr.remove();
                        return r.gridData;
                    } else if (!fromCacheOnly) {
                        itr.remove();
                        if (Objects.equals(r.pdos, pdos)) {
                            request = r;
                        }
                    }
                }
            }
            if (!fromCacheOnly) {
                if (request == null) {
                    request = new GridDataRequest(time, pdos);
                }
                requests.add(0, request);
                if ((request.exception != null) && !request.exceptionHandled) {
                    handleExceptions();
                }
                if (request.shouldRequest()) {
                    GridDataRequestJobPool.schedule(this);
                }
            }
        }
        return null;
    }

    private void handleExceptions() {

        List<GridDataRequest> failedRequests = new ArrayList<>(requests.size());
        synchronized (requests) {
            for (GridDataRequest request : requests) {
                if ((request.exception != null) && !request.exceptionHandled) {
                    failedRequests.add(request);
                }
            }
        }
        if (failedRequests.isEmpty()) {
            return;
        }
        String safeResourceName = resource.getSafeName();
        boolean multiple = failedRequests.size() > 1;
        GridDataRequest request = failedRequests.get(0);
        // Only log one message as a PROBLEM
        statusHandler.handle(Priority.PROBLEM,
                getFailureMessage(safeResourceName, request, multiple),
                request.exception);
        if (multiple) {
            // Log all other messages as INFO so that if there are differences
            // they will be in the log.
            for (GridDataRequest r : failedRequests) {
                statusHandler.handle(Priority.INFO,
                        getFailureMessage(safeResourceName, r, false),
                        r.exception);
                r.exceptionHandled = true;
            }
        } else {
            request.exceptionHandled = true;
        }

    }

    /**
     * Attempt to generate a pretty message to display to the user.
     *
     * @param resourceName
     *            the name of the resource
     * @param request
     *            a failed request
     * @param multiple
     *            whether this message should be for the current request or
     *            multiple frames.
     * @return
     */
    private String getFailureMessage(String resourceName,
            GridDataRequest request, boolean multiple) {
        StringBuilder message = new StringBuilder("Error requesting data for ");
        message.append(resourceName);
        if (multiple) {
            message.append(" Multiple frames");
        } else if (request.time != null) {
            message.append(" ").append(request.time.getLegendString());
        }
        if (request.exception != null) {
            message.append(": ");
            message.append(request.exception.getLocalizedMessage());
        }
        return message.toString();
    }

    public void remove(DataTime time) {
        synchronized (requests) {
            Iterator<GridDataRequest> itr = requests.iterator();
            while (itr.hasNext()) {
                GridDataRequest r = itr.next();
                if (r.time.equals(time)) {
                    itr.remove();
                }
            }
        }
    }

    public void clearRequests() {
        synchronized (requests) {
            requests.clear();
        }
    }

    public boolean isScheduled(DataTime time) {
        synchronized (requests) {
            for (GridDataRequest r : requests) {
                if (r.time.equals(time) && r.shouldRequest()) {
                    return true;
                }
            }
        }
        return false;
    }

}
