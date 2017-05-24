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

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.Authorizer;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.CacheManagerAware;
import org.apache.shiro.cache.MemoryConstrainedCacheManager;
import org.apache.shiro.config.ConfigurationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.user.IUser;

/**
 * Authentication/Authorization manager
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 05, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class ShiroPermissionsManager
        implements IPermissionsManager, IRolesAndPermissionsStore {
    private static final Logger log = LoggerFactory
            .getLogger(ShiroPermissionsManager.class);

    public static final String DEFAULT_PASSWORD = "password";

    private DefaultSecurityManager securityManager;

    /**
     * Constructor with a single realm that handles both authentication and
     * authorization
     *
     * @param realm
     */
    public ShiroPermissionsManager(AuthorizingRealm realm) {
        this(realm, null);
    }

    /**
     * Constructor with separate authenticating and authorizing realms
     *
     * @param authenticator
     * @param authorizer
     *
     */
    public ShiroPermissionsManager(AuthenticatingRealm authenticator,
            AuthorizingRealm authorizer) {

        securityManager = new DefaultSecurityManager(authenticator);
        CacheManager cacheManager = new MemoryConstrainedCacheManager();
        securityManager.setCacheManager(cacheManager);
        if (authorizer != null) {
            securityManager.setAuthorizer(authorizer);
            if (authorizer instanceof CacheManagerAware) {
                authorizer.setCacheManager(cacheManager);
            }
        }

        SecurityUtils.setSecurityManager(securityManager);
    }

    @Override
    public void setThreadSubject(IUser user) {
        Subject subject = SecurityUtils.getSubject();

        if (!subject.isAuthenticated()) {
            String userName = user.uniqueId().toString();
            String password = DEFAULT_PASSWORD;
            if (user.authenticationData() != null) {
                /*
                 * TODO: this probably changes if we ever implement
                 * authentication.
                 *
                 * We will probably need to re-visit the whole IUser concept at
                 * that time
                 */
                password = user.authenticationData().toString();
            }

            UsernamePasswordToken token = new UsernamePasswordToken(userName,
                    password);
            try {
                subject.login(token);
            } catch (AuthenticationException e) {
                log.error("Unable to authenticate user: " + userName, e);
            }
        }
    }

    @Override
    public void removeThreadSubject() {
        ThreadContext.remove(ThreadContext.SUBJECT_KEY);
    }

    @Override
    public boolean isPermitted(String permission) {

        Subject subject = SecurityUtils.getSubject();
        try {
            return subject.isPermitted(permission);
        } catch (Exception e) {
            log.error("Unable to determine authorization for: "
                    + getUserName(subject), e);
        }

        return false;
    }

    @Override
    public void checkPermission(String permission, String action)
            throws AuthorizationException {

        if (!isPermitted(permission)) {
            Subject subject = SecurityUtils.getSubject();
            throw new AuthorizationException(getUserName(subject), permission,
                    action);
        }
    }

    private String getUserName(Subject subject) {
        return subject.getPrincipal().toString();
    }

    private IRolesAndPermissionsStore getRolesAndPermissionsStore() {
        Authorizer authorizer = securityManager.getAuthorizer();
        if (authorizer instanceof ModularRealmAuthorizer) {
            for (Realm realm : ((ModularRealmAuthorizer) authorizer)
                    .getRealms()) {
                if (realm instanceof IRolesAndPermissionsStore) {
                    return (IRolesAndPermissionsStore) realm;
                }
            }
        } else if (authorizer instanceof IRolesAndPermissionsStore) {
            return (IRolesAndPermissionsStore) authorizer;
        }

        throw new ConfigurationException(
                "Authorizer does not extend IRolesAndPermissionsStore");
    }

    @Override
    public RolesAndPermissions getRolesAndPermissions()
            throws AuthorizationException {
        /* Ensure subject has required permission */
        checkPermission("auth:administration",
                "retrieve roles and permissions data");

        return getRolesAndPermissionsStore().getRolesAndPermissions();
    }

    @Override
    public RolesAndPermissions saveRolesAndPermissions(
            RolesAndPermissions rolesAndPermissions)
            throws AuthorizationException {
        /* Ensure subject has required permission */
        checkPermission("auth:administration",
                "retrieve roles and permissions data");
        return getRolesAndPermissionsStore()
                .saveRolesAndPermissions(rolesAndPermissions);
    }
}
