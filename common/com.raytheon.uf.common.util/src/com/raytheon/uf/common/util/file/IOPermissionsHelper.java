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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Implementation of a common capability that can be used to apply specified
 * permissions to individual files and/or directory hierarchies. This class is
 * only usable on Operating Systems that implement POSIX.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 4, 2017  6255       bkowal      Initial creation
 *
 * </pre>
 *
 * @author bkowal
 */

public final class IOPermissionsHelper {

    private static final String ATTRIBUTE_POSIX = "posix";

    /**
     * Constructor.
     */
    private IOPermissionsHelper() {
    }

    /**
     * Attempts to apply the specified {@link PosixFilePermission}s to the
     * specified file {@link Path}.
     * 
     * @param filePath
     *            the specified file {@link Path}.
     * @param permissions
     *            the specified {@link PosixFilePermission}s
     * @throws IOException
     */
    public static void applyFilePermissions(final Path filePath,
            final Set<PosixFilePermission> permissions) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Required argument 'filePath' cannot be NULL.");
        }
        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissions' cannot be NULL.");
        }
        verifyPosixSupport(filePath);

        try {
            Files.setPosixFilePermissions(filePath, permissions);
        } catch (IOException e) {
            throw new IOException(
                    "Failed to update the permissions for file: "
                            + filePath.toString() + " to: "
                            + PosixFilePermissions.toString(permissions) + ".",
                    e);
        }
    }

    /**
     * Attempts to apply the specified {@link PosixFilePermission}s to the
     * specified directory {@link Path}.
     * 
     * @param directoryPath
     *            the specified directory {@link Path}.
     * @param permissions
     *            the specified {@link PosixFilePermission}s.
     * @throws IOException
     */
    public static void applyDirectoryPermissions(final Path directoryPath,
            final Set<PosixFilePermission> permissions) throws IOException {
        if (directoryPath == null) {
            throw new IllegalArgumentException(
                    "Required argument 'directoryPath' cannot be NULL.");
        }
        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissions' cannot be NULL.");
        }
        verifyPosixSupport(directoryPath);

        try {
            Files.setPosixFilePermissions(directoryPath, permissions);
        } catch (IOException e) {
            throw new IOException(
                    "Failed to update the permissions for directory: "
                            + directoryPath.toString() + " to: "
                            + PosixFilePermissions.toString(permissions) + ".",
                    e);
        }
    }

    /**
     * Attempts to apply the specified {@link PosixFilePermission}s to the
     * specified directory {@link Path}s.
     * 
     * @param directoryPaths
     *            the specified directory {@link Path}s.
     * @param permissions
     *            the specified {@link PosixFilePermission}s.
     * @throws IOException
     */
    public static void applyDirectoryPermissions(
            final Collection<Path> directoryPaths,
            final Set<PosixFilePermission> permissions) throws IOException {
        if (directoryPaths == null) {
            throw new IllegalArgumentException(
                    "Required argument 'directoryPaths' cannot be NULL.");
        }
        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissions' cannot be NULL.");
        }
        if (directoryPaths.isEmpty()) {
            /*
             * Nothing to do.
             */
            return;
        }

        for (Path directoryPath : directoryPaths) {
            applyDirectoryPermissions(directoryPath, permissions);
        }
    }

    /**
     * Returns an {@link OutputStream} for the specified file {@link Path} with
     * the specified {@link PosixFilePermission}s applied. Ideally, this method
     * would be invoked within a try-with-resources block.
     * 
     * @param filePath
     *            the specified file {@link Path}.
     * @param permissions
     *            the specified {@link PosixFilePermission}s.
     * @return an {@link OutputStream} to write to the file
     * @throws IOException
     */
    public static OutputStream getOutputStream(final Path filePath,
            final Set<PosixFilePermission> permissions) throws IOException {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Required argument 'filePath' cannot be NULL.");
        }
        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissions' cannot be NULL.");
        }
        if (!Files.exists(filePath)) {
            /*
             * Only create the file if it does not exist, otherwise an
             * FileAlreadyExistsException will be thrown.
             */
            com.raytheon.uf.common.util.file.Files.createFile(filePath,
                    PosixFilePermissions.asFileAttribute(permissions));
        } else {
            /*
             * Ensure that the previously existing file has the correct
             * permissions.
             */
            applyFilePermissions(filePath, permissions);
        }
        return Files.newOutputStream(filePath);
    }

    /**
     * Converts the specified array of {@link PosixFilePermission}s to an
     * {@link EnumSet} of {@link PosixFilePermission}s.
     * 
     * @param permissionsToConvert
     *            the specified array of {@link PosixFilePermission}s
     * @return the {@link EnumSet} of {@link PosixFilePermission}s.
     */
    public static Set<PosixFilePermission> getPermissionsAsSet(
            final PosixFilePermission[] permissionsToConvert) {
        if (permissionsToConvert == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissionsToConvert' cannot be NULL.");
        }

        EnumSet<PosixFilePermission> permissions = EnumSet
                .noneOf(PosixFilePermission.class);
        for (PosixFilePermission permissionToConvert : permissionsToConvert) {
            permissions.add(permissionToConvert);
        }
        return permissions;
    }

    /**
     * Converts the specified array of {@link PosixFilePermission}s to
     * {@link FileAttribute}s.
     * 
     * @param permissionsToConvert
     *            the specified array of {@link PosixFilePermission}s
     * @return {@link FileAttribute}s
     */
    public static FileAttribute<Set<PosixFilePermission>> getPermissionsAsAttributes(
            final PosixFilePermission[] permissionsToConvert) {
        return PosixFilePermissions
                .asFileAttribute(getPermissionsAsSet(permissionsToConvert));
    }

    /**
     * Given a sequence of {@link FileAttribute}s, will separate the
     * {@link PosixFilePermission} attributes and other {@link FileAttribute}s
     * into a separate {@link Set} and {@link Collection} respectively.
     * 
     * @param permissions
     *            a {@link Set} containing the extracted
     *            {@link PosixFilePermission}s
     * @param revisedAttributes
     *            a {@link Collection} of {@link FileAttribute}s without any
     *            {@link PosixFilePermission}s.
     * @param attrs
     *            the sequence of {@link FileAttribute}s
     */
    protected static void separatePosixFilePermissions(
            final Set<PosixFilePermission> permissions,
            final Collection<FileAttribute<?>> revisedAttributes,
            final FileAttribute<?>... attrs) {
        if (permissions == null) {
            throw new IllegalArgumentException(
                    "Required argument 'permissions' cannot be NULL.");
        }
        if (revisedAttributes == null) {
            throw new IllegalArgumentException(
                    "Required argument 'revisedAttributes' cannot be NULL.");
        }
        if (attrs == null) {
            throw new IllegalArgumentException(
                    "Required argument 'attrs' cannot be NULL.");
        }

        /*
         * Iterate through each file attribute.
         */
        for (FileAttribute<?> attr : attrs) {
            if (attr.value() instanceof Set<?>) {
                /*
                 * Found an attribute that is a Set of "something".
                 */
                Set<?> attrValues = (Set<?>) attr.value();
                /*
                 * Determine if the Set actually contains anything and if the
                 * contents of the Set are Posix File Permissions.
                 */
                if (CollectionUtil.isNullOrEmpty(attrValues) && !(attrValues
                        .iterator().next() instanceof PosixFilePermission)) {
                    /*
                     * Not Posix File Permissions, place it into the Collection
                     * of File Attributes that excludes permissions attributes.
                     */
                    revisedAttributes.add(attr);
                    continue;
                }

                /*
                 * There is a possibility that the Posix File Permission
                 * attribute is in the attributes Collection multiple times.
                 * Since that JavaDoc dictates that all but the last occurrence
                 * of an attribute will be ignored, the existing list is cleared
                 * to ensure that it is representative of only one set of
                 * attributes.
                 */
                permissions.clear();
                /*
                 * Iterate through the full value of the attribute and collect
                 * the contents into a Set of Posix File Permissions.
                 */
                for (Object attrValue : attrValues) {
                    permissions.add((PosixFilePermission) attrValue);
                }
            } else {
                /*
                 * Attribute value is not even a Set. Place the File Attribute
                 * into the Collection of File Attributes that excludes
                 * permissions attributes.
                 */
                revisedAttributes.add(attr);
            }
        }
    }

    /**
     * Determines if the Operating System implements the Portable Operating
     * System Interface (POSIX).
     * 
     * @return {@code true} if the OS does implement POSIX; {@code false},
     *         otherwise.
     */
    protected static boolean isPosixSupported(final Path path) {
        return path.getFileSystem().supportedFileAttributeViews()
                .contains(ATTRIBUTE_POSIX);
    }

    /**
     * Verifies that the Operating System implements the Portable Operating
     * System Interface (POSIX).
     */
    private static void verifyPosixSupport(final Path path) {
        if (!isPosixSupported(path)) {
            /*
             * This OS does not support the POSIX filesystem; so, this class
             * will not work correctly.
             */
            throw new RuntimeException("As presently implemented, "
                    + IOPermissionsHelper.class.getName()
                    + " is not compatible with "
                    + path.getFileSystem().getClass().getName() + ".");
        }
    }
}