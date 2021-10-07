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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.util.file.Files;

/**
 * Represents a file in the localization service. See the interface
 * {@link ILocalizationFile} for more information.<BR>
 * <BR>
 * A LocalizationFile cannot be constructed directly, it must be obtained using
 * the PathManager. PathManager should ensure that only one reference to a
 * particular LocalizationFile exists in the JVM.<BR>
 * <BR>
 * A file is generally not realized until the getFile() method is called. At
 * that point, it becomes a real file on the local system. Prior to calling
 * getFile(), the LocalizationFile can be considered a pointer. Operations are
 * provided directly on this interface that allow the changes to be persisted
 * both to the local filesystem and to the localization service. <BR>
 * <BR>
 * <HR>
 * <B>Common Use Cases:</B> <BR>
 * <UL>
 * <LI>Reading a file - A user should call openInputStream() to stream the file
 * contents into memory.
 * <LI>Writing to a file - Ideally use openOutputStream() to write to a file. A
 * user can also write to a file by obtaining the java.io.File object using the
 * getFile() method and then writing to it as if it was a regular file using
 * conventional file I/O methods. To save the file back to the localization
 * store, call save().
 * <LI>Delete - Calling delete() will delete any local file (if it exists), and
 * delete the copy on the localization store.
 * </UL>
 *
 *
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 27, 2008           njensen   Initial creation
 * May 01, 2008           chammack  Added Localization synchronization
 *                                  information
 * May 15, 2008  878      chammack  Refactor
 * Mar 24, 2010  2866     randerso  Removed conditional around call to retrieve.
 *                                  This was added as part of an effort to
 *                                  improve localization performance but caused
 *                                  updated files on the server not to be
 *                                  retrieved.
 * Jan 17, 2013  1412     djohnson  Add jaxbMarshal.
 * Apr 12, 2013  1903     rjpeter   Updated getFile to check parentFile for
 *                                  existence.
 * Jun 05, 2014  3301     njensen   Improved locking efficiency of read()
 * Sep 29, 2014  2975     njensen   Correct usage of mkDirs in getFile(boolean)
 * Feb 11, 2015  4108     randerso  Implemented hashCode()
 * Feb 24, 2015  3978     njensen   Changed openInputStream() return type to
 *                                  InputStream Removed read() method
 * Aug 18, 2015  3806     njensen   Implements ILocalizationFile, deprecated bad
 *                                  methods, extracted jaxb convenience logic
 * Aug 24, 2015  4393     njensen   Added IPathManager to constructor args
 * Aug 26, 2015  4691     njensen   Safely skip file locking on most read
 *                                  operations
 * Nov 12, 2015  4834     njensen   Remove ModifiableLocalizationFile Deprecated
 *                                  and changed add/removeFileUpdatedObserver()
 * Dec 03, 2015  4834     njensen   Updated for ILocalizationFile changes
 * Jan 07, 2016  4834     njensen   Filter notifications using deprecated
 *                                  ILocalizationFileObserver
 * Jan 15, 2016  4834     njensen   More advanced filtering of notifications
 * Jan 28, 2016  4834     njensen   Extracted compatibility logic for old
 *                                  ILocalizationFileObserver API
 * Apr 07, 2016  5540     njensen   Updated isAvailableOnServer() for
 *                                  compatibility with older servers
 * Jun 15, 2016  5695     njensen   Rewrote delete() to delegate to adapter
 * Aug 15, 2016  5834     njensen   Check protection level in openOutputStream()
 * Apr 26, 2017  6258     tgurney   Add default file and dir permissions
 * Aug 04, 2017  6379     njensen   Remove protected level concept
 * Oct 07, 2021  8673     randerso  Add logging to attempt to determine why
 *                                  LocalizationFile.isNull() is returning true.
 *
 * </pre>
 *
 * @author njensen
 */

public final class LocalizationFile
        implements Comparable<LocalizationFile>, ILocalizationFile {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationFile.class);

    // in octal: 0640
    public static final Set<PosixFilePermission> FILE_PERMISSIONS = Collections
            .unmodifiableSet(EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ));

    // directories have 0750
    public static final Set<PosixFilePermission> DIR_PERMISSIONS = Collections
            .unmodifiableSet(EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE));

    /**
     * the max file size of a localization file to attempt to read without
     * locking
     */
    private static final int EAGER_READ_MAXSIZE = 100 * 1024;

    /** Local file pointer to localization file, will never be null */
    private final File file;

    /**
     * The file timestamp on the server, may be null if file doesn't exist yet
     */
    private final Date fileTimestamp;

    /** Checksum of file on server, may be null if file doesn't exist yet */
    private final String fileCheckSum;

    /** The context of the file, never null */
    private final LocalizationContext context;

    /** True if file points to a directory, false otherwise */
    private final boolean isDirectory;

    /** The localization adapter for the file */
    private final ILocalizationAdapter adapter;

    /** The localization path of the file */
    private final String path;

    /** File changed observers */
    private final Map<ILocalizationFileObserver, ILocalizationPathObserver> observers = new HashMap<>();

    /**
     * Check if a file is null type
     *
     * @return
     */
    boolean isNull() {
        return adapter == null && path == null && context == null
                && file == null;
    }

    LocalizationFile(ILocalizationAdapter adapter, LocalizationContext context,
            File file, Date date, String path, String checkSum,
            boolean isDirectory) {
        this.adapter = adapter;
        this.context = context;
        this.file = file;
        this.fileCheckSum = checkSum;
        this.fileTimestamp = date;
        this.isDirectory = isDirectory;
        this.path = LocalizationUtil.getSplitUnique(path);

        if (this.isNull()) {
            StackTraceElement[] stackTrace = Thread.currentThread()
                    .getStackTrace();
            StringBuilder stackTraceBuilder = new StringBuilder(
                    "LocalizationFile.isNull() returns true in constructor\n");
            for (StackTraceElement traceElement : stackTrace) {
                stackTraceBuilder.append("\tat ").append(traceElement)
                        .append('\n');
            }

            statusHandler.warn(stackTraceBuilder.toString());
        }
    }

    /**
     * This returns the time stamp of the file where it is stored, not the local
     * version of the file
     *
     * @return the file time stamp, may be null if file doesn't exist yet
     */
    @Override
    public Date getTimeStamp() {
        return fileTimestamp;
    }

    /**
     * This returns the check sum of this instance of the file
     *
     * @return the file check sum, may be {@value #NON_EXISTENT_CHECKSUM} or
     *         {@value #DIRECTORY_CHECKSUM}
     */
    @Override
    public String getCheckSum() {
        return fileCheckSum;
    }

    /**
     * Return a local file pointer that can be used to interact with the data in
     * the file. This method is NOT recommended for use in reading/writing to
     * the file. The methods openInputStream() and openOutputStream() should be
     * used to safely read/write to the file.
     *
     * <BR>
     * Prior to calling this method, the file is not guaranteed to exist on the
     * local filesystem. Note that in some cases (e.g. when creating a file),
     * the File returned may not actually exist.
     *
     * @deprecated Please use openInputStream() for retrieving the file
     *             contents, openOutputStream() for saving new file contents,
     *             and the {@link ILocalizationFile} interface getters for
     *             getting metadata. <strong>If the localization file represents
     *             a directory</strong>, continue to use this method for the
     *             time being. <strong>If you aren't sure how to replace the
     *             call to this method</strong>, continue to use this method for
     *             the time being.
     *
     *
     * @param retrieveFile
     *            a flag that specifies whether the file should be downloaded if
     *            the local file pointer doesn't exist
     * @return the file
     */
    @Deprecated
    public File getFile(boolean retrieveFile) throws LocalizationException {
        if (retrieveFile) {
            /*
             * Attempt to eagerly create parent directories. It's okay if this
             * fails since they are not needed yet. save() will create them as
             * necessary, and will fail loudly if it cannot.
             */
            try {
                if (isDirectory && !file.exists()) {
                    Files.createDirectories(file.toPath(), PosixFilePermissions
                            .asFileAttribute(DIR_PERMISSIONS));
                } else if (!file.getParentFile().exists()) {
                    Files.createDirectories(file.getParentFile().toPath(),
                            PosixFilePermissions
                                    .asFileAttribute(DIR_PERMISSIONS));
                }
            } catch (IOException e) {
                statusHandler.error(
                        "Error creating directories for "
                                + (isDirectory ? file : file.getParentFile()),
                        e);
            }
            if (isAvailableOnServer()) {
                adapter.retrieve(this);
            }
        }
        return file;
    }

    /**
     * Return a local file pointer that can be used to interact with the data in
     * the file. This method is NOT recommended for use in reading/writing to
     * the file. The methods openInputStream() and openOutputStream() should be
     * used to safely read/write to the file.
     *
     * <BR>
     * Prior to calling this method, the file is not guaranteed to exist on the
     * local filesystem. Note that in some cases (e.g. when creating a file),
     * the File returned may not actually exist.
     *
     * @deprecated Please use openInputStream() for retrieving the file
     *             contents, openOutputStream() for saving new file contents,
     *             and the {@link ILocalizationFile} interface getters for
     *             getting metadata. <strong>If the localization file represents
     *             a directory</strong>, continue to use this method for the
     *             time being. <strong>If you aren't sure how to replace the
     *             call to this method</strong>, continue to use this method for
     *             the time being.
     *
     * @return the file
     */
    @Deprecated
    public File getFile() {
        try {
            return getFile(true);
        } catch (@SuppressWarnings("squid:S1166")
        LocalizationException e) {
            // just return the file path if it could not be retrieved
            return adapter.getPath(context, path);
        }
    }

    /*
     * TODO: Come up with a way to get all the files (potentially recursive) in
     * a directory without using getFile(). At the time of this comment writing
     * (Nov-Dec 2015), getFile() on a directory is retrieving all the files
     * within the directory. This is used for a lot of directories that are
     * added to a python sub-interpreter's sys.path for importing modules.
     * Therefore the files inside the directory need to be in some kind of
     * environment or virtual environment that python can import them from, and
     * openInputStream() can't handle that at present.
     */

    /**
     * Creates an InputStream for the contents of the LocalizationFile. Calling
     * code should close the stream after use, preferably with
     * try(f.openInputStream()){...}.
     *
     * This intentionally returns an InputStream so calling code does not have
     * to worry about where the data is coming from. Please do not cast the
     * InputStream; in the future the underlying type of the InputStream may
     * change.
     *
     * @return the InputStream to be used for reading the file
     * @throws LocalizationException
     */
    @Override
    public InputStream openInputStream() throws LocalizationException {
        try {
            if (context.getLocalizationType() == LocalizationType.CAVE_STATIC
                    && context
                            .getLocalizationLevel() == LocalizationLevel.BASE) {
                /*
                 * Don't bother locking cave_static.base, the application
                 * typically does not have permissions there to create the lock.
                 * Also the application cannot change the file so if any process
                 * does change the file it will likely be ignoring the lock
                 * anyway.
                 *
                 * TODO: if cave_static becomes OBE, this can be removed
                 */
                return new FileInputStream(getFile());
            } else {
                /*
                 * This code eagerly checks the checksum of the local file
                 * against the checksum of the server file without acquiring a
                 * lock. This is an optimization based on the idea that the
                 * majority of times a file is requested to read, the file has
                 * not changed, therefore we shouldn't waste time acquiring a
                 * lock just to verify it.
                 *
                 * If the checksum doesn't match, it falls back to the
                 * LocalizationFileInputStream (which safely locks) and
                 * therefore the only performance hit is we will read the file
                 * contents into memory twice for the checksum check twice. Due
                 * to the performance implications in that scenario, we only
                 * apply the optimization to files smaller than a specific size.
                 */
                File fsFile = getFile(false);
                long length = fsFile.length();
                if (length > 0 && length < EAGER_READ_MAXSIZE) {
                    try {
                        byte[] bytes = java.nio.file.Files
                                .readAllBytes(fsFile.toPath());
                        ByteArrayInputStream bais = new ByteArrayInputStream(
                                bytes);
                        String checksum = Checksum.getMD5Checksum(bais);
                        if (checksum.equals(fileCheckSum)) {
                            bais.reset();
                            return bais;
                        }
                    } catch (@SuppressWarnings("squid:S1166")
                    Exception e) {
                        // ignore, fallback to the other behavior
                    }
                }

                // eager checksum didn't work, do it the locking way
                return new LocalizationFileInputStream(this);
            }
        } catch (FileNotFoundException e) {
            throw new LocalizationException("Error opening input stream", e);
        }
    }

    /**
     * Creates an OutputStream for the LocalizationFile. NOTE: You MUST call
     * SaveableOutputStream.save() before closing the file to save the file to
     * the localization service.
     *
     * @return the OutputStream to be used for writing to the file
     * @throws LocalizationException
     */
    @Override
    public SaveableOutputStream openOutputStream()
            throws LocalizationException {
        return openOutputStream(false);
    }

    /**
     * Creates an OutputStream for the LocalizationFile. NOTE: You MUST call
     * SaveableOutputStream.save() before closing the file to save the file to
     * the localization service.
     *
     * @deprecated Appending will not be supported in the future.
     *
     * @param isAppending
     * @return the OutputStream to be used for writing to the file
     * @throws LocalizationException
     */
    @Deprecated
    public SaveableOutputStream openOutputStream(boolean isAppending)
            throws LocalizationException {
        try {
            return new LocalizationSaveableFileOutputStream(
                    new LocalizationFileOutputStream(this, isAppending));
        } catch (FileNotFoundException e) {
            throw new LocalizationException("Error opening input stream", e);
        }
    }

    /**
     * Writes the data to the underlying file. Also persists the file back to
     * the localization store.
     *
     * @deprecated Please use openOutputStream() to get a SaveableOutputStream
     *             and then call the SaveableOutputStream.save() method after
     *             writing out the contents to the stream.
     *
     * @param bytes
     * @throws LocalizationException
     */
    @Deprecated
    public void write(byte[] bytes) throws LocalizationException {
        try (SaveableOutputStream sos = openOutputStream()) {
            sos.write(bytes);
            sos.save();
        } catch (IOException e) {
            throw new LocalizationException(
                    "Could not write to file " + file.getName(), e);
        }
    }

    /**
     *
     * Return the localization context that this file belongs to
     *
     * @return the context
     */
    @Override
    public LocalizationContext getContext() {
        return context;
    }

    public ILocalizationAdapter getAdapter() {
        return adapter;
    }

    /**
     * Returns true if the file is available on server.
     *
     * @return true if the file is available on the server
     */
    public boolean isAvailableOnServer() {
        /*
         * In theory we should never have a null fileChecksum. If connecting to
         * an older server we can get null checksums for existing directories.
         * (Actual files will have checksums as normal, and new servers will use
         * the ILocalizationFile.DIRECTORY_CHECKSUM for directories).
         *
         * TODO: Remove the fileChecksum == null && isDirectory() condition once
         * older servers are no longer deployed.
         */
        return (fileCheckSum != null && !ILocalizationFile.NON_EXISTENT_CHECKSUM
                .equals(fileCheckSum))
                || (fileCheckSum == null && isDirectory());
    }

    /**
     * Checks if the file points to a directory
     *
     * @return true if the file is actually a directory
     */
    @Override
    public boolean isDirectory() {
        return isDirectory;
    }

    /**
     * Save the file back to the localization store
     *
     * @deprecated Please use openOutputStream() to get a SaveableOutputStream
     *             and then call the SaveableOutputStream.save() method after
     *             writing out the contents to the stream.
     *
     *
     * @throws LocalizationException
     */
    @Deprecated
    public boolean save() throws LocalizationException {
        try {
            FileLocker.lock(this, file, Type.WRITE);
            String checksum = "";
            try {
                checksum = Checksum.getMD5Checksum(file);
            } catch (@SuppressWarnings("squid:S1166")
            IOException e) {
                /*
                 * if checksum can't be computed just save the file
                 * unconditionally
                 */
            }

            // Check if file differs from server file
            if (!checksum.equals(fileCheckSum)) {
                return adapter.save(this);
            }

            // Local file matches server file, success technically
            return true;
        } finally {
            FileLocker.unlock(this, file);
        }
    }

    /**
     * Return the file path (not including the context directories)
     *
     * @deprecated Please use getPath() instead. It will return the exact same
     *             String and is available on the ILocalizationFile interface.
     *
     * @return the file path
     */
    @Deprecated
    public String getName() {
        return getPath();
    }

    @Override
    public String getPath() {
        return path;
    }

    /**
     * Delete the localization file
     *
     * @throws LocalizationException
     */
    @Override
    public void delete() throws LocalizationException {
        adapter.delete(this);
    }

    /**
     * Check if the localization file exists
     *
     * @return true if the file exists
     */
    @Override
    public boolean exists() {
        return !isNull() && adapter.exists(this);
    }

    /**
     * Add an observer on the LocalizationFile.
     *
     * @deprecated Please use IPathManager.addLocalizationPathObserver()
     *             instead. Note that the listening behavior will be different
     *             in that the IPathManager observer will observe all changes to
     *             the file regardless of the context.
     *
     * @param observer
     */
    @Deprecated
    public void addFileUpdatedObserver(
            final ILocalizationFileObserver observer) {
        ILocalizationPathObserver pathObs = new LocalizationFileIntermediateObserver(
                this, observer);

        synchronized (observers) {
            ILocalizationPathObserver old = observers.put(observer, pathObs);
            if (old != null) {
                StackTraceElement[] stackTrace = Thread.currentThread()
                        .getStackTrace();
                StringBuilder stackTraceBuilder = new StringBuilder(
                        "Developer error: added the same observer to the same file twice\n");
                for (StackTraceElement traceElement : stackTrace) {
                    stackTraceBuilder.append("\tat ").append(traceElement)
                            .append('\n');
                }

                statusHandler.warn(stackTraceBuilder.toString());
            }
        }

        PathManagerFactory.getPathManager()
                .addLocalizationPathObserver(this.path, pathObs);
    }

    /**
     * Remove the observer as a listener on the file
     *
     * @deprecated Please see the deprecation comments on
     *             addFileUpdatedObserver() and use the corresponding
     *             IPathManager method for removal.
     *
     * @param observer
     */
    @Deprecated
    public void removeFileUpdatedObserver(ILocalizationFileObserver observer) {
        ILocalizationPathObserver pathObs = null;
        synchronized (observers) {
            pathObs = observers.remove(observer);
        }
        if (pathObs != null) {
            PathManagerFactory.getPathManager()
                    .removeLocalizationPathObserver(pathObs);
        }
    }

    /**
     * Returns the object version of this jaxb serialized file. Returns null if
     * the file does not exist or is empty.
     *
     * @deprecated Please use
     *             <code>JAXBManager.unmarshalFromInputStream(Class<T>,
     *             LocalizationFile.openInputStream());</code> to read in your
     *             object.
     *
     * @param resultClass
     * @param manager
     * @return the object representation umarshaled from this file
     * @throws LocalizationException
     */
    @Deprecated
    public <T> T jaxbUnmarshal(Class<T> resultClass, JAXBManager manager)
            throws LocalizationException {
        return LocalizationXmlUtil.jaxbUnmarshal(this, resultClass, manager);
    }

    /**
     * Marshal the specified object into this file.
     *
     * @deprecated Please use <code>JAXBManager.marshalToOutputStream(Object,
     *             LocalizationFile.openOutputStream()); </code> to write out
     *             your object. Then call
     *             <code>SaveableOutputStream.save()</code> on your output
     *             stream.
     *
     * @param obj
     *            the object to marshal
     *
     * @param jaxbManager
     *            the jaxbManager
     */
    @Deprecated
    public void jaxbMarshal(Object obj, JAXBManager jaxbManager)
            throws LocalizationException {
        LocalizationXmlUtil.jaxbMarshal(this, obj, jaxbManager);
    }

    @Override
    public String toString() {
        return context + IPathManager.SEPARATOR + path;
    }

    /**
     * Compares a LocalizationFile, only by the path and ignoring the context.
     * TODO: Implement this better or make a solid Comparator.
     *
     * @deprecated This implementation is questionable. Try not to use the
     *             method.
     *
     */
    @Override
    @Deprecated
    public int compareTo(LocalizationFile o) {
        return getName().compareTo(o.getName());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (context == null ? 0 : context.hashCode());
        result = prime * result + (path == null ? 0 : path.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LocalizationFile other = (LocalizationFile) obj;
        if (context == null) {
            if (other.context != null) {
                return false;
            }
        } else if (!context.equals(other.context)) {
            return false;
        }
        if (path == null) {
            if (other.path != null) {
                return false;
            }
        } else if (!path.equals(other.path)) {
            return false;
        }
        return true;
    }

}
