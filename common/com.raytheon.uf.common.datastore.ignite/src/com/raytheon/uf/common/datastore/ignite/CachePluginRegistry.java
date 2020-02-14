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

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.cache.Cache;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.configuration.CacheConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry of cache configuration for plugins. Plugins can specify a custom
 * cache per plugin and optionally register a custom cache configuration for the
 * plugin cache.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Feb 03, 2020  7628     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class CachePluginRegistry {

    private static final Logger logger = LoggerFactory
            .getLogger(CachePluginRegistry.class);

    private static final String CACHE_NAME = "data-store-cache-name-map";

    private static final String DEFAULT_CACHE = "defaultDataStore";

    private final Map<String, String> cacheNamesByPlugin = new ConcurrentHashMap<>();

    private final List<CacheConfiguration<?, ?>> configs = new CopyOnWriteArrayList<>();

    private Ignite ignite;

    protected IgniteCache<String, String> cache;

    public String registerPluginCacheName(String plugin, String cacheName) {
        if (cache != null) {
            cache.put(plugin, cacheName);
        }
        String prev = cacheNamesByPlugin.put(plugin, cacheName);
        if (prev == null) {
            logger.info("Ignite cache name has been set to {} for {}",
                    cacheName, plugin);
        } else if (!prev.equals(cacheName)) {
            logger.warn("Ignite cache name has changed for {} {} -> {}", plugin,
                    prev, cacheName);
        }
        return cacheName;
    }

    /**
     * Plugins that need a custom cache configuration will need to use this
     * method to create the cache and then
     * {@link #registerPluginCacheName(String, String)} to associate the cache
     * with the plugin.
     * 
     * @param config
     * @return
     */
    public String addCache(CacheConfiguration<?, ?> config) {
        if (ignite == null) {
            configs.add(config);
            return config.getName();
        } else {
            Cache<?, ?> cache = ignite.getOrCreateCache(config);
            return cache.getName();
        }
    }

    public Ignite setIgnite(Ignite ignite) {
        for (CacheConfiguration<?, ?> config : configs) {
            ignite.getOrCreateCache(config);
        }
        configs.clear();
        cache = ignite.getOrCreateCache(CACHE_NAME);
        cache.putAll(cacheNamesByPlugin);
        this.ignite = ignite;
        return ignite;
    }

    public String getCacheName(File file) {
        String plugin = getPlugin(file);
        if (cacheNamesByPlugin.containsKey(plugin)) {
            return cacheNamesByPlugin.get(plugin);
        } else {
            String name = cache.get(plugin);
            if (name == null) {
                name = DEFAULT_CACHE;
            }
            cacheNamesByPlugin.put(plugin, name);
            return name;
        }
    }

    protected static String getPlugin(File file) {
        String path = file.getPath();
        int index = path.indexOf(File.separatorChar);
        if (index >= 0) {
            return path.substring(0, index);
        } else {
            return path;
        }
    }

}
