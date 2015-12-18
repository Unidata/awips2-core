package com.raytheon.uf.edex.event.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.Subscribe;
import com.raytheon.uf.common.event.Event;

/**
 * 
 * Logs ALL events published on the event bus
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 01, 2012           jsanchez     Initial creation
 * Nov 05, 2012 1305      bgonzale     Added log level Event logging.
 * Feb 05, 2013 1580      mpduff       EventBus refactor.
 * Mar 13, 2013           bphillip     Modified to make event bus registration a post construct operation
 * Mar 27, 2013 1802      bphillip     Moved event bus registration from a PostConstruct method to Spring static method
 * Dec 17, 2015 5166      kbisanz      Update logging to use SLF4J, FATAL
 *                                     level uses SLF4J error() with marker
 * </pre>
 * 
 * @author jsanchez
 * @version 1.0
 */
public class LogHandler {

    private final Logger logger;

    private final Marker fatalMarker = MarkerFactory.getMarker("FATAL");

    /**
     * Creates a new object
     */
    public LogHandler() {
        logger = LoggerFactory.getLogger("Event");
    }

    /**
     * Listens for any DataDeliveryEvent object published on the event bus
     * 
     * @param event
     */
    @Subscribe
    @AllowConcurrentEvents
    public void eventListener(Event event) {
        switch (event.getLogLevel()) {
        case DEBUG:
            logger.debug(event.toString());
            break;
        case INFO:
            logger.info(event.toString());
            break;
        case WARN:
            logger.warn(event.toString());
            break;
        case ERROR:
            logger.error(event.toString());
            break;
        case FATAL:
            // SLF4J does not support a fatal log level. It does provide
            // the ability to provide a marker, however only logback
            // supports markers. See http://www.slf4j.org/faq.html#fatal
            logger.error(fatalMarker, event.toString());
            break;
        case TRACE:
            logger.trace(event.toString());
            break;
        default:
            // ALL
            // logger.(event.toString());
            break;
        }
    }
}
