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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.localization.ILocalizationAdapter;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFile.ModifiableLocalizationFile;
import com.raytheon.uf.common.localization.LocalizationInternalFile;
import com.raytheon.uf.common.localization.LockingFileInputStream;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityCommand;
import com.raytheon.uf.common.localization.msgs.AbstractUtilityResponse;
import com.raytheon.uf.common.localization.msgs.ListResponseEntry;
import com.raytheon.uf.common.localization.msgs.ProtectedFileCommand;
import com.raytheon.uf.common.localization.msgs.ProtectedFileResponse;
import com.raytheon.uf.common.localization.msgs.UtilityRequestMessage;
import com.raytheon.uf.common.util.FileUtil;

/**
 * Provides a single interface to CAVE that provides localization services. Most
 * methods are presently implemented by handing off control to
 * LocalizationManager.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * May 08, 2008 878         chammack    Initial creation
 * Aug 02, 2013 2202        bsteffen    Add edex specific connectivity checking.
 * Aug 09, 2013 2033        mschenke    Made CAVE_STATIC BASE search all plugins
 *                                      if not found in etc base dir if no context
 *                                      name set
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class CAVELocalizationAdapter implements ILocalizationAdapter {

    protected static final String BUNDLE_LOCALIZATION_DIR = "localization";

    private static final Map<LocalizationType, LocalizationContext[]> contexts = new HashMap<LocalizationType, LocalizationContext[]>();

    private static final Map<String, File> caveStaticBaseFiles = new HashMap<String, File>();

    protected static final LocalizationContext CAVE_STATIC_BASE = new LocalizationContext(
            LocalizationType.CAVE_STATIC, LocalizationLevel.BASE);

    protected static final LocalizationContext CAVE_CONFIG_BASE = new LocalizationContext(
            LocalizationType.CAVE_CONFIG, LocalizationLevel.BASE);

    protected static boolean isCaveStaticBase(LocalizationContext ctx) {
        return ctx.getLocalizationType() == CAVE_STATIC_BASE
                .getLocalizationType()
                && ctx.getLocalizationLevel() == CAVE_STATIC_BASE
                        .getLocalizationLevel();
    }

    protected static boolean isCaveConfigBase(LocalizationContext ctx) {
        return ctx.getLocalizationType() == CAVE_CONFIG_BASE
                .getLocalizationType()
                && ctx.getLocalizationLevel() == CAVE_CONFIG_BASE
                        .getLocalizationLevel();
    }

    /**
     * Returns a directory name for the localization type
     * 
     * @param type
     * @return
     */
    public String getDirNameForType(LocalizationType type) {
        if (type == LocalizationType.COMMON_STATIC) {
            return "common";
        } else if (type == LocalizationType.CAVE_STATIC) {
            return "etc";
        } else if (type == LocalizationType.CAVE_CONFIG) {
            return "configuration";
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#getPath(com.
     * raytheon.uf.common.localization.LocalizationContext, java.lang.String)
     */
    @Override
    public File getPath(LocalizationContext context, String fileName) {
        LocalizationLevel level = context.getLocalizationLevel();
        String typeDir = getDirNameForType(context.getLocalizationType());
        String levelDir = level.toString().toLowerCase();
        if (isCaveStaticBase(context)) {
            // Check to see if it is resident in a bundle first
            // else go to the cave static dir
            if (context.getContextName() != null) {
                return BundleScanner.searchInBundle(context.getContextName(),
                        BUNDLE_LOCALIZATION_DIR, fileName);
            } else {
                File file = new File(FileUtil.join(
                        LocalizationManager.getBaseDir(), typeDir, fileName));
                if (file.exists()) {
                    return file;
                } else {
                    File bundleFile = file; // Default to base location
                    boolean containsKey = false;
                    synchronized (caveStaticBaseFiles) {
                        containsKey = caveStaticBaseFiles.containsKey(fileName);
                        if (containsKey) {
                            bundleFile = caveStaticBaseFiles.get(fileName);
                        }
                    }
                    if (!containsKey) {
                        for (String bundle : BundleScanner
                                .getListOfBundles(BUNDLE_LOCALIZATION_DIR)) {
                            file = BundleScanner.searchInBundle(bundle,
                                    BUNDLE_LOCALIZATION_DIR, fileName);
                            if (file != null && file.exists()) {
                                bundleFile = file;
                                break;
                            }
                        }
                        synchronized (caveStaticBaseFiles) {
                            caveStaticBaseFiles.put(fileName, bundleFile);
                        }
                    }
                    return bundleFile;
                }
            }
        } else if (level != LocalizationLevel.UNKNOWN) {
            String baseDir = FileUtil.join(LocalizationManager.getUserDir(),
                    typeDir, levelDir);
            if (context.getContextName() != null) {
                baseDir = FileUtil.join(baseDir, context.getContextName());
            }
            return new LocalizationInternalFile(
                    FileUtil.join(baseDir, fileName));
        } else {
            throw new IllegalArgumentException(
                    "Unsupported localization level: "
                            + context.getLocalizationLevel());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.raytheon.uf.common.localization.ILocalizationAdapter#
     * getLocalizationMetadata
     * (com.raytheon.uf.common.localization.LocalizationContext[],
     * java.lang.String)
     */
    @Override
    public ListResponse[] getLocalizationMetadata(
            LocalizationContext[] context, String fileName)
            throws LocalizationOpFailedException {
        List<LocalizationContext> serverContexts = new ArrayList<LocalizationContext>(
                context.length);
        List<LocalizationContext> localContexts = new ArrayList<LocalizationContext>(
                context.length);
        for (LocalizationContext ctx : context) {
            if (isCaveConfigBase(ctx)) {
                // No need to check CAVE_CONFIG - BASE as they are locally
                // available and are not "protectable"
                localContexts.add(ctx);
            } else {
                serverContexts.add(ctx);
            }
        }

        List<ListResponse> responses = new ArrayList<ListResponse>(
                context.length);

        if (serverContexts.isEmpty() == false) {
            List<ListResponseEntry[]> entriesList = LocalizationManager
                    .getInstance().getListResponseEntry(context, fileName,
                            false, false);

            for (int i = 0; i < context.length; i++) {
                ListResponseEntry[] entries = entriesList.get(i);
                if (entries != null) {
                    for (ListResponseEntry entry : entries) {
                        if (entry.getFileName().equals(fileName)) {
                            responses.add(convertResponse(entry, context[i]));
                            break;
                        }
                    }
                }
            }
        }

        for (LocalizationContext caveConfigBase : localContexts) {
            ListResponse response = new ListResponse();
            response.checkSum = null;
            response.context = caveConfigBase;
            response.date = null;
            response.existsOnServer = false;
            response.fileName = fileName;
            response.protectedLevel = null;
            File file = getPath(caveConfigBase, fileName);
            response.isDirectory = file != null && file.isDirectory();
            responses.add(response);
        }

        return responses.toArray(new ListResponse[responses.size()]);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#retrieve(com
     * .raytheon.uf.common.localization.LocalizationFile)
     */
    @Override
    public void retrieve(LocalizationFile file)
            throws LocalizationOpFailedException {
        LocalizationContext context = file.getContext();
        // cave_static.base and cave_config.base is baselined locally, not on
        // the server

        if (isCaveConfigBase(context) || isCaveStaticBase(context)) {
            return;
        }

        LocalizationManager.getInstance().retrieve(file);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#save(com.raytheon
     * .uf.common.localization.LocalizationFile. ModifiableLocalizationFile)
     */
    @Override
    public boolean save(ModifiableLocalizationFile file)
            throws LocalizationOpFailedException {
        File localFile = file.getLocalFile();
        if (localFile.isDirectory() == false && localFile.exists()) {
            InputStream in = null;
            try {
                in = new LockingFileInputStream(localFile);
                long serverModTime = LocalizationManager.getInstance().upload(
                        file.getContext(), file.getFileName(), in,
                        localFile.length());
                // Success! set potentially changed fields
                file.setTimeStamp(new Date(serverModTime));
                file.setIsAvailableOnServer(true);
                file.setIsDirectory(false);
                return true;
            } catch (FileNotFoundException e) {
                throw new LocalizationOpFailedException(
                        "Error saving, file does not exist");
            } finally {
                // Make sure to close input stream
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // Ignore close exception
                    }
                }
            }
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @seecom.raytheon.uf.common.localization.ILocalizationAdapter#
     * getLocalSearchHierarchy
     * (com.raytheon.uf.common.localization.LocalizationContext
     * .LocalizationType)
     */
    @Override
    public LocalizationContext[] getLocalSearchHierarchy(LocalizationType type) {
        synchronized (contexts) {
            LocalizationContext[] ctx = contexts.get(type);
            LocalizationLevel[] levels = getAvailableLevels();

            if (ctx != null) {
                // Check for new available levels
                Set<LocalizationLevel> levelSet = new HashSet<LocalizationLevel>(
                        Arrays.asList(levels));
                for (LocalizationContext context : ctx) {
                    if (levelSet.contains(context.getLocalizationLevel()) == false) {
                        // New level detected, regenerate search hierarchy
                        ctx = null;
                        break;
                    }
                }
            }

            if (ctx == null) {
                Arrays.sort(levels, LocalizationLevel.REVERSE_COMPARATOR);

                ctx = new LocalizationContext[levels.length];
                for (int i = 0; i < levels.length; ++i) {
                    ctx[i] = getContext(type, levels[i]);
                }

                contexts.put(type, ctx);
            }

            // return a copy for safety in case someone messes with references
            // to the returned values
            LocalizationContext[] cloned = new LocalizationContext[ctx.length];
            for (int i = 0; i < ctx.length; i++) {
                cloned[i] = (LocalizationContext) ctx[i].clone();
            }
            return cloned;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#getStaticContexts
     * ()
     */
    @Override
    public LocalizationType[] getStaticContexts() {
        return new LocalizationType[] { LocalizationType.CAVE_STATIC,
                LocalizationType.COMMON_STATIC };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#listDirectory
     * (com.raytheon.uf.common.localization.LocalizationContext,
     * java.lang.String)
     */
    @Override
    public ListResponse[] listDirectory(LocalizationContext[] contexts,
            String path, boolean recursive, boolean filesOnly)
            throws LocalizationOpFailedException {
        Set<String> addedFiles = new HashSet<String>();
        Set<LocalizationContext> serverContexts = new LinkedHashSet<LocalizationContext>(
                contexts.length);
        Set<LocalizationContext> localContexts = new LinkedHashSet<LocalizationContext>(
                contexts.length);
        for (LocalizationContext context : contexts) {
            if (isCaveStaticBase(context)) {
                localContexts.add(context);
                if (context.getContextName() == null) {
                    // Also search bundles if CAVE_STATIC without context name
                    for (String bundle : BundleScanner
                            .getListOfBundles(BUNDLE_LOCALIZATION_DIR)) {
                        localContexts.add(new LocalizationContext(context
                                .getLocalizationType(), context
                                .getLocalizationLevel(), bundle));
                    }
                }
            } else if (isCaveConfigBase(context)) {
                localContexts.add(context);
            } else {
                serverContexts.add(context);
            }
        }

        List<ListResponse> responses = new ArrayList<ListResponse>(
                contexts.length);

        LocalizationManager manager = LocalizationManager.getInstance();
        if (serverContexts.isEmpty() == false) {
            List<ListResponseEntry[]> entryList = manager.getListResponseEntry(
                    serverContexts
                            .toArray(new LocalizationContext[serverContexts
                                    .size()]), path, recursive, filesOnly);
            for (ListResponseEntry[] lre : entryList) {
                for (ListResponseEntry entry : lre) {
                    String id = entry.getContext().toString() + ":"
                            + entry.getFileName();
                    responses.add(convertResponse(entry, entry.getContext()));
                    addedFiles.add(id);
                }
            }
        }

        String currentSite = manager.getCurrentSite();
        Map<LocalizationContext, File> map = new HashMap<LocalizationContext, File>();
        // Union results from server with local filesystem
        List<ProtectedFileCommand> commands = new ArrayList<ProtectedFileCommand>();
        for (LocalizationContext context : localContexts) {
            // No need to check CAVE_CONFIG - BASE as they are not "protectable"
            File file = getPath(context, "");
            if (file == null || file.exists() == false) {
                continue;
            }
            map.put(context, file);
            List<String> paths = buildPaths(path, file, recursive, filesOnly);
            for (String p : paths) {
                if (context.getLocalizationType() == LocalizationType.CAVE_STATIC) {
                    String id = context.toString() + ":" + p;
                    if (addedFiles.contains(id) == false) {
                        ProtectedFileCommand cmd = new ProtectedFileCommand();
                        cmd.setContext(context);
                        cmd.setSubPath(p);
                        cmd.setLocalizedSite(currentSite);
                        commands.add(cmd);
                    }
                } else {
                    // CAVE_CONFIG - BASE, add entry
                    File configFile = new File(file, p);
                    ListResponse response = new ListResponse();
                    response.context = context;
                    response.isDirectory = configFile.isDirectory();
                    response.protectedLevel = null;
                    response.existsOnServer = false;
                    response.fileName = p;
                    response.date = new Date(configFile.lastModified());
                    responses.add(response);
                }
            }
        }

        if (commands.isEmpty() == false) {
            UtilityRequestMessage msg = new UtilityRequestMessage(
                    commands.toArray(new AbstractUtilityCommand[commands.size()]));

            AbstractUtilityResponse[] rsps = manager.makeRequest(msg);

            for (AbstractUtilityResponse rsp : rsps) {
                ProtectedFileResponse pfr = (ProtectedFileResponse) rsp;
                File file = map.get(pfr.getContext());
                File locFile = new File(file + File.separator
                        + pfr.getPathName());
                ListResponse response = new ListResponse();
                response.context = pfr.getContext();
                response.date = new Date(locFile.lastModified());
                response.existsOnServer = false;
                response.fileName = pfr.getPathName();
                response.isDirectory = locFile.isDirectory();
                response.protectedLevel = pfr.getProtectedLevel();

                if (response.protectedLevel == null
                        || response.context.getLocalizationLevel().compareTo(
                                response.protectedLevel) <= 0) {
                    // if not protected or protected level is less than/equal to
                    // our level, add response
                    responses.add(response);
                }
            }
        }

        return responses.toArray(new ListResponse[responses.size()]);

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#delete(com.raytheon
     * .uf.common.localization.LocalizationFile. ModifiableLocalizationFile)
     */
    @Override
    public boolean delete(ModifiableLocalizationFile file)
            throws LocalizationOpFailedException {
        long deleteTime = LocalizationManager.getInstance().delete(
                file.getContext(), file.getFileName());

        // Made it here! file on server succesfully deleted! Delete local file
        // reference. If that fails, doesn't matter since file does not exist!
        File localFile = file.getLocalFile();
        localFile.delete();

        // Reset fields
        file.setTimeStamp(new Date(deleteTime));
        file.setIsAvailableOnServer(false);
        file.setFileChecksum(null);
        file.setIsDirectory(false);

        return true;
    }

    private ListResponse convertResponse(ListResponseEntry entry,
            LocalizationContext context) {
        ListResponse lr = new ListResponse();
        lr.date = entry.getDate();
        lr.fileName = entry.getFileName();
        lr.checkSum = entry.getChecksum();
        if (isCaveConfigBase(context) || isCaveStaticBase(context)) {
            File file = getPath(context, lr.fileName);
            lr.isDirectory = file != null && file.isDirectory();
        } else {
            lr.isDirectory = entry.isDirectory();
        }
        lr.context = context;
        lr.protectedLevel = entry.getProtectedLevel();
        lr.existsOnServer = entry.isExistsOnServer();
        return lr;
    }

    @Override
    public LocalizationContext getContext(LocalizationType type,
            LocalizationLevel level) {
        String contextName = null;
        if (level == LocalizationLevel.BASE) {
            // nothing to add
        } else {
            contextName = LocalizationManager.getContextName(level);
        }
        LocalizationContext ctx = new LocalizationContext(type, level,
                contextName);
        return ctx;
    }

    /**
     * Recursive method that builds a list of files and directories that are
     * inside of this directory.
     * 
     * @param prefix
     *            the prefix (set to "") for the first recursive call
     * @param startingFile
     *            the starting file
     * @return
     */
    protected List<String> buildPaths(String prefix, File startingFile,
            boolean recursive, boolean filesOnly) {
        List<String> results = new ArrayList<String>();

        String path = startingFile.getPath();
        if ((prefix == null) || prefix.isEmpty()) {
            prefix = ".";
        } else {
            path += "/" + prefix;
        }
        String prependToPath = prefix + "/";

        File file = new File(path);

        if (!file.exists()) {
            return results;
        }

        if (!file.canRead() || file.isHidden()) {
            return results;
        }

        if (!filesOnly || file.isFile()) {
            results.add(prefix);
        }

        if (file.isDirectory()) {
            try { // Win32: listFiles may return null for an empty directory,
                  // Win32: Make all paths use '/'
                for (File f : file.listFiles()) {
                    if (!f.canRead() || f.isHidden()) {
                        continue;
                    }
                    if (f.isFile()) {
                        results.add(prependToPath
                                + FileUtil.edexPath(f.getName()));
                    } else if (f.isDirectory()) {
                        if (recursive) {
                            results.addAll(buildPaths(
                                    prependToPath
                                            + FileUtil.edexPath(f.getName()),
                                    startingFile, recursive, filesOnly));
                        } else if (!filesOnly) {
                            results.add(prependToPath
                                    + FileUtil.edexPath(f.getName()));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(e.toString() + " listing " + file.getPath());
            }
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#getUserSiteList
     * ()
     */
    @Override
    public String[] getContextList(LocalizationLevel level)
            throws LocalizationOpFailedException {
        List<ListResponseEntry[]> entriesList = LocalizationManager
                .getInstance().getContextList(level);
        Set<String> fileList = new HashSet<String>();
        for (ListResponseEntry[] entryArr : entriesList) {
            for (ListResponseEntry entry : entryArr) {
                fileList.add(entry.getContext().getContextName());
            }
        }

        return fileList.toArray(new String[fileList.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#getAvailableLevels
     * ()
     */
    @Override
    public LocalizationLevel[] getAvailableLevels() {
        LocalizationLevel[] levels = LocalizationLevel.values();
        List<LocalizationLevel> available = new ArrayList<LocalizationLevel>(
                levels.length);
        for (LocalizationLevel level : levels) {
            String contextName = LocalizationManager.getContextName(level);
            if (contextName != null || level == LocalizationLevel.BASE) {
                available.add(level);
            }
        }
        return available.toArray(new LocalizationLevel[available.size()]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.localization.ILocalizationAdapter#exists(com.raytheon
     * .uf.common.localization.LocalizationFile)
     */
    @Override
    public boolean exists(LocalizationFile file) {
        if (file.isAvailableOnServer()) {
            // Simple case, on server
            return true;
        }

        // File exists if it is on the server OR it is BASE and CAVE_STATIC or
        // CAVE_CONFIG and File.exists()
        LocalizationType type = file.getContext().getLocalizationType();
        return file.getFile().exists()
                && file.getContext().getLocalizationLevel() == LocalizationLevel.BASE
                && (type == LocalizationType.CAVE_CONFIG || type == LocalizationType.CAVE_STATIC);
    }
}
