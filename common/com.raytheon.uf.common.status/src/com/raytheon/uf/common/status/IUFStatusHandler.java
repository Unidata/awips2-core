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
package com.raytheon.uf.common.status;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Describes a method of handling UFStatuses.
 * 
 * The following section is current as of June 2017. It should be updated if
 * AlertViz is replaced, former behavior is not a requirement, or logback is
 * replaced.
 * 
 * IUFStatusHandler usage is discouraged for EDEX classes. They provide nothing
 * useful over a standard SLF4J Logger and a standard {@link org.slf4j.Logger}
 * should be used instead.
 * 
 * For common classes, IUFStatusHandler is encouraged because the common class
 * may be used in the viz application. See the following paragraphs for more
 * information about logging in CAVE.
 * 
 * In viz classes (or common code running in viz), IUFStatusHandler is
 * encouraged because it behaves differently than SLF4J Loggers. Since Omaha
 * #4473, IUFStatusHandlers are sending code through
 * {@link com.raytheon.uf.common.status.slf4j.Slf4JBridge} which is using a
 * single static logger: CaveLogger. CaveLogger in logback config files has its
 * level set to ALL, meaning all levels will be sent through. Prior to #4473 viz
 * was hardcoded to send all messages to AlertViz, whereas now viz sends
 * messages to SLF4J which has a logback appender that is configured for sending
 * to AlertViz. Therefore, by using IUFStatusHandler the behavior of sending all
 * messages to AlertViz is retained.
 * 
 * In constrast, using an instance of {@link org.slf4j.Logger} in viz (or common
 * code running in viz) skips Slf4JBridge and will use the logback configuration
 * for that particular logger, which is most likely set at a higher level than
 * DEBUG or TRACE, i.e. most loggers are typically set to INFO, WARN, or ERROR
 * levels. Therefore not all events will be recorded, and viz classes are
 * therefore encouraged to use IUFStatusHandler over SLF4J Loggers. Exceptions
 * to this guideline can be made when the message levels in viz or common code
 * should be configurable through the logback config files. For example you may
 * not want specific debug messages going through to AlertViz.
 * 
 * In viz, logging appearance and behavior will potentially be different if a
 * logger is switched from IUFStatusHandler to an SLF4J Logger. UFStatus has
 * confusing legacy concepts of source and category and confusing legacy logging
 * priorities that do not match standard systems. Developers' usage of legacy
 * source, category, and priority is inconsistent and potentially incorrect due
 * to its inherently confusing nature (see {@link UFStatus}). Caution should be
 * exercised if switching from an IUFStatusHandler to an SLF4J Logger wherever
 * legacy source, category, or priority are used.
 * 
 * SLF4J can somewhat accommodate the legacy source, category, and priority
 * through the usage of Markers. For an example see Slf4JBridge and
 * StatusMessageAppender.
 * 
 * TODO: We could solve the discrepancy in behavior of which messages go through
 * to AlertViz by having the root logger for viz have the level of ALL and
 * recording all messages. However this partially defeats the purpose of having
 * different levels if all logging events are always enabled. Regardless, even
 * if all levels are enabled, different logging levels could prove useful when
 * developers maintain code, when developers grep logs, and if AlertViz is ever
 * replaced.
 * 
 * TODO: We should use Markers to distinguish different types of events. Some
 * events are performance events, some events should be displayed to the user,
 * some events are config mistakes, some events are errors we can recover from
 * and should never be shown to the user, etc.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 06, 2008  1433      chammack    Initial creation
 * Jun 14, 2017  6316      njensen     Added methods to match logging APIs
 *                                     Added very detailed class javadoc
 * 
 * </pre>
 * 
 * @author chammack
 */
public interface IUFStatusHandler {
    /**
     * 
     * @param p
     * @return
     */
    public boolean isPriorityEnabled(Priority p);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param status
     */
    public void handle(UFStatus status);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param status
     */
    public void handle(UFStatus status, String category);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param priority
     *            Message priority.
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public void handle(Priority priority, String message);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param priority
     *            Message priority.
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public void handle(Priority priority, String category, String message);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param priority
     *            Message priority.
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public void handle(Priority priority, String message, Throwable throwable);

    /**
     * Send a message to Status handler for logging/display.
     * 
     * @param priority
     *            Message priority.
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public void handle(Priority priority, String category, String message,
            Throwable throwable);

    /**
     * Send a debug message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void debug(String message) {
        handle(Priority.DEBUG, message);
    }

    /**
     * Send a debug message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void debug(String category, String message) {
        handle(Priority.DEBUG, category, message);
    }

    /**
     * Send a debug message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void debug(String message, Throwable throwable) {
        handle(Priority.DEBUG, message, throwable);
    }

    /**
     * Send a debug message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void debug(String category, String message,
            Throwable throwable) {
        handle(Priority.DEBUG, category, message, throwable);
    }

    /**
     * Send an info message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void info(String message) {
        handle(Priority.INFO, message);
    }

    /**
     * Send an info message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void info(String category, String message) {
        handle(Priority.INFO, category, message);
    }

    /**
     * Send an info message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void info(String message, Throwable throwable) {
        handle(Priority.INFO, message, throwable);
    }

    /**
     * Send an info message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void info(String category, String message,
            Throwable throwable) {
        handle(Priority.INFO, category, message, throwable);
    }

    /**
     * Send a warn message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void warn(String message) {
        handle(Priority.WARN, message);
    }

    /**
     * Send a warn message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void warn(String category, String message) {
        handle(Priority.WARN, category, message);
    }

    /**
     * Send a warn message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void warn(String message, Throwable throwable) {
        handle(Priority.WARN, message, throwable);
    }

    /**
     * Send a warn message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void warn(String category, String message,
            Throwable throwable) {
        handle(Priority.WARN, category, message, throwable);
    }

    /**
     * Send an error message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void error(String message) {
        handle(Priority.ERROR, message);
    }

    /**
     * Send an error message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void error(String category, String message) {
        handle(Priority.ERROR, category, message);
    }

    /**
     * Send an error message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void error(String message, Throwable throwable) {
        handle(Priority.ERROR, message, throwable);
    }

    /**
     * Send an error message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */

    public default void error(String category, String message,
            Throwable throwable) {
        handle(Priority.ERROR, category, message, throwable);
    }

    /**
     * Send a fatal message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void fatal(String message) {
        handle(Priority.FATAL, message);
    }

    /**
     * Send a fatal message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * 
     * @see UFStatus
     */
    public default void fatal(String category, String message) {
        handle(Priority.FATAL, category, message);
    }

    /**
     * Send a fatal message to Status handler for logging/display.
     * 
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void fatal(String message, Throwable throwable) {
        handle(Priority.FATAL, message, throwable);
    }

    /**
     * Send a fatal message to Status handler for logging/display.
     * 
     * @param category
     *            Message category
     * @param message
     *            Text to be displayed in the message
     * @param throwable
     *            Associated throwable
     * 
     * @see UFStatus
     */
    public default void fatal(String category, String message,
            Throwable throwable) {
        handle(Priority.FATAL, category, message, throwable);
    }
}
