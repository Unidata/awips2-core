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
package com.raytheon.uf.viz.core.drawables;

import java.awt.Font;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Point;

/**
 * Abstract base class for an AWT-based IFont implementation
 * 
 * This class generates fonts that are properly scaled to the DPI (dots/inch) of
 * the specified display device
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 04, 2015   #5070    randerso    Initial creation
 * Feb 08, 2016   #5318    randerso    Translate generic font aliases to 
 *                                     AWT font families
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public abstract class AbstractAWTDeviceFont implements IFont {

    protected Point dpi;

    protected Font font;

    protected boolean scaleFont = true;

    protected boolean smoothing = true;

    protected AbstractAWTDeviceFont(Point dpi, String fontName, float fontSize,
            Style[] styles) {
        this.dpi = dpi;
        this.font = createFont(dpi, fontName, toAwtStyle(styles), fontSize);

    }

    protected AbstractAWTDeviceFont(Point dpi, File fontFile,
            FontType fontType, float fontSize, Style[] styles) {
        this.dpi = dpi;
        this.font = createFont(dpi, fontFile, fontType, fontSize, styles);
    }

    @Override
    public final String getFontName() {
        return font.getName();
    }

    @Override
    public final Style[] getStyle() {
        return toVizStyles(font.getStyle());
    }

    @Override
    public final void setSmoothing(boolean smooth) {
        this.smoothing = smooth;
    }

    @Override
    public final boolean getSmoothing() {
        return smoothing;
    }

    @Override
    public final boolean isScaleFont() {
        return scaleFont;
    }

    @Override
    public final void setScaleFont(boolean scaleFont) {
        this.scaleFont = scaleFont;
    }

    /**
     * Apply size, style and dpi scaling to font
     * 
     * @param font
     *            AWT font
     * @param size
     *            font size in points (72 points/inch)
     * @param style
     *            AWT font style
     * @param dpi
     *            device dpi in dots/inch
     * @return
     */
    private static Font deriveFont(Font font, float size, int style, Point dpi) {
        /*
         * By default AWT assumes 1 point per pixel or 72 dpi so we need to
         * scale to the actual device dpi
         */
        double sx = (dpi.x) / 72.0;
        double sy = (dpi.y) / 72.0;
        AffineTransform transform = AffineTransform.getScaleInstance(sx, sy);

        return font.deriveFont(size).deriveFont(style, transform);
    }

    /**
     * Create font from fontFile
     * 
     * @param dpi
     *            device dpi in dots/inch
     * @param fontFile
     *            font file
     * @param fontType
     *            font file type
     * @param fontSize
     *            font size in points (72 points/inch)
     * @param styles
     *            array of font styles
     * @return
     */
    protected static Font createFont(Point dpi, File fontFile,
            FontType fontType, float fontSize, Style[] styles) {
        try {
            Font font = Font.createFont(toAwtFontType(fontType), fontFile);

            return deriveFont(font, fontSize, toAwtStyle(styles), dpi);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error creating font", e);
        }
    }

    /**
     * Create font by name
     * 
     * @param dpi
     *            device dpi in dots/inch
     * @param fontName
     *            type face name
     * @param awtStyle
     *            AWT font style
     * @param fontSize
     *            font size in points (72 points/inch)
     * @return
     */
    private static Font createFont(Point dpi, String fontName, int awtStyle,
            float fontSize) {
        Font font = new Font(toAwtFontName(fontName), Font.PLAIN, 1);

        return deriveFont(font, fontSize, awtStyle, dpi);
    }

    /**
     * Translate generic font aliases to AWT font family names
     * 
     * @param fontName
     * @return
     */
    protected static String toAwtFontName(String fontName) {
        if (fontName.equals("Monospace")) {
            return "Monospaced";
        } else if (fontName.equals("Sans")) {
            return "SansSerif";
        }

        /*
         * The generic font alias "Serif" is the same as the AWT font family
         * name "Serif" so needs no translation
         */

        return fontName;
    }

    protected static int toAwtFontType(FontType type) {
        switch (type) {
        case TYPE1:
            return Font.TYPE1_FONT;
        case TRUETYPE:
        default:
            return Font.TRUETYPE_FONT;
        }
    }

    protected static int toAwtStyle(Style[] styles) {
        int styleInt = Font.PLAIN;
        if ((styles == null) || (styles.length == 0)) {
            return styleInt;
        }
        for (Style style : styles) {
            if (style == Style.BOLD) {
                styleInt |= Font.BOLD;
            } else if (style == Style.ITALIC) {
                styleInt |= Font.ITALIC;
            }
        }
        return styleInt;
    }

    protected static Style[] toVizStyles(int style) {
        List<Style> styles = new ArrayList<Style>();
        if ((style & Font.BOLD) != 0) {
            styles.add(Style.BOLD);
        }
        if ((style & Font.ITALIC) != 0) {
            styles.add(Style.ITALIC);
        }
        return styles.toArray(new Style[0]);
    }
}
