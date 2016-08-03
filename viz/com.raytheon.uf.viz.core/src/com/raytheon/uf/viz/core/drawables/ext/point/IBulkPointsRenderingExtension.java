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

import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.drawables.ext.GraphicsExtension.IGraphicsExtensionInterface;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Interface for rendering points in bulk to support efficiency
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

public interface IBulkPointsRenderingExtension extends
        IGraphicsExtensionInterface {

    /**
     * Interface for points that should be bulked together for efficiency
     */
    public interface IBulkPoints {

        public void addPoints(Collection<double[]> points);

        public void setPointStyle(PointStyle style);

        public int size();

        public void dispose();

    }

    /**
     * Create an IBulkPoints with a specific PointStyle
     * 
     * @param style
     * @return
     */
    public IBulkPoints createBulkPoints(PointStyle style);

    /**
     * Draw an IBulkPoints with the specified parameters
     * 
     * @param bulkPoints
     * @param color
     * @param magnification
     * @throws VizException
     */
    public void drawBulkPoints(IBulkPoints bulkPoints, RGB color,
            float magnification) throws VizException;

}
