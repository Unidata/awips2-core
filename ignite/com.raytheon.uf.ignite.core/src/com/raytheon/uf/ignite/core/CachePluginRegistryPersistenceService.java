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
package com.raytheon.uf.ignite.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

import javax.cache.Cache.Entry;
import javax.xml.bind.JAXB;

import org.apache.ignite.Ignite;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastore.ignite.IgniteCacheAccessor;
import com.raytheon.uf.common.datastore.ignite.IgniteServerManager;
import com.raytheon.uf.common.datastore.ignite.IgniteUtils;
import com.raytheon.uf.common.datastore.ignite.plugin.PluginRegistryConfig;
import com.raytheon.uf.common.datastore.ignite.plugin.PluginRegistryConfig.ConfigEntry;

/**
 * Ignite {@link Service} for persisting cache plugin registry configuration
 * across ignite server restarts. The plugin->cache mapping is saved to a file
 * on shutdown and loaded from that file on startup. Since plugin registration
 * can originate from client nodes this allows the server nodes to remember the
 * correct settings even if the client is not available or does not re-register
 * after a restart. This is specifically needed when all servers are restarted
 * together.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------------
 * Jul 15, 2021 8450       mapeters    Initial creation (extracted from
 *                                     CachePluginRegistry)
 * Sep 23, 2021 8608       mapeters    Moved from com.raytheon.uf.common.datastorage.ignite
 * Jun 21, 2022 8879       mapeters    Handle signature change in methods for
 *                                     doing ignite operations (do*Op)
 *
 * </pre>
 *
 * @author mapeters
 */
public class CachePluginRegistryPersistenceService implements Service {

    private static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory
            .getLogger(CachePluginRegistryPersistenceService.class);

    /**
     * Lock object to just ensure that execute() finishes before cancel() is
     * called, since the ignite javadoc indicates that that is not guaranteed.
     */
    private final Object lock = new Object();

    private IgniteCacheAccessor<String, String> cacheAccessor;

    private Path configFilePath;

    private PluginRegistryConfig loadedConfig;

    @IgniteInstanceResource
    public void setIgnite(Ignite ignite) {
        synchronized (lock) {
            /*
             * For some reason this gets called again with a null ignite when
             * cancelling the service, so make sure we only set things the first
             * time
             */
            if (cacheAccessor == null) {
                cacheAccessor = new IgniteServerManager(ignite)
                        .getCacheAccessor(
                                IgniteUtils.PLUGIN_REGISTRY_CACHE_NAME);
                configFilePath = Paths.get(
                        ignite.configuration().getIgniteHome(), "config",
                        "pluginRegistry.xml");
            }
        }
    }

    @Override
    public void init(ServiceContext ctx) throws Exception {
        // do nothing
    }

    @Override
    public void execute(ServiceContext ctx) throws Exception {
        synchronized (lock) {
            loadFromFile();
        }
    }

    @Override
    public void cancel(ServiceContext ctx) {
        synchronized (lock) {
            saveToFile();
        }
    }

    /**
     * Load the previously saved configuration from a file.
     */
    protected void loadFromFile() {
        logger.info("Loading cache plugin registry...");
        if (Files.isReadable(configFilePath)) {
            PluginRegistryConfig config = JAXB.unmarshal(
                    configFilePath.toFile(), PluginRegistryConfig.class);
            for (ConfigEntry entry : config.getEntries()) {
                String plugin = entry.getPlugin();
                String cacheName = entry.getCache();
                try {
                    cacheAccessor.doAsyncCacheOp(
                            c -> c.putIfAbsentAsync(plugin, cacheName), true);
                } catch (StorageException e) {
                    logger.error(
                            "Error storing plugin cache name mapping to cache: "
                                    + plugin + " -> " + cacheName,
                            e);
                }
            }
            logger.info("Loaded cache plugin registry: {}", config);
            loadedConfig = config;
        } else if (Files.exists(configFilePath)) {
            logger.warn("Unable to read cache plugin registry from {}",
                    configFilePath);
        } else {
            logger.info("Cache plugin registry does not exist at {}",
                    configFilePath);
        }
    }

    /**
     * Save the current configuration in the ignite node to a file.
     */
    protected void saveToFile() {
        logger.info("Saving cache plugin registry...");
        boolean writable = Files.isWritable(configFilePath);
        if (!writable) {
            if (!Files.exists(configFilePath)) {
                Path parent = configFilePath.getParent();
                if (!Files.isDirectory(parent)) {
                    try {
                        Files.createDirectories(parent);
                    } catch (IOException e) {
                        logger.error("Cannot write cache plugin registry to {}",
                                configFilePath, e);
                        return;
                    }
                }
            }
            writable = Files.isWritable(configFilePath.getParent());
        }
        if (writable) {
            PluginRegistryConfig outerConfig;
            try {
                outerConfig = cacheAccessor.doSyncCacheOp(c -> {
                    PluginRegistryConfig config = new PluginRegistryConfig();
                    Iterator<Entry<String, String>> it = c.iterator();
                    while (it.hasNext()) {
                        Entry<String, String> entry = it.next();
                        String plugin = entry.getKey();
                        String cacheName = entry.getValue();
                        config.addEntry(plugin, cacheName);
                    }
                    return config;
                }, true);
            } catch (StorageException e) {
                logger.error(
                        "Cannot write cache plugin registry to file due to error reading entries from cache",
                        e);
                return;
            }
            PluginRegistryConfig config = outerConfig;
            config.sortByPlugin();
            if (!config.isEmpty() && !config.equals(loadedConfig)) {
                JAXB.marshal(config, configFilePath.toFile());
                logger.info("Saved cache plugin registry: {}", config);
            } else {
                logger.warn(
                        "Skipped saving empty or redundant cache plugin registry: {}",
                        config);
            }
        } else {
            logger.error("Cannot write cache plugin registry to {}",
                    configFilePath);

        }
    }
}
