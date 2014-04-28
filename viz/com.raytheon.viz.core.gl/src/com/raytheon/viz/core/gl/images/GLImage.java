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

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

import javax.media.jai.PlanarImage;
import javax.media.opengl.GL;

import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.internal.cache.IImageCacheable;
import com.raytheon.viz.core.gl.internal.cache.ImageCache;
import com.raytheon.viz.core.gl.internal.cache.ImageCache.CacheType;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureCoords;
import com.sun.opengl.util.texture.TextureData;
import com.sun.opengl.util.texture.TextureIO;

/**
 * Represents a GL "RenderedImage"
 * 
 * <pre>
 * 
 *       SOFTWARE HISTORY
 *      
 *       Date         Ticket#      Engineer    Description
 *       ------------ ----------   ----------  --------------------------
 *       7/1/06                    chammack    Initial Creation.
 * 
 * </pre>
 * 
 * @author chammack
 * 
 */
public class GLImage extends AbstractGLImage implements IImageCacheable {

    /** The memory resident texture */
    private TextureData theStagedData;

    /** The card resident texture */
    private Texture theTexture;

    /** The rendered image representation of the image */
    protected RenderedImage theImage;

    private IRenderedImageCallback imagePreparer;

    protected int size;

    public GLImage(IRenderedImageCallback preparer,
            Class<? extends IImagingExtension> extensionClass) {
        super(extensionClass);
        theTexture = null;
        this.imagePreparer = preparer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.drawables.IImage#getImage()
     */
    public RenderedImage getImage() {
        return theImage;
    }

    /*
     * Return the size in bytes
     */
    public int getSize() {
        return size;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.drawables.IImage#dispose()
     */
    public void dispose() {
        super.dispose();
        ImageCache.getInstance(CacheType.MEMORY).remove(this);
        ImageCache.getInstance(CacheType.TEXTURE).remove(this);
    }

    public void disposeTexture() {
        synchronized (this) {
            if (theTexture == null) {
                return;
            }

            if (getStatus() == Status.LOADED) {
                if (theTexture != null) {
                    theTexture.dispose();
                    theTexture = null;
                }
                if (theStagedData != null) {
                    setStatus(Status.STAGED);
                } else {
                    setStatus(Status.UNLOADED);
                }
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.images.AbstractGLImage#stageTexture()
     */
    @Override
    public boolean stageTexture() throws VizException {
        if (theImage == null) {
            theImage = imagePreparer.getImage();
        }
        boolean rval = generateTextureData(theImage);
        // Add to memory cache
        ImageCache.getInstance(CacheType.MEMORY).put(this);
        return rval;
    }

    /**
     * Load a staged texture into video memory
     * 
     * @param ctx
     *            the OpenGL context
     * @throws VizException
     */
    public void loadTexture(GL gl) throws VizException {
        synchronized (this) {
            Texture tex = TextureIO.newTexture(theStagedData);

            theTexture = tex;

            tex.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
            tex.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);
            // tex.setTexParameteri(GL.GL_TEXTURE_MIN_FILTER, GL.GL_NICEST);
            // tex.setTexParameteri(GL.GL_TEXTURE_MAG_FILTER, GL.GL_NICEST);
            tex.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
            tex.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
            // tex.setTexParameteri(GL.GL_TEXTURE_WRAP_S, GL.GL_REPEAT);
            // tex.setTexParameteri(GL.GL_TEXTURE_WRAP_T, GL.GL_REPEAT);

            setStatus(Status.LOADED);

            ImageCache.getInstance(CacheType.TEXTURE).put(this);

        }
    }

    /**
     * Initialize a texture from a rendered
     * 
     * <P>
     * Recommended BufferedImage types are TYPE_INT_RGB and TYPE_4BYTE_ABGR (if
     * transparency is necessary)
     * 
     * @param rendImg
     *            the rendered image to load
     */
    private boolean generateTextureData(RenderedImage rendImg) {
        if (rendImg == null) {
            return false;
        }

        if (rendImg instanceof BufferedImage) {
            theStagedData = TextureIO.newTextureData((BufferedImage) rendImg,
                    false);
        } else if (rendImg instanceof PlanarImage) {
            theStagedData = TextureIO.newTextureData(
                    ((PlanarImage) rendImg).getAsBufferedImage(), false);
        } else {
            // convert to buf img
            theStagedData = TextureIO.newTextureData(
                    fromRenderedToBuffered(rendImg), false);
        }

        this.size = rendImg.getHeight() * rendImg.getWidth() * 4;
        return true;
    }

    /**
     * Get the texture that represents this image
     * 
     * This should not be called externally.
     * 
     * @return the texture
     */
    public Texture getTexture() {
        if (theTexture != null) {
            ImageCache.getInstance(CacheType.TEXTURE).put(this);
        }
        return theTexture;
    }

    /**
     * Dispose the texture data
     * 
     * This should not be called directly
     * 
     */
    public void disposeTextureData() {
        synchronized (this) {
            if (theStagedData != null) {
                theStagedData.flush();
            }
            theStagedData = null; // allow gc
            this.theImage = null;
            if (getStatus() == Status.STAGED) {
                setStatus(Status.UNLOADED);
            }
            ImageCache.getInstance(CacheType.MEMORY).remove(this);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.drawables.IImage#getHeight()
     */
    public int getHeight() {
        if (theImage != null) {
            return theImage.getHeight();
        }
        if (theTexture != null) {
            return theTexture.getImageHeight();
        }

        return -1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.drawables.IImage#getWidth()
     */
    public int getWidth() {
        if (theImage != null) {
            return theImage.getWidth();
        }
        if (theTexture != null) {
            return theTexture.getImageWidth();
        }

        return -1;
    }

    private BufferedImage fromRenderedToBuffered(RenderedImage img) {
        ColorModel cm = img.getColorModel();
        int w = img.getWidth();
        int h = img.getHeight();
        WritableRaster raster = cm.createCompatibleWritableRaster(w, h);
        boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                props.put(keys[i], img.getProperty(keys[i]));
            }
        }
        BufferedImage ret = new BufferedImage(cm, raster, isAlphaPremultiplied,
                props);
        img.copyData(raster);
        return ret;
    }

    /**
     * The texture type
     * 
     * @return the texture type id
     */
    public int getTextureStorageType() {
        return theTexture.getTarget();
    }

    public int getTextureid() {
        ImageCache.getInstance(CacheType.TEXTURE).put(this);

        return getTexture().getTextureObject();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.images.AbstractGLImage#getTextureCoords()
     */
    @Override
    public TextureCoords getTextureCoords() {
        return theTexture.getImageTexCoords();
    }

}
