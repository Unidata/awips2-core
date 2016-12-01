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
 *
 * </pre>
 *
 * @author tgurney
 */

public class RateLimitingInputStream extends InputStream {

    private static final int DEFAULT_INTERVAL_MS = 1000;

    /** Stream to get data from. */
    private final InputStream wrappedStream;

    private TokenBucket bucket;

    private int chunkSize;

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
        if (bytesPerSecond < 1) {
            throw new IllegalArgumentException(
                    "bytes per interval must be at least 1 (given: "
                            + bytesPerSecond + ")");
        }
        this.wrappedStream = inputStream;
        this.bucket = new TokenBucket(bytesPerSecond, DEFAULT_INTERVAL_MS);
        this.chunkSize = Math.max(bucket.getCapacity() / 16, 1);
    }

    /**
     * Wraps the provided {@code InputStream} and uses the provided
     * {@code TokenBucket} to limit the number of bytes read over the specified
     * interval. (one token = one byte).
     *
     * @param inputStream
     * @param tokenBucket
     */
    public RateLimitingInputStream(InputStream inputStream,
            TokenBucket bucket) {
        this.wrappedStream = inputStream;
        this.bucket = bucket;
        this.chunkSize = Math.max(bucket.getCapacity() / 16, 1);
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
        int readSize;
        try {
            readSize = bucket.consumeBetween(Math.min(len, chunkSize), len);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        int actualReadSize = wrappedStream.read(b, off, readSize);
        if (actualReadSize == -1) {
            eof = true;
        }
        return actualReadSize;
    }

    /**
     * NOTE: Skipping is rate limited in the same way as reading--you need to
     * perform it in a loop to account for partial skips.
     *
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        if (eof) {
            return -1;
        }
        if (n <= 0) {
            return 0;
        }
        int skipSize;
        try {
            skipSize = bucket.consumeBetween(Math.min((int) n, chunkSize),
                    (int) n);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        int actualSkipSize = (int) wrappedStream.skip(skipSize);
        if (actualSkipSize == -1) {
            eof = true;
        }
        return actualSkipSize;
    }

    @Override
    public int available() throws IOException {
        /*
         * read() will block if there are not enough tokens in the bucket, and
         * we have no way of knowing this without actually trying to read.
         */
        return 0;
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
        try {
            bucket.consume(1);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
        int rval = wrappedStream.read();
        if (rval == -1) {
            eof = true;
        }
        return rval;
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
