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
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.slf4j.Logger;

import com.raytheon.uf.common.datastorage.StorageException;

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
 * Jun 21, 2022 8879       mapeters    Add allowRetries param to do*IgniteOp()
 *
 * </pre>
 *
 * @author mapeters
 */
public abstract class AbstractIgniteManager implements Serializable {

    private static final long serialVersionUID = 1L;

    protected Logger logger;

    /**
     * Initialize the managed ignite instance.
     */
    public abstract void initialize();

    /**
     * Get an object for accessing a particular ignite cache.
     *
     * @param cacheName
     *            the name of the cache
     * @return the ignite cache accessor
     */
    public <K, V> IgniteCacheAccessor<K, V> getCacheAccessor(String cacheName) {
        return new IgniteCacheAccessor<>(this, cacheName);
    }

    /**
     * Execute the given ignite operation and return its result. This retries
     * the operation if it throws an exception.
     *
     * @param <T>
     *            the ignite operation return value type
     * @param igniteOpFunction
     *            a function that performs the ignite operation when applied to
     *            the managed ignite instance and returns the operation result
     * @param allowRetries
     *            whether or not to allow retrying this operation if it fails
     * @return the ignite operation result
     * @throws StorageException
     *             if the operation still fails after retrying
     */
    public <T> T doIgniteOp(Function<Ignite, T> igniteOpFunction,
            boolean allowRetries) throws StorageException {
        int maxAttempts = allowRetries ? IgniteUtils.OP_NUM_ATTEMPTS : 1;

        Exception exception = null;
        for (int i = 0; i < maxAttempts; ++i) {
            try {
                return igniteOpFunction.apply(getIgnite());
            } catch (Exception e) {
                exception = e;
                IgniteUtils.handleException(logger, e, i, maxAttempts, null);
            }
        }

        throw new StorageException("Ignite operation failed to complete", null,
                exception);
    }

    /**
     * Execute the given void ignite operation. This retries the operation if it
     * throws an exception.
     *
     * @param igniteOpConsumer
     *            a consumer that performs the ignite operation when applied to
     *            the managed ignite instance
     * @param allowRetries
     *            whether or not to allow retrying this operation if it fails
     * @throws StorageException
     *             if the operation still fails after retrying
     */
    public void doVoidIgniteOp(Consumer<Ignite> igniteOpConsumer,
            boolean allowRetries) throws StorageException {
        doIgniteOp(ignite -> {
            igniteOpConsumer.accept(ignite);
            return null;
        }, allowRetries);
    }

    protected void setLogger(Logger logger) {
        this.logger = logger;
    }

    /**
     * Get the managed ignite instance.
     *
     * NOTE: This should only be called within {@link #doIgniteOp},
     * {@link #doVoidIgniteOp}, or within subclass methods that know that they
     * don't need the special handling of those 2 methods. Everything else
     * should go through those 2 methods.
     *
     * @return the managed ignite instance
     */
    protected abstract Ignite getIgnite();

    /**
     * Get the cache with the given name within the managed ignite instance.
     *
     * NOTE: This should only be called from within {@link IgniteCacheAccessor},
     * everything else should use {@link #getCacheAccessor(String)} to go
     * through there.
     *
     * @param <K>
     *            cache key type
     * @param <V>
     *            cache value type
     * @param cacheName
     *            the name of the cache
     * @return the ignite cache
     */
    protected abstract <K, V> IgniteCache<K, V> getCache(String cacheName);
}
