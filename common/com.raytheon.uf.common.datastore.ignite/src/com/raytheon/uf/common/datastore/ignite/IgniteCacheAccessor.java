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

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.StorageException;

/**
 * A wrapper class for accessing an ignite cache. All ignite cache access should
 * go through here so that timeouts, exceptions, and retries are handled
 * appropriately.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 15, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 * @param <K>
 *            the wrapped cache key type
 * @param <V>
 *            the wrapped cache value type
 */
public class IgniteCacheAccessor<K, V> {

    private final Logger logger;

    private final AbstractIgniteManager igniteManager;

    private final String cacheName;

    public IgniteCacheAccessor(AbstractIgniteManager igniteManager,
            String cacheName) {
        this.logger = LoggerFactory
                .getLogger(getClass().getSimpleName() + "-" + cacheName);
        this.igniteManager = igniteManager;
        this.cacheName = cacheName;
    }

    /**
     * Execute the given cache operation and return its result. This retries the
     * operation if it times out or throws an exception.
     *
     * @param <T>
     *            the cache operation return value type
     * @param cacheAsyncOpFunction
     *            a function that starts the async cache operation when applied
     *            to the cache and returns a future for it
     * @return the cache operation result
     * @throws StorageException
     *             if the operation still fails after retrying
     */
    public <T> T doAsyncCacheOp(
            Function<IgniteCache<K, V>, IgniteFuture<T>> cacheAsyncOpFunction)
            throws StorageException {
        Exception exception = null;
        for (int i = 0; i < IgniteUtils.OP_NUM_ATTEMPTS; ++i) {
            IgniteFuture<T> cacheOpFuture = null;
            try {
                cacheOpFuture = cacheAsyncOpFunction.apply(getCache());
                return cacheOpFuture.get(IgniteUtils.OP_TIMEOUT_SECS,
                        TimeUnit.SECONDS);
            } catch (Exception e) {
                exception = e;
                IgniteUtils.handleException(logger, e, i, cacheOpFuture);
            }
        }

        throw new StorageException("Ignite cache operation failed to complete",
                null, exception);
    }

    /**
     * Execute the given cache operation and return its result. This retries the
     * operation if it throws an exception.
     *
     * @param <T>
     *            the cache operation return value type
     * @param cacheSyncOpFunction
     *            a function that starts the synchronous cache operation when
     *            applied to the cache and returns the operation result
     * @return the cache operation result
     * @throws StorageException
     *             if the operation still fails after retrying
     */
    public <T> T doSyncCacheOp(
            Function<IgniteCache<K, V>, T> cacheSyncOpFunction)
            throws StorageException {
        Exception exception = null;
        for (int i = 0; i < IgniteUtils.OP_NUM_ATTEMPTS; ++i) {
            try {
                return cacheSyncOpFunction.apply(getCache());
            } catch (Exception e) {
                exception = e;
                IgniteUtils.handleException(logger, e, i, null);
            }
        }

        throw new StorageException("Ignite cache operation failed to complete",
                null, exception);
    }

    /**
     * @return the name of the cache that this provides access to
     */
    public String getCacheName() {
        return cacheName;
    }

    private IgniteCache<K, V> getCache() {
        return igniteManager.getCache(cacheName);
    }
}