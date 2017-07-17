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

import java.nio.file.Path;

import com.raytheon.uf.common.auth.user.User;
import com.raytheon.uf.common.http.auth.BasicCredential;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.edex.auth.AuthManagerFactory;
import com.raytheon.uf.edex.auth.IPermissionsManager;
import com.raytheon.uf.edex.localization.http.LocalizationHttpException;

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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Dec 03, 2015  4834     njensen   Initial creation
 * May 18, 2017  6242     randerso  Changed to use new roles and permissions
 *                                  framework
 *
 * </pre>
 *
 * @author njensen
 */

public class LocalizationAuthorization {

    private static final String PATH_SEPARATOR = IPathManager.SEPARATOR;

    private static final String SEPARATOR = ":";

    private static final String PERMISSION_PREFIX = "localization";

    /**
     * Checks if the user is authorized to modify or delete a file at the
     * specified context and path.
     *
     * @param user
     * @param operation
     *            write or delete
     * @param context
     * @param fileName
     * @return if the user is authorized or not
     * @throws LocalizationHttpException
     */
    public static boolean isAuthorized(BasicCredential cred, String operation,
            LocalizationContext context, String fileName)
            throws LocalizationHttpException {
        if ("read".equals(operation)) {
            /*
             * Allow everyone to read for now
             *
             * If/when we decide to implement read permissions remove this if
             * statement
             */
            return true;
        } else if (cred == null) {
            /*
             * TODO consider mapping requests without credentials to a default
             * user for simple anonymous access.
             */
            return false;
        }

        String contextName = context.getContextName();
        LocalizationLevel level = context.getLocalizationLevel();
        LocalizationType type = context.getLocalizationType();

        if (level == LocalizationLevel.USER
                && cred.getUserid().equals(contextName)) {
            // Don't prevent users from modifying own files
            return true;
        }

        String permission = buildPermissionString(operation, level, type,
                contextName, fileName);

        IPermissionsManager manager = AuthManagerFactory.getInstance()
                .getPermissionsManager();

        /*
         * TODO The password has not been checked at this point and is not being
         * passed along to the permissions manager. The User object supports a
         * concept of IAuthenticationData which could be used to pass the
         * password along but there is no standard way for the manager to get
         * the password out.
         */

        manager.setThreadSubject(new User(cred.getUserid()));
        try {
            return manager.isPermitted(permission);
        } finally {
            manager.removeThreadSubject();
        }
    }

    private static String buildPermissionString(String operation,
            LocalizationLevel level, LocalizationType type, String contextName,
            String fileName) {
        String namePart = String.join(SEPARATOR,
                fileName.split(PATH_SEPARATOR));

        String permission = String.join(SEPARATOR, PERMISSION_PREFIX, operation,
                type.toString().toLowerCase(), level.toString().toLowerCase(),
                contextName == null ? "" : contextName, namePart);
        return permission;
    }

    public static boolean isContextAuthorized(BasicCredential cred,
            String operation, Path relative) {
        /*
         * If reads are ever limited then this should be converted into a
         * specific permission.
         */
        return "read".equals(operation);
    }

}
