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
package com.raytheon.uf.edex.database.cluster.lock;

import java.util.concurrent.locks.Lock;

/**
 * Interface for getting {@link java.util.concurrent.locks.Lock} instances that
 * lock across an EDEX cluster.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 01, 2016  3440      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public interface EdexClusterLockMgr {

    /**
     * Gets the Lock object for the specified name. Note this does not mean the
     * lock was acquired, you must call one of the lock() or tryLock() methods
     * to acquire the lock, and if the lock action is successful you must unlock
     * the lock, typically in a finally block.
     * 
     * @see java.util.concurrent.locks.Lock
     * @param name
     * @throws ClusterLockException
     * @return
     */
    public Lock allocateLock(String name);

}
