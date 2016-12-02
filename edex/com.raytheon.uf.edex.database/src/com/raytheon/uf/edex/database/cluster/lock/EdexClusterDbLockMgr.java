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

import java.util.Objects;
import java.util.concurrent.locks.Lock;

import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Implementation of EdexClusterLockMgr that uses a database to manage locks.
 * Also starts the LeasingThread to track locked locks and ensure they do not
 * expire until they are unlocked.
 * 
 * There is no fairness policy, i.e. locks are not granted in order, if multiple
 * cluster nodes are going for the same lock, it is non-deterministic as to
 * which cluster node will acquire the lock.
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

public class EdexClusterDbLockMgr implements EdexClusterLockMgr {

    protected static class DbLockConfig {

        protected DbLockConfig(String selfIdentifier, String dbName,
                long sleepTime, long leaseTime) {
            this.selfIdentifier = selfIdentifier;
            this.dbName = dbName;
            this.sleepTime = sleepTime;
            this.leaseTime = leaseTime;
            this.dao = new CoreDao(DaoConfig.forDatabase(dbName));
        }

        /** the owner name to use when attempting to acquire the lock */
        protected final String selfIdentifier;

        /** the db name to make a DAO to use */
        protected final String dbName;

        /** The amount of time to wait between attempts to lock a lock. */
        protected final long sleepTime;

        /**
         * The amount of time to add to an expiration time of a locked lock to
         * ensure it does not expire as long as the JVM is still running well.
         */
        protected final long leaseTime;

        protected final CoreDao dao;
    }

    protected final DbLockConfig config;

    protected final LeasingThread leasingThread;

    /*
     * TODO Spring should make this class with a custom beanId, we should inject
     * it where it is desirable to use
     */

    /**
     * Constructor
     * 
     * @param selfIdentifier
     * @param dbName
     * @param sleepTime
     * @param leaseTime
     */
    public EdexClusterDbLockMgr(String selfIdentifier, String dbName,
            long sleepTime, long leaseTime) {
        Objects.requireNonNull(selfIdentifier);
        Objects.requireNonNull(dbName);
        if (sleepTime < 0) {
            throw new IllegalArgumentException(
                    "sleep time must be a whole number");
        }
        if (leaseTime < 0) {
            throw new IllegalArgumentException(
                    "lease time must be a whole number");
        }

        this.config = new DbLockConfig(selfIdentifier, dbName, sleepTime,
                leaseTime);

        leasingThread = new LeasingThread(this.config);
        leasingThread.start();
    }

    @Override
    public Lock allocateLock(String name) {
        return new EdexClusterDbLock(name, config, leasingThread);
    }

}