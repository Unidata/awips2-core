/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import com.raytheon.uf.common.security.encryption.AESEncryptor;

/**
 *
 * Utilities for working with ignite passwords.
 *
 * TODO RODO #8677: The ignite password encryption/decryption code in this and
 * associated files is based off similar JMS password code that exists in a
 * later version, so the similar code should be consolidated later on.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Oct 12, 2021  7899     mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgnitePasswordUtils {

    private static final String IGNITE_PASSWORD_KEY = "IIlYEURoPjmfZLowN6sPd5gmxEKhaDZDtmLkN5ldTHUpH658VF5gQw5cuUDGaDNYkDAwPDTUxwuN";

    private static final String PASSWORDS_PROPS_FILENAME = "passwords.properties";

    private static final String CERT_DB_VAR = "IGNITE_SSL_CERT_DB";

    private static final String CERT_DB = System.getenv(CERT_DB_VAR);

    private static final String KEYSTORE_PASSWORD_PROP = "a2.ignite.keystore.password";

    private static final String TRUSTSTORE_PASSWORD_PROP = "a2.ignite.truststore.password";

    /**
     * Private constructor to prevent instantiation
     */
    private IgnitePasswordUtils() {
    }

    /**
     * @return the decrypted ignite keystore password
     * @throws Exception
     */
    public static String getIgniteKeyStorePassword() throws Exception {
        return getIgnitePassword(KEYSTORE_PASSWORD_PROP);
    }

    /**
     * @return the decrypted ignite truststore password
     * @throws Exception
     */
    public static String getIgniteTrustStorePassword() throws Exception {
        return getIgnitePassword(TRUSTSTORE_PASSWORD_PROP);
    }

    private static String getIgnitePassword(String passwordPropKey)
            throws Exception {
        if (CERT_DB == null) {
            throw new IllegalStateException(
                    "Unable to load " + PASSWORDS_PROPS_FILENAME
                            + " file for ignite. Consider setting the "
                            + CERT_DB_VAR + " environment variable.");
        }
        Path certDB = Paths.get(CERT_DB);

        Path propsFile = certDB.resolve(PASSWORDS_PROPS_FILENAME);
        if (!Files.isRegularFile(propsFile)) {
            throw new IllegalStateException("Unable to load "
                    + PASSWORDS_PROPS_FILENAME
                    + " file for ignite. Consider updating the " + CERT_DB_VAR
                    + " environment variable and/or running the updateIgnitePasswords utility script.");
        }

        Properties properties = new Properties();
        try (InputStream fis = Files.newInputStream(propsFile)) {
            properties.load(fis);
        }

        String encryptedPassword = properties.getProperty(passwordPropKey);
        AESEncryptor encryptor = new AESEncryptor(IGNITE_PASSWORD_KEY);
        return encryptor.decrypt(encryptedPassword);
    }

    private static void updateIgnitePassword(String password,
            String passwordPropKey, Path propsFilePath) throws Exception {
        AESEncryptor encryptor = new AESEncryptor(IGNITE_PASSWORD_KEY);
        password = encryptor.encrypt(password);
        Properties properties = new Properties();
        if (Files.isRegularFile(propsFilePath)) {
            try (InputStream fis = Files.newInputStream(propsFilePath)) {
                properties.load(fis);
            }
        }
        properties.setProperty(passwordPropKey, password);

        try (OutputStream fos = Files.newOutputStream(propsFilePath)) {
            properties.store(fos, null);
        }
    }

    /**
     * Main method for calling from python to update a password.
     *
     * @param args
     *            the arguments, should either be "--update ${plaintextPassword}
     *            ${passwordKey} ${propertiesFilePath}"
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "Must provide option \"--update\".");
        }

        if ("--update".equals(args[0])) {
            if (args.length == 4) {
                updateIgnitePassword(args[1], args[2], Paths.get(args[3]));
            } else {
                throw new IllegalArgumentException(
                        "Must provide a password, password key, and properties file path with option \"--encrypt\".");
            }
        } else {
            throw new IllegalArgumentException(
                    "Option " + args[0] + " not recognized.");
        }
    }
}