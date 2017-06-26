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

/**
 * Stream that counts the number of bytes that have been written, and tracks the
 * time of the first and last write()
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 15, 2014 2928       rjpeter     Initial creation
 * Jun 22, 2017 6222       tgurney     Track first and last write time
 *
 * </pre>
 *
 * @author rjpeter
 */
public class CountingOutputStream extends OutputStream {

    private Long firstWriteTimeMillis = null;

    private Long lastWriteTimeMillis = null;

    /**
     * Stream to write data to.
     */
    protected final OutputStream wrappedStream;

    /**
     * Number of bytes that have been written.
     */
    protected long bytesWritten = 0;

    /**
     * Wraps the passed {@code OutputStream} counting the bytes that are written
     * to it.
     *
     * @param outputStream
     */
    public CountingOutputStream(OutputStream outputStream) {
        this.wrappedStream = outputStream;
    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            if (firstWriteTimeMillis == null) {
                firstWriteTimeMillis = System.currentTimeMillis();
            }
            wrappedStream.write(b, off, len);
            increaseBytesWritten(len);
        } finally {
            lastWriteTimeMillis = System.currentTimeMillis();
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
            if (firstWriteTimeMillis == null) {
                firstWriteTimeMillis = System.currentTimeMillis();
            }
            wrappedStream.write(b);
            increaseBytesWritten(1);
        } finally {
            lastWriteTimeMillis = System.currentTimeMillis();
        }
    }

    /**
     * Method that updates the internal count of the number of bytes written.
     * Also useful extension point for special handling based on amount of bytes
     * written.
     *
     * @param bytesRead
     * @throws IOException
     */
    public void increaseBytesWritten(int bytesWritten) throws IOException {
        this.bytesWritten += bytesWritten;
    }

    /**
     * Returns the bytes written so far.
     *
     * @return
     */
    public long getBytesWritten() {
        return bytesWritten;
    }

    /**
     * @return The time at which the first write to this stream began. null if
     *         this stream has never been read from
     */
    public Long getFirstWriteTimeMillis() {
        return firstWriteTimeMillis;
    }

    /**
     * @return The time at which the most recent write to this stream ended.
     *         null if this stream has never been written to
     */
    public Long getLastWriteTimeMillis() {
        return lastWriteTimeMillis;
    }
}
