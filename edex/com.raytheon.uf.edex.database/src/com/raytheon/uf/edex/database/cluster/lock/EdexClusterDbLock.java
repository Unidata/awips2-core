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

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.HibernateException;
import org.hibernate.LockMode;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.edex.database.cluster.lock.EdexClusterDbLockMgr.DbLockConfig;

/**
 * An implementation of {@link java.util.concurrent.locks.Lock} for using a
 * database as the backing mechanism for managing locks between edex cluster
 * nodes.
 * 
 * There is no fairness policy, i.e. locks are not granted in order, if multiple
 * cluster nodes are going for the same lock, it is non-deterministic as to
 * which cluster node will acquire the lock.
 * 
 * This implementation does not support a reentrant concept. If you attempt to
 * lock a lock that you already have locked, it will fail to lock.
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

@Entity
@Table(name = "cluster_lock")
public class EdexClusterDbLock implements Lock, Serializable {

    private static final long serialVersionUID = 1L;

    protected static transient final Logger logger = LoggerFactory
            .getLogger(EdexClusterDbLock.class);

    /** the name of the lock */
    @Id
    protected final String name;

    /** whether or not the lock is locked */
    @Column(nullable = false)
    protected boolean locked;

    /**
     * when the lock will expire and be able to be locked by other cluster nodes
     */
    @Column(nullable = false)
    protected long expiration;

    /** a unique id identifying the cluster node who owns the lock */
    @Column(nullable = true)
    protected String owner;

    @Transient
    protected transient final DbLockConfig config;

    @Transient
    protected transient final LeasingThread leasingThread;

    /**
     * Constructor
     * 
     * @param name
     * @param config
     */
    protected EdexClusterDbLock(String name, DbLockConfig config,
            LeasingThread leasingThread) {
        this.name = Objects.requireNonNull(name);
        this.config = Objects.requireNonNull(config);
        this.leasingThread = Objects.requireNonNull(leasingThread);
    }

    /**
     * Constructor for Hibernate usage only. Do not call this.
     */
    protected EdexClusterDbLock() {
        this.name = null;
        this.config = null;
        this.leasingThread = null;
    }

    @Override
    public void lock() {
        while (!lockTheLock()) {
            try {
                Thread.sleep(config.sleepTime);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }

        if (tryLock()) {
            return;
        }

        while (!lockTheLock()) {
            Thread.sleep(config.sleepTime);
        }
    }

    @Override
    public boolean tryLock() {
        return lockTheLock();
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
        long current = System.currentTimeMillis();
        long toWait = unit.toMillis(time);
        long acquireBy = current + toWait;
        boolean gotLock = tryLock();
        if (gotLock) {
            return true;
        }

        while (acquireBy >= System.currentTimeMillis()) {
            Thread.sleep(Math.min(config.sleepTime,
                    acquireBy - System.currentTimeMillis()));
            gotLock = tryLock();
            if (gotLock) {
                break;
            }
        }

        return gotLock;
    }

    @Override
    public void unlock() {
        unlockTheLock();
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException(
                "Conditions not supported by EdexClusterDbLock at this time");
    }

    /**
     * Makes one attempt to lock the actual lock in the database. If the lock
     * does not exist, creates the row in the database. If the lock does exist,
     * checks if the existing lock has expired or not to determine whether to
     * attempt to lock. Returns whether or not the lock was successfully locked.
     * 
     * @return
     */
    private boolean lockTheLock() {
        StatelessSession s = null;
        Transaction tx = null;
        EdexClusterDbLock dbLock = null;

        try {
            s = this.config.dao.getSessionFactory().openStatelessSession();
            tx = s.beginTransaction();

            /*
             * get the current state of the lock in the database as it may have
             * changed state since the lock in memory was originally retrieved
             */
            dbLock = (EdexClusterDbLock) s.get(EdexClusterDbLock.class, name,
                    LockMode.PESSIMISTIC_WRITE);
            if (dbLock == null) {
                // lock does not exist in database
                try {
                    this.locked = true;
                    this.expiration = System.currentTimeMillis()
                            + config.leaseTime;
                    this.owner = this.config.selfIdentifier;
                    s.insert(this);
                    tx.commit();
                    leasingThread.addLock(this.name);
                    logger.debug(
                            "Successfully created and locked cluster lock ["
                                    + name + "]");
                    return true;
                } catch (Exception e) {
                    // most likely another cluster node made the lock row
                    tx.rollback();
                    tx = s.beginTransaction();
                    dbLock = (EdexClusterDbLock) s.get(EdexClusterDbLock.class,
                            name, LockMode.PESSIMISTIC_WRITE);
                    if (dbLock == null) {
                        throw new ClusterLockException(
                                "Error getting cluster lock for name " + name,
                                e);
                    }
                }
            }

            if (dbLock.locked
                    && dbLock.expiration > System.currentTimeMillis()) {
                /*
                 * another cluster node currently holds the lock, or the
                 * developer tried to lock a lock they already had locked
                 */
                tx.rollback();
                return false;
            }

            dbLock.locked = true;
            dbLock.expiration = System.currentTimeMillis() + config.leaseTime;
            dbLock.owner = this.config.selfIdentifier;
            s.update(dbLock);
            tx.commit();
            leasingThread.addLock(this.name);
            logger.debug("Successfully locked cluster lock [" + name + "]");

            /* successful lock, update in memory lock */
            this.locked = dbLock.locked;
            this.expiration = dbLock.expiration;
            this.owner = dbLock.owner;
            return true;
        } catch (Exception e) {
            logger.error("Error locking cluster lock [" + name + "]", e);
            try {
                tx.rollback();
                if (dbLock != null) {
                    this.expiration = dbLock.expiration;
                    this.locked = dbLock.locked;
                    this.owner = dbLock.owner;
                }
            } catch (HibernateException he) {
                logger.error("Error rolling back lock transaction", he);
            }
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (HibernateException e) {
                    logger.error("Error closing lock session", e);
                }
            }
        }

        return false;
    }

    /**
     * Makes one attempt to unlock the lock in the database. A successful unlock
     * will delete the row in the database. If the lock was never locked, throws
     * a ClusterLockException. If the lock is held by a lock owner other than
     * this one, throws a ClusterLockStolenException.
     * 
     * @param lock
     * @throws ClusterLockException
     * @throws ClusterLockStolenException
     */
    private void unlockTheLock()
            throws ClusterLockException, ClusterLockStolenException {
        StatelessSession s = null;
        Transaction tx = null;
        EdexClusterDbLock dbLock = null;

        try {
            leasingThread.removeLock(this.name);
            s = this.config.dao.getSessionFactory().openStatelessSession();
            tx = s.beginTransaction();

            /*
             * get the current state of the lock in the database as it may have
             * changed state since the lock in memory was originally retrieved
             */
            dbLock = (EdexClusterDbLock) s.get(EdexClusterDbLock.class, name,
                    LockMode.PESSIMISTIC_WRITE);

            if (dbLock == null) {
                /*
                 * this shouldn't be possible unless a developer calls
                 * Lock.unlock() without having first locked it since a
                 * lockTheLock() call will create the row
                 */
                throw new ClusterLockException("Attempted to unlock " + name
                        + " lock when it was not locked");
            }

            if (!this.config.selfIdentifier.equals(dbLock.owner)) {
                // another cluster node must have grabbed the lock
                String msg = null;
                if (dbLock.owner != null) {
                    msg = dbLock.owner;
                } else {
                    msg = "Unknown cluster node";
                }
                msg += " stole lock " + dbLock.name;
                throw new ClusterLockStolenException(msg);
            }

            s.delete(dbLock);
            tx.commit();
            logger.debug("Successfully unlocked and deleted cluster lock ["
                    + name + "]");

            /*
             * successful unlock, hopefully the developer will stop using this
             * instance, but just in case, update in memory lock
             */
            this.locked = false;
            this.owner = null;
        } catch (Exception e) {
            /*
             * TODO Should there be some retries in here in case the database is
             * not responding? If so, how many retries or how long do we wait?
             * Even if we fail to unlock, the lease will expire shortly since we
             * removed the lock from the leasing thread...
             */
            if (tx != null) {
                try {
                    tx.rollback();
                    if (dbLock != null) {
                        this.locked = dbLock.locked;
                        this.owner = dbLock.owner;
                        this.expiration = dbLock.expiration;
                    }
                } catch (HibernateException he) {
                    logger.error("Error rolling back unlock transaction", he);
                }
            }
            if (e instanceof ClusterLockException) {
                throw e;
            }
            throw new ClusterLockException(
                    "Error unlocking cluster lock [" + name + "]", e);
        } finally {
            if (s != null) {
                try {
                    s.close();
                } catch (HibernateException e) {
                    logger.error("Error closing unlock session", e);
                }
            }
        }
    }

}
