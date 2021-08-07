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
package com.raytheon.uf.common.datastore.ignite.plugin;

import java.io.File;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastore.ignite.IgniteCacheAccessor;
import com.raytheon.uf.common.datastore.ignite.IgniteClusterManager;
import com.raytheon.uf.common.datastore.ignite.IgniteUtils;

/**
 * Registry of cache configuration for plugins. Plugins can specify a custom
 * cache per plugin and optionally register a custom cache configuration for the
 * plugin cache.
 *
 * The plugin configuration is managed in three places:
 * <ol>
 * <li>An ignite cache is used to keep all ignite nodes using the same cacheName
 * for any plugin. A plugin can register on a single ignite node and all nodes
 * will use the same cacheName because of the ignite cache. This should be
 * considered the authoritative source of cacheNames.
 * <li>A local map is just used for quick lookups, since the plugin
 * configuration rarely changes it saves time to keep the mapping local. This
 * local mapping is periodically refreshed to ensure it is in sync with the
 * ignite cache.
 * <li>A config file is used on server nodes to repopulate the ignite cache
 * during startup. See CachePluginRegistryPersisterService.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Feb 03, 2020  7628     bsteffen  Initial creation
 * Mar 26, 2020  8074     bsteffen  Parse plugin from filenames that start with '/'
 * Apr 01, 2020  8072     bsteffen  Store cache to file on server nodes.
 * Jun 25, 2021  8450     mapeters  Updated for centralized ignite instance management,
 *                                  moved persistence to file on server side to
 *                                  CachePluginRegistryPersistenceService
 *
 * </pre>
 *
 * @author bsteffen
 */
public class CachePluginRegistry {

    private static final Logger logger = LoggerFactory
            .getLogger(CachePluginRegistry.class);

    private static final long REFRESH_INTERVAL = Duration
            .ofSeconds(IgniteUtils.getLongProperty(
                    "ignite.cache.plugin.registry.refresh.interval.secs"))
            .toMillis();

    private final AtomicLong nextRefresh = new AtomicLong(
            System.currentTimeMillis() + REFRESH_INTERVAL);

    private final Map<String, String> cacheNamesByPlugin = Collections
            .synchronizedMap(new TreeMap<>());

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private IgniteCacheAccessor<String, String> cacheAccessor;

    public CachePluginRegistry() {
    }

    public String registerPluginCacheName(String plugin, String cacheName) {
        lock.readLock().lock();
        try {
            if (cacheAccessor != null) {
                try {
                    cacheAccessor
                            .doAsyncCacheOp(c -> c.putAsync(plugin, cacheName));
                } catch (StorageException e) {
                    logger.error(
                            "Error storing plugin cache name mapping to ignite server: "
                                    + plugin + " -> " + cacheName,
                            e);
                }
            }

            String prev = cacheNamesByPlugin.put(plugin, cacheName);
            if (prev == null) {
                logger.info("Ignite cache name has been set to {} for {}",
                        cacheName, plugin);
            } else if (!prev.equals(cacheName)) {
                logger.warn("Ignite cache name has changed for {}: {} -> {}",
                        plugin, prev, cacheName);
            }
        } finally {
            lock.readLock().unlock();
        }

        return cacheName;
    }

    public void initialize(IgniteClusterManager clusterManager) {
        lock.writeLock().lock();
        try {
            this.cacheAccessor = clusterManager
                    .getCacheAccessor(IgniteUtils.PLUGIN_REGISTRY_CACHE_NAME);
            try {
                cacheAccessor
                        .doAsyncCacheOp(c -> c.putAllAsync(cacheNamesByPlugin));
            } catch (StorageException e) {
                logger.error(
                        "Error storing plugin cache name mappings to ignite server",
                        e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public String getCacheName(File file) {
        throwIfUninitialized();
        refreshCache();
        String plugin = getPlugin(file);
        if (cacheNamesByPlugin.containsKey(plugin)) {
            return cacheNamesByPlugin.get(plugin);
        } else {
            String name = null;
            boolean errorOccurred = false;
            try {
                name = cacheAccessor.doAsyncCacheOp(c -> c.getAsync(plugin));
            } catch (StorageException e) {
                logger.error("Error retrieving cache name to use for plugin "
                        + plugin + ", temporarily falling back to "
                        + IgniteUtils.DEFAULT_CACHE, e);
                errorOccurred = true;
            }
            if (name == null) {
                name = IgniteUtils.DEFAULT_CACHE;
            }
            if (!errorOccurred) {
                cacheNamesByPlugin.put(plugin, name);
            }
            return name;
        }
    }

    protected static String getPlugin(File file) {
        String path = file.getPath();
        int start = 0;
        if (path.charAt(0) == File.separatorChar) {
            start = 1;
        }
        int index = path.indexOf(File.separatorChar, start);
        if (index >= 0) {
            return path.substring(start, index);
        } else {
            return path;
        }
    }

    /**
     * Clear the local mapping to be sure it matches the ignite cache when it
     * repopulates (if we haven't already cleared it within the refresh
     * interval).
     */
    protected void refreshCache() {
        long currentTime = System.currentTimeMillis();
        if (nextRefresh.get() < currentTime) {
            cacheNamesByPlugin.clear();
            nextRefresh.set(currentTime + REFRESH_INTERVAL);
        }
        long runTime = System.currentTimeMillis() - currentTime;
        if (runTime > 500) {
            logger.warn("Cache Refresh took {} ms", runTime);
        }
    }

    private void throwIfUninitialized() {
        lock.readLock().lock();
        try {
            if (cacheAccessor == null) {
                throw new IllegalStateException(
                        "Cache plugin registry has not been initialized");
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
