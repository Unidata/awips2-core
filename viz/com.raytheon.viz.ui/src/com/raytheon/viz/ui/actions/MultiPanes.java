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
package com.raytheon.viz.ui.actions;

/**
 * Utility Enum class
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date           Ticket#    Engineer    Description
 * ------------   ---------- ----------- --------------------------
 * Feb 17, 2020   75012      ksunil      Initial creation
 *
 * </pre>
 *
 * @author ksunil
 */

public enum MultiPanes {
    Two(2), Four(4), Nine(9), Sixteen(16);

    private int count;

    private MultiPanes(int numPanes) {
        this.count = numPanes;
    }

    public int numPanes() {
        return count;
    }
}