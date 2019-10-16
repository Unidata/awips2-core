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
package com.raytheon.uf.common.localization.msgs;

import java.util.Map;

import com.raytheon.uf.common.jms.JMSConnectionInfo;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * The response GetServersHandler returns. Sets the jms and http server based on
 * the environment.xml
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 6, 2009            mschenke     Initial creation
 * Sep 12, 2012 1167      djohnson     Add datadelivery servers.
 * Jan 14, 2013 1469      bkowal       Removed the hdf5 data directory
 * Aug 27, 2013 2995      bkowal       Removed jms server; added jms connection string
 * Oct 16, 2019 7724      tgurney      Replace connection string and info map
 *                                     with a single {@link JMSConnectionInfo}
 *                                     object
 *
 * </pre>
 *
 * @author mschenke
 */
@DynamicSerialize
public class GetServersResponse {

    @DynamicSerializeElement
    private String httpServer;

    @DynamicSerializeElement
    private String pypiesServer;

    @DynamicSerializeElement
    private Map<String, String> serverLocations;

    @DynamicSerializeElement
    private JMSConnectionInfo jmsConnectionInfo;

    public JMSConnectionInfo getJmsConnectionInfo() {
        return jmsConnectionInfo;
    }

    public void setJmsConnectionInfo(JMSConnectionInfo connectionInfo) {
        this.jmsConnectionInfo = connectionInfo;
    }

    public String getHttpServer() {
        return httpServer;
    }

    public void setHttpServer(String httpServer) {
        this.httpServer = httpServer;
    }

    public String getPypiesServer() {
        return pypiesServer;
    }

    public void setPypiesServer(String pypiesServer) {
        this.pypiesServer = pypiesServer;
    }

    public Map<String, String> getServerLocations() {
        return serverLocations;
    }

    public void setServerLocations(Map<String, String> serverLocations) {
        this.serverLocations = serverLocations;
    }
}
