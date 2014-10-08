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

import java.util.Collections;
import java.util.List;

import com.raytheon.uf.common.auth.resp.UserNotAuthenticated;
import com.raytheon.uf.common.auth.resp.UserNotAuthorized;
import com.raytheon.uf.common.auth.user.IAuthenticationData;
import com.raytheon.uf.common.auth.user.IPermission;
import com.raytheon.uf.common.auth.user.IRole;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.auth.user.User;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.INotAuthHandler;

/**
 * Default user manager for authorization with EDEX
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 6, 2014  3398      bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class BasicUserManager implements IUserManager {

    private static final IUFStatusHandler log = UFStatus
            .getHandler(BasicUserManager.class);

    private static final INotAuthHandler NOT_AUTH_HANDLER = new INotAuthHandler() {
        @Override
        public Object notAuthenticated(UserNotAuthenticated response)
                throws VizException {
            throw new VizException(
                    "Could not perform request, user is not authenticated with server.");
        }

        @Override
        public Object notAuthorized(UserNotAuthorized response)
                throws VizException {
            throw new VizException(response.getMessage());
        }
    };

    private volatile IUser user;

    /**
     * 
     */
    public BasicUserManager() {
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#getUserObject()
     */
    @Override
    public IUser getUserObject() {
        IUser rval;
        if (this.user == null) {
            String userId = System.getProperty("user.name");
            if (userId == null || userId.isEmpty()) {
                log.error("Unable to find user.name in system properties");
                rval = null;
            } else {
                rval = new User(userId);
                this.user = rval;
            }
        } else {
            rval = user;
        }
        return rval;
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#updateUserObject(com.raytheon.uf.common.auth.user.IUser, com.raytheon.uf.common.auth.user.IAuthenticationData)
     */
    @Override
    public void updateUserObject(IUser user, IAuthenticationData authData) {
        this.user = user;
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#getNotAuthHandler()
     */
    @Override
    public INotAuthHandler getNotAuthHandler() {
        return NOT_AUTH_HANDLER;
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#getPermissions(java.lang.String)
     */
    @Override
    public List<IPermission> getPermissions(String application) {
        return Collections.emptyList();
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#getRoles(java.lang.String)
     */
    @Override
    public List<IRole> getRoles(String application) {
        return Collections.emptyList();
    }

    /* (non-Javadoc)
     * @see com.raytheon.uf.viz.core.auth.IUserManager#updateUserObject(java.lang.String, com.raytheon.uf.common.auth.user.IAuthenticationData)
     */
    @Override
    public void updateUserObject(String userId, IAuthenticationData authData) {
        user = new User(userId);
    }

}
