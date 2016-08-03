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
package com.raytheon.uf.viz.core.drawables.ext.point;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.drawables.ext.point.IBulkPointsRenderingExtension.IBulkPoints;

/**
 * POJO representing a List of points that should be bulked together to support
 * efficiency. Primarily used by the GeneralBulkPointsRenderingExtension.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 25, 2016  5759      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 */

public class BulkPoints implements IBulkPoints {

    protected List<double[]> points;

    protected PointStyle style;

    @Override
    public void addPoints(Collection<double[]> pointsToAdd) {
        if (this.points == null) {
            this.points = new ArrayList<>(pointsToAdd);
        } else {
            this.points.addAll(pointsToAdd);
        }

    }

    @Override
    public void setPointStyle(PointStyle style) {
        this.style = style;
    }

    @Override
    public int size() {
        if (points == null) {
            return 0;
        }
        return points.size();
    }

    @Override
    public void dispose() {
        points = null;
    }

}
