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

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLProfile;

import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.internal.cache.IImageCacheable;
import com.raytheon.viz.core.gl.internal.cache.ImageCache;
import com.raytheon.viz.core.gl.internal.cache.ImageCache.CacheType;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;

/**
 * Represents a GL "RenderedImage"
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------------------------------
 * Jul 01, 2006           chammack  Initial Creation.
 * May 27, 2014  3196     bsteffen  Remove jai.
 * May 10, 2015           mjames@ucar Refactor for jogl 2.3.2
 * Dec 21, 2015			  mjames@ucar Mute theTexture.destroy
 * Jan 21, 2020  73572    tjensen   Add sizeManagement arg to disposeTexture
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

    private final IRenderedImageCallback imagePreparer;

    protected int size;

    public GLImage(IRenderedImageCallback preparer,
            Class<? extends IImagingExtension> extensionClass) {
        super(extensionClass);
        theTexture = null;
        this.imagePreparer = preparer;
    }

    public RenderedImage getImage() {
        return theImage;
    }

    /*
     * Return the size in bytes
     */
    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void dispose() {
        super.dispose();
        ImageCache.getInstance(CacheType.MEMORY).remove(this);
        ImageCache.getInstance(CacheType.TEXTURE).remove(this);
    }

    @Override
    public void disposeTexture(boolean sizeManagement) {
        synchronized (this) {
            if (theTexture == null) {
                return;
            }

            if (getStatus() == Status.LOADED) {
                if (theTexture != null) {
                    //theTexture.dispose();
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
    @Override
    public void loadTexture(GL2 gl) throws VizException {
        synchronized (this) {
            Texture tex = AWTTextureIO.newTexture(theStagedData);

            theTexture = tex;

            tex.setTexParameteri(gl, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
	        tex.setTexParameteri(gl, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
	        tex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
	        tex.setTexParameteri(gl, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);

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
        GLProfile glp = GLProfile.getDefault();
        if (rendImg instanceof BufferedImage) {
            theStagedData = AWTTextureIO.newTextureData(glp, (BufferedImage) rendImg,
                    false);
        } else {
            // convert to buf img
            theStagedData = AWTTextureIO
                    .newTextureData(glp, fromRenderedToBuffered(rendImg), false);
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
    @Override
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

    @Override
    public int getHeight() {
        if (theImage != null) {
            return theImage.getHeight();
        }
        if (theTexture != null) {
            return theTexture.getImageHeight();
        }

        return -1;
    }

    @Override
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
        Hashtable<String, Object> props = new Hashtable<>();
        String[] keys = img.getPropertyNames();

        if (keys != null) {
            for (String key : keys) {
                props.put(key, img.getProperty(key));
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
    @Override
    public int getTextureStorageType() {
        return theTexture.getTarget();
    }

    @Override
    public int getTextureid() {
        ImageCache.getInstance(CacheType.TEXTURE).put(this);

        return getTexture().getTextureObject();
    }

    @Override
    public TextureCoords getTextureCoords() {
        return theTexture.getImageTexCoords();
    }

}
