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
package com.raytheon.viz.core.gl.ext;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Stack;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension.IGraphicsExtensionInterface;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.IGLTarget;
import com.raytheon.viz.core.gl.dataformat.AbstractGLColorMapDataFormat;
import com.raytheon.viz.core.gl.dataformat.GLByteDataFormat;
import com.raytheon.viz.core.gl.dataformat.GLColorMapData;
import com.raytheon.viz.core.gl.dataformat.GLColorMapDataFormatFactory;
import com.raytheon.viz.core.gl.ext.imaging.GLColormappedImageExtension;
import com.raytheon.viz.core.gl.ext.imaging.GLDefaultImagingExtension;
import com.raytheon.viz.core.gl.images.AbstractGLImage;
import com.raytheon.viz.core.gl.images.GLImage;
import com.raytheon.viz.core.gl.images.GLOffscreenColormappedImage;
import com.raytheon.viz.core.gl.internal.GLView2D;

/**
 * 
 * Provides logic for creating offscreen images in GL so that data can be
 * rendered and saved for better efficiency. When rendering offscreen all render
 * events to the target are sent to an offscreen image, this image can later be
 * rendered onscreen.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 10, 2012            bsteffen     Initial creation
 * Mar 21, 2013 1806       bsteffen    Update GL mosaicing to use dynamic data
 *                                     format for offscreen textures.
 * Jan 26, 2015  3980      bsteffen    Add system property to disable luminance.
 * Jan 18, 2023			srcarter@ucar  Bring over MJ changes for GL2
 * Oct 28, 2026         tiffanym@ucar  Preserve and restore the OpenGL viewport during offscreen rendering to fix RGB display scaling on macOS Retina displays
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class GLOffscreenRenderingExtension extends GraphicsExtension<IGLTarget>
        implements IGraphicsExtensionInterface {

	private static class ViewInfo {
	    private IView view;

	    private AbstractGLImage image;

	    private int[] viewport;

	    public ViewInfo(IView view, AbstractGLImage image, int[] viewport) {
	        this.view = view;
	        this.image = image;
	        this.viewport = viewport;
	    }
	}

    private static boolean checkedLuminance = false;

    private static boolean supportsLuminance = !Boolean
            .getBoolean("gl.no.luminance");

    private Stack<ViewInfo> viewStack = new Stack<ViewInfo>();

    private ViewInfo currentInfo = null;

    /**
     * Begins offscreen rendering to the glImage. All rendering after this call
     * will occur on the image until {@link #endOffscreenRendering()} is called.
     * Render area will be set to the current view's extent
     * 
     * @param glImage
     * @throws VizException
     */
    public void beginOffscreenRendering(AbstractGLImage glImage)
            throws VizException {
        beginOffscreenRendering(glImage, target.getView().getExtent());
    }

    /**
     * Begins offscreen rendering to the glImage. All rendering after this call
     * will occur on the image until {@link #endOffscreenRendering()} is called.
     * Render area will be set to the offscreenExtent provided
     * 
     * @param glImage
     * @throws VizException
     */
    public void beginOffscreenRendering(AbstractGLImage glImage,
            IExtent offscreenExtent) throws VizException {
        if (glImage.getStatus() == IImage.Status.UNLOADED
                || glImage.getStatus() == IImage.Status.LOADING) {
            glImage.setStatus(IImage.Status.LOADING);
            glImage.stage();
        }

        if (glImage.getStatus() == IImage.Status.STAGED) {
            glImage.target(target);
        }

        IView view = new GLView2D(offscreenExtent);

        // Preserve the actual viewport in framebuffer pixels.
        // Target bounds may use logical dimensions on HiDPI displays.
        int[] viewport = new int[4];
        target.getGl().glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);

        if (currentInfo == null) {
            viewStack.push(new ViewInfo(target.getView(), null, viewport));
        } else {
            // Preserve the viewport for nested offscreen rendering too.
            viewStack.push(new ViewInfo(currentInfo.view,
                    currentInfo.image, viewport));
        }

        setCurrentView(new ViewInfo(view, glImage, null));
    }
    /**
     * Ends offscreen rendering. Should only be called if a call to
     * {@link #beginOffscreenRendering(AbstractGLImage)} or
     * {@link #beginOffscreenRendering(AbstractGLImage, IExtent)} was
     * successfully called first first
     * 
     * @throws VizException
     */
    public void endOffscreenRendering() throws VizException {
        if (viewStack.isEmpty() == false) {
            setCurrentView(viewStack.pop());
        }
    }

    private void setCurrentView(ViewInfo current) throws VizException {
        currentInfo = current;

        if (current.image == null) {
            currentInfo = null;
            target.getGl().glBindFramebuffer(GL.GL_FRAMEBUFFER, 0);
        } else {
            current.image.usaAsFrameBuffer();
        }

        target.setView(current.view);

        if (current.viewport != null) {
            // Restore the exact viewport saved before the offscreen pass.
            target.getGl().glViewport(
                    current.viewport[0],
                    current.viewport[1],
                    current.viewport[2],
                    current.viewport[3]);
        } else {
            // A new offscreen pass uses the texture's pixel dimensions.
            target.getGl().glViewport(0, 0,
                    current.image.getWidth(),
                    current.image.getHeight());
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.drawables.ext.IGraphicsExtension#
     * getCompatibilityValue(com.raytheon.uf.viz.core.IGraphicsTarget)
     */
    @Override
    public int getCompatibilityValue(IGLTarget target) {
        return Compatibilty.TARGET_COMPATIBLE;
    }

    /**
     * Creates an RGB based image for use in offscreen rendering
     * 
     * @param dimensions
     * @return
     * @throws VizException
     */
    public AbstractGLImage constructOffscreenImage(final int[] dimensions)
            throws VizException {
        return new GLImage(new IRenderedImageCallback() {
            @Override
            public RenderedImage getImage() throws VizException {
                return new BufferedImage(dimensions[0], dimensions[1],
                        BufferedImage.TYPE_INT_RGB);
            }
        }, GLDefaultImagingExtension.class);
    }

    /**
     * Creates a colormapped offscreen image with the specified dataType and
     * dimensions
     * 
     * @param dataType
     * @param dimensions
     * @return
     * @throws VizException
     */
    public GLOffscreenColormappedImage constructOffscreenImage(
            ColorMapDataType dataType, int[] dimensions) throws VizException {
        return constructOffscreenImage(dataType, dimensions, null);
    }

    /**
     * Creates a colormapped offscreen image with the specified dataType and
     * dimensions and default colormap parameters
     * 
     * @param dataType
     * @param dimensions
     * @param parameters
     * @return
     * @throws VizException
     */
    public GLOffscreenColormappedImage constructOffscreenImage(
            final ColorMapDataType dataType, final int[] dimensions,
            ColorMapParameters parameters) throws VizException {
        AbstractGLColorMapDataFormat format = null;

        if (supportsLuminance) {
            format = GLColorMapDataFormatFactory
                    .getGLColorMapDataFormat(dataType);
        } else {
            format = new NoLuminanceDataFormat(dataType);
        }

        GLOffscreenColormappedImage image = new GLOffscreenColormappedImage(
                new GLColorMapData(format, dataType, dimensions), parameters,
                GLColormappedImageExtension.class);

        if (!checkedLuminance) {
            checkedLuminance = true;
            try {
                beginOffscreenRendering(image);
            } catch (VizException e) {
                // Log this so it is easy to see in the console logs.
                new VizException(
                        "Graphics card does not support luminance textures.", e)
                        .printStackTrace(System.out);
                // assume we don't support luminance
                supportsLuminance = false;
                // Reconstruct image
                image = constructOffscreenImage(dataType, dimensions,
                        parameters);
            } finally {
                endOffscreenRendering();
            }
        }

        return image;
    }

    private static final class NoLuminanceDataFormat extends GLByteDataFormat {

        // Used to get the original min/max which makes signed bytes work and
        // theoretically will give better looking results for other integer data
        // types.
        private final AbstractGLColorMapDataFormat originalFormat;

        private NoLuminanceDataFormat(ColorMapDataType originalType) {
            this.originalFormat = GLColorMapDataFormatFactory
                    .getGLColorMapDataFormat(originalType);
        }

        @Override
        public int getTextureInternalFormat() {
            return GL2.GL_RGB8;
        }

        @Override
        public int getTextureFormat() {
            return GL2.GL_RGB;
        }

        @Override
        public int getValuesPerPixel() {
            return 3;
        }

        @Override
        public double getDataFormatMin() {
            return originalFormat.getDataFormatMin();
        }

        @Override
        public double getDataFormatMax() {
            return originalFormat.getDataFormatMax();
        }

    }

}
