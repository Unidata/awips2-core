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
package com.raytheon.uf.viz.core;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IFont;

/**
 * Object used to store information for drawing a string to an IGraphicsTarget.
 * RGB in basics object is ignored
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Dec 14, 2010           mschenke    Initial creation
 * Apr 04, 2014  2920     bsteffen    Allow strings to use mulitple styles.
 * May 12, 2014  3074     bsteffen    Add support for multicolor text styles.
 * Aug 05, 2014  3489     mapeters    Add constructors whose only parameter is 
 *                                    text for use by {@link IGraphicsTarget
 *                                    #getStringsBounds(DrawableString)}.
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class DrawableString extends AbstractDrawableObject {

    /** The font to draw in */
    public IFont font;

    /** The lines of text to draw */
    private String[] text;

    /** The left/right alignment of the text */
    public HorizontalAlignment horizontalAlignment = HorizontalAlignment.LEFT;

    /** The up/down alignment of the text */
    public VerticalAlignment verticallAlignment = VerticalAlignment.BOTTOM;

    /**
     * Magnification of the text, text will be scaled (zoomed). For smooth
     * magnification, set it on the IFont which then should internally change
     * the font size
     */
    public double magnification = 1.0f;

    /** amount of rotation in degrees from right */
    public double rotation = 0.0;

    /** The colors to use for the strings */
    private RGB[] colors;

    /** @deprecated use {@link #addTextStyle(TextStyle)} */
    @Deprecated
    public TextStyle textStyle = TextStyle.NORMAL;

    private EnumMap<TextStyle, RGB> textStyles = new EnumMap<TextStyle, RGB>(
            TextStyle.class);

    /** @deprecated use {@link #addTextStyle(TextStyle, RGB)} */
    @Deprecated
    public RGB shadowColor = new RGB(0, 0, 0);

    /** @deprecated use {@link #addTextStyle(TextStyle, RGB)} */
    @Deprecated
    public RGB boxColor;

    /**
     * Default color to use for creating a DrawableString for
     * {@link IGraphicsTarget#getStringsBounds(DrawableString)}
     */
    private static final RGB GRAY = new RGB(127, 127, 127);

    public DrawableString(DrawableString that) {
        this.basics.alpha = that.basics.alpha;
        this.basics.color = that.basics.color;
        this.basics.xOrColors = that.basics.xOrColors;
        this.setCoordinates(that.basics.x, that.basics.y, that.basics.z);
        this.text = that.text;
        this.colors = that.colors;
        this.font = that.font;
        this.horizontalAlignment = that.horizontalAlignment;
        this.verticallAlignment = that.verticallAlignment;
        this.magnification = that.magnification;
        this.rotation = that.rotation;
        this.textStyles = new EnumMap<TextStyle, RGB>(that.textStyles);
        this.textStyle = that.textStyle;
        this.shadowColor = that.shadowColor;
        this.boxColor = that.boxColor;
    }

    /**
     * Construct parameters with text, splits by newline, all text will be drawn
     * with color "color"
     * 
     * @param text
     */
    public DrawableString(String text, RGB color) {
        setText(text, color);
    }

    /**
     * Construct parameters with lines of text, all text will be drawn with
     * color "color"
     * 
     * @param text
     */
    public DrawableString(String[] text, RGB color) {
        setText(text, color);
    }

    /**
     * Construct parameters with lines of text and color for each line
     * 
     * @param text
     * @param colors
     */
    public DrawableString(String[] text, RGB[] colors) {
        setText(text, colors);
    }

    /**
     * This constructor is only intended to be used to create a DrawableString
     * for {@link IGraphicsTarget#getStringsBounds(DrawableString)}. It
     * constructs parameters with text and default gray color.
     * 
     * @param text
     */
    public DrawableString(String text) {
        setText(text, GRAY);
    }

    /**
     * This constructor is only intended to be used to create a DrawableString
     * for {@link IGraphicsTarget#getStringsBounds(DrawableString)}. It
     * constructs parameters with lines of text and default gray color.
     * 
     * @param text
     */
    public DrawableString(String[] text) {
        setText(text, GRAY);
    }

    /**
     * Set the text to be drawn, splits by newline
     * 
     * @param text
     */
    public void setText(String text, RGB color) {
        setText(text.split("[\n]"), color);
    }

    /**
     * Set the lines of text to be drawn
     * 
     * @param text
     * @param color
     *            color of text to be drawn
     */
    public void setText(String[] text, RGB color) {
        RGB[] colors = new RGB[text.length];
        for (int i = 0; i < text.length; ++i) {
            colors[i] = color;
        }
        setText(text, colors);
    }

    /**
     * Set the lines of text to be drawn
     * 
     * @param text
     * @param colors
     *            colors of text strings to be drawn
     */
    public void setText(String[] text, RGB[] colors) {
        this.text = text;
        this.colors = colors;
    }

    /**
     * Get the lines of text to draw
     * 
     * @return the lines of text
     */
    public String[] getText() {
        return text;
    }

    /**
     * Get the colors of the text to draw
     * 
     * @return the colors of the lines of text
     */
    public RGB[] getColors() {
        return colors;
    }

    /**
     * Add a new textStyle to this string. For the BLANKED style the background
     * color will be used, for DROP_SHADOR the shadow will be balck and for
     * other styles the color of the text will be used.
     * 
     * @param textStyle
     */
    public void addTextStyle(TextStyle textStyle) {
        this.addTextStyle(textStyle, null);
    }

    /**
     * Add a new text style to this string.
     * 
     * @param textStyle
     *            the style to add
     * @param color
     *            the color to render the style.
     */
    public void addTextStyle(TextStyle textStyle, RGB color) {
        textStyles.put(textStyle, color);
        /*
         * This check is the best we can do to support targets that don't know
         * about textStyles yet.
         */
        if (this.textStyle == null || this.textStyle == TextStyle.NORMAL) {
            this.textStyle = textStyle;
        }
        if (textStyle == TextStyle.DROP_SHADOW && color != null) {
            this.shadowColor = color;
        }
    }

    /**
     * Remove a style from the the set of styles used to render this text.
     */
    public void removeTextStyle(TextStyle textStyle) {
        textStyles.remove(textStyle);
        /*
         * This check is the best we can do to support targets that don't know
         * about textStyles yet.
         */
        if (textStyle == this.textStyle) {
            if (textStyles.isEmpty()) {
                this.textStyle = TextStyle.NORMAL;
            } else {
                this.textStyle = textStyles.keySet().iterator().next();
            }
        }
    }

    /**
     * @return true if the provided TextStyle is to be used when rendering this,
     *         false otherwise.
     */
    public boolean hasTextStyle(TextStyle textStyle) {
        return this.getTextStyleColorMap().containsKey(textStyle);
    }

    /**
     * @deprecated to determine if a style is used use
     *             {@link #hasTextStyle(TextStyle)}, for other cases
     *             {@link #getTextStyleColorMap()} should be used to ensure the
     *             color is used correctly.
     * @return set of all styles to be used for rendering this.
     */
    @Deprecated
    public EnumSet<TextStyle> getTextStyles() {
        Set<TextStyle> mapSet = getTextStyleColorMap().keySet();
        if (mapSet.isEmpty()) {
            return EnumSet.noneOf(TextStyle.class);
        } else {
            return EnumSet.copyOf(mapSet);
        }

    }

    /**
     * @return The styles and colors to render this string. This map may contain
     *         null values in which case the default colors described in
     *         {@link #addTextStyle(TextStyle)} will be used.
     */
    public Map<TextStyle, RGB> getTextStyleColorMap() {
        EnumMap<TextStyle, RGB> textStyles = this.textStyles.clone();
        /*
         * Add in support for deprecated options.
         */
        if (this.textStyle != null && this.textStyle != TextStyle.NORMAL
                && !this.textStyles.containsKey(this.textStyle)) {
            textStyles.put(this.textStyle, null);
            /* BOXED used to imply BLANKED. */
            if (this.textStyle == TextStyle.BOXED
                    && this.textStyles.containsKey(TextStyle.BLANKED) == false) {
                this.textStyles.put(TextStyle.BLANKED, this.boxColor);
            }
        }
        if (this.shadowColor != null
                && textStyles.containsKey(TextStyle.DROP_SHADOW)) {
            textStyles.put(TextStyle.DROP_SHADOW, this.shadowColor);
        }
        return textStyles;
    }

}
