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

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.graphics.Rectangle;
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
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.maps.rsc.AbstractDbMapResourceData.ColumnDefinition;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.LabelableCapability;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Base class for database map resources
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 10, 2011           randerso  Initial creation
 * Apr 17, 2014  2997     randerso  Moved buildBoundingGeometry up from
 *                                  DbMapResource
 * Aug 21, 2014  3459     randerso  Restructured Map resource class hierarchy
 * Jan 29, 2015  4062     randerso  Added a buffer to bounding Geometry
 * Mar 15, 2018  6967     randerso  Code cleanup
 *
 * </pre>
 *
 * @author randerso
 *
 * @param <T>
 *            the resource data type
 * @param <D>
 *            the descriptor type
 */

public abstract class AbstractDbMapResource<T extends AbstractDbMapResourceData, D extends IMapDescriptor>
        extends StyledMapResource<T, D> {

    private List<String> labelFields;

    private double[] levels;

    private String geometryType;

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
                this.labelFields = DbMapQueryFactory
                        .getMapQuery(resourceData.getTable(),
                                resourceData.getGeomField())
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

    /**
     * @return the levels
     */
    protected double[] getLevels() {
        if (levels == null) {
            try {
                List<Double> results = DbMapQueryFactory
                        .getMapQuery(resourceData.getTable(),
                                resourceData.getGeomField())
                        .getLevels();
                levels = new double[results.size()];
                for (int i = 0; i < results.size(); i++) {
                    levels[i] = results.get(i);
                }
                Arrays.sort(levels);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error querying available levels", e);
            }
        }

        return levels;
    }

    /**
     * Determine simplification level
     *
     * @param paintProps
     * @return the current simplification level to use
     */
    protected double getSimpLev(PaintProperties paintProps) {
        PixelExtent screenExtent = (PixelExtent) paintProps.getView()
                .getExtent();

        // compute an estimate of degrees per pixel
        double yc = screenExtent.getCenter()[1];
        double x1 = screenExtent.getMinX();
        double x2 = screenExtent.getMaxX();
        double[] c1 = descriptor.pixelToWorld(new double[] { x1, yc });
        double[] c2 = descriptor.pixelToWorld(new double[] { x2, yc });
        Rectangle canvasBounds = paintProps.getCanvasBounds();
        int screenWidth = canvasBounds.width;
        double dppX = Math.abs(c2[0] - c1[0]) / screenWidth;

        double[] levels = getLevels();
        double simpLev = levels[0];
        for (double level : getLevels()) {
            if (dppX < level) {
                break;
            }
            simpLev = level;
        }
        return simpLev;
    }

    protected String getGeomField(double simpLev) {
        DecimalFormat df = new DecimalFormat("0.######");
        String suffix = "_"
                + StringUtils.replaceChars(df.format(simpLev), '.', '_');

        return resourceData.getGeomField() + suffix;
    }

    protected String getGeometryType() {
        if (geometryType == null) {
            try {
                geometryType = DbMapQueryFactory
                        .getMapQuery(resourceData.getTable(),
                                resourceData.getGeomField())
                        .getGeometryType();
            } catch (Throwable e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }

        return geometryType;
    }

    protected boolean isPuntal() {
        return getGeometryType().endsWith("POINT");
    }

    protected boolean isLineal() {
        return getGeometryType().endsWith("LINESTRING");
    }

    protected boolean isPolygonal() {
        return getGeometryType().endsWith("POLYGON");
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
        int maxHorDivisions = (int) Math
                .ceil(extent.getWidth() / SPEED_UP / worldToScreenRatio);
        int maxVertDivisions = (int) Math
                .ceil(extent.getHeight() / SPEED_UP / worldToScreenRatio);

        Geometry g = null;
        try {
            g = EnvelopeIntersection.createEnvelopeIntersection(sourceEnvelope,
                    targetEnvelope, threshold, maxHorDivisions,
                    maxVertDivisions);

            CoordinateSequenceTransformer cst = new DefaultCoordinateSequenceTransformer(
                    PackedCoordinateSequenceFactory.DOUBLE_FACTORY);
            final GeometryTransformer transformer = new GeometryTransformer(
                    cst);
            MathTransform toLL = MapUtil.getTransformToLatLon(targetCRS);
            transformer.setMathTransform(toLL);

            g = transformer.transform(g);
        } catch (Exception e1) {
            statusHandler.handle(Priority.PROBLEM, e1.getLocalizedMessage(),
                    e1);
        }

        // Add just a little buffer to get past EnvelopeIntersection limiting
        // us to 179.99999 instead of going clear to 180
        g = g.buffer(0.0001);

        // long t1 = System.currentTimeMillis();
        // System.out.println("buildBoundingGeometry took: " + (t1 - t0));
        return g;
    }

}
