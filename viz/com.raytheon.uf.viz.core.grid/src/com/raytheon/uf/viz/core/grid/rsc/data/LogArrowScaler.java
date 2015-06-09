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
package com.raytheon.uf.viz.core.grid.rsc.data;

import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig.IArrowScaler;

/**
 * 
 * Provides logarithmic scaling of arrows.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Sep 23, 2013  2363     bsteffen    Initial creation
 * May 14, 2015  4079     bsteffen    Move to core.grid
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 * @see VectorGraphicsConfig
 */
public class LogArrowScaler implements IArrowScaler {

    protected final double scaleFactor;

    public LogArrowScaler(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public double scale(double magnitude) {
        return Math.log10(magnitude * scaleFactor) * 10 + 10;
    }

}