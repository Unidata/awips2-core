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
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 04/23/2008   1088        chammack    Split from Util
 * 11/22/2010   2235        cjeanbap    Added audio file to StatusMessage.
 * 02/02/2011   6500        cjeanbap    Added paramter to method signature and
 *                                      properly assign source value.
 * 06/12/2012   0609        djohnson    Use EDEXUtil for EDEX_HOME.
 * 03/18/2013   1802        bphillip    Added getList utility function
 * 04/10/2014   2726        rjpeter     Added methods for waiting for edex to be running.
 * 06/25/2014   3165        njensen     Remove dead code
 * Jul 16, 2014 2914        garmendariz Remove EnvProperties
 * Jul 27, 2015 4654        skorolev    Added filters in sendMessageAlertViz
 * Dec 17, 2015 5166        kbisanz     Update logging to use SLF4J
 * Apr 25, 2016 5604        rjpeter     Updated checkPersistenceTimes to utilize same object for each call.
 * Apr 19, 2017 6187        njensen     Improved logging
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

    private static Logger logger = LoggerFactory.getLogger(EDEXUtil.class);

    private static ApplicationContext CONTEXT;

    private static IMessageProducer MESSAGE_PRODUCER;

    private static IContextAdmin CONTEXT_ADMIN;

    private static final String alertEndpoint = "alertVizNotify";

    private static final Object waiter = new Object();

    private static volatile boolean shuttingDown = false;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                shuttingDown = true;
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
        CONTEXT = context;
    }

    public static ApplicationContext getSpringContext() {
        return CONTEXT;
    }

    /**
     * Retrieve an object from the ESB context This object could be a Spring
     * Bean, a context or a property container
     * 
     * @param name
     *            name of the object
     * @return The instance
     */
    public static Object getESBComponent(String name) {
        Object result = null;

        try {
            result = CONTEXT.getBean(name);
        } catch (Exception e) {
            logger.error("Unable to retrieve component: " + name
                    + " from ESB.", e);
        }

        return result;

    }

    /**
     * Retrieve an object from the ESB context This object could be a Spring
     * Bean, a context or a property container
     * 
     * @param clazz
     *            the return class type
     * @param name
     *            the name of the component
     * @return The casted instance
     */
    public static <T> T getESBComponent(Class<T> clazz, String name) {
        return clazz.cast(getESBComponent(name));
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
                    waiter.wait(15000);
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

    public static boolean containsESBComponent(String name) {
        return CONTEXT.containsBean(name);
    }

    /**
     * True if shutdown has been initiated, false otherwise.
     * 
     * @return
     */
    public static boolean isShuttingDown() {
        return shuttingDown;
    }

    /**
     * If EDEX is shutting down throws a ShutdownException
     * 
     * @throws ShutdownException
     */
    public static void checkShuttingDown() throws ShutdownException {
        if (shuttingDown) {
            throw new ShutdownException();
        }
    }

    public static IMessageProducer getMessageProducer() {
        return MESSAGE_PRODUCER;
    }

    public void setMessageProducer(IMessageProducer message_producer) {
        MESSAGE_PRODUCER = message_producer;
    }

    public static IContextAdmin getContextAdmin() {
        return CONTEXT_ADMIN;
    }

    public void setContextAdmin(IContextAdmin admin) {
        CONTEXT_ADMIN = admin;
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
    public static void sendMessageAlertViz(Priority priority,
            String pluginName, String source, String category, String message,
            String details, String audioFile, Map<String, String> filters) {

        StatusMessage sm = new StatusMessage();
        sm.setPriority(priority);
        sm.setPlugin(pluginName);
        sm.setCategory(category);
        sm.setMessage(message);
        sm.setMachineToCurrent();
        sm.setSourceKey(source);
        sm.setDetails(details);
        sm.setEventTime(new Date());
        sm.setAudioFile(audioFile);
        sm.setFilters(filters);
        try {
            getMessageProducer().sendAsync(alertEndpoint, sm);
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
    public static void sendMessageAlertViz(Priority priority,
            String pluginName, String source, String category, String message,
            String details, String audioFile) {

        sendMessageAlertViz(priority, pluginName, source, category, message,
                details, audioFile, null);
    }

    /**
     * @return the alertendpoint
     */
    public static String getAlertendpoint() {
        return alertEndpoint;
    }

}
