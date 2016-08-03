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

import java.util.Collection;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * General rendering extension for rendering an IBulkPoints on generic
 * IGraphicsTargets
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

public class GeneralBulkPointsRenderingExtension extends
        GraphicsExtension<IGraphicsTarget> implements
        IBulkPointsRenderingExtension {

    @Override
    public IBulkPoints createBulkPoints(PointStyle style) {
        BulkPoints bulk = new BulkPoints();
        bulk.setPointStyle(style);
        return bulk;
    }

    @Override
    public void drawBulkPoints(IBulkPoints bulkPoints, RGB color,
            float magnification) throws VizException {
        if (!(bulkPoints instanceof BulkPoints)) {
            throw new IllegalArgumentException(this.getClass().getSimpleName()
                    + " cannot handle bulk points of type: "
                    + bulkPoints.getClass().getSimpleName());
        }
        BulkPoints cast = (BulkPoints) bulkPoints;
        Collection<double[]> points = cast.points;
        target.drawPoints(points, color, cast.style, magnification);
    }

    @Override
    public int getCompatibilityValue(IGraphicsTarget target) {
        // return GENERIC since this is the General extension
        return Compatibilty.GENERIC;
    }

}
