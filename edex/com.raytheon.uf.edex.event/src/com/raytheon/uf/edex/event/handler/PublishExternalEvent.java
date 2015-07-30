package com.raytheon.uf.edex.event.handler;

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

import com.raytheon.uf.common.event.Event;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * 
 * Publish External events
 * All EDEX Events going outside their JVM
 * must go through this Class.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * June 25, 2014 2760      dhladky     Initial creation
 * May 14, 2015  4493      dhladky     General method of external event publishing.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class PublishExternalEvent {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PublishExternalEvent.class);

    /** static instance **/
    public static PublishExternalEvent instance = new PublishExternalEvent();

    /** URI to send events to **/
    private String externalUri = null;
    
    /** Property name of external URI in eventbus.properties file */
    private static final String EXTERNAL_URI_PROPERTY = "event.external.publish.uri";

    private PublishExternalEvent() {

        try {
            externalUri = System.getProperty(EXTERNAL_URI_PROPERTY);
        } catch (Exception e) {
            statusHandler.error(
                    "Unable to read property: "+EXTERNAL_URI_PROPERTY, e);
        }
        
        statusHandler.info("Publishing externally marked events to URI: "
                + externalUri);
    }

    /**
     * Static for speed
     * 
     * @return
     */
    public static PublishExternalEvent getInstance() {
        
        return instance;
    }

    /**
     * Publish registry event to topic.
     * 
     * @param event
     */
    public void publish(Event event) {

        if (event != null) {
            try {
                if (externalUri != null) {
                    if (event.isExternal()) {
                        /**
                         * This may seem rather odd since we are externally
                         * publishing the event. It's done for a very good
                         * reason though. It Prevents re-publishing the event
                         * back to JMS at the receiving end. Many times Events
                         * are dropped back into the EDEX Event System upon
                         * delivery. The Guava Event Bus is an example of this.
                         * If the event is marked "External", it will get
                         * published back to the JMS endpoint in a viscous cycle
                         * ending up never being delivered. The "external"
                         * marker is only meant to flag the first hop
                         * essentially.
                         */
                        event.setExternal(false);
                    }

                    byte[] bytes = SerializationUtil.transformToThrift(event);
                    EDEXUtil.getMessageProducer().sendAsyncUri(externalUri,
                            bytes);
                }
            } catch (Exception e) {
                statusHandler.error("Unable to publish event " + event.getId()
                        + " to URI: " + externalUri, e);
            }
        } 
    }
}
