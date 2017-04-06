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
package com.raytheon.uf.common.auth.util;

import com.raytheon.uf.common.localization.IPathManager;

/**
 * Build user friendly description of permission string
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 01, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class PermissionDescriptionBuilder {

    /**
     * Private constructor for static class
     */
    private PermissionDescriptionBuilder() {
    }

    /**
     * Creates a user-friendly description of a localization permission
     *
     * Example For the following permission string:
     * localization:write:common_static:site:*:gfe:*
     *
     * You get a description like this: Allow write to
     * common_static.site.any_site_name:gfe/*
     *
     * @param parts
     * @return
     */
    private static String buildLocalizationDescription(String[] parts) {
        StringBuilder description = new StringBuilder("Allow ");
        for (int i = 1; i < Math.max(6, parts.length); i++) {

            switch (i) {
            case 1:
                // read/write/delete
                if (i >= parts.length || "*".equals(parts[i])) {
                    description.append("full access to ");
                } else {
                    description.append(parts[i]);
                    if ("write".equals(parts[i])) {
                        description.append(" to ");
                    } else {
                        description.append(" from ");
                    }
                }
                break;
            case 2:
                // localization type
                if (i >= parts.length || "*".equals(parts[i])) {
                    description.append("all_types");
                } else {
                    description.append(parts[i]);
                }
                description.append('.');
                break;

            case 3:
                // localization level
                if (i >= parts.length || "*".equals(parts[i])) {
                    description.append("all_levels");
                } else {
                    description.append(parts[i]);
                }
                description.append('.');
                break;

            case 4:
                // context name
                if (i >= parts.length || "*".equals(parts[i])) {
                    if (i - 1 >= parts.length || "*".equals(parts[i - 1])) {
                        description.append("any_name");
                    } else {
                        description.append(
                                "any_" + parts[i - 1].toLowerCase() + "_name");
                    }
                } else {
                    description.append(parts[i]);
                }
                description.append(':');
                break;

            case 5:
                // directory path parts
                if (i >= parts.length) {
                    description.append('*');
                } else {
                    description.append(parts[i]);
                }
                break;

            default:
                // directory/file
                description.append(IPathManager.SEPARATOR).append(parts[i]);
                break;
            }
        }
        return description.toString();
    }

    /**
     * Build a user friendly description of a (possibly) wild carded permission
     * string
     *
     * @param permission
     *            colon separated permission string with optional asterisk wild
     *            cards
     *
     *            <pre>
     *
     *     Examples:
     *         bmh:dialog:broadcastCycle
     *         gfe:*
     *         localization:*:*:site:*:gfe:*
     *            </pre>
     *
     * @return description
     */
    public static String buildDescription(String permission) {
        String[] parts = permission.split(":");
        if ("localization".equals(parts[0])) {
            return buildLocalizationDescription(parts);
        } else {
            StringBuilder description = new StringBuilder("Allow ");

            if (parts.length == 1 && "*".equals(parts[0])) {
                description.append("all");
            } else if (parts.length == 2 && "*".equals(parts[1])) {
                description.append("all ").append(parts[0]);
            } else {
                description.append(permission);
            }

            description.append(" actions");
            return description.toString();
        }
    }

}
