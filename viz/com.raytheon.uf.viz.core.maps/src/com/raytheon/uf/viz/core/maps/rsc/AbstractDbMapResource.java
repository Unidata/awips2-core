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
package com.raytheon.uf.viz.core.maps.rsc;

import java.util.List;

import org.geotools.geometry.jts.CoordinateSequenceTransformer;
import org.geotools.geometry.jts.DefaultCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.raytheon.uf.common.geospatial.GeometryTransformer;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.util.EnvelopeIntersection;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.rsc.AbstractDbMapResourceData.ColumnDefinition;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.LabelableCapability;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Base class for database map resources
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            randerso    Initial creation
 * Apr 17, 2014  #2997     randerso    Moved buildBoundingGeometry up from DbMapResource
 * Aug 21, 2014  #3459     randerso    Restructured Map resource class hierarchy
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public abstract class AbstractDbMapResource<T extends AbstractDbMapResourceData, D extends IMapDescriptor>
        extends StyledMapResource<T, D> {

    private List<String> labelFields;

    /**
     * @param resourceData
     * @param loadProperties
     */
    protected AbstractDbMapResource(T resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
    }

    /**
     * @return the labelFields
     */
    public List<String> getLabelFields() {
        if (this.labelFields == null) {
            try {
                this.labelFields = DbMapQueryFactory.getMapQuery(
                        resourceData.getTable(), resourceData.getGeomField())
                        .getColumnNamesWithoutGeometries();
                ColumnDefinition[] columns = resourceData.getColumns();
                if (columns != null) {
                    for (ColumnDefinition col : columns) {
                        labelFields.add(col.getName());
                    }
                }
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error querying available label fields", e);
            }
        }
        return this.labelFields;
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);
        getCapability(LabelableCapability.class).setAvailableLabelFields(
                getLabelFields().toArray(new String[0]));
    }

    @Override
    protected Geometry buildBoundingGeometry(PixelExtent extent,
            double worldToScreenRatio, double kmPerPixel) {
        // long t0 = System.currentTimeMillis();

        Envelope env = descriptor.pixelToWorld(extent, descriptor.getCRS());
        org.opengis.geometry.Envelope sourceEnvelope = new ReferencedEnvelope(
                env, descriptor.getCRS());

        CoordinateReferenceSystem targetCRS = MapUtil
                .constructEquidistantCylindrical(MapUtil.AWIPS_EARTH_RADIUS,
                        MapUtil.AWIPS_EARTH_RADIUS, 0, 0);

        double[] srcPts = new double[] { -180, -90, 180, 90 };
        double[] dstPts = new double[srcPts.length];
        try {
            MathTransform toEC = MapUtil.getTransformFromLatLon(targetCRS);
            toEC.transform(srcPts, 0, dstPts, 0, 2);
        } catch (Exception e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
        org.opengis.geometry.Envelope targetEnvelope = new ReferencedEnvelope(
                new Envelope(dstPts[0], dstPts[2], dstPts[1], dstPts[3]),
                targetCRS);

        double threshold = kmPerPixel * SPEED_UP;
        int maxHorDivisions = (int) Math.ceil(extent.getWidth() / SPEED_UP
                / worldToScreenRatio);
        int maxVertDivisions = (int) Math.ceil(extent.getHeight() / SPEED_UP
                / worldToScreenRatio);

        Geometry g = null;
        try {
            g = EnvelopeIntersection.createEnvelopeIntersection(sourceEnvelope,
                    targetEnvelope, threshold, maxHorDivisions,
                    maxVertDivisions);

            CoordinateSequenceTransformer cst = new DefaultCoordinateSequenceTransformer(
                    PackedCoordinateSequenceFactory.DOUBLE_FACTORY);
            final GeometryTransformer transformer = new GeometryTransformer(cst);
            MathTransform toLL = MapUtil.getTransformToLatLon(targetCRS);
            transformer.setMathTransform(toLL);

            g = transformer.transform(g);
        } catch (Exception e1) {
            statusHandler
                    .handle(Priority.PROBLEM, e1.getLocalizedMessage(), e1);
        }

        // long t1 = System.currentTimeMillis();
        // System.out.println("buildBoundingGeometry took: " + (t1 - t0));
        return g;
    }

}
