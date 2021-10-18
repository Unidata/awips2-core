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
package com.raytheon.uf.common.comm.ssl;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 *
 * Abstract class for configuring the credentials for an SSL connection.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 28, 2021 8667       mapeters    Initial creation (mainly extracted from
 *                                     JmsSslConfiguration)
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractSslConfiguration {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final String clientName;

    private final Path clientCert;

    private final Path clientKey;

    private final Path rootCert;

    private final String keystoreType;

    private final String keystoreExt;

    protected AbstractSslConfiguration(String defaultClientName, String clientNameVar,
            Path certDir, String keystoreType, String keystoreExt) {
        String certName = System.getenv(clientNameVar);
        if (certName == null) {
            certName = defaultClientName;
        }

        clientName = certName;
        clientCert = certDir.resolve(certName + ".crt");
        clientKey = certDir.resolve(certName + ".key");
        rootCert = certDir.resolve("root.crt");
        this.keystoreType = keystoreType;
        this.keystoreExt = keystoreExt;

    }

    public String getClientName() {
        return clientName;
    }

    public Path getClientCert() {
        return clientCert;
    }

    public Path getClientKey() {
        return clientKey;
    }

    public Path getRootCert() {
        return rootCert;
    }

    /**
     * Load the key store.
     *
     * @return the key store
     * @throws Exception
     */
    public KeyStore loadKeyStore() throws Exception {
        try (InputStream keyStream = Files.newInputStream(getClientKey());
                InputStream crtStream = Files.newInputStream(getClientCert())) {
            PrivateKey privateKey = readPrivateKey(keyStream);
            X509Certificate[] certs = readCertificates(crtStream);
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(null, null);
            keyStore.setKeyEntry(getClientName(), privateKey,
                    getKeyStorePassword().toCharArray(), certs);
            return keyStore;
        }
    }

    /**
     * Load the trust store.
     *
     * @return the trust store
     * @throws Exception
     */
    public KeyStore loadTrustStore() throws Exception {
        try (InputStream crtStream = Files.newInputStream(getRootCert())) {
            X509Certificate[] certs = readCertificates(crtStream);
            KeyStore keyStore = KeyStore.getInstance(keystoreType);
            keyStore.load(null, null);
            int alias = 1;
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry(Integer.toString(alias), cert);
                alias += 1;
            }
            return keyStore;
        }
    }

    /**
     * @return the trust store file as a {@link Path}
     */
    public Path getJavaTrustStoreFile() {
        Path path = clientKey.resolveSibling(clientName + keystoreExt);
        boolean createStore = !Files.exists(path);

        if (!createStore) {
            // double check file times
            try {
                long storeTime = Files.getLastModifiedTime(path).toMillis();
                long certTime = Files.getLastModifiedTime(getRootCert())
                        .toMillis();
                createStore = certTime >= storeTime;
            } catch (IOException e) {
                statusHandler.error(
                        "Failed to check modified times for trust store.", e);
            }

        }

        if (createStore) {
            try {
                KeyStore trustStore = loadTrustStore();
                try (OutputStream out = Files.newOutputStream(path)) {
                    trustStore.store(out,
                            getTrustStorePassword().toCharArray());
                }
            } catch (Exception e) {
                statusHandler.error("Failed to create java trust store file.",
                        e);
            }
        }
        return path;
    }

    /**
     * @return the key store file as a {@link Path}
     */
    public Path getJavaKeyStoreFile() {
        Path path = rootCert.resolveSibling("root" + keystoreExt);
        boolean createStore = !Files.exists(path);

        if (!createStore) {
            // double check file times
            try {
                long storeTime = Files.getLastModifiedTime(path).toMillis();
                long certTime = Files.getLastModifiedTime(getClientCert())
                        .toMillis();
                long keyTime = Files.getLastModifiedTime(getClientKey())
                        .toMillis();
                createStore = certTime >= storeTime || keyTime >= storeTime;
            } catch (IOException e) {
                statusHandler.error(
                        "Failed to check modified times for trust store.", e);
            }

        }

        if (createStore) {
            try {
                KeyStore keyStore = loadKeyStore();
                try (OutputStream out = Files.newOutputStream(path)) {
                    keyStore.store(out, getKeyStorePassword().toCharArray());
                }
            } catch (Exception e) {
                statusHandler.error("Failed to create java key store file.", e);
            }
        }
        return path;
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

    /**
     * @return the key store password
     * @throws Exception
     */
    public abstract String getKeyStorePassword() throws Exception;

    /**
     * @return the trust store password
     * @throws Exception
     */
    public abstract String getTrustStorePassword() throws Exception;
}
