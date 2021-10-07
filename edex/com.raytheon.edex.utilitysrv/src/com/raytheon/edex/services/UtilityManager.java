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

package com.raytheon.edex.services;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.checksum.ChecksumIO;
import com.raytheon.uf.common.localization.msgs.DeleteUtilityResponse;
import com.raytheon.uf.common.localization.msgs.ListResponseEntry;
import com.raytheon.uf.common.localization.msgs.ListUtilityResponse;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.core.EdexException;

/**
 *
 * Utility manager
 *
 * Provides the business logic for the utility service
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Apr 23, 2007           chammack  Initial Creation.
 * Jul 24, 2007           njensen   Updated putFile()
 * Jul 30, 2007           njensen   Added deleteFile()
 * May 19, 2007  1127     randerso  Implemented error reporting
 * Nov 17, 2015  4834     njensen   Extracted checksum code to ChecksumIO
 * Dec 17, 2015  5166     kbisanz   Update logging to use SLF4J
 * Feb 05, 2016  4754     bsteffen  Use PathManager for checksums
 * Aug 15, 2016  5834     njensen   Always return entry in addEntry, even if
 *                                  file is protected
 * Jun 22, 2017  6339     njensen   Refactored validation of files to reduce
 *                                  calls to underlying filesystem
 * Aug 04, 2017  6379     njensen   Removed protected-ness from responses
 * Oct 08, 2021  8673     randerso  Added AlertViz/log message if a
 *                                  file/directory in the utility tree is
 *                                  unreadable by EDEX.
 *
 * </pre>
 *
 * @author chammack
 */
public class UtilityManager {

    private static final Logger logger = LoggerFactory
            .getLogger(UtilityManager.class);

    public static final String NOTIFY_ID = "utilityNotify";

    // Do not allow instantiation
    private UtilityManager() {

    }

    /**
     *
     * List available files in a context
     *
     * @param baseDir
     *            the base directory
     * @param context
     *            the utility context
     * @param subPath
     *            file or directory path below context
     * @param fileExtension
     *            the file extension to filter on, or null if no filter
     * @param recursive
     *            true if recursive file listing is desired
     * @param filesOnly
     *            true to return only files, false to return directories and
     *            files
     * @return the list response
     */
    public static ListUtilityResponse listFiles(String localizedSite,
            String baseDir, LocalizationContext context, String subPath,
            String fileExtension, boolean recursive, boolean filesOnly) {
        List<ListResponseEntry> entries = new ArrayList<>();
        String msg = null;
        try {
            checkParameters(baseDir, context);
            String path = contextToDirectory(baseDir, context);
            File file = new File(path);

            recursiveFileBuild(localizedSite, context, file, subPath,
                    fileExtension, recursive, filesOnly, entries, 0);
        } catch (@SuppressWarnings("squid:S1166")
        EdexException e) {
            /* Error is sent in the response */
            msg = e.getMessage();
        }

        return new ListUtilityResponse(context, subPath, msg,
                entries.toArray(new ListResponseEntry[entries.size()]));
    }

    /**
     * Deletes a file in a context
     *
     * @param baseDir
     *            the base dir
     * @param context
     *            the context
     * @param fileName
     *            the name of the file
     * @return the delete response
     */
    public static DeleteUtilityResponse deleteFile(String baseDir,
            LocalizationContext context, String fileName) {

        /*
         * TODO verify checksum on filesystem matches checksum sent from delete
         * request, otherwise throw
         * LocalizationFileChangedOutFromUnderYouException aka
         * LocalizationFileVersionConflictException.
         */

        String msg = null;
        try {
            checkParameters(baseDir, context);

            String fullPath = contextToDirectory(baseDir, context)
                    + File.separator + fileName;
            File delFile = new File(fullPath);

            if (delFile.exists()) {
                if (!delFile.delete()) {
                    // Failed to delete file...
                    msg = "File could not be deleted: ";
                    if (delFile.isDirectory() && delFile.list().length > 0) {
                        msg += "Non-empty directory";
                    } else if (!delFile.canWrite()) {
                        msg += "Do not have write permission to file";
                    } else if (delFile.getParentFile() != null
                            && !delFile.getParentFile().canWrite()) {
                        msg += "Do not have write permission to file's parent directory";
                    } else {
                        msg += "Reason unknown";
                    }
                }
                String md5Path = fullPath + ".md5";
                File md5File = new File(md5Path);
                if (md5File.exists()) {
                    md5File.delete();
                }
            }
        } catch (@SuppressWarnings("squid:S1166")
        Exception e) {
            /* Error is sent in the response */
            return new DeleteUtilityResponse(context, e.getMessage(), fileName,
                    System.currentTimeMillis());
        }

        long timeStamp = System.currentTimeMillis();
        // send notification
        try {
            EDEXUtil.getMessageProducer().sendAsync(NOTIFY_ID,
                    new FileUpdatedMessage(context, fileName,
                            FileChangeType.DELETED, timeStamp,
                            ILocalizationFile.NON_EXISTENT_CHECKSUM));
        } catch (Exception e) {
            logger.error("Error sending file updated message", e);
        }

        return new DeleteUtilityResponse(context, msg, fileName, timeStamp);
    }

    /**
     * Sanity check the user-provided parameters
     *
     * @param baseDir
     *            the base directory
     * @param context
     *            the utility context
     * @throws EdexException
     */
    private static void checkParameters(String baseDir,
            LocalizationContext context) throws EdexException {
        File baseDirFile = new File(baseDir);

        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }

        if (!baseDirFile.isDirectory()) {
            throw new EdexException(
                    "Utility service base directory does not contain a directory: "
                            + baseDir);
        }

        if (context == null) {
            throw new EdexException("Context is null");
        }
    }

    private static String contextToDirectory(String baseDir,
            LocalizationContext context) {
        String dir = baseDir + File.separator + context.toPath();

        return dir;
    }

    private static void addEntry(String localizedSite,
            LocalizationContext context, String path, File file,
            List<ListResponseEntry> entries) {

        if (!path.endsWith(Checksum.CHECKSUM_FILE_EXTENSION)) {
            ListResponseEntry entry = new ListResponseEntry();
            entry.setContext(context);
            entry.setFileName(path);
            if (file.exists()) {
                entry.setExistsOnServer(true);
                Date modTime = new Date(file.lastModified());
                entry.setDate(modTime);
                if (file.isDirectory()) {
                    entry.setDirectory(true);
                }
                /* Use the checksum already in memory if possible */
                ILocalizationFile localizationFile = PathManagerFactory
                        .getPathManager().getLocalizationFile(context, path);
                if (modTime.equals(localizationFile.getTimeStamp())) {
                    entry.setChecksum(localizationFile.getCheckSum());
                } else {
                    /*
                     * The PathManager does not necessarily update if a file
                     * changes so must get it from the file directly.
                     */
                    entry.setChecksum(ChecksumIO.getFileChecksum(file));
                }
            } else {
                entry.setExistsOnServer(false);
                entry.setChecksum(ILocalizationFile.NON_EXISTENT_CHECKSUM);
            }

            entries.add(entry);
        }
    }

    private static void recursiveFileBuild(String localizedSite,
            LocalizationContext context, File dir, String subPath,
            String fileExtension, boolean recursive, boolean filesOnly,
            List<ListResponseEntry> entries, int depth) throws EdexException {

        String path = dir.getPath();
        if ((subPath == null) || subPath.isEmpty()) {
            subPath = ".";
        } else {
            path += "/" + subPath;
        }
        String prependToPath = subPath + "/";

        File file = new File(path);
        if (depth == 0 && !file.exists()) {
            /*
             * File doesn't exist, make sure we flush any NFS caches by listing
             * the parent files and recreating the file. We only need to perform
             * this hack if depth is 0 since otherwise we were called based on
             * results of a listFiles() call
             */
            File parent = file.getParentFile();
            parent.listFiles();
            file = new File(path);
        }

        if (!isValidEntry(file, fileExtension)) {
            if (file.exists()) {
                String message = file + " is hidden and/or unreadable by user "
                        + System.getProperty("user.name")
                        + ". Please correct this immediately.";
                logger.error(message);
                EDEXUtil.sendMessageAlertViz(Priority.ERROR,
                        "com.raytheon.edex.utilitySrv", "Localization",
                        "DEFAULT", message, null, null);
            }
            return;
        }

        if (!filesOnly || file.isFile()) {
            addEntry(localizedSite, context, subPath, file, entries);
        }

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                if (!isValidEntry(f, fileExtension)) {
                    continue;
                }
                if (f.isFile()) {
                    addEntry(localizedSite, context,
                            prependToPath + f.getName(), f, entries);
                } else if (f.isDirectory()) {
                    if (recursive) {
                        recursiveFileBuild(localizedSite, context, dir,
                                prependToPath + f.getName(), fileExtension,
                                recursive, filesOnly, entries, depth + 1);
                    } else if (!filesOnly) {
                        addEntry(localizedSite, context,
                                prependToPath + f.getName(), f, entries);
                    }
                }
            }
        }
    }

    /**
     * Verifies a File is potentially a valid entry, i.e. if the filename
     * matches the fileExtension (if provided), and if the file exists then is
     * it readable and not hidden. Note a non-existent file may be considered a
     * valid entry if the filename matches the fileExtension (if provided). This
     * method is optimized to do the least number of operations against the
     * underlying filesystem as possible.
     *
     * @param file
     *            the file to verify
     * @param fileExtension
     *            a fileExtension to filter on, or null
     * @return
     */
    private static boolean isValidEntry(File file, String fileExtension) {
        /*
         * file.getName() does not go to the underlying filesystem, so check
         * filenames first
         */
        String filename = file.getName();
        if (filename.endsWith(Checksum.CHECKSUM_FILE_EXTENSION)) {
            return false;
        }

        if (fileExtension != null && !filename.endsWith(fileExtension)
                && !file.isDirectory()) {
            return false;
        }

        /*
         * According to File.canRead() javadoc, canRead() should return false if
         * the file doesn't exist. However, that is not guaranteed and does not
         * seem to work right so we will check existence.
         */
        if (file.exists() && (!file.canRead() || file.isHidden())) {
            return false;
        }

        return true;
    }

    public static ListUtilityResponse listContexts(String path,
            LocalizationLevel level) {
        List<ListResponseEntry> entries = new ArrayList<>();
        for (LocalizationType type : LocalizationType.values()) {
            if ("UNKNOWN".equals(type.name())
                    || "EDEX_STATIC".equals(type.name())) {
                continue;
            }
            String fullPath = path + File.separator + type.name().toLowerCase()
                    + File.separator + level.name().toLowerCase();
            File dir = new File(fullPath);

            // We only want directories
            FileFilter fileFilter = new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            };

            File[] files = dir.listFiles(fileFilter);
            if (files != null) {
                for (File file : files) {
                    LocalizationContext context = new LocalizationContext(type,
                            level, file.getName());
                    addEntry(null, context,
                            type.name().toLowerCase() + File.separator
                                    + level.name().toLowerCase()
                                    + File.separator + file.getName(),
                            file, entries);
                }
            }
        }

        return new ListUtilityResponse(
                entries.toArray(new ListResponseEntry[entries.size()]));
    }

}
