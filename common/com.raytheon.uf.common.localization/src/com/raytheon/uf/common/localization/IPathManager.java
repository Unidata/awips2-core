package com.raytheon.uf.common.localization;

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

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * A generalized interface for getting ILocalizationFiles. Implementations
 * should enforce that there will only exist a single reference to any
 * ILocalizationFile per IPathManager instance. It is the IPathManager's
 * responsibility to ensure multiple objects representing the same
 * ILocalizationFile are not returned.
 * 
 * Paths passed as String arguments to methods should use IPathManager.SEPARATOR
 * as the separator for consistency. The client OS could potentially differ from
 * the localization store's OS, and this ensures the path will be resolved
 * correctly.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/12/2008              chammack    Initial Creation.
 * Jul 24, 2014 3378       bclement    added cache operations
 * Sep 08, 2014 3592       randerso    Added single type listStaticFiles, 
 *                                     getStaticLocalizationFile, and getStaticFile APIs 
 *                                     Improved JavaDoc for all listStaticFiles, 
 *                                     getStaticLocalizationFile, and getStaticFile APIs.
 * Apr 13, 2015 4383       dgilling    Added listStaticFiles, getStaticLocalizationFile
 *                                     and getStaticFile that searches based on
 *                                     LocalizationContexts.
 * Aug 24, 2015 4393       njensen     Added getObserver()                                    
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */
public interface IPathManager {

    /**
     * Directory separator used by Localization
     */
    public static final String SEPARATOR = "/";

    /**
     * 
     * Finds the specified file in any applicable LocalizationType. Note: If the
     * same file exists in multiple localization levels, only the lowest level
     * version of the file will be returned.
     * 
     * Returns null if file is not found.
     * 
     * @param name
     *            the name of the file to search for
     * @return a pointer to the location on the filesystem
     */
    public File getStaticFile(String name);

    /**
     * 
     * Finds the specified file in the specified LocalizationType. Note: If the
     * same file exists in multiple localization levels, only the lowest level
     * version of the file will be returned.
     * 
     * Returns null if file is not found.
     * 
     * @param type
     *            LocalizationType of file to be found
     * 
     * @param name
     *            the name of the file to search for
     * @return a pointer to the location on the filesystem
     */
    public File getStaticFile(LocalizationType type, String name);

    /*
     * TODO eventually these methods should gravitate towards returning the
     * interface ILocalizationFile instead of the implementation
     * LocalizationFile
     */

    /**
     * 
     * Finds the specified file in any applicable LocalizationType. Note: If the
     * same file exists in multiple localization levels, only the lowest level
     * version of the file will be returned.
     * 
     * Returns null if file is not found.
     * 
     * @param name
     *            the name of the file to search for
     * @return the file and the context it was found in
     */
    public LocalizationFile getStaticLocalizationFile(String name);

    /**
     * 
     * Finds the specified file in the specified LocalizationType. Note: If the
     * same file exists in multiple localization levels, only the lowest level
     * version of the file will be returned.
     * 
     * Returns null if file is not found.
     * 
     * @param type
     *            LocalizationType of file to be found
     * 
     * @param name
     *            the name of the file to search for
     * @return the file and the context it was found in
     */
    public LocalizationFile getStaticLocalizationFile(LocalizationType type,
            String name);

    /**
     * Finds the specified file by searching the specified
     * {@code LocalizationContext}s. Note: If the same file exists in multiple
     * localization levels, only the lowest level version of the file will be
     * returned.
     * <p>
     * Returns {@code null} if file is not found.
     * 
     * @param contexts
     *            the {@code LocalizationContext}s to search
     * @param name
     *            the name of the file to search for
     * @return the file and the context it was found in
     */
    public LocalizationFile getStaticLocalizationFile(
            LocalizationContext[] contexts, String name);

    /**
     * 
     * Return a pointer to a localization file given a specific context
     * 
     * @param context
     *            the localization context to use
     * @param name
     *            the filename to search
     * @return an absolute pointer on the filesystem to the file
     */
    public File getFile(LocalizationContext context, String name);

    /**
     * 
     * Return a pointer to a localization file given a specific context
     * 
     * @param context
     *            the localization context to use
     * @param name
     *            the filename to search
     * @return an absolute pointer on the filesystem to the file
     */
    public LocalizationFile getLocalizationFile(LocalizationContext context,
            String name);

    /**
     * Returns a map containing a LocalizationFile for each LocalizationLevel
     * where the file exists
     * 
     * @param type
     *            desired LocalizationType
     * @param name
     *            the localization file name
     * @return map of LocalizationLevel to LocalizationFile
     */
    public Map<LocalizationLevel, LocalizationFile> getTieredLocalizationFile(
            LocalizationType type, String name);

    /**
     * Lists all files in the localization hierarchy for the specified directory
     * Note: If the same file exists in multiple localization levels, all
     * versions of the file will be returned in the array.
     * 
     * @param context
     *            the localization context to use
     * @param name
     *            the directory name to search
     * @param extensions
     *            a list of file extensions to look for, or null if no filter
     * @param recursive
     *            true for recursive directory listing, false for a single level
     *            listing
     * @param filesOnly
     *            true if only files are listed, false to list both files and
     *            directories
     * @return the files on the filesystem in the directory
     */
    public LocalizationFile[] listFiles(LocalizationContext context,
            String name, String[] extensions, boolean recursive,
            boolean filesOnly);

    /**
     * Lists all files in the localization hierarchy for the specified directory
     * Note: If the same file exists in multiple localization levels, all
     * versions of the file will be returned in the array.
     * 
     * @param contexts
     *            the localization contexts to search
     * @param name
     *            the directory name to search
     * @param extensions
     *            a list of file extensions to look for, or null if no filter
     * @param recursive
     *            true for recursive directory listing, false for a single level
     *            listing
     * @param filesOnly
     *            true if only files are listed, false to list both files and
     *            directories
     * @return the files on the filesystem in the directory or null in case of
     *         error
     */
    public LocalizationFile[] listFiles(LocalizationContext[] contexts,
            String name, String[] extensions, boolean recursive,
            boolean filesOnly);

    /**
     * Lists all files in all localization contexts in a particular directory.
     * Note: If the same file exists in multiple localization levels, only the
     * lowest level version of the file will be returned in the array.
     * 
     * @param name
     *            the directory to look in
     * @param extensions
     *            a list of file extensions to look for, or null if no filter
     * @param recursive
     *            true for recursive directory listing, false for a single level
     *            listing
     * @param filesOnly
     *            true if only files are listed, false to list both files and
     *            directories
     * @return a list of files
     */
    public LocalizationFile[] listStaticFiles(String name, String[] extensions,
            boolean recursive, boolean filesOnly);

    /**
     * Lists all files in the specified LocalizationType in a particular
     * directory. Note: If the same file exists in multiple localization levels,
     * only the lowest level version of the file will be returned in the array.
     * 
     * @param type
     *            the localization type to use
     * 
     * @param name
     *            the directory to look in
     * @param extensions
     *            a list of file extensions to look for, or null if no filter
     * @param recursive
     *            true for recursive directory listing, false for a single level
     *            listing
     * @param filesOnly
     *            true if only files are listed, false to list both files and
     *            directories
     * @return a list of files
     */
    public LocalizationFile[] listStaticFiles(LocalizationType type,
            String name, String[] extensions, boolean recursive,
            boolean filesOnly);

    /**
     * Lists all files in the specified {@code LocalizationContext}s in a
     * particular directory. Note: If the same file exists in multiple
     * localization levels, only the lowest level version of the file will be
     * returned in the array.
     * <p>
     * Do not pass in a mix LocalizationContexts with different
     * LocalizationTypes to this method, the results will be unreliable.
     * 
     * @param contexts
     *            the {@code LocalizationContext}s to search
     * @param name
     *            the directory to look in
     * @param extensions
     *            a list of file extensions to look for, or {@code null} if no
     *            filter
     * @param recursive
     *            {@code true} for recursive directory listing, {@code false}
     *            for a single level listing
     * @param filesOnly
     *            {@code true} if only files are listed, {@code false} to list
     *            both files and directories
     * @return a list of files
     */
    public LocalizationFile[] listStaticFiles(LocalizationContext[] contexts,
            String name, String[] extensions, boolean recursive,
            boolean filesOnly);

    /**
     * Returns a localization context for the given type and level for the
     * active user.
     * 
     * @param type
     * @param level
     * @return the localization context
     */
    public LocalizationContext getContext(LocalizationType type,
            LocalizationLevel level);

    /**
     * Returns the site localization context for the given type and siteId.
     * 
     * @param type
     * @param siteId
     * @return the site localization context
     */
    public LocalizationContext getContextForSite(LocalizationType type,
            String siteId);

    /**
     * Return the localization contexts that should be searched given a
     * localization type
     * 
     * @param type
     *            the type
     * @return the localization contexts
     */
    public LocalizationContext[] getLocalSearchHierarchy(
            LocalizationContext.LocalizationType type);

    /**
     * Get the available context strings for the given level
     * 
     * @param level
     * @return the available contexts
     */
    public String[] getContextList(LocalizationLevel level);

    /**
     * Returns the available levels to be used, sorted from lowest search level
     * to highest (BASE,...,SITE,...,USER)
     * 
     * @return Available sorted levels
     */
    public LocalizationLevel[] getAvailableLevels();

    /**
     * Stores the localization file cache in this class to the file passed in.
     * Can be used to take a snapshot of the current cached files
     * 
     * @param cacheFile
     */
    public void storeCache(File cacheFile) throws IOException,
            SerializationException;

    /**
     * Restores the LocalizationFile cache from this file. Can be used in
     * conjunction with storeCache to restore a snapshot of the cache
     * 
     * @param cacheFile
     */
    public void restoreCache(File cacheFile) throws IOException,
            SerializationException;

    /**
     * Gets the observer for watching for changes to ILocalizationFiles that are
     * returned by this IPathManager
     * 
     * @return the observer
     */
    public ILocalizationNotificationObserver getObserver();

}