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

package com.raytheon.uf.edex.ingest;

import com.raytheon.uf.edex.routes.EDEXRouteBuilder;

/**
 * Camel routes converted from file "persist-ingest.xml", context "persist-camel"
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


public class PersistCamelRoutes extends EDEXRouteBuilder {

    public PersistCamelRoutes() {
    }

    @Override
    public void configure() throws Exception {
        /* Generic persist and indexing
           Intended for routes that need persisting to HDF5,
           Indexing but no alert processing */
        from("direct:persistIndex")
          .bean("persist", "persist")
          .bean("index", "index")
          .bean("processUtil", "log")
          .setId("persistIndex");
        
        /* Generic persist, index and alert route
           Intended for routes that need persisting to HDF5,
           Indexing and Alerting */
        from("direct:persistIndexAlert")
          .bean("persist", "persist")
          .bean("index", "index")
          .bean("processUtil", "log")
          .to("direct:stageNotification")
          .setId("persistIndexAlert");
        
        /* Generic index and alert route
           Intended for routes that need Indexing and Alerting */
        from("direct:indexAlert")
          .bean("index", "auditMissingPiecesForDatabaseOnlyPdos")
          .bean("index", "index")
          .bean("processUtil", "log")
          .to("direct:stageNotification")
          .setId("indexAlert");
        
        /* This route should come after all other routes in this context
           that send data to it for proper startup/shutdown order. */
        from("direct:stageNotification")
          .bean("pluginNotifier", "notifyRoutes")
          .setId("notificationAggregation");
        
        from("timer://notificationTimer?fixedRate=true&period=1000")
          .bean("pluginNotifier", "sendQueuedNotifications")
          .setId("notificationTimer");
        
        from("direct:logFailedData")
          .bean("processUtil", "logFailedData")
          .setId("logFailedData");
        
        from("direct:logFailureAsInfo")
          .bean("processUtil", "logFailureAsInfo")
          .setId("logFailureAsInfo");
        
        from("timer://reloadTimer?fixedRate=true&period=60s")
          .bean("pluginNotifier", "reloadConfigurations")
          .setId("reloadTimer");
    }
}
