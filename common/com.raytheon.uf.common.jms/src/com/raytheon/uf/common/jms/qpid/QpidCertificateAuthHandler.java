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
package com.raytheon.uf.common.jms.qpid;

import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import com.raytheon.uf.common.comm.HttpAuthHandler;
import com.raytheon.uf.common.jms.JmsSslConfiguration;

/**
 * 
 * Loads a key store and a trust store to be used when making ssl rest requests
 * to qpid.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jan 31, 2017  6083     bsteffen    Initial creation
 * Feb 02, 2017  6085     bsteffen    Extract certificate lookup to JmsSslConfiguration
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 *
 * </pre>
 *
 * @author bsteffen
 */
public class QpidCertificateAuthHandler implements HttpAuthHandler {

    private final KeyStore keyStore;

    private final KeyStore trustStore;

    private final char[] password;

    public QpidCertificateAuthHandler() throws JMSConfigurationException {
        JmsSslConfiguration sslConfig = new JmsSslConfiguration("guest");
        try {
            keyStore = sslConfig.loadKeyStore();
            trustStore = sslConfig.loadTrustStore();
            password = sslConfig.getPassword().toCharArray();
        } catch (GeneralSecurityException | IOException e) {
            throw new JMSConfigurationException(
                    "Failed to load ssl certificates.", e);
        }
    }

    @Override
    public String[] getCredentials(URI uri, String authValue) {
        throw new UnsupportedOperationException(
                "Only certificate authentication is supported");
    }

    @Override
    public void credentialsFailed() {
        throw new UnsupportedOperationException(
                "Only certificate authentication is supported");
    }

    @Override
    public KeyStore getTruststore() {
        return trustStore;
    }

    @Override
    public KeyStore getKeystore() {
        return keyStore;
    }

    @Override
    public char[] getKeystorePassword() {
        return password;
    }

    @Override
    public boolean isValidateCertificates() {
        return true;
    }

}
