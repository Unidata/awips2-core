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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.style.RGBUtil;
import com.raytheon.uf.common.style.RGBUtil.RGBIntEntry;

/**
 * This class reads in the rgb.txt file and creates a map of color names and the
 * associated RGB value.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer     Description
 * ------------- ----------- ------------ --------------------------
 * Nov 29, 2007  373         lvenable     Initial creation
 * Jul 14, 2008  1223        randerso     reworked for common access from all of
 *                                        CAVE
 * Apr 07, 2009              njensen      Added RGB to string features
 * Jan 29, 2018  7153        randerso     Handle missing colors better.
 * Jun 18, 2018  6748        randerso     Trim leading and trailing spaces when
 *                                        looking up color by name. Added text
 *                                        to IllegalArgumentException.
 * Jun 27, 2019  65510       ksunil       Made to extend RGBUtils and some re-factoring around it.
 * Jul 18, 2019  66188       ksunil       Change RGBColors to refer to static RGBUtil instead extending.
 * </pre>
 *
 * @author lvenable
 *
 */
public class RGBColors {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RGBColors.class);

    private RGBColors() {
    }

    /**
     * Map of color names (key) and RGB color (value).
     */
    private static Map<String, RGB> rgbObjectcolorMap;

    /**
     * Maps of RGB colors to color names
     */
    private static Map<RGB, String> rgbObjectReverseColorMap;

    /**
     * Get the RGB value of the color name passed in.
     *
     * @param colorName
     *            Name of the color.
     * @return The associated RGB value.
     */
    public static RGB getRGBColor(String colorName) {
        if (colorName.startsWith("#")) {
            int[] rgb = RGBUtil.parseHexString(colorName);
            return new RGB(rgb[0], rgb[1], rgb[2]);
        }

        RGB rgbColor = getRGBColorMap().get(colorName.trim().toUpperCase());
        if (rgbColor == null) {
            String msg = String.format("\"%s\" is not defined in %s", colorName,
                    RGBUtil.getRgbFile());
            statusHandler.warn(msg + ", using \"white\"",
                    new IllegalArgumentException(msg));
            return new RGB(255, 255, 255);
        }
        return rgbColor;
    }

    private static Map<String, RGB> getRGBColorMap() {
        if (rgbObjectcolorMap == null) {
            initRGBObjectMaps();
        }
        return rgbObjectcolorMap;
    }

    private static Map<RGB, String> getRGBReverseColorMap() {
        if (rgbObjectReverseColorMap == null) {
            initRGBObjectMaps();
        }
        return rgbObjectReverseColorMap;
    }

    private static synchronized void initRGBObjectMaps() {
        // get the underlying map in the int[] form. And convert to a map in
        // RGB. This conversion is done only once.

        Map<String, RGB> rgbObjectcolorMapTmp = new HashMap<>();
        Map<RGB, String> rgbObjectReverseColorMapTmp = new HashMap<>();

        Map<String, RGBIntEntry> entryMap = RGBUtil.getColorMap();
        for (Map.Entry<String, RGBIntEntry> rgbNameVals : entryMap.entrySet()) {
            RGBIntEntry entry = rgbNameVals.getValue();
            RGB col = new RGB(entry.colorValues[0], entry.colorValues[1],
                    entry.colorValues[2]);
            rgbObjectcolorMapTmp.put(entry.colorName.toUpperCase(), col);

            if (!rgbObjectReverseColorMapTmp.containsKey(col)) {
                rgbObjectReverseColorMapTmp.put(col, entry.colorName);
            }
        }
        rgbObjectcolorMap = rgbObjectcolorMapTmp;
        rgbObjectReverseColorMap = rgbObjectReverseColorMapTmp;
    }

    /**
     * Returns the name of a color mapped to the RGB values. If the name is not
     * found, returns the hex string of the color.
     *
     * @param color
     *            an RGB color
     * @return the color name or hex string
     */
    public static String getColorName(RGB color) {

        String name = getRGBReverseColorMap().get(color);
        if (name == null) {
            name = getHexString(color);
        }
        return name;
    }

    /**
     * Get the color name of the hex string passed in. If there is not a color
     * name associated then the hex string is returned.
     *
     * @param hexColor
     *            Color as a hex string.
     * @return Color name.
     */
    public static String getColorName(String hexColor) {

        int[] rgb = RGBUtil.parseHexString(hexColor);

        String name = getRGBReverseColorMap()
                .get(new RGB(rgb[0], rgb[1], rgb[2]));
        if (name != null) {
            return name;
        }

        return hexColor;
    }

    /**
     * Returns a hex string representing the RGB color
     *
     * @param color
     *            the RGB color
     *
     * @return the hex string of the color
     */
    public static String getHexString(RGB color) {
        return String.format("#%02x%02x%02x", color.red, color.green,
                color.blue);
    }
}
