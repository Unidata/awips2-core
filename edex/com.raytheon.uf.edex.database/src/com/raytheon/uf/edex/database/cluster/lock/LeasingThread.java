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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.edex.database.cluster.lock.EdexClusterDbLockMgr.DbLockConfig;

/**
 * A thread that tracks locks held by this cluster node (JVM) and periodically
 * updates the held locks' expiration time until the lock is unlocked.
 * 
 * The primary purpose of this class is to update the expiration time on locked
 * locks to prevent other cluster nodes from taking/stealing a locked lock.
 * Should a cluster node stop responding (e.g. shutdown, crash), then this
 * thread will have stopped running. If locks were still locked, the locked
 * locks' expiration time will no longer be updated, and other cluster nodes can
 * then take/steal the lock after a period of time.
 * 
 * Essentially this class ensures other cluster nodes cannot take a locked lock
 * while this cluster node is still functional, but enables other cluster nodes
 * to take a lock after a period of time where this cluster node stops
 * responding.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 15, 2016  3440      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class LeasingThread extends Thread {

    protected static transient final Logger logger = LoggerFactory
            .getLogger(LeasingThread.class);

    protected final DbLockConfig config;

    protected final Set<String> heldLocks = new HashSet<>();

    /**
     * Constructor
     * 
     * @param config
     */
    protected LeasingThread(DbLockConfig config) {
        this.config = config;
        this.setName("Cluster Lock Leasing Thread");
        this.setDaemon(true);
    }

    @Override
    public void run() {
        while (true) {
            renewLeases();

            try {
                Thread.sleep(config.leaseTime / 3);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Updates the expiration time on locks held by this cluster node (JVM).
     */
    protected void renewLeases() {
        Set<String> locksToUpdate = null;
        synchronized (heldLocks) {
            if (!heldLocks.isEmpty()) {
                locksToUpdate = new HashSet<>(heldLocks);
            }
        }

        if (locksToUpdate != null) {
            Map<String, Object> params = new HashMap<>();
            long timeout = System.currentTimeMillis() + config.leaseTime;
            params.put("timeout", timeout);
            params.put("selfId", config.selfIdentifier);
            params.put("names", locksToUpdate);

            String sql = "update cluster_lock set expiration = :timeout where locked = true and owner = :selfId and name in (:names)";
            try {
                this.config.dao.executeSQLUpdate(sql, params);
            } catch (Throwable t) {
                logger.error("Error renewing lock leases", t);
            }
        }
    }

    /**
     * Adds a lock to be tracked that will receive periodic updates of its
     * expiration time.
     * 
     * @param lockName
     */
    protected void addLock(String lockName) {
        synchronized (heldLocks) {
            heldLocks.add(lockName);
        }
    }

    /**
     * Removes a lock from tracking so it will no longer receive periodic
     * updates of its expiration time.
     * 
     * @param lockName
     */
    protected void removeLock(String lockName) {
        synchronized (heldLocks) {
            heldLocks.remove(lockName);
        }
    }

}
