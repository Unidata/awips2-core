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

package com.raytheon.uf.edex.distribution;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "distribution-spring.xml", context "distro"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-08-12   2037702    lisa.singh   Initial creation (from auto-generated)
 *
 * </pre>
 */


public class DistroRoutes extends EDEXRouteBuilder {

    private final String distributionCron;

    public DistroRoutes(String distributionCron) {
        this.distributionCron = distributionCron;
    }

    @Override
    public void configure() throws Exception {
        from("jms-durable:queue:external.dropbox?concurrentConsumers=5&maxConcurrentConsumers=5")
          .doTry()
              .bean("distributionSrv", "route")
          .doCatch(Throwable.class)
              .to("log:distribution?level=ERROR")
          .endDoTry()
          .end()
          .setId("distribution");
        from("jms-durable:queue:handleoup.dropbox")
          .doTry()
              .bean("handleoupDistributionSrv", "route")
          .doCatch(Throwable.class)
              .to("log:distribution?level=ERROR")
          .endDoTry()
          .end()
          .setId("handleoupDistribution");
        from("jms-durable:queue:radarserver.dropbox")
          .doTry()
              .bean("radarserverDistributionSrv", "route")
          .doCatch(Throwable.class)
              .to("log:distribution?level=ERROR")
          .endDoTry()
          .end()
          .setId("radarserverDistribution");
        from("quartz://refreshDist/refreshDistRoute/?cron=" + this.distributionCron)
          .doTry()
              .bean("distributionPatterns", "refresh")
          .doCatch(Throwable.class)
              .to("log:refreshDistribution?level=ERROR")
          .endDoTry()
          .end()
          .setId("refreshDistributionPatterns");
    }
}
