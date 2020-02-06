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
package com.raytheon.uf.viz.core.rsc.sampling;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.GenericResourceData;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory.ResourceOrder;
import org.locationtech.jts.geom.Coordinate;

/**
 * Resource that inspects lat/lon coordinates
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 22, 2010            mschenke     Initial creation
 * Jan 6, 2016  5202       tgurney      Add "enabled" property to turn output
 *                                      on/off
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class LatLonReadoutResource extends
        AbstractVizResource<GenericResourceData, IMapDescriptor> {

    private NumberFormat formatter;

    private boolean enabled = true;

    /**
     * @param resourceData
     * @param loadProperties
     */
    public LatLonReadoutResource(GenericResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
    }

    @Override
    protected void disposeInternal() {

    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {

    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        formatter = DecimalFormat.getInstance();
        formatter.setMinimumFractionDigits(2);
        formatter.setMaximumFractionDigits(2);
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        try {
            return formatLatLon(coord.asLatLon());
        } catch (Exception e) {
            return "";
        }
    }

    @Override
    public ResourceOrder getResourceOrder() {
        return ResourceOrder.LOWEST;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Format a lat/lon for display. If this.isEnabled() is false, return an
     * empty string.
     * 
     * @param coord
     *            the coordinate to format
     * @return the formatted string
     */
    public String formatLatLon(Coordinate coord) {
        if (enabled) {
            Double dX = Double.valueOf(formatter.format(coord.x));
            Double dY = Double.valueOf(formatter.format(coord.y));
            String dz = "";
            if (!Double.isNaN(coord.z)) {
                dz = formatter.format(coord.z);
            }
            String locX = dX > 0 ? "E" : "W";
            String locY = dY > 0 ? "N" : "S";
            return formatter.format(Math.abs(dY)) + locY + " "
                    + formatter.format(Math.abs(dX)) + locX + " " + dz;
        } else {
            return "";
        }
    }
}
