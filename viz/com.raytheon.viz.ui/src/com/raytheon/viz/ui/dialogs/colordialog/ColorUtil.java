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

package com.raytheon.viz.ui.dialogs.colordialog;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.localization.exception.LocalizationOpFailedException;

/**
 * Util methods for colormaps.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jul 18, 2007           njensen     Initial creation	
 * Aug 20, 2008           dglazesk    Updated for the new ColorMap interface
 *                                    and for the JiBX to JaXB transition
 * Aug 06, 2013  2210     njensen     Moved colormaps to common_static
 * Nov 11, 2013  2361     njensen     Use ColorMap.JAXB for XML processing
 * Apr 08, 2014  2950     bsteffen    Allow buildColorData to take an IColorMap
 * Jun 30, 2014  3165     njensen     Major cleanup
 * 
 * 
 * </pre>
 * 
 * @author njensen
 */
public class ColorUtil {

    /**
     * value used when converting a color component from an 8 bit value (0-255)
     * to a float percentage(0.0-1.0)
     **/
    public static final float MAX_VALUE = 255.0f;

    /**
     * Updates a ColorMap with the values in an ArrayList<ColorData>
     * 
     * @param aData
     *            the data to update the colormap to
     * @param aMapToUpdate
     *            the colormap to update
     * @return the updated colormap
     */
    public static ColorMap updateColorMap(List<ColorData> aData,
            ColorMap aMapToUpdate) {
        int size = aData.size();
        float[] r = new float[size];
        float[] g = new float[size];
        float[] b = new float[size];
        float[] a = new float[size];
        for (int i = 0; i < size; i++) {
            ColorData cd = aData.get(i);
            r[i] = cd.rgbColor.red / MAX_VALUE;
            g[i] = cd.rgbColor.green / MAX_VALUE;
            b[i] = cd.rgbColor.blue / MAX_VALUE;
            a[i] = cd.alphaValue / MAX_VALUE;
        }

        aMapToUpdate.setRed(r);
        aMapToUpdate.setGreen(g);
        aMapToUpdate.setBlue(b);
        aMapToUpdate.setAlpha(a);

        return aMapToUpdate;
    }

    /**
     * Builds a ColorMap from an ArrayList<ColorData>
     * 
     * @param aData
     *            the colors to build the colormap with
     * @param aName
     *            the name of the new colormap
     * @return a new colormap
     */
    public static ColorMap buildColorMap(List<ColorData> aData, String aName) {
        int size = aData.size();
        float[] r = new float[size];
        float[] g = new float[size];
        float[] b = new float[size];
        float[] a = new float[size];
        for (int i = 0; i < size; i++) {
            ColorData cd = aData.get(i);
            r[i] = cd.rgbColor.red / MAX_VALUE;
            g[i] = cd.rgbColor.green / MAX_VALUE;
            b[i] = cd.rgbColor.blue / MAX_VALUE;
            a[i] = cd.alphaValue / MAX_VALUE;
        }

        return new ColorMap(aName, r, g, b, a);

    }

    /**
     * Builds an ArrayList<ColorData> from a ColorMap
     * 
     * @param aColorMap
     *            the ColorMap to extract ColorData from
     * @return
     */
    public static ArrayList<ColorData> buildColorData(IColorMap aColorMap) {
        ArrayList<ColorData> colors = new ArrayList<ColorData>();

        if (aColorMap != null) {
            for (Color c : aColorMap.getColors()) {
                RGB rgb = new RGB(Math.round(c.getRed() * MAX_VALUE),
                        Math.round(c.getGreen() * MAX_VALUE), Math.round(c
                                .getBlue() * MAX_VALUE));
                colors.add(new ColorData(rgb, Math.round(c.getAlpha()
                        * MAX_VALUE)));
            }
        }
        return colors;
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
        String filename = getColormapFilename(colormapName);
        File path = null;
        IPathManager pm = PathManagerFactory.getPathManager();
        path = pm.getFile(pm.getContext(LocalizationType.COMMON_STATIC, level),
                filename);
        return path.exists();
    }

    /**
     * Saves a colormap to localization
     * 
     * @param colorMap
     * @param filename
     * @param level
     * @throws LocalizationException
     */
    public static void saveColorMap(ColorMap colorMap, String colormapName,
            LocalizationLevel level) throws LocalizationException {
        String xml;
        try {
            xml = ColorMap.JAXB.marshalToXml(colorMap);
        } catch (JAXBException e) {
            throw new LocalizationException("Unable to marshal colormap "
                    + colorMap.getName(), e);
        }

        String filename = getColormapFilename(colormapName);
        IPathManager pathMgr = PathManagerFactory.getPathManager();
        LocalizationContext context = pathMgr.getContext(
                LocalizationType.COMMON_STATIC, level);
        LocalizationFile localizationFile = pathMgr.getLocalizationFile(
                context, filename);
        // getFile(false) will call mkdirs() on the parent dir
        localizationFile.getFile(false);
        localizationFile.write(xml.getBytes());
        localizationFile.save();
    }

    /**
     * Deletes a color map at the specified level
     * 
     * @param colormapName
     *            the name of the colormap to delete
     */
    public static void deleteColorMap(String colormapName,
            LocalizationLevel level) throws LocalizationOpFailedException {
        String filename = getColormapFilename(colormapName);
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationFile lfile = pm.getLocalizationFile(
                pm.getContext(LocalizationType.COMMON_STATIC, level), filename);
        if (lfile.exists()) {
            lfile.delete();
        }
    }

    /**
     * Returns a localization filename (without the context) of the colormap
     * 
     * @param shortName
     * @return
     */
    private static String getColormapFilename(String shortName) {
        String filename = ColorMapLoader.DIR_NAME + IPathManager.SEPARATOR
                + shortName;
        if (!filename.endsWith(ColorMapLoader.EXTENSION)) {
            filename += ColorMapLoader.EXTENSION;
        }
        return filename;
    }
}
