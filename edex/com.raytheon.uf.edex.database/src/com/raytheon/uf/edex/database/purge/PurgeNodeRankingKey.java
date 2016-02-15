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
package com.raytheon.uf.edex.database.purge;

/**
 * Used to rank purge nodes based on the total length to root and the number of
 * exact matches between the node and root.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 11, 2016 5307       bkowal      Initial creation
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

public class PurgeNodeRankingKey {

    private final int matchLength;

    private final int exactMatches;

    public PurgeNodeRankingKey(final int matchLength, final int exactMatches) {
        this.matchLength = matchLength;
        this.exactMatches = exactMatches;
    }

    /**
     * @return the matchLength
     */
    public int getMatchLength() {
        return matchLength;
    }

    /**
     * @return the exactMatches
     */
    public int getExactMatches() {
        return exactMatches;
    }

    public int getRankingValue() {
        /*
         * exact matches have more affluence than number of nodes in the match
         * sequence.
         */
        return matchLength + (exactMatches * 2);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(
                "PurgeNodeRankingKey [matchLength=");
        sb.append(this.matchLength).append(", exactMatches=");
        sb.append(this.exactMatches).append("]");

        return sb.toString();
    }
}