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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.raytheon.uf.common.localization.FileLocker.Type;
import com.raytheon.uf.common.localization.ILocalizationAdapter.ListResponse;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Represents a file in the localization service.<BR>
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
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * Mar 27, 2008             njensen     Initial creation
 * May 01, 2008             chammack    Added Localization synchronization information
 * May 15, 2008 #878        chammack    Refactor
 * Mar 24, 2010 #2866       randerso    Removed conditional around call to retrieve. 
 *                                      This was added as part of an effort to improve 
 *                                      localization performance but caused updated 
 *                                      files on the server not to be retrieved.
 * Jan 17, 2013 1412        djohnson    Add jaxbMarshal.
 * Apr 12, 2013 1903        rjpeter     Updated getFile to check parentFile for existence.
 * Jun 05, 2014 3301        njensen     Improved locking efficiency of read()
 * Sep 29, 2014 2975        njensen     Correct usage of mkDirs in getFile(boolean)
 * Feb 11, 2015 4108        randerso    Implemented hashCode()
 * Feb 24, 2015 3978        njensen     Changed openInputStream() return type to InputStream
 *                                       Removed read() method
 * Aug 18, 2015 3806        njensen     Implements ILocalizationFile, deprecated bad
 *                                       methods, extracted jaxb convenience logic                                      
 * 
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public final class LocalizationFile implements Comparable<LocalizationFile>,
        ILocalizationFile {

    private static transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalizationFile.class);

    /**
     * Class {@link LocalizationFile} exposes to the
     * {@link ILocalizationAdapter} objects so they can modify the file if
     * anything changes. Don't want to expose ability to modify
     * {@link LocalizationFile} contents to everyone
     * 
     * @author mschenke
     * @version 1.0
     */
    public class ModifiableLocalizationFile {

        private ModifiableLocalizationFile() {
            // Private constructor
        }

        public LocalizationFile getLocalizationFile() {
            return LocalizationFile.this;
        }

        public void setTimeStamp(Date timeStamp) {
            getLocalizationFile().fileTimestamp = timeStamp;
        }

        public void setFileChecksum(String checksum) {
            getLocalizationFile().fileCheckSum = checksum;
        }

        public void setIsAvailableOnServer(boolean isAvailableOnServer) {
            getLocalizationFile().isAvailableOnServer = isAvailableOnServer;
        }

        public void setIsDirectory(boolean isDirectory) {
            getLocalizationFile().isDirectory = isDirectory;
        }

        public File getLocalFile() {
            return getLocalizationFile().file;
        }

        public String getFileName() {
            return getLocalizationFile().path;
        }

        public LocalizationContext getContext() {
            return getLocalizationFile().context;
        }
    }

    /** Local file pointer to localization file, will never be null */
    protected final File file;

    /** The file timestamp on the server, may be null if file doesn't exist yet */
    private Date fileTimestamp;

    /** Checksum of file on server, may be null if file doesn't exist yet */
    private String fileCheckSum;

    /** The context of the file, never null */
    private final LocalizationContext context;

    /** True if file points to a directory, false otherwise */
    private boolean isDirectory;

    /** Denotes whether the file exists on the server */
    private boolean isAvailableOnServer;

    /** The localization adapter for the file */
    protected final ILocalizationAdapter adapter;

    /** The localization path of the file */
    private final String path;

    /** Protection flag of file, if file cannot be overridden, it is protected */
    private LocalizationLevel protectedLevel;

    /** File changed observers */
    private final Set<ILocalizationFileObserver> observers = new HashSet<ILocalizationFileObserver>();

    /** Flag to set if file has been requested */
    protected boolean fileRequested = false;

    /**
     * Construct a null localization file, used to keep track of files that
     * cannot exist.
     */
    LocalizationFile() {
        file = null;
        path = null;
        adapter = null;
        context = null;
    }

    /**
     * Check if a file is null type
     * 
     * @return
     */
    boolean isNull() {
        return (adapter == null) && (path == null) && (context == null)
                && (file == null);
    }

    LocalizationFile(ILocalizationAdapter adapter, LocalizationContext context,
            File file, Date date, String path, String checkSum,
            boolean isDirectory, boolean existsOnServer,
            LocalizationLevel protectedLevel) {
        this.adapter = adapter;
        this.context = context;
        this.file = file;
        this.fileCheckSum = checkSum;
        this.fileTimestamp = date;
        this.isAvailableOnServer = existsOnServer;
        this.isDirectory = isDirectory;
        this.path = LocalizationUtil.getSplitUnique(path);
        this.protectedLevel = protectedLevel;
        LocalizationNotificationObserver.getInstance().addObservedFile(this);
    }

    /**
     * Update the localization file with new metadata
     * 
     * @param metadata
     */
    void update(ListResponse metadata) {
        if (metadata != null) {
            // Update new metadata
            this.isAvailableOnServer = metadata.existsOnServer;
            this.fileTimestamp = metadata.date;
            this.fileCheckSum = metadata.checkSum;
            this.isDirectory = metadata.isDirectory;
            this.protectedLevel = metadata.protectedLevel;
        }
    }

    /**
     * Returns a modifiable version of the localization file. Meant to be used
     * internally within localization only which is why package level
     * 
     * @return
     */
    ModifiableLocalizationFile getModifiableFile() {
        return new ModifiableLocalizationFile();
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
     * This returns the check sum of the file where it is stored
     * 
     * @return the file check sum, may be null if file doesn't exist yet
     */
    @Override
    public String getCheckSum() {
        return fileCheckSum;
    }

    /**
     * Return a local file pointer that can be used to interact with the data in
     * the file. This method is NOT recommended for use in reading/writing to
     * the file. The methods openInputStream and openOutputStream should be used
     * to safely read/write to the file.
     * 
     * <BR>
     * Prior to calling this method, the file is not guaranteed to exist on the
     * local filesystem. Note that in some cases (e.g. when creating a file),
     * the File returned may not actually exist.
     * 
     * @deprecated Please use openInputStream() for retrieving the file
     *             contents, openOutputStream() for saving new file contents,
     *             and the interface getters for getting metadata.
     * 
     * @param retrieveFile
     *            a flag that specifies whether the file should be downloaded if
     *            the local file pointer doesn't exist
     * @return the file
     */
    @Deprecated
    public File getFile(boolean retrieveFile) throws LocalizationException {
        if (retrieveFile) {
            fileRequested = true;
            if (isDirectory) {
                file.mkdirs();
            } else if (!file.getParentFile().exists()) {
                try {
                    file.getParentFile().mkdirs();
                } catch (Throwable t) {
                    /*
                     * try to create the file's directory automatically, but if
                     * it fails, don't report it as it is just something to do
                     * to help the user of the file for easier creation of the
                     * file
                     */
                }
            }
            if (isAvailableOnServer) {
                adapter.retrieve(this);
            }
        }
        return file;
    }

    /**
     * This method is not recommended for use, use openInputStream() for reading
     * the LocalizationFile and openOutputStream() for writing to the
     * LocalizationFile. ALWAYS close() the streams when done reading/writing as
     * those methods will auto lock the file. If must use this method, call
     * FileLocker.lock/unlock when using the file.
     * 
     * @deprecated Please use openInputStream() for retrieving the file
     *             contents, openOutputStream() for saving new file contents,
     *             and the interface getters for getting metadata.
     * 
     * @return File pointer
     */
    @Deprecated
    public File getFile() {
        try {
            return getFile(true);
        } catch (LocalizationException e) {
            return adapter.getPath(context, path);
        }
    }

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
                    && context.getLocalizationLevel() == LocalizationLevel.BASE) {
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
                 * LocalizationFileInputStream will ensure that the file is
                 * locked before reading, with the lock released when the stream
                 * is closed.
                 */
                return new LocalizationFileInputStream(this);
            }
        } catch (FileNotFoundException e) {
            throw new LocalizationException("Error opening input stream", e);
        }
    }

    /**
     * Creates an OutputStream for the LocalizationFile
     * 
     * @return the OutputStream to be used for writing to the file
     * @throws LocalizationException
     */
    @Override
    public SaveableOutputStream openOutputStream() throws LocalizationException {
        return openOutputStream(false);
    }

    /**
     * Creates an OutputStream for the LocalizationFile
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
     * @deprecated Please use openOutputStream to write out contents.
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
            throw new LocalizationException("Could not write to file "
                    + file.getName(), e);
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

    /**
     * Returns true if the file is available on server.
     * 
     * @return true if the file is available on the server
     */
    public boolean isAvailableOnServer() {
        return isAvailableOnServer;
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
     * Check if file is protected
     * 
     * @return true if file is protected and cannot be overridden
     */
    public boolean isProtected() {
        return protectedLevel != null;
    }

    /**
     * Gets the level the file is protected at, null otherwise
     * 
     * @return the level the file is protected at, or null
     */
    public LocalizationLevel getProtectedLevel() {
        return protectedLevel;
    }

    /**
     * Save the file back to the localization store
     * 
     * @throws LocalizationOpFailedException
     */
    @Deprecated
    public boolean save() throws LocalizationOpFailedException {
        try {
            FileLocker.lock(this, file, Type.WRITE);
            String checksum = "";
            try {
                checksum = Checksum.getMD5Checksum(file);
            } catch (Exception e) {
                // Ignore
            }
            // Check if file differs from server file
            if (!checksum.equals(fileCheckSum)) {
                boolean rval = adapter.save(getModifiableFile());
                if (rval) {
                    fileCheckSum = checksum;
                }
                return rval;
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
     * @return the file path
     */
    @Override
    public String getName() {
        return path;
    }

    /**
     * Delete the localization file
     * 
     * @return true if file is deleted, false if file still exists
     * @throws LocalizationOpFailedException
     */
    public boolean delete() throws LocalizationOpFailedException {
        try {
            FileLocker.lock(this, file, Type.WRITE);
            if (exists()) {
                return adapter.delete(getModifiableFile());
            } else if (file.exists()) {
                // Local file does actually exist, delete manually
                return file.delete();
            }

            // File doesn't exist, it is deleted, so technically success?
            return true;
        } finally {
            FileLocker.unlock(this, file);
        }
    }

    /**
     * Check if the localization file exists
     * 
     * @return true if the file exists
     */
    @Override
    public boolean exists() {
        return (isNull() == false) && adapter.exists(this);
    }

    /**
     * Add an observer on the LocalizationFile
     * 
     * @param observer
     */
    public void addFileUpdatedObserver(ILocalizationFileObserver observer) {
        synchronized (observers) {
            observers.add(observer);
        }
    }

    /**
     * Remove the observer as a listener on the file
     * 
     * @param observer
     */
    public void removeFileUpdatedObserver(ILocalizationFileObserver observer) {
        synchronized (observers) {
            observers.remove(observer);
        }
    }

    /**
     * Notify the observers for the LocalizationFile of the change
     * 
     * @param message
     *            update message
     * @param metadata
     *            updated metadata for the file based on the message
     */
    void notifyObservers(FileUpdatedMessage message) {
        List<ILocalizationFileObserver> toNotify = new ArrayList<ILocalizationFileObserver>();
        synchronized (observers) {
            toNotify.addAll(observers);
        }
        for (Object observer : toNotify) {
            try {
                ((ILocalizationFileObserver) observer).fileUpdated(message);
            } catch (Throwable t) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error notifying observer of file change", t);
            }
        }
    }

    /**
     * Returns the object version of this jaxb serialized file. Returns null if
     * the file does not exist or is empty.
     * 
     * @deprecated Please use openInputStream() to read in your object, or if
     *             you must have convenience, use LocalizationXmlUtil
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
     * @deprecated Please use openOutputStream() to write out your object, or if
     *             you must have convenience, use LocalizationXmlUtil
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

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(LocalizationFile o) {
        return getName().compareTo(o.getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result)
                + ((context == null) ? 0 : context.hashCode());
        result = (prime * result) + ((path == null) ? 0 : path.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
