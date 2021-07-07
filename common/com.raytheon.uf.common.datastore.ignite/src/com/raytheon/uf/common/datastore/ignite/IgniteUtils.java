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

/**
 * Contains various constants and utilities used with ignite.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 8, 2021  8450       mapeters     Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteUtils {

    public static final String PLUGIN_REGISTRY_CACHE_NAME = "data-store-cache-name-map";

    public static final String DEFAULT_CACHE = "defaultDataStore";

    public static final String WILDCARD_CACHE_NAME = "*";

    public static final String NO_CACHE_NAME = "none";

    public static final String IGNITE_CLUSTER_SERVERS = "IGNITE_CLUSTER_SERVERS";

    public static final String SECOND_IGNITE_CLUSTER_SERVERS = "SECOND_IGNITE_CLUSTER_SERVERS";

    /**
     * @return true if ignite is active/being used, false if only pypies is
     *         being used
     */
    public static boolean isIgniteActive() {
        return "ignite".equals(System.getenv("DATASTORE_PROVIDER"));
    }

    /**
     * Prevent instantiation.
     */
    private IgniteUtils() {
    }
}
