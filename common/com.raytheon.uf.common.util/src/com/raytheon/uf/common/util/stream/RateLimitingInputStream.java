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
package com.raytheon.uf.common.util.stream;

import java.io.IOException;
import java.io.InputStream;

import com.raytheon.uf.common.util.rate.TokenBucket;

/**
 * Input stream that limits throughput using a bytes-per-second value, or a
 * specified interval and number of bytes allowed to read over the interval.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 22, 2016 5937       tgurney     Initial creation
 * May 15, 2017 5937       tgurney     Add support for weighting
 *
 * </pre>
 *
 * @author tgurney
 */

public class RateLimitingInputStream extends InputStream {

    private static final int DEFAULT_INTERVAL_MS = 1000;

    /** Stream to get data from. */
    private final InputStream wrappedStream;

    private final TokenBucket bucket;

    private final double weight;

    private boolean eof = false;

    /**
     * Wraps the provided {@code InputStream} limiting the number of bytes read
     * per second.
     *
     * @param bytesPerSecond
     *            Maximum rate of reading data from the wrapped stream
     */
    public RateLimitingInputStream(InputStream inputStream,
            int bytesPerSecond) {
        this(inputStream, new TokenBucket(bytesPerSecond, DEFAULT_INTERVAL_MS));
    }

    /**
     * Wraps the provided {@code InputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes read over the specified
     * interval. (one token = one byte). As a consumer from the token bucket,
     * this stream will have a relative weight of 1.0. (see class Javadoc for
     * {@code TokenBucket})
     *
     * @param inputStream
     * @param tokenBucket
     */
    public RateLimitingInputStream(InputStream inputStream,
            TokenBucket bucket) {
        this(inputStream, bucket, 1.0);
    }

    /**
     * Wraps the provided {@code InputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes read over the specified
     * interval. (one token = one byte).
     *
     * @param inputStream
     * @param tokenBucket
     * @param weight
     *            A positive value specifying the weight of this stream relative
     *            to other consumers taking from the same token bucket. The
     *            relative weights of all concurrent consumers are compared to
     *            determine how much throughput each consumer is allowed. Higher
     *            number = greater proportion of the maximum throughput.
     */
    public RateLimitingInputStream(InputStream inputStream, TokenBucket bucket,
            double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException(
                    "weight must be greater than 0 (given: " + weight + ")");
        }
        this.wrappedStream = inputStream;
        this.bucket = bucket;
        this.weight = weight;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (eof) {
            return -1;
        }
        if (len == 0) {
            return 0;
        }
        int actualReadSize = wrappedStream.read(b, off, len);
        if (actualReadSize == -1) {
            eof = true;
        } else {
            try {
                bucket.consume(actualReadSize, weight);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return actualReadSize;
    }

    @Override
    public long skip(long n) throws IOException {
        if (eof) {
            return -1;
        }
        if (n <= 0) {
            return 0;
        }
        int actualSkipSize = (int) wrappedStream.skip(n);
        if (actualSkipSize == -1) {
            eof = true;
        } else {
            try {
                bucket.consume(actualSkipSize, weight);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return actualSkipSize;
    }

    /**
     * NOTE: read() may still block regardless of the return value of this
     * method, due to throughput limitation.
     *
     * <br>
     * <br>
     *
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return wrappedStream.available();
    }

    @Override
    public void close() throws IOException {
        wrappedStream.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        wrappedStream.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        wrappedStream.reset();
    }

    @Override
    public boolean markSupported() {
        return wrappedStream.markSupported();
    }

    @Override
    public int read() throws IOException {
        int rval = wrappedStream.read();
        if (rval == -1) {
            eof = true;
        } else {
            try {
                bucket.consume(1, weight);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        return rval;
    }
}
