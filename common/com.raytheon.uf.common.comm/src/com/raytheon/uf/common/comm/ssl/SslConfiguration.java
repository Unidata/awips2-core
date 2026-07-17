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
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.xml.bind.DatatypeConverter;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 *
 * Configures the credentials for an SSL connection.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 28, 2021 8667       mapeters    Initial creation (mainly extracted from
 *                                     JmsSslConfiguration)
 * Apr 12, 2022 8677       tgurney     Use temporary keystore and truststore
 *                                     with randomly generated password. Switch
 *                                     from JKS to PKCS12
 *
 * </pre>
 *
 * @author mapeters
 */
/*
 * Implementation details: Keystore and truststore are each written to a temp
 * file when they are first accessed. These files are encrypted using a randomly
 * generated password that is kept in RAM and is never stored elsewhere. There
 * is a separate set of keystore/truststore files created for each
 * SslConfiguration instance. The files are deleted on JVM exit.
 */
public class SslConfiguration {

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(getClass());

    private final String clientName;

    private final Path clientCert;

    private final Path clientKey;

    private final Path rootCert;

    private static final String STORE_TYPE = "pkcs12";

    private static final String STORE_EXT = ".p12";

    private Path trustStorePath = null;

    private Path keyStorePath = null;

    private String storePassword = null;

    /*
     * Must hold this lock before accessing trustStorePath, keyStorePath, or
     * storePassword
     */
    private final Object storeLock = new Object();

    private final Set<PosixFilePermission> STORE_PERMS = EnumSet.of(
            PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

    protected SslConfiguration(String defaultClientName, String clientNameVar,
            Path certDir) {
        String certName = System.getenv(clientNameVar);
        if (certName == null) {
            certName = defaultClientName;
        }

        clientName = certName;
        clientCert = certDir.resolve(certName + ".crt");
        clientKey = certDir.resolve(certName + ".key");
        rootCert = certDir.resolve("root.crt");
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
            KeyStore keyStore = KeyStore.getInstance(STORE_TYPE);
            keyStore.load(null, null);
            keyStore.setKeyEntry(getClientName(), privateKey,
                    getStorePassword().toCharArray(), certs);
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
            KeyStore keyStore = KeyStore.getInstance(STORE_TYPE);
            keyStore.load(null, null);
            int alias = 1;
            for (Certificate cert : certs) {
                keyStore.setCertificateEntry(Integer.toString(alias), cert);
                alias += 1;
            }
            return keyStore;
        }
    }

    private Path createTempStore(String label) throws IOException {
        Path rval = com.raytheon.uf.common.util.file.Files.createTempFile(
                Paths.get(System.getProperty("java.io.tmpdir")),
                "pid" + Long.toString(ProcessHandle.current().pid()) + "-"
                        + label + "-",
                STORE_EXT, PosixFilePermissions.asFileAttribute(STORE_PERMS));
        rval.toFile().deleteOnExit();
        return rval;
    }

    /**
     * @return the trust store file as a {@link Path}
     */
    public Path getJavaTrustStoreFile() {
        synchronized (storeLock) {
            if (trustStorePath == null) {
                try {
                    trustStorePath = createTempStore("truststore");
                    KeyStore trustStore = loadTrustStore();
                    try (OutputStream out = Files
                            .newOutputStream(trustStorePath)) {
                        trustStore.store(out, getStorePassword().toCharArray());
                    }
                } catch (Exception e) {
                    statusHandler.error(
                            "Failed to create java trust store file.", e);
                }
            }
            return trustStorePath;
        }
    }

    /**
     * @return the key store file as a {@link Path}
     */
    public Path getJavaKeyStoreFile() {
        synchronized (storeLock) {
            if (keyStorePath == null) {
                try {
                    keyStorePath = createTempStore("keystore");
                    KeyStore keyStore = loadKeyStore();
                    try (OutputStream out = Files
                            .newOutputStream(keyStorePath)) {
                        keyStore.store(out, getStorePassword().toCharArray());
                    }
                } catch (Exception e) {
                    statusHandler.error("Failed to create java key store file.",
                            e);
                }
            }
            return keyStorePath;
        }
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
     * @return the key store / trust store password
     */
    public String getStorePassword() {
        synchronized (storeLock) {
            if (storePassword == null) {
                storePassword = UUID.randomUUID().toString();
            }
            return storePassword;
        }
    }
}
