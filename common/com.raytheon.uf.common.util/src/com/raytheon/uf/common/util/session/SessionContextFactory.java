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
package com.raytheon.uf.common.util.session;

/**
 * A factory to create a session context instance.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 07, 2013 1543       djohnson     Initial creation.
 * 
 * </pre>
 * 
 * @author djohnson
 * @version 1.0
 */

public interface SessionContextFactory<T extends SessionContext> {
    /**
     * Returns the session context instance that should be used.
     * 
     * @return the session context
     */
    T getSessionContext();

    /**
     * Returns the session context class instance for the implementation.
     * 
     * @return the session context class
     */
    Class<T> getSessionContextClass();
}
