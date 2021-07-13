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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.cache.Cache.Entry;
import javax.xml.bind.JAXB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastore.ignite.AbstractIgniteManager;
import com.raytheon.uf.common.datastore.ignite.AbstractIgniteManager.IgniteCacheAccessor;
import com.raytheon.uf.common.datastore.ignite.plugin.PluginRegistryConfig.ConfigEntry;

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
 * <li>The config file is used on server nodes to repopulate the ignite cache
 * during startup. This allows consistent cacheNames even if all server nodes
 * are restarted. Since plugin registration can originate from client nodes this
 * allows the server nodes to remember the correct settings even if the client
 * is not available or does not re-register after a restart.
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
 * Jun 25, 2021  8450     mapeters  Updated for centralized ignite instance management
 *
 * </pre>
 *
 * @author bsteffen
 */
public class CachePluginRegistry {

    private static final Logger logger = LoggerFactory
            .getLogger(CachePluginRegistry.class);

    private static final String CACHE_NAME = "data-store-cache-name-map";

    public static final String DEFAULT_CACHE = "defaultDataStore";

    private static final long REFRESH_INTERVAL = Duration.ofMinutes(5)
            .toMillis();

    private final AtomicLong nextRefresh = new AtomicLong(
            System.currentTimeMillis() + REFRESH_INTERVAL);

    private final Map<String, String> cacheNamesByPlugin = Collections
            .synchronizedMap(new TreeMap<>());

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private AbstractIgniteManager igniteManager;

    private IgniteCacheAccessor<String, String> cacheAccessor;

    protected PluginRegistryConfig savedConfig;

    public CachePluginRegistry() {
    }

    public String registerPluginCacheName(String plugin, String cacheName) {
        lock.readLock().lock();
        try {
            if (cacheAccessor != null) {
                cacheAccessor.getCache().put(plugin, cacheName);
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

    public void initialize(AbstractIgniteManager igniteManager) {
        lock.writeLock().lock();
        try {
            this.igniteManager = igniteManager;
            this.cacheAccessor = igniteManager.getCacheAccessor(CACHE_NAME);
            cacheAccessor.getCache().putAll(cacheNamesByPlugin);
            if (!igniteManager.getIgnite().configuration().isClientMode()) {
                loadFromFile();
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
            String name = cacheAccessor.getCache().get(plugin);
            if (name == null) {
                name = DEFAULT_CACHE;
            }
            cacheNamesByPlugin.put(plugin, name);
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
     * repopulates. On server nodes this will also save the current ignite cache
     * into a file if it has changed.
     */
    protected void refreshCache() {
        long currentTime = System.currentTimeMillis();
        if (nextRefresh.get() < currentTime) {
            cacheNamesByPlugin.clear();
            nextRefresh.set(currentTime + REFRESH_INTERVAL);
            if (!igniteManager.getIgnite().configuration().isClientMode()) {
                saveToFile();
            }
        }
        long runTime = System.currentTimeMillis() - currentTime;
        if (runTime > 500) {
            logger.warn("Cache Refresh took {} ms", runTime);
        }
    }

    /**
     * Load the previously saved configuration from a file. This should only be
     * called on server nodes.
     */
    protected void loadFromFile() {
        Path registryFile = getConfigFilePath();
        if (Files.isReadable(registryFile)) {
            PluginRegistryConfig config = JAXB.unmarshal(registryFile.toFile(),
                    PluginRegistryConfig.class);
            for (ConfigEntry entry : config.getEntries()) {
                String plugin = entry.getPlugin();
                String cacheName = entry.getCache();
                if (cacheAccessor.getCache().putIfAbsent(plugin, cacheName)) {
                    cacheNamesByPlugin.put(plugin, cacheName);
                }
            }
            logger.info("Loaded cache plugin registry: {}", config);
            savedConfig = config;
        } else if (Files.exists(registryFile)) {
            logger.warn("Unable to read cache plugin registry from {}",
                    registryFile);
        } else {
            logger.info("Cache plugin registry does not exist at {}",
                    registryFile);
        }
    }

    /**
     * Save the current configuration in the ignite node to a file if it is
     * different from the previously saved configuration. This should only be
     * done on server nodes. Since this requires iterating the entire ignite
     * cache the local mapping is also updated to match the ignite cache.
     */
    protected void saveToFile() {
        Path registryFile = getConfigFilePath();
        boolean writable = Files.isWritable(registryFile);
        if (!writable) {
            if (!Files.exists(registryFile)) {
                Path parent = registryFile.getParent();
                if (!Files.isDirectory(parent)) {
                    try {
                        Files.createDirectories(parent);
                    } catch (IOException e) {
                        logger.error("Cannot write cache plugin registry to {}",
                                registryFile, e);
                        return;
                    }
                }
            }
            writable = Files.isWritable(registryFile.getParent());
        }
        if (writable) {
            PluginRegistryConfig config = new PluginRegistryConfig();
            Iterator<Entry<String, String>> it = cacheAccessor.getCache()
                    .iterator();
            while (it.hasNext()) {
                Entry<String, String> entry = it.next();
                String plugin = entry.getKey();
                String cacheName = entry.getValue();
                cacheNamesByPlugin.put(plugin, cacheName);
                config.addEntry(plugin, cacheName);
            }
            config.sortByPlugin();
            if (!config.isEmpty() && !config.equals(savedConfig)) {
                logger.info("Saving cache plugin registry: {}", config);
                JAXB.marshal(config, registryFile.toFile());
                savedConfig = config;
            }
        } else {
            logger.error("Cannot write cache plugin registry to {}",
                    registryFile);

        }
    }

    protected Path getConfigFilePath() {
        return Paths.get(
                igniteManager.getIgnite().configuration().getIgniteHome(),
                "config", "pluginRegistry.xml");
    }

    private void throwIfUninitialized() {
        lock.readLock().lock();
        try {
            if (igniteManager == null) {
                throw new IllegalStateException(
                        "Cache plugin registry has not been initialized");
            }
        } finally {
            lock.readLock().unlock();
        }
    }
}
