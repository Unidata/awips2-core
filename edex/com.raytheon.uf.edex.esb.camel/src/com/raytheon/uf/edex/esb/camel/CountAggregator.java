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
package com.raytheon.uf.edex.esb.camel;

import org.apache.camel.AggregationStrategy;
import org.apache.camel.Exchange;
import org.apache.camel.Message;

/**
 *
 * Aggregates the results of PluginNotifier.notify
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 28, 2017 6130       tjensen     Initial creation
 * Mar  4, 2021 8326       tgurney     Fix import for Camel 3
 *
 * </pre>
 *
 * @author tjensen
 */
public class CountAggregator implements AggregationStrategy {

    @Override
    public Exchange aggregate(Exchange oldExchange, Exchange newExchange) {
        if (oldExchange == null) {
            return newExchange;
        }
        Message oldIn = oldExchange.getIn();
        if (oldIn == null) {
            return newExchange;
        }

        int oldBody = oldIn.getBody(Number.class).intValue();
        int newBody = 0;
        Message newIn = newExchange.getIn();
        if (newIn != null) {

            newBody = newIn.getBody(Number.class).intValue();
        }
        oldIn.setBody(oldBody + newBody);
        return oldExchange;
    }

}
