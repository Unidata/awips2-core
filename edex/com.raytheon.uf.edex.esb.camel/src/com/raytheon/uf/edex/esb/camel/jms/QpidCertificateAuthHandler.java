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
package com.raytheon.uf.edex.esb.camel.jms;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import com.raytheon.uf.common.comm.HttpAuthHandler;

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
 *
 * </pre>
 *
 * @author bsteffen
 */
public class QpidCertificateAuthHandler implements HttpAuthHandler {

    private static final String CERTIFICATE_DIR = "QPID_SSL_CERT_DB";

    private static final String CERTIFICATE_NAME = "QPID_SSL_CERT_NAME";

    private final KeyStore keyStore;

    private final KeyStore trustStore;

    public QpidCertificateAuthHandler() throws JMSConfigurationException {
        Path certDB = null;
        String certDir = System.getenv(CERTIFICATE_DIR);
        if (certDir == null) {
            String userHome = System.getProperty("user.home");
            if (userHome != null) {
                certDB = Paths.get(userHome).resolve(".qpid");
                if (!Files.isDirectory(certDB)) {
                    certDB = null;
                }
            }
            if (certDB == null) {
                throw new JMSConfigurationException(
                        "Cannot load KeyStore. Need to set " + CERTIFICATE_DIR);
            }
        } else {
            certDB = Paths.get(certDir);
        }
        String certName = System.getenv(CERTIFICATE_NAME);
        if (certName == null) {
            certName = "guest";
        }
        keyStore = loadKeyStore(certDB, certName);
        trustStore = loadTrustStore(certDB);

    }

    private static KeyStore loadKeyStore(Path certDB, String certName)
            throws JMSConfigurationException {
        Path keyPath = certDB.resolve(certName + ".key");
        Path crtPath = certDB.resolve(certName + ".crt");

        try (InputStream keyStream = Files.newInputStream(keyPath);
                InputStream crtStream = Files.newInputStream(crtPath)) {
            PrivateKey privateKey = readPrivateKey(keyStream);
            X509Certificate[] certs = readCertificates(crtStream);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            keyStore.setKeyEntry(certName, privateKey, new char[0], certs);
            return keyStore;
        } catch (GeneralSecurityException | IOException e) {
            throw new JMSConfigurationException("Error loading KeyStore", e);
        }
    }

    private static KeyStore loadTrustStore(Path certDB)
            throws JMSConfigurationException {
        try (InputStream crtStream = Files
                .newInputStream(certDB.resolve("root.crt"))) {
            X509Certificate[] certs = readCertificates(crtStream);
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            int alias = 1;
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry(Integer.toString(alias), cert);
                alias += 1;
            }
            return keyStore;
        } catch (GeneralSecurityException | IOException e) {
            throw new JMSConfigurationException("Error loading KeyStore", e);
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
        return new char[0];
    }

    @Override
    public boolean isValidateCertificates() {
        return true;
    }

    /**
     * TODO Upgrade to a newer version of qpid and use
     * org.apache.qpid.transport.network.security.ssl.SSLUtil.readCertificates
     */
    private static X509Certificate[] readCertificates(InputStream input)
            throws IOException, GeneralSecurityException {
        List<X509Certificate> crt = new ArrayList<>();
        try {
            do {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                crt.add((X509Certificate) cf.generateCertificate(input));
            } while (input.available() != 0);
        } catch (CertificateException e) {
            if (crt.isEmpty()) {
                throw e;
            }
        }
        return crt.toArray(new X509Certificate[crt.size()]);
    }

    /**
     * TODO Upgrade to a newer version of qpid and use
     * org.apache.qpid.transport.network.security.ssl.SSLUtil.readPrivateKey
     */
    private static PrivateKey readPrivateKey(InputStream input)
            throws IOException, GeneralSecurityException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        byte[] tmp = new byte[1024];
        int read;
        while ((read = input.read(tmp)) != -1) {
            buffer.write(tmp, 0, read);
        }

        byte[] content = buffer.toByteArray();
        String contentAsString = new String(content, StandardCharsets.US_ASCII);
        if (contentAsString.contains("-----BEGIN ")
                && contentAsString.contains(" PRIVATE KEY-----")) {
            BufferedReader lineReader = new BufferedReader(
                    new StringReader(contentAsString));

            String line;
            do {
                line = lineReader.readLine();
            } while (line != null && !(line.startsWith("-----BEGIN ")
                    && line.endsWith(" PRIVATE KEY-----")));

            if (line != null) {
                StringBuilder keyBuilder = new StringBuilder();

                while ((line = lineReader.readLine()) != null) {
                    if (line.startsWith("-----END ")
                            && line.endsWith(" PRIVATE KEY-----")) {
                        break;
                    }
                    keyBuilder.append(line);
                }

                content = DatatypeConverter
                        .parseBase64Binary(keyBuilder.toString());
            }
        }
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(content);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(keySpec);
    }

}
