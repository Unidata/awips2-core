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
import org.apache.ignite.configuration.IgniteConfiguration;
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

    private final Logger logger;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private final Map<String, CacheConfiguration<?, ?>> cacheConfigs = new HashMap<>();

    private final IIgniteConfigGenerator configGenerator;

    private Ignite ignite;

    /**
     * Constructor. The managed ignite instance is not actually started up until
     * {@link #initialize()} is called.
     *
     * @param configGenerator
     *            generates ignite config instances to use when starting up the
     *            managed ignite instance
     */
    public IgniteClientManager(IIgniteConfigGenerator configGenerator,
            int clusterNum) {
        this.configGenerator = configGenerator;
        this.logger = LoggerFactory
                .getLogger(getClass().getSimpleName() + "-" + clusterNum);
    }

    @Override
    public void initialize() {
        lock.writeLock().lock();
        try {
            internalInitIgnite();
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
            CacheConfiguration<?, ?> config = cacheConfigs.get(cacheName);
            if (config == null) {
                throw new IllegalArgumentException(
                        "Cache does not exist: " + cacheName);
            }
            IgniteCache<K, V> cache = (IgniteCache<K, V>) ignite
                    .getOrCreateCache(config);
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

            internalInitIgnite();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Add a cache configuration. This method should only be called from the
     * cluster manager.
     *
     * @param config
     *            the cache config
     */
    public void addCache(CacheConfiguration<?, ?> config) {
        lock.writeLock().lock();
        try {
            String cacheName = config.getName();
            if (cacheName.contains(IgniteUtils.WILDCARD_CACHE_NAME)) {
                throw new UnsupportedOperationException(
                        "Cache names cannot contain wildcard: " + cacheName);
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
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void throwIfUninitialized() {
        if (ignite == null) {
            throw new IllegalStateException(
                    "Ignite client has not been initialized");
        }
    }

    private void internalInitIgnite() {
        logger.info("Starting ignite client...");

        IgniteConfiguration igniteConfig = configGenerator.getNewConfig();
        ignite = Ignition.start(igniteConfig);
        for (CacheConfiguration<?, ?> config : cacheConfigs.values()) {
            ignite.getOrCreateCache(config);
        }

        ((IgniteClientFailureHandler) igniteConfig.getFailureHandler())
                .addListener(this);

        logger.info("Successfully started ignite client '"
                + igniteConfig.getIgniteInstanceName() + "'");
    }
}
