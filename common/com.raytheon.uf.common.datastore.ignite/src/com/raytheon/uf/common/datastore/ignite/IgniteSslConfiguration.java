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

import java.nio.file.Path;
import java.nio.file.Paths;

import com.raytheon.uf.common.comm.ssl.AbstractSslConfiguration;

/**
 *
 * Configures SSL credentials for an ignite connection.
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
public class IgniteSslConfiguration extends AbstractSslConfiguration {

    private static final String KEY_STORE_TYPE = "pkcs12";

    private static final String KEY_STORE_EXT = ".p12";

    private static final String CERTIFICATE_DIR = "IGNITE_SSL_CERT_DB";

    private static final String CERTIFICATE_NAME = "IGNITE_SSL_CERT_NAME";

    /**
     * @param defaultClientName
     *            the clientName to use if the environment variables does not
     *            specify a name. This argument is ignored if the environment
     *            contains a different name.
     */
    public IgniteSslConfiguration(String defaultClientName) {
        super(defaultClientName, CERTIFICATE_NAME, getCertDbPath(),
                KEY_STORE_TYPE, KEY_STORE_EXT);
    }

    private static Path getCertDbPath() {
        String certDB = System.getenv(CERTIFICATE_DIR);
        if (certDB == null) {
            throw new IllegalStateException(
                    "Unable to load ssl certificates for ignite ssl. Consider setting the environmental variable: "
                            + CERTIFICATE_DIR);
        }
        return Paths.get(certDB);

    }

    /**
     * @return the key store path as a string
     */
    public String getJavaKeyStorePath() {
        return getJavaKeyStoreFile().toAbsolutePath().toString();
    }

    /**
     * @return the trust store path as a string
     */
    public String getJavaTrustStorePath() {
        return getJavaTrustStoreFile().toAbsolutePath().toString();
    }

    @Override
    public String getKeyStorePassword() throws Exception {
        return IgnitePasswordUtils.getIgniteKeyStorePassword();
    }

    @Override
    public String getTrustStorePassword() throws Exception {
        return IgnitePasswordUtils.getIgniteTrustStorePassword();
    }

}
