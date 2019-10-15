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
package com.raytheon.uf.viz.core.auth;

/**
 * Defines a method of retrieving the {@link IUserManager} implementation. All
 * access to this interface should be constrained to {@link UserController}.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 04, 2013 1451       djohnson    Initial creation
 * Jun 11, 2019 7867       tgurney     Make interface public - allow access
 *                                     by {@link java.util.ServiceLoader}.
 *                                     Fix for Java 9 changes to ServiceLoader.
 *
 * </pre>
 *
 * @author djohnson
 */
public interface IUserManagerLoader {
    /**
     * Load the {@link IUserManager} that should be used on the system.
     *
     * @return the user manager
     */
    IUserManager getUserManager();
}
