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
package com.raytheon.uf.common.comm;

import java.security.KeyStore;

/**
 * Interface for collecting https Credentials.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 04, 2013    1786     mpduff      Initial creation
 * Feb 10, 2014    2704     njensen     Added credentialsFailed()
 * Sep 3, 2014     3570     bclement    added host and port to getCredentials()
 * Nov 15, 2014    3757     dhladky     Consolidated the Credentials handler to be a general HTTPS handler.
 * May 10, 2015    4435     dhladky     KeyStore loading needed for presentation of keys to SSL servers.
 * 
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public interface IHttpsHandler {

    /**
     * Get the https credentials.
     * 
     * @param host
     * @param port
     * @param authValue
     *            The authorization message, typically returned from the server
     *            requesting authentication.
     * 
     * @return String Array, username and password
     */
    String[] getCredentials(String host, int port, String authValue);
        
    /**
     * Credential Validation has failed
     */
    void credentialsFailed();
    
    /**
     * Get the trustore used to validate certificates
     * @return
     */
    KeyStore getTruststore();
    
    /**
     * Get the keystore used to submit certificates
     * @return
     */
    KeyStore getKeystore();
    
    /**
     * Get the un-encrypted keystore password.
     * @return
     */
    char[] getKeystorePassword();
    
    /**
     * Whether or not you care to validate certificates
     */
    boolean isValidateCertificates();
}
