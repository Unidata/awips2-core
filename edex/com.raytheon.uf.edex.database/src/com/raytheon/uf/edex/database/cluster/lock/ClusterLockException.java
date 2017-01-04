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
 * An exception for problems with cluster locks.
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

public class ClusterLockException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClusterLockException() {
        super();
    }

    public ClusterLockException(String message) {
        super(message);
    }

    public ClusterLockException(String message, Throwable cause) {
        super(message, cause);
    }

    public ClusterLockException(Throwable cause) {
        super(cause);
    }

}
