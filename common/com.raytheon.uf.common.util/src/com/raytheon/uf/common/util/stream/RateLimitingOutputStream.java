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
 *
 * </pre>
 *
 * @author tgurney
 */

public class RateLimitingOutputStream extends OutputStream {

    private static final int DEFAULT_INTERVAL_MS = 1000;

    /** Stream to write data to. */
    private final OutputStream wrappedStream;

    private TokenBucket bucket;

    private int chunkSize;

    /**
     * Wraps the provided {@code OutputStream} limiting the number of bytes
     * written per second.
     *
     * @param bytesPerSecond
     *            Maximum rate of writing data to the wrapped stream
     */
    public RateLimitingOutputStream(OutputStream outputStream,
            int bytesPerSecond) {
        if (bytesPerSecond < 1) {
            throw new IllegalArgumentException(
                    "bytes per second must be at least 1 (given: "
                            + bytesPerSecond + ")");
        }
        this.wrappedStream = outputStream;
        this.bucket = new TokenBucket(bytesPerSecond, DEFAULT_INTERVAL_MS);
        this.chunkSize = Math.max(bucket.getCapacity() / 16, 1);
    }

    /**
     * Wraps the provided {@code OutputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes written over the
     * specified interval. (one token = one byte).
     *
     * @param outputStream
     * @param tokenBucket
     */
    public RateLimitingOutputStream(OutputStream outputStream,
            TokenBucket bucket) {
        this.wrappedStream = outputStream;
        this.bucket = bucket;
        this.chunkSize = Math.max(bucket.getCapacity() / 16, 1);
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
                        Math.min(bytesLeft, chunkSize), bytesLeft);
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
            bucket.consume(1);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        wrappedStream.write(b);
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException(
                    "chunkSize must be at least 1 (given: " + chunkSize + ")");
        }
        this.chunkSize = chunkSize;
    }

}
