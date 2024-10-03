package com.raytheon.uf.edex.esb.camel.cluster.cron;

import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.component.cron.SpringCronEndpoint;
import org.apache.camel.component.cron.api.CamelCronConfiguration;

/**
 * The clustered cron endpoint that handles job scheduling and ensures that jobs
 * only execute if the cluster lock is acquired.
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
class ClusteredCronEndpoint extends SpringCronEndpoint {

    private CamelCronConfiguration configuration;

    public ClusteredCronEndpoint(String endpointUri,
            ClusteredCronComponent component,
            CamelCronConfiguration configuration) {
        super(endpointUri, component);

        this.configuration = configuration;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        ClusteredCronConsumer consumer = new ClusteredCronConsumer(this,
                processor, this.configuration.getSchedule());
        configureConsumer(consumer);
        return consumer;
    }

}