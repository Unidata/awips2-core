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

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.req.CheckAuthorizationRequest;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.edex.auth.authentication.EmptyAuthenticationStorage;
import com.raytheon.uf.edex.auth.authentication.HonorSystemAuthenticator;
import com.raytheon.uf.edex.auth.authorization.AllowAllAuthorizer;

/**
 * Singleton class which plugins should register their authentication
 * implementations to.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------
 * May 21, 2010           mschenke  Initial creation
 * May 28, 2014  3211     njensen   Added allowEverything()
 * Apr 06, 2017  6217     randerso  Added support IPermissionsManager
 *
 * </pre>
 *
 * @author mschenke
 */

public class AuthManagerFactory {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AuthManagerFactory.class);

    /** The instance for the factory */
    private static AuthManagerFactory instance = new AuthManagerFactory();

    /** The implementing auth manager */
    private volatile AuthManager manager;

    private volatile IPermissionsManager permissionsManager;

    private AuthManagerFactory() {

    }

    public static AuthManagerFactory getInstance() {
        return instance;
    }

    public void setManager(AuthManager manager) {
        this.manager = manager;
    }

    public AuthManager getManager() {
        if (manager == null) {
            synchronized (this) {
                if (manager == null) {
                    manager = allowEverything();
                }
            }
        }
        return manager;
    }

    public void setPermissionsManager(IPermissionsManager permissionsManager) {
        this.permissionsManager = permissionsManager;
    }

    /**
     * Returns the permissions manager
     *
     * @return the permissions manager
     */
    public IPermissionsManager getPermissionsManager() {
        if (permissionsManager == null) {
            synchronized (this) {
                if (permissionsManager == null) {
                    permissionsManager = allowNothing();
                }
            }
        }
        return permissionsManager;
    }

    /**
     * Initializes an AuthManager which has members that allow everything
     * through. Also logs an error since this should not be encountered outside
     * of a development environment unless something is misconfigured.
     *
     * @return an auth manager that allows everything
     */
    private AuthManager allowEverything() {
        IllegalStateException throwable = new IllegalStateException(
                "Unable to perform priviledged request validation, AuthManager not set. ALL REQUESTS WILL BE EXECUTED!");
        statusHandler.handle(Priority.PROBLEM, throwable.getLocalizedMessage(),
                throwable);

        AuthManager manager = new AuthManager();
        manager.setAuthenticationStorage(new EmptyAuthenticationStorage());
        manager.setAuthenticator(new HonorSystemAuthenticator());
        manager.setAuthorizer(new AllowAllAuthorizer());
        return manager;
    }

    /*
     * Initializes an IPermissionsManager that allows nothing. This is the
     * default if a valid implementations is not injected via spring xml
     */
    private IPermissionsManager allowNothing() {
        IllegalStateException throwable = new IllegalStateException(
                "Unable to perform permission validation, permissionsManager not set. ALL REQUESTS WILL BE DENIED!");
        statusHandler.handle(Priority.PROBLEM, throwable.getLocalizedMessage(),
                throwable);

        return new IPermissionsManager() {
            /**
             * Sets the subject on the current thread
             *
             * @param user
             *            user object
             */
            @Override
            public void setSubject(IUser user) {
                // do nothing
            }

            /**
             * Remove the subject from the current thread
             */
            @Override
            public void removeSubject() {
                // do nothing
            }

            /**
             * Query whether the current subject has permission
             *
             * This API is useful for determining whether a user has a
             * permission to change options presented to the user, or disabling
             * GUI elements. Normally server side code will use the
             * {@link #checkPermission(IUser, String, String)} method below.
             *
             * For client side code see {@link CheckAuthorizationRequest}
             *
             * @param permission
             *            the permission
             * @return true if user has permission
             */
            @Override
            public boolean isPermitted(String permission) {
                // default method denies all requests
                return false;
            }

            /**
             * Check if the current subject has permission to perform the
             * desired action
             *
             * This API is recommended for use in server side code so we get
             * consistently formatted AuthorizationExceptions
             *
             * @param permission
             *            the permission
             * @param action
             *            string describing the desired action to be displayed
             *            in exception if user does not have permission
             * @throws AuthorizationException
             *             if user does not have required permission
             */
            @Override
            public void checkPermission(String permission, String action)
                    throws AuthorizationException {
                // default method denies all requests
                throw new AuthorizationException("unknown", permission, action);
            }
        };

    }
}
