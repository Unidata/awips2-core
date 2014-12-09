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
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import org.apache.http.conn.ssl.TrustStrategy;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * AWIPS implementation of TrustStrategy
 * 
 * It predicates that if this returns false, Java will automatically, (According
 * to documentation) then validate using the loaded truststore(KeyStore) this
 * should allow for "self" signed certs used by Data Delivery and such.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 15, 2014 3757      dhladky      Initial Implementation.
 * 
 * 
 * </pre>
 * 
 * @author dhladky
 * @version 1.0
 * */

public class LocalTrustStrategy implements TrustStrategy {
    
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalTrustStrategy.class);

    private KeyStore truststore = null;
    
    public LocalTrustStrategy(KeyStore truststore) {
        this.truststore = truststore;
    }
    
    @Override
    public boolean isTrusted(X509Certificate[] chain,
            String authType) throws CertificateException {
        /**
         * Compare with the loaded truststore certificates first.
         * Even if this returns false, it will still compare with
         * the default java loaded CA certificates for validation.
         */
        boolean isTrust = false;

        /**
         * Automatically direct to compare with default java CA's
         * if no truststore is loaded. Cave will use this path
         * checking CA's like Verisign, Google, Go Daddy, Thawt,
         * etc.
         */
        if (truststore == null) {
            return false;
        }

        try {
            // loop over the different aliases for this authority
            Enumeration<String> aliases = truststore.aliases();

            for (String caAlias = aliases.nextElement(); aliases
                    .hasMoreElements();) {

                // Certifying Authority to check with
                X509Certificate caCert = (X509Certificate) truststore
                        .getCertificate(caAlias);

                // If one works we claim victory and return true;
                for (X509Certificate cert : chain) {

                    try {
                        caCert.verify(cert.getPublicKey());
                        isTrust = true;
                        break;
                        
                    } catch (InvalidKeyException e) {
                        statusHandler
                                .handle(Priority.WARN, "Invalid key: cert: "
                                        + cert.toString() + "\n CA cert: "
                                        + caCert.toString(), e);
                    } catch (NoSuchAlgorithmException e) {
                        statusHandler
                                .handle(Priority.WARN,
                                        "No such algorithim: cert: "
                                                + cert.toString()
                                                + "\n CA cert: "
                                                + caCert.toString(), e);
                    } catch (NoSuchProviderException e) {
                        statusHandler
                                .handle(Priority.WARN,
                                        "No such Provider: cert: "
                                                + cert.toString()
                                                + "\n CA cert: "
                                                + caCert.toString(), e);
                    } catch (SignatureException e) {
                        statusHandler
                                .handle(Priority.WARN,
                                        "Signature Failure: cert: "
                                                + cert.toString()
                                                + "\n CA cert: "
                                                + caCert.toString(), e);
                    }
                }

                if (isTrust) {
                    break;
                }
            }

        } catch (KeyStoreException e) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Key(trust) store loaded is invalid, "
                            + truststore.toString(), e);
        }

        return isTrust;
    }
}
