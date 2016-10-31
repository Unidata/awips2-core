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
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.conn.ssl.TrustStrategy;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.Pair;

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
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 15, 2014  3757     dhladky   Initial Implementation.
 * May 08, 2015  4435     dhladky   Updated logging of cert validation.
 * Oct 22. 2015  5031     dhladky   Added some more informative logging when
 *                                  using external CA's.
 * Aug 16, 2016  5659     rjpeter   Fix infinite loop and checking of certs.
 * 
 * </pre>
 * 
 * @author dhladky
 * */

public class LocalTrustStrategy implements TrustStrategy {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(LocalTrustStrategy.class);

    private KeyStore truststore = null;

    /**
     * Keeps track of whether the isTrusted call has been made or not. Setup to
     * prevent log spamming as every HTTPS connections attempted will print the
     * same information needlessly.
     **/
    private static boolean isTrustedChecked = false;

    public LocalTrustStrategy(KeyStore truststore) {
        this.truststore = truststore;
    }

    @Override
    public boolean isTrusted(X509Certificate[] chain, String authType)
            throws CertificateException {
        /**
         * Compare with the loaded truststore certificates first. Even if this
         * returns false, it will still compare with the default java loaded CA
         * certificates for validation.
         */
        boolean isTrust = false;

        /**
         * Automatically direct to compare with default java CA's if no
         * truststore is loaded. Cave will use this path checking CA's like
         * Verisign, Google, Go Daddy, Thawt, DoD, anything we have loaded into
         * the Java CA.
         */
        if (truststore == null) {
            if (!isTrustedChecked) {
                statusHandler
                        .handle(Priority.INFO,
                                "No local Truststore configured.  Redirecting to external Certificate Authority for validation. "
                                        + "Not having a locally configured Truststore is not an error.  The external validation CA's can be found in your JAVA CA configuration. ");
            }

            isTrustedChecked = true;

            return false;
        }

        if (!isTrustedChecked) {
            statusHandler.handle(Priority.INFO,
                    "TrustStore loaded, ready to validate aliases.");
        }

        try {
            // loop over the different aliases for this authority
            Enumeration<String> aliases = truststore.aliases();
            boolean foundPublicKey = false;
            List<Pair<String, Exception>> issueList = new LinkedList<>();

            while (aliases.hasMoreElements() && !isTrust) {
                String caAlias = aliases.nextElement();

                if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                    statusHandler.handle(Priority.DEBUG,
                            "Verifying cert against alias: " + caAlias);
                }

                // Certifying Authority to check with
                X509Certificate caCert = (X509Certificate) truststore
                        .getCertificate(caAlias);

                // If one works we claim victory and return true;
                for (X509Certificate cert : chain) {
                    try {
                        if (caCert.getPublicKey().equals(cert.getPublicKey())) {
                            foundPublicKey = true;
                            caCert.verify(cert.getPublicKey());
                            isTrust = true;
                            if (statusHandler.isPriorityEnabled(Priority.DEBUG)) {
                                statusHandler.handle(Priority.DEBUG,
                                        "Verified! " + caAlias + " Cert Good!");
                            }
                            break;
                        }
                    } catch (Exception e) {
                        /*
                         * Save off any errors in case a later alias validates
                         * the chain
                         */
                        issueList
                                .add(new Pair<>(
                                        "Error verifying incoming certificate. Alias: "
                                                + caAlias + ", CA Cert: ["
                                                + caCert
                                                + "] incoming cert: [" + cert
                                                + "]", e));
                    }
                }
            }

            if (!isTrust) {
                /* certificate invalid, print any previous messages */
                for (Pair<String, Exception> issue : issueList) {
                    statusHandler.error(issue.getFirst(), issue.getSecond());
                }
            }
            if (!foundPublicKey) {
                StringBuilder msg = new StringBuilder(
                        "No Public Key in trust store matched for incoming certificate chain: ");
                for (X509Certificate cert : chain) {
                    msg.append(cert);
                    msg.append(", ");
                }
                msg.delete(msg.length() - 2, msg.length());
                statusHandler.warn(msg.toString());
            }
        } catch (KeyStoreException e) {
            statusHandler.handle(
                    Priority.PROBLEM,
                    "Key(trust) store loaded is invalid, "
                            + truststore.toString(), e);
        }

        isTrustedChecked = true;

        return isTrust;
    }
}
