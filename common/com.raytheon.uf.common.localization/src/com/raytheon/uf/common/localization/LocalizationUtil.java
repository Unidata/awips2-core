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
package com.raytheon.uf.common.localization;

import java.util.Arrays;

import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;

/**
 * Utility functions for Localization
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ------------------
 * Jan 13, 2011           njensen   Initial creation
 * Apr 25, 2016  5605     randerso  Added join()
 * Aug 11, 2016  5816     randerso  Added getParent()
 * Oct 07, 2021  8673     randerso  Code cleanup
 *
 * </pre>
 *
 * @author njensen
 */

public class LocalizationUtil {

    /**
     * Extracts the parent directory of a file
     *
     * @param filePath
     * @return the parent directory
     */
    public static String getParent(String filePath) {
        String[] split = splitUnique(filePath);
        if (split.length > 1) {
            return join(Arrays.copyOfRange(split, 0, split.length - 1));
        }
        return "";
    }

    /**
     * Extracts the name of a file or directory from a path
     *
     * @param filePath
     * @return the name of the file/directory
     */
    public static String extractName(String filePath) {
        String[] split = splitUnique(filePath);
        if (split.length > 0) {
            return split[split.length - 1];
        }
        return filePath;
    }

    /**
     * Split the path by the file separator, getting rid of empty parts
     *
     * @param filePath
     * @return
     */
    public static String[] splitUnique(String filePath) {
        String[] split = filePath.split("[/\\\\]"); // Win32

        String[] parts = Arrays.stream(split).filter(item -> !item.isEmpty())
                .toArray(String[]::new);

        return parts;
    }

    /**
     * Split the path by the file separator, getting rid of empty parts and
     * reconstructs the file path
     *
     * @param filePath
     * @return
     */
    public static String getSplitUnique(String filePath) {
        String filename = join(splitUnique(filePath));
        return filename;
    }

    /**
     * Gets the "proper" name of the level, which capitalizes the first letter.
     * USER becomes User, SITE Site, etc
     *
     * @param level
     * @return
     */
    public static String getProperName(LocalizationLevel level) {
        char[] chars = level.name().toLowerCase().toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    public static String join(String... parts) {
        return String.join(IPathManager.SEPARATOR, parts);
    }
}
