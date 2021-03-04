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
package com.raytheon.uf.edex.esb.camel.cluster.quartz;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.camel.component.quartz.QuartzComponent;
import org.apache.camel.component.quartz.QuartzEndpoint;
import org.apache.camel.component.quartz.QuartzHelper;
import org.apache.camel.util.ObjectHelper;
import org.apache.camel.util.PropertiesHelper;
import org.apache.camel.util.StringHelper;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.listeners.TriggerListenerSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.edex.database.cluster.ClusterLockUtils;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils.LockState;
import com.raytheon.uf.edex.database.cluster.ClusterTask;

/**
 * Replacement for QuartzComponent that vetoes execution of the job if unable to
 * acquire a cluster lock when the job would be executed.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 22, 2010            njensen     Initial creation
 * Feb 22, 2021            tgurney     Rewrite for Camel 3
 *
 * </pre>
 *
 * @author njensen
 */

public class ClusteredQuartzComponent extends QuartzComponent {

    private static final Logger logger = LoggerFactory
            .getLogger(ClusteredQuartzComponent.class);

    private static class ClusteredTriggerListener
            extends TriggerListenerSupport {

        private static final String TASK = "ClusteredQuartz";

        private final String uri;

        public ClusteredTriggerListener(String uri) {
            this.uri = uri;
        }

        @Override
        public String getName() {
            return uri;
        }

        @Override
        public boolean vetoJobExecution(Trigger trigger,
                JobExecutionContext context) {
            String jName = uri + ClusterLockUtils.CLUSTER_SUFFIX;
            long period = Math.abs(context.getFireTime().getTime()
                    - context.getNextFireTime().getTime()) / 2;
            ClusterTask ct = ClusterLockUtils.lock(TASK, jName, period, false);

            boolean veto = !LockState.SUCCESSFUL.equals(ct.getLockState());

            if (veto && logger.isDebugEnabled()) {
                logger.debug("Vetoing execution of job " + jName
                        + " due to failure to acquire cluster lock (Lock state: "
                        + ct.getLockState() + ")");
            }

            return veto;
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    protected QuartzEndpoint createEndpoint(String uri, String remaining,
            Map parameters) throws Exception {
        QuartzEndpoint answer = new QuartzEndpoint(uri, this);

        TriggerKey triggerKey = createTriggerKey(uri);
        answer.setTriggerKey(triggerKey);

        getScheduler().getListenerManager().addTriggerListener(
                new ClusteredTriggerListener(uri),
                KeyMatcher.keyEquals(triggerKey));

        Map<String, Object> triggerParameters = PropertiesHelper
                .extractProperties(parameters, "trigger.");
        answer.setTriggerParameters(triggerParameters);

        Map<String, Object> jobParameters = PropertiesHelper
                .extractProperties(parameters, "job.");
        answer.setJobParameters(jobParameters);

        String cron = getAndRemoveParameter(parameters, "cron", String.class);

        // This matches the behavior of QuartzComponent.createEndpoint
        if (cron != null) {
            cron = cron.replace('+', ' ');
            answer.setCron(cron);
        }

        setProperties(answer, parameters);
        return answer;
    }

    private TriggerKey createTriggerKey(String uri) throws URISyntaxException {
        URI u = new URI(uri);
        String path = StringHelper.after(u.getPath(), "/");
        String host = u.getHost();

        // group can be optional, if so, set it to this context's unique name
        String name;
        String group;
        if (ObjectHelper.isNotEmpty(path) && ObjectHelper.isNotEmpty(host)) {
            group = host;
            name = path;
        } else {
            String camelContextName = QuartzHelper
                    .getQuartzContextName(getCamelContext());
            group = camelContextName == null ? "Camel"
                    : "Camel_" + camelContextName;
            name = host;
        }

        TriggerKey triggerKey = new TriggerKey(name, group);
        return triggerKey;
    }
}
