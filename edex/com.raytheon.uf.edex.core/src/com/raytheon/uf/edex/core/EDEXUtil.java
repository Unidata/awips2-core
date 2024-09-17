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

package com.raytheon.uf.edex.core;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.message.StatusMessage;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.edex.core.exception.ShutdownException;

/**
 * Contains utility methods for use by EDEX.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -------------------------------------------------------------
 * Apr 23, 2008  1088     chammack     Split from Util
 * Nov 22, 2010  2235     cjeanbap     Added audio file to StatusMessage.
 * Feb 02, 2011  6500     cjeanbap     Added paramter to method signature and properly assign source
 *                                     value.
 * Jun 12, 2012  609      djohnson     Use EDEXUtil for EDEX_HOME.
 * Mar 18, 2013  1802     bphillip     Added getList utility function
 * Apr 10, 2014  2726     rjpeter      Added methods for waiting for edex to be running.
 * Jun 25, 2014  3165     njensen      Remove dead code
 * Jul 16, 2014  2914     garmendariz  Remove EnvProperties
 * Jul 27, 2015  4654     skorolev     Added filters in sendMessageAlertViz
 * Dec 17, 2015  5166     kbisanz      Update logging to use SLF4J
 * Apr 25, 2016  5604     rjpeter      Updated checkPersistenceTimes to utilize same object for each
 *                                     call.
 * Apr 19, 2017  6187     njensen      Improved logging
 * Mar 20, 2018  7096     randerso     Remove call to StatusMessage.setEventTime()
 * Apr 21, 2021  7849     mapeters     Deprecate methods using static Spring context, add {@link
 *                                     #getESBComponent(ApplicationContext, String)}, remove
 *                                     IContextAdmin field
 * Sep 17, 2024  2037700  tgurney      Replace shutdown boolean flag with a latch
 *                                     to allow blocking wait for shutdown
 *
 * </pre>
 *
 * @author chammack
 */
public class EDEXUtil implements ApplicationContextAware {

    private static final String EDEX_SITE = System
            .getProperty("aw.site.identifier");

    private static final String EDEX_HOME = System.getProperty("edex.home");

    private static final String EDEX_BIN = EDEX_HOME + File.separatorChar
            + "bin";

    private static final String EDEX_PLUGINS = EDEX_HOME + File.separator
            + FileUtil.join("lib", "plugins") + File.separator;

    private static final String EDEX_DATA = EDEX_HOME + File.separator + "data"
            + File.separator;

    private static final String EDEX_UTILITY = EDEX_DATA + "utility";

    private static final String EDEX_SHARE = EDEX_DATA + "share";

    private static final Logger logger = LoggerFactory
            .getLogger(EDEXUtil.class);

    private static ApplicationContext mainContext;

    private static IMessageProducer mainMessageProducer;

    private static final String alertEndpoint = "alertVizNotify";

    private static final Object waiter = new Object();

    /* Latch is released when JVM begins shutting down. */
    private static volatile CountDownLatch shutdownLatch = new CountDownLatch(
            1);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shutdownLatch.countDown();
            }
        });
    }

    public static String getEdexSite() {
        return EDEX_SITE;
    }

    public static String getEdexHome() {
        return EDEX_HOME;
    }

    public static String getEdexUtility() {
        return EDEX_UTILITY;
    }

    public static String getEdexShare() {
        return EDEX_SHARE;
    }

    public static String getEdexPlugins() {
        return EDEX_PLUGINS;
    }

    public static String getEdexBin() {
        return EDEX_BIN;
    }

    public static String getEdexData() {
        return EDEX_DATA;
    }

    @Override
    public void setApplicationContext(ApplicationContext context)
            throws BeansException {
        if (mainContext == null) {
            mainContext = context;
        }
    }

    /**
     * @deprecated There can be multiple Spring contexts within a single EDEX
     *             JVM, and this can use the wrong one. Inject the Spring
     *             application context more directly into the calling code, such
     *             as by implementing ApplicationContextAware.
     */
    @Deprecated
    public static ApplicationContext getSpringContext() {
        return mainContext;
    }

    /**
     * Retrieve an object from the ESB context. This object could be a Spring
     * Bean, a context or a property container
     *
     * @param name
     *            name of the object
     * @return The instance
     * @deprecated There can be multiple Spring contexts within a single EDEX
     *             JVM, and this can use the wrong one. Inject the Spring
     *             application context into the calling code (e.g. by
     *             implementing ApplicationContextAware) and instead call
     *             {@link #getESBComponent(ApplicationContext, String)}.
     */
    @Deprecated
    public static Object getESBComponent(String name) {
        return getESBComponent(mainContext, name);
    }

    /**
     * Retrieve an object from the given ESB context. This object could be a
     * Spring bean, a context or a property container.
     *
     * @param context
     *            ESB context
     * @param name
     *            name of the object
     * @return the object instance
     */
    public static Object getESBComponent(ApplicationContext context,
            String name) {
        Object result = null;

        try {
            result = context.getBean(name);
        } catch (Exception e) {
            logger.error("Unable to retrieve component '" + name
                    + "' from ESB context '" + context.getDisplayName() + "'",
                    e);
        }

        return result;
    }

    public static boolean isRunning() {
        return "Operational".equals(System.getProperty("System.status"));
    }

    /**
     * Blocks until EDEX is in the running state.
     */
    public static void waitForRunning() {
        synchronized (waiter) {
            try {
                while (!isRunning()) {
                    waiter.wait(15 * TimeUtil.MILLIS_PER_SECOND);
                }
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Called once EDEX is in a running state to notify all waiting clients.
     */
    public static void notifyIsRunning() {
        synchronized (waiter) {
            waiter.notifyAll();
        }
    }

    /**
     * True if shutdown has been initiated, false otherwise.
     *
     * @return
     */
    public static boolean isShuttingDown() {
        return shutdownLatch.getCount() == 0;
    }

    /**
     * If EDEX is shutting down throws a ShutdownException
     *
     * @throws ShutdownException
     */
    public static void checkShuttingDown() throws ShutdownException {
        if (isShuttingDown()) {
            throw new ShutdownException();
        }
    }

    /**
     * Get the message producer of the main Spring application context
     *
     * @return the main application context message producer
     * @deprecated There can be multiple Spring contexts within a single EDEX
     *             JVM, and this can be the message producer for the wrong one.
     *             Inject the Spring application context's message producer more
     *             directly into the calling code.
     */
    @Deprecated
    public static IMessageProducer getMessageProducer() {
        return mainMessageProducer;
    }

    public void setMessageProducer(IMessageProducer messageProducer) {
        if (mainMessageProducer == null) {
            mainMessageProducer = messageProducer;
        }
    }

    public static void checkPersistenceTimes(PluginDataObject[] pdos) {
        Date curTime = new Date();

        for (PluginDataObject record : pdos) {
            if (record instanceof IPersistable) {
                if (((IPersistable) record).getPersistenceTime() == null) {
                    ((IPersistable) record).setPersistenceTime(curTime);
                }
            } else {
                record.setInsertTime(TimeUtil.newGmtCalendar(curTime));
            }
        }
    }

    /**
     * Send a message to alertViz with filters
     *
     * @param priority
     * @param pluginName
     * @param source
     * @param category
     * @param message
     * @param details
     * @param audioFile
     * @param filters
     */
    public static void sendMessageAlertViz(Priority priority, String pluginName,
            String source, String category, String message, String details,
            String audioFile, Map<String, String> filters) {

        StatusMessage sm = new StatusMessage();
        sm.setPriority(priority);
        sm.setPlugin(pluginName);
        sm.setCategory(category);
        sm.setMessage(message);
        sm.setMachineToCurrent();
        sm.setSourceKey(source);
        sm.setDetails(details);
        sm.setAudioFile(audioFile);
        sm.setFilters(filters);
        try {
            /*
             * Main message producer works fine for AlertViz regardless of
             * calling code's Spring application context
             */
            mainMessageProducer.sendAsync(alertEndpoint, sm);
        } catch (Exception e) {
            logger.error("Could not send message to AlertViz", e);
        }
    }

    /**
     * Send a message to alertViz
     *
     * @param priority
     * @param pluginName
     * @param source
     * @param category
     * @param message
     * @param details
     * @param audioFile
     */
    public static void sendMessageAlertViz(Priority priority, String pluginName,
            String source, String category, String message, String details,
            String audioFile) {

        sendMessageAlertViz(priority, pluginName, source, category, message,
                details, audioFile, null);
    }

    /**
     * @return the alertendpoint
     */
    public static String getAlertendpoint() {
        return alertEndpoint;
    }

    /**
     * Wait up to the specified time for EDEX to begin shutting down
     *
     * @param timeout
     * @param timeUnit
     * @return true if EDEX shutdown started within the time interval, false if
     *         the specified time elapsed without EDEX shutdown
     * @throws InterruptedException
     */
    public static boolean awaitShutdown(long timeout, TimeUnit timeUnit)
            throws InterruptedException {
        return shutdownLatch.await(timeout, timeUnit);
    }

}
