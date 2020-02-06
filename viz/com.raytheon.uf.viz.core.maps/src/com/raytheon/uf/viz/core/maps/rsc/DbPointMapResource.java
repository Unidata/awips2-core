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

import java.awt.geom.Rectangle2D;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.pointdata.vadriver.VA_Advanced;
import com.raytheon.uf.common.pointdata.vadriver.VA_Advanced.IVAMonitor;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.PointStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.MapDescriptor;
import com.raytheon.uf.viz.core.maps.jobs.AbstractMapQueryJob;
import com.raytheon.uf.viz.core.maps.jobs.AbstractMapRequest;
import com.raytheon.uf.viz.core.maps.jobs.AbstractMapResult;
import com.raytheon.uf.viz.core.maps.rsc.AbstractDbMapResourceData.ColumnDefinition;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.DensityCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.LabelableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.PointCapability;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBReader;

/**
 * Databased map resource for point data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 19, 2009           randerso  Initial creation
 * Apr 09, 2014  2997     randerso  Replaced buildEnvelope with
 *                                  buildBoundingGeometry
 * May 14, 2014  3074     bsteffen  Remove WORD_WRAP TextStyle and handle
 *                                  wrapping locally.
 * Aug 11, 2014  3459     randerso  Cleaned up MapQueryJob implementation
 * Nov 04, 2015  5070     randerso  Change map resources to use a preference
 *                                  based font Move management of font
 *                                  magnification into AbstractMapResource
 * Mar 15, 2018  6967     randerso  Set paint status to incomplete until first
 *                                  painted
 *
 * </pre>
 *
 * @author randerso
 */
public class DbPointMapResource
        extends AbstractDbMapResource<DbPointMapResourceData, MapDescriptor> {

    private class LabelNode {
        private final String label;

        private final ReferencedCoordinate location;

        private Coordinate screenLoc = null;

        private double distance;

        private int goodness;

        LabelNode(String label, Point c) {
            this.label = label;
            this.location = new ReferencedCoordinate(c.getCoordinate());
            try {
                screenLoc = location.asPixel(descriptor.getGridGeometry());
            } catch (Exception e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error converting lat/lon to screen space: "
                                + e.getLocalizedMessage(),
                        e);
            }
        }

        /**
         * @return the location
         */
        public ReferencedCoordinate getLocation() {
            return location;
        }

        /**
         * @return the distance
         */
        public double getDistance() {
            return distance;
        }

        /**
         * @return the screen projected coordinate
         */
        public Coordinate getScreenLocation() {
            return screenLoc;
        }

        /**
         * @param distance
         *            the distance to set
         */
        public void setDistance(double distance) {
            this.distance = distance;
        }

        /**
         * @return the goodness
         */
        public int getGoodness() {
            return goodness;
        }

        /**
         * @param goodness
         *            the goodness to set
         */
        public void setGoodness(int goodness) {
            this.goodness = goodness;
        }
    }

    private class Request extends AbstractMapRequest<DbPointMapResource> {

        private String labelField;

        private String goodnessField;

        public Request(IGraphicsTarget target, DbPointMapResource rsc,
                String labelField, String goodnessField,
                Geometry boundingGeometry) {
            super(target, rsc, boundingGeometry);
            this.labelField = labelField;
            this.goodnessField = goodnessField;
        }

    }

    private class Result extends AbstractMapResult {

        public List<LabelNode> labels;

        private Result(Request request) {
            super(request);
        }

        @Override
        public void dispose() {
            if (labels != null) {
                labels.clear();
                labels = null;
            }
        }

    }

    private class MapQueryJob extends AbstractMapQueryJob<Request, Result> {

        public MapQueryJob() {
            super();
        }

        @Override
        protected Result getNewResult(Request req) {
            return new Result(req);
        }

        @Override
        protected void processRequest(Request req, final Result result)
                throws Exception {
            List<String> columns = new ArrayList<>();
            if (req.labelField != null) {
                columns.add(req.labelField);
            }
            if ((req.goodnessField != null)
                    && (!req.goodnessField.equals(req.labelField))) {
                columns.add(req.goodnessField);
            }
            if (resourceData.getColumns() != null) {
                for (ColumnDefinition column : resourceData.getColumns()) {
                    if (columns.contains(column.getName())) {
                        columns.remove(column.getName());
                    }
                    columns.add(column.toString());
                }
            }
            columns.add("ST_AsBinary(" + resourceData.getGeomField() + ") as "
                    + resourceData.getGeomField());

            List<String> constraints = null;
            if (resourceData.getConstraints() != null) {
                constraints = Arrays.asList(resourceData.getConstraints());
            }

            QueryResult results = DbMapQueryFactory
                    .getMapQuery(resourceData.getTable(),
                            resourceData.getGeomField())
                    .queryWithinGeometry(req.getBoundingGeom(), columns,
                            constraints);

            List<LabelNode> newLabels = new ArrayList<>();

            WKBReader wkbReader = new WKBReader();
            for (int c = 0; c < results.getResultCount(); c++) {
                if (checkCanceled(result)) {
                    return;
                }

                Geometry g = null;
                Object geomObj = results.getRowColumnValue(c,
                        resourceData.getGeomField());
                if (geomObj instanceof byte[]) {
                    byte[] wkb = (byte[]) geomObj;
                    g = wkbReader.read(wkb);
                } else {
                    statusHandler.handle(Priority.ERROR,
                            "Expected byte[] received "
                                    + geomObj.getClass().getName() + ": "
                                    + geomObj.toString());
                }

                if (g != null) {
                    String label = "";
                    if ((req.labelField != null) && (results
                            .getRowColumnValue(c, req.labelField) != null)) {
                        Object r = results.getRowColumnValue(c, req.labelField);
                        if (r instanceof BigDecimal) {
                            label = Double.toString(((Number) r).doubleValue());
                        } else {
                            label = r.toString();
                        }
                    }
                    LabelNode node = new LabelNode(label, g.getCentroid());

                    if (req.goodnessField != null) {
                        node.setGoodness(((Number) results.getRowColumnValue(c,
                                req.goodnessField)).intValue());
                    }
                    newLabels.add(node);
                }
            }

            VA_Advanced distanceCalc = new VA_Advanced(new IVAMonitor() {
                @Override
                public boolean isCanceled() {
                    return checkCanceled(result);
                }
            });

            distanceCalc.setVaWeighting(0.0f);
            Coordinate[] coords = new Coordinate[newLabels.size()];
            Integer[] goodness = new Integer[newLabels.size()];
            Double[] dst = new Double[newLabels.size()];
            for (int j = 0; j < newLabels.size(); j++) {
                coords[j] = newLabels.get(j).getLocation().asLatLon();
                goodness[j] = newLabels.get(j).getGoodness();
                dst[j] = 0d;
            }
            Double[] distances;

            if (req.goodnessField != null) {
                distances = distanceCalc.getVaAdvanced(coords, goodness, dst);
            } else {
                distances = distanceCalc.getVaSimple(coords, dst);
            }

            for (int j = 0; j < newLabels.size(); j++) {
                newLabels.get(j).setDistance(distances[j]);
            }

            result.labels = newLabels;

        }
    }

    private static final int PIXEL_SIZE_HINT = 45;

    private List<LabelNode> labels;

    private String lastLabelField;

    private MapQueryJob queryJob;

    /**
     * Constructor
     *
     * @param data
     * @param loadProperties
     */
    public DbPointMapResource(DbPointMapResourceData data,
            LoadProperties loadProperties) {
        super(data, loadProperties);
        queryJob = new MapQueryJob();
    }

    @Override
    protected void disposeInternal() {
        queryJob.stop();
        super.disposeInternal();
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        PixelExtent screenExtent = (PixelExtent) paintProps.getView()
                .getExtent();
        Rectangle canvasBounds = paintProps.getCanvasBounds();
        int screenWidth = canvasBounds.width;
        double worldToScreenRatio = screenExtent.getWidth() / screenWidth;

        int displayWidth = (int) (descriptor.getMapWidth()
                * paintProps.getZoomLevel());
        double kmPerPixel = (displayWidth / screenWidth) / 1000.0;

        double magnification = getCapability(MagnificationCapability.class)
                .getMagnification();
        double density = getCapability(DensityCapability.class).getDensity();

        double displayHintSize = PIXEL_SIZE_HINT * magnification;
        double threshold = (displayHintSize * kmPerPixel) / density;

        String labelField = getCapability(LabelableCapability.class)
                .getLabelField();
        boolean isLabeled = labelField != null;
        if ((isLabeled && !labelField.equals(lastLabelField))
                || (lastExtent == null) || !lastExtent.getEnvelope().contains(
                        clipToProjExtent(screenExtent).getEnvelope())) {
            if (!paintProps.isZooming()) {
                PixelExtent expandedExtent = getExpandedExtent(screenExtent);
                Geometry boundingGeom = buildBoundingGeometry(expandedExtent,
                        worldToScreenRatio, kmPerPixel);

                queryJob.queueRequest(new Request(target, this, labelField,
                        resourceData.getGoodnessField(), boundingGeom));

                lastExtent = expandedExtent;
                lastLabelField = labelField;
            }
        }

        // if queryJob is running
        if (queryJob.getState() != Job.NONE) {
            updatePaintStatus(PaintStatus.INCOMPLETE);
        }

        Result result = queryJob.getLatestResult();

        // if query job has just completed
        if (result != null) {
            updatePaintStatus(PaintStatus.PAINTED);

            if (result.isFailed()) {
                // force to re-query when re-enabled
                lastExtent = null;
                throw new VizException(
                        "Error processing map query request for: "
                                + result.getName(),
                        result.getCause());
            }
            labels = result.labels;
        }

        if (labels != null) {

            RGB color = getCapability(ColorableCapability.class).getColor();
            DrawableString test = new DrawableString("N", color);
            test.font = getFont(target);
            Rectangle2D charSize = target.getStringsBounds(test);
            double charWidth = charSize.getWidth();
            double charHeight = charSize.getHeight();

            double screenToWorldRatio = paintProps.getCanvasBounds().width
                    / paintProps.getView().getExtent().getWidth();

            HorizontalAlignment horizAlign = HorizontalAlignment.LEFT;
            double offsetX = charWidth / 2.0 / screenToWorldRatio;
            double offsetY = charHeight / screenToWorldRatio;

            PointStyle pointStyle = getCapability(PointCapability.class)
                    .getPointStyle();
            if (pointStyle.equals(PointStyle.NONE)) {
                horizAlign = HorizontalAlignment.CENTER;
                offsetX = 0;
                offsetY = 0;
            }
            offsetX += getCapability(LabelableCapability.class).getxOffset()
                    / screenToWorldRatio;
            offsetY -= getCapability(LabelableCapability.class).getyOffset()
                    / screenToWorldRatio;
            List<double[]> points = new ArrayList<>();
            List<DrawableString> strings = new ArrayList<>();
            for (LabelNode node : labels) {
                try {
                    if (node.getDistance() > threshold) {
                        Coordinate c = node.getScreenLocation();
                        if ((c != null) && screenExtent.contains(c.x, c.y)) {
                            points.add(new double[] { c.x, c.y, 0.0 });
                            if (isLabeled && (magnification != 0)) {
                                String label = node.label;
                                label = label.replaceAll("([^\n]{3}\\S*)\\s+",
                                        "$1\n");
                                DrawableString str = new DrawableString(label,
                                        color);
                                str.setCoordinates(c.x + offsetX,
                                        c.y + offsetY);
                                str.horizontalAlignment = horizAlign;
                                str.verticallAlignment = VerticalAlignment.MIDDLE;
                                str.font = getFont(target);
                                strings.add(str);
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new VizException("Error transforming", e);
                }
            }

            target.drawPoints(points, color, pointStyle, 1.0f);
            target.drawStrings(strings);
        }
    }
}
