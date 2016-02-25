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
package com.raytheon.uf.common.dataplugin;

import java.io.File;
import java.util.List;

import com.raytheon.uf.common.dataplugin.persist.IHDFFilePathProvider;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.localization.IPathManager;

/**
 *
 * Utility class for HDF5 data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * ??           ??         ??          Initial creation
 * Feb 26, 2016            nabowle     Initial creation
 *
 * </pre>
 *
 * @author nabowle
 * @version 1.0
 */
public class HDF5Util {

    /**
     * Finds the insert hour of the data record at the specified index.
     *
     * @param object
     *            data record to process
     * @return the file to open
     */
    public static File findHDF5Location(PluginDataObject object) {
        File file = null;
        if (object instanceof IPersistable) {
            IPersistable persistable = (IPersistable) object;

            IHDFFilePathProvider pathProvider = persistable
                    .getHDFPathProvider();

            String path = pathProvider.getHDFPath(object.getPluginName(),
                    persistable);
            String fileName = pathProvider.getHDFFileName(
                    object.getPluginName(), persistable);

            file = new File(object.getPluginName() + IPathManager.SEPARATOR
                    + path + IPathManager.SEPARATOR + fileName);
        }

        return file;
    }

    /**
     * Builds a regex that matches the path of hdf5 data, up to the to final
     * purge key.
     * 
     * @param purgeKeys
     *            The list of purge keys.
     * @param pathKeys
     *            The list of path keys.
     * @param vals
     *            A distinct set of purge values.
     * @return The path regex.
     */
    public static String buildPathRegex(List<String> purgeKeys,
            List<String> pathKeys, String[] vals, String pluginName) {
        int i = 0;
        StringBuilder sb = new StringBuilder(pluginName);
        for (String pathKey : pathKeys) {
            sb.append(File.separator);
            if (purgeKeys.contains(pathKey)) {
                i++;
                /*
                 * Escape the value so characters like "()" aren't mishandled.
                 * We can't escape the whole pathRegex in case a non-purge path
                 * key precedes any purge keys and we include the wildcard from
                 * the else section.
                 */
                sb.append(regexEscape(vals[purgeKeys.indexOf(pathKey)]));
                if (i >= vals.length) {
                    /*
                     * all purge-keys on the accounted for. Any further path
                     * keys can be ignored.
                     */
                    break;
                }
            } else {
                // Insert a wildcard for a path key that precedes a purge key.
                sb.append("[^").append(File.separator).append("]+");
            }
        }
        return sb.toString();
    }

    /**
     * Escapes non-alphanumeric characters in the String.
     *
     * @param s
     *            The String to escape.
     * @return The escaped String.
     */
    public static String regexEscape(String s) {
        /*
         * A recreation of Python's re.escape() since Python's regex engine
         * doesn't support \Q<literal>\E for literal quoting, which is what
         * Java's Pattern.quote() uses.
         */
        return s.replaceAll("([^a-zA-Z0-9])", "\\\\$1");
    }
}
