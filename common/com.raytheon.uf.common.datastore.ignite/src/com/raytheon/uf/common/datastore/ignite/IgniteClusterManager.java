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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastore.ignite.AbstractIgniteManager.IgniteCacheAccessor;

/**
 * Manages access to clients for the active ignite cluster(s) and maps caches to
 * clusters.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 7, 2021  8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteClusterManager {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, Integer> clusterNumsByCache = new ConcurrentHashMap<>();

    private final IgniteClientManager ignite1;

    private final IgniteClientManager ignite2;

    /**
     * Constructor. Ignite client instances for the managed cluster(s) are not
     * actually started up until {@link #initialize()} is called.
     *
     * @param clusterConfigGenerator1
     *            ignite config generator for the first cluster
     * @param clusterConfigGenerator2
     *            ignite config generator for the second cluster, ignored if
     *            null or "SECOND_IGNITE_CLUSTER_SERVERS" env variable is empty
     */
    public IgniteClusterManager(IIgniteConfigGenerator clusterConfigGenerator1,
            IIgniteConfigGenerator clusterConfigGenerator2) {
        ignite1 = new IgniteClientManager(clusterConfigGenerator1, 1);

        String clusterServers2 = System
                .getenv(IgniteUtils.SECOND_IGNITE_CLUSTER_SERVERS);
        if (clusterConfigGenerator2 != null && clusterServers2 != null
                && !clusterServers2.isEmpty()) {
            ignite2 = new IgniteClientManager(clusterConfigGenerator2, 2);
        } else {
            ignite2 = null;
        }
    }

    /**
     * Initialize the managed ignite client instance(s).
     */
    public void initialize() {
        String clusterServers1 = System
                .getenv(IgniteUtils.IGNITE_CLUSTER_SERVERS);
        ignite1.initialize();
        if (ignite2 != null) {
            ignite2.initialize();
            String clusterServers2 = System
                    .getenv(IgniteUtils.SECOND_IGNITE_CLUSTER_SERVERS);
            logger.info("Running ignite with two clusters: " + clusterServers1
                    + " and " + clusterServers2);
        } else {
            logger.info("Running ignite with one cluster: " + clusterServers1);
        }
    }

    /**
     * Get the ignite client manager for the given cache.
     *
     * @param cacheName
     *            the cache name
     * @return the ignite client manager
     */
    public IgniteClientManager getIgniteManager(String cacheName) {
        return getIgniteManager(getClusterNum(cacheName));
    }

    /**
     * Get the ignite client manager for the given cluster.
     *
     * @param clusterNum
     *            the cluster number
     * @return the ignite client manager.
     */
    public IgniteClientManager getIgniteManager(int clusterNum) {
        clusterNum = getValidClusterNum(clusterNum);
        return clusterNum == 1 ? ignite1 : ignite2;
    }

    /**
     * Get a cache accessor for the given cache.
     *
     * @param cacheName
     *            the cache name
     * @return the cache accessor
     */
    public <K, V> IgniteCacheAccessor<K, V> getCacheAccessor(String cacheName) {
        return getIgniteManager(cacheName).getCacheAccessor(cacheName);
    }

    /**
     * Get the given cache.
     *
     * @param cacheName
     *            the cache name
     * @return the cache
     */
    public <K, V> IgniteCache<K, V> getCache(String cacheName) {
        return this.<K, V> getCacheAccessor(cacheName).getCache();
    }

    /**
     * Plugins that need a custom cache configuration will need to use this
     * method to create the cache and then
     * CachePluginRegistry.registerPluginCacheName(String, String) to associate
     * the cache with the plugin.
     *
     * @param config
     *            the cache config
     * @param clusterNum
     *            the ignite cluster to add the cache to
     *
     * @return the cache name
     */
    public String addCache(CacheConfiguration<?, ?> config, int clusterNum) {
        String cacheName = config.getName();
        clusterNum = getValidClusterNum(clusterNum);
        Integer prev = clusterNumsByCache.put(cacheName, clusterNum);
        if (prev == null) {
            logger.info("Ignite cluster number has been set to {} for {}",
                    clusterNum, cacheName);
        } else {
            throw new IllegalStateException(
                    "Attempted to add multiple cache configs for " + cacheName);
        }

        getIgniteManager(clusterNum).addCache(config);

        return config.getName();
    }

    private int getValidClusterNum(int clusterNum) {
        if (clusterNum == 1) {
            return clusterNum;
        }
        if (clusterNum == 2) {
            return ignite2 == null ? 1 : 2;
        }
        throw new IllegalArgumentException(
                "Invalid cluster number: " + clusterNum);
    }

    private int getClusterNum(String cacheName) {
        Integer clusterNum = clusterNumsByCache.get(cacheName);
        if (clusterNum == null) {
            throw new IllegalArgumentException("No cluster for " + cacheName);
        }

        return getValidClusterNum(clusterNum);
    }
}
