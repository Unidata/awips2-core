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
package com.raytheon.uf.viz.personalities.cave.component;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.core.engine.CSSEngine;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.css.swt.engine.CSSSWTEngineImpl;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.pypies.PyPiesDataStoreFactory;
import com.raytheon.uf.common.pypies.PypiesProperties;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.viz.application.component.IStandaloneComponent;
import com.raytheon.uf.viz.core.ProgramArguments;
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.localization.CAVELocalizationAdapter;
import com.raytheon.uf.viz.core.localization.CAVELocalizationNotificationObserver;
import com.raytheon.uf.viz.core.localization.LocalizationInitializer;
import com.raytheon.uf.viz.core.notification.jobs.NotificationManagerJob;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.uf.viz.core.status.VizStatusHandlerFactory;
import com.raytheon.uf.viz.personalities.cave.workbench.VizWorkbenchAdvisor;
import com.raytheon.viz.alerts.jobs.AutoUpdater;
import com.raytheon.viz.alerts.jobs.MenuUpdater;
import com.raytheon.viz.alerts.observers.ProductAlertObserver;
import com.raytheon.viz.core.CorePlugin;
import com.raytheon.viz.core.mode.CAVEMode;
import com.raytheon.viz.core.units.UnitRegistrar;

/**
 * {@link IStandaloneComponent} that starts and initializes the CAVE Workbench
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 20, 2013            mschenke    Initial creation
 * Sep 10, 2014 3612       mschenke    Refactored, mirgrated logic from awips
 * Jan 15, 2015 3947       mapeters    Don't save simulated time
 * Jun 26, 2015 4474       bsteffen    Register the PathManager as an OSGi service.
 * Jan 11, 2016 5232       njensen     Apply css style at startup
 * Jun 27, 2017 6316       njensen     Pass along start time
 * Oct 07, 2021 8673       randerso    Eliminate MessageDialog during startup.
 *
 * </pre>
 *
 * @author mschenke
 */

public class CAVEApplication implements IStandaloneComponent {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(CAVEApplication.class, "CAVE");

    /** The name of the component launched */
    private String componentName;

    private Display applicationDisplay;

    @Override
    @SuppressWarnings("restriction")
    public Object startComponent(String componentName) throws Exception {
        long workbenchStartTime = System.currentTimeMillis();
        this.componentName = componentName;

        // Workaround for when PlatformUI has not been started
        Platform.getLog(WorkbenchPlugin.getDefault().getBundle())
                .addLogListener(getEclipseLogListener());

        UnitRegistrar.registerUnits();
        CAVEMode.performStartupDuties();

        // Get the display
        this.applicationDisplay = createDisplay();
        applyCssStyle(this.applicationDisplay);

        // verify Spring successfully initialized, otherwise stop CAVE
        if (!com.raytheon.uf.viz.spring.dm.Activator.getDefault()
                .isSpringInitSuccessful()) {
            return handleSpringFailure();
        }

        try {
            initializeLocalization();
        } catch (Exception e) {
            e.printStackTrace();
            statusHandler.fatal("Could not connect to localization server: "
                    + e.getLocalizedMessage(), e);
            // we return EXIT_OK here so eclipse doesn't try to pop up an error
            // dialog which would break gfeClient-based cron jobs.
            return IApplication.EXIT_OK;
        }

        int returnCode = IApplication.EXIT_OK;
        WorkbenchAdvisor workbenchAdvisor = null;
        // A component was passed as command line arg
        // launch cave normally, should cave be registered as component?
        try {
            UFStatus.setHandlerFactory(new VizStatusHandlerFactory());

            initializeDataStoreFactory();
            initializeObservers();

            initializeSimulatedTime();

            // open JMS connection to allow alerts to be received
            NotificationManagerJob.connect();

            workbenchAdvisor = getWorkbenchAdvisor();
            if (workbenchAdvisor instanceof VizWorkbenchAdvisor) {
                ((VizWorkbenchAdvisor) workbenchAdvisor)
                        .setWorkbenchStartTime(workbenchStartTime);
                ((VizWorkbenchAdvisor) workbenchAdvisor)
                        .setAppStartTime(com.raytheon.uf.viz.spring.dm.Activator
                                .getDefault().getApplicationStartTime());
                // Only initialize the procedure XML if the workbench advisor is
                // a VizWorkbenchAdvisor meaning the CAVE display will be up
                ProcedureXmlManager.inititializeAsync();
            }

            if (workbenchAdvisor != null) {
                returnCode = PlatformUI.createAndRunWorkbench(
                        this.applicationDisplay, workbenchAdvisor);
            }
        } catch (Throwable t) {
            statusHandler.fatal(
                    "Error instantiating workbench: " + t.getLocalizedMessage(),
                    t);
        } finally {
            cleanup();
        }

        if (returnCode == PlatformUI.RETURN_RESTART) {
            return IApplication.EXIT_RESTART;
        }
        return IApplication.EXIT_OK;
    }

    /**
     * Cleanup called before existing to clean up any resources that need it.
     */
    protected void cleanup() {
        try {
            // disconnect from JMS
            NotificationManagerJob.disconnect();
        } catch (@SuppressWarnings("squid:S1166")
        RuntimeException e) {
            /*
             * catch any exceptions to ensure the rest of the finally block in
             * the calling method executes
             */
        }

        if (this.applicationDisplay != null) {
            this.applicationDisplay.dispose();
        }
    }

    /**
     * @return the componentName
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @return true of the application does not have a UI associated with it,
     *         {@link CAVEApplication} implementation returns false by default
     */
    protected boolean isNonUIComponent() {
        return false;
    }

    /**
     * This is a workaround to receive status messages because without the
     * PlatformUI initialized Eclipse throws out the status messages. Once
     * PlatformUI has started, the status handler will take over.
     */
    protected ILogListener getEclipseLogListener() {
        return new ILogListener() {
            @Override
            @SuppressWarnings("squid:S106")
            public void logging(IStatus status, String plugin) {
                if (status.getMessage() != null) {
                    System.out.println(status.getMessage());
                }
                if (status.getException() != null) {
                    status.getException().printStackTrace();
                }
            }
        };
    }

    /**
     * @return the {@link #applicationDisplay}
     */
    protected Display getApplicationDisplay() {
        return this.applicationDisplay;
    }

    /**
     * Creates the {@link Display} for use within the application. Uses
     * {@link PlatformUI#createDisplay()} in {@link CAVEApplication}
     * implementation
     *
     * @return The {@link Display}
     */
    protected Display createDisplay() {
        if (isNonUIComponent()) {
            return new Display();
        }
        return PlatformUI.createDisplay();
    }

    /**
     * Method called when the spring plugin fails to initialize,
     * {@link CAVEApplication} implementation will prompt user for what to do
     * (restart or exit)
     *
     * @return An {@link IApplication} exit value depending on action that
     *         should be taken due to spring initialization failure
     */
    protected int handleSpringFailure() {
        String msg = "CAVE's Spring container did not initialize correctly and CAVE must shut down.";
        boolean restart = false;
        if (!isNonUIComponent()) {
            msg += " Attempt to restart CAVE?";
            restart = MessageDialog.openQuestion(
                    new Shell(Display.getDefault()), "Startup Error", msg);
        }
        if (restart) {
            return IApplication.EXIT_RESTART;
        }
        return IApplication.EXIT_OK;
    }

    /**
     * Initializes localization API for use within CAVE. Calls
     * {@link #initializeLocalization(boolean, boolean)} with true, false in
     * {@link CAVEApplication} implementation
     *
     * @throws Exception
     */
    protected void initializeLocalization() throws Exception {
        initializeLocalization(true, false);
    }

    protected void initializeLocalization(boolean promptUI,
            boolean checkAlertServer) throws Exception {
        PathManagerFactory.setAdapter(new CAVELocalizationAdapter());
        new LocalizationInitializer(promptUI, checkAlertServer).run();
        FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(
                IPathManager.class, PathManagerFactory.getPathManager(), null);
    }

    /**
     * Initializes the DataStoreFactory for the component.
     */
    protected void initializeDataStoreFactory() {
        PypiesProperties pypiesProps = new PypiesProperties();
        pypiesProps.setAddress(VizApp.getPypiesServer());
        DataStoreFactory.getInstance()
                .setUnderlyingFactory(new PyPiesDataStoreFactory(pypiesProps));
    }

    /**
     * Initialize any observers needed by the application
     */
    protected void initializeObservers() {
        // Setup cave notification observer
        CAVELocalizationNotificationObserver.register();
        registerProductAlerts();
    }

    protected void registerProductAlerts() {
        // Register product observers
        ProductAlertObserver.addObserver(null, new MenuUpdater());
        for (String plugin : RecordFactory.getInstance()
                .getSupportedPlugins()) {
            // Create separate AutoUpdater per plugin
            ProductAlertObserver.addObserver(plugin, new AutoUpdater());
        }
    }

    /**
     * Restore the prior state of SimulatedTime
     */
    private void initializeSimulatedTime() {
        long timeValue = 0;

        // If CorePlugin.getDefault() == null, assume running from a unit test
        if (CorePlugin.getDefault() != null) {
            String dateString = ProgramArguments.getInstance()
                    .getString("-time");
            if (dateString != null && !dateString.isEmpty()) {
                try {
                    DateFormat dateParser = new SimpleDateFormat(
                            "yyyyMMdd_HHmm");
                    dateParser.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Date newSimTime = dateParser.parse(dateString);
                    timeValue = newSimTime.getTime();
                } catch (ParseException e) {
                    statusHandler.warn(
                            "Invalid argument specified for command-line parameter '-time'.",
                            e);
                }
            }
        }

        SimulatedTime systemTime = SimulatedTime.getSystemTime();
        systemTime.notifyListeners(false);
        systemTime.setRealTime();
        if (timeValue != 0) {
            systemTime.setTime(new Date(timeValue));
        }
        systemTime.notifyListeners(true);
    }

    /**
     * Get the workbench advisor for the application
     *
     * @return the {@link WorkbenchAdvisor}
     */
    protected WorkbenchAdvisor getWorkbenchAdvisor() {
        return new VizWorkbenchAdvisor();
    }

    @SuppressWarnings("restriction")
    protected void applyCssStyle(Display display) throws IOException {
        CAVEMode mode = CAVEMode.getMode();
        CSSEngine cssEngine = new CSSSWTEngineImpl(this.applicationDisplay);
        String cssFile = "css" + File.separator;
        switch (mode) {
        case PRACTICE:
            cssFile += "practicemode.css";
            break;
        case TEST:
            cssFile += "testmode.css";
            break;
        default:
            cssFile += "viz.css";
            break;
        }
        Bundle b = FrameworkUtil.getBundle(CAVEApplication.class);
        IPath path = new Path(cssFile);
        try (InputStream is = FileLocator.openStream(b, path, false)) {
            cssEngine.parseStyleSheet(is);
            WidgetElement.setEngine(this.applicationDisplay, cssEngine);
        }
    }
}
