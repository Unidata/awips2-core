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
package com.raytheon.uf.edex.localization.http.scheme;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.auth.AuthManager;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.authorization.IAuthorizer;
import com.raytheon.uf.edex.auth.roles.IRoleStorage;
import com.raytheon.uf.edex.localization.http.LocalizationHttpException;
import com.raytheon.uf.edex.localization.http.LocalizationHttpService;

/**
 * Checks if the localization action is authorized.
 * 
 * TODO: This code was largely borrowed/copied from
 * AbstractPrivilegedLocalizationRequestHandler. Localization is advanced enough
 * that it should have separate mechanisms for managing permissions than the
 * rest of the system.
 * 
 * TODO: The code in this class needs to be more adaptable for future
 * capabilities. One possible idea is connecting to an entirely separate server
 * for gets or puts. Another idea is putting files (e.g. at SITE level) in a
 * staging area where they can be reviewed before becoming the official site
 * config file.
 * 
 * TODO: Code here or in LocalizationHttpService needs to authenticate that a
 * user is who they claim to be.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 3, 2015   4834      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class LocalizationAuthorization {

    private static final IUFStatusHandler log = UFStatus
            .getHandler(LocalizationHttpService.class);

    private static final String PATH_SEPARATOR = IPathManager.SEPARATOR;

    private static final String SEPARATOR = ".";

    private static final String ROLE_PREFIX = "com.raytheon.localization";

    private static final String APPLICATION = "Localization";

    /**
     * Checks if the user is authorized to modify or create a file at the
     * specified context and path.
     * 
     * @param user
     * @param context
     * @param fileName
     * @return if the user is authorized or not
     * @throws LocalizationHttpException
     */
    public static boolean isPutAuthorized(String user,
            LocalizationContext context, String fileName)
            throws LocalizationHttpException {
        String contextName = context.getContextName();
        LocalizationLevel level = context.getLocalizationLevel();
        LocalizationType type = context.getLocalizationType();
        if (level.isSystemLevel()) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Cannot use REST service to put localization files at system levels");
        } else if (level == LocalizationLevel.USER && user.equals(contextName)) {
            // Don't prevent users from modifying own files
            return true;
        }

        AuthManager manager = AuthManagerFactory.getInstance().getManager();
        IAuthorizer auth = manager.getAuthorizer();
        Set<String> definedPermissions = new HashSet<String>();
        if (auth instanceof IRoleStorage) {
            String[] permissions;
            try {
                permissions = ((IRoleStorage) auth)
                        .getAllDefinedPermissions(APPLICATION);
            } catch (AuthorizationException e) {
                log.error("Error determining localization permissions", e);
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                        LocalizationHttpService.SERVER_ERROR);
            }
            for (String permission : permissions) {
                definedPermissions.add(permission.toLowerCase());
            }
        }

        // First round check com.raytheon.localization.level
        // Second round check com.raytheon.localization.level.name
        for (int i = 0; i < 2; ++i) {
            String contextNameToUse = i > 0 ? contextName : null;
            String roleId = buildRoleId(level, type, contextNameToUse, fileName);

            // check most specific to least specific
            // com.raytheon.localization.<level>.(<specificLevel>.)/type/path/name/
            int minLength = roleId.length() - fileName.length() - 1;
            do {
                try {
                    if (auth.isAuthorized(roleId, user, APPLICATION)) {
                        return true;
                    } else if (definedPermissions
                            .contains(roleId.toLowerCase())) {
                        /*
                         * User not authorized and this roleId is explicitly
                         * defined
                         */
                        return false;
                    }
                } catch (AuthorizationException e) {
                    log.error("Error determining localization permissions", e);
                    throw new LocalizationHttpException(
                            HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                            LocalizationHttpService.SERVER_ERROR);
                }

                roleId = roleId.substring(0,
                        roleId.lastIndexOf(PATH_SEPARATOR, roleId.length()));
            } while (roleId.length() >= minLength);
        }

        return false;
    }

    private static String buildRoleId(LocalizationLevel level,
            LocalizationType type, String contextName, String fileName) {
        StringBuilder sb = new StringBuilder(256);
        sb.append(ROLE_PREFIX).append(SEPARATOR).append(level);
        if (contextName != null) {
            sb.append(SEPARATOR).append(contextName);
        }
        sb.append(PATH_SEPARATOR).append(type);
        sb.append(PATH_SEPARATOR).append(fileName);
        return sb.toString();
    }

}
