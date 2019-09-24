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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.utils.URIBuilder;

/**
 * Class to manage JMS connection information.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 17, 2019 7724       mrichardson Initial creation
 * 
 * </pre>
 * 
 * @author mrichardson
 */

public class JMSConnectionInfo {

    private String hostname;
    private String port;
    private String vhost;
    private Map<String, String> optionalParameters = null;
    private static JMSConnectionInfo instance;
    private static final String BROKER_HOST = "BROKER_HOST";
    private static final String BROKER_PORT = "BROKER_PORT";
    private static final String JMS_VIRTUALHOST = "JMS_VIRTUALHOST";
    private static final String QPID_SSL_CERT_DB = "QPID_SSL_CERT_DB";
    private static final String TRUST_KEYSTORE_PASSWORD = "password";

    private JMSConnectionInfo() {
    }

    public static synchronized JMSConnectionInfo getInstance() {
        if (instance == null) {
            instance = new JMSConnectionInfo();
        }
        
        return instance;
    }

    public void setConnectionInfo(Map<String,String> connectionInfo) {
        String hostname = connectionInfo.remove("hostname");
        if (hostname != null) {
            setHostname(hostname);
        }
        String port = connectionInfo.remove("port");
        if (port != null) {
            setPort(port);
        }
        String vhost = connectionInfo.remove("vhost");
        if (vhost != null) {
            setVhost(vhost);
        }
        if (!connectionInfo.isEmpty()) {
            setOptionalParameters(connectionInfo);
        }
    }
    
    public Map<String,String> getConnectionInfo() {
        Map<String,String> connectionInfo = new HashMap<>();
        connectionInfo.put("hostname", getHostname());
        connectionInfo.put("port", getPort());
        connectionInfo.put("vhost", getVhost());
        connectionInfo.putAll(optionalParameters);
        return connectionInfo;
    }

    public String configureURL(Map<String, String> connectionInfo) {
        getInstance().setConnectionInfo(connectionInfo);
        return configureURL();
    }

    public String configureURL() {
        URIBuilder uriBuilder = new URIBuilder();
        
        if (hostname == null || hostname.isEmpty()) {
            hostname = System.getenv(BROKER_HOST);
            // really, this shouldn't ever happen; however, for
            //  the sake of consistency, let's default to localhost
            if ((hostname == null) || hostname.isEmpty()) {
                hostname = "localhost";
            }
        }
        
        if (port == null || port.isEmpty()) {
            port = System.getenv(BROKER_PORT);
            if (port == null || port.isEmpty()) {
                port = "5672";
            }
        }
        
        if (vhost == null || vhost.isEmpty()) {
            vhost = System.getenv(JMS_VIRTUALHOST);
            if (vhost == null || vhost.isEmpty()) {
                vhost = "edex";
            }
        }
        
        uriBuilder.setScheme("amqps");
        uriBuilder.setHost(hostname);
        uriBuilder.setPort(Integer.parseInt(port));
        uriBuilder.addParameter("amqp.vhost", vhost);
        uriBuilder.addParameter("jms.username", "guest");
        for (Entry<String, String> parameter : optionalParameters.entrySet()) {
            uriBuilder.addParameter(parameter.getKey(), parameter.getValue());
        }
        uriBuilder = configureSSL(uriBuilder);
        
        return uriBuilder.toString();
    }

    public URIBuilder configureSSL(URIBuilder uriBuilder) {
        String qpidSslCertDb = System.getenv(QPID_SSL_CERT_DB);
        Path certsPath = null;
        
        if (qpidSslCertDb == null) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                certsPath = Paths.get(userHome).resolve(".qpid");
                if (!Files.isDirectory(certsPath)) {
                    certsPath = null;
                }
            }
            if (certsPath == null) {
                throw new IllegalStateException(
                        "Unable to load ssl certificates for jms ssl. "
                        + "Consider setting the environmental variable: "
                        + QPID_SSL_CERT_DB);
            }
        } else {
            certsPath = Paths.get(qpidSslCertDb);
        }
        
        uriBuilder.addParameter("transport.trustStoreLocation",
                certsPath.resolve("guest.jks").toString());
        uriBuilder.addParameter("transport.trustStorePassword",
                TRUST_KEYSTORE_PASSWORD);
        uriBuilder.addParameter("transport.keyStoreLocation",
                certsPath.resolve("root.jks").toString());
        uriBuilder.addParameter("transport.keyStorePassword",
                TRUST_KEYSTORE_PASSWORD);
        
        return uriBuilder;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getPort() {
        return port;
    }

    public void setVhost(String vhost) {
        this.vhost = vhost;
    }

    public String getVhost() {
        return vhost;
    }
    
    public void setOptionalParameters(Map<String, String> optionalParameters) {
        this.optionalParameters = optionalParameters;
    }

}