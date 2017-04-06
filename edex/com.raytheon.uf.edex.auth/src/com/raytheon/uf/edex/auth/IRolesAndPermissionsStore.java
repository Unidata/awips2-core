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
package com.raytheon.uf.edex.auth;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.exception.AuthorizationException;

/**
 * Interface for the roles and permissions data store
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 24, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */
public interface IRolesAndPermissionsStore {

    /**
     * Get the defined user roles and permissions
     *
     * @return the roles and permissions
     * @throws AuthorizationException
     */
    default RolesAndPermissions getRolesAndPermissions()
            throws AuthorizationException {
        throw new IllegalStateException(
                "Roles and permissions are not properly configured");
    }

    /**
     * Save the user roles and permissions
     *
     * @param rolesAndPermissions
     * @return the updated rolesAndPermissions
     * @throws AuthorizationException
     */
    default RolesAndPermissions saveRolesAndPermissions(
            RolesAndPermissions rolesAndPermissions)
            throws AuthorizationException {
        throw new IllegalStateException(
                "Roles and permissions are not properly configured");
    }
}
