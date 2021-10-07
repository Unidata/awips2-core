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
package com.raytheon.uf.common.python;

import java.util.HashMap;
import java.util.Map;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;

/**
 * Utility for getting python directories to include.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 27, 2013           dgilling  Initial creation
 * May 20, 2015  4509     njensen   Added getCommonPythonIncludePath(String...)
 * Oct 22, 2015  5003     dgilling  Added getEdexPythonIncludePath.
 * Oct 07, 2021  8673     randerso  Added null check in getPath()
 *
 * </pre>
 *
 * @author dgilling
 */

public class PythonIncludePathUtil {

    protected static final IPathManager PATH_MANAGER = PathManagerFactory
            .getPathManager();

    protected static final LocalizationContext COMMON_STATIC_BASE = PATH_MANAGER
            .getContext(LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);

    protected static final LocalizationContext EDEX_STATIC_BASE = PATH_MANAGER
            .getContext(LocalizationType.EDEX_STATIC, LocalizationLevel.BASE);

    private static Map<LocalizationContext, Map<String, String>> pathMap = new HashMap<>();

    public static final String PYTHON = "python";

    protected static String getPath(LocalizationContext ctx, String locPath) {
        Map<String, String> ctxMap = pathMap.get(ctx);
        if (ctxMap == null) {
            ctxMap = new HashMap<>();
            pathMap.put(ctx, ctxMap);
        }
        String fsPath = ctxMap.get(locPath);
        if (fsPath == null) {
            LocalizationFile file = PATH_MANAGER.getLocalizationFile(ctx,
                    locPath);
            if (file != null) {
                fsPath = file.getFile().getAbsolutePath();
                ctxMap.put(locPath, fsPath);
            }
        }
        return fsPath;
    }

    /**
     * Gets the path for the common_static/base/python directory
     *
     * @return the path to that python directory
     */
    public static String getCommonPythonIncludePath() {
        return getPath(COMMON_STATIC_BASE, PYTHON);
    }

    /**
     * Gets the path for the common_static/base/python directory
     *
     * @return the path to that python directory
     */
    public static String getEdexPythonIncludePath() {
        return getPath(EDEX_STATIC_BASE, PYTHON);
    }

    /**
     * Builds a python include path of common_static/base/python and any
     * subdirectories if specified, such as common_static/base/python/time
     *
     * @param subDirs
     *            varargs argument of a subdir name, such as "time"
     * @return the base python include path combined with subdirs
     */
    public static String getCommonPythonIncludePath(String... subDirs) {
        String[] dirs = new String[subDirs.length + 1];
        dirs[0] = getCommonPythonIncludePath();
        for (int i = 0; i < subDirs.length; i++) {
            dirs[i + 1] = getPath(COMMON_STATIC_BASE,
                    PYTHON + IPathManager.SEPARATOR + subDirs[i]);
        }
        return PyUtil.buildJepIncludePath(dirs);
    }

}
