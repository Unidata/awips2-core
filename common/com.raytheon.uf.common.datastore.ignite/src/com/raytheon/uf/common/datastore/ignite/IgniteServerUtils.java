/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.datastore.ignite;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains various constants and utilities used with ignite on the server side.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 21, 2021 8450       mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class IgniteServerUtils {

    public static final String IGNITE_HOME = System.getenv("IGNITE_HOME");

    private static final String CLUSTER_INDEX = "IGNITE_CLUSTER_INDEX";

    /**
     * If this ignite cluster's index matches the given cluster index, add
     * clusterAdditions to baseList and return it, otherwise return just
     * baseList.
     *
     * This is intended to be used in spring.
     *
     * @param <T>
     *            the type of objects in the list
     * @param clusterIndex
     *            the cluster index to check
     * @param baseList
     *            the base list
     * @param clusterAdditions
     *            the list of values to add if the cluster matches
     * @return the combined list if the cluster index matches, otherwise
     *         baseList
     */
    public static <T> List<T> addToListIfClusterIndexMatches(int clusterIndex,
            List<T> baseList, List<? extends T> clusterAdditions) {
        List<T> rval = new ArrayList<>(baseList);
        if (Integer.toString(clusterIndex)
                .equals(System.getenv(CLUSTER_INDEX))) {
            rval.addAll(clusterAdditions);
        }
        return rval;
    }

}
