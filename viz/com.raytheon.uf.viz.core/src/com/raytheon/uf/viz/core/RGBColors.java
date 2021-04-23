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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * This class reads in the rgb.txt file and creates a map of color names and the
 * associated RGB value.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 29, 2007  373      lvenable  Initial creation
 * Jul 14, 2008  1223     randerso  reworked for common access from all of CAVE
 * Apr 07, 2009           njensen   Added RGB to string features
 * Jan 29, 2018  7153     randerso  Handle missing colors better.
 *
 * </pre>
 *
 * @author lvenable
 *
 */
public class RGBColors {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RGBColors.class);

    /**
     * rgb.txt file.
     */
    private static final String RGB_FILE_NAME = LocalizationUtil
            .join("colorfile", "rgb.txt");

    private static LocalizationFile rgbFile;

    private static class RGBEntry {
        public RGB color;

        public String name;
    }

    /**
     * Map of color names (key) and RGB color (value).
     */
    private static Map<String, RGBEntry> colorMap;

    /**
     * Maps of RGB colors to color names
     */
    private static Map<RGB, String> reverseMap;

    /**
     * Constructor.
     */
    private RGBColors() {
    }

    private static synchronized void init() {
        colorMap = new HashMap<>();
        reverseMap = new HashMap<>();
        rgbFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(RGB_FILE_NAME);

        parseFile();
    }

    /**
     * Parse the RGB file. Put the color names and the RGB values in a map.
     */
    private static void parseFile() {
        String fileLine;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(rgbFile.openInputStream()))) {

            int lineNumber = 0;
            while ((fileLine = reader.readLine()) != null) {
                lineNumber++;
                try {
                    RGBEntry entry = new RGBEntry();
                    String[] strArray = fileLine.trim().split("\\s+", 4);
                    entry.name = strArray[3].trim();
                    entry.color = new RGB(Integer.valueOf(strArray[0]),
                            Integer.valueOf(strArray[1]),
                            Integer.valueOf(strArray[2]));
                    colorMap.put(entry.name.toUpperCase(), entry);

                    /*
                     * Check if the RGB key exists the text file contains
                     * several colors that have the same RGB value (for example:
                     * black & grey0 are the same RGB).
                     */
                    if (!reverseMap.containsKey(entry.color)) {
                        reverseMap.put(entry.color, entry.name);
                    }
                } catch (IndexOutOfBoundsException
                        | IllegalArgumentException e) {
                    statusHandler.error(String.format(
                            "Invalid syntax at line %d of %s.\n" + "%s\n"
                                    + "Expected rrr, ggg, bbb, name, where rrr, ggg, bbb are integers between 0 and 255\n",
                            lineNumber, rgbFile, fileLine), e);
                }

            }
        } catch (LocalizationException | IOException e) {
            statusHandler.error("Error reading " + rgbFile, e);
        }
    }

    private static synchronized Map<String, RGBEntry> getColorMap() {
        if (colorMap == null) {
            init();
        }
        return colorMap;
    }

    private static synchronized Map<RGB, String> getReverseMap() {
        if (reverseMap == null) {
            init();
        }
        return reverseMap;
    }

    /**
     * Get the RGB value of the color name passed in.
     *
     * @param colorName
     *            Name of the color.
     * @return The associated RGB value.
     */
    public static RGB getRGBColor(String colorName) {
        if (colorName.startsWith("#")) {
            return parseHexString(colorName);
        }

        RGBEntry entry = getColorMap().get(colorName.toUpperCase());
        if (entry == null) {
            statusHandler.warn(
                    String.format(
                            "\"%s\" is not defined in %s, using \"white\"\n",
                            colorName, rgbFile),
                    new IllegalArgumentException());
            return new RGB(255, 255, 255);
        }
        return entry.color;
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

        String name = getReverseMap().get(color);
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

        RGB rgb = parseHexString(hexColor);

        String name = getReverseMap().get(rgb);
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

    private static RGB parseHexString(String s) {
        if (s.startsWith("#") && (s.length() == 7)) {
            try {
                int red = Integer.parseInt(s.substring(1, 3), 16);
                int green = Integer.parseInt(s.substring(3, 5), 16);
                int blue = Integer.parseInt(s.substring(5, 7), 16);
                return new RGB(red, green, blue);
            } catch (NumberFormatException e) {
                // fall through to throw
            }
        }

        throw new IllegalArgumentException("\"" + s
                + "\" is not a valid hexadecimal color string of the form (#rrggbb))");
    }
}
