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

package com.raytheon.viz.core.gl.internal;

import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.opengl.GLCanvas;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.PlatformUI;
import org.geotools.coverage.grid.GeneralGridGeometry;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.colormap.image.ColorMapData;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.AbstractGraphicsTarget;
import com.raytheon.uf.viz.core.Activator;
import com.raytheon.uf.viz.core.DrawableCircle;
import com.raytheon.uf.viz.core.DrawableColorMap;
import com.raytheon.uf.viz.core.DrawableLine;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.ProgramArguments;
import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IFont.FontType;
import com.raytheon.uf.viz.core.drawables.IFont.Style;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension.IGraphicsExtensionInterface;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtensionManager;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.preferences.PreferenceConstants;
import com.raytheon.viz.core.gl.GLContextBridge;
import com.raytheon.viz.core.gl.GLDisposalManager;
import com.raytheon.viz.core.gl.GLStats;
import com.raytheon.viz.core.gl.IGLFont;
import com.raytheon.viz.core.gl.IGLTarget;
import com.raytheon.viz.core.gl.dataformat.GLByteDataFormat;
import com.raytheon.viz.core.gl.ext.GLStringRenderingExtension;
import com.raytheon.viz.core.gl.ext.imaging.GLDefaultImagingExtension;
import com.raytheon.viz.core.gl.glsl.GLSLFactory;
import com.raytheon.viz.core.gl.glsl.GLSLStructFactory;
import com.raytheon.viz.core.gl.glsl.GLShaderProgram;
import com.raytheon.viz.core.gl.images.GLBufferCMTextureData;
import com.raytheon.viz.core.gl.images.GLImage;
import com.raytheon.viz.core.gl.objects.GLTextureObject;
import com.sun.opengl.util.Screenshot;
import com.sun.opengl.util.j2d.TextRenderer;

/**
 * 
 * Implements the GL graphics target
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 *                
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Jul 01, 2006           chammack    Initial Creation.
 * Jul 24, 2007           njensen     Colormaps can reload in drawRaster().
 * Oct 01, 2007  467      brockwoo    Fix for disabling interpolation of plot
 *                                    data textures
 * Oct 16, 2007  468      njensen     drawString() supports rotation.
 * Mar 18, 2008           chammack    Improve legend rendering
 * Mar 12, 2009  2092     njensen     Added offscreen rendering support
 * Jul 01, 2010  6146     bkowal      The offset that is needed to set the 
 *                                    &quot;Y&quot; coordinate of the legend
 *                                    text that is drawn is now calculated based
 *                                    on the font size rather than being
 *                                    hard-coded.
 * Jul 08, 2010  6146     bkowal      The font size will now be adjusted
 *                                    automatically by comparing the current
 *                                    size of the pane of interest to the
 *                                    overall size of the screen.
 * Jul 19, 2010  5952     bkowal      GLTarget will now check for the existence
 *                                    of updated extents before drawing. A
 *                                    method has also been added to notify
 *                                    GLTarget of when there are updated extents
 *                                    to load.
 * Feb 14, 2013  1616     bsteffen    Add option for interpolation of colormap
 *                                    parameters, disable colormap interpolation
 *                                    by default.
 * Apr 18, 2013  1638     mschenke    Made string rendering always occur in
 *                                    canvas space so strings are always
 *                                    readable despite extent
 * May 28, 2013  1638     mschenke    Made sure {@link TextStyle#BLANKED} text
 *                                    is drawing correct size box around text
 * Nov 04, 2013  2492     mschenke    Switched colormap drawing to use 1D
 *                                    texture object for alpha mask
 * Mar 03, 2014  2804     mschenke    Added clipping pane field to only setup
 *                                    if changed
 * Apr 04, 2014  2920     bsteffen    Allow strings to use mulitple styles.
 * Apr 08, 2014  2950     bsteffen    Reduce oversized colormaps.
 * May 08, 2014  2920     bsteffen    Fix default color of BOXED text.
 * May 12, 2014  3074     bsteffen    Move string rendering to an extension.
 * Jun 17, 2014  2903     bclement    added PIPE to PointStyle
 * Jul 28, 2014  3397     bclement    deprecated createWireframeShape() that takes in spatialChopFlag
 *                                      removed drawWireFrameShapeInternal()
 * Aug 07, 2014  3492     mapeters    Replaced deprecated createWireframeShape() methods, deprecated
 *                                    calls now handled by {@link AbstractGraphicsTarget}
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 * 
 */
public class GLTarget extends AbstractGraphicsTarget implements IGLTarget {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(GLTarget.class);

    protected static String DEFAULT_FONT = "Monospace";

    protected static final int TICK_SIZE = 3;

    protected static final int maxColorMapCacheSize = com.raytheon.viz.core.gl.Activator
            .getDefault().getPreferenceStore().getInt("colorMapCacheSize");

    /** The gl context */
    protected final GL gl;

    /** The eclipse GL canvas, not used if drawing offscreen */
    protected final GLCanvas theCanvas;

    /** the GLContext */
    protected final GLContextBridge theContext;

    /** Has a texure load occurred during a draw operation */
    protected boolean hasLoadedTextureOnLoop = false;

    /** A refresh count used to handle refresh requests that occur during draws */
    protected int refreshCount = 0;

    public static final boolean FORCE_NO_SHADER = ProgramArguments
            .getInstance().getBoolean("-no_shader");

    /** The current visible extent */
    protected IView targetView;

    /** The width of the screen */
    protected final float theWidth;

    /** The height of the screen */
    protected final float theHeight;

    /** The current Zoom level */
    protected double theCurrentZoom;

    /** The GLU object */
    protected final GLU glu = new GLU();

    protected static final Map<String, GLTextureObject> loadedColorMaps = new LinkedHashMap<String, GLTextureObject>() {

        private static final long serialVersionUID = 1L;

        @Override
        protected boolean removeEldestEntry(
                Entry<String, GLTextureObject> eldest) {
            if (size() > maxColorMapCacheSize) {
                handleRemove(eldest.getValue());
                return true;
            } else {
                return false;
            }
        }

        @Override
        public GLTextureObject remove(Object key) {
            GLTextureObject id = super.remove(key);
            handleRemove(id);
            return id;
        }

        private void handleRemove(GLTextureObject value) {
            if (value != null && value.isValid()) {
                value.dispose();
            }
        }

    };

    protected GLUquadric quadric;

    protected UnmodifiableGLFont colorbarFont;

    protected UnmodifiableGLFont defaultFont;

    protected final boolean drawTileBoundaries = Activator.getDefault()
            .getPreferenceStore()
            .getBoolean(PreferenceConstants.P_DRAW_TILE_BOUNDARIES);

    protected final float textMagnification;

    protected Listener canvasResizeListener;

    protected com.raytheon.viz.core.gl.GLCapabilities capabilities;

    /**
     * Should use this instead of theCanvas.getBounds(...) because it is very
     * expensive as it has to make native calls
     * 
     * The variable is kept up to date with the canvasResizeListener
     */
    protected Rectangle canvasSize;

    protected FontFactory fontFactory;

    protected Rectangle monitorBounds;

    protected IExtent clippingPane;

    /**
     * Construct a GL target using a canvas (inherited from IGraphicsTarget)
     * 
     * @param canvas
     *            the canvas object that the target will be drawn to
     * @param width
     *            the width of the target space (in gl coordinates)
     * @param height
     *            the height of the target space (in gl coordinates)
     * @throws VizException
     */
    public GLTarget(Canvas canvas, float width, float height)
            throws VizException {

        /*
         * Make sure we are passed something we can draw on
         */
        if (!(canvas instanceof GLCanvas)) {
            throw new VizException("Must provide gl-enabled canvas");
        }

        theCanvas = (GLCanvas) canvas;
        theCanvas.setCurrent();
        theContext = new GLContextBridge(theCanvas);

        theContext.makeContextCurrent();

        gl = GLU.getCurrentGL();

        theWidth = width;
        theHeight = height;

        float magnificationVal = Activator.getDefault().getPreferenceStore()
                .getFloat(PreferenceConstants.P_FONT_MAGNIFICATION);
        if (magnificationVal > 0) {
            this.textMagnification = magnificationVal;
        } else {
            this.textMagnification = 1.0f;
        }

        this.canvasSize = this.theCanvas.getBounds();
        this.canvasResizeListener = new Listener() {

            @Override
            public void handleEvent(Event event) {
                GLTarget.this.canvasSize = GLTarget.this.theCanvas.getBounds();
            }

        };
        this.theCanvas.addListener(SWT.Resize, this.canvasResizeListener);
        monitorBounds = this.theCanvas.getDisplay().getPrimaryMonitor()
                .getBounds();

        extensionManager = new GraphicsExtensionManager(this);
    }

    /**
     * Construct a GLTarget using the GLPbuffer for offscreen rendering.
     * 
     * @param width
     *            the width of the target space
     * @param height
     *            the height of the target space
     * @throws VizException
     */
    public GLTarget(float width, float height) throws VizException {
        theCanvas = null;
        canvasSize = new Rectangle(0, 0, (int) width, (int) height);
        theContext = new GLContextBridge((int) width, (int) height);

        theContext.makeContextCurrent();

        gl = GLU.getCurrentGL();
        theWidth = width;
        theHeight = height;

        float magnificationVal = Activator.getDefault().getPreferenceStore()
                .getFloat(PreferenceConstants.P_FONT_MAGNIFICATION);
        if (magnificationVal > 0) {
            this.textMagnification = magnificationVal;
        } else {
            this.textMagnification = 1.0f;
        }

        extensionManager = new GraphicsExtensionManager(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#getGl()
     */
    @Override
    public GL getGl() {
        return this.gl;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#getGlu()
     */
    @Override
    public GLU getGlu() {
        return this.glu;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.IGraphicsTarget#beginFrame(IView, boolean)
     */
    @Override
    public void beginFrame(IView view, boolean clearBackground) {

        if (theCanvas != null && theCanvas.isDisposed()) {
            return;
        }

        makeContextCurrent();

        setView(view);

        if (clearBackground) {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        IExtent viewExtent = targetView.getExtent();
        theCurrentZoom = (viewExtent.getMaxY() - viewExtent.getMinY())
                / theHeight;

        hasLoadedTextureOnLoop = false;
        synchronized (this) {
            if (refreshCount > 0) {
                refreshCount--;
            }

            if (refreshCount == 0) {
                needsRefresh = false;
            }

        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#clearClippingPlane()
     */
    @Override
    public void clearClippingPlane() {
        gl.glDisable(GL.GL_CLIP_PLANE0);
        gl.glDisable(GL.GL_CLIP_PLANE1);
        gl.glDisable(GL.GL_CLIP_PLANE2);
        gl.glDisable(GL.GL_CLIP_PLANE3);
        this.clippingPane = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IGraphicsTarget#createShadedShape(boolean,
     * org.geotools.coverage.grid.GeneralGridGeometry, boolean)
     */
    @Override
    public IShadedShape createShadedShape(boolean mutable,
            GeneralGridGeometry targetGeometry, boolean tesselate) {
        return new GLShadedShape(targetGeometry, mutable, tesselate);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#createWireframeShape(boolean,
     * org.geotools.coverage.grid.GeneralGridGeometry)
     */
    @Override
    public IWireframeShape createWireframeShape(boolean mutable,
            GeneralGridGeometry geom) {
        return new GLWireframeShape2D(geom, mutable);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.IGraphicsTarget#dispose()
     */
    @Override
    public void dispose() {

        if (defaultFont != null) {
            defaultFont.disposeInternal();
        }
        if (colorbarFont != null) {
            colorbarFont.disposeInternal();
        }

        theContext.destroyContext();

        if (theCanvas != null && theCanvas.isDisposed() == false) {
            theCanvas.removeListener(SWT.Resize, this.canvasResizeListener);
        }
        extensionManager.dispose();
    }

    /**
     * Dispose a vbo
     * 
     * @param id
     *            the vbo id
     */
    public void disposeVBO(int id) {
        gl.glDeleteBuffers(1, IntBuffer.wrap(new int[] { id }));
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#drawColorRamp(com.raytheon.uf
     * .viz.core.DrawableColorMap)
     */
    @Override
    public void drawColorRamp(DrawableColorMap drawableColorMap)
            throws VizException {
        this.pushGLState();
        try {
            ColorMapParameters colorMapParams = drawableColorMap
                    .getColorMapParams();
            IExtent pixelExtent = drawableColorMap.extent;
            float blendAlpha = drawableColorMap.alpha;
            float brightness = drawableColorMap.brightness;
            float contrast = drawableColorMap.contrast;
            float logFactor = drawableColorMap.getColorMapParams()
                    .getLogFactor();

            double x1 = pixelExtent.getMinX();
            double x2 = pixelExtent.getMaxX();
            double y1 = pixelExtent.getMinY();
            double y2 = pixelExtent.getMaxY();

            GLTextureObject i = getColorMapTexture(colorMapParams);

            GLBufferCMTextureData alphaMaskTexture = null;
            if (colorMapParams.isUseMask() && capabilities.cardSupportsShaders) {
                byte[] mask = colorMapParams.getAlphaMask();
                alphaMaskTexture = new GLBufferCMTextureData(new ColorMapData(
                        ByteBuffer.wrap(mask), new int[] { mask.length },
                        ColorMapDataType.BYTE), new GLByteDataFormat());
                gl.glActiveTexture(GL.GL_TEXTURE1);
                if (alphaMaskTexture.loadTexture(gl)) {
                    gl.glBindTexture(alphaMaskTexture.getTextureStorageType(),
                            alphaMaskTexture.getTexId());
                } else {
                    alphaMaskTexture.dispose();
                }
            }

            gl.glPolygonMode(GL.GL_BACK, GL.GL_FILL);

            gl.glEnable(GL.GL_TEXTURE_1D);

            gl.glActiveTexture(GL.GL_TEXTURE0);
            i.bind(gl, GL.GL_TEXTURE_1D);

            if (colorMapParams.isInterpolate()) {
                gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MIN_FILTER,
                        GL.GL_LINEAR);
                gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MAG_FILTER,
                        GL.GL_LINEAR);
            } else {
                gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MIN_FILTER,
                        GL.GL_NEAREST);
                gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MAG_FILTER,
                        GL.GL_NEAREST);
            }

            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_S,
                    GL.GL_CLAMP_TO_EDGE);
            gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_T,
                    GL.GL_CLAMP_TO_EDGE);
            gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
                    GL.GL_REPLACE);

            GLShaderProgram program = null;
            if (capabilities.cardSupportsShaders) {
                program = GLSLFactory.getInstance().getShaderProgram(gl, null,
                        "colormap");
                if (program != null) {
                    program.startShader();

                    GLSLStructFactory.createColorMapping(program,
                            "colorMapping", 0, 1, colorMapParams);
                    GLSLStructFactory.createColorModifiers(program,
                            "modifiers", blendAlpha, brightness, contrast);
                    program.setUniform("bkgrndRed",
                            backgroundColor.red / 255.0f);
                    program.setUniform("bkgrndGreen",
                            backgroundColor.green / 255.0f);
                    program.setUniform("bkgrndBlue",
                            backgroundColor.blue / 255.0f);
                }

                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
                        GL.GL_ADD);

                gl.glEnable(GL.GL_BLEND);

                gl.glTexEnvi(GL.GL_TEXTURE_ENV, GL.GL_TEXTURE_ENV_MODE,
                        GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

                gl.glColor4d(0.0, 0.0, 0.0, blendAlpha);
            } else {
                gl.glEnable(GL.GL_BLEND);
                gl.glBlendFunc(GL.GL_CONSTANT_ALPHA,
                        GL.GL_ONE_MINUS_CONSTANT_ALPHA);
                gl.glBlendColor(1.0f, 1.0f, 1.0f, blendAlpha);
                gl.glColor4f(1.0f, 1.0f, 1.0f, blendAlpha);
            }
            if (!capabilities.cardSupportsShaders && logFactor > 0.0) {
                // Normally the colorbar is scaled in shader, but we can do the
                // same thing with a mesh

                double minLog = Math.log(logFactor);
                double maxLog = Math.log(logFactor + 1.0);

                gl.glBegin(GL.GL_TRIANGLE_STRIP);

                gl.glTexCoord1f(0.0f);
                gl.glVertex2d(x1, y1);
                gl.glTexCoord1f(0.0f);
                gl.glVertex2d(x1, y2);
                for (int c = 1; c < 10; c++) {
                    // linear value for index and x
                    float index = c / 10.0f;
                    double x = x1 + index * (x2 - x1);
                    // Make index Log
                    double lg = Math.log(logFactor + index);
                    index = (float) ((lg - minLog) / (maxLog - minLog));

                    gl.glTexCoord1f(index);
                    gl.glVertex2d(x, y1);
                    gl.glTexCoord1f(index);
                    gl.glVertex2d(x, y2);
                }

                gl.glTexCoord1f(1.0f);
                gl.glVertex2d(x2, y1);
                gl.glTexCoord1f(1.0f);
                gl.glVertex2d(x2, y2);

                gl.glEnd();
            } else {
                gl.glBegin(GL.GL_QUADS);

                gl.glTexCoord1f(0.0f);
                gl.glVertex2d(x1, y1);

                gl.glTexCoord1f(1.0f);
                gl.glVertex2d(x2, y1);

                gl.glTexCoord1f(1.0f);
                gl.glVertex2d(x2, y2);

                gl.glTexCoord1f(0.0f);
                gl.glVertex2d(x1, y2);

                gl.glEnd();
            }

            gl.glBlendColor(1.0f, 1.0f, 1.0f, 1.0f);

            if (alphaMaskTexture != null) {
                gl.glActiveTexture(GL.GL_TEXTURE1);
                gl.glBindTexture(alphaMaskTexture.getTextureStorageType(), 0);

                alphaMaskTexture.dispose();
            }

            gl.glBindTexture(GL.GL_TEXTURE_1D, 0);
            gl.glDisable(GL.GL_TEXTURE_1D);

            if (program != null) {
                program.endShader();
            }

            gl.glPolygonMode(GL.GL_BACK, GL.GL_LINE);
            gl.glColor4f(0.66f, 0.66f, 0.66f, blendAlpha);

            gl.glLineWidth(1.5f);
            // Draw the border
            gl.glBegin(GL.GL_QUADS);
            gl.glVertex2d(x1, y1);
            gl.glVertex2d(x2, y1);
            gl.glVertex2d(x2, y2);
            gl.glVertex2d(x1, y2);
            gl.glEnd();
            gl.glDisable(GL.GL_BLEND);
        } finally {
            this.popGLState();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#drawLine(com.raytheon.uf.viz
     * .core.DrawableLine[])
     */
    @Override
    public void drawLine(DrawableLine... lines) throws VizException {
        this.pushGLState();
        try {
            RGB prevColor = null;
            float prevWidth = 0.0f;
            LineStyle prevStyle = null;
            gl.glPolygonMode(GL.GL_BACK, GL.GL_LINE);

            for (DrawableLine line : lines) {
                RGB color = line.basics.color;
                LineStyle style = line.lineStyle;
                float width = line.width;

                if (style != prevStyle) {
                    handleLineStyle(style);
                    prevStyle = style;
                }
                if (color.equals(prevColor) == false) {
                    gl.glColor4d(color.red / 255.0, color.green / 255.0,
                            color.blue / 255.0, 1.0);
                    prevColor = color;
                }
                if (width != prevWidth) {
                    gl.glLineWidth(width);
                    prevWidth = width;
                }

                List<double[]> points = line.points;
                FloatBuffer buf = ByteBuffer
                        .allocateDirect(points.size() * 4 * 3)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                buf.rewind();

                for (double[] point : points) {
                    buf.put(new float[] { (float) point[0], (float) point[1],
                            (float) point[2] });
                }

                gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
                gl.glVertexPointer(3, GL.GL_FLOAT, 0, buf.rewind());
                gl.glDrawArrays(GL.GL_LINE_STRIP, 0, points.size());
            }
        } finally {
            this.popGLState();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.IGraphicsTarget#drawRect(org.eclipse.swt.graphics
     * .Rectangle)
     */
    @Override
    public void drawRect(IExtent extent, RGB color, float lineWidth,
            double alpha) {
        this.pushGLState();
        try {
            gl.glPolygonMode(GL.GL_BACK, GL.GL_LINE);
            gl.glColor3d(color.red / 255.0, color.green / 255.0,
                    color.blue / 255.0);
            gl.glLineWidth(lineWidth);
            gl.glBegin(GL.GL_QUADS);

            // gl.glVertex3d(extent.getMinX(), extent.getMinY(), 0);
            // gl.glVertex3d(extent.getMaxX(), extent.getMinY(), 0);
            // gl.glVertex3d(extent.getMaxX(), extent.getMaxY(), 0);
            // gl.glVertex3d(extent.getMinX(), extent.getMaxY(), 0);
            gl.glVertex2d(extent.getMinX(), extent.getMinY());
            gl.glVertex2d(extent.getMaxX(), extent.getMinY());
            gl.glVertex2d(extent.getMaxX(), extent.getMaxY());
            gl.glVertex2d(extent.getMinX(), extent.getMaxY());
            gl.glEnd();
        } finally {
            this.popGLState();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.IGraphicsTarget#drawShadedRect(com.raytheon.viz
     * .core.IExtent, org.eclipse.swt.graphics.RGB, float, double)
     */
    @Override
    public void drawShadedRect(IExtent pe, RGB color, double alpha,
            byte[] stipple) throws VizException {
        this.pushGLState();
        try {

            // set the shading and alpha
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            gl.glColor4d(color.red / 255.0, color.green / 255.0,
                    color.blue / 255.0, alpha);

            if (stipple != null) {
                gl.glEnable(GL.GL_POLYGON_STIPPLE);

                gl.glPolygonStipple(stipple, 0);
            }

            gl.glPolygonMode(GL.GL_BACK, GL.GL_FILL);
            gl.glBegin(GL.GL_QUADS);

            gl.glVertex2d(pe.getMinX(), pe.getMinY());
            gl.glVertex2d(pe.getMaxX(), pe.getMinY());
            gl.glVertex2d(pe.getMaxX(), pe.getMaxY());
            gl.glVertex2d(pe.getMinX(), pe.getMaxY());
            gl.glEnd();
        } finally {
            this.popGLState();
        }
    }

    @Override
    public void drawShadedShapes(float alpha, float brightness,
            IShadedShape... shapes) throws VizException {
        List<GLShadedShape> glShapes = new ArrayList<GLShadedShape>(
                shapes.length);

        for (IShadedShape shape : shapes) {
            if (shape instanceof GLShadedShape) {
                glShapes.add((GLShadedShape) shape);
            } else {
                throw new VizException("Must pass gl shape: "
                        + shape.getClass().getName());
            }
        }
        if (!glShapes.isEmpty()) {
            drawShadedShapesInternal(glShapes, alpha, 1.0f);
        }
    }

    private void drawShadedShapesInternal(List<GLShadedShape> shapes,
            float alpha, float brightness) {
        brightness = Math.max(brightness, 0.0f);
        brightness = Math.min(brightness, 1.0f);

        pushGLState();
        try {
            gl.glPolygonMode(GL.GL_BACK, GL.GL_FILL);
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL.GL_COLOR_ARRAY);
            gl.glEnable(GL.GL_BLEND);
            // if (cardSupportsHighEndFeatures) {
            gl.glBlendColor(1.0f, 1.0f, 1.0f, alpha);
            // alpha
            gl.glBlendFunc(GL.GL_CONSTANT_ALPHA, GL.GL_ONE_MINUS_CONSTANT_ALPHA);
            // brightness
            // gl.glBlendFunc(GL.GL_CONSTANT_COLOR, GL.GL_ZERO);
            // both
            // }
            for (GLShadedShape shape : shapes) {
                shape.paint(gl, capabilities.cardSupportsHighEndFeatures,
                        brightness);
            }
            gl.glDisableClientState(GL.GL_COLOR_ARRAY);
            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
            gl.glDisable(GL.GL_BLEND);
            // if (cardSupportsHighEndFeatures) {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            // }
        } finally {
            popGLState();
        }
    }

    public double calculateFontResizePercentage(IFont font) {
        double paneWidth = this.canvasSize.width;
        if (this.theCanvas == null) {
            return 1.0;
        }
        if (font == null) {
            font = this.getDefaultFont();
        }

        double ratio = 1.0f;
        if (font.isScaleFont()) {
            /*
             * The Canvas Width Can Never Be The Same Size As The Monitor Width
             * Due To The Smaller Panes And The Window Borders. But, An 80/20
             * Split Seems Reasonable.
             */
            double windowWidth = monitorBounds.width * 0.80;
            ratio = (paneWidth / windowWidth);
        }

        /*
         * Only Degrade 60% Of The Font Size. and never go more than 100% of the
         * size
         */
        double fontRatio = 0.6;

        return Math.min((fontRatio + (ratio * (1 - fontRatio)))
                * textMagnification, 1.0)
                * font.getMagnification();
    }

    @Override
    public void drawWireframeShape(IWireframeShape shape, RGB aColor,
            float lineWidth, IGraphicsTarget.LineStyle lineStyle, float alpha)
            throws VizException {
        drawWireframeShape(shape, aColor, lineWidth, lineStyle, colorbarFont,
                alpha);
    }

    @Override
    public void drawWireframeShape(IWireframeShape shape, RGB aColor,
            float lineWidth, IGraphicsTarget.LineStyle lineStyle, IFont font,
            float alpha) throws VizException {
        if (shape instanceof GLWireframeShape2D) {
            pushGLState();
            try {
                ((GLWireframeShape2D) shape).paint(this,
                        targetView.getExtent(), canvasSize, aColor, lineWidth,
                        lineStyle, font, alpha);
            } finally {
                popGLState();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.IGraphicsTarget#endFrame()
     */
    @Override
    public void endFrame() {
        makeContextCurrent();
        gl.glFinish();

        if (theCanvas != null && theCanvas.isDisposed() == false) {
            theCanvas.swapBuffers();
        }
        GLContextBridge.makeMasterContextCurrent();

        GLDisposalManager.performDispose(GLU.getCurrentGL());

        GLStats.printStats(gl, theCanvas.getShell());

        GLContextBridge.releaseMasterContext();
        releaseContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#getDefaultFont()
     */
    @Override
    public IFont getDefaultFont() {
        return this.defaultFont;
    }

    /**
     * @return the Height
     */
    public float getHeight() {
        return theHeight;
    }

    private double getScaleX() {
        return targetView.getExtent().getWidth() / this.canvasSize.width;

    }

    private double getScaleY() {
        return targetView.getExtent().getHeight() / this.canvasSize.height;

    }

    /**
     * @return the Width
     */
    public float getWidth() {
        return theWidth;
    }

    /**
     * Enables or disables line styles
     * 
     * @param lineStyle
     *            the style of the line
     */
    protected void handleLineStyle(LineStyle lineStyle) {
        if (lineStyle != null && lineStyle != LineStyle.SOLID
                && lineStyle != LineStyle.DEFAULT) {
            gl.glEnable(GL.GL_LINE_STIPPLE);
            gl.glLineStipple(lineStyle.getFactor(), lineStyle.getPattern());
        } else {
            gl.glDisable(GL.GL_LINE_STIPPLE);
        }
    }

    /**
     * Indicates that textures have been loaded during the last beginFrame() /
     * endFrame() sequence
     * 
     * @return
     */
    public boolean hasLoadedTextureOnLoop() {
        return hasLoadedTextureOnLoop;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.IGraphicsTarget#init()
     */
    @Override
    public void init() {
        makeContextCurrent();
        GLU glu = new GLU();
        String exts = glu.gluGetString(GL.GL_EXTENSIONS);
        String openGlRenderer = gl.glGetString(GL.GL_RENDERER);
        String openGlVersion = gl.glGetString(GL.GL_VERSION);
        String openGlVendor = gl.glGetString(GL.GL_VENDOR);

        System.out.println(openGlRenderer + " " + openGlVersion + " "
                + openGlVendor + " " + exts);

        capabilities = com.raytheon.viz.core.gl.GLCapabilities.getInstance(gl);

        Rectangle bounds = this.canvasSize;
        gl.glViewport(0, 0, bounds.width, bounds.height);

        // gl.glDisable(GL.GL_DEPTH_TEST);

        gl.glEnable(GL.GL_STENCIL_TEST);
        gl.glClearStencil(0x0);
        gl.glClearDepth(1.0);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glColor3f(1.0f, 0.0f, 0.0f);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        this.colorbarFont = new UnmodifiableGLFont(new GLFont(DEFAULT_FONT,
                (10 * textMagnification), null));

        if (PlatformUI.isWorkbenchRunning()) {
            fontFactory = FontFactory.getInstance();
            this.defaultFont = new UnmodifiableGLFont(
                    fontFactory.getFont(FontFactory.DEFAULT_FONT_ID));
        } else {
            this.defaultFont = new UnmodifiableGLFont(
                    new GLFont(java.awt.Font.MONOSPACED, 14.0f,
                            new Style[] { Style.BOLD }));
        }

        // Set swap interval to 1 refresh
        gl.setSwapInterval(1);
        // Set swap interval to 0 refresh (disables vsync)
        gl.setSwapInterval(0);

        releaseContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#initializeRaster(com.raytheon
     * .uf.viz.core.data.IRenderedImageCallback)
     */
    @Override
    public IImage initializeRaster(IRenderedImageCallback imageCallback) {
        return new GLImage(imageCallback, GLDefaultImagingExtension.class);
    }

    @Override
    public IFont initializeFont(File fontFile, FontType type, float size,
            Style[] styles) {
        return new GLFont(fontFile, type, size, styles);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.IGraphicsTarget#initializeFont(java.lang.String,
     * float, com.raytheon.viz.core.drawables.IFont.Style[])
     */
    @Override
    public IFont initializeFont(String fontName, float size, Style[] styles) {
        return new GLFont(fontName, size, styles);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#initializeFont(java.lang.String)
     */
    @Override
    public IFont initializeFont(String font) {
        if (fontFactory != null && fontFactory.hasId(font)) {
            return fontFactory.getFont(font);
        }
        return defaultFont.deriveWithSize(defaultFont.getFontSize());
    }

    protected GLTextureObject loadColormapIntoTexture(ColorMap glColorMap) {
        if (glColorMap.getSize() > capabilities.maxTextureSize) {
            double ratio = (glColorMap.getSize() - 1)
                    / (capabilities.maxTextureSize - 1.0);
            List<Color> colors = glColorMap.getColors();
            List<Color> lessColors = new ArrayList<Color>(
                    capabilities.maxTextureSize);
            for (int i = 0; i < capabilities.maxTextureSize; i += 1) {
                lessColors.add(colors.get((int) (i * ratio)));
            }
            statusHandler.info("Reducing colormap to " + lessColors.size()
                    + " colors because this graphics card only supports "
                    + capabilities.maxTextureSize
                    + " colors and the colormap contains "
                    + glColorMap.getSize() + " colors.");
            glColorMap = new ColorMap(lessColors.size());
            for (int i = 0; i < lessColors.size(); i += 1) {
                glColorMap.setColor(i, lessColors.get(i));
            }
        }
        Buffer bb = glColorMap.getColorMap();
        GLContextBridge.makeMasterContextCurrent();
        GLTextureObject t = new GLTextureObject();
        GLContextBridge.releaseMasterContext();
        if (gl.isFunctionAvailable("glActiveTexture")) {
            gl.glActiveTexture(GL.GL_TEXTURE1);
        }
        gl.glEnable(GL.GL_TEXTURE_1D);
        t.bind(gl, GL.GL_TEXTURE_1D);
        bb.rewind();
        gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_S,
                GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_WRAP_T,
                GL.GL_CLAMP_TO_EDGE);

        gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MAG_FILTER,
                GL.GL_NEAREST);
        gl.glTexParameteri(GL.GL_TEXTURE_1D, GL.GL_TEXTURE_MIN_FILTER,
                GL.GL_NEAREST);

        gl.glTexImage1D(GL.GL_TEXTURE_1D, 0, GL.GL_RGBA, glColorMap.getSize(),
                0, GL.GL_RGBA, GL.GL_FLOAT, bb);
        gl.glBindTexture(GL.GL_TEXTURE_1D, 0);
        gl.glDisable(GL.GL_TEXTURE_1D);

        return t;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#makeContextCurrent()
     */
    @Override
    public boolean makeContextCurrent() {
        return theContext.makeContextCurrent();
    }

    /**
     * Release the GL context
     * 
     * Do not call outside of gl package
     * 
     */
    @Override
    public void releaseContext() {
        theContext.releaseContext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#resize()
     */
    @Override
    public void resize() {
        if (theCanvas.isDisposed()) {
            return;
        }

        Rectangle bounds = theCanvas.getClientArea();

        makeContextCurrent();
        gl.glViewport(0, 0, bounds.width, bounds.height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        releaseContext();

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#screenshot()
     */
    @Override
    public BufferedImage screenshot() {

        boolean needsRelease = makeContextCurrent();
        if (theCanvas != null) {
            theCanvas.swapBuffers();
        }
        Rectangle bounds = this.canvasSize;
        BufferedImage bi = Screenshot.readToBufferedImage(bounds.width,
                bounds.height, false);
        if (theCanvas != null) {
            theCanvas.swapBuffers();
        }

        if (needsRelease) {
            releaseContext();
        }
        return bi;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.IGraphicsTarget#setBackgroundColor(org.eclipse.
     * swt.graphics.RGB)
     */
    @Override
    public void setBackgroundColor(RGB backgroundColor) {
        if (backgroundColor == null) {
            throw new IllegalArgumentException(
                    "Background color cannot be null");
        }
        super.setBackgroundColor(backgroundColor);
        gl.glClearColor(backgroundColor.red / 255f,
                backgroundColor.green / 255f, backgroundColor.blue / 255f, 1.0f);
    }

    public RGB getBackgroundColor() {
        return backgroundColor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#setNeedsRefresh(boolean)
     */
    @Override
    public void setNeedsRefresh(boolean needsRefresh) {
        synchronized (this) {
            if (needsRefresh && refreshCount <= 1) {
                refreshCount += 2;
            }
        }
        this.needsRefresh = needsRefresh;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.IGraphicsTarget#setupClippingPlane(float,
     * float, float, float)
     */
    @Override
    public void setupClippingPlane(IExtent extent) {
        if (this.clippingPane == extent
                || (this.clippingPane != null && extent != null && this.clippingPane
                        .equals(extent))) {
            // Clipping pane already set to this
            return;
        }
        this.clippingPane = extent;
        if (clippingPane == null) {
            clearClippingPlane();
            return;
        }
        // Clone to preserve clipping pane extent
        this.clippingPane = clippingPane.clone();

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        double eqn[] = { 0.0, 1.0, 0.0, 0.0 };

        double eqn2[] = { 1.0, 0.0, 0.0, 0.0 };

        double eqn3[] = { 0.0, -1.0, 0.0, 0.0 };

        double eqn4[] = { -1.0, 0.0, 0.0, 0.0 };

        gl.glTranslated(extent.getMinX(), extent.getMinY(), 0);

        /* clip upper */
        gl.glClipPlane(GL.GL_CLIP_PLANE0, eqn, 0);
        gl.glEnable(GL.GL_CLIP_PLANE0);

        /* clip left */
        gl.glClipPlane(GL.GL_CLIP_PLANE1, eqn2, 0);
        gl.glEnable(GL.GL_CLIP_PLANE1);

        gl.glTranslated(-extent.getMinX(), -extent.getMinY(), 0);

        gl.glTranslated(extent.getMaxX(), 0, 0);

        /* clip right */
        gl.glClipPlane(GL.GL_CLIP_PLANE2, eqn4, 0);
        gl.glEnable(GL.GL_CLIP_PLANE2);
        gl.glTranslated(-extent.getMaxX(), 0, 0);

        /* clip bottom */
        gl.glTranslated(0, extent.getMaxY(), 0);
        gl.glClipPlane(GL.GL_CLIP_PLANE3, eqn3, 0);
        gl.glEnable(GL.GL_CLIP_PLANE3);

        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPopMatrix();
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IGraphicsTarget#getModelView()
     */
    @Override
    public double[] getModelView() {
        double[] modelMatrix = new double[16];
        gl.glGetDoublev(GL.GL_MODELVIEW_MATRIX, modelMatrix, 0);
        return modelMatrix;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IGraphicsTarget#getModelView()
     */
    @Override
    public double[] getProjection() {
        double[] projection = new double[16];
        gl.glGetDoublev(GL.GL_PROJECTION_MATRIX, projection, 0);
        return projection;
    }

    /**
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IGraphicsTarget#getViewPort()
     */
    @Override
    public int[] getViewPort() {
        int[] vp = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, vp, 0);
        return vp;
    }

    /**
     * Convert pixel coordinates to window.
     * 
     * @param p
     * @return
     */
    @Override
    public double[] project(double[] p) {
        double[] window = new double[3];
        if (!this.glu.gluProject(p[0], p[1], p[2], getModelView(), 0,
                getProjection(), 0, getViewPort(), 0, window, 0)) {

            return null;
        }
        int[] vp = getViewPort();
        // flip the y
        window[1] = vp[3] - window[1];
        return window;
    }

    /**
     * Convert screen x,y to pixel space
     * 
     * @param pos
     *            screen coordinate
     * @return pixel value
     */
    @Override
    public double[] unproject(double[] pos) {
        return this.unproject(pos[0], pos[1], pos[2]);
    }

    /**
     * Convert screen x,y to pixel space
     * 
     * @param x
     *            screen x value
     * @param y
     *            screen y value
     * @param z
     *            0 near 1 far plane
     * @return pixel value
     */
    public double[] unproject(double x, double y, double z) {

        if (z < 0 || z > 1) {
            return null;
        }

        double[] pixel = new double[3];
        int[] vp = getViewPort();

        try {
            this.glu.gluUnProject(x, vp[3] - y, z, getModelView(), 0,
                    getProjection(), 0, vp, 0, pixel, 0);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return pixel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IDisplayPane#getBounds()
     */
    @Override
    public Rectangle getBounds() {
        if (theCanvas != null && theCanvas.isDisposed()) {
            return null;
        }

        return this.canvasSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#pushGLState()
     */
    @Override
    public void pushGLState() {
        gl.glPushAttrib(GL.GL_COLOR_BUFFER_BIT | GL.GL_CURRENT_BIT
                | GL.GL_ENABLE_BIT | GL.GL_TEXTURE_BIT | GL.GL_LINE_BIT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#popGLState()
     */
    @Override
    public void popGLState() {
        gl.glPopAttrib();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.viz.core.IGraphicsTarget#getView()
     */
    @Override
    public IView getView() {
        return targetView;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.gl.IGLTarget#setView(com.raytheon.uf.viz.core.IView
     * )
     */
    @Override
    public void setView(IView view) {
        this.targetView = view.clone();
        this.targetView.setupView(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.viz.core.gl.IGLTarget#getColorMapTexture(com.raytheon.uf
     * .viz.core.drawables.ColorMapParameters)
     */
    @Override
    public GLTextureObject getColorMapTexture(ColorMapParameters cmapParams) {
        IColorMap cmap = cmapParams.getColorMap();
        String name = cmap.getName();
        if (name == null) {
            name = ColorMapParameters.class.toString() + "@"
                    + cmapParams.hashCode();
        }

        GLTextureObject i = loadedColorMaps.get(name);
        if (i == null || cmap.isChanged()) {
            if (i != null) {
                loadedColorMaps.remove(name);
            }

            i = loadColormapIntoTexture((ColorMap) cmap);
            loadedColorMaps.put(name, i);
            cmap.setChanged(false);
        }
        return i;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#getStringsBounds(com.raytheon
     * .uf.viz.core.DrawStringsParameters, java.lang.String)
     */
    @Override
    public Rectangle2D getStringsBounds(DrawableString parameters, String string) {
        IFont font = parameters.font;
        if (font == null) {
            font = getDefaultFont();
        }

        double fontPercentage = this.calculateFontResizePercentage(font)
                * parameters.magnification;

        TextRenderer textRenderer = ((IGLFont) font).getTextRenderer();

        // use apostrophe and y to get full ascender and descender in height
        Rectangle2D b1 = textRenderer.getBounds("'y");

        // add character to start and end of string to so leading and
        // trailing spaces will be included
        Rectangle2D b2 = textRenderer.getBounds("'" + string + "y");

        // Make Necessary Adjustments To The Calculated Bounds Based On The
        // Pane Size And The Scale Associated With The Pane Size.
        b2.setFrame(b2.getX() * fontPercentage, b2.getY() * fontPercentage,
                (b2.getWidth() - b1.getWidth()) * fontPercentage,
                b2.getHeight() * fontPercentage);
        return b2;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.IGraphicsTarget#drawCircle(com.raytheon.uf.viz
     * .core.DrawableCircle)
     */
    @Override
    public void drawCircle(DrawableCircle... circles) throws VizException {

        this.pushGLState();
        try {
            gl.glEnable(GL.GL_BLEND);
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

            for (DrawableCircle circle : circles) {
                boolean fill = circle.filled;
                double radius = 0.0;

                if (circle.screenRadius != null) {
                    radius = circle.screenRadius * getScaleX();
                } else if (circle.radius != null) {
                    radius = circle.radius;
                }

                float width = circle.lineWidth;
                RGB color = circle.basics.color;
                float alpha = circle.basics.alpha;
                double x = circle.basics.x;
                double y = circle.basics.y;
                double z = circle.basics.z;
                boolean xOr = circle.basics.xOrColors;

                gl.glPolygonMode(GL.GL_BACK, fill ? GL.GL_FILL : GL.GL_LINE);
                gl.glColor4d(color.red / 255.0, color.green / 255.0,
                        color.blue / 255.0, alpha);
                gl.glLineWidth(width);

                if (xOr) {
                    gl.glEnable(GL.GL_COLOR_LOGIC_OP);
                    gl.glLogicOp(GL.GL_XOR);
                }

                float startAzm = circle.startAzimuth;
                float endAzm = circle.endAzimuth;

                if (endAzm < startAzm) {
                    endAzm += 360.0;
                }

                double totalAzimuth = (endAzm - startAzm);
                boolean includeSides = circle.includeSides && !fill
                        && (totalAzimuth < 360.0);

                if (fill) {
                    gl.glBegin(GL.GL_TRIANGLE_FAN);
                    gl.glVertex3d(x, y, z);
                } else {
                    handleLineStyle(circle.lineStyle);
                    gl.glBegin(GL.GL_LINE_STRIP);
                    if (includeSides) {
                        gl.glVertex3d(x, y, z);
                    }
                }

                // Get the number of unique points in the circle
                int points = circle.numberOfPoints;
                if (totalAzimuth >= 360) {
                    // If angle is meant to be complete circle, add extra point
                    // to ensure circle is touching
                    points += 1;
                }
                double step = totalAzimuth / (points - 1);
                for (int i = 0; i < points; ++i) {
                    double azm = startAzm + (i * step);
                    double[] pointOnCircle = getPointOnCircle(x, y, z, radius,
                            azm);
                    gl.glVertex3d(pointOnCircle[0], pointOnCircle[1],
                            pointOnCircle[2]);
                }

                if (includeSides) {
                    gl.glVertex3d(x, y, z);
                }

                gl.glEnd();

                if (xOr) {
                    gl.glDisable(GL.GL_COLOR_LOGIC_OP);
                }
            }

            gl.glDisable(GL.GL_BLEND);
        } finally {
            this.popGLState();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.viz.core.gl.IGLTarget#handleError(int)
     */
    @Override
    public void handleError(int errorid) {
        String message = null;
        switch (errorid) {
        case GL.GL_NO_ERROR:
            break;
        case GL.GL_INVALID_ENUM: {
            message = "Invalid enum passed to gl!";
            break;
        }
        case GL.GL_INVALID_VALUE: {
            message = "Invalid value passed to gl!";
            break;
        }
        case GL.GL_INVALID_OPERATION: {
            message = "Invalid operation used!";
            break;
        }
        case GL.GL_STACK_OVERFLOW: {
            message = "Stack overflow, causing command ignored!";
            break;
        }
        case GL.GL_STACK_UNDERFLOW: {
            message = "Stack underflow, causing command ignored!";
            break;
        }
        case GL.GL_OUT_OF_MEMORY: {
            message = "GL is OUT of memory!!";
            break;
        }
        case GL.GL_TABLE_TOO_LARGE: {
            message = "GL table too large?";
            break;
        }
        case GL.GL_INVALID_FRAMEBUFFER_OPERATION_EXT: {
            message = "Invalid FrameBuffer operation";
            break;
        }
        default: {
            message = "GL Error: " + errorid;
        }
        }

        if (message != null) {
            // System.err.println("!!!!!!!GL ERROR GL ERROR GL ERROR!!!!!!!!: "
            // + message);
            // new VizException().printStackTrace();
            statusHandler.handle(Priority.PROBLEM, message, new VizException(
                    "GL Error"));
        }
    }

    @Override
    public void drawStrings(Collection<DrawableString> parameters)
            throws VizException {
        getExtension(GLStringRenderingExtension.class).drawStrings(parameters);
    }

    @Override
    public void drawPoints(Collection<double[]> locations, RGB color,
            PointStyle pointStyle, float magnification) throws VizException {
        if (pointStyle == PointStyle.NONE || locations.size() == 0) {
            return;
        }

        this.pushGLState();
        try {
            int pointsPerLocation = 1;
            int geomType = GL.GL_LINES;
            switch (pointStyle) {
            case PIPE:
            case POINT:
            case DASH: {
                pointsPerLocation = 2;
                break;
            }
            case X:
            case CROSS: {
                pointsPerLocation = 4;
                break;
            }
            case BOX:
                geomType = GL.GL_LINE_STRIP;
                pointsPerLocation = 5;
                break;
            case SQUARE: {
                geomType = GL.GL_QUADS;
                pointsPerLocation = 4;
                break;
            }
            case STAR: {
                pointsPerLocation = 6;
                break;
            }
            case CIRCLE: {
                geomType = GL.GL_LINE_STRIP;
                pointsPerLocation = (int) (17 * magnification);
                break;
            }
            case DISC: {
                geomType = GL.GL_TRIANGLE_FAN;
                pointsPerLocation = (int) (18 * magnification);
                break;
            }
            default:
                statusHandler.error("Unsupported point style: " + pointStyle);
            }

            FloatBuffer buf = ByteBuffer
                    .allocateDirect(
                            pointsPerLocation * locations.size() * 4 * 2)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
            buf.rewind();

            float xScale = (float) getScaleX() * magnification;
            float xTick = TICK_SIZE * xScale;
            float yTick = (float) (TICK_SIZE * getScaleY() * magnification);
            for (double[] location : locations) {
                int numPoints = pointsPerLocation;
                float x = (float) location[0];
                float y = (float) location[1];

                switch (pointStyle) {
                case POINT: {
                    buf.put(new float[] { x, y, x + xScale, y });
                    break;
                }
                case CROSS: {
                    buf.put(new float[] { x - xTick, y, x + xTick, y, x,
                            y - yTick, x, y + yTick });
                    break;
                }
                case DASH: {
                    buf.put(new float[] { x - xTick, y, x + xTick, y });
                    break;
                }
                case STAR: {
                    buf.put(new float[] { x, y - (yTick * 1.5f), x,
                            y + (yTick * 1.5f), x - xTick, y - yTick,
                            x + xTick, y + yTick, x - xTick, y + yTick,
                            x + xTick, y - yTick });
                    break;
                }
                case X: {
                    buf.put(new float[] { x - xTick, y - yTick, x + xTick,
                            y + yTick, x - xTick, y + yTick, x + xTick,
                            y - yTick });
                    break;
                }

                case DISC:
                    buf.put(new float[] { x, y });
                    numPoints--;
                case CIRCLE:
                    numPoints--;
                    double step = 360.0 / numPoints;
                    for (int i = 0; i <= numPoints; i++) {
                        double[] points = getPointOnCircle(x, y, 0.0, xTick, i
                                * step);
                        buf.put((float) points[0]);
                        buf.put((float) points[1]);
                    }
                    break;
                case BOX:
                    buf.put(new float[] { x - xTick, y - yTick });
                case SQUARE:
                    buf.put(new float[] { x - xTick, y + yTick, x + xTick,
                            y + yTick, x + xTick, y - yTick, x - xTick,
                            y - yTick });
                    break;
                case PIPE:
                    buf.put(new float[] { x, y - yTick, x, y + yTick });
                    break;
                default: 
                    statusHandler.error("Unsupported point style: "
                            + pointStyle);
                }
            }

            gl.glPolygonMode(GL.GL_BACK, GL.GL_FILL);
            gl.glColor4d(color.red / 255.0, color.green / 255.0,
                    color.blue / 255.0, 1.0);
            gl.glLineWidth(magnification);

            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glVertexPointer(2, GL.GL_FLOAT, 0, buf.rewind());
            int loop = geomType == GL.GL_LINES ? 1 : locations.size();
            int size = geomType == GL.GL_LINES ? locations.size()
                    * pointsPerLocation : pointsPerLocation;
            int cur = 0;
            for (int i = 0; i < loop; ++i) {
                gl.glDrawArrays(geomType, cur, size);
                cur += size;
            }

            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        } finally {
            popGLState();
        }
    }

    @Override
    public final <T extends IGraphicsExtensionInterface> T getExtension(
            Class<T> extensionClass) throws VizException {
        if (extensionClass == IImagingExtension.class) {
            return extensionClass.cast(super
                    .getExtension(GLDefaultImagingExtension.class));
        } else {
            return super.getExtension(extensionClass);
        }
    }
}
