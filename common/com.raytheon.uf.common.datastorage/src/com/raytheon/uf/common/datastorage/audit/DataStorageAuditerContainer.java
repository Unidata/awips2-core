/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastorage.audit;

/**
 * Container for the {@link IDataStorageAuditer} to use within a JVM.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 23, 2021 8608       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class DataStorageAuditerContainer {

    private static final DataStorageAuditerContainer instance = new DataStorageAuditerContainer();

    private static final Object LOCK = new Object();

    private IDataStorageAuditer auditer;

    /**
     * Private constructor to prevent instantiation.
     */
    private DataStorageAuditerContainer() {
    }

    /**
     * @return the auditer container instance
     */
    public static DataStorageAuditerContainer getInstance() {
        return instance;
    }

    /**
     * @return the auditer implementation
     */
    public IDataStorageAuditer getAuditer() {
        return auditer;
    }

    /**
     * Set the auditer implementation to use. This should be called once on
     * startup.
     *
     * @param auditer
     *            the auditer to use
     */
    public void setAuditer(IDataStorageAuditer auditer) {
        synchronized (LOCK) {
            if (this.auditer != null) {
                throw new IllegalStateException(
                        "Auditer already set: " + this.auditer);
            }
            this.auditer = auditer;
        }
    }
}
