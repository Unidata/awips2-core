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
package com.raytheon.uf.common.util.file;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Provides custom implementations of
 * {@link java.nio.file.Files#createFile(Path, FileAttribute...)},
 * {@link java.nio.file.Files#createDirectory(Path, FileAttribute...)}, and
 * {@link java.nio.file.Files#createDirectories(Path, FileAttribute...)}.
 * 
 * The custom implementations were created due to bugs related to how
 * {@link PosixFilePermission} are either not recognized or incorrectly handled
 * as {@link FileAttribute} in the methods that these custom methods have been
 * created to temporarily replace (when POSIX permissions are important). The
 * original java.nio.file.Files methods do create the expected file or
 * directory; however, the correct permissions are not applied. So, for now a
 * workaround has been implemented to first: create the file, open an
 * {@link OutputStream} to the file (which will create the file), or create the
 * directories recursively and then use
 * {@link Files#setPosixFilePermissions(Path, Set)} to apply the correct
 * permissions to the file or directory that had been created.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 5, 2017  6255       bkowal      Initial creation
 *
 * </pre>
 *
 * @author bkowal
 */

public final class Files {

    /**
     * Constructor.
     */
    private Files() {
    }

    /**
     * Creates a new and empty file, failing if the file already exists. The
     * check for the existence of the file and the creation of the new file if
     * it does not exist are a single operation that is atomic with respect to
     * all other filesystem activities that might affect the directory.
     *
     * <p>
     * The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the file. Each attribute
     * is identified by its {@link FileAttribute#name name}. If more than one
     * attribute of the same name is included in the array then all but the last
     * occurrence is ignored.
     *
     * @param path
     *            the path to the file to create
     * @param attrs
     *            an optional list of file attributes to set atomically when
     *            creating the file
     *
     * @return the file
     *
     * @throws UnsupportedOperationException
     *             if the array contains an attribute that cannot be set
     *             atomically when creating the file
     * @throws FileAlreadyExistsException
     *             if a file of that name already exists <i>(optional specific
     *             exception)</i>
     * @throws IOException
     *             if an I/O error occurs or the parent directory does not exist
     * @throws SecurityException
     *             In the case of the default provider, and a security manager
     *             is installed, the {@link SecurityManager#checkWrite(String)
     *             checkWrite} method is invoked to check write access to the
     *             new file.
     * 
     *             <pre>
     * (https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createFile%28java.nio.file.Path,%20java.nio.file.attribute.FileAttribute...%29)
     *             </pre>
     */
    public static Path createFile(Path path, FileAttribute<?>... attrs)
            throws IOException {
        final Set<PosixFilePermission> permissions = EnumSet
                .noneOf(PosixFilePermission.class);
        /*
         * Using a Linked List because order is important per the JavaDoc for
         * attributes: "If more than one attribute of the same name is included
         * in the array then all but the last occurrence is ignored."
         */
        final List<FileAttribute<?>> revisedAttributes = new LinkedList<>();
        IOPermissionsHelper.separatePosixFilePermissions(permissions,
                revisedAttributes, attrs);
        java.nio.file.Files.createFile(path,
                revisedAttributes.toArray(new FileAttribute<?>[0]));
        if (IOPermissionsHelper.isPosixSupported(path)
                && !permissions.isEmpty()) {
            IOPermissionsHelper.applyFilePermissions(path, permissions);
        }
        return path;
    }

    /**
     * Creates a new directory. The check for the existence of the file and the
     * creation of the directory if it does not exist are a single operation
     * that is atomic with respect to all other filesystem activities that might
     * affect the directory. The {@link #createDirectories createDirectories}
     * method should be used where it is required to create all nonexistent
     * parent directories first.
     *
     * <p>
     * The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the directory. Each
     * attribute is identified by its {@link FileAttribute#name name}. If more
     * than one attribute of the same name is included in the array then all but
     * the last occurrence is ignored.
     *
     * @param dir
     *            the directory to create
     * @param attrs
     *            an optional list of file attributes to set atomically when
     *            creating the directory
     *
     * @return the directory
     *
     * @throws UnsupportedOperationException
     *             if the array contains an attribute that cannot be set
     *             atomically when creating the directory
     * @throws FileAlreadyExistsException
     *             if a directory could not otherwise be created because a file
     *             of that name already exists <i>(optional specific
     *             exception)</i>
     * @throws IOException
     *             if an I/O error occurs or the parent directory does not exist
     * @throws SecurityException
     *             In the case of the default provider, and a security manager
     *             is installed, the {@link SecurityManager#checkWrite(String)
     *             checkWrite} method is invoked to check write access to the
     *             new directory.
     * 
     *             <pre>
     * (https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createDirectory%28java.nio.file.Path,%20java.nio.file.attribute.FileAttribute...%29)
     *             </pre>
     */
    public static Path createDirectory(Path dir, FileAttribute<?>... attrs)
            throws IOException {
        final Set<PosixFilePermission> permissions = EnumSet
                .noneOf(PosixFilePermission.class);
        /*
         * Using a Linked List because order is important per the JavaDoc for
         * attributes: "If more than one attribute of the same name is included
         * in the array then all but the last occurrence is ignored."
         */
        final List<FileAttribute<?>> revisedAttributes = new LinkedList<>();
        IOPermissionsHelper.separatePosixFilePermissions(permissions,
                revisedAttributes, attrs);
        java.nio.file.Files.createDirectory(dir,
                revisedAttributes.toArray(new FileAttribute<?>[0]));
        if (IOPermissionsHelper.isPosixSupported(dir)
                && !permissions.isEmpty()) {
            IOPermissionsHelper.applyDirectoryPermissions(dir, permissions);
        }
        return dir;
    }

    /**
     * Creates a directory by creating all nonexistent parent directories first.
     * Unlike the {@link #createDirectory createDirectory} method, an exception
     * is not thrown if the directory could not be created because it already
     * exists.
     *
     * <p>
     * The {@code attrs} parameter is optional {@link FileAttribute
     * file-attributes} to set atomically when creating the nonexistent
     * directories. Each file attribute is identified by its
     * {@link FileAttribute#name name}. If more than one attribute of the same
     * name is included in the array then all but the last occurrence is
     * ignored.
     *
     * <p>
     * If this method fails, then it may do so after creating some, but not all,
     * of the parent directories.
     *
     * @param dir
     *            the directory to create
     *
     * @param attrs
     *            an optional list of file attributes to set atomically when
     *            creating the directory
     *
     * @return the directory
     *
     * @throws UnsupportedOperationException
     *             if the array contains an attribute that cannot be set
     *             atomically when creating the directory
     * @throws FileAlreadyExistsException
     *             if {@code dir} exists but is not a directory <i>(optional
     *             specific exception)</i>
     * @throws IOException
     *             if an I/O error occurs
     * @throws SecurityException
     *             in the case of the default provider, and a security manager
     *             is installed, the {@link SecurityManager#checkWrite(String)
     *             checkWrite} method is invoked prior to attempting to create a
     *             directory and its {@link SecurityManager#checkRead(String)
     *             checkRead} is invoked for each parent directory that is
     *             checked. If {@code
     *          dir} is not an absolute path then its {@link Path#toAbsolutePath
     *             toAbsolutePath} may need to be invoked to get its absolute
     *             path. This may invoke the security manager's
     *             {@link SecurityManager#checkPropertyAccess(String)
     *             checkPropertyAccess} method to check access to the system
     *             property {@code user.dir}
     * 
     * 
     *             <pre>
     * (https://docs.oracle.com/javase/7/docs/api/java/nio/file/Files.html#createDirectories%28java.nio.file.Path,%20java.nio.file.attribute.FileAttribute...%29)
     *             </pre>
     */
    public static Path createDirectories(Path dir, FileAttribute<?>... attrs)
            throws IOException {
        /*
         * Keep track of which directories we will create, if any.
         */
        final Set<Path> directoriesCreated = new HashSet<>(dir.getNameCount(),
                1.0f);
        Path currentPath = dir;
        boolean notExists = true;
        while (notExists) {
            if (java.nio.file.Files.exists(currentPath)) {
                notExists = false;
            } else {
                directoriesCreated.add(currentPath);

                currentPath = currentPath.getParent();
                if (currentPath == null) {
                    notExists = false;
                }
            }
        }

        final Set<PosixFilePermission> permissions = EnumSet
                .noneOf(PosixFilePermission.class);
        /*
         * Using a Linked List because order is important per the JavaDoc for
         * attributes: "If more than one attribute of the same name is included
         * in the array then all but the last occurrence is ignored."
         */
        final List<FileAttribute<?>> revisedAttributes = new LinkedList<>();
        IOPermissionsHelper.separatePosixFilePermissions(permissions,
                revisedAttributes, attrs);
        java.nio.file.Files.createDirectories(dir,
                revisedAttributes.toArray(new FileAttribute<?>[0]));
        if (IOPermissionsHelper.isPosixSupported(dir)
                && !permissions.isEmpty()) {
            IOPermissionsHelper.applyDirectoryPermissions(directoriesCreated,
                    permissions);
        }
        return dir;
    }
}