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
package com.raytheon.edex.utility;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ArrayUtils;

import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationAdapter;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationFileKey;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * TODO: Should we be sending out FileUpdateMessages for save/delete?
 *
 * Provides unified methods of accessing localization files from EDEX code.
 * <p>
 * Methods provided in this class are tailored to support paths to files from
 * EDEX specific code.
 * </p>
 * <p>
 * Some methods defined in the {@link ILocalizationAdapter} interface are not
 * useful from the EDEX side since all files are locally stored. If called, the
 * useless methods will no op.
 * </p>
 *
 * TODO: This class is in serious need of rework. Either the UtilityManager
 * should make use of this class through the IPathManager, or this class should
 * make use of the UtilityManager. The two classes serve somewhat redundant
 * purposes while having entirely different APIs and in some cases, inconsistent
 * behavior.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Jul 11, 2008 1250        jelkins     Initial creation
 * Mar 14, 2013 1794        djohnson    FileUtil.listFiles now returns List.
 * Nov 03, 2013 2511        mnash       Fix issue where if name occurs in path
 *                                      file won't be returned correctly
 * Feb 13, 2014             mnash       Add region level to localization
 * Jul 10, 2014 2914        garmendariz Remove EnvProperties
 * Jul 21, 2014 2768        bclement    added notification in save() and delete()
 * Jul 24, 2014 3378        bclement    added createCache()
 * Jul 25, 2014 3378        bclement    removed uf prefix from system property
 * Nov 13, 2014 4953        randerso    Changed delete() to also remove .md5 file
 * Feb 16, 2015 3978        njensen     listDirectory() no longer includes .md5 files
 * Feb 18, 2015 3978        njensen     no max size on cache map is safer
 * Nov 17, 2015 4834        njensen     Remove ModifiableLocalizationFile
 *                                       Set checksum on list response entries
 * Nov 30, 2015 4834        njensen     Removed references to LocalizationOpFailedException
 * Jan 21, 2016 4834        njensen     Fixed checksum and timestamp notification on save()
 * Apr 26, 2017 6258        tgurney     Set file/dir permissions on save
 * Jun 22, 2017 6339        njensen     Updated listDirectory() method signature
 *                                      Use FilenameFilter since we now have an extension
 * Jun 30, 2017 6316        njensen     Improved regions.xml debug message
 * Aug 04, 2017 6379        njensen     Removed protected-ness from responses
 * Aug 07, 2017 5731        bsteffen    Implement getContextList
 * Sep  8, 2017 6255        tgurney     Check ownership before setting permissions
 * Feb 04, 2019             mjames@ucar Remove REGION and WORKSTATION levels.
 *
 * </pre>
 *
 * @author jelkins
 */

public class EDEXLocalizationAdapter implements ILocalizationAdapter {

    private static final int CACHE_SIZE = Integer
            .getInteger("edex.localization.cache.size", 2048);

    private static final IUFStatusHandler handler = UFStatus
            .getHandler(EDEXLocalizationAdapter.class);

    private static final String CHECKSUM_FILE_EXTENSION = ".md5";

    private final Map<LocalizationType, LocalizationContext[]> contexts;

    private static final String FILE_UPDATE_ENDPOINT = "utilityNotify";

    /**
     * Constructs this class
     */
    public EDEXLocalizationAdapter() {
        this.contexts = new HashMap<>();
    }

    @SuppressWarnings("null")
	@Override
    public LocalizationContext[] getLocalSearchHierarchy(
            LocalizationType type) {
    	
        synchronized (this.contexts) {
            LocalizationContext[] ctx = this.contexts.get(type);
            if (ctx == null) {
                ctx = new LocalizationContext[3];
                ctx[0] = getContext(type, LocalizationLevel.SITE);
                ctx[1] = getContext(type, LocalizationLevel.CONFIGURED);
                ctx[2] = getContext(type, LocalizationLevel.BASE);
                this.contexts.put(type, ctx);
            }
	    return ctx;
            // return a copy for safety in case someone messes with references
            // to the returned values
            //LocalizationContext[] cloned = new LocalizationContext[ctx.length];
            //for (int i = 0; i < ctx.length; i++) {
            //    cloned[i] = (LocalizationContext) ctx[i].clone();
            //}
            //return cloned;
        }
    }

    protected String getSiteName() {
        String site = EDEXUtil.getEdexSite();

        if (site == null || site.isEmpty()) {
            site = "none";
        }
        return site;
    }

    /**
     * Obtain file or directory metadata
     * <p>
     * Populate a ListResponse Array with metadata pertaining to the given file
     * or directory within the given contexts
     * </p>
     *
     * @param context
     *            the contexts in which to obtain metadata for the given file or
     *            directory
     *
     * @param fileName
     *            the file or directory for which to obtain metadata
     *
     * @return a ListResponse Array where each entry details metadata for the
     *         given file or directory in the given contexts
     *
     * @see com.raytheon.uf.common.localization.ILocalizationAdapter#getLocalizationMetadata(com.raytheon.uf.common.localization.LocalizationContext[],
     *      java.lang.String)
     */
    @Override
    public ListResponse[] getLocalizationMetadata(LocalizationContext[] context,
            String fileName) throws LocalizationException {

        List<ListResponse> contents = new ArrayList<>(context.length);

        for (LocalizationContext ctx : context) {
            ListResponse entry = createListResponse(ctx, fileName,
                    getPath(ctx, fileName));
            contents.add(entry);
        }

        return contents.toArray(new ListResponse[0]);
    }

    @Override
    public File getPath(LocalizationContext context, String fileName) {

        File utilityDir = getUtilityDir();

        if (context.getLocalizationLevel() == LocalizationLevel.UNKNOWN) {
            throw new IllegalArgumentException("Unsupported localization level:"
                    + context.getLocalizationLevel());
            // } else if (false) {
            // TODO: Check for invalid type / level combinations
            // Change the above condition and add invalid type / level checking
            // if needed
        }

        File baseDir = new File(utilityDir, context.toPath());

        return new File(baseDir, fileName);
    }

    @Override
    public LocalizationType[] getStaticContexts() {
        LocalizationType[] type = new LocalizationType[] {
                LocalizationType.EDEX_STATIC, LocalizationType.COMMON_STATIC };
        return type;
    }

    /**
     * Get the file reference to the utility directory.
     *
     * @return the file reference to the utility directory
     */
    protected File getUtilityDir() {
        return new File(EDEXUtil.getEdexUtility());
    }

    /**
     * Create ListResponse metadata
     * <p>
     * Populate a ListResponce data object with file context, name, data
     * modified, directory flag, and MD5 checksum.
     * </p>
     *
     * @param ctx
     *            the context to use when looking up the metadata for the
     *            filename
     * @param file
     *            the file for which to find metadata
     * @return a ListResponse containing metadata information. If the file does
     *         not exist calling the following fields of ListResponse will
     *         return:
     *         <ul>
     *         <li>directory: false</li>
     *         <li>date: 0ms since epoch</li>
     *         <li>checksum: null</li>
     *         </ul>
     */
    private ListResponse createListResponse(LocalizationContext ctx,
            String basePath, File file) {
        ListResponse entry = new ListResponse();

        entry.isDirectory = file.isDirectory();
        entry.context = ctx;
        String fullPath = file.getAbsolutePath();
        String path = getUtilityDir() + File.separator + ctx.toPath()
                + File.separator;
        if (fullPath.length() > path.length()) {
            entry.fileName = fullPath.replaceFirst(path, "");
            if (entry.fileName.startsWith(File.separator)) {
                entry.fileName = entry.fileName.substring(1);
            }
        } else {
            /*-
             * This case hits for the root where
             * fullPath = /a2/edex/data/utility/common_static/base
             * path = /a2/edex/data/utility/common_static/base/
             */
            entry.fileName = path.replaceFirst(fullPath, "");
            if (entry.fileName.startsWith(File.separator)) {
                entry.fileName = entry.fileName.substring(1);
            }
        }
        entry.date = new Date(file.lastModified());
        entry.existsOnServer = file.exists();
        entry.checkSum = ChecksumIO.getFileChecksum(file);

        return entry;
    }

    @Override
    public ListResponse[] listDirectory(LocalizationContext[] context,
            String path, String fileExtension, boolean recursive,
            boolean filesOnly) throws LocalizationException {

        // use the Set datatype to ensure no duplicate entries, use linked to
        // ensure order is deterministic when scanning multiple contexts
        Set<ListResponse> contents = new LinkedHashSet<>();

        FilenameFilter filter = null;
        if (fileExtension != null) {
            filter = new FilenameFilter() {

                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(fileExtension);
                }

            };
        }

        for (LocalizationContext ctx : context) {
            List<File> fileList = com.raytheon.uf.common.util.FileUtil
                    .listFiles(getPath(ctx, path), filter, recursive);

            for (File file : fileList) {

                if (file.isDirectory() && filesOnly
                        || file.getName().endsWith(CHECKSUM_FILE_EXTENSION)) {
                    // skip
                } else {
                    ListResponse entry = createListResponse(ctx, path, file);
                    contents.add(entry);
                }
            }

        }

        return contents.toArray(new ListResponse[0]);

    }

    // --- CRUD Operations ---------------------------------------------------

    @Override
    public boolean save(LocalizationFile file) throws LocalizationException {
        LocalizationContext context = file.getContext();
        if (context.getLocalizationLevel().equals(LocalizationLevel.BASE)) {
            throw new UnsupportedOperationException(
                    "Saving to the BASE context is not supported.");
        }

        /*
         * TODO Verify file's pre-modification checksum is the non-existent file
         * checksum or matches the server file's current checksum. If not, throw
         * LocalizationFileVersionConflictException.
         *
         * Note there is some risk to implementing this todo, as any code that
         * keeps a reference to an ILocalizationFile and calls save() on it
         * repeatedly will then trigger the conflict exception on each
         * subsequent save. That code would need to be updated to make use of
         * the ILocalizationFile instance with the new checksum after each save.
         */

        FileChangeType changeType;
        if (file.isAvailableOnServer()) {
            changeType = FileChangeType.UPDATED;
        } else {
            changeType = FileChangeType.ADDED;
        }

        // Set restricted permissions on the file
        try {
            File theFile = getPath(file.getContext(), file.getPath());
            Path p = theFile.toPath();
            Set<PosixFilePermission> permsToSet;
            if (theFile.isDirectory()) {
                permsToSet = LocalizationFile.DIR_PERMISSIONS;
            } else {
                permsToSet = LocalizationFile.FILE_PERMISSIONS;
            }
            String owner = Files.getOwner(p).getName();
            if (owner.equals(System.getProperty("user.name"))) {
                Files.setPosixFilePermissions(p, permsToSet);
            } else {
                handler.info("Not changing permissions for " + p
                        + " because it is owned by user " + owner);
            }
        } catch (Exception e) {
            handler.error("Error setting permissions on " + file, e);
        }

        // send notification
        try {
            // generate the new checksum after the change
            File actualFile = getPath(file.getContext(), file.getPath());
            String checksum = ChecksumIO.writeChecksum(actualFile);
            long timeStamp = actualFile.lastModified();

            EDEXUtil.getMessageProducer().sendAsync(FILE_UPDATE_ENDPOINT,
                    new FileUpdatedMessage(context, file.getPath(), changeType,
                            timeStamp, checksum));
        } catch (Exception e) {
            handler.error("Error sending file updated message", e);
        }
        return true;
    }

    @Override
    public boolean delete(LocalizationFile file) throws LocalizationException {

        /*
         * TODO Verify checksum on filesystem matches checksum sent from delete
         * request, otherwise throw LocalizationFileVersionConflictException.
         */

        File localFile = getPath(file.getContext(), file.getPath());
        boolean deleted = false;
        if (localFile.exists()) {
            deleted = localFile.delete();
        }

        File md5File = new File(
                localFile.getAbsolutePath() + CHECKSUM_FILE_EXTENSION);
        if (md5File.exists()) {
            if (!md5File.delete()) {
                handler.error("Unable to delete: " + md5File.getAbsolutePath());
            }
        }

        if (deleted) {
            long timeStamp = System.currentTimeMillis();
            LocalizationContext context = file.getContext();
            // send notification
            try {
                EDEXUtil.getMessageProducer().sendAsync(FILE_UPDATE_ENDPOINT,
                        new FileUpdatedMessage(context, file.getPath(),
                                FileChangeType.DELETED, timeStamp,
                                ILocalizationFile.NON_EXISTENT_CHECKSUM));
            } catch (Exception e) {
                handler.error("Error sending file updated message", e);
            }
        }
        return deleted;
    }

    @Override
    public LocalizationContext getContext(LocalizationType type,
            LocalizationLevel level) {

        String contextName = null;
        if (level == LocalizationLevel.BASE) {
            // nothing to add
        } else if (level == LocalizationLevel.SITE || level == LocalizationLevel.CONFIGURED) {
            // fill in site name
            contextName = getSiteName();
        } else {
            // EDEX has no concept of current user or personality
            contextName = "none";
        }

        LocalizationContext ctx = new LocalizationContext(type, level,
                contextName);
        return ctx;
    }

    @Override
    public String[] getContextList(LocalizationLevel level) {
        /* Sorting is not required but alphabetical just looks nicer. */
        Set<String> results = new TreeSet<>();
        File utilityDir = getUtilityDir();

        for (LocalizationType type : getStaticContexts()) {
            File typeDir = new File(utilityDir, type.name().toLowerCase());
            File levelDir = new File(typeDir, level.name().toLowerCase());

            File[] files = levelDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        results.add(file.getName());
                    }
                }

            }
        }

        return results.toArray(new String[0]);

    }

    @Override
    public void retrieve(LocalizationFile file) throws LocalizationException {
        // do nothing
    }

    @Override
    public LocalizationLevel[] getAvailableLevels() {
        LocalizationLevel[] levels = new LocalizationLevel[] {
                LocalizationLevel.BASE, LocalizationLevel.CONFIGURED, LocalizationLevel.SITE };

        return levels;
    }

    @Override
    public boolean exists(LocalizationFile file) {
        return getPath(file.getContext(), file.getPath()).exists();
    }

    @Override
    public Map<LocalizationFileKey, LocalizationFile> createCache() {
        /*
         * Intentionally no max size on this cache, so eventually if you had
         * 100% EDEX uptime and more and more localization files kept getting
         * added to the system and then requested by clients, this could use up
         * a significant amount of memory. However, the disconnect between
         * LocalNotificationObserver's cache and PathManager's cache will cause
         * issues if anything is ever evicted from the cache. Unless that is
         * sorted out and simplified, no values should be evicted from this map.
         */
        return new ConcurrentHashMap<>(CACHE_SIZE);
    }

}
