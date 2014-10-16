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
package com.raytheon.uf.edex.database.cluster;

import java.util.List;

import com.raytheon.uf.edex.database.cluster.handler.IClusterLockHandler;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Cluster locking tools for the default database
 * {@link DaoConfig#DEFAULT_DB_NAME}. To access locks within the context of a
 * specific database use {@link ClusterLocker} instead.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 28, 2010 #5050      rjpeter     Initial creation from SmartInitTransaction.
 * Aug 26, 2013 #2272      bkowal      Add a function to see if a cluster suffix has
 *                                     been specified via the environment.
 * Dec 13, 2013 2555       rjpeter     Added updateExtraInfoAndLockTime and javadoc.
 * Oct 06, 2014 3702       bsteffen    Extract logic to ClusterLocker to allow separate locks per database.
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class ClusterLockUtils {
    /*
     * An optional context suffix can be included in an EDEX properties file.
     * This suffix will be appended to the details of each cluster task.
     */
    public static final String CLUSTER_SUFFIX;

    static {
        CLUSTER_SUFFIX = System.getProperty("cluster.suffix") != null ? "-"
                + System.getProperty("cluster.suffix") : "";
    }

    public enum LockState {
        SUCCESSFUL, ALREADY_RUNNING, FAILED, OLD;
    }

    private static final ClusterLocker DEFAULT_LOCKER = new ClusterLocker(
            DaoConfig.DEFAULT_DB_NAME);

    /**
     * 
     * @param taskName
     * @param details
     * @param timeOutOverride
     * @param waitForRunningToFinish
     * @return
     */
    public static ClusterTask lock(String taskName, String details,
            long timeOutOverride, boolean waitForRunningToFinish) {
        return DEFAULT_LOCKER.lock(taskName, details, timeOutOverride,
                waitForRunningToFinish);
    }

    /**
     * Attempts to lock based on the taskName/details and the current system
     * clock for checkTime. If waitForRunningToFinish it will sleep and then
     * attempt to lock again until it achieves a lock other than already
     * running. The waitForRunningToFinish is not part of the main lock logic
     * due to checkTime being keyed off something other than System clock.
     * 
     * @param taskName
     * @param details
     * @param extraInfo
     * @param timeOutOverride
     *            value in milliseconds, if the currentTime > last execution
     *            time + override, it will take the lock even if it is marked as
     *            currently running
     * @param waitForRunningToFinish
     * @return the ClusterTask that was attempted to be locked, see its
     *         LockState for the result of the lock operation. Note: Never
     *         change the ClusterTask that was returned.
     */
    public static ClusterTask lock(String taskName, String details,
            String extraInfo, long timeOutOverride,
            boolean waitForRunningToFinish) {
        return DEFAULT_LOCKER.lock(taskName, details, extraInfo,
                timeOutOverride, waitForRunningToFinish);
    }

    /**
     * Attempts to lock based on the taskName/details and the specified
     * validTime for checkTime. If waitForRunningToFinish it will sleep and then
     * attempt to lock again until it achieves a lock other than already
     * running. The waitForRunningToFinish is not part of the main lock logic
     * due to checkTime being keyed off something other than System clock. If
     * the validTime is older than the current validTime for the lock, an OLD
     * LockState will be returned.
     * 
     * @param taskName
     * @param details
     * @param validTime
     * @param timeOutOverride
     * @param waitForRunningToFinish
     * @return
     */
    public static ClusterTask lock(String taskName, String details,
            long validTime, long timeOutOverride, boolean waitForRunningToFinish) {
        return DEFAULT_LOCKER.lock(taskName, details, validTime,
                timeOutOverride, waitForRunningToFinish);
    }

    /**
     * Attempts to lock based on the taskName/details and the specified
     * lockHandler. If waitForRunningToFinish it will sleep and then attempt to
     * lock again until it achieves a lock other than already running. The
     * waitForRunningToFinish is not part of the main lock logic due to
     * checkTime being keyed off something other than System clock.
     * 
     * @param taskName
     * @param details
     * @param lockHandler
     * @param waitForRunningToFinish
     * @return
     */
    public static ClusterTask lock(String taskName, String details,
            IClusterLockHandler lockHandler, boolean waitForRunningToFinish) {
        return DEFAULT_LOCKER.lock(taskName, details, lockHandler,
                waitForRunningToFinish);
    }

    /**
     * Updates the lock time for the specified lock. IMPORTANT: No tracking is
     * done to ensure caller has lock, so only use when you know you have a
     * valid lock.
     * 
     * @param taskName
     * @param details
     * @param updateTime
     * @return
     */
    public static boolean updateLockTime(String taskName, String details,
            long updateTime) {
        return DEFAULT_LOCKER.updateLockTime(taskName, details, updateTime);
    }

    /**
     * Updates the extra info field for a cluster task. IMPORTANT: No tracking
     * is done to ensure caller has lock, so only use when you know you have a
     * valid lock.
     * 
     * @param taskName
     *            The name of the task
     * @param details
     *            The details associated with the task
     * @param extraInfo
     *            The new extra info to set
     * @return True if the update was successful, else false if the update
     *         failed
     */
    public static boolean updateExtraInfo(String taskName, String details,
            String extraInfo) {
        return DEFAULT_LOCKER.updateExtraInfo(taskName, details, extraInfo);
    }

    /**
     * Updates the extra info and lock time fields for a cluster task.
     * IMPORTANT: No tracking is done to ensure caller has lock, so only use
     * when you know you have a valid lock.
     * 
     * @param taskName
     *            The name of the task
     * @param details
     *            The details associated with the task
     * @param extraInfo
     *            The new extra info to set
     * @oaran lockTime The lock time to set
     * @return True if the update was successful, else false if the update
     *         failed
     */
    public static boolean updateExtraInfoAndLockTime(String taskName,
            String details, String extraInfo, long lockTime) {
        return DEFAULT_LOCKER.updateExtraInfoAndLockTime(taskName, details,
                extraInfo,
                lockTime);

    }

    /**
     * Looks up the specified cluster lock.
     * 
     * @param taskName
     * @param details
     * @return
     */
    public static ClusterTask lookupLock(String taskName, String details) {
        return DEFAULT_LOCKER.lookupLock(taskName, details);
    }

    /**
     * Unlocks the given cluster lock. If clear time is set, time field will be
     * reset to the epoch time. This can be useful when wanting the next check
     * to always succeed.
     * 
     * @param ct
     * @param clearTime
     * @return
     */
    public static boolean unlock(ClusterTask ct, boolean clearTime) {
        return DEFAULT_LOCKER.unlock(ct, clearTime);
    }

    /**
     * Standard unlock. Doesn't handle clearing the time due to different
     * implementations of IClusterLockHandler.
     * 
     * @param taskName
     * @param details
     * @return
     */
    public static boolean unlock(String taskName, String details) {
        return DEFAULT_LOCKER.unlock(taskName, details);
    }

    /**
     * Deletes the specified cluster lock.
     * 
     * @param taskName
     * @param details
     * @return
     */
    public static boolean deleteLock(String taskName, String details) {
        return DEFAULT_LOCKER.deleteLock(taskName, details);
    }

    /**
     * Returns all cluster locks that match the specified name.
     * 
     * @param name
     * @return
     */
    public static List<ClusterTask> getLocks(String name) {
        return DEFAULT_LOCKER.getLocks(name);

    }
}
