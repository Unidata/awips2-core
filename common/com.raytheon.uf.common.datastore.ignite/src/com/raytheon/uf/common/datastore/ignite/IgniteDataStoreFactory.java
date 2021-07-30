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

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.LazyDataStore;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastore.ignite.plugin.CachePluginRegistry;
import com.raytheon.uf.common.datastore.ignite.store.DataStoreCacheStoreFactory;

/**
 *
 * {@link IDataStoreFactory} for making {@link IgniteDataStore}s.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 29, 2019  7628     bsteffen  Initial creation
 * Mar 26, 2020  8074     bsteffen  Add capability to skip ignite.
 * Dec  8, 2020  8299     tgurney   Add code to get the through-datastore from
 *                                  a server node if it is not found in the
 *                                  client's configuration
 * Jun 25, 2021  8450     mapeters  Centralize ignite instance/cache management
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteDataStoreFactory implements IDataStoreFactory {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IgniteClusterManager clusterManager;

    private final CachePluginRegistry pluginRegistry;

    public IgniteDataStoreFactory(IgniteClusterManager clusterManager,
            CachePluginRegistry pluginRegistry) {
        String name = getClass().getSimpleName();
        logger.info("Creating " + name);
        this.clusterManager = clusterManager;
        this.clusterManager.initialize();
        this.pluginRegistry = pluginRegistry;
        this.pluginRegistry.initialize(this.clusterManager);
        logger.info("Created " + name);
    }

    @Override
    public IDataStore getDataStore(File file, boolean useLocking) {
        String cacheName = pluginRegistry.getCacheName(file);
        if (IgniteUtils.NO_CACHE_NAME.equalsIgnoreCase(cacheName)) {
            IgniteCacheAccessor<?, ?> defaultCacheAccessor = clusterManager
                    .getCacheAccessor(IgniteUtils.DEFAULT_CACHE);
            return getThroughDataStore(file, defaultCacheAccessor);
        } else {
            IgniteCacheAccessor<DataStoreKey, DataStoreValue> cache = clusterManager
                    .getCacheAccessor(cacheName);
            IDataStore throughStore = new LazyDataStore() {
                @Override
                protected IDataStore createDataStore() {
                    return getThroughDataStore(file, cache);
                }
            };
            IgniteDataStore dataStore = new IgniteDataStore(file,
                    clusterManager.getCacheAccessor(cacheName), throughStore);
            return dataStore;
        }
    }

    @SuppressWarnings("unchecked")
    private IDataStore getThroughDataStore(File file,
            IgniteCacheAccessor<?, ?> cacheAccessor) {

        CacheConfiguration<?, ?> config;
        Object factory = null;
        try {
            config = cacheAccessor.doSyncCacheOp(
                    c -> c.getConfiguration(CacheConfiguration.class));
            factory = config.getCacheStoreFactory();
        } catch (StorageException e) {
            logger.error("Error getting cache configuration", e);
        }
        if (factory instanceof DataStoreCacheStoreFactory) {
            return ((DataStoreCacheStoreFactory) factory).getDataStore(file);
        } else {
            String cacheName = cacheAccessor.getCacheName();
            try {
                factory = clusterManager.getIgniteClientManager(cacheName)
                        .doIgniteOp(ignite -> {
                            IgniteCompute compute = ignite.compute();
                            GetCacheStoreFactoryTask task = new GetCacheStoreFactoryTask(
                                    cacheName);
                            return compute.call(task);
                        });
            } catch (StorageException e) {
                logger.error(
                        "Error retrieving cache store factory for " + cacheName,
                        e);
            }
            if (factory instanceof DataStoreCacheStoreFactory) {
                return ((DataStoreCacheStoreFactory) factory)
                        .getDataStore(file);
            } else {
                throw new IllegalStateException(cacheName
                        + " does not have a DataStoreCacheStoreFactory "
                        + "configured as the cache store factory. (got instead: "
                        + factory + ")");
            }
        }
    }

    private static class GetCacheStoreFactoryTask
            implements IgniteCallable<Object> {

        private static final long serialVersionUID = 1L;

        private IgniteCacheAccessor<?, ?> cacheAccessor;

        private String cacheName;

        public GetCacheStoreFactoryTask(String cacheName) {
            this.cacheName = cacheName;
        }

        @IgniteInstanceResource
        public void setIgnite(Ignite ignite) {
            this.cacheAccessor = new IgniteServerManager(ignite)
                    .getCacheAccessor(cacheName);
        }

        @Override
        public Object call() throws StorageException {
            return cacheAccessor.doSyncCacheOp(c -> {
                @SuppressWarnings("unchecked")
                CacheConfiguration<?, ?> config = c
                        .getConfiguration(CacheConfiguration.class);
                return config.getCacheStoreFactory();
            });
        }
    }
}
