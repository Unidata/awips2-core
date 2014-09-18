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
package com.raytheon.uf.common.status.logback;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

/**
 * Logback {@code Appender} implementation that prevents log spam by suppressing
 * messages that are quickly repeated over a configurable interval. This
 * appender requires being attached to another appender for proper operations.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 15, 2014  #3633     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

public class SuppressingAppender extends
        UnsynchronizedAppenderBase<ILoggingEvent> implements
        AppenderAttachable<ILoggingEvent> {

    private static final class MessageCacheEntry {

        private int count;

        private long timeStamp;

        private final ILoggingEvent logEvent;

        public MessageCacheEntry(final ILoggingEvent logEvent) {
            this.count = 0;
            this.logEvent = logEvent;
            this.timeStamp = logEvent.getTimeStamp();
        }

        public int getCount() {
            return count;
        }

        public long getTimeStamp() {
            return timeStamp;
        }

        public ILoggingEvent getLogEvent() {
            return logEvent;
        }

        public int incrementCount() {
            return ++count;
        }

        public void setTimestamp(long newTimestamp) {
            timeStamp = newTimestamp;
        }
    }

    private final class LRUMessageCache extends
            LinkedHashMap<String, MessageCacheEntry> {

        private static final long serialVersionUID = -4498274196005049671L;

        private final int cacheSize;

        private final long expirationInterval;

        private final int duplicateThreshold;

        private final long loggingInterval;

        public LRUMessageCache(final int cacheSize,
                final long expirationInterval, final int duplicateThreshold,
                final long loggingInterval) {
            super(cacheSize, 1f, true);
            if (cacheSize < 1) {
                throw new IllegalArgumentException(
                        "Cache size cannot be smaller than 1");
            }
            this.cacheSize = cacheSize;
            this.expirationInterval = expirationInterval;
            this.duplicateThreshold = duplicateThreshold;
            this.loggingInterval = loggingInterval;
        }

        public int putAndGetMessageCount(ILoggingEvent event) {
            if (event == null) {
                return 0;
            }

            int count = 0;
            synchronized (this) {
                String cacheKey = event.getFormattedMessage();
                MessageCacheEntry cacheEntry = get(cacheKey);

                if (cacheEntry != null) {
                    long eventTime = event.getTimeStamp();
                    long timeSinceLast = eventTime - cacheEntry.getTimeStamp();

                    if (timeSinceLast <= expirationInterval) {
                        cacheEntry.setTimestamp(eventTime);

                        long eventLength = eventTime
                                - cacheEntry.getLogEvent().getTimeStamp();
                        if (eventLength >= loggingInterval) {
                            cacheEntry = resetCacheEntry(event, cacheKey);
                        }
                    } else {
                        cacheEntry = resetCacheEntry(event, cacheKey);
                    }
                } else {
                    cacheEntry = new MessageCacheEntry(event);
                }

                count = cacheEntry.incrementCount();
                put(cacheKey, cacheEntry);
            }

            return count;
        }

        private MessageCacheEntry resetCacheEntry(ILoggingEvent event,
                String cacheKey) {
            remove(cacheKey);
            return new MessageCacheEntry(event);
        }

        @Override
        public MessageCacheEntry remove(Object key) {
            MessageCacheEntry retVal;
            synchronized (this) {
                retVal = super.remove(key);
            }

            logCacheItem(retVal);

            return retVal;
        }

        private void logCacheItem(MessageCacheEntry cacheEntry) {
            int msgCount = cacheEntry.getCount();
            if (msgCount > duplicateThreshold) {
                ILoggingEvent srcEvent = cacheEntry.getLogEvent();
                long duration = cacheEntry.getTimeStamp()
                        - srcEvent.getTimeStamp();

                ILoggingEvent wrappedEvent = new SuppressedLogEvent(srcEvent,
                        msgCount, duration);
                log(wrappedEvent);
            }
        }

        @Override
        protected boolean removeEldestEntry(
                Entry<String, MessageCacheEntry> eldest) {
            long timeSinceLastLogged = System.currentTimeMillis()
                    - eldest.getValue().getTimeStamp();
            boolean removeEntry = (timeSinceLastLogged > expirationInterval)
                    || (size() > cacheSize);

            logCacheItem(eldest.getValue());

            return removeEntry;
        }

        @Override
        public synchronized void clear() {
            for (MessageCacheEntry cacheEntry : values()) {
                logCacheItem(cacheEntry);
            }
            super.clear();
        }
    }

    private static final class SuppressedLogEvent implements ILoggingEvent {

        private final ILoggingEvent event;

        private final int count;

        private final long duration;

        public SuppressedLogEvent(final ILoggingEvent event, final int count,
                final long duration) {
            this.event = event;
            this.count = count;
            this.duration = duration;
        }

        private String buildMessagePreamble() {
            return "Received " + count + " duplicate messages in " + duration
                    + "ms for message: ";
        }

        @Override
        public String getThreadName() {
            return event.getThreadName();
        }

        @Override
        public Level getLevel() {
            return event.getLevel();
        }

        @Override
        public String getMessage() {
            return buildMessagePreamble() + event.getMessage();
        }

        @Override
        public Object[] getArgumentArray() {
            return event.getArgumentArray();
        }

        @Override
        public String getFormattedMessage() {
            return buildMessagePreamble() + event.getFormattedMessage();
        }

        @Override
        public String getLoggerName() {
            return event.getLoggerName();
        }

        @Override
        public LoggerContextVO getLoggerContextVO() {
            return event.getLoggerContextVO();
        }

        @Override
        public IThrowableProxy getThrowableProxy() {
            return event.getThrowableProxy();
        }

        @Override
        public StackTraceElement[] getCallerData() {
            return event.getCallerData();
        }

        @Override
        public boolean hasCallerData() {
            return event.hasCallerData();
        }

        @Override
        public Marker getMarker() {
            return event.getMarker();
        }

        @Override
        public Map<String, String> getMDCPropertyMap() {
            return event.getMDCPropertyMap();
        }

        @Override
        public Map<String, String> getMdc() {
            return event.getMDCPropertyMap();
        }

        @Override
        public long getTimeStamp() {
            return System.currentTimeMillis();
        }

        @Override
        public void prepareForDeferredProcessing() {
            event.prepareForDeferredProcessing();
        }
    }

    private static final int DEFAULT_DUPLICATES_ALLOWED = 1;

    private static final int DEFAULT_CACHE_SIZE = 10;

    private static final long DEFAULT_TIME_THRESHOLD = 1000L; // in ms

    private static final long DEFAULT_LOGGING_INTERVAL = 5000L; // in ms

    private final AppenderAttachable<ILoggingEvent> attachableDelegate;

    private int appenderCount;

    /**
     * Number of unique messages to track for duplicates.
     */
    private int cacheSize;

    /**
     * Number of messages that can be logged during the configured
     * "timeThreshold" before a given message is to be determined to be flooding
     * the logs and needs to be suppressed.
     */
    private int duplicateThreshold;

    /**
     * Time in milliseconds that determines how long a message is tracked.
     * Seeing the same message more than "duplicateThreshold" number of times
     * over this time period will cause suppression to occur.
     */
    private long timeThreshold;

    /**
     * Time in milliseconds that a message will be suppressed before logging
     * that suppression is in effect. In other words, if a message continues to
     * flood the logs for more than this amount of time, every
     * "suppressionLoggingInterval" number of ms a message will be logged that
     * suppression is in effect for a message.
     */
    private long suppressionLoggingInterval;

    private LRUMessageCache cache;

    public SuppressingAppender() {
        this.attachableDelegate = new AppenderAttachableImpl<ILoggingEvent>();
        this.appenderCount = 0;
        this.cacheSize = DEFAULT_CACHE_SIZE;
        this.duplicateThreshold = DEFAULT_DUPLICATES_ALLOWED;
        this.timeThreshold = DEFAULT_TIME_THRESHOLD;
        this.suppressionLoggingInterval = DEFAULT_LOGGING_INTERVAL;
    }

    @Override
    public void start() {
        if (appenderCount == 0) {
            addError("No attached appenders found.");
            return;
        }

        if (cacheSize < 1) {
            addError("Cache size for SuppressingAppender must be at least 1.");
            return;
        }

        if (duplicateThreshold < 1) {
            addError("Duplicate threshold for SuppressingAppender must be at least 1.");
            return;
        }

        if (timeThreshold < 1) {
            addError("Time threshold for SuppressingAppender must be at least 1.");
            return;
        }

        if (suppressionLoggingInterval < 1) {
            addError("Logging interval for SuppressingAppender must be at least 1.");
            return;
        }

        cache = new LRUMessageCache(cacheSize, timeThreshold,
                duplicateThreshold, suppressionLoggingInterval);

        super.start();
    }

    @Override
    public void stop() {
        /*
         * Flush all remaining unlogged items from the cache, because this
         * appender is shutting down...
         */
        cache.clear();
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (cache.putAndGetMessageCount(eventObject) <= duplicateThreshold) {
            log(eventObject);
        }
    }

    private void log(ILoggingEvent eventObject) {
        for (Iterator<Appender<ILoggingEvent>> it = attachableDelegate
                .iteratorForAppenders(); it.hasNext();) {
            it.next().doAppend(eventObject);
        }
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getDuplicateThreshold() {
        return duplicateThreshold;
    }

    public void setDuplicateThreshold(int duplicateThreshold) {
        this.duplicateThreshold = duplicateThreshold;
    }

    public long getTimeThreshold() {
        return timeThreshold;
    }

    public void setTimeThreshold(long timeThreshold) {
        this.timeThreshold = timeThreshold;
    }

    public long getSuppressionLoggingInterval() {
        return suppressionLoggingInterval;
    }

    public void setSuppressionLoggingInterval(long suppressionLoggingInterval) {
        this.suppressionLoggingInterval = suppressionLoggingInterval;
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        if (appenderCount == 0) {
            appenderCount++;
            addInfo("Attaching appender named [" + newAppender.getName()
                    + "] to SuppressingAppender.");
            attachableDelegate.addAppender(newAppender);
        } else {
            addWarn("One and only one appender may be attached to SuppressingAppender.");
            addWarn("Ignoring additional appender named ["
                    + newAppender.getName() + "]");
        }
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return attachableDelegate.iteratorForAppenders();
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return attachableDelegate.getAppender(name);
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        return attachableDelegate.isAttached(appender);
    }

    @Override
    public void detachAndStopAllAppenders() {
        attachableDelegate.detachAndStopAllAppenders();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        return attachableDelegate.detachAppender(appender);
    }

    @Override
    public boolean detachAppender(String name) {
        return attachableDelegate.detachAppender(name);
    }
}
