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
package com.raytheon.uf.viz.core.maps.jobs;

/**
 * Base map query job result container
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 11, 2014  #3459     randerso     Initial creation
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public abstract class AbstractMapResult {
    private AbstractMapRequest<?> request;

    private boolean canceled;

    private Throwable cause;

    public AbstractMapResult(AbstractMapRequest<?> request) {
        this.request = request;
    }

    public abstract void dispose();

    /**
     * Set request to canceled
     */
    public void cancel() {
        this.canceled = true;
    }

    /**
     * @return the canceled
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Set exception which caused the request to fail
     * 
     * @param cause
     *            the cause to set
     */
    public void setCause(Throwable cause) {
        this.cause = cause;
    }

    /**
     * Get cause of failure
     * 
     * @return the cause
     */
    public Throwable getCause() {
        return cause;
    }

    public String getName() {
        return request.getResource().getName();
    }

    /**
     * @return true if cause has been set
     */
    public boolean isFailed() {
        return cause != null;
    }
}
