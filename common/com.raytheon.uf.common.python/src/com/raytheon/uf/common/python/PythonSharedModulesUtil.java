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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Utilities for interacting with shared modules
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 19, 2017 7149           njensen     Initial creation
 * 
 *
 * </pre>
 *
 * @author njensen
 */

public class PythonSharedModulesUtil {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PythonSharedModulesUtil.class);

    public static Set<String> getSharedModules() {
        Set<String> sharedModules = new HashSet<>();
        Map<LocalizationLevel, LocalizationFile> map = PathManagerFactory
                .getPathManager().getTieredLocalizationFile(
                        LocalizationType.COMMON_STATIC, "python"
                                + IPathManager.SEPARATOR + "sharedModules.txt");

        for (LocalizationFile file : map.values()) {
            if (file != null && file.exists()) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(file.openInputStream()))) {
                    String line = br.readLine();
                    while (line != null) {
                        if (line.trim().length() != 0
                                && !line.startsWith("#")) {
                            sharedModules.add(line);
                        }
                        line = br.readLine();
                    }
                } catch (IOException | LocalizationException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Unable to retrieve shared modules list from "
                                    + file.getPath(),
                            e);
                }
            }
        }

        return sharedModules;
    }

}
