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
package com.raytheon.uf.common.util;

import java.util.concurrent.ArrayBlockingQueue;

/**
 * Pools ByteArrayOutputStream objects. The lookup of a ByteArrayOutputStream
 * will always return without blocking. Will instead discard excessive objects
 * on return to the pool. Stream objects will also be discarded if they exceed
 * the maximum size. Relies on stream being closed to return the stream to the
 * pool.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 06, 2010            rjpeter     Initial creation
 * Oct 30, 2015 4710       bclement    moved ByteArrayOutputStream subclass to ResizeableByteArrayOutputStream
 *                                      now returns PooledByteArrayOutputStream objects to protected pooled streams
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */
public class ByteArrayOutputStreamPool {
    private static final int MEGABYTE = 1024 * 1024;

    private static final ByteArrayOutputStreamPool instance = new ByteArrayOutputStreamPool();

    /** Pooled output streams for performance **/
    private ArrayBlockingQueue<ResizeableByteArrayOutputStream> streams;

    private int maxPoolSize = 8;

    private int initStreamSize = MEGABYTE;

    private int maxStreamSize = (int) (5.5 * MEGABYTE);

    public static ByteArrayOutputStreamPool getInstance() {
        return instance;
    }

    private ByteArrayOutputStreamPool() {
        streams = new ArrayBlockingQueue<>(maxPoolSize);
    }

    /**
     * Returns a ByteArrayOutputStream whose initial capacity is at least the
     * initStreamSize. Close must be called on the returned object to return the
     * stream to the pool.
     * 
     * @param minInitialSize
     * @return
     */
    public PooledByteArrayOutputStream getStream() {
        return getStream(initStreamSize);
    }

    /**
     * Returns a ByteArrayOutputStream whose initial capacity is at least the
     * minInitialSize. Close must be called on the returned object to return the
     * stream to the pool.
     * 
     * @param minInitialSize
     * @return
     */
    public PooledByteArrayOutputStream getStream(int minInitialSize) {
        ResizeableByteArrayOutputStream stream = streams.poll();
        if (stream == null) {
            stream = new ResizeableByteArrayOutputStream(minInitialSize);
        } else if (stream.getCapacity() < minInitialSize) {
            stream.setCapacity(minInitialSize);
        }

        return new PooledByteArrayOutputStream(this, stream);
    }

    public void setMaxPoolSize(int size) {
        if (size > 0) {
            ArrayBlockingQueue<ResizeableByteArrayOutputStream> tmp = new ArrayBlockingQueue<>(
                    size);
            streams.drainTo(tmp, size);
            streams = tmp;
        } else {
            // throw illegal arg exception
            streams = new ArrayBlockingQueue<>(1);
        }
    }

    /**
     * 
     * @param streamInitSize
     */
    public void setInitStreamSize(double initStreamSize) {
        this.initStreamSize = (int) (initStreamSize * MEGABYTE);
    }

    /**
     * 
     * @param streamMaxSize
     */
    public void setMaxStreamSize(double maxStreamSize) {
        this.maxStreamSize = (int) (maxStreamSize * MEGABYTE);
    }

    /**
     * Returns the stream to the pool if the stream is less than maxStreamSize
     * and if their is room in the pool.
     * 
     * @param stream
     */
    protected void returnToPool(ResizeableByteArrayOutputStream stream) {
        if (stream.getCapacity() <= maxStreamSize) {
            stream.reset();
            streams.offer(stream);
        }
    }

}
