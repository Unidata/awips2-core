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

package com.raytheon.uf.common.style;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.SaveableOutputStream;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * This class reads in the rgb.txt file and creates a map of color names and the
 * associated RGB int[] values. Also holds some static color map file operations
 * that were moved from ColorUtil to here.
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#     Engineer     Description
 * ------------- ----------- ------------ --------------------------
 * Jun 27, 2019  65510       ksunil       initial creation
 * Jul 18, 2019  66188       ksunil       Change RGBColors to refer to static RGBUtil instead extending.
 * Jul 25, 2019  65809       ksunil       re-factoring around ColorUtil and RGBUtil
 *
 * </pre>
 *
 * @author ksunil
 *
 */
public class RGBUtil {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(RGBUtil.class);

    /**
     * rgb.txt file.
     */
    private static final String RGB_FILE_NAME = LocalizationUtil
            .join("colorfile", "rgb.txt");

    private static LocalizationFile rgbFile;

    public static class RGBIntEntry {
        public int[] colorValues;

        public String colorName;
    }

    /**
     * Map of color names (key) and RGB color int[] values.
     */
    private static Map<String, RGBIntEntry> colorMap;

    /**
     * Constructor.
     */
    private RGBUtil() {
    }

    private static synchronized void init() {
        rgbFile = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(RGB_FILE_NAME);

        parseFile();
    }

    /**
     * Parse the RGB file. Put the color names and the RGB values in a map.
     */
    private static synchronized void parseFile() {
        String fileLine;
        Map<String, RGBIntEntry> colorMapTmp = new HashMap<>();
        Map<int[], String> reverseMapTmp = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(rgbFile.openInputStream()))) {

            int lineNumber = 0;
            while ((fileLine = reader.readLine()) != null) {
                lineNumber++;
                try {
                    RGBIntEntry entry = new RGBIntEntry();
                    String[] strArray = fileLine.trim().split("\\s+", 4);
                    entry.colorName = strArray[3].trim();
                    entry.colorValues = new int[] {
                            Integer.valueOf(strArray[0]),
                            Integer.valueOf(strArray[1]),
                            Integer.valueOf(strArray[2]) };
                    colorMapTmp.put(entry.colorName.toUpperCase(), entry);

                    /*
                     * Check if the RGB key exists the text file contains
                     * several colors that have the same RGB value (for example:
                     * black & grey0 are the same RGB).
                     */

                    if (!reverseMapTmp.containsKey(entry.colorValues)) {
                        reverseMapTmp.put(entry.colorValues, entry.colorName);
                    }
                } catch (IndexOutOfBoundsException
                        | IllegalArgumentException e) {
                    statusHandler.error(String.format(
                            "Invalid syntax at line %d of %s.\n" + "%s\n"
                                    + "Expected rrr, ggg, bbb, name, where rrr, ggg, bbb are integers between 0 and 255\n",
                            lineNumber, rgbFile, fileLine), e);
                }

            }
            colorMap = Collections.unmodifiableMap(colorMapTmp);

        } catch (LocalizationException | IOException e) {
            statusHandler.error("Error reading " + rgbFile, e);
        }
    }

    public static synchronized Map<String, RGBIntEntry> getColorMap() {
        if (colorMap == null) {
            init();
        }
        return colorMap;
    }

    /**
     * Get the R,G,B int values of the color name as an int[].
     *
     * @param colorName
     *            Name of the color.
     * @return The associated RGB int values as an array.
     */
    public static int[] getRGBIntValues(String colorName) {
        if (colorName.startsWith("#")) {
            return parseHexString(colorName);
        }

        RGBIntEntry entry = getColorMap().get(colorName.trim().toUpperCase());
        if (entry == null) {
            String msg = String.format("\"%s\" is not defined in %s", colorName,
                    rgbFile);
            statusHandler.warn(msg + ", using \"white\"",
                    new IllegalArgumentException(msg));
            return new int[] { 255, 255, 255 };
        }
        return entry.colorValues;
    }

    public static int[] parseHexString(String s) {
        if (s.startsWith("#") && (s.length() == 7)) {
            try {
                int red = Integer.parseInt(s.substring(1, 3), 16);
                int green = Integer.parseInt(s.substring(3, 5), 16);
                int blue = Integer.parseInt(s.substring(5, 7), 16);
                return new int[] { red, green, blue };
            } catch (NumberFormatException e) {
                // fall through to throw
            }
        }

        throw new IllegalArgumentException("\"" + s
                + "\" is not a valid hexadecimal color string of the form (#rrggbb))");
    }

    public static LocalizationFile getRgbFile() {
        return rgbFile;
    }

    /**
     * Saves a colormap to localization
     *
     * @param colorMap
     * @param colormapName
     * @param level
     * @throws LocalizationException
     */
    public static void saveColorMap(IColorMap colorMap, String colormapName,
            LocalizationLevel level) throws LocalizationException {
        String filename = getColormapFilename(colormapName);
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr
                .getContext(LocalizationType.COMMON_STATIC, level);
        ILocalizationFile localizationFile = pathMgr
                .getLocalizationFile(context, filename);
        try (SaveableOutputStream sos = localizationFile.openOutputStream()) {
            ColorMap.JAXB.marshalToStream(colorMap, sos);
            sos.save();
        } catch (SerializationException | IOException e) {
            throw new LocalizationException(
                    "Error saving colormap " + colormapName, e);
        }
    }

    public static String getColormapFilename(String shortName) {
        String filename = ColorMapLoader.DIR_NAME + IPathManager.SEPARATOR
                + shortName;
        if (!filename.endsWith(ColorMapLoader.EXTENSION)) {
            filename += ColorMapLoader.EXTENSION;
        }
        return filename;
    }

    /**
     * Checks if a colormap already exists at the specified localization level
     *
     * @param colormapName
     * @param level
     * @return
     */
    public static boolean checkIfColormapExists(String colormapName,
            LocalizationLevel level) {
        String filename = RGBUtil.getColormapFilename(colormapName);
        File path = null;
        IPathManager pm = PathManagerFactory.getPathManager();
        path = pm.getFile(pm.getContext(LocalizationType.COMMON_STATIC, level),
                filename);
        return path.exists();
    }

    /**
     * Deletes a color map at the specified level
     *
     * @param colormapName
     *            the name of the colormap to delete
     */
    public static void deleteColorMap(String colormapName,
            LocalizationLevel level) throws LocalizationException {
        String filename = RGBUtil.getColormapFilename(colormapName);
        IPathManager pm = PathManagerFactory.getPathManager();
        ILocalizationFile lfile = pm.getLocalizationFile(
                pm.getContext(LocalizationType.COMMON_STATIC, level), filename);
        if (lfile.exists()) {
            lfile.delete();
        }
    }

}
