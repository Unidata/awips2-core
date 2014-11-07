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

import com.raytheon.uf.common.localization.FileLocker.Type;

/**
 * {@link FileInputStream} which locks the file using {@link FileLocker} when
 * the stream is opened and unlocks the file when the stream is closed.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 23, 2011            mschenke     Initial creation
 * Nov 03, 2014 3761       bsteffen     Fix constructors so lock is obtained
 *                                       before super constructor called
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class LockingFileInputStream extends FileInputStream {

    private File file;

    private final Object locker;

    /**
     * Create a new LockingFileInputStream, creates a lock on the file
     * 
     * @param file
     * @throws FileNotFoundException
     */
    public LockingFileInputStream(File file) throws FileNotFoundException {
        this(file, lockFile(file));
    }

    /**
     * Intentionally private constructor that takes a locker object to provide a
     * unique lock tied to this stream instance. This constructor should only be
     * called once the locker has been used to successful lock the provided
     * file.
     * 
     * @param file
     * @param locker
     * @throws FileNotFoundException
     */
    private LockingFileInputStream(File file, Object locker)
            throws FileNotFoundException {
        super(file);
        this.file = file;
        this.locker = locker;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            FileLocker.unlock(locker, file);
        }
    }

    private static Object lockFile(File file) throws FileNotFoundException {
        Object locker = new Object();
        boolean locked = FileLocker.lock(locker, file, Type.READ);
        if (!locked) {
            throw new IllegalStateException("Unable to obtain lock on file: "
                    + file);
        }
        if (!file.exists()) {
            FileLocker.unlock(locker, file);
            throw new FileNotFoundException("File does not exist: " + file);
        }
        return locker;

    }

}
