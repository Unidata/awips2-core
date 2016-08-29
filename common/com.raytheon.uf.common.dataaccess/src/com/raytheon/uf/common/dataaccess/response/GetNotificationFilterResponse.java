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
package com.raytheon.uf.common.dataaccess.response;

import com.raytheon.uf.common.dataaccess.INotificationFilter;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Response for <code>GetNotificationFilterRequest</code>.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 22, 2016 2416       tgurney     Initial creation
 * 
 * </pre>
 * 
 * @author tgurney
 */

@DynamicSerialize
public class GetNotificationFilterResponse {

    @DynamicSerializeElement
    private INotificationFilter notificationFilter;

    @DynamicSerializeElement
    private String jmsConnectionInfo;

    public GetNotificationFilterResponse() {
        // no-op, for serialization only
    }

    public void setNotificationFilter(INotificationFilter notificationFilter) {
        this.notificationFilter = notificationFilter;
    }

    public INotificationFilter getNotificationFilter() {
        return notificationFilter;
    }

    public void setJmsConnectionInfo(String jmsConnectionInfo) {
        this.jmsConnectionInfo = jmsConnectionInfo;
    }

    public String getJmsConnectionInfo() {
        return jmsConnectionInfo;
    }

}
