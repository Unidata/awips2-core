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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.failure.FailureContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastore.ignite.IgniteClientFailureHandler.IgniteClientFailureListener;

/**
 * Manager for an ignite client instance. Manages cache configuration for the
 * ignite instance and restarting the ignite client on failure.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 25, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteClientManager extends AbstractIgniteManager
        implements IgniteClientFailureListener {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, CacheConfiguration<?, ?>> cacheConfigs = new HashMap<>();

    private final IIgniteConfigGenerator configGenerator;

    private Ignite ignite;

    /**
     * Constructor.
     *
     * @param configGenerator
     *            generates ignite config instances to use when starting up the
     *            managed ignite instance
     */
    public IgniteClientManager(IIgniteConfigGenerator configGenerator) {
        this.configGenerator = configGenerator;
    }

    @Override
    public void initialize() {
        lock.writeLock().lock();
        try {
            logger.info("Initializing ignite client...");
            internalInitIgnite();
            logger.info("Successfully initialized ignite client");
            IgniteClientFailureHandler.getInstance().addListener(this);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Ignite getIgnite() {
        lock.readLock().lock();
        try {
            throwIfUninitialized();
            return ignite;
        } finally {
            lock.readLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K, V> IgniteCache<K, V> getCache(String cacheName) {
        lock.readLock().lock();
        try {
            throwIfUninitialized();
            IgniteCache<K, V> cache;
            CacheConfiguration<?, ?> config = cacheConfigs.get(cacheName);
            if (config != null) {
                cache = (IgniteCache<K, V>) ignite.getOrCreateCache(config);
            } else {
                /*
                 * The config has to already exist on the server side, but we
                 * still have to use getOrCreateCache() instead of cache()
                 * because of the defaultDataStore config having its name set to
                 * "*"
                 */
                cache = ignite.getOrCreateCache(cacheName);
            }
            return cache;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void handle(Ignite failedIgnite, FailureContext failureCtx) {
        lock.writeLock().lock();
        try {
            throwIfUninitialized();
            String igniteName = ignite.name();
            String failedIgniteName = failedIgnite.name();
            boolean igniteInstancesMatch = igniteName == null
                    ? failedIgniteName == null
                    : igniteName.equals(failedIgniteName);
            if (!igniteInstancesMatch) {
                throw new IllegalStateException("Managed ignite client '"
                        + igniteName + "' does not match failed ignite client: "
                        + failedIgnite);
            }
            logger.info("Stopping failed ignite client...");
            ignite.close();
            logger.info("Starting new ignite client...");
            internalInitIgnite();
            logger.info("Successfully started new ignite client");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Plugins that need a custom cache configuration will need to use this
     * method to create the cache and then
     * CachePluginRegistry.registerPluginCacheName to associate the cache with
     * the plugin.
     *
     * @param config
     *            the cache config
     * @return the cache name
     */
    public String addCache(CacheConfiguration<?, ?> config) {
        lock.readLock().lock();
        try {
            String cacheName = config.getName();
            if (cacheName.contains("*")) {
                throw new UnsupportedOperationException(
                        "This method does not support wildcard cache config names: '"
                                + cacheName + "'");
            }
            CacheConfiguration<?, ?> prevConfig = cacheConfigs
                    .putIfAbsent(cacheName, config);
            if (prevConfig != null) {
                throw new IllegalStateException(
                        "Attempted to add multiple cache configs for "
                                + cacheName);
            }

            if (ignite != null) {
                ignite.getOrCreateCache(config);
            }

            logger.info("Added cache config: " + config);
            return cacheName;
        } finally {
            lock.readLock().unlock();
        }
    }

    private void throwIfUninitialized() {
        if (ignite == null) {
            throw new IllegalStateException(
                    "Ignite client has not been initialized");
        }
    }

    private void internalInitIgnite() {
        ignite = Ignition.start(configGenerator.getNewConfig());
        for (CacheConfiguration<?, ?> config : cacheConfigs.values()) {
            ignite.getOrCreateCache(config);
        }
    }
}
