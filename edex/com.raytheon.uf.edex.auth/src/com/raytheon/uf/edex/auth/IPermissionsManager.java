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

/**
 * Permissions Manager interface
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 12, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */
public interface IPermissionsManager extends IRolesAndPermissionsStore {

    /**
     * Sets the subject on the current thread.
     *
     * {@link #removeThreadSubject()} should be called on the same thread when
     * the protected operation is complete.
     *
     * @param user
     *            user object
     */
    public void setThreadSubject(IUser user);

    /**
     * Remove the subject from the current thread
     */
    public void removeThreadSubject();

    /**
     * Query whether the current subject has permission
     *
     * This API is useful for determining whether a user has a permission to
     * change options presented to the user, or disabling GUI elements. Normally
     * server side code will use the {@link #checkPermission(String, String)}
     * method below.
     *
     * For client side code see {@link CheckAuthorizationRequest}
     *
     * @param permission
     *            the permission
     * @return true if user has permission
     */
    public boolean isPermitted(String permission);

    /**
     * Check if the current subject has permission to perform the desired action
     *
     * This API is recommended for use in server side code so we get
     * consistently formatted AuthorizationExceptions
     *
     * @param permission
     *            the permission
     * @param action
     *            string describing the desired action to be displayed in
     *            exception if user does not have permission
     * @throws AuthorizationException
     *             if user does not have required permission
     */
    public void checkPermission(String permission, String action)
            throws AuthorizationException;
}