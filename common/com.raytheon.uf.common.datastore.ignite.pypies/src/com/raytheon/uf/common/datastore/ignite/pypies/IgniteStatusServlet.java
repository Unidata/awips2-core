/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite.pypies;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collection;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.ignite.DataRegionMetrics;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCluster;
import org.apache.ignite.cluster.ClusterMetrics;
import org.apache.ignite.cluster.ClusterNode;

import com.raytheon.uf.common.datastore.ignite.AbstractIgniteManager;
import com.raytheon.uf.common.util.SizeUtil;

/**
 * {@link HttpServlet} which handles GET requests by returning some metrics
 * about an instance of {@link Ignite}. The resulting page attempts to be
 * similar to the metrics that ignite periodically logs, however not all those
 * stats are available through the public API so it is not as good.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Mar 27, 2020  8071     bsteffen  Initial creation
 * Jun 25, 2021  8450     mapeters  Updated for centralized ignite instance management
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteStatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private final AbstractIgniteManager igniteManager;

    public IgniteStatusServlet(AbstractIgniteManager igniteManager) {
        this.igniteManager = igniteManager;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        IgniteCluster cluster = igniteManager.getIgnite().cluster();
        if (!cluster.active()) {
            resp.setStatus(503);
        }

        /*
         * This is loosely based off the internal metrics logging in the
         * IgniteKernal class.
         */
        ClusterNode localNode = cluster.localNode();
        ClusterMetrics localMetrics = localNode.metrics();
        ClusterMetrics metrics = cluster.metrics();

        String nodeId = localNode.id().toString().substring(0, 8);
        long upsecs = Duration.ofMillis(localMetrics.getUpTime()).getSeconds();

        int hosts = cluster.hostNames().size();
        int nodes = metrics.getTotalNodes();
        int cpus = metrics.getTotalCpus();
        int servers = 0;
        int clients = 0;
        for (ClusterNode node : cluster.nodes()) {
            if (node.isClient()) {
                clients += 1;
            } else {
                servers += 1;
            }
        }

        double cpuLoadPct = localMetrics.getCurrentCpuLoad() * 100;
        double avgCpuLoadPct = localMetrics.getAverageCpuLoad() * 100;
        double gcPct = localMetrics.getCurrentGcCpuLoad() * 100;

        long heapUsed = localMetrics.getHeapMemoryUsed();
        long heapMax = localMetrics.getHeapMemoryMaximum();
        String prettyHeapUsed = SizeUtil.prettyByteSize(heapUsed);
        String prettyHeapComm = SizeUtil
                .prettyByteSize(localMetrics.getHeapMemoryCommitted());
        double freeHeapPct = heapMax > 0
                ? ((double) ((heapMax - heapUsed) * 100)) / heapMax
                : -1;

        boolean storageMetricsEnabled = igniteManager.getIgnite()
                .configuration().getDataStorageConfiguration()
                .isMetricsEnabled();

        long offHeapUsed = 0;
        long offHeapComm = 0;
        Collection<DataRegionMetrics> regions = igniteManager.getIgnite()
                .dataRegionMetrics();
        for (DataRegionMetrics region : regions) {
            offHeapUsed += region.getOffheapUsedSize();
            offHeapComm += region.getOffHeapSize();
        }
        String prettyOffHeapUsed = SizeUtil.prettyByteSize(offHeapUsed);
        String prettyOffHeapComm = SizeUtil.prettyByteSize(offHeapComm);

        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();
        out.println("<html>");
        out.printf("<head><title>Ignite Status of Node:%s</title></head>\n",
                nodeId);
        out.println("<body>");
        out.printf("Node [id=%s, uptime=%02d:%02d:%02d]<br />\n", nodeId,
                upsecs / 3600, (upsecs % 3600) / 60, upsecs % 60);
        out.printf(
                "H/N/S/C/CPU [hosts=%d, nodes=%d, servers=%d, clients=%d, CPUs=%d]<br />\n",
                hosts, nodes, servers, clients, cpus);
        if (cpuLoadPct >= 0) {
            out.printf("CPU [cur=%.2f%%, avg=%.2f%%, GC=%.2f%%]<br />\n",
                    cpuLoadPct, avgCpuLoadPct, gcPct);
        } else {
            /* Negative indicates it is not available */
            out.printf("CPU [GC=%.2f%%]<br />\n", gcPct);
        }
        out.printf("Heap [used=%s, free=%.2f%%, comm=%s]<br />\n",
                prettyHeapUsed, freeHeapPct, prettyHeapComm);

        if (offHeapUsed <= 0 && !storageMetricsEnabled) {
            /* committed size is currently reported even with metrics off. */
            out.printf("Off-heap [comm=%s]<br />\n", prettyOffHeapComm);
        } else {
            out.printf("Off-heap [used=%s, comm=%s]<br />\n", prettyOffHeapUsed,
                    prettyOffHeapComm);
        }

        out.println("<ul>");
        for (DataRegionMetrics region : regions) {
            long used = region.getOffheapUsedSize();
            long comm = region.getOffHeapSize();

            String prettyUsed = SizeUtil.prettyByteSize(used);
            String prettyComm = SizeUtil.prettyByteSize(comm);

            if (used <= 0 && !storageMetricsEnabled) {
                /*
                 * committed size is currently reported even with metrics off.
                 */
                out.printf("<li>%s region [comm=%s]</li>\n", region.getName(),
                        prettyComm);
            } else {
                out.printf("<li>%s region [used=%s, comm=%s]</li>\n",
                        region.getName(), prettyUsed, prettyComm);
            }

        }
        out.println("</ul>");

        out.println("</body>");
        out.println("</html>");

    }

}
