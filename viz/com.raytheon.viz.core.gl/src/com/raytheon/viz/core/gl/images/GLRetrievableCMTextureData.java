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
package com.raytheon.viz.core.gl.images;

import java.nio.Buffer;
import java.util.HashMap;
import java.util.Map;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.gl2.GLUgl2;

import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.viz.core.data.IColorMapDataRetrievalCallback;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.GLContextBridge;
import com.raytheon.viz.core.gl.dataformat.GLBufferColorMapData;
import com.raytheon.viz.core.gl.dataformat.IGLColorMapDataFormatProvider;
import com.raytheon.viz.core.gl.internal.cache.IImageCacheable;
import com.raytheon.viz.core.gl.internal.cache.ImageCache;
import com.raytheon.viz.core.gl.internal.cache.ImageCache.CacheType;

/**
 * {@link GLCMTextureData} that's backed by a {@link Buffer} that represents a
 * colormapped texture that is cacheable and can be unloaded and reloaded from
 * main/graphics memory using a retrieval callback
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------------------------------
 * Jun 24, 2013           mschenke  Initial creation
 * Oct 23, 2013  2492     mschenke  Extracted Buffer backing into super class
 * Jan 21, 2020  73572    tjensen   Add sizeManagement arg to disposeTexture
 * Jan 18, 2023		  srcarter@ucar Bring in MJ changes for GL2
 *
 * </pre>
 *
 * @author mschenke
 */

public class GLRetrievableCMTextureData extends GLBufferCMTextureData
        implements IImageCacheable {

    private static Map<IColorMapDataRetrievalCallback, GLRetrievableCMTextureData> texMap = new HashMap<>();

    /**
     * Gets a {@link GLRetrievableCMTextureData} for the callback. These
     * TextureData objects can be shared among callbacks
     *
     * @param callback
     * @return
     */
    public static GLRetrievableCMTextureData getGlTextureId(
            IColorMapDataRetrievalCallback callback) {
        synchronized (texMap) {
            GLRetrievableCMTextureData data = texMap.get(callback);
            if (data == null) {
                data = new GLRetrievableCMTextureData(callback);
                texMap.put(callback, data);
            }
            data.use();
            return data;
        }
    }

    private final IColorMapDataRetrievalCallback callback;

    private int refCount = 0;

    /**
     * Private constructor, access only allowed through
     * {@link #getGlTextureId(IColorMapDataRetrievalCallback)}
     *
     * @param callback
     */
    private GLRetrievableCMTextureData(
            IColorMapDataRetrievalCallback callback) {
        super(null);
        this.callback = callback;
    }

    @Override
    public synchronized boolean stageTexture() throws VizException {
        // Don't need to stage if we are already in GL
        if (isLoaded()) {
            return true;
        }
        // Don't need to stage if we already have data locally
        if (isStaged()) {
            return true;
        }

        // OK, Fetch the data
        ColorMapData cmData = callback.getColorMapData();
        if (cmData != null) {
            IGLColorMapDataFormatProvider glDataFormatCallback = IGLColorMapDataFormatProvider.defaultCallback;
            if (callback instanceof IGLColorMapDataFormatProvider) {
                glDataFormatCallback = (IGLColorMapDataFormatProvider) callback;
            }
            this.data = new GLBufferColorMapData(cmData,
                    glDataFormatCallback.getGLColorMapDataFormat(cmData));
            if (isStaged()) {
                ImageCache.getInstance(CacheType.MEMORY).put(this);
                return true;
            }
        }
        // The data fetch didn't go well
        return false;
    }

    @Override
    public synchronized boolean loadTexture(GL2 gl) throws VizException {
        if (super.loadTexture(gl)) {
            ImageCache.getInstance(CacheType.TEXTURE).put(this);
            return true;
        }
        return false;
    }

    @Override
    public synchronized void disposeTexture(boolean sizeManagement) {
        // check if this should be restaged
        if (sizeManagement && callback.isRestagingEnabled()) {
            GLBufferColorMapData textureData = getDataObject();
            restageTexture(textureData);
        }
        super.disposeTexture();
        ImageCache.getInstance(CacheType.TEXTURE).remove(this);
    }

    @Override
    public void disposeTextureData() {
        if (isStaged()) {
            getDataObject().setData(null);
        }
        ImageCache.getInstance(CacheType.MEMORY).remove(this);
    }

    public void use() {
        refCount += 1;
    }

    @Override
    public void dispose() {
        synchronized (texMap) {
            refCount -= 1;
            if (refCount == 0) {
                texMap.remove(callback);
                ImageCache.getInstance(CacheType.TEXTURE).remove(this);
                ImageCache.getInstance(CacheType.MEMORY).remove(this);
            }
        }
    }

    @Override
    public int getTexId() {
        if (isLoaded()) {
            ImageCache.getInstance(CacheType.TEXTURE).put(this);
        }
        return super.getTexId();
    }

    @Override
    public int getSize() {
        GLBufferColorMapData data = getDataObject();
        if (data != null) {
            int[] dimensions = data.getDimensions();
            int totalSize = data.getBytesPerPixel();
            for (int dimension : dimensions) {
                totalSize *= dimension;
            }
            return totalSize;
        }
        return 0;
    }

    @Override
    public double getValue(int x, int y) {
        GLBufferColorMapData data = getDataObject();
        double value = Double.NaN;
        restageTexture(data);
        if (data != null) {
            value = data.getValue(x, y).doubleValue();
        }
        return value;
    }

    public void restageTexture(GLBufferColorMapData data) {
        if (!isStaged() && isLoaded()) {
            GLContextBridge.makeMasterContextCurrent();
            GL2 gl = GLUgl2.getCurrentGL().getGL2();
            int textureStorageType = getTextureStorageType();
            int copybackTextureType = data.getCopyBackTextureType();
            Buffer copybackBuffer = data.getCopybackBuffer();
            gl.glEnable(textureStorageType);
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            tex.bind(gl, textureStorageType);
            gl.glGetTexImage(textureStorageType, 0, getTextureFormat(),
                    copybackTextureType, copybackBuffer.rewind());
            gl.glActiveTexture(GL2.GL_TEXTURE0);
            gl.glBindTexture(textureStorageType, 0);
            gl.glDisable(textureStorageType);

            data.setTextureType(copybackTextureType);
            data.setData(copybackBuffer);
            GLContextBridge.releaseMasterContext();
        }
        if (data != null) {
            ImageCache.getInstance(CacheType.MEMORY).put(this);
        }
    }

}
