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

import java.awt.Rectangle;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 * Utility for creating and managing nio Buffers
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 01, 2010            mschenke    Initial creation
 * Apr 07, 2014 2968       njensen     Added asReadOnly() and duplicate()
 * May 21, 2015 4495       njensen     Deprecated methods that are OBE
 * Apr 06, 2016 5450       nabowle     Add replaceValue and toIntBuffer.
 *
 * </pre>
 *
 * @author mschenke
 * @version 1.0
 */

public class BufferUtil {

    /**
     * @deprecated this is no longer necessary thanks to
     *             GLColorMapDataFormatFactory
     * @param datasetBounds
     * @return a padded width
     */
    @Deprecated
    public static int wordAlignedByteWidth(Rectangle datasetBounds) {
        int paddedWidth = datasetBounds.width;

        int width = paddedWidth % 4;
        if (width != 0) {
            paddedWidth += 4 - width;
        }
        return paddedWidth;
    }

    /**
     * @deprecated this is no longer necessary thanks to
     *             GLColorMapDataFormatFactory
     * @param datasetBounds
     * @return a padded width
     */
    @Deprecated
    public static int wordAlignedShortWidth(Rectangle datasetBounds) {
        int paddedWidth = datasetBounds.width;

        if ((paddedWidth & 1) == 1) {
            paddedWidth++;
        }
        return paddedWidth;
    }

    /**
     * @deprecated this is no longer necessary thanks to
     *             GLColorMapDataFormatFactory
     * @param byteData
     * @param datasetBounds
     * @param bytesPerPixel
     * @return a direct byte buffer of the data
     */
    @Deprecated
    public static ByteBuffer wrapDirect(byte[] byteData,
            Rectangle datasetBounds, int bytesPerPixel) {
        int paddedWidth = wordAlignedByteWidth(new Rectangle(datasetBounds.x,
                datasetBounds.y, datasetBounds.width * bytesPerPixel,
                datasetBounds.height * bytesPerPixel));
        Rectangle actualBounds = new Rectangle(datasetBounds.x,
                datasetBounds.y, bytesPerPixel * datasetBounds.width,
                datasetBounds.height);

        ByteBuffer bb = createByteBuffer(actualBounds.height * paddedWidth,
                true);
        int diff = paddedWidth - actualBounds.width;

        int szX = actualBounds.x + actualBounds.width;
        int szY = actualBounds.y + actualBounds.height;
        int k = 0;
        for (int j = actualBounds.y; j < szY; j++) {
            for (int i = actualBounds.x; i < szX; i++) {
                bb.put(byteData[k++]);
            }

            for (int i = 0; i < diff; i++) {
                bb.put((byte) 0);
            }
        }

        return bb;
    }

    /**
     * @deprecated this is no longer necessary thanks to
     *             GLColorMapDataFormatFactory
     * @param byteData
     * @param datasetBounds
     * @return a direct byte buffer of the data
     */
    @Deprecated
    public static ByteBuffer wrapDirect(byte[] byteData, Rectangle datasetBounds) {
        return wrapDirect(byteData, datasetBounds, 1);
    }

    /**
     * @deprecated this is no longer necessary thanks to
     *             GLColorMapDataFormatFactory
     * @param shortBuffer
     * @param datasetBounds
     * @return a direct byte buffer of the data
     */
    @Deprecated
    public static ShortBuffer wrapDirect(short[] shortBuffer,
            Rectangle datasetBounds) {

        int paddedWidth = wordAlignedShortWidth(datasetBounds);

        ByteBuffer bb = ByteBuffer.allocateDirect(paddedWidth
                * datasetBounds.height * 2);
        bb.order(ByteOrder.nativeOrder());
        ShortBuffer sb = bb.asShortBuffer();

        int szX = datasetBounds.x + datasetBounds.width;
        int szY = datasetBounds.y + datasetBounds.height;
        int k = 0;
        for (int j = datasetBounds.y; j < szY; j++) {
            for (int i = datasetBounds.x; i < szX; i++) {
                sb.put(shortBuffer[k++]);
            }

            if (paddedWidth != datasetBounds.width) {
                sb.put((short) 0);
            }
        }

        return sb;
    }

    public static ShortBuffer directBuffer(ShortBuffer sb) {
        if (sb.isDirect()) {
            return sb;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(sb.capacity() * 2);
        bb.rewind();
        ShortBuffer directSb = bb.asShortBuffer();
        directSb.rewind();
        sb.rewind();
        directSb.put(sb);

        directSb.rewind();
        return directSb;
    }

    public static IntBuffer directBuffer(IntBuffer ib) {
        if (ib.isDirect()) {
            return ib;
        }

        ByteBuffer bb = ByteBuffer.allocateDirect(ib.capacity() * 4);
        bb.order(ByteOrder.nativeOrder());
        bb.rewind();
        IntBuffer directIb = bb.asIntBuffer();
        directIb.rewind();
        ib.rewind();
        directIb.put(ib);
        directIb.rewind();
        return directIb;
    }

    public static ByteBuffer directBuffer(ByteBuffer ib) {
        if (ib.isDirect()) {
            return ib;
        }

        ByteBuffer bbDirect = createByteBuffer(ib.capacity(), true);
        bbDirect.rewind();
        ib.rewind();
        bbDirect.put(ib);

        bbDirect.rewind();
        return bbDirect;
    }

    public static synchronized ByteBuffer createByteBuffer(int capacity,
            boolean direct) {
        if (direct) {
            return ByteBuffer.allocateDirect(capacity);
        } else {
            return ByteBuffer.allocate(capacity);
        }
    }

    /**
     * Returns a new read-only view into a buffer
     *
     * @param buffer
     * @return a read-only view of the same buffer
     */
    public static Buffer asReadOnly(Buffer buffer) {
        Buffer ret = null;
        if (buffer instanceof ByteBuffer) {
            ret = ((ByteBuffer) buffer).asReadOnlyBuffer();
        } else if (buffer instanceof ShortBuffer) {
            ret = ((ShortBuffer) buffer).asReadOnlyBuffer();
        } else if (buffer instanceof IntBuffer) {
            ret = ((IntBuffer) buffer).asReadOnlyBuffer();
        } else if (buffer instanceof LongBuffer) {
            ret = ((LongBuffer) buffer).asReadOnlyBuffer();
        } else if (buffer instanceof FloatBuffer) {
            ret = ((FloatBuffer) buffer).asReadOnlyBuffer();
        } else if (buffer instanceof DoubleBuffer) {
            ret = ((DoubleBuffer) buffer).asReadOnlyBuffer();
        } else {
            throw new IllegalArgumentException(
                    "Cannot create read only buffer of type "
                            + buffer.getClass().getName());
        }

        return ret;
    }

    /**
     * Creates a new view into the buffer with an independent position and mark.
     *
     * @param buffer
     * @return an independent view into the same buffer
     */
    public static Buffer duplicate(Buffer buffer) {
        Buffer ret = null;
        if (buffer instanceof ByteBuffer) {
            ret = ((ByteBuffer) buffer).duplicate();
        } else if (buffer instanceof ShortBuffer) {
            ret = ((ShortBuffer) buffer).duplicate();
        } else if (buffer instanceof IntBuffer) {
            ret = ((IntBuffer) buffer).duplicate();
        } else if (buffer instanceof LongBuffer) {
            ret = ((LongBuffer) buffer).duplicate();
        } else if (buffer instanceof FloatBuffer) {
            ret = ((FloatBuffer) buffer).duplicate();
        } else if (buffer instanceof DoubleBuffer) {
            ret = ((DoubleBuffer) buffer).duplicate();
        } else {
            throw new IllegalArgumentException(
                    "Cannot create duplicate buffer of type "
                            + buffer.getClass().getName());
        }

        return ret;
    }

    /**
     * Replaces the value in the buffer at the given index with the appropriate
     * numeric cast of fill. That is, if buffer is an IntBuffer, fill.intValue()
     * will be put into the buffer, which may truncate or otherwise change
     * fill's value.
     * 
     * @param buffer
     *            the Buffer to modify.
     * @param index
     *            the index that will be changed.
     * @param fill
     *            the value to put into the buffer at the specified index.
     */
    public static void replaceValue(Buffer buffer, int index, Number fill) {
        if (buffer instanceof DoubleBuffer) {
            DoubleBuffer buff = (DoubleBuffer) buffer;
            buff.put(index, fill.doubleValue());
        } else if (buffer instanceof FloatBuffer) {
            FloatBuffer buff = (FloatBuffer) buffer;
            buff.put(index, fill.floatValue());
        } else if (buffer instanceof LongBuffer) {
            LongBuffer buff = (LongBuffer) buffer;
            buff.put(index, fill.longValue());
        } else if (buffer instanceof IntBuffer) {
            IntBuffer buff = (IntBuffer) buffer;
            buff.put(index, fill.intValue());
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer buff = (ShortBuffer) buffer;
            buff.put(index, fill.shortValue());
        } else if (buffer instanceof ByteBuffer) {
            ByteBuffer buff = (ByteBuffer) buffer;
            buff.put(index, fill.byteValue());
        }
    }

    /**
     * Returns an IntBuffer version of buffer. If buffer is already an
     * IntBuffer, buffer is returned unmodified. Otherwise, a new IntBuffer is
     * allocated, and the data inside buffer is cast to an int and inserted into
     * the IntBuffer. If buffer cannot be converted into an IntBuffer, null is
     * returned.
     *
     * @param buffer
     *            The buffer.
     * @return If buffer is an IntBuffer already, buffer is returned. If buffer
     *         is a numeric-type buffer, a newly allocated IntBuffer with
     *         buffer's data cast to integers is returned. Otherwise, null is
     *         returned.
     */
    public static IntBuffer toIntBuffer(Buffer buffer) {
        if (buffer == null) {
            return null;
        }

        if (buffer instanceof IntBuffer) {
            return (IntBuffer) buffer;
        }

        IntBuffer intBuffer = IntBuffer.allocate(buffer.capacity());
        if (buffer instanceof DoubleBuffer) {
            DoubleBuffer dBuffer = (DoubleBuffer) buffer;
            for (int i = 0; i < dBuffer.capacity(); i++) {
                intBuffer.put(i, (int) dBuffer.get(i));
            }
        } else if (buffer instanceof FloatBuffer) {
            FloatBuffer fBuffer = (FloatBuffer) buffer;
            for (int i = 0; i < fBuffer.capacity(); i++) {
                intBuffer.put(i, (int) fBuffer.get(i));
            }
        } else if (buffer instanceof LongBuffer) {
            LongBuffer lBuffer = (LongBuffer) buffer;
            for (int i = 0; i < lBuffer.capacity(); i++) {
                intBuffer.put(i, (int) lBuffer.get(i));
            }
        } else if (buffer instanceof ShortBuffer) {
            ShortBuffer sBuffer = (ShortBuffer) buffer;
            for (int i = 0; i < sBuffer.capacity(); i++) {
                intBuffer.put(i, sBuffer.get(i));
            }
        } else if (buffer instanceof ByteBuffer) {
            ByteBuffer bBuffer = (ByteBuffer) buffer;
            for (int i = 0; i < bBuffer.capacity(); i++) {
                intBuffer.put(i, bBuffer.get(i));
            }
        } else {
            return null;
        }
        return intBuffer;
    }

}
