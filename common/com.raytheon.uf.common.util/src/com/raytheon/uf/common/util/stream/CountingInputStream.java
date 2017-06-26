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

/**
 * Stream that counts the number of bytes that have been read, and tracks the
 * time of the first and last read()
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 15, 2014 2928       rjpeter     Initial creation
 * Jun 22, 2017 6222       tgurney     Track first and last read time
 *
 * </pre>
 *
 * @author rjpeter
 */
public class CountingInputStream extends InputStream {
    private Long firstReadTimeMillis = null;

    private Long lastReadTimeMillis = null;

    /**
     * Stream to get data from.
     */
    protected final InputStream wrappedStream;

    /**
     * Number of bytes that have been read.
     */
    protected long bytesRead = 0;

    /**
     * Wraps the passed {@code InputStream} counting the bytes that are read
     * from it.
     *
     * @param inputStream
     */
    public CountingInputStream(InputStream inputStream) {
        this.wrappedStream = inputStream;
    }

    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            if (firstReadTimeMillis == null) {
                firstReadTimeMillis = System.currentTimeMillis();
            }
            int rval = wrappedStream.read(b, off, len);
            increaseBytesRead(rval);
            return rval;
        } finally {
            lastReadTimeMillis = System.currentTimeMillis();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        return wrappedStream.skip(n);
    }

    @Override
    public int available() throws IOException {
        return wrappedStream.available();
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
            if (firstReadTimeMillis == null) {
                firstReadTimeMillis = System.currentTimeMillis();
            }
            int rval = wrappedStream.read();
            increaseBytesRead(1);
            return rval;
        } finally {
            lastReadTimeMillis = System.currentTimeMillis();
        }
    }

    /**
     * Method that updates the internal count of the number of bytes read. Also
     * useful extension point for special handling based on amount of bytes
     * read.
     *
     * @param bytesRead
     * @throws IOException
     */
    public void increaseBytesRead(int bytesRead) throws IOException {
        this.bytesRead += bytesRead;
    }

    /**
     * Returns the bytes read so far.
     *
     * @return
     */
    public long getBytesRead() {
        return bytesRead;
    }

    /**
     * @return The time at which the first read from this stream began. null if
     *         this stream has never been read from
     */
    public Long getFirstReadTimeMillis() {
        return firstReadTimeMillis;
    }

    /**
     * @return The time at which the most recent read from this stream ended.
     *         null if this stream has never been read from
     */
    public Long getLastReadTimeMillis() {
        return lastReadTimeMillis;
    }
}
