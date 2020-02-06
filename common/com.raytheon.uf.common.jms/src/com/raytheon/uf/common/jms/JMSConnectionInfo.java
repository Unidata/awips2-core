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
package com.raytheon.uf.common.jms;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Class that encapsulates all information required to make a JMS connection
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 17, 2019 7724       mrichardson Initial creation
 * Oct 16, 2019 7724       tgurney     Move connection URL and SSL logic to
 *                                     the connection factory. Add serialization
 *                                     annotations
 *
 * </pre>
 *
 * @author mrichardson
 */

@DynamicSerialize
@XmlRootElement(name = "jmsConnectionInfo")
@XmlAccessorType(XmlAccessType.NONE)
public class JMSConnectionInfo {

    @DynamicSerializeElement
    @XmlElement(name = "host", required = true)
    private String host;

    @DynamicSerializeElement
    @XmlElement(name = "port", required = true)
    private String port;

    @DynamicSerializeElement
    @XmlElement(name = "vhost", required = true)
    private String vhost;

    @DynamicSerializeElement
    @XmlElement(name = "parameters")
    private Map<String, String> parameters = new HashMap<>();

    /** For serialization only */
    public JMSConnectionInfo() {
    }

    public JMSConnectionInfo(String host, String port, String vhost,
            Map<String, String> parameters) {
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.parameters = parameters;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getVhost() {
        return vhost;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (host == null ? 0 : host.hashCode());
        result = prime * result
                + (parameters == null ? 0 : parameters.hashCode());
        result = prime * result + (port == null ? 0 : port.hashCode());
        result = prime * result + (vhost == null ? 0 : vhost.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        JMSConnectionInfo other = (JMSConnectionInfo) obj;
        return Objects.equals(host, other.host)
                && Objects.equals(port, other.port)
                && Objects.equals(vhost, other.vhost)
                && Objects.equals(parameters, other.parameters);
    }

    @Override
    public String toString() {
        return "JMSConnectionInfo [host=" + host + ", port=" + port + ", vhost="
                + vhost + ", parameters=" + parameters + "]";
    }
}