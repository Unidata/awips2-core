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

import java.io.InputStream;
import java.util.Date;

import com.raytheon.uf.common.localization.exception.LocalizationException;

/**
 * An interface representing a localization file conceptually. Mimics common
 * file usage while hiding the implementation details of a localization file and
 * the localization store.
 * 
 * Code that uses the localization plugin(s) should strive to use the interface
 * instead of the implementation classes.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 18, 2015  3806      njensen     Initial creation
 * Nov 12, 2015  4834      njensen     Added NON_EXISTENT_CHECKSUM and DIRECTORY_CHECKSUM
 * Dec 09, 2015  4834      njensen     Added delete()
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public interface ILocalizationFile {

    /**
     * Checksum indicating a non-existent file. This is used primarily when
     * creating a new file or deleting an existing file. On create of new file,
     * the previous checksum should be the non-existent checksum. On delete of
     * an existing file, the new checksum should be the non-existent checksum.
     */
    public static final String NON_EXISTENT_CHECKSUM = "NON_EXISTENT_CHECKSUM";

    /**
     * Checksum denoting a directory and not a file.
     */
    public static final String DIRECTORY_CHECKSUM = "DIRECTORY_CHECKSUM";

    /**
     * Checks if the file exists or not in the localization store
     * 
     * @return true if the file exists, otherwise false
     */
    public boolean exists();

    /**
     * Gets the checksum of the file from the localization store
     * 
     * @return the checksum of the file, or null if the file does not yet exist
     */
    public String getCheckSum();

    /**
     * Gets the localization context associated with this file
     * 
     * @return the context this file belongs to
     */
    public LocalizationContext getContext();

    /**
     * Gets the file name and relative path (not including the context
     * directories) in the localization store
     * 
     * @return the relative path and name of the file
     */
    public String getPath();

    /**
     * Gets the timestamp of the file in the localization store
     * 
     * @return the timestamp of the file, or null if the file does not yet exist
     */
    public Date getTimeStamp();

    /**
     * Checks if this file represents a directory in the localization store
     * 
     * @return true if it represents a directory, otherwise false
     */
    public boolean isDirectory();

    /**
     * Creates an InputStream for retrieving and reading the contents of the
     * file. Calling code should close the stream after use, preferably with
     * try-with-resources and {@link AutoCloseable} such as:
     * 
     * <pre>
     * try (InputStream is = f.openInputStream()){
     *     data = parser.unmarshal(is);
     *     return data;
     * }     
     * </pre>
     * 
     * This intentionally returns an InputStream so calling code does not have
     * to know about the underlying localization store. Please do not cast the
     * InputStream; in the future the underlying type of the InputStream may
     * change.
     * 
     * @return the InputStream to be used for reading the file contents
     * @throws LocalizationException
     */
    public InputStream openInputStream() throws LocalizationException;

    /**
     * Creates a SaveableOutputStream for writing and saving the contents of the
     * file. Calling code should close the stream after use, preferably with
     * try-with-resources and {@link AutoCloseable} such as:
     * 
     * <pre>
     * try (SaveableOutputStream sos = f.openOutputStream()) {
     *     sos.write(data);
     *     sos.save();
     * }
     * </pre>
     * 
     * @return the OutputStream to be used for writing contents to the file
     * @throws LocalizationException
     */
    public SaveableOutputStream openOutputStream() throws LocalizationException;

    /**
     * Deletes the file from the localization store.
     * 
     * @throws LocalizationException
     */
    public void delete() throws LocalizationException;

}
