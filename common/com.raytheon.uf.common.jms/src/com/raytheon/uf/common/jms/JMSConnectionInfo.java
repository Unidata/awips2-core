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
 * May 27, 2021 8469       dgilling    Add servicePort field.
 * Aug 16, 2022 8916       njensen     Added copy constructor
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
    @XmlElement(name = "servicePort", required = true)
    private String servicePort;

    @DynamicSerializeElement
    @XmlElement(name = "parameters")
    private Map<String, String> parameters = new HashMap<>();

    /** For serialization only */
    public JMSConnectionInfo() {
    }

    public JMSConnectionInfo(String host, String port, String vhost,
            String servicePort, Map<String, String> parameters) {
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.servicePort = servicePort;
        this.parameters = parameters;
    }

    public JMSConnectionInfo(JMSConnectionInfo toCopy) {
        this.host = toCopy.host;
        this.port = toCopy.port;
        this.vhost = toCopy.vhost;
        this.servicePort = toCopy.servicePort;
        this.parameters = new HashMap<>(toCopy.parameters);
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

    public String getServicePort() {
        return servicePort;
    }

    public void setServicePort(String servicePort) {
        this.servicePort = servicePort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(host, parameters, port, servicePort, vhost);
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
                && Objects.equals(parameters, other.parameters)
                && Objects.equals(port, other.port)
                && Objects.equals(servicePort, other.servicePort)
                && Objects.equals(vhost, other.vhost);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("JMSConnectionInfo [host=").append(host)
                .append(", port=").append(port).append(", vhost=").append(vhost)
                .append(", servicePort=").append(servicePort)
                .append(", parameters=").append(parameters).append("]");
        return builder.toString();
    }
}