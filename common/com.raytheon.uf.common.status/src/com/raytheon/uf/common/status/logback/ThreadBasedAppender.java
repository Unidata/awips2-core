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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.AppenderAttachable;

/**
 * Appender for logging based on the thread name of the logging event. Since
 * spring can register patterns, the registration is kept at the class level.
 * Also, if the configuration is changed the class will be recreated with the
 * new configuration. NOTE: This appender does not support more than one
 * instance being run at once.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 25, 2010            rjpeter     Initial creation
 * Jun 24, 2013 2142       njensen     Changes for logback compatibility
 * Apr 29, 2014 3114       rjpeter     Make plugin contributable.
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */

public class ThreadBasedAppender extends AppenderBase<ILoggingEvent> implements
        AppenderAttachable<ILoggingEvent> {
    private static final Pattern NAME_REPLACE_PATTERN = Pattern
            .compile("%s\\{name\\}");

    /**
     * Current instance of the ThreadBasedAppender.
     */
    private static volatile ThreadBasedAppender instance;

    /**
     * Appenders that were created via plugins and registered.
     */
    private static final ConcurrentMap<String, Appender<ILoggingEvent>> registeredAppenderMap = new ConcurrentHashMap<String, Appender<ILoggingEvent>>();

    /**
     * Thread patterns that were registered.
     */
    private static final ConcurrentMap<Pattern, String> threadPatterns = new ConcurrentHashMap<Pattern, String>();

    /**
     * Default pattern layout.
     */
    private String patternLayout = "%-5p %d [%t] %c{0}: %m%n";

    /**
     * Default max history.
     */
    private int maxHistory = 30;

    private String fileNameBase = "${edex.home}/logs/edex-${edex.run.mode}-%s{name}-%d{yyyyMMdd}.log";

    private String defaultAppenderName;

    private final ThreadLocal<Appender<ILoggingEvent>> threadAppender = new ThreadLocal<Appender<ILoggingEvent>>();

    private final ThreadLocal<String> threadNameCache = new ThreadLocal<String>();

    private final ConcurrentMap<String, Appender<ILoggingEvent>> appenderMap = new ConcurrentHashMap<String, Appender<ILoggingEvent>>();

    private volatile Appender<ILoggingEvent> defaultAppender;

    public ThreadBasedAppender() {
        synchronized (ThreadBasedAppender.class) {
            if (instance == null) {
                instance = this;
            } else {
                throw new AssertionError(
                        "ThreadBasedAppender already in use.  Cannot start another.");
            }
        }
    }

    @Override
    public void start() {
        if (defaultAppenderName != null) {
            defaultAppender = getAppender(defaultAppenderName);
        }

        appenderMap.putAll(registeredAppenderMap);

        super.start();
    }

    @Override
    public void addAppender(Appender<ILoggingEvent> newAppender) {
        if ((newAppender != null) && (newAppender.getName() != null)) {
            appenderMap.put(newAppender.getName(), newAppender);
        }
    }

    @Override
    public Appender<ILoggingEvent> getAppender(String name) {
        return getAppender(name, false);
    }

    /**
     * Look up the appender by name. If create is true and appender is not
     * currently known, an appender will be created using the default values of
     * this appender.
     * 
     * @param name
     * @param create
     * @return
     */
    public Appender<ILoggingEvent> getAppender(String name, boolean create) {
        if (name != null) {
            synchronized (this) {
                Appender<ILoggingEvent> rval = appenderMap.get(name);
                if ((rval == null) && create) {
                    PatternLayoutEncoder encoder = new PatternLayoutEncoder();
                    encoder.setContext(instance.getContext());
                    encoder.setPattern(patternLayout);
                    encoder.start();

                    TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<ILoggingEvent>();
                    policy.setContext(instance.getContext());
                    policy.setMaxHistory(maxHistory);
                    Matcher matcher = NAME_REPLACE_PATTERN
                            .matcher(fileNameBase);
                    String filePattern = matcher.replaceAll(name);
                    policy.setFileNamePattern(filePattern);

                    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<ILoggingEvent>();
                    appender.setContext(instance.getContext());
                    appender.setName(name);
                    appender.setEncoder(encoder);
                    policy.setParent(appender);
                    appender.setRollingPolicy(policy);
                    policy.start();
                    appender.start();
                    rval = appender;
                    appenderMap.put(name, rval);
                }

                return rval;
            }
        }

        return null;
    }

    @Override
    public boolean isAttached(Appender<ILoggingEvent> appender) {
        if (appender != null) {
            return appenderMap.containsKey(appender.getName());
        }

        return false;
    }

    @Override
    public void detachAndStopAllAppenders() {
        /*
         * keep the plugin registered appenders, they cannot be updated via
         * configuration.
         */
        appenderMap.keySet().removeAll(registeredAppenderMap.keySet());

        for (Appender<ILoggingEvent> app : appenderMap.values()) {
            app.stop();
        }

        appenderMap.clear();
    }

    @Override
    public boolean detachAppender(Appender<ILoggingEvent> appender) {
        boolean retVal = false;
        if (appender != null) {
            retVal = detachAppender(appender.getName());
        }
        return retVal;
    }

    @Override
    public boolean detachAppender(String name) {
        boolean retVal = false;
        if (name != null) {
            Appender<ILoggingEvent> app = appenderMap.remove(name);
            if (app != null) {
                retVal = true;
            }
        }

        return retVal;
    }

    @Override
    protected void append(ILoggingEvent event) {
        String threadName = event.getThreadName();
        String currentThreadName = Thread.currentThread().getName();
        Appender<ILoggingEvent> app = null;
        boolean sameThread = currentThreadName.equals(threadName);

        if (sameThread) {
            // Double check someone hasn't called setThreadName
            String prevThreadName = threadNameCache.get();
            if (currentThreadName.equals(prevThreadName)) {
                app = threadAppender.get();
            }
        }

        if (app == null) {
            // determine which appender to use
            for (Entry<Pattern, String> entry : threadPatterns.entrySet()) {
                Pattern pat = entry.getKey();
                if (pat.matcher(threadName).matches()) {
                    String name = entry.getValue();
                    app = getAppender(name, true);
                    break;
                }
            }

            if ((app == null) && (defaultAppender != null)) {
                app = defaultAppender;
            }

            if ((app != null) && sameThread) {
                threadAppender.set(app);
                threadNameCache.set(currentThreadName);
            }
        }

        if (app != null) {
            app.doAppend(event);
        }
    }

    @Override
    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
        return appenderMap.values().iterator();
    }

    public static String registerThreadPattern(String appenderName,
            String pattern) {
        threadPatterns.put(Pattern.compile(pattern), appenderName);
        return appenderName;
    }

    public static Appender<ILoggingEvent> registerAppenderPattern(
            Appender<ILoggingEvent> appender, String pattern) {
        if (appender != null) {
            String name = appender.getName();
            threadPatterns.put(Pattern.compile(pattern), name);
            registeredAppenderMap.put(name, appender);

            synchronized (ThreadBasedAppender.class) {
                if (instance != null) {
                    instance.addAppender(appender);
                }
            }
        } else {
            throw new AssertionError("Cannot register a null appender");
        }

        return appender;
    }

    public String getPatternLayout() {
        return patternLayout;
    }

    public void setPatternLayout(String patternLayout) {
        this.patternLayout = patternLayout;
    }

    public int getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(int maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getFileNameBase() {
        return fileNameBase;
    }

    public void setFileNameBase(String fileNameBase) {
        this.fileNameBase = fileNameBase;
    }

    public String getDefaultAppenderName() {
        return defaultAppenderName;
    }

    public void setDefaultAppenderName(String defaultAppenderName) {
        this.defaultAppenderName = defaultAppenderName;
    }

    @Override
    public void stop() {
        synchronized (ThreadBasedAppender.class) {
            instance = null;
        }

        detachAndStopAllAppenders();
        super.stop();
    }
}
