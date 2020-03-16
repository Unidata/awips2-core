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

package com.raytheon.uf.common.colormap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;

/**
 * 
 * Facilitates loading of colormaps
 * 
 * <pre>
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
 * Jun 30, 2014  3165     njensen     Moved to common colormap plugin
 * Dec 10, 2015  4834     njensen     Simplified observing and caching
 * Jul 25, 2019  65809    ksunil      Added putIfAbsentInCache to support the ticket.
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */

public class ColorMapLoader {

    public static final String EXTENSION = ".cmap";

    public static final String DIR_NAME = "colormaps";

    private static final Object sharedMutex = new Object();

    /** Map of cached color maps **/
    private static Map<String, IColorMap> cachedMaps = new HashMap<String, IColorMap>();

    private static ILocalizationPathObserver observer;

    /**
     * Load a colormap by name
     * 
     * @param name
     *            name of the colormap
     * @return the colormap representation
     * @throws ColorMapException
     */
    public static IColorMap loadColorMap(String name) throws ColorMapException {
        IColorMap cm = null;

        synchronized (sharedMutex) {
            if (observer == null) {
                observer = new ILocalizationPathObserver() {
                    @Override
                    public void fileChanged(ILocalizationFile file) {
                        String cname = shortenName(file);
                        synchronized (sharedMutex) {
                            cachedMaps.remove(cname);
                        }

                    }
                };
                PathManagerFactory.getPathManager()
                        .addLocalizationPathObserver(DIR_NAME, observer);
            }
            cm = cachedMaps.get(name);
        }

        if (cm == null) {
            // not found in cache
            try {
                ILocalizationFile f = PathManagerFactory.getPathManager()
                        .getStaticLocalizationFile(DIR_NAME
                                + IPathManager.SEPARATOR + name + EXTENSION);
                if (f == null || !f.exists()) {
                    // If the file was not found check to see if the
                    // localization context is encoded as part of the path.
                    String[] split = name.split(IPathManager.SEPARATOR, 3);
                    for (LocalizationLevel level : LocalizationLevel.values()) {
                        if (level.name().equals(split[0])) {
                            LocalizationContext context = new LocalizationContext(
                                    LocalizationType.COMMON_STATIC, level,
                                    split[1]);
                            f = PathManagerFactory.getPathManager()
                                    .getLocalizationFile(context,
                                            DIR_NAME + IPathManager.SEPARATOR
                                                    + split[2] + EXTENSION);
                            if (f == null) {
                                return loadColorMap(split[2]);
                            }
                        }
                    }
                }
                cm = loadColorMap(name, f);
                if (cm != null) {
                    cachedMaps.put(name, cm);
                } else {
                    throw new ColorMapException("Can't find colormap " + name);
                }
            } catch (SerializationException e) {
                throw new ColorMapException(
                        "Exception while loading colormap " + name, e);
            }
        }
        return cm;
    }

    /**
     * Recursively searches for the colormaps that are in the specified
     * directory
     * 
     * @param dir
     *            the directory to search recursively
     * @return the localization files of the colormaps that are found
     */
    private static ILocalizationFile[] internalListColorMapFiles(String dir) {

        IPathManager pm = PathManagerFactory.getPathManager();
        Set<LocalizationContext> searchContexts = new HashSet<LocalizationContext>();

        searchContexts.addAll(Arrays.asList(
                pm.getLocalSearchHierarchy(LocalizationType.COMMON_STATIC)));

        // Use of LocalizationLevels.values() in this case should be okay since
        // we are requesting all possible context names for the level, doesn't
        // matter if our local context for the level is set
        LocalizationLevel[] levels = pm.getAvailableLevels();
        for (LocalizationLevel level : levels) {
            if (level.isSystemLevel() == false) {
                String[] available = pm.getContextList(level);
                for (String s : available) {
                    LocalizationContext ctx = pm
                            .getContext(LocalizationType.COMMON_STATIC, level);
                    ctx.setContextName(s);
                    searchContexts.add(ctx);
                }
            }
        }

        ILocalizationFile[] files = pm.listFiles(
                searchContexts.toArray(
                        new LocalizationContext[searchContexts.size()]),
                dir, new String[] { EXTENSION }, true, true);
        return files;
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
    public static ILocalizationFile[] listColorMapFiles(String subDirectory) {
        return internalListColorMapFiles(
                DIR_NAME + IPathManager.SEPARATOR + subDirectory);
    }

    /**
     * Gets the colormap file's name without the colormap dir and the colormap
     * extension but potentially including localization directories
     * 
     * @param file
     *            the colormap file
     * @return
     */
    public static String shortenName(ILocalizationFile file) {
        String path = file.getPath();
        int startIndex = DIR_NAME.length();
        int endIndex = path.indexOf(EXTENSION);
        String cname = null;
        if (startIndex > -1 && endIndex > -1) {
            cname = path.substring(startIndex + 1, endIndex);
            LocalizationContext ctx = file.getContext();
            if (!ctx.getLocalizationLevel().equals(LocalizationLevel.BASE)) {
                // rebuild the weird name with the context in it
                cname = ctx.getLocalizationLevel().toString()
                        + IPathManager.SEPARATOR + ctx.getContextName()
                        + IPathManager.SEPARATOR + cname;
            }
        } else {
            // shouldn't be possible but just in case
            cname = path;
        }
        return cname;
    }

    /**
     * Lists all the colormaps available in the colormaps dir
     * 
     * 
     * @return an array of all the colormap names
     */
    public static String[] listColorMaps() {
        ILocalizationFile[] files = internalListColorMapFiles(DIR_NAME);
        String[] cmaps = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            cmaps[i] = shortenName(files[i]);
        }
        Arrays.sort(cmaps);
        return cmaps;
    }

    private static IColorMap loadColorMap(String name,
            ILocalizationFile colorMapFile) throws SerializationException {
        if (colorMapFile != null) {
            try (InputStream is = colorMapFile.openInputStream()) {
                ColorMap cm = ColorMap.JAXB.unmarshalFromInputStream(is);
                cm.setName(name);
                cm.setChanged(false);
                return cm;
            } catch (LocalizationException | IOException e) {
                throw new SerializationException("Error loading colormap "
                        + name + " from file " + colorMapFile);
            }
        }

        return null;
    }

    public static void putIfAbsentInCache(String name, IColorMap cMap) {
        synchronized (sharedMutex) {
            cachedMaps.putIfAbsent(name, cMap);
        }
    }
}
