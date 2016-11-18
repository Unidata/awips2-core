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

/**
 * Standard token bucket algorithm. Fills with tokens at a specified rate.
 * Useful for rate limiting. This class is thread-safe.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 22, 2016 5937       tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public class TokenBucket {

    private final int tokensPerInterval;

    private final long intervalMs;

    private double tokensAvailable = 0;

    private long lastCheckTs = 0;

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
        lastCheckTs = System.currentTimeMillis();
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

    /** Add tokens to the bucket based on elapsed time since last refresh */
    private void refresh() {
        long now = System.currentTimeMillis();
        long timePassed = now - lastCheckTs;
        lastCheckTs = now;
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
     * Block until n tokens are available, then consume n tokens.
     *
     * @return the number of tokens actually consumed (n)
     * @throws IllegalArgumentException
     *             If n is larger than the maximum number of tokens
     * @throws InterruptedException
     */
    public synchronized int consume(int n) throws InterruptedException {
        if (n > tokensPerInterval) {
            throw new IllegalArgumentException("Cannot consume " + n
                    + " tokens. Maximum allowed is " + tokensPerInterval);
        }
        if (n < 0) {
            throw new IllegalArgumentException(
                    "number of tokens must be at least 0 (given: " + n + ")");
        }
        refresh();
        if (tokensAvailable < n) {
            double tokensNeeded = n - tokensAvailable;
            long sleepTime = (long) Math
                    .ceil(intervalMs * tokensNeeded / tokensPerInterval);
            if (sleepTime > 0) {
                Thread.sleep(Math.min(sleepTime, intervalMs));
            }
            refresh();
        }
        tokensAvailable -= n;
        return n;
    }

    /**
     * Block until at least {@code min} tokens are available. Then consume at
     * least {@code min} tokens, but no more than {@code max} tokens.
     * {@code max} may be larger than the bucket capacity, but the number of
     * tokens actually consumed will not exceed the bucket capacity.
     *
     * @param min
     * @param max
     * @return the number of tokens actually consumed.
     * @throws IllegalArgumentException
     *             If {@code min} is less than 0 or exceeds the bucket capacity,
     *             or if {@code min} is larger than {@code max}.
     * @throws InterruptedException
     */
    public synchronized int consumeBetween(int min, int max)
            throws InterruptedException {
        if (min > tokensPerInterval) {
            throw new IllegalArgumentException("Cannot consume " + min
                    + " tokens. Maximum allowed is " + tokensPerInterval);
        }
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
        consume(min);
        refresh();
        int extra = (int) Math.min(max - min, Math.floor(tokensAvailable));
        consume(extra);
        return min + extra;
    }

    /**
     * Attempt to consume n tokens. If less than n tokens are available, consume
     * no tokens.
     *
     * @param n
     * @return the number of tokens actually consumed (n or 0)
     * @throws IllegalArgumentException
     *             If n is larger than the maximum number of tokens
     */
    public synchronized int tryConsume(int n) {
        if (n > tokensPerInterval) {
            throw new IllegalArgumentException("Cannot consume " + n
                    + " tokens. Maximum allowed is " + tokensPerInterval);
        }
        if (n < 0) {
            throw new IllegalArgumentException(
                    "number of tokens must be at least 1 (given: " + n + ")");
        }
        refresh();
        if (tokensAvailable < n) {
            return 0;
        }
        tokensAvailable -= n;
        return n;
    }

    /**
     * Return the capacity of this bucket, i.e., the maximum number of tokens it
     * can hold.
     */
    public int getCapacity() {
        return tokensPerInterval;
    }

}
