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
package com.raytheon.uf.viz.auth;

import org.eclipse.core.expressions.PropertyTester;

import com.raytheon.uf.common.auth.req.CheckAuthorizationRequest;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Property Tester to verify a user has a specified permission
 *
 * Can be used in plugin.xml enabledWhen clause to gray out a menu item if user
 * does not have the necessary permission.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 25, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class AuthPropertyTester extends PropertyTester {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AuthPropertyTester.class);

    /**
     * Constructor
     */
    public AuthPropertyTester() {
        super();
    }

    @Override
    public boolean test(Object receiver, String property, Object[] args,
            Object expectedValue) {
        boolean authorized = false;

        /*
         * TODO: try to make this not error of when running against a 17.3.1
         * server
         */

        CheckAuthorizationRequest request = new CheckAuthorizationRequest(
                expectedValue.toString());

        try {
            authorized = (Boolean) ThriftClient.sendRequest(request);
        } catch (VizException e) {
            statusHandler.error("Unable to determine user authorization", e);
        }

        return authorized;
    }

}
