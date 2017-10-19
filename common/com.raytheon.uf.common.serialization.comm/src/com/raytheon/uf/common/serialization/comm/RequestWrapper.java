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
package com.raytheon.uf.common.serialization.comm;

import java.util.UUID;

import com.raytheon.uf.common.message.WsId;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Wraps an IServerRequest so it can be tracked on both client applications and
 * the server. Contains a unique identifier for this particular request and a
 * workstation ID which has the network address and process id.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2012            njensen     Initial creation
 * Aug 15, 2014 3541       mschenke    Made implement IServerRequest since sent
 *                                     as one 
 * Aug 25, 2017 6316       njensen     Improved toString()
 * 
 * </pre>
 * 
 * @author njensen
 */

@DynamicSerialize
public class RequestWrapper implements IServerRequest {

    @DynamicSerializeElement
    private IServerRequest request;

    @DynamicSerializeElement
    private WsId wsId;

    @DynamicSerializeElement
    private String uniqueId;

    public RequestWrapper() {

    }

    public RequestWrapper(IServerRequest request, WsId workstationId) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (workstationId == null) {
            throw new IllegalArgumentException(
                    "workstationId must not be null");
        }
        this.request = request;
        this.wsId = workstationId;
        this.uniqueId = UUID.randomUUID().toString();
    }

    public IServerRequest getRequest() {
        return request;
    }

    public void setRequest(IServerRequest request) {
        this.request = request;
    }

    public WsId getWsId() {
        return wsId;
    }

    public void setWsId(WsId wsId) {
        this.wsId = wsId;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(125);
        sb.append("Request[");
        if (wsId != null) {
            sb.append(wsId.toPrettyString());
        } else {
            sb.append("null wsId");
        }
        sb.append("][").append(uniqueId).append("] ");
        if (request != null) {
            sb.append(request.getClass().getSimpleName());
        } else {
            sb.append("null request");
        }
        return sb.toString();
    }

}
