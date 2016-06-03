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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2GL3;

import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.IView;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension.IGraphicsExtensionInterface;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.core.gl.IGLFont;
import com.raytheon.viz.core.gl.internal.GLTarget;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * Extension which handles all String rendering
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * May 12, 2014  3074     bsteffen    Initial creation
 * Jul 30, 2014  3476     bsteffen    Flush text renderer before rotating strings.
 * Sep 08, 2015  4824     bsteffen    Fix underline, overline, strikethrough.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class GLStringRenderingExtension extends GraphicsExtension<GLTarget>
        implements IGraphicsExtensionInterface {

    protected final RGB DEFAULT_LABEL_COLOR = new RGB(255, 255, 255);

    /** @deprecated this can be removed when support for word wrap is removed. */
    @Deprecated
    private static final int MIN_WRAP_LEN = 3;

    @Override
    public int getCompatibilityValue(GLTarget target) {
        return Compatibilty.TARGET_COMPATIBLE;
    }

    public void drawStrings(Collection<DrawableString> strings)
            throws VizException {
        if (strings.isEmpty()) {
            return;
        }
        strings = prepareStrings(strings);

        IExtent extent = target.getView().getExtent();
        Rectangle bounds = target.getBounds();
        double glScaleX = extent.getWidth() / bounds.width;
        double glScaleY = extent.getHeight() / bounds.height;

        GL2 gl = target.getGl().getGL2();
        /* Save state */
        target.pushGLState();
        /* Save modelview matrix and set to canvas */
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glPushMatrix();
        try {
            /* Enable needed rendering capabilities. */
            gl.glEnable(GL2.GL_BLEND);
            gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
            gl.glEnable(GL2.GL_TEXTURE_2D);

            gl.glTranslated(extent.getMinX(), extent.getMinY(), 0);
            gl.glScaled(glScaleX, -glScaleY, 1.0);

            /* Draw any box or a blank rectangles. */
            for (DrawableString string : strings) {
                drawBlankOrBox(string);
            }
            /* Draw the actual strings */
            renderStrings(strings);
            /* Draws any under/over/strike lines. */
            for (DrawableString string : strings) {
                drawHorizontalLineStyle(string);
            }
        } finally {
            /* Restore the matrix */
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glPopMatrix();
            /* Disable rendering capabilities */
            gl.glDisable(GL2.GL_TEXTURE_2D);
            gl.glDisable(GL2.GL_BLEND);
            gl.glPolygonMode(GL2.GL_BACK, GL2.GL_LINE);
            /* Restore state */
            target.popGLState();
        }
    }

    /**
     * Clones the string and perform some normalization operations.
     * <ol>
     * <li>Convert x,y into canvas space to match the modelview matrix used in
     * rendering.</li>
     * <li>Ensure font is not null and an IGLFont</li>
     * <li>Switch multiline data the has VerticalAlignment.MIDDLE to
     * VerticalAlignment.TOP and shift y coordinates accordingly.</li>
     * </ol>
     * Ensure font is a IGLFont, Adjust coordinates so that multiline data that
     * is vertically aligned in the middle is rendered from the top.
     */
    protected Collection<DrawableString> prepareStrings(
            Collection<DrawableString> strings) throws VizException {
        Rectangle bounds = target.getBounds();
        IView view = target.getView();
        List<DrawableString> copy = new ArrayList<DrawableString>(
                strings.size());
        for (DrawableString string : strings) {
            /* Convert strings into canvas location */
            string = new DrawableString(string);
            double[] screen = view.gridToScreen(new double[] { string.basics.x,
                    string.basics.y, string.basics.z }, target);
            string.setCoordinates(bounds.x + screen[0], bounds.y + screen[1]);
            /* Ensure font is set and an IGlFont. */
            if (string.font == null) {
                string.font = target.getDefaultFont();
            } else if (!(string.font instanceof IGLFont)) {
                throw new VizException("Font was not prepared using GLTarget");
            }
            /* Switch multi-line vertical middle to top and shift y. */
            if (string.verticalAlignment == VerticalAlignment.MIDDLE
                    && string.getText().length > 1) {
                Rectangle2D totalBounds = target.getStringsBounds(string);
                double totalHeight = totalBounds.getHeight();
                string.basics.y -= totalHeight * .5;
                string.verticalAlignment = VerticalAlignment.TOP;
            }
            copy.add(string);
        }
        return copy;
    }

    /**
     * Draw box or blank style if necessary. This should be the first thing
     * rendered for strings to ensure that the blank is behind everything else.
     * 
     */
    protected void drawBlankOrBox(DrawableString string) {
        boolean boxed = string.hasTextStyle(TextStyle.BOXED);
        boolean blanked = string.hasTextStyle(TextStyle.BLANKED);
        if (!boxed && !blanked) {
            return;
        }
        Map<TextStyle, RGB> textStyles = string.getTextStyleColorMap();
        RGB blankColor = textStyles.get(TextStyle.BLANKED);
        if (blanked && blankColor == null) {
            blankColor = target.getBackgroundColor();
        }
        float alpha = Math.min(string.basics.alpha, 1.0f);

        float[] rotatedPoint = rotate(string);
        GL2 gl = target.getGl().getGL2();
        if (boxed) {
            /* Draw a big box around everything */
            Rectangle2D bounds = target.getStringsBounds(string);
            // getStringBounds adds one to width for boxed that we don't want
            bounds.setFrame(0, 0, bounds.getWidth() - 1, bounds.getHeight());
            shiftRectangle(string, bounds, string.basics.y);
            if (blanked) {
                gl.glPolygonMode(GL2.GL_BACK, GL2.GL_FILL);
                setGlColor(gl, blankColor, alpha);
                gl.glRectd(bounds.getMinX(), bounds.getMaxY(),
                        bounds.getMaxX(), bounds.getMinY());
            }

            RGB boxColor = textStyles.get(TextStyle.BOXED);
            if (boxColor == null) {
                boxColor = string.getColors()[0];
                if (boxColor == null) {
                    boxColor = DEFAULT_LABEL_COLOR;
                }
            }
            gl.glLineWidth(2);
            gl.glPolygonMode(GL2.GL_BACK, GL2.GL_LINE);
            setGlColor(gl, boxColor, alpha);
            gl.glRectd(bounds.getMinX(), bounds.getMaxY(), bounds.getMaxX(),
                    bounds.getMinY());
        } else if (blanked) {
            /* Draw a separate box over each line. */
            double yPos = string.basics.y;

            gl.glPolygonMode(GL2.GL_BACK, GL2.GL_FILL);
            for (int c = 0; c < string.getText().length; c++) {
                Rectangle2D bounds = target.getStringsBounds(string,
                        string.getText()[c]);
                /* save height of line before adding border for box */
                double lineHeight = bounds.getHeight();
                shiftRectangle(string, bounds, yPos);

                setGlColor(gl, blankColor, alpha);
                gl.glRectd(bounds.getMinX(), bounds.getMaxY(),
                        bounds.getMaxX(), bounds.getMinY());
                if (string.verticalAlignment == VerticalAlignment.TOP) {
                    yPos += lineHeight;
                } else {
                    yPos -= lineHeight;
                }
            }
        }
        unrotate(string, rotatedPoint);
    }

    /**
     * Reposition bounds so that the x,y are set correctly reflect string
     * position and alignment. Also add a 1 pixel border needed for rendering
     * boxed/blanked style.
     * 
     * @param string
     *            string being rendered
     * @param bounds
     *            bounds of text, will be modified instead of returning a new
     *            rectangle.
     * @param yPos
     *            starts at string.basics.y but will need to be adjusted for
     *            multiline rendering.
     */
    protected void shiftRectangle(DrawableString string, Rectangle2D bounds,
            double yPos) {
        float[] xy = getUpdatedCoordinates(bounds, string.horizontalAlignment,
                string.verticalAlignment, string.basics.x, yPos);
        double x = xy[0] - 1;
        double y = xy[1] - (bounds.getHeight() + bounds.getY()) - 1;
        double w = bounds.getWidth() + 2;
        double h = bounds.getHeight() + 2;

        bounds.setRect(x, y, w, h);
    }

    /**
     * Perform actual rendering of strings using a {@link TextRenderer}. This
     * also handles the WORD_WRAP and DROP_SHADOW styles.
     * 
     * @param strings
     */
    protected void renderStrings(Collection<DrawableString> strings) {
        GL2 gl = target.getGl();
        gl.glPolygonMode(GL2.GL_BACK, GL2.GL_FILL);
        IFont font = null;
        double magnification = -1.0;
        float fontPercentage = -1.0f;
        TextRenderer textRenderer = null;
        boolean lastXOr = false;
        RGB lastColor = null;
        try {
            // This loop renders the strings.
            for (DrawableString string : strings) {
                IFont stringFont = string.font;
                if (string.rotation != 0 && textRenderer != null) {
                    textRenderer.flush();
                }
                float[] rotatedPoint = rotate(string);

                if (stringFont != font) {
                    /*
                     * Font changes are one of the most expensive parts of
                     * string rendering, avoid them.
                     */
                    font = stringFont;
                    if (textRenderer != null) {
                        textRenderer.end3DRendering();
                    }
                    textRenderer = ((IGLFont) font).getTextRenderer();
                    textRenderer.setSmoothing(font.getSmoothing());
                    textRenderer.begin3DRendering();
                    fontPercentage = -1.0f;
                    lastColor = null;
                }

                if (string.magnification != magnification
                        || fontPercentage == -1.0f) {
                    magnification = string.magnification;
                    fontPercentage = (float) (target
                            .calculateFontResizePercentage(font) * magnification);
                }

                double yPos = string.basics.y;
                float alpha = Math.min(string.basics.alpha, 1.0f);

                if (lastXOr != string.basics.xOrColors) {
                    lastXOr = string.basics.xOrColors;
                    textRenderer.flush();
                    if (lastXOr) {
                        gl.glEnable(GL2.GL_COLOR_LOGIC_OP);
                        gl.glLogicOp(GL2.GL_XOR);
                    } else {
                        gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
                    }
                }

                for (int c = 0; c < string.getText().length; c++) {

                    String str = string.getText()[c];
                    RGB color = string.getColors()[c];
                    Rectangle2D textBounds = target.getStringsBounds(string,
                            str);

                    if (color == null) {
                        color = DEFAULT_LABEL_COLOR;
                    }

                    float[] xy = getUpdatedCoordinates(textBounds,
                            string.horizontalAlignment,
                            string.verticalAlignment, string.basics.x, yPos);

                    if (color != lastColor) {
                        lastColor = color;
                        textRenderer.setColor(color.red / 255.0f,
                                color.green / 255.0f, color.blue / 255.0f,
                                alpha);
                    }
                    if (string.hasTextStyle(TextStyle.WORD_WRAP)) {
                        int i = 0;
                        int j = -1;
                        float y = xy[1];
                        float x = xy[0];
                        while (true) {
                            j = str.indexOf(' ', j + 1);
                            if (j < 0) {
                                break;
                            }
                            if ((j - i) >= MIN_WRAP_LEN) {
                                textRenderer.draw3D(str.substring(i, j), x, y,
                                        0.0f, fontPercentage);
                                i = j + 1;
                                y -= fontPercentage * textBounds.getHeight();
                            }
                        }
                        textRenderer.draw3D(str.substring(i), x, y, 0.0f,
                                fontPercentage);

                    } else if (string.hasTextStyle(TextStyle.DROP_SHADOW)) {
                        RGB shadowColor = string.getTextStyleColorMap().get(
                                TextStyle.DROP_SHADOW);
                        if (shadowColor == null) {
                            shadowColor = new RGB(0, 0, 0);
                        }
                        textRenderer.setColor(shadowColor.red / 255.0f,
                                shadowColor.green / 255.0f,
                                shadowColor.blue / 255.0f, alpha);
                        float halfScaleX = (0.5f * fontPercentage);
                        float halfScaleY = (0.5f * fontPercentage);
                        textRenderer.draw3D(str, xy[0] - halfScaleX, xy[1]
                                + halfScaleY, 0.0f, fontPercentage);
                        textRenderer.draw3D(str, xy[0] + halfScaleX, xy[1]
                                - halfScaleY, 0.0f, fontPercentage);
                        textRenderer.setColor(color.red / 255.0f,
                                color.green / 255.0f, color.blue / 255.0f,
                                alpha);
                        textRenderer.draw3D(str, xy[0], xy[1], 0.0f,
                                fontPercentage);

                    } else {
                        textRenderer.draw3D(str, xy[0], xy[1], 0.0f,
                                fontPercentage);
                    }
                    if (string.verticalAlignment == VerticalAlignment.TOP) {
                        yPos += textBounds.getHeight();
                    } else {
                        yPos -= textBounds.getHeight();
                    }
                }
                if (rotatedPoint != null) {
                    textRenderer.flush();
                    unrotate(string, rotatedPoint);
                }
            }
        } finally {
            if (textRenderer != null) {
                textRenderer.end3DRendering();
            }
            if (lastXOr) {
                gl.glDisable(GL2.GL_COLOR_LOGIC_OP);
            }
        }
    }

    /**
     * Draw the underline, overline, and strikethrough styles on top of the
     * rendered string.
     * 
     * @param string
     */
    protected void drawHorizontalLineStyle(DrawableString string) {
        boolean underline = string.hasTextStyle(TextStyle.UNDERLINE);
        boolean overline = string.hasTextStyle(TextStyle.OVERLINE);
        boolean strikethrough = string.hasTextStyle(TextStyle.STRIKETHROUGH);
        if (!underline && !overline && !strikethrough) {
            return;
        }
        double yPos = string.basics.y;
        float alpha = Math.min(string.basics.alpha, 1.0f);

        double scaleX = 1.0f;
        double scaleY = 1.0f;

        float[] rotatedPoint = rotate(string);
        GL2 gl = target.getGl().getGL2();
        Map<TextStyle, RGB> textStyles = string.getTextStyleColorMap();
        ((GL2GL3) gl).glPolygonMode(GL2.GL_BACK, GL2.GL_LINE);
        gl.glLineWidth(1);
        for (int c = 0; c < string.getText().length; c++) {
            String str = string.getText()[c];

            Rectangle2D textBounds = target.getStringsBounds(string, str);

            float[] xy = getUpdatedCoordinates(textBounds,
                    string.horizontalAlignment, string.verticalAlignment,
                    string.basics.x, yPos);

            double width = textBounds.getWidth();
            double height = textBounds.getHeight();
            double diff = height + textBounds.getY();

            double x1 = xy[0] - scaleX;
            double y1 = (xy[1] - diff) - scaleY;
            double x2 = xy[0] + width + scaleX;
            double y2 = (xy[1] - diff) + height + scaleY;

            gl.glBegin(GL2.GL_LINES);
            if (underline) {
                RGB lineColor = textStyles.get(TextStyle.UNDERLINE);
                if (lineColor == null) {
                    lineColor = string.getColors()[c];
                    if (lineColor == null) {
                        lineColor = DEFAULT_LABEL_COLOR;
                    }
                }
                setGlColor(gl, lineColor, alpha);
                double lineY = y1 + (y2 - y1) * .2;
                gl.glVertex2d(x1, lineY);
                gl.glVertex2d(x2, lineY);
            }
            if (overline) {
                RGB lineColor = textStyles.get(TextStyle.OVERLINE);
                if (lineColor == null) {
                    lineColor = string.getColors()[c];
                    if (lineColor == null) {
                        lineColor = DEFAULT_LABEL_COLOR;
                    }
                }
                setGlColor(gl, lineColor, alpha);
                double lineY = y1 + (y2 - y1);
                gl.glVertex2d(x1, lineY);
                gl.glVertex2d(x2, lineY);
            }
            if (strikethrough) {
                RGB lineColor = textStyles.get(TextStyle.STRIKETHROUGH);
                if (lineColor == null) {
                    lineColor = string.getColors()[c];
                    if (lineColor == null) {
                        lineColor = DEFAULT_LABEL_COLOR;
                    }
                }
                setGlColor(gl, lineColor, alpha);
                double lineY = y1 + (y2 - y1) * .5;
                gl.glVertex2d(x1, lineY);
                gl.glVertex2d(x2, lineY);
            }
            gl.glEnd();

            if (string.verticalAlignment == VerticalAlignment.TOP) {
                yPos += textBounds.getHeight();
            } else {
                yPos -= textBounds.getHeight();
            }
        }

        unrotate(string, rotatedPoint);
    }

    /**
     * Rotate the modelview matrix so that text is rendered rotated, the return
     * value of this method should be passed to
     * {@link #unrotate(DrawableString, float[])} after rendering to reset the
     * modelview matrix.
     * 
     * @param string
     * @return
     */
    protected float[] rotate(DrawableString string) {
        if (string.rotation != 0.0) {
            GL2 gl = target.getGl().getGL2();
            float[] rotatedPoint = getUpdatedCoordinates(
                    new java.awt.Rectangle(0, 0, 0, 0),
                    HorizontalAlignment.LEFT, VerticalAlignment.BOTTOM,
                    string.basics.x, string.basics.y);
            gl.glTranslated(rotatedPoint[0], rotatedPoint[1], 0.0);
            gl.glRotated(string.rotation, 0.0, 0.0, 1.0);
            gl.glTranslated(-rotatedPoint[0], -rotatedPoint[1], 0.0);
            return rotatedPoint;
        }
        return null;
    }

    /**
     * Undo any rotation that was performed in rotate.
     * 
     * @param string
     * @param rotatedPoint
     */
    protected void unrotate(DrawableString string, float[] rotatedPoint) {
        if (rotatedPoint != null) {
            GL2 gl = target.getGl();
            gl.glTranslated(rotatedPoint[0], rotatedPoint[1], 0.0);
            gl.glRotated(-string.rotation, 0.0, 0.0, 1.0);
            gl.glTranslated(-rotatedPoint[0], -rotatedPoint[1], 0.0);
        }
    }

    /**
     * Transform xPos, yPos so that rendering can be done using the returned
     * point as the upper left of the text.
     * 
     * @return float[] of length 2 that represents an x,y coordinate of upper
     *         left corner of text.
     */
    protected static float[] getUpdatedCoordinates(Rectangle2D textBounds,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment, double xPos, double yPos) {
        double width = textBounds.getWidth();
        double height = textBounds.getHeight();

        double offset = (height + textBounds.getY());

        double canvasX1 = 0.0;
        double canvasY1 = 0.0;

        // Calculate the horizontal point based on alignment
        if (horizontalAlignment == HorizontalAlignment.LEFT) {
            canvasX1 = xPos;

        } else if (horizontalAlignment == HorizontalAlignment.CENTER) {
            canvasX1 = xPos - width / 2;

        } else if (horizontalAlignment == HorizontalAlignment.RIGHT) {
            canvasX1 = xPos - width;
        }

        // Calculate the vertical point based on alignment
        if (verticalAlignment == VerticalAlignment.BOTTOM) { // normal
            canvasY1 = yPos;
        } else if (verticalAlignment == VerticalAlignment.MIDDLE) {
            canvasY1 = yPos + height / 2;
        } else if (verticalAlignment == VerticalAlignment.TOP) {
            canvasY1 = yPos + height;
        }

        canvasY1 = offset - canvasY1;

        return new float[] { (float) canvasX1, (float) canvasY1 };
    }

    private static void setGlColor(GL2 gl, RGB color, double alpha) {
        gl.glColor4d(color.red / 255.0, color.green / 255.0,
                color.blue / 255.0, alpha);
    }

}
