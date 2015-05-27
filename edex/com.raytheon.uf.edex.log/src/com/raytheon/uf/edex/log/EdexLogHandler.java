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
package com.raytheon.uf.edex.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.raytheon.uf.common.status.AbstractHandlerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.status.slf4j.Slf4JBridge;
import com.raytheon.uf.common.status.slf4j.UFMarkers;

/**
 * UFStatusHandler for EDEX
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 28, 2009            njensen     Initial creation
 * Jun 27, 2013 2142       njensen     Use SLF4J instead of log4j
 * May 27, 2015 4473       njensen     Refactored
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class EdexLogHandler implements IUFStatusHandler {

    private final Logger clazzLogger;

    private final AbstractHandlerFactory factory;

    private final String pluginId;

    private final String category;

    private String source;

    public EdexLogHandler(String clazz, String category, String source) {
        this.clazzLogger = LoggerFactory.getLogger(clazz);
        this.pluginId = clazz;
        this.category = category;
        this.source = source;
        this.factory = null;
    }

    public EdexLogHandler(AbstractHandlerFactory factory, String pluginId,
            String category) {
        this.clazzLogger = LoggerFactory.getLogger(pluginId);
        this.pluginId = pluginId;
        this.category = category;
        this.factory = null;
    }

    @Override
    public boolean isPriorityEnabled(Priority p) {
        switch (p) {
        case CRITICAL:
            return true;
        case SIGNIFICANT:
            return clazzLogger.isErrorEnabled();
        case PROBLEM:
            return clazzLogger.isWarnEnabled();
        case EVENTA: // fall through
        case EVENTB:
            return clazzLogger.isInfoEnabled();
        case VERBOSE:
            return clazzLogger.isDebugEnabled();
        default:
            return true;
        }
    }

    @Override
    public void handle(UFStatus status) {
        handle(status, this.category);
    }

    @Override
    public void handle(UFStatus status, String category) {
        handle(status.getPriority(), category, status.getMessage(),
                status.getException());
    }

    private String getSource() {
        if (this.source == null) {
            if (factory != null) {
                this.source = factory.getSource(source, pluginId);
            }
        }
        return this.source;
    }

    @Override
    public void handle(Priority p, String msg) {
        handle(p, this.category, msg);
    }

    @Override
    public void handle(Priority p, String category, String msg) {
        handle(p, category, msg, null);
    }

    @Override
    public void handle(Priority p, String msg, Throwable t) {
        handle(p, category, msg, t);
    }

    @Override
    public void handle(Priority p, String category, String msg, Throwable t) {
        /*
         * msg can be null if someone does e.getLocalizedMessage() and it is
         * null which causes null pointer exception
         */
        msg = String.valueOf(msg);
        // detached ensures we will get a new instance
        Marker marker = MarkerFactory.getDetachedMarker("edex");
        if (category != null) {
            StringBuilder sb = new StringBuilder(msg.length() + 64);
            sb.append(category);
            marker.add(UFMarkers.getCategoryMarker(category));

            String source = getSource();
            if (source != null) {
                sb.append(": ");
                sb.append(source);
                marker.add(UFMarkers.getSourceMarker(source));
            }

            sb.append(" - ");
            sb.append(msg);
            msg = sb.toString();
        }
        marker.add(UFMarkers.getUFPriorityMarker(p));

        Slf4JBridge.logToSLF4J(clazzLogger, p, marker, msg, t);
    }

    @Override
    public void debug(String message) {
        if (this.clazzLogger.isDebugEnabled()) {
            handle(Priority.DEBUG, message);
        }
    }

    @Override
    public void debug(String category, String message) {
        if (this.clazzLogger.isDebugEnabled()) {
            handle(Priority.DEBUG, category, message);
        }
    }

    @Override
    public void info(String message) {
        handle(Priority.INFO, message);
    }

    @Override
    public void info(String category, String message) {
        handle(Priority.INFO, category, message);
    }

    @Override
    public void warn(String message) {
        handle(Priority.WARN, message);
    }

    @Override
    public void warn(String category, String message) {
        handle(Priority.WARN, category, message);
    }

    @Override
    public void error(String message) {
        handle(Priority.ERROR, message);
    }

    @Override
    public void error(String category, String message) {
        handle(Priority.ERROR, category, message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        handle(Priority.ERROR, message, throwable);
    }

    @Override
    public void error(String category, String message, Throwable throwable) {
        handle(Priority.ERROR, category, message, throwable);
    }

    @Override
    public void fatal(String message, Throwable throwable) {
        handle(Priority.FATAL, message, throwable);
    }

    @Override
    public void fatal(String category, String message, Throwable throwable) {
        handle(Priority.FATAL, category, message, throwable);
    }

}
