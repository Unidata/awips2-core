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
package com.raytheon.uf.common.util;

import java.io.File;
import java.nio.file.WatchService;

/**
 * Implementation of {@link IFileModifiedWatcher} that watches the last modified
 * time of the file. This class is not thread-safe. Intentionally
 * package-private, only FileUtil should be constructing instances.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 12, 2013 1778       djohnson     Initial creation
 * Aug 07, 2014 3502       bclement     removed logger
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 * @deprecated use {@link WatchService}
 */
@Deprecated
class FileLastModifiedTimeWatcher implements IFileModifiedWatcher {

    private final File file;

    private long configFileLastModified;

    /**
     * Constructor.
     * 
     * @param file
     *            the file to watch
     */
    FileLastModifiedTimeWatcher(File file) {
        this.file = file;
        this.configFileLastModified = file.lastModified();
    }

    @Override
    public boolean hasBeenModified() {
        final long currentConfigFileLastModified = file.lastModified();
        final boolean fileModified = (currentConfigFileLastModified != configFileLastModified);

        if (fileModified) {
            configFileLastModified = currentConfigFileLastModified;
        }
        return fileModified;
    }
}
