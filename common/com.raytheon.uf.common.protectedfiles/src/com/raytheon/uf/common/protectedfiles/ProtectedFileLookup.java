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
package com.raytheon.uf.common.protectedfiles;

import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;

/**
 * Methods to look up if an ILocalizationFile is protected or not. A protected
 * file is one that cannot be overridden below a specific level.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 04, 2017  6379      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class ProtectedFileLookup {

    private ProtectedFileLookup() {
        // do not allow instantiation
    }

    /**
     * Gets the localization level the localization file is protected at, or
     * null if it is not protected
     * 
     * @param file
     * @return
     */
    public static LocalizationLevel getProtectedLevel(ILocalizationFile file) {
        String site = null;
        if (LocalizationLevel.SITE
                .equals(file.getContext().getLocalizationLevel())
                || LocalizationLevel.CONFIGURED
                        .equals(file.getContext().getLocalizationLevel())) {
            site = file.getContext().getContextName();
        } else {
            site = PathManagerFactory.getPathManager()
                    .getContext(LocalizationType.COMMON_STATIC,
                            LocalizationLevel.SITE)
                    .getContextName();
        }

        return ProtectedFiles.getProtectedLevel(site,
                file.getContext().getLocalizationType(), file.getPath());
    }

    /**
     * Checks if the localization file is protected
     * 
     * @param file
     * @return
     */
    public static boolean isProtected(ILocalizationFile file) {
        return getProtectedLevel(file) != null;
    }

}
