package com.raytheon.uf.edex.esb.camel.ftp;

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


import java.util.Map.Entry;

import org.apache.camel.builder.RouteBuilder;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * Utility class for creating FTP routes in a camel context.
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * July 29, 2014 3404       dhladky     FTP library initial release.
* 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 */

public class FTPRouteBuilder extends RouteBuilder {
    
    private static final String pathSeparator = "/";
    
    private static final String colon = ":";
    
    private static final String at = "@";
    
    private static final String question = "?";
    
    private static final String and = "&";
    
    private static final String file = "file";
    
    private static final String equals = "=";
    
    /** working directory path **/
    private static String workDirectoryPath = System.getProperty("ftp.workDirectoryPath");
    
    /** request object **/
    private FTPRequest request;    
    
    /** output file for transfer **/
    private String outputFile;
    
    /** URI location for FTP **/
    private String uri;
    
    /** local destination **/
    private String destinationURI;
    
    /**  keeps hostanem prepend for removal around **/
    private String hostnamePrepend = null;
       
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FTPRouteBuilder.class);
    
    /**
     * Utilize this constructor
     * @param request
     */
    public FTPRouteBuilder(FTPRequest request) {
        this.request = request;
    }

    /**
     * Creates the URI for the FTP through camel
     * 
     * ftp://[username@]hostname[:port]/remoteDirectoryPath[?destinationDirectoryPath&fileName&workingDirectory]
     * sftp://[username@]hostname[:port]/remoteDirectoryPath[?destinationDirectoryPath&fileName&workingDirectory]
     * ftps://[username@]hostname[:port]/remoteDirectoryPath[?destinationDirectoryPath&fileName&workingDirectory]
     *
     * Create the FTP connection URI for camel to reach out too.
     *  @param FTPRequest request
     *  @return String
     */
    private String createURI() {
        
        StringBuilder buf = new StringBuilder();
        
        if (request.getType() != null) {

            buf.append(request.getType().name().toLowerCase());
            buf.append(colon).append(pathSeparator).append(pathSeparator);
            hostnamePrepend = buf.toString();
            
            if (request.getUsername() != null) {
                buf.append(request.getUsername());
                buf.append(at);
                
                if (request.getHostname() != null) {
                    
                    String hostname = null;
                    if (request.getHostname().startsWith(hostnamePrepend)) {
                        hostname = request.getHostname().replace(hostnamePrepend, "");
                    } else {
                        hostname = request.getHostname();
                    }
                    
                    buf.append(hostname);
                    buf.append(colon);
                    
                    if (request.getPort() != null) {
                        buf.append(request.getPort());
                        
                        if (request.getRemoteDirectoryPath() != null) {

                            buf.append(request.getRemoteDirectoryPath());
                            buf.append(question);
                            
                            // now do the options switches
                            
                            if (request.getPassword() != null) {
                                // check where we are
                                if (!buf.toString().endsWith(question)) {
                                    buf.append(and);
                                }
                                
                                buf.append("password=");
                                buf.append(request.getPassword());
                            }
                          
                            // check where we are
                            if (!buf.toString().endsWith(question)) {
                                buf.append(and);
                            }
                            // always use this as it make the transfer more efficient
                            buf.append("localWorkDirectory=");
                            buf.append(workDirectoryPath);

                            if (request.getFileName() != null) {

                                buf.append(and);
                                buf.append("fileName=");
                                buf.append(request.getFileName());
                            } else {
                                throw new IllegalArgumentException(
                                        "Need to set a {fileName} to download!");
                            }
                            
                            // Process any additional parameters
                            if (request.getAdditionalParameters() != null) {
                                // add them one at a time
                                for (Entry<String, String> entry : request.getAdditionalParameters().entrySet()) {
                                    buf.append(and);
                                    buf.append(entry.getKey()).append(equals);
                                    buf.append(entry.getValue());

                                }

                            }
                        } else {
                            throw new IllegalArgumentException(
                                    "Need to set a remote directory {path}!");
                        }
                    } else {
                        throw new IllegalArgumentException("Need to set a {port}!");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "Need to set a {hostname}!");
                }
            } else {
                throw new IllegalArgumentException("Need to set a {username}!");
            }
        } else {
            throw new IllegalArgumentException(
                    "Need to choose a {type} of FTP for transfer!");
        }
        
        return buf.toString();
    }
    
    /**
     * Create the local file location
     * @param destinationFilePath
     * @return
     */
    private String createDestinationURI() {
        
        StringBuilder buf = new StringBuilder();
        buf.append(file);
        buf.append(colon).append(pathSeparator).append(pathSeparator);
        
        if (request.getDestinationDirectoryPath() != null) {

            buf.append(request.getDestinationDirectoryPath());
        
            if (request.getFileName() != null) {
                // check where we are
                if (!buf.toString().endsWith(pathSeparator)) {
                    buf.append(pathSeparator);
                }
                // fileName
                buf.append(request.getFileName());
            } else {
                throw new IllegalArgumentException("Need to set a destination {fileName}!");
            }
        } else {
            throw new IllegalArgumentException("Need to set a destination directory!");
        }
        
        return buf.toString();
    }
   
    @Override
    public void configure() throws Exception {
        
        try {
            uri = createURI();
            destinationURI = createDestinationURI();
        } catch (Exception e) {
            statusHandler.error("Couldn't configure URI's for route!", e);
        }
        
        // Make sure both are created
        if (uri != null && destinationURI != null) {
            // configure the route
            from(uri).to(destinationURI);
            setOutputFile();
        }
    }


    /**
     * getter for FTP request object
     * @return
     */
    public FTPRequest getRequest() {
        return request;
    }


    /**
     * setter for FTP request object
     * @param request
     */
    public void setRequest(FTPRequest request) {
        this.request = request;
    }

    /**
     * File output by this FTP session
     * @return
     */
    public String getOutputFile() {
        return outputFile;
    }

    public void setOutputFile() {

        StringBuilder sb = new StringBuilder();
        sb.append(getRequest().getDestinationDirectoryPath());
        sb.append(pathSeparator);
        sb.append(getRequest().getFileName());
        this.outputFile = sb.toString();

    }
    
    public String getDestinationUri() {
        return destinationURI;
    }
    
    public String getUri() {
        return uri;
    }
    
    
    
}
