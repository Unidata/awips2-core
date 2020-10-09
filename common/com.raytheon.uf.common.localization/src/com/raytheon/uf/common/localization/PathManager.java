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
package com.raytheon.uf.common.localization;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.lang3.Validate;

import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationAdapter.ListResponse;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.msgs.ListResponseEntry;
import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * A generalized implementation for interfacing with LocalizationFiles.
 *
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/12/2008              chammack    Initial Creation.
 * Oct 23, 2012 1322       djohnson    Allow test code in the same package to clear fileCache.
 * Jul 24, 2014 3378       bclement    cache implementation provided by localization adapter
 * Jul 25, 2014 3378       bclement    implements ILocalizationFileObserver
 * Sep 08, 2014 3592       randerso    Added single type listStaticFiles,
 *                                     getStaticLocalizationFile, and getStaticFile APIs
 * Feb 17, 2015 4137       reblum      no longer implements ILocalizationFileObserver
 * Aug 24, 2015 4393       njensen     Added field observer
 * Oct 14, 2015 4410       bsteffen    listStaticFiles will now merge different types.
 * Nov 12, 2015 4834       njensen     PathManager takeover of watching for localization file changes
 * Jan 28, 2016 4834       njensen     Pass along FileChangeType to old style observers
 * Jun 21, 2016 5695       njensen     Clear parent directories from cache on file add
 * Aug 15, 2016 5834       njensen     Reuse protected file level in fireListeners()
 * Jun 22, 2017 6339       njensen     listFiles() now has an eager filter
 * Aug 04, 2017 6379       njensen     Updated LocalizationFile constructor signature
 * Dec 07, 2017 6355       nabowle     Normalize observed paths that end with SEPARATOR.
 *
 * </pre>
 *
 * @author chammack
 */
public class PathManager implements IPathManager {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PathManager.class, "Localization");

    private static final Comparator<LocalizationContext> LOCALIZATION_CTX_COMPARATOR = new Comparator<LocalizationContext>() {

        @Override
        public int compare(LocalizationContext o1, LocalizationContext o2) {
            return LocalizationLevel.REVERSE_COMPARATOR.compare(
                    o1.getLocalizationLevel(), o2.getLocalizationLevel());
        }
    };

    /**
     * Cache of LocalizationFile instances that hold metadata (checksum,
     * timestamp, etc) about the files.
     */
    protected final Map<LocalizationFileKey, LocalizationFile> fileCache;

    protected final ILocalizationAdapter adapter;

    protected final ConcurrentMap<String, Collection<ILocalizationPathObserver>> listenerMap;

    PathManager(ILocalizationAdapter adapter) {
        this.adapter = adapter;
        this.fileCache = adapter.createCache();
        this.listenerMap = new ConcurrentHashMap<>();
    }

    @Override
    public File getStaticFile(String name) {
        LocalizationFile locFile = getStaticLocalizationFile(name);
        File file = null;
        if (locFile != null) {
            file = locFile.getFile();
        }

        return file;
    }

    @Override
    public File getStaticFile(LocalizationType type, String name) {
        LocalizationFile locFile = getStaticLocalizationFile(type, name);
        File file = null;
        if (locFile != null) {
            file = locFile.getFile();
        }

        return file;
    }

    @Override
    public LocalizationFile getStaticLocalizationFile(LocalizationType type,
            String name) {
        Validate.notNull(name, "Path name must not be null");
        return this.getStaticLocalizationFile(new LocalizationType[] { type },
                name);
    }

    @Override
    public LocalizationFile getStaticLocalizationFile(String name) {
        Validate.notNull(name, "Path name must not be null");
        LocalizationType[] types = this.adapter.getStaticContexts();
        return this.getStaticLocalizationFile(types, name);
    }

    private LocalizationFile getStaticLocalizationFile(LocalizationType[] types,
            String name) {
        Validate.notNull(name, "Path name must not be null");
        name = name.replace(File.separator, IPathManager.SEPARATOR);

        // Iterate through the types
        List<LocalizationContext> contexts = new ArrayList<>();
        for (LocalizationType type : types) {
            // Iterate through the hierarchy
            LocalizationContext[] searchContexts = this.adapter
                    .getLocalSearchHierarchy(type);

            contexts.addAll(java.util.Arrays.asList(searchContexts));
        }

        LocalizationFile[] files = getLocalizationFile(
                contexts.toArray(new LocalizationContext[contexts.size()]),
                name);

        for (LocalizationFile file : files) {
            if ((file != null) && file.exists()) {
                // First file found in hierarchy is used
                return file;
            }
        }

        return null;
    }

    /*
     * FIXME: This method was only added because of limitations in
     * EDEXLocalizationAdapter--it isn't able to properly search for files on
     * systems that have multiple sites activated on EDEX. It can only search
     * the primary site's (or AW_SITE_IDENTIFIER) contexts. However, GFE will
     * frequently need to search through one of the non-primary site's
     * localization store. If the localization API and specifically the
     * EDEXLocalizationApdater gets smartened up enough to handle multi-domain
     * systems, this method should be able to go away.
     */
    @Override
    public LocalizationFile getStaticLocalizationFile(
            LocalizationContext[] contexts, String name) {
        Validate.notNull(contexts, "Search contexts must not be null");
        Validate.notNull(name, "Path name must not be null");

        LocalizationContext[] searchContexts = new LocalizationContext[contexts.length];
        System.arraycopy(contexts, 0, searchContexts, 0, contexts.length);
        Arrays.sort(searchContexts, LOCALIZATION_CTX_COMPARATOR);

        name = name.replace(File.separator, IPathManager.SEPARATOR);

        LocalizationFile[] files = getLocalizationFile(searchContexts, name);

        for (LocalizationFile file : files) {
            if ((file != null) && file.exists()) {
                // First file found in hierarchy is used
                return file;
            }
        }

        return null;
    }

    @Override
    public File getFile(LocalizationContext context, String name) {
        Validate.notNull(context, "Context must not be null");
        Validate.notNull(name, "Path name must not be null");

        LocalizationFile file = getLocalizationFile(context, name);
        return file != null ? file.getFile() : null;
    }

    @Override
    public Map<LocalizationLevel, LocalizationFile> getTieredLocalizationFile(
            LocalizationType type, String name) {
        Map<LocalizationLevel, LocalizationFile> map = new HashMap<>();

        LocalizationContext[] contexts = getLocalSearchHierarchy(type);

        LocalizationFile[] files = getLocalizationFile(contexts, name);
        for (LocalizationFile lf : files) {
            if ((lf != null) && lf.exists()) {
                map.put(lf.getContext().getLocalizationLevel(), lf);
            }
        }

        return map;
    }

    @Override
    public LocalizationFile getLocalizationFile(LocalizationContext context,
            String name) {
        Validate.notNull(name, "File name string must be provided");
        Validate.notNull(context, "Context must not be null");

        while (name.endsWith("/") || name.endsWith("\\")) { // Win32
            name = name.substring(0, name.length() - 1);
        }
        LocalizationFile[] files = getLocalizationFile(
                new LocalizationContext[] { context }, name);
        for (LocalizationFile file : files) {
            if (file != null) {
                return file;
            }
        }
        statusHandler.debug("getLocalizationFile is returning null for file(s): "
            + Arrays.toString(files));
        return null;
    }

    private LocalizationFile[] getLocalizationFile(
            LocalizationContext[] contexts, String name) {
        name = name.replace(File.separator, IPathManager.SEPARATOR); // Win32

        Map<LocalizationContext, LocalizationFile> availableFiles = new HashMap<>(
                contexts.length * 2);

        List<LocalizationContext> ctxToCheck = new ArrayList<>(contexts.length);
        for (LocalizationContext ctx : contexts) {
            LocalizationFile cached = fileCache
                    .get(new LocalizationFileKey(name, ctx));
            if (cached != null) {
                if (!cached.isNull()) {
                    availableFiles.put(ctx, cached);
                }
            } else {
                ctxToCheck.add(ctx);
            }
        }

        if (!ctxToCheck.isEmpty()) {
            ListResponse[] entry = null;
            try {
                entry = this.adapter.getLocalizationMetadata(
                        ctxToCheck.toArray(
                                new LocalizationContext[ctxToCheck.size()]),
                        name);
            } catch (LocalizationException e) {
                // Error on server, no files will be returned
                statusHandler
                        .error("Error retrieving localization file metadata for "
                                + name, e);
            }

            if (entry != null) {
                synchronized (fileCache) {
                    for (ListResponse lr : entry) {
                        LocalizationFile file = createFromResponse(lr);
                        if (!file.isNull()) {
                            availableFiles.put(file.getContext(), file);
                        }
                    }
                }
            }
        }

        List<LocalizationFile> rval = new ArrayList<>(availableFiles.size());
        for (LocalizationContext ctx : contexts) {
            LocalizationFile file = availableFiles.get(ctx);
            if (file != null) {
                rval.add(file);
            }
        }

        return rval.toArray(new LocalizationFile[rval.size()]);
    }

    /**
     * Creates a LocalizationFile from a {@link ListResponse}, callers need to
     * synchronize on fileCache before calling
     *
     * @param response
     * @return
     */
    private LocalizationFile createFromResponse(ListResponse response) {
        // able to resolve file, lf will be set, check cache
        LocalizationFileKey key = new LocalizationFileKey(response.fileName,
                response.context);
        LocalizationFile lf = fileCache.get(key);
        if (lf == null) {
            // Not in cache
            File file = this.adapter.getPath(response.context,
                    response.fileName);
            if (file != null) {
                // No cache file available but path is resolved, create
                lf = new LocalizationFile(this.adapter, response.context, file,
                        response.date, response.fileName, response.checkSum,
                        response.isDirectory);
            } else {
                // file does not exist
                lf = new LocalizationFile(this.adapter, response.context, null,
                        null, response.fileName,
                        ILocalizationFile.NON_EXISTENT_CHECKSUM, false);
            }
            fileCache.put(key, lf);
        }
        return lf;
    }

    @Override
    public LocalizationFile[] listFiles(LocalizationContext context,
            String name, String[] filter, boolean recursive,
            boolean filesOnly) {
        Validate.notNull(name, "Path name must not be null");
        Validate.notNull(context, "Context must not be null");

        return listFiles(new LocalizationContext[] { context }, name, filter,
                recursive, filesOnly);
    }

    @Override
    public LocalizationFile[] listFiles(LocalizationContext[] contexts,
            String name, String[] filter, boolean recursive,
            boolean filesOnly) {
        try {
            List<LocalizationFile> files = new ArrayList<>();
            String eagerFilter = null;
            if (filter != null && filter.length == 1) {
                eagerFilter = filter[0];
            }
            ListResponse[] entries = this.adapter.listDirectory(contexts, name,
                    eagerFilter, recursive, filesOnly);

            synchronized (fileCache) {
                for (ListResponse entry : entries) {
                    if (entry.isDirectory
                            || matchesExtension(entry.fileName, filter)) {
                        LocalizationFile file = createFromResponse(entry);
                        if (file.exists()) {
                            files.add(file);
                        }
                    }
                }
            }

            return files.toArray(new LocalizationFile[files.size()]);
        } catch (LocalizationException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error listing files: " + e.getLocalizedMessage(), e);
        }

        return null;
    }

    /**
     * Convenience method that checks to see if a file string ends with a set of
     * extensions. If extensions is null, true is returned.
     *
     * @param name
     *            the file name
     * @param extensions
     *            the list of extensions, or null if no filter
     * @return true if the filename matches the filter
     */
    private boolean matchesExtension(String name, String[] extensions) {
        if (extensions == null) {
            return true;
        }

        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public LocalizationFile[] listStaticFiles(LocalizationType type,
            String name, String[] filter, boolean recursive,
            boolean filesOnly) {

        return this.listStaticFiles(new LocalizationType[] { type }, name,
                filter, recursive, filesOnly);
    }

    @Override
    public LocalizationFile[] listStaticFiles(String name, String[] filter,
            boolean recursive, boolean filesOnly) {
        Validate.notNull(name, "Path name must not be null");

        // Iterate through the types
        LocalizationType[] types = this.adapter.getStaticContexts();
        return this.listStaticFiles(types, name, filter, recursive, filesOnly);
    }

    private LocalizationFile[] listStaticFiles(LocalizationType[] types,
            String name, String[] filter, boolean recursive,
            boolean filesOnly) {
        Validate.notNull(name, "Path name must not be null");

        List<LocalizationContext> contexts = new ArrayList<>();
        for (LocalizationType type : types) {
            // Iterate through the hierarchy
            LocalizationContext[] searchContexts = this.adapter
                    .getLocalSearchHierarchy(type);

            contexts.addAll(java.util.Arrays.asList(searchContexts));
        }

        return listStaticFiles(
                contexts.toArray(new LocalizationContext[contexts.size()]),
                name, filter, recursive, filesOnly);
    }

    /*
     * FIXME: This method was only added because of limitations in
     * EDEXLocalizationAdapter--it isn't able to properly search for files on
     * systems that have multiple sites activated on EDEX. It can only search
     * the primary site's (or AW_SITE_IDENTIFIER) contexts. However, GFE will
     * frequently need to search through one of the non-primary site's
     * localization store. If the localization API and specifically the
     * EDEXLocalizationApdater gets smartened up enough to handle multi-domain
     * systems, this method should be able to go away.
     */
    @Override
    public LocalizationFile[] listStaticFiles(LocalizationContext[] contexts,
            String name, String[] filter, boolean recursive,
            boolean filesOnly) {
        Validate.notNull(name, "Path name must not be null");
        Validate.notNull(contexts, "Search contexts must not be null");

        LocalizationContext[] searchContexts = new LocalizationContext[contexts.length];
        System.arraycopy(contexts, 0, searchContexts, 0, contexts.length);
        Arrays.sort(searchContexts, LOCALIZATION_CTX_COMPARATOR);

        LocalizationFile[] files = listFiles(searchContexts, name, filter,
                recursive, filesOnly);
        List<LocalizationFile> filterFiles = new ArrayList<>();

        Map<String, LocalizationFile> filterMap = new HashMap<>();
        for (LocalizationFile file : files) {
            String id = file.getPath();
            id = id.replace("\\", "/"); // Win32
            if (!filterMap.containsKey(id)) {
                filterFiles.add(file);
                filterMap.put(id, file);
            }
        }
        return filterFiles.toArray(new LocalizationFile[filterFiles.size()]);
    }

    @Override
    public LocalizationContext getContext(LocalizationType type,
            LocalizationLevel level) {
        return this.adapter.getContext(type, level);
    }

    @Override
    public LocalizationContext getContextForSite(LocalizationType type,
            String siteId) {
        // TODO: What is this method for? shouldn't there be checking for
        // null/empty siteId?
        LocalizationContext ctx = this.adapter.getContext(type,
                LocalizationLevel.SITE);
        ctx.setContextName(siteId);
        return ctx;
    }

    @Override
    public LocalizationContext[] getLocalSearchHierarchy(
            LocalizationType type) {
        return this.adapter.getLocalSearchHierarchy(type);
    }

    @Override
    public String[] getContextList(LocalizationLevel level) {
        try {
            return this.adapter.getContextList(level);
        } catch (LocalizationException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        return new String[0];
    }

    @Override
    public LocalizationLevel[] getAvailableLevels() {
        return adapter.getAvailableLevels();
    }

    @Override
    public void storeCache(File cacheFile)
            throws IOException, SerializationException {
        Map<SerializableKey, ListResponseEntry> cacheObject = new HashMap<>(
                fileCache.size() * 2);
        for (Map.Entry<LocalizationFileKey, LocalizationFile> entry : fileCache
                .entrySet()) {
            ListResponseEntry lre = new ListResponseEntry();
            LocalizationFile file = entry.getValue();
            lre.setChecksum(file.getCheckSum());
            lre.setContext(file.getContext());
            lre.setDate(file.getTimeStamp());
            lre.setDirectory(file.isDirectory());
            lre.setExistsOnServer(file.isAvailableOnServer());
            lre.setFileName(file.getPath());
            cacheObject.put(new SerializableKey(entry.getKey()), lre);
        }

        FileOutputStream fout = new FileOutputStream(cacheFile);
        try {
            DynamicSerializationManager.getManager(SerializationType.Thrift)
                    .serialize(cacheObject, fout);
        } finally {
            fout.close();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void restoreCache(File cacheFile)
            throws IOException, SerializationException {
        PathManager pm = (PathManager) PathManagerFactory.getPathManager();
        FileInputStream fin = new FileInputStream(cacheFile);
        Map<SerializableKey, ListResponseEntry> cacheObject;
        try {
            cacheObject = (Map<SerializableKey, ListResponseEntry>) DynamicSerializationManager
                    .getManager(SerializationType.Thrift).deserialize(fin);
        } finally {
            fin.close();
        }

        for (Map.Entry<SerializableKey, ListResponseEntry> entry : cacheObject
                .entrySet()) {
            ListResponseEntry lre = entry.getValue();
            SerializableKey key = entry.getKey();
            if (lre.getContext() != null && lre.getFileName() != null) {
                LocalizationFile file = new LocalizationFile(pm.adapter,
                        lre.getContext(),
                        pm.adapter.getPath(lre.getContext(), lre.getFileName()),
                        lre.getDate(), lre.getFileName(), lre.getChecksum(),
                        lre.isDirectory());
                fileCache.put(new LocalizationFileKey(key.getFileName(),
                        key.getContext()), file);
            }
        }
    }

    @DynamicSerialize
    public static class SerializableKey {

        @DynamicSerializeElement
        private String fileName;

        @DynamicSerializeElement
        private LocalizationContext context;

        public SerializableKey() {

        }

        /**
         *
         */
        public SerializableKey(LocalizationFileKey key) {
            this.fileName = key.path;
            this.context = key.context;
        }

        /**
         * @return the fileName
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * @param fileName
         *            the fileName to set
         */
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        /**
         * @return the context
         */
        public LocalizationContext getContext() {
            return context;
        }

        /**
         * @param context
         *            the context to set
         */
        public void setContext(LocalizationContext context) {
            this.context = context;
        }

    }

    @Override
    public void addLocalizationPathObserver(String path,
            ILocalizationPathObserver observer) {
        if (path == null) {
            throw new IllegalArgumentException(
                    "Cannot watch for changes on a null path!");
        }
        if (observer == null) {
            throw new IllegalArgumentException(
                    "Cannot watch for changes with a null observer!");
        }

        // globally watching for all file changes
        if (path.isEmpty()) {
            path = SEPARATOR;
        }

        // remove a trailing SEPARATORs
        while (path.endsWith(SEPARATOR) && !SEPARATOR.equals(path)) {
            path = path.substring(0, path.length() - 1);
        }

        Collection<ILocalizationPathObserver> observers = null;

        observers = listenerMap.get(path);
        if (observers == null) {
            observers = new CopyOnWriteArraySet<>();
            Collection<ILocalizationPathObserver> previous = listenerMap
                    .putIfAbsent(path, observers);
            if (previous != null) {
                /*
                 * this didn't get put in the map, another thread must have
                 * created the collection at roughly the same time
                 */
                observers = previous;
            }
        }

        observers.add(observer);
    }

    @Override
    public void removeLocalizationPathObserver(
            ILocalizationPathObserver observer) {
        for (Collection<ILocalizationPathObserver> value : listenerMap
                .values()) {
            value.remove(observer);
        }
    }

    @Override
    public void removeLocalizationPathObserver(String path,
            ILocalizationPathObserver observer) {
        Collection<ILocalizationPathObserver> observers = listenerMap.get(path);
        if (observers != null) {
            observers.remove(observer);
        }
    }

    /**
     * Fires the listeners for changes to localization files.
     *
     * This method is intentionally not on the IPathManager interface as it
     * should not be a public method. It should only be accessible by the
     * classes listening for the updates (e.g.
     * CAVELocalizationNotificationObserver), however, that is not easily
     * do-able at this time without making it public.
     *
     * @param fum
     */
    public void fireListeners(FileUpdatedMessage fum) {
        String name = fum.getFileName();
        LocalizationFileKey key = new LocalizationFileKey(name,
                fum.getContext());
        LocalizationFile fileInCache = fileCache.get(key);
        if (fileInCache != null
                && fileInCache.getCheckSum().equals(fum.getCheckSum())) {
            // already received this update, don't need to notify again
            return;
        }

        fileCache.remove(key);
        LocalizationFile newInstance = null;
        if (fum.getCheckSum() == null) {
            // ideally we should never get a null checksum but just in case...
            newInstance = getLocalizationFile(fum.getContext(), name);
        } else {
            File localFile = adapter.getPath(fum.getContext(), name);
            newInstance = new LocalizationFile(adapter, fum.getContext(),
                    localFile, new Date(fum.getTimeStamp()), name,
                    fum.getCheckSum(), false);
        }
        fileCache.put(key, newInstance);

        /*
         * Split on separator so if an observer is watching a parent directory
         * that observer will also be triggered.
         */
        String[] split = name.split(SEPARATOR);
        List<String> pathsToCheck = new ArrayList<>(split.length + 1);
        pathsToCheck.add(SEPARATOR);

        StringBuilder parent = new StringBuilder();
        for (String s : split) {
            if (!s.trim().isEmpty()) {
                parent.append(s);
                pathsToCheck.add(parent.toString());
                parent.append(SEPARATOR);
            }
        }

        /*
         * If a file was added, it's possible mkdirs() was called to create
         * parent directories. If those parent directories were in the
         * fileCache, we need to clear them out as we may not get notifications
         * about them.
         */
        if (fum.getChangeType() == FileChangeType.ADDED
                && pathsToCheck.size() > 1) {
            for (int i = 1; i < pathsToCheck.size() - 1; i++) {
                String parentName = pathsToCheck.get(i);
                LocalizationFileKey parentKey = new LocalizationFileKey(
                        parentName, fum.getContext());
                LocalizationFile parentFile = fileCache.get(parentKey);
                if (parentFile != null
                        && ILocalizationFile.NON_EXISTENT_CHECKSUM
                                .equals(parentFile.getCheckSum())) {
                    fileCache.remove(parentKey);
                }
            }
        }

        // notify listeners
        for (String path : pathsToCheck) {
            Collection<ILocalizationPathObserver> listeners = listenerMap
                    .get(path);
            if (listeners != null && !listeners.isEmpty()) {
                for (ILocalizationPathObserver observer : listeners) {
                    if (observer instanceof LocalizationFileIntermediateObserver) {
                        ((LocalizationFileIntermediateObserver) observer)
                                .fileChanged(newInstance, fum.getChangeType());
                    } else {
                        observer.fileChanged(newInstance);
                    }
                }
            }
        }

    }

}
