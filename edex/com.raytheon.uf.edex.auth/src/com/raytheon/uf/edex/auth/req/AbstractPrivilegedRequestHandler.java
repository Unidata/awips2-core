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

import com.raytheon.uf.common.auth.exception.AuthorizationException;
import com.raytheon.uf.common.auth.req.AbstractPrivilegedRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.auth.resp.AuthorizationResponse;

/**
 * Handler class for privileged requests
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 18, 2010           mschenke  Initial creation
 * Jul 18, 2017  6217     randerso  Removed user argument from authorized method
 *
 * </pre>
 *
 * @author mschenke
 */

public abstract class AbstractPrivilegedRequestHandler<T extends AbstractPrivilegedRequest>
        implements IRequestHandler<T> {

    /**
     * This function should look at the contents of the request to determine if
     * the user is authorized to execute the request.
     *
     * @param request
     * @return the AuthorizationResponse
     * @throws AuthorizationException
     */
    public abstract AuthorizationResponse authorized(T request)
            throws AuthorizationException;

}
