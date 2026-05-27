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

package com.raytheon.uf.edex.purgesrv;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "purge-spring.xml", context
 * "clusteredpurgeContext"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-08-21   2037701    lisa.singh   Initial creation (from auto-generated)
 *
 * </pre>
 */

// @formatter:off
/* Original XML context
   <camelContext id="clusteredpurgeContext" xmlns="http://camel.apache.org/schema/spring"
        errorHandlerRef="errorHandler">

        <endpoint id="purgeOutgoingCron" uri="quartz://purge/purgeOutgoingScheduled/?cron=${purge.outgoing.cron}"/>
        <endpoint id="purgeOrphanTimer" uri="timer://purgeOrphan?fixedRate=true&amp;period=${purge.orphan.period}"/>

        <route id="purgeByRequest">
            <from uri="jms-generic:queue:purgeRequest" />
            <doTry>
                <bean ref="purge" method="purge" />
                <doCatch>
                    <exception>java.lang.Throwable</exception>
                    <to
                        uri="log:purge?level=ERROR" />
                </doCatch>
            </doTry>
        </route>


        <!-- schedule the timer to purge outgoing directory -->
        <route id="purgeOutgoingScheduled">
            <from uri="purgeOutgoingCron" />
            <bean ref="purgeOutgoing" method="purge" />
        </route>

        <route id="purgeOrphanScheduled">
            <from uri="purgeOrphanTimer" />
            <doTry>
                <bean ref="purgeManager" method="purgeOrphanedData" />
                <doCatch>
                    <exception>java.lang.Throwable</exception>
                    <to
                        uri="log:purge?level=ERROR" />
                </doCatch>
            </doTry>
        </route>
    </camelContext>
 */
// @formatter:on

public class ClusteredPurgeContextRoutes extends EDEXRouteBuilder {

    private final String purgeOutgoingCron;

    private final String purgeOrphanPeriod;

    public ClusteredPurgeContextRoutes(String purgeOutgoingCron,
            String purgeOrphanPeriod) {
        this.purgeOutgoingCron = purgeOutgoingCron;
        this.purgeOrphanPeriod = purgeOrphanPeriod;
    }

    @Override
    public void configure() throws Exception {
        // @formatter:off
        from("jms-generic:queue:purgeRequest")
          .doTry()
              .bean("purge", "purge")
          .doCatch(Throwable.class)
              .to("log:purge?level=ERROR")
          .endDoTry()
          .end()
          .setId("purgeByRequest");

        from("cron:purge/purgeOutgoingScheduled?schedule=" + this.purgeOutgoingCron)
          .bean("purgeOutgoing", "purge")
          .setId("purgeOutgoingScheduled");

        from("timer://purgeOrphan?fixedRate=true&period=" + this.purgeOrphanPeriod)
          .doTry()
              .bean("purgeManager", "purgeOrphanedData")
          .doCatch(Throwable.class)
              .to("log:purge?level=ERROR")
          .endDoTry()
          .end()
          .setId("purgeOrphanScheduled");
        // @formatter:on
    }
}
