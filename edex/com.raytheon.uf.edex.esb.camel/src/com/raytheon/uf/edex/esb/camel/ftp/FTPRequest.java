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
 * 
 */

package com.raytheon.uf.edex.esb.camel.ftp;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlEnum;

/**
 * FTP request object for wrapping FTP parameters.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 29, 2014 3404       dhladky     FTPUtil initial release.
* 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */
public class FTPRequest {
    
    private String username;
    
    private String password;
    
    private String hostname;
    
    private String securityProtocol;
    
    private String destinationDirectoryPath;
    
    private String remoteDirectoryPath;
    
    private String fileName;
    
    private FTPType type;
    
    private String port;
    
    private Map<String, String> additionalParameters;
    
    @XmlEnum
    public enum FTPType {
        FTP, SFTP, FTPS;
    }
    
    /**
     * Default public constructor
     * @param type
     */
    public FTPRequest(FTPType type) {
        this.type = type;
    }
    
    /**
     * Functional public constructor
     * @param type
     * @param hostname
     * @param username
     * @param password
     * @param port
     */
    public FTPRequest(FTPType type, String hostname, String username, String password, String port) {
        this.type = type;
        this.username = username;
        this.hostname = hostname;
        this.password = password;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getSecurityProtocol() {
        return securityProtocol;
    }

    public void setSecurityProtocol(String securityProtocol) {
        this.securityProtocol = securityProtocol;
    }

    public String getDestinationDirectoryPath() {
        return destinationDirectoryPath;
    }

    public void setDestinationDirectoryPath(String destinationDirectoryPath) {
        this.destinationDirectoryPath = destinationDirectoryPath;
    }

    public String getRemoteDirectoryPath() {
        return remoteDirectoryPath;
    }

    public void setRemoteDirectoryPath(String remoteDirectoryPath) {
        this.remoteDirectoryPath = remoteDirectoryPath;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public FTPType getType() {
        return type;
    }

    public void setType(FTPType type) {
        this.type = type;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    /**
     * Set a list of additional parameters for FTP
     * @param additionalParameters
     */
    public void setAdditionalParameters(Map<String, String> additionalParameters) {
        this.additionalParameters = additionalParameters;
    }
    
    /**
     * Get the map of additional FTP parameters
     * @return
     */
    public Map<String, String> getAdditionalParameters() {
        return additionalParameters;
    }
    
    /**
     * Request an additional parameter
     * @param param
     * @return
     */
    public String getAdditionalParamValue(String param) {
        // returns null if not in list
        return getAdditionalParameters().get(param);
    }
    
    /**
     * Add an additional FTP parameter
     * @param param
     * @param value
     */
    public void addAdditionalParameter(String param, String value) {
        if (getAdditionalParameters() == null) {
            additionalParameters = new HashMap<String, String>(5);
        }
        
        additionalParameters.put(param, value);
    }
}
