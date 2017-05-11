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
import java.io.OutputStream;

import com.raytheon.uf.common.util.rate.TokenBucket;

/**
 * Output stream that limits throughput using a bytes-per-second value, or a
 * specified interval and number of bytes allowed to write over the interval.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 17, 2016 5937       tgurney     Initial creation
 * May 15, 2017 5937       tgurney     Add support for weighting
 *
 * </pre>
 *
 * @author tgurney
 */

public class RateLimitingOutputStream extends OutputStream {

    private static final int DEFAULT_INTERVAL_MS = 1000;

    /** Stream to write data to. */
    private final OutputStream wrappedStream;

    private final TokenBucket bucket;

    private final double weight;

    private int minWriteSize;

    /**
     * Wraps the provided {@code OutputStream} limiting the number of bytes
     * written per second.
     *
     * @param bytesPerSecond
     *            Maximum rate of writing data to the wrapped stream
     */
    public RateLimitingOutputStream(OutputStream outputStream,
            int bytesPerSecond) {
        this(outputStream,
                new TokenBucket(bytesPerSecond, DEFAULT_INTERVAL_MS));
    }

    /**
     * Wraps the provided {@code OutputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes written over the
     * specified interval. (one token = one byte). As a consumer from the token
     * bucket, this stream will have a relative weight of 1.0. (see class
     * Javadoc for {@code TokenBucket})
     *
     * @param outputStream
     * @param tokenBucket
     */
    public RateLimitingOutputStream(OutputStream outputStream,
            TokenBucket bucket) {
        this(outputStream, bucket, 1.0);
    }

    /**
     * Wraps the provided {@code OutputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes written over the
     * specified interval. (one token = one byte).
     *
     * @param outputStream
     * @param tokenBucket
     * @param weight
     *            A positive value specifying the weight of this stream relative
     *            to other consumers taking from the same token bucket. The
     *            relative weights of all concurrent consumers are compared to
     *            determine how much throughput each consumer is allowed. Higher
     *            number = greater proportion of the maximum throughput.
     */
    public RateLimitingOutputStream(OutputStream outputStream,
            TokenBucket bucket, double weight) {
        if (weight <= 0) {
            throw new IllegalArgumentException(
                    "weight must be greater than 0 (given: " + weight + ")");
        }
        this.wrappedStream = outputStream;
        this.bucket = bucket;
        this.weight = weight;
        this.minWriteSize = Math.max(bucket.getCapacity() / 16, 1);
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        int bytesLeft = len;
        int ind = off;
        while (bytesLeft > 0) {
            int writeSize;
            try {
                writeSize = bucket.consumeBetween(
                        Math.min(bytesLeft, minWriteSize), bytesLeft, weight);
            } catch (InterruptedException e) {
                throw new IOException(e);
            }
            wrappedStream.write(b, ind, writeSize);
            bytesLeft -= writeSize;
            ind += writeSize;
        }
    }

    @Override
    public void flush() throws IOException {
        wrappedStream.flush();
    }

    /**
     * Closes the underlying stream. Nothing in this stream needs to be closed
     * as long as the wrappedStream is closed.
     */
    @Override
    public void close() throws IOException {
        wrappedStream.close();
    }

    @Override
    public void write(int b) throws IOException {
        try {
            bucket.consume(1, weight);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        wrappedStream.write(b);
    }

    /**
     * @return Minimum size of each write to the wrapped stream, in bytes. A
     *         single call to write() will result in multiple writes to the
     *         wrapped stream that are each no smaller than this, except for the
     *         last write, which may be as small as 1 byte.
     */
    public int getMinWriteSize() {
        return minWriteSize;
    }

    public void setMinWriteSize(int minWriteSize) {
        if (minWriteSize < 1) {
            throw new IllegalArgumentException(
                    "minWriteSize must be at least 1 (given: " + minWriteSize
                            + ")");
        }
        this.minWriteSize = minWriteSize;
    }
}
