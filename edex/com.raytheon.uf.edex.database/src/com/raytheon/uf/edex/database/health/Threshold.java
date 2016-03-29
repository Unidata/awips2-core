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
package com.raytheon.uf.edex.database.health;

import com.raytheon.uf.common.util.SizeUtil;

/**
 * Minimum table/index size and bloat percent for a given warning / critical
 * threshold.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 10, 2016 4630       rjpeter     Initial creation
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */

public class Threshold {
    /** Size of table or index */
    private final long sizeInBytes;

    /** Percent of size to take action on */
    private final double percent;

    public Threshold(long sizeInMb, double percent) {
        this.sizeInBytes = sizeInMb * SizeUtil.BYTES_PER_MB;
        this.percent = percent;
    }

    /**
     * @return the size
     */
    public long getSizeInBytes() {
        return sizeInBytes;
    }

    /**
     * @return the percent
     */
    public double getPercent() {
        return percent;
    }
}
