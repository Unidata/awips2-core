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

import java.io.Serializable;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;

/**
 * Abstract manager for an ignite instance.
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
public abstract class AbstractIgniteManager implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Initialize the managed ignite instance.
     */
    public abstract void initialize();

    /**
     * @return the managed ignite instance
     */
    public abstract Ignite getIgnite();

    /**
     * Get the cache with the given name within the managed ignite instance.
     *
     * @param <K>
     *            cache key type
     * @param <V>
     *            cache value type
     * @param cacheName
     *            the name of the cache
     * @return the ignite cache
     */
    public abstract <K, V> IgniteCache<K, V> getCache(String cacheName);

    /**
     * Get a convenience class for accessing a particular ignite cache.
     *
     * @param cacheName
     *            the name of the cache
     * @return the ignite cache accessor
     */
    public <K, V> IgniteCacheAccessor<K, V> getCacheAccessor(String cacheName) {
        return new IgniteCacheAccessor<>(this, cacheName);
    }

    /**
     * A convenience class that provides a simplified way of accessing a
     * particular ignite cache by just calling {@link #getCache()}.
     */
    public static class IgniteCacheAccessor<K, V> {

        private final AbstractIgniteManager igniteManager;

        private final String cacheName;

        public IgniteCacheAccessor(AbstractIgniteManager igniteManager,
                String cacheName) {
            this.igniteManager = igniteManager;
            this.cacheName = cacheName;
        }

        public IgniteCache<K, V> getCache() {
            return igniteManager.getCache(cacheName);
        }
    }
}
