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

package com.raytheon.uf.viz.core;

import java.lang.management.ManagementFactory;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.statushandlers.StatusManager;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.message.WsId;
import com.raytheon.uf.common.util.SystemUtil;
import com.raytheon.uf.viz.core.localization.LocalizationManager;

/**
 * General purpose utility method class
 * 
 * <pre>
 * 
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date          Ticket#     Engineer    Description
 *    ------------	----------	-----------	--------------------------
 *    7/1/06                    chammack    Initial Creation.
 *    Sep 12, 2012  1167        djohnson    Add datadelivery servers.
 *    Jan 14, 2013  1469        bkowal      Removed the hdf5 data directory.
 *    Aug 27, 2013  2295        bkowal      Removed the jms server property; added
 *                                          jms connection string
 *    Feb 17, 2014  2812        njensen     getHostName() now uses getWsId()'s hostname
 *    Mar 20, 2014  2726        rjpeter     Moved host processing to SystemUtil.
 * </pre>
 * 
 * @author chammack
 * 
 */
public final class VizApp {

    /**
     * Disabled constructor.
     */
    private VizApp() {
    }

    private static final String USER_FLAG = "-u";

    private static WsId wsId;

    private static String httpServer;

    private static String jmsConnectionString;

    private static String pypiesServer;

    static {
        ManagementFactory.getRuntimeMXBean().getName();
    }

    /**
     * Get a unique workstation Id
     * 
     * @return the wsId
     */
    public static WsId getWsId() {
        if (wsId == null) {
            String[] args = Platform.getApplicationArgs();
            String userName = "";
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if ((USER_FLAG.equals(arg)) && ((i + 1) < args.length)) {
                    userName = args[i + 1];
                }
            }

            if (!userName.isEmpty()) {
                System.setProperty("user.name", userName);
                LocalizationManager.registerContextName(LocalizationLevel.USER,
                        userName);
            }

            wsId = new WsId(null, null, Platform.getProduct().getName());
        }

        return wsId;
    }

    /**
     * Run a task asynchronously on the UI thread
     * 
     * @param aTask
     *            the task to run
     */
    public static void runAsync(Runnable aTask) {
        Display.getDefault().asyncExec(aTask);
    }

    /**
     * Run a task synchronously on the UI thread
     * 
     * @param task
     *            the task to run
     */
    public static void runSync(Runnable task) {
        Display.getDefault().syncExec(task);
    }

    /**
     * Run a task synchronously on the UI thread if a workbench is running,
     * otherwise just runs the task
     * 
     * @param task
     *            the task to run
     */
    public static void runSyncIfWorkbench(Runnable task) {
        if (PlatformUI.isWorkbenchRunning()) {
            runSync(task);
        } else {
            task.run();
        }
    }

    /**
     * Return the base installation directory
     * 
     * This should be used rather than absolute paths
     * 
     * @return the installation directory
     */
    public static String getBaseDir() {
        return LocalizationManager.getBaseDir();
    }

    /**
     * Return the user installation directory
     * 
     * This should be used rather than absolute paths
     * 
     * @return the user directory
     */
    public static String getUserDir() {
        return LocalizationManager.getUserDir();
    }

    /**
     * @return the mapsDir
     */
    public static String getMapsDir() {
        return "basemaps";
    }

    /**
     * @return the dataDir
     */
    public static String getDataDir() {
        return Activator
                .getDefault()
                .getPreferenceStore()
                .getString(
                        com.raytheon.uf.viz.core.preferences.PreferenceConstants.P_DATA_DIRECTORY);
    }

    public static int getCorePreferenceInt(String pref) {
        return Activator.getDefault().getPreferenceStore().getInt(pref);
    }

    public static String getCorePreferenceString(String pref) {
        return Activator.getDefault().getPreferenceStore().getString(pref);
    }

    /**
     * Use UFStatus instead
     * 
     * @param severity
     * @param t
     * @param title
     * @param message
     * @param activator
     * @param pluginId
     */
    @Deprecated
    public static void logAndAlert(int severity, Throwable t,
            final String title, final String message,
            AbstractUIPlugin activator, String pluginId) {
        final Status s = new Status(severity, pluginId, message, t);

        StatusManager.getManager().handle(s);

    }

    /**
     * Use UFStatus instead.
     * 
     * @param severity
     * @param t
     * @param message
     * @param activator
     * @param pluginId
     */
    @Deprecated
    public static void logWithoutAlert(int severity, Throwable t,
            final String message, AbstractUIPlugin activator, String pluginId) {
        final Status s = new Status(severity, pluginId, message, t);
        StatusManager.getManager().handle(s);
    }

    public static String getHttpServer() {
        return httpServer;
    }

    public static void setHttpServer(String httpServer) {
        // overrides allowed for thin client
        VizApp.httpServer = System.getProperty("awips.httpServer", httpServer);
    }

    public static String getJmsConnectionString() {
        return jmsConnectionString;
    }

    public static void setJmsConnectionString(String jmsConnectionString) {
        VizApp.jmsConnectionString = jmsConnectionString;
    }

    public static String getPypiesServer() {
        return pypiesServer;
    }

    public static void setPypiesServer(String pypiesServer) {
        VizApp.pypiesServer = pypiesServer;
    }

    private static String host = null;

    /**
     * Gets the host name of the machine calling the function
     * 
     * @return
     */
    public static synchronized String getHostName() {
        if (host == null) {
            host = SystemUtil.getHostName();
        }
        return host;
    }
}
