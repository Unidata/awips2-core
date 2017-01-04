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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Output stream backed by a byte array that is borrowed from a pool
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Oct 30, 2015  4710     bclement  Initial creation
 * Jan 04, 2017  6041     bsteffen  Allow close() to be called more than once
 * 
 * </pre>
 * 
 * @author bclement
 */
public class PooledByteArrayOutputStream extends OutputStream {

    private final ByteArrayOutputStreamPool pool;

    private ResizeableByteArrayOutputStream _delegate;

    private boolean autoReturn = true;

    protected PooledByteArrayOutputStream(ByteArrayOutputStreamPool pool,
            ResizeableByteArrayOutputStream delegate) {
        this.pool = pool;
        this._delegate = delegate;
    }

    private ResizeableByteArrayOutputStream getDelegate() {
        if (_delegate == null) {
            throw new IllegalStateException(
                    "Attempted to used PooledByteArrayOutputStream after stream had been returned to pool");
        }
        return _delegate;
    }

    public void returnToPool() {
        if (_delegate != null) {
            pool.returnToPool(_delegate);
            _delegate = null;
        }
    }

    /**
     * @see com.raytheon.uf.common.util.ResizeableByteArrayOutputStream#getCapacity()
     */
    public int getCapacity() {
        return getDelegate().getCapacity();
    }

    /**
     * @see com.raytheon.uf.common.util.ResizeableByteArrayOutputStream#setCapacity(int)
     */
    public void setCapacity(int length) {
        getDelegate().setCapacity(length);
    }

    /**
     * @see com.raytheon.uf.common.util.ResizeableByteArrayOutputStream#getUnderlyingArray()
     */
    public byte[] getUnderlyingArray() {
        return getDelegate().getUnderlyingArray();
    }

    @Override
    public void write(byte[] b) throws IOException {
        getDelegate().write(b);
    }

    /**
     * @see com.raytheon.uf.common.util.ResizeableByteArrayOutputStream#setCount(int)
     */
    public void setCount(int count) {
        getDelegate().setCount(count);
    }

    @Override
    public void write(int b) {
        getDelegate().write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        getDelegate().write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        getDelegate().flush();
    }

    /**
     * @see java.io.ByteArrayOutputStream#reset()
     */
    public void reset() {
        getDelegate().reset();
    }

    /**
     * @see java.io.ByteArrayOutputStream#size()
     */
    public int size() {
        return getDelegate().size();
    }

    @Override
    public void close() throws IOException {
        if (_delegate != null) {
            _delegate.close();
        }
        if (autoReturn) {
            returnToPool();
        }
    }

    /**
     * @see java.io.ByteArrayOutputStream#toByteArray()
     */
    public byte[] toByteArray() {
        return getDelegate().toByteArray();
    }

    public boolean isAutoReturn() {
        return autoReturn;
    }

    /**
     * If true, backing memory is returned to pool on close. Otherwise,
     * {@link #returnToPool()} must be called to return memory to pool
     * 
     * @param autoReturn
     *            the autoReturn to set
     */
    public void setAutoReturn(boolean autoReturn) {
        this.autoReturn = autoReturn;
    }

}
