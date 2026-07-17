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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.component.cron.CronComponent;
import org.apache.camel.component.cron.CronEndpoint;
import org.apache.camel.component.cron.api.CamelCronConfiguration;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultEndpoint;
import org.apache.camel.util.ObjectHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A clustered cron component that is called when the "clusteredcron" URI
 * camel endpoint is called. It is a copy of
 * {@link org.apache.camel.component.cron.CronComponent} but adds extra support
 * for the timeZone property in the endpoint URI. <BR/>
 * <BR/>
 * When a camel endpoint calls the clusteredcron endpoint, for example: <BR/>
 * {@code from("clusteredcron://textSubscription/purgeTextTriggerFiles/?schedule=" + this.purgeTextTriggerFilesCron)}
 * <BR>
 * ... this component is called. <BR/>
 * <BR/>
 * This component also has support for the timeZone property. For example: <BR/>
 * {@code
 * from("clusteredcron://cpg/autocreateclimeAM/?schedule=" + this.cpgAmCron +
 * "&timeZone={{climate.cpg.cron.timezone}}")} <BR/>
 * <BR/>
 * This component supports cluster-locking, so when the endpoint is called, it
 * first checks that this cron job hasn't already been created by another
 * instance of Edex. The cluster-locking logic is handled in
 * {@link ClusteredCronConsumer}.
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
@Component("clusteredcron")
public class ClusteredCronComponent extends CronComponent {
    private static final Logger logger = LoggerFactory
            .getLogger(ClusteredCronComponent.class);

    /**
     * This method creates an cron endpoint for the "clusteredcron" uri. It was
     * copied from
     * {@link org.apache.camel.component.cron.CronComponent#createEndpoint(String, String, Map)}
     * and modified to add support for the timeZone parameter.
     */
    @Override
    public Endpoint createEndpoint(String uri, String remaining,
            Map<String, Object> parameters) throws Exception {
        logger.debug("createEndpoint called for uri: {}", uri);

        TimeZonedCamelCronConfiguration configuration = new TimeZonedCamelCronConfiguration();
        configuration.setName(remaining);

        // special for schedule where we replace + as space
        String schedule = getAndRemoveParameter(parameters, "schedule",
                String.class);
        if (schedule != null) {
            // replace + as space
            schedule = schedule.replace('+', ' ');
        }
        configuration.setSchedule(schedule);

        // set the time zone
        String timeZone = getAndRemoveParameter(parameters, "timeZone",
                String.class);
        configuration.setTimeZone(timeZone);

        // This is the endpoint that is returned to Camel when a route is
        // created. Camel uses this to triggered the scheduled job. It binds
        // around the delegate endpoint, which is created further below.
        CronEndpoint answer = new CronEndpoint(uri, this, configuration);
        setProperties(answer, parameters);

        // validate configuration
        validate(configuration);

        // create delegate and set on endpoint
        // This is where the ClusteredCronEndpoint gets created, via the
        // service.
        Endpoint delegate = super.getService().createEndpoint(configuration);
        answer.setDelegate(delegate);
        if (delegate instanceof DefaultEndpoint) {
            DefaultEndpoint de = (DefaultEndpoint) delegate;
            de.setAutowiredEnabled(answer.isAutowiredEnabled());
            de.setBridgeErrorHandler(answer.isBridgeErrorHandler());
            de.setExceptionHandler(answer.getExceptionHandler());
            de.setExchangePattern(answer.getExchangePattern());
        }

        return answer;
    }

    /**
     * This method is an exact copy of CronComponent.validate(). It was
     * recreated because the original method was private and couldn't be reused
     * when overriding the method CronComponent.createEndpoint().
     * 
     * @param configuration
     */
    protected void validate(CamelCronConfiguration configuration) {
        ObjectHelper.notNull(configuration, "configuration");
        ObjectHelper.notNull(configuration.getName(), "name");
        ObjectHelper.notNull(configuration.getSchedule(), "schedule");

        String[] parts = configuration.getSchedule().split("\\s");
        if (parts.length < 5 || parts.length > 7) {
            throw new IllegalArgumentException(
                    "Invalid number of parts in cron expression. Expected 5 to 7, got: "
                            + parts.length);
        }
    }

}
