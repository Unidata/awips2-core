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

/**
 * An exception indicating that another cluster node took/stole the lock.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 14, 2016  3440      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class ClusterLockStolenException extends ClusterLockException {

    private static final long serialVersionUID = 1L;

    public ClusterLockStolenException() {
        super();
    }

    public ClusterLockStolenException(String message) {
        super(message);
    }

    public ClusterLockStolenException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterLockStolenException(Throwable cause) {
        super(cause);
    }

}
