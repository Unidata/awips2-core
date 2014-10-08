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
package com.raytheon.uf.common.auth.user;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Implementation of IUser
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 26, 2010            rgeorge     Initial creation
 * Oct 06, 2014 3398       bclement    moved to common.auth
 * 
 * </pre>
 * 
 * @author rgeorge
 * @version 1.0
 */
@DynamicSerialize
public class User implements IUser {

    @DynamicSerializeElement
    private IUserId userId;

    @DynamicSerializeElement
    private IAuthenticationData authenticationData;

    public User() {

    }

    public User(String userId) {
        setUserId(new UserId(userId));
    }

    public IUserId getUserId() {
        return userId;
    }

    public void setUserId(IUserId userId) {
        this.userId = userId;
    }

    public IAuthenticationData getAuthenticationData() {
        return authenticationData;
    }

    public void setAuthenticationData(IAuthenticationData authenticationData) {
        this.authenticationData = authenticationData;
    }

    @Override
    public IUserId uniqueId() {
        return userId;
    }

    @Override
    public IAuthenticationData authenticationData() {
        return this.authenticationData;
    }

    @Override
    public String toString() {
        return this.getClass().getName() + "[userId = " + userId.toString()
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((authenticationData == null) ? 0 : authenticationData
                        .hashCode());
        result = prime * result + ((userId == null) ? 0 : userId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        User other = (User) obj;
        if (authenticationData == null) {
            if (other.authenticationData != null)
                return false;
        } else if (!authenticationData.equals(other.authenticationData))
            return false;
        if (userId == null) {
            if (other.userId != null)
                return false;
        } else if (!userId.equals(other.userId))
            return false;
        return true;
    }
}
