package com.raytheon.uf.common.comm;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

/**
 * 
 * Provides connectivity to HTTPS utils
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date          Ticket#     Engineer    Description
 *    ------------  ----------  ----------- --------------------------
 *    11/15/14        #3757       dhladky    Initial Creation.
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1
 */
public class HttpsUtils {

    /**
     * Load a store for certificate comparisons
     * 
     * @param filePath
     * @param storetype
     * @param trustStorePassword
     * @return
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeystore(String filePath, String storeType,
            String keyStorePassword) throws KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException {
        
        KeyStore keyStore = null;
        // load the keystore
        try (InputStream keyStoreInput = new FileInputStream(filePath)) {
            
            keyStore = KeyStore.getInstance(storeType);
            keyStore.load(keyStoreInput, keyStorePassword.toCharArray());
        } 
        
        return keyStore;
    }
}
