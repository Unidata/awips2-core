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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Loads in the ignite.server.properties system properties in an ignite server.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 21, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteServerPropertiesLoader {

    private static final Path PROPS_FILE_PATH = Paths.get(
            IgniteServerUtils.IGNITE_HOME, "config",
            "ignite.server.properties");

    /**
     * Load the ignite.server.properties into the system properties.
     *
     * @return null (spring requires a return value)
     */
    public static Object load() {
        try (InputStream is = Files.newInputStream(PROPS_FILE_PATH)) {
            System.getProperties().load(is);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Unable to read ignite server properties", e);
        }
        return null;
    }
}
