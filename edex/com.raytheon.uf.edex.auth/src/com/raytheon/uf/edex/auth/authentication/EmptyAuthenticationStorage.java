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
package com.raytheon.uf.edex.auth.authentication;

import com.raytheon.uf.common.auth.user.AuthenticationData;
import com.raytheon.uf.common.auth.user.IAuthenticationData;
import com.raytheon.uf.common.auth.user.IUser;

/**
 * Default authentication storage that does not do anything, the data returned
 * has no value.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 21, 2010            mschenke     Initial creation
 * May 28, 2014 3211       njensen      Renamed and moved to uf.edex.auth
 * 
 * </pre>
 * 
 */
public class EmptyAuthenticationStorage implements IAuthenticationStorage {

    @Override
    public IAuthenticationData getAuthenticationDataForUser(IUser user) {
        return new AuthenticationData();
    }

}
