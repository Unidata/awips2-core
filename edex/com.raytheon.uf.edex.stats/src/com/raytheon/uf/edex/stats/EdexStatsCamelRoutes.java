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

package com.raytheon.uf.edex.stats;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "stats-ingest.xml", context
 * "edexStats-camel"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2024-07-29   2037701    lisa.singh   Initial creation (from auto-generated)
 *
 * </pre>
 */

public class EdexStatsCamelRoutes extends EDEXRouteBuilder {

    private final String statsScanInterval;

    private final String statsAggregateToCsvCron;

    private final String statsPurgeCron;

    public EdexStatsCamelRoutes(String statsScanInterval,
            String statsAggregateToCsvCron, String statsPurgeCron) {
        this.statsScanInterval = statsScanInterval;
        this.statsAggregateToCsvCron = statsAggregateToCsvCron;
        this.statsPurgeCron = statsPurgeCron;
    }

    @Override
    public void configure() throws Exception {
        //@formatter:off
        from("timer://scanStats?period=" + this.statsScanInterval + "m")
          .doTry()
          .bean("aggregateManager", "scan")
          .doCatch(Throwable.class)
          .to("log:stats?level=ERROR")
          .endDoTry()
          .end()
          .setId("statsTableScan");
        from("cron:stats/aggrToCsv?schedule=" + this.statsAggregateToCsvCron)
          .doTry()
          .bean("aggregateManager", "offlineAggregates")
          .doCatch(Throwable.class)
          .to("log:stats?level=ERROR")
          .endDoTry()
          .end()
          .setId("statsAggrToCsv");
        from("cron:stats/purge?schedule=" + this.statsPurgeCron)
          .doTry()
          .bean("statsPurge", "purge")
          .doCatch(Throwable.class)
          .to("log:stats?level=ERROR")
          .endDoTry()
          .end()
          .setId("statsPurgeRoute");
        //@formatter:on
    }
}
