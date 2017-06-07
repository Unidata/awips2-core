/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract DG133W-05-CQ-1067 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     6825 Pine Street, Suite 340
 *                         Mail Stop B8
 *                         Omaha, NE 68106
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.util.rate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe token bucket. Fills with tokens at a specified rate, and
 * guarantees that concurrent consumers will be allowed to take tokens in FIFO
 * order. Useful for rate limiting.
 *
 * An optional weight may be specified when consuming tokens, to give a
 * different relative priority to different consumers--the higher the weight,
 * the more tokens a consumer may take before having to wait again. The weight
 * is only meaningful in its magnitude relative to other weights--that is, you
 * can multiply the weight of every consumer by a constant value and the
 * behavior will be the same.
 *
 * IMPORTANT NOTE ON WEIGHTING: Weighting only occurs when there is direct
 * contention between threads. In the case of e.g. a large bucket with a large
 * interval and several consumers that take small amounts at a time, there will
 * be little to no contention, and as a result the tokens will be distributed
 * equally across all threads regardless of their specified weight. This
 * behavior can be corrected by using a small interval (say, tens of ms). The
 * smaller the interval, the greater the effect weighting has on the
 * distribution of tokens.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 22, 2016 5937       tgurney     Initial creation
 * May 11, 2017 6223       tgurney     Add fairness and prioritization
 * Jun 07, 2017 6222       tgurney     Add DEFAULT_WEIGHT
 *
 * </pre>
 *
 * @author tgurney
 */

public class TokenBucket {

    public static final double DEFAULT_WEIGHT = 1.0;

    private final int tokensPerInterval;

    private final long intervalMs;

    /*
     * volatile not necessary on this field since it is only accessed by the
     * holder of tokensAvailableLock
     */
    private long lastRefreshTs = 0;

    private double tokensAvailable = 0;

    /**
     * Must acquire this lock before accessing tokensAvailable field
     */
    private Lock tokensAvailableLock = new ReentrantLock(true);

    /**
     * Threads that are waiting to consume tokens, and the relative weight of
     * each thread
     */
    private Map<Thread, Double> consumers = Collections
            .synchronizedMap(new HashMap<>());

    /**
     * Create a new token bucket.
     *
     * @param tokensPerInterval
     *            Number of tokens to add per interval. This divided by
     *            intervalMs determines the rate at which tokens are added. This
     *            also is the maximum number of tokens.
     * @param intervalMs
     *            Length of the interval, in milliseconds.
     */
    public TokenBucket(int tokensPerInterval, long intervalMs) {
        if (tokensPerInterval < 1) {
            throw new IllegalArgumentException(
                    "tokens per interval must be at least 1 (given: "
                            + tokensPerInterval + ")");
        }
        if (intervalMs < 1) {
            throw new IllegalArgumentException(
                    "interval must be at least 1 millisecond (given: "
                            + intervalMs + ")");
        }
        this.tokensPerInterval = tokensPerInterval;
        this.intervalMs = intervalMs;
        tokensAvailable = 0;
        lastRefreshTs = System.currentTimeMillis();
    }

    /**
     * Create a new token bucket.
     *
     * @param tokensPerInterval
     *            Number of tokens to add per second. This is also the maximum
     *            number of tokens.
     */
    public TokenBucket(int tokensPerSecond) {
        this(tokensPerSecond, 1000);
    }

    /**
     * Consume n tokens. Will block for as long as necessary to consume the
     * whole n. This may be a long time if n is much larger than the bucket
     * capacity. The calling thread will have a weight of DEFAULT_WEIGHT.
     *
     * @return the number of tokens actually consumed (n)
     * @throws IllegalArgumentException
     *             If n is less than 0
     * @throws InterruptedException
     */
    public int consume(int n) throws InterruptedException {
        return consume(n, DEFAULT_WEIGHT);
    }

    /**
     * Consume n tokens. Will block for as long as necessary to consume the
     * whole n. This may be a long time if n is much larger than the bucket
     * capacity.
     *
     * @param n
     *            Number of tokens to consume
     * @param weight
     *            Weight of the current thread, relative to other consumer
     *            threads. Used to determine how many tokens each thread can
     *            take at once, before being forced to wait for other threads to
     *            take tokens.
     * @return the number of tokens actually consumed (n)
     * @throws IllegalArgumentException
     *             If n is less than 0, or weight not greater than 0
     * @throws InterruptedException
     */
    public int consume(int n, double weight) throws InterruptedException {
        return consumeBetween(n, n, weight);
    }

    /**
     * Consume at least {@code min} tokens, but no more than {@code max} tokens.
     * Either number may be larger than the bucket capacity. Note that a
     * {@code min} larger than the bucket capacity guarantees that this method
     * will block. The calling thread will have a weight of DEFAULT_WEIGHT.
     *
     * @param min
     * @param max
     * @return the number of tokens actually consumed.
     * @throws IllegalArgumentException
     *             If {@code min} is less than 0, or if {@code min} is larger
     *             than {@code max}.
     * @throws InterruptedException
     */
    public int consumeBetween(int min, int max) throws InterruptedException {
        return consumeBetween(min, max, DEFAULT_WEIGHT);
    }

    /**
     * Consume at least {@code min} tokens, but no more than {@code max} tokens,
     * blocking for as little time as possible. Either number may be larger than
     * the bucket capacity. Note that a {@code min} larger than the bucket
     * capacity guarantees that this method will block.
     *
     * @param min
     * @param max
     * @param weight
     *            Weight of the current thread, relative to other consumer
     *            threads. Used to determine how many tokens each thread can
     *            take at once, before being forced to wait for other threads to
     *            take tokens.
     * @return the number of tokens actually consumed.
     * @throws IllegalArgumentException
     *             If {@code min} is less than 0, or if {@code min} is larger
     *             than {@code max}, or weight is not greater than 0.
     * @throws InterruptedException
     */
    public int consumeBetween(int min, int max, double weight)
            throws InterruptedException {
        if (min < 0) {
            throw new IllegalArgumentException(
                    "minimum number of tokens must be at least 0 (given: " + min
                            + ")");
        }
        if (min > max) {
            throw new IllegalArgumentException("minimum number of tokens must "
                    + "not exceed maximum number of tokens (given min: " + min
                    + ", given max: " + max + ")");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException(
                    "weight must be greater than 0 (given: " + weight + ")");
        }
        Thread thread = Thread.currentThread();
        int tokensConsumed = 0;
        try {
            consumers.put(thread, weight);
            while (tokensConsumed < min) {
                tokensConsumed += consumeBetweenInternal(thread,
                        min - tokensConsumed, max - tokensConsumed);
            }
        } finally {
            consumers.remove(thread);
        }
        return tokensConsumed;
    }

    /**
     * Consume up to {@code max} tokens. Will attempt to consume at least
     * {@code min} tokens, but may consume fewer tokens if there are other
     * threads waiting. Uses the provided thread to determine what proportion of
     * available tokens we are allowed to consume at this instant. If there are
     * no other consumers then we can consume as many tokens as are available
     *
     * @param thread
     *            Thread that is consuming the tokens.
     * @param min
     *            Number of tokens to attempt to consume. Will wait for up to
     *            one full refill interval to achieve this number. May consume
     *            less than {@code min} if there are other consumers waiting or
     *            if {@code min} is larger than the bucket capacity
     * @param max
     *            Consume up to this many tokens if they are immediately
     *            available.
     * @return Number of tokens actually consumed, a number between 0 and max
     *         inclusive
     * @throws InterruptedException
     */
    private int consumeBetweenInternal(Thread thread, int min, int max)
            throws InterruptedException {
        try {
            tokensAvailableLock.lock();
            int allowedConsumption = getAllowedConsumption(thread);
            waitUntilAvailable(Math.min(min, allowedConsumption));
            /*
             * It is necessary to call getAllowedConsumption() twice as more
             * consumers may have queued up during waitUntilAvailable()
             */
            allowedConsumption = getAllowedConsumption(thread);
            int actualConsumption = Math.min(allowedConsumption, max);
            if (actualConsumption > tokensAvailable) {
                actualConsumption = (int) Math.floor(tokensAvailable);
            }
            tokensAvailable -= actualConsumption;
            return actualConsumption;
        } finally {
            tokensAvailableLock.unlock();
        }
    }

    /**
     * Attempt to consume n tokens without blocking. Return 0 if no tokens were
     * consumed because blocking would have been required (either because other
     * threads were consuming or waiting to consume tokens, or because not
     * enough tokens were available).
     *
     * @param n
     * @return the number of tokens actually consumed (n or 0)
     * @throws IllegalArgumentException
     *             If n is less than 0 or larger than the maximum number of
     *             tokens
     */
    public int tryConsume(int n) {
        if (n > tokensPerInterval) {
            throw new IllegalArgumentException("Cannot consume " + n
                    + " tokens. Maximum allowed is " + tokensPerInterval);
        }
        if (n < 0) {
            throw new IllegalArgumentException(
                    "number of tokens must be at least 0 (given: " + n + ")");
        }
        try {
            /*
             * Need to use the timed tryLock to prevent stealing lock from a
             * waiting thread (untimed tryLock does not honor the fairness
             * policy)
             */
            if (tokensAvailableLock.tryLock(0, TimeUnit.SECONDS)) {
                try {
                    refresh();
                    if (tokensAvailable < n) {
                        return 0;
                    }
                    tokensAvailable -= n;
                    return n;
                } finally {
                    tokensAvailableLock.unlock();
                }
            }
        } catch (InterruptedException e) {
            // ignore
        }
        return 0;
    }

    /**
     * Return the capacity of this bucket, i.e., the maximum number of tokens it
     * can hold.
     */
    public int getCapacity() {
        return tokensPerInterval;
    }

    /**
     * Add tokens to the bucket based on elapsed time since last refresh. Must
     * acquire tokensAvailableLock before calling this method
     */
    private void refresh() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastRefreshTs;
        lastRefreshTs = now;
        if (timePassed >= intervalMs) {
            tokensAvailable = tokensPerInterval;
        } else {
            tokensAvailable += (double) timePassed / (double) intervalMs
                    * tokensPerInterval;
            if (tokensAvailable > tokensPerInterval) {
                tokensAvailable = tokensPerInterval;
            }
        }
    }

    /**
     * @param consumer
     * @return The number of tokens consumer can take before letting another
     *         thread have a turn. If consumer thread has not "gotten in line"
     *         yet (by adding itself to consumers map) then this value is 0.
     *         Otherwise it will be a value between 1 and the bucket capacity,
     *         inclusive. The value will differ depending on how many other
     *         consumers are waiting, and the relative weight of each consumer.
     *         The value may be greater than the number of tokens currently in
     *         the bucket.
     */
    private int getAllowedConsumption(Thread consumer) {
        int rval = 0;
        /* Must synchronize on the map for iteration */
        synchronized (consumers) {
            if (!consumers.isEmpty()) {
                double weight = consumers.getOrDefault(consumer, (double) 0);
                if (weight != 0) {
                    double totalWeights = 0;
                    for (double value : consumers.values()) {
                        totalWeights += value;
                    }
                    double adjWeight = weight / totalWeights;
                    rval = Math.max(
                            (int) Math.floor(tokensPerInterval * adjWeight), 1);
                }
            }
        }
        return rval;
    }

    /**
     * Sleep until n tokens are available for consumption. Must acquire
     * tokensAvailableLock before calling this method
     *
     * @param n
     * @throws InterruptedException
     */
    private void waitUntilAvailable(int n) throws InterruptedException {
        refresh();
        if (tokensAvailable < n) {
            double tokensWaitingFor = n - tokensAvailable;
            long sleepTime = (long) Math
                    .ceil(intervalMs * tokensWaitingFor / tokensPerInterval);
            if (sleepTime > 0) {
                Thread.sleep(Math.min(sleepTime, intervalMs));
            }
            refresh();
        }
    }
}
