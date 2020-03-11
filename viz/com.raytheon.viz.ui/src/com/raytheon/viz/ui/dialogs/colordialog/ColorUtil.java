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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.colormap.Color;
import com.raytheon.uf.common.colormap.ColorMap;
import com.raytheon.uf.common.colormap.IColorMap;

/**
 * Utility methods for colormaps.
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
 * Dec 09, 2015  4834     njensen     Don't save colormaps twice
 * Jan 13, 2016  5242     kbisanz     Replaced calls to deprecated LocalizationFile methods
 * Jul 25, 2019  65809    ksunil      re-factoring around ColorUtil and RGBUtil. Moved file operations.
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
     * Updates a ColorMap with the values in a List<ColorData>
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
     * Builds a ColorMap from a List<ColorData>
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
        ArrayList<ColorData> colors = new ArrayList<>();

        if (aColorMap != null) {
            for (Color c : aColorMap.getColors()) {
                RGB rgb = new RGB(Math.round(c.getRed() * MAX_VALUE),
                        Math.round(c.getGreen() * MAX_VALUE),
                        Math.round(c.getBlue() * MAX_VALUE));
                colors.add(new ColorData(rgb,
                        Math.round(c.getAlpha() * MAX_VALUE)));
            }
        }
        return colors;
    }

}
