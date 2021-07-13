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
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.LazyDataStore;
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

    private static final String NO_CACHE_NAME = "none";

    private final AbstractIgniteManager igniteManager;

    private final CachePluginRegistry pluginRegistry;

    public IgniteDataStoreFactory(AbstractIgniteManager igniteManager,
            CachePluginRegistry pluginRegistry) {
        String name = getClass().getSimpleName();
        logger.info("Creating " + name);
        this.igniteManager = igniteManager;
        this.igniteManager.initialize();
        this.pluginRegistry = pluginRegistry;
        this.pluginRegistry.initialize(this.igniteManager);
        logger.info("Created " + name);
    }

    @Override
    public IDataStore getDataStore(File file, boolean useLocking) {
        String cacheName = pluginRegistry.getCacheName(file);
        if (NO_CACHE_NAME.equalsIgnoreCase(cacheName)) {
            IgniteCache<?, ?> defaultCache = igniteManager
                    .getCache(CachePluginRegistry.DEFAULT_CACHE);
            return getThroughDataStore(file, defaultCache);
        } else {
            IgniteCache<DataStoreKey, DataStoreValue> cache = igniteManager
                    .getCache(cacheName);
            IDataStore throughStore = new LazyDataStore() {
                @Override
                protected IDataStore createDataStore() {
                    return getThroughDataStore(file, cache);
                }
            };
            IgniteDataStore dataStore = new IgniteDataStore(file,
                    igniteManager.getCacheAccessor(cacheName), throughStore);
            return dataStore;
        }
    }

    private IDataStore getThroughDataStore(File file, IgniteCache<?, ?> cache) {
        @SuppressWarnings("unchecked")
        CacheConfiguration<?, ?> config = cache
                .getConfiguration(CacheConfiguration.class);
        Object factory = config.getCacheStoreFactory();
        if (factory instanceof DataStoreCacheStoreFactory) {
            return ((DataStoreCacheStoreFactory) factory).getDataStore(file);
        } else {
            String cacheName = cache.getName();
            IgniteCompute compute = igniteManager.getIgnite().compute();
            GetCacheStoreFactoryTask task = new GetCacheStoreFactoryTask(
                    cacheName);
            factory = compute.call(task);
            if (factory instanceof DataStoreCacheStoreFactory) {
                return ((DataStoreCacheStoreFactory) factory)
                        .getDataStore(file);
            } else {
                throw new IllegalStateException(cacheName
                        + " does not have a DataStoreCacheStoreFactory "
                        + "configured as the CacheStoreFactory. (got instead: "
                        + factory + ")");
            }
        }
    }

    private static class GetCacheStoreFactoryTask
            implements IgniteCallable<Object> {

        private static final long serialVersionUID = 1L;

        @IgniteInstanceResource
        private Ignite ignite;

        private String cacheName;

        public GetCacheStoreFactoryTask(String cacheName) {
            this.cacheName = cacheName;
        }

        @Override
        public Object call() {
            IgniteCache<DataStoreKey, DataStoreValue> cache = ignite
                    .getOrCreateCache(cacheName);
            @SuppressWarnings("unchecked")
            CacheConfiguration<?, ?> config = cache
                    .getConfiguration(CacheConfiguration.class);
            return config.getCacheStoreFactory();
        }
    }

}
