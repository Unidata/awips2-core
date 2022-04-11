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
package com.raytheon.uf.common.datastore.ignite.processor;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.cache.integration.CacheLoader;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteLogger;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.ignite.resources.LoggerResource;

import com.raytheon.uf.common.datastorage.IDataStore.StoreOp;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastore.ignite.DataStoreKey;
import com.raytheon.uf.common.datastore.ignite.DataStoreValue;
import com.raytheon.uf.common.datastore.ignite.IgniteCacheAccessor;
import com.raytheon.uf.common.datastore.ignite.IgniteServerManager;

/**
 *
 * Alternative implementation of store that is optimized for the common case
 * where all of the data in a group is stored in a single operation.
 *
 * This should be ran via IgniteCompute.affinityCall(), so that it runs on the
 * correct ignite server node for the given cache/key combination.
 *
 * This implementation is not as good for more complex operations such as
 * append, detecting duplicates, and partial inserts (including inserting
 * different datasets in a group with multiple store operations). For these
 * operations the previous cache value is needed. (TODO: This may not actually
 * be true anymore. Before, requiring the previous cache value hurt performance
 * because this was running in the ignite client in edex, and pulling the
 * previous value to edex is more expensive than pulling it to the appropriate
 * ignite server node. That is not the case now that this runs in the
 * appropriate ignite server node. Leaving as is for now though.)
 *
 * For the complex operations described above the exact behavior of this method
 * is dependent on the value of
 * {@link CacheConfiguration#isLoadPreviousValue()}. When this setting is false
 * the previous value will only be used if it is already loaded in the cache,
 * this is much faster but may overwrite the previous value if it is not already
 * in the cache. When loadPreviousValue is true the previous value is loaded
 * with the {@link CacheLoader} which is slower but guarantees the existing data
 * will not be overwritten unless this is a REPLACE operation.
 *
 * Note that this class specifically is an {@link IgniteCallable}, because a
 * processor automatically tries to read through the value from the underlying
 * data store if it is not in the cache. That is unnecessary for the common case
 * and hurts performance.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2022 8608       mapeters    Initial creation (extracted and updated
 *                                     from IgniteDataStore.FastReplaceCallable)
 * Apr 08, 2022 8653       tjensen     Update replace to check h5s for prevValue
 *                                     and properly merge cached values
 *
 * </pre>
 *
 * @author mapeters
 */
public class FastStoreCallable implements IgniteCallable<StorageStatus> {

    private static final long serialVersionUID = 1L;

    private static final Map<DataStoreKey, Object> locks = new LinkedHashMap<DataStoreKey, Object>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(
                Map.Entry<DataStoreKey, Object> eldest) {
            return size() > 256;
        }
    };

    @LoggerResource
    private IgniteLogger logger;

    private IgniteCacheAccessor<DataStoreKey, DataStoreValue> cacheAccessor;

    private final String cacheName;

    private final DataStoreKey key;

    private DataStoreValue value;

    private final StoreOp storeOp;

    public FastStoreCallable(String cacheName, DataStoreKey key,
            DataStoreValue value, StoreOp storeOp) {
        this.cacheName = cacheName;
        this.key = key;
        this.value = value;
        this.storeOp = storeOp;
    }

    @IgniteInstanceResource
    public void setIgnite(Ignite ignite) {
        this.cacheAccessor = new IgniteServerManager(ignite)
                .getCacheAccessor(cacheName);
    }

    @Override
    public StorageStatus call() {
        StorageStatus status = new StorageStatus();
        Object lock = getLock(key);

        synchronized (lock) {
            try {
                DataStoreValue prevValue = null;
                try {
                    prevValue = cacheAccessor.doAsyncCacheOp(cache -> {
                        @SuppressWarnings("unchecked")
                        CacheConfiguration<?, ?> cacheConfig = cache
                                .getConfiguration(CacheConfiguration.class);
                        if (!cacheConfig.isLoadPreviousValue()) {
                            cache = cache.withSkipStore();
                        }
                        return cache.getAsync(key);
                    });
                } catch (StorageException e) {
                    throw new StorageException(
                            "Error loading previous cache value for: "
                                    + cacheName + ", " + key,
                            e.getRecord(), e);
                }

                if (prevValue != null) {
                    value = StoreProcessor.merge(
                            Arrays.asList(prevValue.getRecordsAndMetadata()),
                            Arrays.asList(value.getRecordsAndMetadata()),
                            storeOp, status);
                }

                cacheAccessor.doAsyncCacheOp(c -> c.putAsync(key, value));
            } catch (StorageException e) {
                /*
                 * Store and return exceptions instead of letting them just
                 * propagate up. Otherwise they get wrapped in various ignite
                 * exceptions, which makes it harder to differentiate exceptions
                 * (e.g. duplicate data vs failed data).
                 */
                status.setExceptions(new StorageException[] { e });
            }
        }

        return status;
    }

    private static Object getLock(DataStoreKey key) {
        synchronized (locks) {
            return locks.computeIfAbsent(key, k -> new Object());
        }
    }
}