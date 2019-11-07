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

package com.raytheon.uf.viz.core.localization;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.FileLocker;
import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.ILocalizationAdapter;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManager;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.msgs.AbstractPrivilegedUtilityCommand;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityResponse;
import com.raytheon.uf.common.localization.msgs.DeleteUtilityCommand;
import com.raytheon.uf.common.localization.msgs.DeleteUtilityResponse;
import com.raytheon.uf.common.localization.msgs.GetServersResponse;
import com.raytheon.uf.common.localization.msgs.GetUtilityCommand;
import com.raytheon.uf.common.localization.msgs.ListContextCommand;
import com.raytheon.uf.common.localization.msgs.ListResponseEntry;
import com.raytheon.uf.common.localization.msgs.ListUtilityCommand;
import com.raytheon.uf.common.localization.msgs.ListUtilityResponse;
import com.raytheon.uf.common.localization.msgs.PrivilegedUtilityRequestMessage;
import com.raytheon.uf.common.localization.msgs.UtilityRequestMessage;
import com.raytheon.uf.common.localization.msgs.UtilityResponseMessage;
import com.raytheon.uf.common.localization.region.RegionLookup;
import com.raytheon.uf.common.python.PyCacheUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.viz.core.ProgramArguments;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.VizServers;
import com.raytheon.uf.viz.core.comm.ConnectivityManager;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.PrivilegedRequestFactory;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Manages the localization settings of the viz process and handles the
 * communication with the localization service for downloading/uploading files.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 17, 2007            chammack    Initial Creation.
 * Jul 24, 2007            njensen     Added upload().
 * Jul 30, 2007            njensen     Refactored.
 * Feb 12, 2008            chammack    Removed base configuration
 * Mar 26, 2008            njensen     Added rename() and getFileContents().
 * May 19, 2007 1127       randerso    Implemented error handling
 * Sep 12, 2012 1167       djohnson    Add datadelivery servers.
 * Jan 14, 2013 1469       bkowal      Removed the hdf5 data directory.
 * Aug 02, 2013 2202       bsteffen    Add edex specific connectivity checking.
 * Aug 27, 2013 2295       bkowal      The entire jms connection string is now
 *                                     provided by EDEX.
 * Feb 04, 2014 2704       njensen     Allow setting server without saving
 * Feb 06, 2014 2761       mnash       Add region localization level
 * Jun 19, 2014 3301       njensen     Acquire lock inside loop of retrieveFiles()
 * Jan 12, 2015 3993       njensen     Added checkPreinstalled()
 * Feb 16, 2015 3978       njensen     Use REST service for efficient GET of files
 * Aug 26, 2015 4691       njensen     No longer using timestamp as part of needDownload()
 * Dec 03, 2015 4834       njensen     Use REST service for efficient PUT of files
 * Jan 11, 2016 5242       kbisanz     Replaced calls to deprecated LocalizationFile methods
 * Jun 13, 2016 4907       mapeters    Added retrieveToFile()
 * Jun 22, 2017 6339       njensen     Use fileExtension in ListUtilityCommands
 * Jun 30, 2017 6316       njensen     Improved regions.xml debug message
 * Jul 18, 2017 6316       njensen     Log setting site localization
 * Sep 12, 2019 7917       tgurney     Update handling of pyc files for Python 3
 * Oct 16, 2019 7724       tgurney     Replace connection string with a
 *                                     {@link JMSConnectionInfo} object
 *
 * </pre>
 *
 * @author chammack
 */
public class LocalizationManager implements IPropertyChangeListener {

    private static IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationManager.class, "CAVE");

    public static final String USER_CONTEXT = LocalizationConstants.P_LOCALIZATION_USER_NAME;

    public static final String SITE_CONTEXT = LocalizationConstants.P_LOCALIZATION_SITE_NAME;

    private static final String PREINSTALLED_DIR = "utility"
            + IPathManager.SEPARATOR
            + LocalizationType.COMMON_STATIC.toString().toLowerCase()
            + IPathManager.SEPARATOR
            + LocalizationLevel.BASE.toString().toLowerCase();

    /** The singleton instance */
    private static LocalizationManager instance;

    /** The localization preference store */
    private ScopedPreferenceStore localizationStore;

    /** The localization adapter */
    private final ILocalizationAdapter adapter;

    /** The base context directory */
    private static String baseDir;

    /** The user directory */
    private static String userDir;

    private static File orphanFileDir;

    /** The current localization server */
    private String currentServer;

    private boolean overrideServer;

    /** The current localization site */
    private String currentSite;

    private boolean overrideSite;

    private final LocalizationRestConnector restConnect;

    /** Was the alert server launched within cave? */
    public static final boolean internalAlertServer = ProgramArguments
            .getInstance().getBoolean("-alertviz");

    private static Map<LocalizationLevel, String> contextMap = new HashMap<>();

    /**
     * Private constructor Use singleton construction
     */
    private LocalizationManager() {
        this.adapter = new CAVELocalizationAdapter();
        this.overrideServer = false;
        this.overrideSite = false;
        try {
            localizationStore = new ScopedPreferenceStore(
                    InstanceScope.INSTANCE, "localization");
            localizationStore.addPropertyChangeListener(this);
            loadHttpServer();
            loadAlertServer();
            loadCurrentSite();
        } catch (ExceptionInInitializerError e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Error initializing localization store", e);
        }
        registerContextName(LocalizationLevel.USER, getCurrentUser());
        registerContextName(LocalizationLevel.WORKSTATION,
                VizApp.getHostName());
        registerContextName(LocalizationLevel.BASE, null);
        /*
         * look for current site, only do site/region/configured if current site
         * is available
         */
        String currentSite = getCurrentSite();
        if (currentSite != null && !currentSite.isEmpty()) {
            registerContextName(LocalizationLevel.SITE, currentSite);
            registerContextName(LocalizationLevel.CONFIGURED, currentSite);
            String region = RegionLookup.getWfoRegion(getCurrentSite());
            if (region != null) {
                registerContextName(LocalizationLevel.REGION, region);
            } else {
                statusHandler.debug("Site " + getCurrentSite()
                        + " is not in regions.xml file, region localization level will be ignored");
            }
        }

        this.restConnect = new LocalizationRestConnector(this.adapter);
    }

    /**
     * Singleton: get the localization manager
     *
     * @return the localization manager singleton
     */
    public static synchronized LocalizationManager getInstance() {
        if (instance == null) {
            instance = new LocalizationManager();
        }

        return instance;
    }

    /**
     * Register a context name for a localization level
     *
     * @param level
     *            the localization level
     * @param name
     *            the name
     */
    public static void registerContextName(LocalizationLevel level,
            String name) {
        contextMap.put(level, name);
    }

    /**
     * Get context name
     *
     * @param level
     *            desired level
     * @return context name for desired level
     */
    public static String getContextName(LocalizationLevel level) {
        return contextMap.get(level);
    }

    /**
     * @return the current site
     */
    public String getCurrentSite() {
        return this.currentSite;
    }

    /**
     * Set the current site
     *
     * @param currentSite
     */
    public void setCurrentSite(String currentSite) {
        if (!this.currentSite.equals(currentSite)) {
            this.currentSite = currentSite;
            registerContextName(LocalizationLevel.SITE, this.currentSite);
            registerContextName(LocalizationLevel.CONFIGURED, this.currentSite);
            String region = RegionLookup.getWfoRegion(this.currentSite);
            if (region != null) {
                registerContextName(LocalizationLevel.REGION, region);
            } else {
                statusHandler.warn("Unable to find " + this.currentSite
                        + " in regions.xml file");
                contextMap.remove(LocalizationLevel.REGION);
            }
            if (!overrideSite) {
                localizationStore.putValue(
                        LocalizationConstants.P_LOCALIZATION_SITE_NAME,
                        this.currentSite);
                applyChanges();
            }
        }
        statusHandler.info("Localizing as site " + this.currentSite);
    }

    /**
     * Sets the localization server and saves the setting
     *
     * @param currentServer
     *            the localization URI
     */
    public void setCurrentServer(String currentServer) {
        setCurrentServer(currentServer, true);
    }

    /**
     * Sets the localization server
     *
     * @param currentServer
     *            the localization URI
     * @param save
     *            whether or not to save the setting
     */
    public void setCurrentServer(String currentServer, boolean save) {
        if (!this.currentServer.equals(currentServer)) {
            this.currentServer = currentServer;
            if (!overrideServer) {
                localizationStore.putValue(
                        LocalizationConstants.P_LOCALIZATION_HTTP_SERVER,
                        this.currentServer);
                if (save) {
                    applyChanges();
                }
            }

            try {
                GetServersResponse resp = ConnectivityManager
                        .checkLocalizationServer(currentServer, false);
                VizApp.setHttpServer(resp.getHttpServer());
                VizApp.setJmsConnectionInfo(resp.getJmsConnectionInfo());
                VizApp.setPypiesServer(resp.getPypiesServer());
                VizServers.getInstance()
                        .setServerLocations(resp.getServerLocations());
            } catch (VizException e) {
                statusHandler.handle(UFStatus.Priority.SIGNIFICANT,
                        "Error connecting to localization server", e);
            }
        }
    }

    /**
     * @return the current user
     */
    public String getCurrentUser() {
        return System.getProperty("user.name");
    }

    /**
     * @return the localizationStore
     */
    public ScopedPreferenceStore getLocalizationStore() {
        return localizationStore;
    }

    /**
     * Return the base installation directory
     *
     * This should be used rather than absolute paths
     *
     * @return the installation directory
     */
    public static synchronized String getBaseDir() {
        if (baseDir == null) {
            baseDir = System.getenv("VIZ_HOME");
        }

        if (baseDir == null) {
            baseDir = Platform.getInstallLocation().getURL().getPath();
            // Win32 URLS start with "/" but also may contain a drive letter
            // if so, then remove the "/" because it's improper for a path
            if (baseDir.startsWith("/") && baseDir.charAt(2) == ':') {
                baseDir = baseDir.substring(1);
            }
        }

        return baseDir;
    }

    /**
     * Return the user installation directory
     *
     * This should be used rather than absolute paths
     *
     * @return the user directory
     */
    public static synchronized String getUserDir() {

        if (userDir == null) {
            userDir = Platform.getUserLocation().getURL().getPath();
            // Win32 URLS start with "/" but also may contain a drive letter
            // if so, then remove the "/" because it's improper for a path
            if (userDir.startsWith("/") && userDir.charAt(2) == ':') {
                userDir = userDir.substring(1);
            }
        }

        return userDir;
    }

    private static synchronized File getOrphanedFileDir() {
        if (orphanFileDir == null) {
            orphanFileDir = new File(
                    getUserDir() + File.separator + "orphanFiles");
            if (!orphanFileDir.exists()) {
                orphanFileDir.mkdirs();
            }
        }
        return orphanFileDir;
    }

    /**
     * Get the current localization server
     *
     * @return the current server
     */
    public String getLocalizationServer() {
        return currentServer;
    }

    /**
     * Get the current site
     *
     * @return the current site
     */
    public String getSite() {
        return getCurrentSite();
    }

    /**
     * Save the localization store, handles errors through UFStatus
     */
    private void applyChanges() {
        try {
            localizationStore.save();
        } catch (IOException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error saving localization store", e);
        }
    }

    /**
     * Load the http server from the localization store and set up defaults if
     * needed
     */
    private void loadHttpServer() {
        if (!localizationStore
                .contains(LocalizationConstants.P_LOCALIZATION_HTTP_SERVER)) {
            // No value present, use default and save off
            currentServer = LocalizationConstants.DEFAULT_LOCALIZATION_SERVER;
            localizationStore.putValue(
                    LocalizationConstants.P_LOCALIZATION_HTTP_SERVER,
                    LocalizationConstants.DEFAULT_LOCALIZATION_SERVER);
            applyChanges();
        } else {
            currentServer = localizationStore.getString(
                    LocalizationConstants.P_LOCALIZATION_HTTP_SERVER);
        }
        checkForServerOverride();
    }

    /**
     * Check to see if the store has the alert server. If not store off the
     * default
     */
    private void loadAlertServer() {
        if (!localizationStore.contains(LocalizationConstants.P_ALERT_SERVER)) {
            localizationStore.putValue(LocalizationConstants.P_ALERT_SERVER,
                    LocalizationConstants.DEFAULT_ALERT_SERVER);
            applyChanges();
        }
    }

    private void loadCurrentSite() {
        if (ProgramArguments.getInstance().getString("-site") == null) {
            this.currentSite = this.localizationStore
                    .getString(LocalizationConstants.P_LOCALIZATION_SITE_NAME);
        } else {
            this.currentSite = ProgramArguments.getInstance().getString("-site")
                    .toUpperCase();
            this.overrideSite = true;
        }
        statusHandler.info("Localizing as site " + this.currentSite);
    }

    private void checkForServerOverride() {
        if (ProgramArguments.getInstance().getString("-server") != null) {
            String serverOverride = ProgramArguments.getInstance()
                    .getString("-server");
            currentServer = serverOverride;
            this.overrideServer = true;
            statusHandler.debug("Server overridden to: " + currentServer);
        }
    }

    @Override
    public void propertyChange(
            org.eclipse.jface.util.PropertyChangeEvent event) {
        // Listen for localization server and personality changes
        if (LocalizationConstants.P_LOCALIZATION_HTTP_SERVER
                .equals(event.getProperty())) {
            // Server changed, grab
            String newServer = (String) event.getNewValue();
            setCurrentServer(newServer);
        } else if (LocalizationConstants.P_LOCALIZATION_SITE_NAME
                .equals(event.getProperty())) {
            String site = (String) event.getNewValue();
            setCurrentSite(site);
        }
    }

    /**
     * Retrieves the files from the localization service
     *
     * @param commands
     * @param fileStamps
     */
    private void retrieveFiles(GetUtilityCommand[] commands,
            Date[] fileStamps) {
        for (int i = 0; i < commands.length; ++i) {
            GetUtilityCommand command = commands[i];
            File file = buildFileLocation(command.getContext(),
                    command.getFileName(), true);
            if (file == null) {
                continue;
            }
            try {
                FileLocker.lock(this, file, Type.WRITE);
                file.delete();
                restConnect.restGetFile(command.getContext(),
                        command.getFileName());

                if (fileStamps[i] != null) {
                    file.setLastModified(fileStamps[i].getTime());
                }

                // Mark as read only if the file is a system level
                if (command.getContext().getLocalizationLevel()
                        .isSystemLevel()) {
                    file.setReadOnly();
                }
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error requesting file: " + String.valueOf(file), e);
            } finally {
                FileLocker.unlock(this, file);
            }
        }
    }

    protected List<ListResponseEntry[]> getListResponseEntry(
            LocalizationContext[] contexts, String fileName,
            String fileExtension, boolean recursive, boolean filesOnly)
            throws LocalizationException {
        ListUtilityCommand[] cmds = new ListUtilityCommand[contexts.length];
        for (int i = 0; i < contexts.length; i++) {
            cmds[i] = new ListUtilityCommand(contexts[i], fileName,
                    fileExtension, recursive, filesOnly, getCurrentSite());
        }

        UtilityRequestMessage localizationRequest = new UtilityRequestMessage(
                cmds);

        AbstractUtilityResponse[] responseList = makeRequest(
                localizationRequest);
        if (responseList.length != contexts.length) {
            throw new LocalizationException(
                    "Server returned more or less results than requested.  Requested "
                            + contexts.length + ", returned: "
                            + responseList.length);
        } else if (responseList.length == 0) {
            return new ArrayList<>();
        }

        List<ListResponseEntry[]> responses = new ArrayList<>();

        for (AbstractUtilityResponse element : responseList) {
            AbstractUtilityResponse response = element;
            if (!(response instanceof ListUtilityResponse)) {
                throw new LocalizationException("Unexpected type returned"
                        + response.getClass().getName());
            }

            ListUtilityResponse listResponse = (ListUtilityResponse) response;

            ListResponseEntry[] entries = listResponse.getEntries();
            responses.add(entries);
        }

        return responses;

    }

    /**
     * Retrieves the LocalizationFile contents from the localization server.
     * Locks on the file
     *
     * @param file
     * @throws LocalizationException
     */
    protected void retrieve(ILocalizationFile file)
            throws LocalizationException {
        if (file.isDirectory()) {
            retrieveDir(file.getContext(), file.getPath());
        } else {
            File localFile = buildFileLocation(file.getContext(),
                    file.getPath(), false);
            checkPreinstalled(file.getContext(), file.getPath(), localFile);
            /*
             * We don't have a READ lock for needDownload() so the checksum can
             * potentially be messed up if another process or thread modifies at
             * this exact moment. However, this scenario is negligible since
             * it's very rare and needDownload() returns true if anything goes
             * wrong.
             */
            if (needDownload(localFile, file.getCheckSum())) {
                retrieveFiles(
                        new GetUtilityCommand[] { new GetUtilityCommand(
                                file.getContext(), file.getPath()) },
                        new Date[] { file.getTimeStamp() });
            }
        }
    }

    /**
     * Retrieves the LocalizationFile contents from the server to the given
     * outputFile.
     *
     * @param locFile
     *            the localization file to retrieve
     * @param outputFile
     *            the location where the localization file will be downloaded to
     * @throws CommunicationException
     */
    public void retrieveToFile(ILocalizationFile locFile, File outputFile)
            throws CommunicationException {
        try {
            FileLocker.lock(this, outputFile, Type.WRITE);
            outputFile.delete();
            restConnect.restGetFile(locFile.getContext(), locFile.getPath(),
                    outputFile);
        } finally {
            FileLocker.unlock(this, outputFile);
        }
    }

    /**
     * Retrieval which recursively downloads files for the path given the name.
     * Should be used for directories
     *
     * @param context
     * @param fileName
     * @throws LocalizationException
     */
    private void retrieveDir(LocalizationContext context, String fileName)
            throws LocalizationException {
        List<ListResponseEntry[]> entriesList = getListResponseEntry(
                new LocalizationContext[] { context }, fileName, null, true,
                false);

        List<File> toCheck = new ArrayList<>();
        Set<File> available = new TreeSet<>();
        if (!entriesList.isEmpty()) {
            ListResponseEntry[] entries = entriesList.get(0);

            List<GetUtilityCommand> commands = new ArrayList<>();
            List<Date> dates = new ArrayList<>();
            for (ListResponseEntry entry : entries) {
                File file = buildFileLocation(entry.getContext(),
                        entry.getFileName(), false);
                if (!entry.isDirectory()) {
                    available.add(file);
                    if (this.needDownload(context, entry)) {
                        GetUtilityCommand getCommand = new GetUtilityCommand(
                                context, entry.getFileName());
                        commands.add(getCommand);
                        dates.add(entry.getDate());
                    }
                } else {
                    if (file != null) {
                        file.mkdirs();
                    }

                    File[] list = file.listFiles(
                            (FileFilter) pathname -> !pathname.isDirectory());

                    if (list != null) {
                        toCheck.addAll(Arrays.asList(list));
                    } else {
                        statusHandler.debug("ERROR READING DIRECTORY: "
                                + file.getAbsolutePath());
                    }
                }
            }
            if (!commands.isEmpty()) {
                retrieveFiles(commands.toArray(new GetUtilityCommand[0]),
                        dates.toArray(new Date[0]));
            }
        }

        // Clean up any stale files that don't exist anymore
        for (File check : toCheck) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            // hidden files are not returned from server so ignore those
            if (!check.isHidden() && !available.contains(check)) {
                String name = check.getName();
                /*
                 * Make sure compiled Python files don't get removed when the
                 * source file is still available
                 */
                if (PyCacheUtil.isPyCacheFile(check.toPath())) {
                    Path sourceFilePath = PyCacheUtil
                            .getSourceFilePath(check.toPath());
                    if (available.contains(sourceFilePath.toFile())) {
                        continue;
                    }
                }

                File newFile = new File(getOrphanedFileDir(), name + "_"
                        + sdf.format(Calendar.getInstance().getTime()));
                statusHandler
                        .debug("MOVING OLD FILE: " + check + " to " + newFile);
                check.renameTo(newFile);
            }
        }
    }

    private File buildFileLocation(LocalizationContext context,
            String fullFileName, boolean createDirectories) {
        File file = this.adapter.getPath(context, fullFileName);

        if (createDirectories && file != null) {
            file.getParentFile().mkdirs();
        }

        return file;
    }

    /**
     * If the userDir does not contain the specified localFile, this method
     * checks the viz install to see if the file is present in an installed
     * bundle. If so, it copies the file from the installed bundle to the
     * userDir. This speeds up performance with a fresh install by getting the
     * files locally where available instead of downloading them from the
     * server.
     *
     * The checksum will still be checked by the needDownload() method, so this
     * should be called before needDownload(). If by chance the local viz
     * install is very out of date, this will be copying files into the userDir
     * that will then be immediately replaced when needDownload() recognizes the
     * checksum is incorrect.
     */
    private void checkPreinstalled(LocalizationContext context, String filename,
            File localFile) {
        if (LocalizationLevel.BASE.equals(context.getLocalizationLevel())
                && LocalizationType.COMMON_STATIC.equals(
                        context.getLocalizationType())
                && !localFile.exists()) {
            Collection<String> bundles = BundleScanner
                    .getListOfBundles(PREINSTALLED_DIR);
            for (String b : bundles) {
                File foundFile = BundleScanner.searchInBundle(b,
                        PREINSTALLED_DIR, filename);
                if (foundFile != null && foundFile.exists()) {
                    try {
                        if (FileLocker.lock(this, localFile, Type.WRITE)) {
                            /*
                             * Locking the file will cause mkdirs() to be called
                             * on the parent dir. We skip checking the checksum
                             * because needDownload() is called after
                             * checkPreinstalled() and will verify the checksum.
                             */
                            FileUtil.copyFile(foundFile, localFile);
                        }
                    } catch (IOException e) {
                        /*
                         * log as debug, if this fails it will just pull the
                         * file from the server
                         */
                        statusHandler.handle(Priority.DEBUG, "Error copying "
                                + filename + " to " + localFile.getPath(), e);
                    } finally {
                        FileLocker.unlock(this, localFile);
                    }

                    // file found, break out of for loop
                    break;
                }
            }
        }
    }

    /**
     * Need to download files?
     *
     * @param context
     * @param listResponseEntry
     * @return true if file needs to be downloaded
     */
    public boolean needDownload(LocalizationContext context,
            ListResponseEntry listResponseEntry) {
        String fullFileName = listResponseEntry.getFileName();

        File file = buildFileLocation(context, fullFileName, false);
        checkPreinstalled(context, fullFileName, file);
        /*
         * We don't have a READ lock for needDownload() so the checksum can
         * potentially be messed up if another process or thread modifies at
         * this exact moment. However, this scenario is negligible since it's
         * very rare and needDownload() returns true if anything goes wrong.
         */
        return needDownload(file, listResponseEntry.getChecksum());
    }

    /**
     * Checks if the file needs downloaded based on if it exists locally and the
     * checksum. This method does no locking and if reading the file for the
     * checksum goes wrong in any way, it will return true.
     *
     * @param file
     * @param remoteChecksum
     * @return
     */
    private boolean needDownload(File file, String remoteChecksum) {
        if (file == null) {
            return false;
        }

        if (!file.exists()) {
            return true;
        }

        try {
            // Check the checksum (integrity check)
            String localChecksum = Checksum.getMD5Checksum(file);
            return !localChecksum.equals(remoteChecksum);
        } catch (Throwable t) {
            statusHandler.handle(Priority.DEBUG,
                    "Exception computing MD5 checksum", t);

            // something went wrong, just re-download the file
            return true;
        }

    }

    /**
     * Uploads a file to the localization service.
     *
     * @param file
     *            the localization file
     * @param fileToUpload
     *            the local file that backs the localization file
     * @throws LocalizationException
     */
    protected void upload(ILocalizationFile file, File fileToUpload)
            throws LocalizationException {
        try {
            FileUpdatedMessage fum = restConnect.restPutFile(file,
                    fileToUpload);

            // notify our JVM of the update
            for (PathManager pm : PathManagerFactory.getActivePathManagers()) {
                pm.fireListeners(fum);
            }
        } catch (CommunicationException e) {
            throw new LocalizationException("Error uploading file: " + file, e);
        }

    }

    /**
     * Deletes a localization file from localization server
     *
     * @param context
     *            the context to the file
     * @param filename
     *            the name of the file
     * @return modified time on server
     * @throws LocalizationException
     */
    protected long delete(LocalizationContext context, String filename)
            throws LocalizationException {
        PrivilegedUtilityRequestMessage request;
        try {
            request = PrivilegedRequestFactory.constructPrivilegedRequest(
                    PrivilegedUtilityRequestMessage.class);
        } catch (VizException e) {
            throw new LocalizationException(
                    "Could not construct privileged utility request", e);
        }

        DeleteUtilityCommand command = new DeleteUtilityCommand(context,
                filename);
        command.setMyContextName(
                getContextName(context.getLocalizationLevel()));
        AbstractPrivilegedUtilityCommand[] commands = new AbstractPrivilegedUtilityCommand[] {
                command };
        request.setCommands(commands);
        try {
            UtilityResponseMessage response = (UtilityResponseMessage) ThriftClient
                    .sendLocalizationRequest(request);
            if (response == null) {
                throw new LocalizationException(
                        "No response received for delete command");
            }
            AbstractUtilityResponse[] responses = response.getResponses();
            if (responses == null || responses.length != commands.length) {
                throw new LocalizationException(
                        "Unexpected return type from delete: Expected "
                                + commands.length + " responses, received "
                                + (responses != null ? responses.length
                                        : null));
            }
            AbstractUtilityResponse rsp = responses[0];
            if (rsp instanceof DeleteUtilityResponse) {
                DeleteUtilityResponse dur = (DeleteUtilityResponse) rsp;
                if (dur.getErrorText() != null) {
                    throw new LocalizationException(
                            "Error processing delete command: "
                                    + dur.getErrorText());
                }
                // Yay, successful execution!
                return dur.getTimeStamp();
            }

            throw new LocalizationException(
                    "Unexpected return type from delete: Expected "
                            + DeleteUtilityResponse.class + " received "
                            + (rsp != null ? rsp.getClass() : null));
        } catch (VizException e) {
            throw new LocalizationException("Error processing delete command: "
                    + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Makes a request to the UtilitySrv
     *
     * @param request
     *            the request to make
     * @return the responses from the request
     * @throws LocalizationException
     */
    protected AbstractUtilityResponse[] makeRequest(
            UtilityRequestMessage request) throws LocalizationException {

        AbstractUtilityResponse[] responseList;

        UtilityResponseMessage localizationResponse = null;
        try {
            localizationResponse = (UtilityResponseMessage) ThriftClient
                    .sendLocalizationRequest(request);
        } catch (VizException e) {
            throw new LocalizationException("Localization error ", e);
        }

        responseList = localizationResponse.getResponses();

        for (AbstractUtilityResponse response : responseList) {
            if (!response.successful()) {
                throw new LocalizationException(
                        response.getFormattedErrorMessage());
            }
        }

        return responseList;
    }

    /**
     * Get context list for a localization level
     *
     * @param level
     * @return the context list
     * @throws LocalizationException
     */
    public List<ListResponseEntry[]> getContextList(LocalizationLevel level)
            throws LocalizationException {
        ListContextCommand cmd = new ListContextCommand();
        cmd.setRequestLevel(level);

        UtilityRequestMessage localizationRequest = new UtilityRequestMessage(
                new ListContextCommand[] { cmd });

        AbstractUtilityResponse[] responseList = makeRequest(
                localizationRequest);

        List<ListResponseEntry[]> responses = new ArrayList<>();

        for (AbstractUtilityResponse element : responseList) {
            AbstractUtilityResponse response = element;
            if (!(response instanceof ListUtilityResponse)) {
                throw new LocalizationException("Unexpected type returned"
                        + response.getClass().getName());
            }

            ListUtilityResponse listResponse = (ListUtilityResponse) response;

            ListResponseEntry[] entries = listResponse.getEntries();
            responses.add(entries);
        }

        return responses;
    }

    /**
     * @return true if server is overridden
     */
    public boolean isOverrideServer() {
        return overrideServer;
    }

    /**
     * @return true if user is overridden
     */
    public boolean isOverrideSite() {
        return overrideSite;
    }
}
