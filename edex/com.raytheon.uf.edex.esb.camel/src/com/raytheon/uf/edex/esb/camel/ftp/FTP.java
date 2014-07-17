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
package com.raytheon.uf.edex.esb.camel.ftp;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * FTP a file FROM a designated FTPRequest
 * 
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

public class FTP {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(FTP.class);
    
    private static int timeout = new Integer(System.getProperty("ftp.timeout")).intValue();
    
    public FTPRequest request = null;

    public FTP(FTPRequest request) {
        this.request = request;
    }

    /**
     * Execute a download
     * @throws Exception
     */
    public String executeConsumer() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();
        // performance enhancement
        camelContext.setAllowUseOriginalMessage(false);
        String outputFile = null;
        FTPRouteBuilder route = null;
        
        try {
            route = new FTPRouteBuilder(getRequest());
            camelContext.addRoutes(route);
            camelContext.start();
            // do the transaction
            statusHandler.info("Attempting download of file: "+getRequest().getFileName());
            camelContext.createConsumerTemplate().receive(route.getDestinationUri(), timeout);
            outputFile = route.getOutputFile();

        } catch (Exception e) {
            if (route != null) {
                statusHandler.error("Failed to download file: "+route.getDestinationUri(), e);
            } else {
                statusHandler.error("Failed to download file.", e);
            }
            
        } finally {
            camelContext.stop();
        }
        
        return outputFile;
    }
    
    /**
     * Execute an upload
     * @throws Exception
     */
    public String executeProducer() throws Exception {

        CamelContext camelContext = new DefaultCamelContext();
        // performance enhancement
        camelContext.setAllowUseOriginalMessage(false);
        String destinationFile = null;
        FTPRouteBuilder route = null;
        
        try {
            route = new FTPRouteBuilder(getRequest());
            camelContext.addRoutes(route);
            statusHandler.info("Attempting upload of file: "+getRequest().getFileName());
            camelContext.start();
            // do the transaction
            camelContext.createProducerTemplate().sendBody(route.getDestinationUri(), getRequest().getFileName());
            destinationFile = getRequest().getFileName();

        } catch (Exception e) {
            if (route != null) {
                statusHandler.error("Failed to upload file: "+route.getDestinationUri(), e);
            } else {
                statusHandler.error("Failed to upload file.", e);
            }
            
        } finally {
            camelContext.stop();
        }
        
        return destinationFile;
    }

    public FTPRequest getRequest() {
        return request;
    }

}