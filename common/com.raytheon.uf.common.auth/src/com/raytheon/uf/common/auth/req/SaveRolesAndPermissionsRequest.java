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
package com.raytheon.uf.common.auth.req;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * Save user roles and permissions
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

@DynamicSerialize
public class SaveRolesAndPermissionsRequest implements IServerRequest {

    @DynamicSerializeElement
    private RolesAndPermissions rolesAndPermissions;

    /**
     * Default constructor for serialization
     */
    public SaveRolesAndPermissionsRequest() {

    }

    /**
     * Constructor
     *
     * @param rolesAndPermissions
     *            to be saved
     */
    public SaveRolesAndPermissionsRequest(
            RolesAndPermissions rolesAndPermissions) {
        this.rolesAndPermissions = rolesAndPermissions;
    }

    /**
     * @return the rolesAndPermissions
     */
    public RolesAndPermissions getRolesAndPermissions() {
        return rolesAndPermissions;
    }

    /**
     * @param rolesAndPermissions
     *            the rolesAndPermissions to set
     */
    public void setRolesAndPermissions(
            RolesAndPermissions rolesAndPermissions) {
        this.rolesAndPermissions = rolesAndPermissions;
    }
}
