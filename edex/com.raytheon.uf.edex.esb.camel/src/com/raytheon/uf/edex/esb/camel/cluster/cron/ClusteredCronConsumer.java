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
package com.raytheon.uf.edex.esb.camel.cluster.cron;

import java.time.ZonedDateTime;

import org.apache.camel.Processor;
import org.apache.camel.component.cron.SpringCronConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;

import com.raytheon.uf.edex.database.cluster.ClusterLockUtils;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils.LockState;
import com.raytheon.uf.edex.database.cluster.ClusterTask;

/**
 * The consumer for the ClusteredCronComponent, responsible for managing cron
 * job execution and ensuring that the job only proceeds if a cluster lock is
 * successfully acquired.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date           Ticket#      Engineer        Description
 * ------------   ----------   -----------     --------------------------
 * Sep 24, 2024   2037919      lisa.singh      Initial creation
 *
 * </pre>
 */
public class ClusteredCronConsumer extends SpringCronConsumer {

    private static final Logger logger = LoggerFactory
            .getLogger(ClusteredCronConsumer.class);

    private static final String TASK = "ClusteredCron";

    private final String schedule;

    public ClusteredCronConsumer(ClusteredCronEndpoint endpoint,
            Processor processor, String schedule) {
        super(endpoint, processor);
        this.schedule = schedule;
        logger.trace("Creating ClusteredCronConsumer with uri: {}",
                endpoint.getEndpointUri());
    }

    @Override
    protected int poll() throws Exception {
        String jobName = getEndpoint().getEndpointUri()
                + ClusterLockUtils.CLUSTER_SUFFIX;

        // Calculate an arbitrary interval that's neither too long nor too
        // short.
        long lockTimeoutOverride = calculateInterval(this.schedule) / 2;

        logger.debug(
                "poll() called  for clustered cron endpoint: {} with timeout: {}",
                getEndpoint().getEndpointUri(), lockTimeoutOverride);

        // Try to acquire the cluster lock
        ClusterTask lockTask = ClusterLockUtils.lock(TASK, jobName,
                lockTimeoutOverride, false);

        if (lockTask != null
                && lockTask.getLockState() == LockState.SUCCESSFUL) {
            return super.poll();
        } else {
            logger.debug("Failed to acquire cluster lock. Lock state: "
                    + (lockTask != null ? lockTask.getLockState() : "null"));
            return 0;
        }
    }

    /**
     * Calculate the interval of the cron expression
     *
     * @param cronExpression
     * @return the interval in milliseconds.
     */
    private long calculateInterval(String cronExpression) {
        CronExpression cron = CronExpression.parse(cronExpression);

        ZonedDateTime now = ZonedDateTime.now();

        // Get the next two execution times
        ZonedDateTime nextExecutionTime = cron.next(now);
        ZonedDateTime secondExecutionTime = cron.next(nextExecutionTime);

        // Calculate the interval between the two execution times
        return secondExecutionTime.toInstant().toEpochMilli()
                - nextExecutionTime.toInstant().toEpochMilli();
    }

}