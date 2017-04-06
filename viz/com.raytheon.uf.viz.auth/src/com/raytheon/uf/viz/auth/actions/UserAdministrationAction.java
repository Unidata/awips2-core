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
package com.raytheon.uf.viz.auth.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.common.auth.req.CheckAuthorizationRequest;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.auth.ui.UserAdministrationDialog;
import com.raytheon.uf.viz.core.auth.UserController;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Display the User Administration GUI
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

public class UserAdministrationAction extends AbstractHandler {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private UserAdministrationDialog adminDlg;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (isAuthorized()) {

            try {
                if (adminDlg == null || !adminDlg.isOpen()) {
                    Shell shell = HandlerUtil.getActiveShell(event);
                    adminDlg = new UserAdministrationDialog(shell);
                    adminDlg.open();
                } else {
                    adminDlg.bringToTop();
                }
            } catch (VizException e) {
                statusHandler.error(e.getLocalizedMessage(), e);
            }
        }

        return null;
    }

    private boolean isAuthorized() {
        boolean authorized = false;
        CheckAuthorizationRequest request = new CheckAuthorizationRequest(
                "auth:administration");

        try {
            authorized = (Boolean) ThriftClient.sendRequest(request);
        } catch (VizException e) {
            statusHandler.error("Unable to determine user authorization", e);
        }

        if (!authorized) {
            statusHandler.error(String.format(
                    "User '%s' is not authorized to access the User Administration GUI",
                    UserController.getUserObject().uniqueId()));
        }
        return authorized;
    }

}
