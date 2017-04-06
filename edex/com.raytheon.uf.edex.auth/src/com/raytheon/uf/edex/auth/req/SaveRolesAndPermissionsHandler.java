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
package com.raytheon.uf.edex.auth.req;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.req.SaveRolesAndPermissionsRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.IPermissionsManager;

/**
 * Handler for GetRolesAndPermissionsRequest
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

public class SaveRolesAndPermissionsHandler
        implements IRequestHandler<SaveRolesAndPermissionsRequest> {

    @Override
    public RolesAndPermissions handleRequest(
            SaveRolesAndPermissionsRequest request)
            throws AuthorizationException {

        IPermissionsManager permissionsManager = AuthManagerFactory
                .getInstance().getPermissionsManager();

        return permissionsManager
                .saveRolesAndPermissions(request.getRolesAndPermissions());
    }
}
