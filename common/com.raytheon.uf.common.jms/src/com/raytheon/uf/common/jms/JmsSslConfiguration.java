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
package com.raytheon.uf.common.jms;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.raytheon.uf.common.comm.ssl.AbstractSslConfiguration;

/**
 *
 * Uses common environmental variables to find the ssl certificates for a jms
 * connection.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 02, 2017  6085     bsteffen    Initial creation
 * Jun 26, 2017  6340     rjpeter     Check file times and recreate stores if necessary.
 * Jul 17, 2019  7724     mrichardson Upgrade Qpid to Qpid Proton.
 * Oct 29, 2021  8667     mapeters    Abstracted a lot out to {@link AbstractSslConfiguration}
 *
 * </pre>
 *
 * @author bsteffen
 */
public class JmsSslConfiguration extends AbstractSslConfiguration {

    private static final String KEY_STORE_TYPE = "jks";

    private static final String KEY_STORE_EXT = ".jks";

    private static final String CERTIFICATE_DIR = "QPID_SSL_CERT_DB";

    private static final String CERTIFICATE_NAME = "QPID_SSL_CERT_NAME";

    private static final String CERTIFICATE_PASSWORD = "QPID_SSL_CERT_PASSWORD";

    /**
     * Create a new instance. If the environmental variables do not specify a
     * specific username then the current user name will be used.
     */
    public JmsSslConfiguration() {
        this(System.getProperty("user.name"));
    }

    /**
     * @param defaultClientName
     *            the clientName to use if the environment variables does not
     *            specify a name. This argument is ignored if the environment
     *            contains a different name.
     */
    public JmsSslConfiguration(String defaultClientName) {
        super(defaultClientName, CERTIFICATE_NAME, getCertDbPath(),
                KEY_STORE_TYPE, KEY_STORE_EXT);
    }

    private static Path getCertDbPath() {
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
                throw new IllegalStateException(
                        "Unable to load ssl certificates for jms ssl. Consider setting the environmental variable: "
                                + CERTIFICATE_DIR);
            }
        } else {
            certDB = Paths.get(certDir);
        }
        return certDB;
    }

    public String getPassword() {
        String password = System.getenv(CERTIFICATE_PASSWORD);
        if (password == null) {
            password = "password";
        }
        return password;
    }

    @Override
    public String getKeyStorePassword() {
        return getPassword();
    }

    @Override
    public String getTrustStorePassword() {
        return getPassword();
    }

}
