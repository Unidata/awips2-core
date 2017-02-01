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

import org.apache.qpid.jms.ConnectionURL;

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
 *
 * </pre>
 *
 * @author bsteffen
 */
public class JmsSslConfiguration {

    private static final String CERTIFICATE_DIR = "QPID_SSL_CERT_DB";

    private static final String CERTIFICATE_NAME = "QPID_SSL_CERT_NAME";

    private final String clientName;

    private final Path clientCert;

    private final Path clientKey;

    private final Path rootCert;

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
        String certName = System.getenv(CERTIFICATE_NAME);
        if (certName == null) {
            certName = defaultClientName;
        }

        clientName = certName;
        clientCert = certDB.resolve(certName + ".crt");
        clientKey = certDB.resolve(certName + ".key");
        rootCert = certDB.resolve("root.crt");
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
     * If the url passed in has the ssl option set to true then this will find
     * the correct ssl certificates based off the environmental variables and
     * add the corresponding options to the URL. If there is no ssl option on
     * the provided url, or if it is not set to true, then no changes are made
     * to the URL.
     * 
     * @param url
     *            The url, which may need to be modified to add ssl
     *            certificates.
     * @return THe same URL that was passed in.
     */
    public static ConnectionURL configureURL(ConnectionURL url) {
        String ssl = url.getOption("ssl");
        if (ssl != null && Boolean.parseBoolean(ssl)) {
            JmsSslConfiguration config = new JmsSslConfiguration(
                    url.getUsername());
            url.setOption("client_cert_path",
                    config.getClientCert().toString());
            url.setOption("client_cert_priv_key_path",
                    config.getClientKey().toString());
            url.setOption("trusted_certs_path",
                    config.getRootCert().toString());
        }
        return url;
    }

}
