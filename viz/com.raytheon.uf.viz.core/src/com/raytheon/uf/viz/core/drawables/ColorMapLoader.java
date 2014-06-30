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

import com.raytheon.uf.common.colormap.ColorMapException;
import com.raytheon.uf.common.colormap.IColorMap;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * Facilitates loading of colormaps. This class is deprecated and will be
 * removed in the future.
 * 
 * @deprecated Use the com.raytheon.uf.common.colormap.ColorMapLoader instead.
 * 
 *             <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 13, 2007           chammack    Initial Creation.
 * Aug 20, 2007           njensen     Added listColorMaps().
 * Aug 20, 2008           dglazesk    JiBX to JaXB
 * Aug 20, 2008           dglazesk    Updated for new ColorMap interface
 * Jun 10, 2013  2075     njensen     Added listColorMapFiles(subdirectory)
 * Aug 06, 2013  2210     njensen     Moved colormaps to common_static
 * Sep 18, 2013  2421     bsteffen    Moved some listing capabilities into
 *                                    ColorMapTree.
 * Nov 11, 2013  2361     njensen     Use ColorMap.JAXB for XML processing
 * Jun 30, 2014  3165     njensen     Deprecated and copied to common colormap plugin
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */

@Deprecated
public class ColorMapLoader {

    /**
     * Load a colormap by name
     * 
     * @param name
     *            name of the colormap
     * @return the colormap representation
     * @throws VizException
     */
    public static IColorMap loadColorMap(String name) throws VizException {
        try {
            return com.raytheon.uf.common.colormap.ColorMapLoader
                    .loadColorMap(name);
        } catch (ColorMapException e) {
            throw new VizException(e);
        }
    }

    /**
     * Lists all the colormaps in the specified subdirectory. For example, if
     * subdirectory is "ffmp", it will recursively walk down the colormaps/ffmp
     * directory
     * 
     * @param subDirectory
     *            the subdirectory of the colormaps dir to search
     * @return
     */
    public static LocalizationFile[] listColorMapFiles(String subDirectory) {
        return com.raytheon.uf.common.colormap.ColorMapLoader
                .listColorMapFiles(subDirectory);
    }

    public static String shortenName(LocalizationFile file) {
        return com.raytheon.uf.common.colormap.ColorMapLoader.shortenName(file);
    }

    /**
     * Lists all the colormaps available in the colormaps dir
     * 
     * 
     * @return an array of all the colormap names
     */
    public static String[] listColorMaps() {
        return com.raytheon.uf.common.colormap.ColorMapLoader.listColorMaps();
    }

}
