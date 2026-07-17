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

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.apache.camel.Endpoint;
import org.apache.camel.component.cron.CamelSpringCronService;
import org.apache.camel.component.cron.api.CamelCronConfiguration;

/**
 * The service to handle the "clusteredcron" camel endpoint. In a clustered
 * system where multiple instances of edex may be running, this service ensures
 * that only one instance of the given cron is running at a time.
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
public class ClusteredCronService extends CamelSpringCronService {

    @Override
    public Endpoint createEndpoint(CamelCronConfiguration configuration)
            throws Exception {
        ClusteredCronComponent cronComponent = getCamelContext()
                .getComponent("clusteredcron", ClusteredCronComponent.class);

        if (cronComponent == null) {
            throw new IllegalStateException(
                    "ClusteredCronComponent not found in Camel context");
        }

        // Create the URI
        String uri = "clusteredcron:" + configuration.getName();
        ClusteredCronEndpoint cronEndpoint = new ClusteredCronEndpoint(uri,
                cronComponent, configuration);

        // Add scheduler and cron configuration
        Map<String, Object> options = new HashMap<>();
        options.put("scheduler", "spring");
        options.put("scheduler.cron", configuration.getSchedule());
        
        // set the time zone if it is set
        if (configuration instanceof TimeZonedCamelCronConfiguration timeZonedConfig) {
            String timeZone = timeZonedConfig.getTimeZone();
            if (timeZone != null) {
                options.put("scheduler.timeZone",
                        TimeZone.getTimeZone(timeZone));
            }
        }

        cronEndpoint.configureProperties(options);

        return cronEndpoint;
    }

}
