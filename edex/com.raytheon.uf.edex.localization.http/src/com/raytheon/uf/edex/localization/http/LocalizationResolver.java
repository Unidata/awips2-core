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
package com.raytheon.uf.edex.localization.http;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;

/**
 * Utility class for resolving localization resources from HTTP paths
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 16, 2015 3978       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public class LocalizationResolver {

    public static final int CONTEXT_PATH_BASE_COUNT = 2;

    public static final int CONTEXT_PATH_WITH_ID_COUNT = 3;

    public static final int CONTEXT_PATH_TYPE_INDEX = 0;

    public static final int CONTEXT_PATH_LEVEL_INDEX = 1;

    public static final int CONTEXT_PATH_ID_INDEX = 2;

    /**
     * 
     */
    private LocalizationResolver() {
    }

    /**
     * @param resourcePath
     * @return true if request is for listing of context parts (localization
     *         type or level)
     */
    public static boolean isContextQuery(Path resourcePath) {
        /*
         * if we don't have at least the localization type and level on the url,
         * then it is a query for one of them
         */
        return resourcePath.getNameCount() < CONTEXT_PATH_BASE_COUNT;
    }

    /**
     * Get the localization file referenced by resource path
     * 
     * @param resourcePath
     * @return
     * @throws LocalizationHttpException
     *             if file isn't found or can't be accessed
     */
    public static LocalizationFile getFile(Path resourcePath)
            throws LocalizationHttpException {
        LocalizationFile rval = null;

        LocalizationContext context = getContext(resourcePath);
        if (context != null) {
            Path filePath = relativize(context, resourcePath);
            if (filePath.toString().isEmpty()) {
                /*
                 * FIXME the path manager does not currently support getting a
                 * file for the root of the localization context
                 */
                throw new LocalizationHttpException(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Unable to view contents of path: " + resourcePath);
            } else {
                rval = PathManagerFactory.getPathManager().getLocalizationFile(
                        context, filePath.toString());
            }
        }
        if (rval == null) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_NOT_FOUND, "Resource not found: "
                            + resourcePath);
        }
        return rval;
    }

    /**
     * Remove the localization type and level from the path. Levels other than
     * base have identifiers that also need to be removed from the path (eg USER
     * level will have the userid).
     * 
     * @param context
     * @param resourcePath
     * @return
     */
    public static Path relativize(LocalizationContext context, Path resourcePath) {
        Path contextPath = Paths.get(context.toPath());
        return contextPath.relativize(resourcePath);
    }

    /**
     * Get the localization file referenced by resource path
     * 
     * @param resourcePath
     * @return
     * @throws LocalizationHttpException
     *             if context isn't found or can't be accessed
     */
    public static LocalizationContext getContext(Path resourcePath)
            throws LocalizationHttpException {

        String typeString = resourcePath.getName(CONTEXT_PATH_TYPE_INDEX)
                .toString();
        LocalizationType type = findType(typeString);

        String levelString = resourcePath.getName(CONTEXT_PATH_LEVEL_INDEX)
                .toString();
        LocalizationLevel level = findLevel(levelString);

        LocalizationContext rval = null;
        if (type != null && level != null) {
            if (level.equals(LocalizationLevel.BASE)) {
                rval = new LocalizationContext(type, level);
            } else {
                /*
                 * all levels other than base have an identifier, check that
                 * there is a path element left that has the identifier
                 */
                if (resourcePath.getNameCount() < CONTEXT_PATH_WITH_ID_COUNT) {
                    /*
                     * FIXME this denotes that client expects a listing of all
                     * identifiers under the type/level, however the path
                     * manager does not expose a clean method of listing
                     * identifiers. This workaround relies on the assumption
                     * that a context without the identifier can be used as a
                     * localization directory. See Omaha #4003
                     */
                    rval = new LocalizationContext(type,
                            LocalizationLevel.UNKNOWN);
                    rval.setLocalizationLevel(level);
                } else {
                    String identifier = resourcePath.getName(
                            CONTEXT_PATH_ID_INDEX).toString();
                    rval = new LocalizationContext(type, level, identifier);
                }
            }
        }
        if (rval == null) {
            throw new LocalizationHttpException(
                    HttpServletResponse.SC_NOT_FOUND, "Resource not found: "
                            + resourcePath);
        }
        return rval;
    }

    /**
     * Find localization type object
     * 
     * @param typeString
     * @return null if none found
     */
    public static LocalizationType findType(String typeString) {
        LocalizationType rval = null;
        for (LocalizationType type : LocalizationType.values()) {
            if (type.name().equalsIgnoreCase(typeString)) {
                rval = type;
                break;
            }
        }
        return rval;
    }

    /**
     * Find localization level object
     * 
     * @param levelString
     * @return null if none found
     */
    public static LocalizationLevel findLevel(String levelString) {
        LocalizationLevel rval = null;
        for (LocalizationLevel level : LocalizationLevel.values()) {
            if (level.name().equalsIgnoreCase(levelString)) {
                rval = level;
                break;
            }
        }
        return rval;
    }

}
