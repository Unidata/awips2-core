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
package com.raytheon.edex.services;

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.auth.user.IUser;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.IPermissionsManager;
import com.raytheon.uf.edex.auth.req.AbstractPrivilegedRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;

/**
 * Abstract privileged request handler for localization requests
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 06, 2011           mschenke  Initial creation
 * Jul 08, 2012  719      mpduff    Fix order of checks
 * May 28, 2014  3211     njensen   Updated for IAuthorizer changes
 * May 18, 2017  6242     randerso  Changed to use new roles and permissions
 *                                  framework
 *
 * </pre>
 *
 * @author mschenke
 * @param <T>
 */
public abstract class AbstractPrivilegedLocalizationRequestHandler<T extends AbstractPrivilegedRequest>
        extends AbstractPrivilegedRequestHandler<T> {

    private static final String PATH_SEPARATOR = IPathManager.SEPARATOR;

    private static final String SEPARATOR = ":";

    private static final String PERMISSION_PREFIX = "localization";

    /**
     * @param user
     * @param operation
     * @param context
     * @param fileName
     * @param myContextName
     * @return AuthorizationResponse
     * @throws AuthorizationException
     */
    protected AuthorizationResponse getAuthorizationResponse(IUser user,
            String operation, LocalizationContext context, String fileName,
            String myContextName) throws AuthorizationException {

        String contextName = context.getContextName();
        LocalizationLevel level = context.getLocalizationLevel();
        LocalizationType type = context.getLocalizationType();
        boolean contextsMatch = myContextName != null
                && myContextName.equals(contextName);
        if (level.isSystemLevel()) {
            return new AuthorizationResponse(false,
                    "Modification to system level configuration is prohibited.");
        } else if (level == LocalizationLevel.USER && contextsMatch) {
            // Don't prevent users from modifying own files
            return new AuthorizationResponse(true);
        }

        /*
         * Allow everyone to read for now
         *
         * If/when we decide to implement read permissions remove the following
         * if statement
         */
        if ("read".equals(operation)) {
            return new AuthorizationResponse(true);
        }

        String permission = buildPermissionString(operation, level, type,
                contextName, fileName);

        IPermissionsManager manager = AuthManagerFactory.getInstance()
                .getPermissionsManager();
        manager.setThreadSubject(user);
        try {
            if (manager.isPermitted(permission)) {
                return new AuthorizationResponse(true);
            }
            return notAuthorized(user, permission);
        } finally {
            manager.removeThreadSubject();
        }
    }

    private String buildPermissionString(String operation,
            LocalizationLevel level, LocalizationType type, String contextName,
            String fileName) {
        String namePart = String.join(SEPARATOR,
                fileName.split(PATH_SEPARATOR));

        String permission = String.join(SEPARATOR, PERMISSION_PREFIX, operation,
                type.toString().toLowerCase(), level.toString().toLowerCase(),
                contextName == null ? "" : contextName, namePart);
        return permission;
    }

    private AuthorizationResponse notAuthorized(IUser user, String permission) {
        return new AuthorizationResponse(false,
                "User '" + user.uniqueId()
                        + "' is not authorized to perform request needing permission: "
                        + permission);
    }
}
