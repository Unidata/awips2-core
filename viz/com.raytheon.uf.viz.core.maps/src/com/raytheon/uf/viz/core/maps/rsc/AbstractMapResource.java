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
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;

/**
 * Abstract base class for all maps
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 10, 2010            randerso    Initial creation
 * Aug 21, 2014  #3459     randerso    Restructured Map resource class hierarchy
 * Nov 04, 2015  #5070     randerso    Change map resources to use a preference based font
 *                                     Move management of font magnification into AbstractMapResource
 * May 31, 2018  6562      tgurney     T extends AbstractMapResourceData
 * Aug 20, 2018  6891      tgurney     Set the font magnification every time
 *                                     determineFont is called
 *
 * </pre>
 *
 * @author randerso
 */

public abstract class AbstractMapResource<T extends AbstractMapResourceData, D extends IMapDescriptor>
        extends AbstractVizResource<T, D> implements IResourceDataChanged {

    protected static final String LABEL_FONT_ID = "com.raytheon.uf.viz.core.mapLabelFont";

    protected static final double EXPANSION_FACTOR = 0.25;

    // Unitless fudge factor to adjust performance.
    // A higher value increases the threshold and reduces the max divisions
    // passed into EnveleopeIntersection, making it faster but less accurate.
    // Values < 4 get very slow. Values > 16 start to cause noticeable drop outs
    // in the map geometry.
    protected static final double SPEED_UP = 8;

    protected IFont font;

    protected PixelExtent lastExtent;

    protected PixelExtent projExtent;

    /**
     * @param resourceData
     * @param loadProperties
     */
    protected AbstractMapResource(T resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        resourceData.addChangeListener(this);
    }

    @Override
    protected void disposeInternal() {
        resourceData.removeChangeListener(this);

        if (font != null) {
            font.dispose();
            font = null;
        }

        lastExtent = null;
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
    }

    protected IFont determineFont(IGraphicsTarget target) {
        double magnification = getCapability(MagnificationCapability.class)
                .getMagnification();
        if (font == null) {
            IFont font = target.initializeFont(LABEL_FONT_ID);
            font.setScaleFont(false);
            font.setSmoothing(false);

            this.font = font;
        }
        font.setMagnification((float) magnification);
        return font;
    }

    @Override
    public void resourceChanged(ChangeType type, Object object) {
        if (type == ChangeType.CAPABILITY) {

            if (object instanceof MagnificationCapability) {
                if (font != null) {
                    font.dispose();
                    font = null;
                }
            }
        }
        issueRefresh();
    }

    @Override
    public void setDescriptor(D descriptor) {
        super.setDescriptor(descriptor);

        projExtent = new PixelExtent(
                descriptor.getGridGeometry().getGridRange());

        lastExtent = null;
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        super.project(crs);

        projExtent = new PixelExtent(
                descriptor.getGridGeometry().getGridRange());

        lastExtent = null;
    }

    /**
     * @param screenExtent
     * @return
     */
    protected PixelExtent getExpandedExtent(PixelExtent screenExtent) {
        PixelExtent expandedExtent = screenExtent.clone();
        expandedExtent.getEnvelope().expandBy(
                expandedExtent.getWidth() * EXPANSION_FACTOR,
                expandedExtent.getHeight() * EXPANSION_FACTOR);

        return clipToProjExtent(expandedExtent);
    }

    protected PixelExtent clipToProjExtent(PixelExtent extent) {
        Envelope e = extent.getEnvelope()
                .intersection(projExtent.getEnvelope());
        PixelExtent clipped = new PixelExtent(e.getMinX(), e.getMaxX(),
                e.getMinY(), e.getMaxY());
        return clipped;
    }

    @Override
    public String toString() {
        return this.resourceData.toString();
    }

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

        // long t1 = System.currentTimeMillis();
        // System.out.println("buildBoundingGeometry took: " + (t1 - t0));
        return g;
    }

    protected IFont getFont(IGraphicsTarget target) {
        return determineFont(target);
    }

}
