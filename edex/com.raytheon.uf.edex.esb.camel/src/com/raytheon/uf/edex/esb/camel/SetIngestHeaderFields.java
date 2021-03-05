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

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds properties for instrumentation to ensure log messages contain all
 * information.
 *
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2008            chammack    Initial creation
 * Jul 15, 2010 6624       garmendariz Log error and interrupt if file missing
 * Dec 17, 2015 5166       kbisanz     Update logging to use SLF4J
 * Mar  4, 2021  8326      tgurney     Fix for Camel 3 removal of fault API
 *
 * </pre>
 *
 * @author chammack
 */

public class SetIngestHeaderFields implements Processor {

    protected transient Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void process(Exchange arg0) throws Exception {
        arg0.getIn().setHeader("dequeueTime", System.currentTimeMillis());
        Object payload = arg0.getIn().getBody();

        if (payload instanceof String) {
            String bodyString = (String) payload;
            File file = new File(bodyString);

            // if file does not exist, set fault to interrupt processing
            if (!file.exists()) {
                logger.error("File does not exist : " + bodyString);
                arg0.setRouteStop(true);
            } else {
                arg0.getIn().setHeader("ingestFileName", file.toString());
            }
        }
    }
}
