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
package com.raytheon.uf.viz.core.localization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient.IStreamHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Downloads a file to the specified location.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 16, 2015  3978      njensen     Initial creation
 * Mar 03, 2015  3978      njensen     Added debug when no bytes processed
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class DownloadFileStreamHandler implements IStreamHandler {

    protected static final IUFStatusHandler logger = UFStatus
            .getHandler(DownloadFileStreamHandler.class);

    protected File localFile;

    public DownloadFileStreamHandler(File localFile) {
        this.localFile = localFile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.comm.HttpClient.IStreamHandler#handleStream(java
     * .io.InputStream)
     */
    @Override
    public void handleStream(InputStream is) throws CommunicationException {
        try {
            long bytesReceived = Files.copy(is, localFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            if (bytesReceived == 0) {
                logger.debug("WARNING: File retrieved from server appears empty: "
                        + localFile);
            }
        } catch (IOException e) {
            logger.error("Error writing file " + localFile, e);
        }

    }

}
