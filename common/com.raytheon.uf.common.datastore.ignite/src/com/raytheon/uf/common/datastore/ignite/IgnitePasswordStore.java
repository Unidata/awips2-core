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
package com.raytheon.uf.common.datastore.ignite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Stores passwords for Ignite keystore and truststore
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 18, 2020            tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class IgnitePasswordStore {

    private final Properties properties = new Properties();

    public IgnitePasswordStore(String propertiesFilePath) {
        String igniteHome = System.getenv("IGNITE_HOME");
        File passwordPropertiesFile = new File(propertiesFilePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(passwordPropertiesFile)))) {
            properties.load(br);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getKeystorePassword() {
        return properties.getProperty("a2.ignite.keystore.password");
    }

    public String getTruststorePassword() {
        return properties.getProperty("a2.ignite.truststore.password");
    }
}
