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
package com.raytheon.uf.edex.esb.camel;

import java.io.File;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.component.file.GenericFileExclusiveReadLockStrategy;
import org.apache.camel.component.file.GenericFileOperations;

import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Two step file scan strategy to ensure files aren't accepted for processing
 * while they are still being written. First time a file is found its modify
 * time is checked. If the modify time is older than 1 minute (such as system
 * restart scenario) the file is immediately accepted. If the file has been
 * modified recently the time is recorded and the file is not processed at this
 * time. In, general this requires a file to be checked twice making its minimum
 * latency at least 1 full scan period before being processed.
 *
 * This class is not thread safe. A separate instance of this strategy should be
 * created for each directory.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------------
 * Sep 02, 2010           rjpeter   Initial creation
 * May 09, 2013  1989     njensen   Camel 2.11 compatibility
 * May 06, 2014  3115     bclement  Camel 2.12.3 compatibility
 * Jan 05, 2015  3800     bclement  Camel 2.14.1 compatibility
 * Oct 22, 2015  4999     nabowle   Camel 2.16.0 compatibility
 * Mar 06, 2019  7582     randerso  Made minFileAge a configurable parameter
 *
 * </pre>
 *
 * @author rjpeter
 */

public class FileChangedExclusiveReadLockStrategy
        implements GenericFileExclusiveReadLockStrategy<File> {

    private long minFileAge;

    /**
     * Nullary constructor
     *
     * defaults minFileAge to 1 minute
     */
    public FileChangedExclusiveReadLockStrategy() {
        this(TimeUtil.MILLIS_PER_MINUTE);
    }

    /**
     * Constructor
     *
     * @param minFileAge
     *            minimum age for a file to be accepted
     */
    public FileChangedExclusiveReadLockStrategy(long minFileAge) {
        this.minFileAge = minFileAge;
    }

    @Override
    public boolean acquireExclusiveReadLock(
            GenericFileOperations<File> operations, GenericFile<File> file,
            Exchange exchange) throws Exception {

        // accept file if it hasn't been modified for at list MIN_FILE_AGE,
        long modifyTime = file.getLastModified();
        long curTime = System.currentTimeMillis();
        return curTime - modifyTime > minFileAge;
    }

    @Override
    public void prepareOnStartup(GenericFileOperations<File> operations,
            GenericFileEndpoint<File> endpoint) throws Exception {
    }

    @Override
    public void setTimeout(long timeout) {
    }

    @Override
    public void setCheckInterval(long checkInterval) {
    }

    @Override
    public void setReadLockLoggingLevel(LoggingLevel level) {
        // new in camel 2.12.3
    }

    @Override
    public void setMarkerFiler(boolean arg0) {
        // new in camel 2.14.1
    }

    @Override
    public void releaseExclusiveReadLockOnAbort(
            GenericFileOperations<File> operation, GenericFile<File> file,
            Exchange exchange) throws Exception {
        // new in 2.16.0
    }

    @Override
    public void releaseExclusiveReadLockOnCommit(
            GenericFileOperations<File> operation, GenericFile<File> file,
            Exchange exchange) throws Exception {
        // new in 2.16.0
    }

    @Override
    public void releaseExclusiveReadLockOnRollback(
            GenericFileOperations<File> operation, GenericFile<File> file,
            Exchange exchange) throws Exception {
        // new in 2.16.0
    }

    @Override
    public void setDeleteOrphanLockFiles(boolean arg0) {
        // new in 2.16.0
    }
}
