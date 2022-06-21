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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.ignite.IgniteCacheRestartingException;
import org.apache.ignite.IgniteClientDisconnectedException;
import org.apache.ignite.IgniteException;
import org.apache.ignite.cluster.ClusterTopologyException;
import org.apache.ignite.lang.IgniteFuture;
import org.slf4j.Logger;

import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;
import com.raytheon.uf.common.datastorage.records.RecordAndMetadata;

/**
 * Contains various constants and utilities used with ignite on both the client
 * and server side.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 08, 2021 8450       mapeters    Initial creation
 * Jun 21, 2022 8879       mapeters    Change OP_NUM_ATTEMPTS to int, add
 *                                     maxAttempts param to handleException()
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteUtils {

    public static final String PLUGIN_REGISTRY_CACHE_NAME = "data-store-cache-name-map";

    public static final String DEFAULT_CACHE = "defaultDataStore";

    public static final String WILDCARD_CACHE_NAME = "*";

    public static final String NO_CACHE_NAME = "none";

    public static final String IGNITE_CLUSTER_1_SERVERS = "IGNITE_CLUSTER_1_SERVERS";

    public static final String IGNITE_CLUSTER_2_SERVERS = "IGNITE_CLUSTER_2_SERVERS";

    public static final int OP_NUM_ATTEMPTS = getIntegerProperty(
            "ignite.op.num.attempts");

    public static final long OP_TIMEOUT_SECS = getLongProperty(
            "ignite.op.timeout.secs");

    private static final long EXCEPTION_RECOVERY_TIMEOUT_SECS = getLongProperty(
            "ignite.op.exception.recovery.timeout.secs");

    private static final long OP_RETRY_DELAY_SECS = getLongProperty(
            "ignite.op.retry.delay.secs");

    /**
     * Prevent instantiation.
     */
    private IgniteUtils() {
    }

    /**
     * Get the long integer value of the given property.
     *
     * @param name
     *            the property name
     * @return the long value
     * @throws IllegalStateException
     *             if the property is not set to a valid long value
     */
    public static long getLongProperty(String name) {
        Long value = Long.getLong(name);
        if (value == null) {
            throw new IllegalStateException(
                    "Invalid long integer value for property " + name + ": '"
                            + System.getProperty(name) + "'");
        }

        return value;
    }

    /**
     * Get the integer value of the given property.
     *
     * @param name
     *            the property name
     * @return the integer value
     * @throws IllegalStateException
     *             if the property is not set to a valid integer value
     */
    public static int getIntegerProperty(String name) {
        Integer value = Integer.getInteger(name);
        if (value == null) {
            throw new IllegalStateException(
                    "Invalid integer value for property " + name + ": '"
                            + System.getProperty(name) + "'");
        }

        return value;
    }

    /**
     * @return true if ignite is active/being used, false if only pypies is
     *         being used
     */
    public static boolean isIgniteActive() {
        return "ignite".equals(System.getenv("DATASTORE_PROVIDER"));
    }

    /**
     * Handle an exception performing an ignite operation.
     *
     * @param logger
     *            the logger to use
     * @param e
     *            the exception
     * @param attemptNum
     *            the operation attempt number (0-based)
     * @param maxAttempts
     *            the maximum number of times to attempt the operation (1-based)
     * @param asyncCacheOpFuture
     *            the async cache op future that the exception occurred during,
     *            or null if it was not such an operation
     */
    public static void handleException(Logger logger, Exception e,
            int attemptNum, int maxAttempts,
            IgniteFuture<?> asyncCacheOpFuture) {
        logger.error("Error executing ignite operation on attempt "
                + (attemptNum + 1) + "/" + maxAttempts, e);

        /*
         * Some ignite exceptions provide a future that is supposed to complete
         * when the cause of the exception is resolved. Check for that and wait
         * for it to complete/recover if available.
         */
        IgniteFuture<?> recoveryFuture = getExceptionRecoveryFuture(e);
        boolean recovered = false;
        if (recoveryFuture != null) {
            logger.info("Attempting to wait up to "
                    + EXCEPTION_RECOVERY_TIMEOUT_SECS
                    + "s for exception to tell us that its cause has been resolved: "
                    + recoveryFuture);
            try {
                recoveryFuture.get(EXCEPTION_RECOVERY_TIMEOUT_SECS,
                        TimeUnit.SECONDS);
                recovered = true;
                logger.info("Recovered from " + e.getClass().getSimpleName());
            } catch (IgniteException e2) {
                logger.error(
                        "Error recovering from " + e.getClass().getSimpleName(),
                        e2);
            }
        }

        if (asyncCacheOpFuture != null) {
            logger.info("Cancelling failed async cache operation: "
                    + asyncCacheOpFuture);
            try {
                asyncCacheOpFuture.cancel();
                logger.info("Cancelled failed async cache operation");
            } catch (IgniteException e2) {
                logger.warn("Error cancelling ignite future", e2);
            }
        }

        if (!recovered && attemptNum < maxAttempts - 1) {
            // Give ignite time to hopefully fix itself
            try {
                logger.info("Waiting " + OP_RETRY_DELAY_SECS
                        + "s before retrying the operation");
                TimeUnit.SECONDS.sleep(OP_RETRY_DELAY_SECS);
            } catch (InterruptedException e2) {
                logger.error("Interrupted while delaying retry", e);
            }
        }
    }

    /**
     * Some ignite exceptions provide an {@link IgniteFuture} that is supposed
     * to complete when the cause of the exception is resolved. Extract the
     * recovery future from the given exception if it has one, otherwise return
     * null.
     *
     * @param e
     *            the exception to check for a recovery future
     * @return the recovery future, or null
     */
    private static IgniteFuture<?> getExceptionRecoveryFuture(Exception e) {
        if (e instanceof IgniteClientDisconnectedException) {
            return ((IgniteClientDisconnectedException) e).reconnectFuture();
        }
        if (e instanceof IgniteCacheRestartingException) {
            return ((IgniteCacheRestartingException) e).restartFuture();
        }
        if (e instanceof ClusterTopologyException) {
            return ((ClusterTopologyException) e).retryReadyFuture();
        }

        return null;
    }

    /**
     * Update the metadata identifiers of the given value.
     *
     * @param value
     *            the value to add metadata to (modified by this method)
     * @param metadataToAdd
     *            the metadata to add to the value
     */
    public static void updateMetadata(DataStoreValue value,
            List<RecordAndMetadata> metadataToAdd) {
        Map<String, Set<IMetadataIdentifier>> metadataToAddByName = new HashMap<>();
        for (RecordAndMetadata rm : metadataToAdd) {
            String recordName = rm.getRecord().getName();
            metadataToAddByName
                    .computeIfAbsent(recordName, name -> new HashSet<>())
                    .addAll(rm.getMetadata());
        }

        for (RecordAndMetadata recordAndMetadata : value
                .getRecordsAndMetadata()) {
            String name = recordAndMetadata.getRecord().getName();
            Set<IMetadataIdentifier> metadata = metadataToAddByName.get(name);
            if (metadata != null) {
                metadata.forEach(
                        metaId -> recordAndMetadata.addMetadata(metaId));
            }
        }
    }
}
