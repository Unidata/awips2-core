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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.WKBReader;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery.QueryLanguage;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.drawables.JTSCompiler;
import com.raytheon.uf.viz.core.drawables.JTSCompiler.JTSGeometryData;
import com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
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
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ShadeableCapability;
import com.raytheon.uf.viz.core.rsc.interrogation.Interrogatable;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogateMap;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogationKey;
import com.raytheon.uf.viz.core.rsc.interrogation.Interrogator;
import com.raytheon.uf.viz.core.rsc.interrogation.StringInterrogationKey;
import com.raytheon.uf.viz.core.spatial.GeometryCache;

/**
 * Databased map resource for line and polygon data
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 19, 2009           randerso  Initial creation
 * Sep 18, 2012  1019     randerso  improved error handling
 * Aug 12, 2013  1133     bsteffen  Better error handling for invalid polygons
 *                                  in map resource.
 * Nov 06, 2013  2361     njensen   Prepopulate fields in initInternal instead
 *                                  of constructor for speed
 * Feb 18, 2014  2819     randerso  Removed unnecessary clones of geometries
 * Apr 09, 2014  2997     randerso  Replaced buildEnvelope with
 *                                  buildBoundingGeometry
 * May 15, 2014  2820     bsteffen  Implement Interrogatable
 * Jul 25, 2014  3447     bclement  reset map query job on dispose
 * Aug 01, 2014  3471     mapeters  Updated deprecated createShadedShape()
 *                                  calls.
 * Aug 11, 2014  3459     randerso  Cleaned up MapQueryJob implementation
 * Aug 13, 2014  3492     mapeters  Updated deprecated createWireframeShape()
 *                                  calls.
 * Oct 23, 2014  3685     randerso  Fix nullPointer if shadingField contains a
 *                                  null
 * Nov 04, 2015  5070     randerso  Change map resources to use a preference
 *                                  based font Move management of font
 *                                  magnification into AbstractMapResource
 * Feb 27, 2818  7012     tgurney   Dedupe map query fields
 * Mar 15, 2018  6967     randerso  Set paint status to incomplete until first
 *                                  painted
 * May 19, 2021  8468     randerso  Catch TopologyException from
 *                                  getInteriorPoint() and place label at the
 *                                  point of the exception.
 *
 * </pre>
 *
 * @author randerso
 */
public class DbMapResource
        extends AbstractDbMapResource<DbMapResourceData, MapDescriptor>
        implements Interrogatable {

    /**
     * A key to be used in
     * {@link #interrogate(ReferencedCoordinate, DataTime, InterrogationKey...)}
     * for retrieving the current label or null if the geometries are unlabeled.
     */
    public static final InterrogationKey<String> LABEL_KEY = new StringInterrogationKey<>(
            "label", String.class);

    private static final String GID = "gid";

    /**
     * at time of writing this is the density multiplier used to determine if a
     * label should be drawn in ZoneSelectorResource
     */
    private static final int BASE_DENSITY_MULT = 50;

    protected class LabelNode {
        private final Rectangle2D rect;

        private final String label;

        private final double[] location;

        public LabelNode(String label, Coordinate c, IGraphicsTarget target,
                IFont font) {
            this.label = label;
            this.location = descriptor.worldToPixel(new double[] { c.x, c.y });
            DrawableString ds = new DrawableString(label, null);
            ds.font = font;
            rect = target.getStringsBounds(ds);
        }

        /**
         * @return the rect
         */
        public Rectangle2D getRect() {
            return rect;
        }

        /**
         * @return the label
         */
        public String getLabel() {
            return label;
        }

        /**
         * @return the location
         */
        public double[] getLocation() {
            return location;
        }
    }

    private class Request extends AbstractMapRequest<DbMapResource> {

        private Random rand = new Random(System.currentTimeMillis());

        private IMapDescriptor descriptor;

        private String geomField;

        private String labelField;

        private String shadingField;

        private Map<Object, RGB> colorMap;

        Request(IGraphicsTarget target, IMapDescriptor descriptor,
                DbMapResource rsc, Geometry boundingGeom, String geomField,
                String labelField, String shadingField,
                Map<Object, RGB> colorMap) {
            super(target, rsc, boundingGeom);
            this.descriptor = rsc.getDescriptor();
            this.geomField = geomField;
            this.labelField = labelField;
            this.shadingField = shadingField;
            this.colorMap = colorMap;
        }

        RGB getColor(Object key) {
            if (colorMap == null) {
                colorMap = new HashMap<>();
            }
            RGB color = colorMap.get(key);
            if (color == null) {
                color = new RGB(rand.nextInt(206) + 50, rand.nextInt(206) + 50,
                        rand.nextInt(206) + 50);
                colorMap.put(key, color);
            }

            return color;
        }
    }

    private class Result extends AbstractMapResult {
        public IWireframeShape outlineShape;

        public List<LabelNode> labels;

        public IShadedShape shadedShape;

        public Map<Object, RGB> colorMap;

        private Result(Request request) {
            super(request);
        }

        @Override
        public void dispose() {
            if (outlineShape != null) {
                outlineShape.dispose();
                outlineShape = null;
            }

            if (shadedShape != null) {
                shadedShape.dispose();
                shadedShape = null;
            }

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
            String table = resourceData.getTable();
            List<String> constraints = new ArrayList<>();
            if (resourceData.getConstraints() != null) {
                constraints
                        .addAll(Arrays.asList(resourceData.getConstraints()));
            }
            Set<String> fields = new HashSet<>();
            fields.add(GID);
            if (req.labelField != null) {
                fields.add(req.labelField.toLowerCase());
            }
            if (req.shadingField != null) {
                fields.add(req.shadingField.toLowerCase());
            }

            if (resourceData.getColumns() != null) {
                for (ColumnDefinition column : resourceData.getColumns()) {
                    if (fields.contains(column.getName().toLowerCase())) {
                        fields.remove(column.getName().toLowerCase());
                    }
                    fields.add(column.toString());
                }
            }

            double[] lev = getLevels();
            QueryResult mappedResult = DbMapQueryFactory
                    .getMapQuery(resourceData.getTable(),
                            getGeomField(lev[lev.length - 1]))
                    .queryWithinGeometry(req.getBoundingGeom(),
                            new ArrayList<>(fields), constraints);
            Map<Integer, Geometry> gidMap = new HashMap<>(
                    mappedResult.getResultCount() * 2);
            List<Integer> toRequest = new ArrayList<>(
                    mappedResult.getResultCount());
            for (int i = 0; i < mappedResult.getResultCount(); ++i) {
                if (checkCanceled(result)) {
                    return;
                }

                int gid = ((Number) mappedResult.getRowColumnValue(i, GID))
                        .intValue();
                Geometry geom = GeometryCache.getGeometry(table,
                        Integer.toString(gid), req.geomField);
                if (geom != null) {
                    gidMap.put(gid, geom);
                } else {
                    toRequest.add(gid);
                }
            }

            if (!toRequest.isEmpty()) {
                WKBReader wkbReader = new WKBReader();
                StringBuilder geomQuery = new StringBuilder();
                geomQuery.append("SELECT ").append(GID).append(", ST_AsBinary(")
                        .append(req.geomField).append(") as ")
                        .append(req.geomField).append(" FROM ").append(table)
                        .append(" WHERE ").append(GID).append(" IN (");
                Integer first = toRequest.get(0);
                geomQuery.append('\'').append(first).append('\'');
                for (int i = 1; i < toRequest.size(); ++i) {
                    Integer gid = toRequest.get(i);
                    geomQuery.append(",'").append(gid).append('\'');
                }
                geomQuery.append(");");

                if (checkCanceled(result)) {
                    return;
                }
                QueryResult geomResults = DirectDbQuery.executeMappedQuery(
                        geomQuery.toString(), "maps", QueryLanguage.SQL);
                for (int i = 0; i < geomResults.getResultCount(); ++i) {
                    if (checkCanceled(result)) {
                        return;
                    }

                    int gid = ((Number) geomResults.getRowColumnValue(i, 0))
                            .intValue();
                    Geometry g = null;
                    Object obj = geomResults.getRowColumnValue(i, 1);
                    if (obj instanceof byte[]) {
                        byte[] wkb = (byte[]) obj;
                        g = wkbReader.read(wkb);
                    } else {
                        statusHandler.handle(Priority.ERROR,
                                "Expected byte[] received "
                                        + obj.getClass().getName() + ": "
                                        + obj.toString() + "\n  table=\""
                                        + resourceData.getTable() + "\"");
                    }
                    gidMap.put(gid, g);
                    GeometryCache.putGeometry(table, Integer.toString(gid),
                            req.geomField, g);
                }
            }

            IWireframeShape newOutlineShape = req.getTarget()
                    .createWireframeShape(false, req.descriptor);

            List<LabelNode> newLabels = new ArrayList<>();

            IShadedShape newShadedShape = null;
            if (req.shadingField != null) {
                newShadedShape = req.getTarget().createShadedShape(false,
                        req.descriptor.getGridGeometry());
            }

            JTSCompiler jtsCompiler = new JTSCompiler(newShadedShape,
                    newOutlineShape, req.descriptor);
            JTSGeometryData geomData = jtsCompiler.createGeometryData();
            geomData.setWorldWrapCorrect(true);
            geomData.setPointStyle(PointStyle.CROSS);

            List<Geometry> resultingGeoms = new ArrayList<>(
                    mappedResult.getResultCount());

            Set<String> unlabelablePoints = new HashSet<>(0);

            int numPoints = 0;
            for (int i = 0; i < mappedResult.getResultCount(); ++i) {
                if (checkCanceled(result)) {
                    return;
                }

                int gid = ((Number) mappedResult.getRowColumnValue(i, GID))
                        .intValue();
                Geometry g = gidMap.get(gid);
                Object obj = null;

                if (req.labelField != null) {
                    obj = mappedResult.getRowColumnValue(i,
                            req.labelField.toLowerCase());
                }

                if ((obj != null) && (g != null)) {
                    String label;
                    if (obj instanceof BigDecimal) {
                        label = Double.toString(((Number) obj).doubleValue());
                    } else {
                        label = obj.toString();
                    }
                    int numGeometries = g.getNumGeometries();
                    List<Geometry> gList = new ArrayList<>(numGeometries);
                    for (int polyNum = 0; polyNum < numGeometries; polyNum++) {
                        Geometry poly = g.getGeometryN(polyNum);
                        gList.add(poly);
                    }
                    // Sort polygons in g so biggest comes first.
                    Collections.sort(gList, new Comparator<Geometry>() {
                        @Override
                        public int compare(Geometry g1, Geometry g2) {
                            return (int) Math.signum(g2.getEnvelope().getArea()
                                    - g1.getEnvelope().getArea());
                        }
                    });

                    for (Geometry poly : gList) {
                        try {
                            Coordinate c = null;
                            try {
                                c = poly.getInteriorPoint().getCoordinate();
                            } catch (TopologyException e) {
                                c = e.getCoordinate();
                            }
                            if (c != null) {
                                LabelNode node = new LabelNode(label, c,
                                        req.getTarget(), req.getResource()
                                                .getFont(req.getTarget()));
                                newLabels.add(node);
                            }
                        } catch (TopologyException e) {
                            statusHandler.handle(Priority.VERBOSE,
                                    "Invalid geometry cannot be labeled: "
                                            + label,
                                    e);
                            unlabelablePoints.add(label);
                        }
                    }
                }

                if (g != null) {
                    numPoints += g.getNumPoints();
                    resultingGeoms.add(g);
                    if (req.shadingField != null) {
                        g.setUserData(mappedResult.getRowColumnValue(i,
                                req.shadingField.toLowerCase()));
                    }
                }
            }

            if (!unlabelablePoints.isEmpty()) {
                statusHandler.handle(Priority.WARN,
                        "Invalid geometries cannot be labeled: "
                                + unlabelablePoints.toString());
            }

            newOutlineShape.allocate(numPoints);

            for (Geometry g : resultingGeoms) {
                RGB color = null;
                Object shadedField = g.getUserData();
                color = req.getColor(shadedField);
                geomData.setGeometryColor(color);

                try {
                    jtsCompiler.handle(g, geomData);
                } catch (VizException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error reprojecting map outline", e);
                }
            }

            newOutlineShape.compile();

            if (req.shadingField != null) {
                newShadedShape.compile();
            }

            result.outlineShape = newOutlineShape;
            result.labels = newLabels;
            result.shadedShape = newShadedShape;
            result.colorMap = req.colorMap;
        }
    }

    protected IWireframeShape outlineShape;

    protected List<LabelNode> labels;

    protected IShadedShape shadedShape;

    protected Map<Object, RGB> colorMap;

    protected double lastSimpLev;

    protected String lastLabelField;

    protected String lastShadingField;

    private MapQueryJob queryJob;

    /**
     * Constructor
     *
     * @param data
     * @param loadProperties
     */
    public DbMapResource(DbMapResourceData data,
            LoadProperties loadProperties) {
        super(data, loadProperties);
        queryJob = new MapQueryJob();
    }

    @Override
    protected void disposeInternal() {
        queryJob.stop();

        if (outlineShape != null) {
            outlineShape.dispose();
            outlineShape = null;
        }

        if (shadedShape != null) {
            shadedShape.dispose();
            shadedShape = null;
        }
        super.disposeInternal();
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        super.initInternal(target);

        // Prepopulate fields in initInternal since this is not on the UI
        // thread
        getGeometryType();
        getLabelFields();
        getLevels();

        getCapability(ShadeableCapability.class).setAvailableShadingFields(
                getLabelFields().toArray(new String[0]));
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

        double simpLev = getSimpLev(paintProps);

        String labelField = getCapability(LabelableCapability.class)
                .getLabelField();
        boolean isLabeled = labelField != null;

        String shadingField = getCapability(ShadeableCapability.class)
                .getShadingField();

        boolean isShaded = isPolygonal() && (shadingField != null);

        double labelMagnification = getCapability(MagnificationCapability.class)
                .getMagnification();

        if ((simpLev < lastSimpLev)
                || (isLabeled && !labelField.equals(lastLabelField))
                || (isShaded && !shadingField.equals(lastShadingField))
                || (lastExtent == null) || !lastExtent.getEnvelope().contains(
                        clipToProjExtent(screenExtent).getEnvelope())) {
            if (!paintProps.isZooming()) {
                PixelExtent expandedExtent = getExpandedExtent(screenExtent);
                Geometry boundingGeom = buildBoundingGeometry(expandedExtent,
                        worldToScreenRatio, kmPerPixel);

                queryJob.queueRequest(new Request(target, descriptor, this,
                        boundingGeom, getGeomField(simpLev), labelField,
                        shadingField, colorMap));
                lastExtent = expandedExtent;
                lastSimpLev = simpLev;
                lastLabelField = labelField;
                lastShadingField = shadingField;
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

            if (outlineShape != null) {
                outlineShape.dispose();
            }

            if (shadedShape != null) {
                shadedShape.dispose();
            }

            outlineShape = result.outlineShape;
            labels = result.labels;
            shadedShape = result.shadedShape;
            colorMap = result.colorMap;
        }

        float alpha = paintProps.getAlpha();

        if ((shadedShape != null) && shadedShape.isDrawable() && isShaded) {
            float opacity = getCapability(ShadeableCapability.class)
                    .getOpacity();
            target.drawShadedShape(shadedShape, alpha * opacity);
        }

        if ((outlineShape != null) && outlineShape.isDrawable()
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            target.drawWireframeShape(outlineShape,
                    getCapability(ColorableCapability.class).getColor(),
                    getCapability(OutlineCapability.class).getOutlineWidth(),
                    getCapability(OutlineCapability.class).getLineStyle(),
                    alpha);
        } else if ((outlineShape == null)
                && getCapability(OutlineCapability.class).isOutlineOn()) {
            issueRefresh();
        }

        if ((labels != null) && isLabeled && (labelMagnification != 0)) {
            double offsetX = getCapability(LabelableCapability.class)
                    .getxOffset() * worldToScreenRatio;
            double offsetY = getCapability(LabelableCapability.class)
                    .getyOffset() * worldToScreenRatio;
            RGB color = getCapability(ColorableCapability.class).getColor();
            IExtent extent = paintProps.getView().getExtent();
            List<DrawableString> strings = new ArrayList<>(labels.size());
            List<LabelNode> selectedNodes = new ArrayList<>(labels.size());
            List<IExtent> extents = new ArrayList<>();
            String lastLabel = null;
            // get min distance
            double density = this.getCapability(DensityCapability.class)
                    .getDensity();
            double minScreenDistance = Double.MAX_VALUE;
            if (density > 0) {
                minScreenDistance = (worldToScreenRatio * BASE_DENSITY_MULT)
                        / density;
            }

            // find which nodes to draw
            for (LabelNode node : labels) {
                if (extent.contains(node.location)) {
                    if (shouldDraw(node, selectedNodes, minScreenDistance)) {
                        selectedNodes.add(node);
                    }
                }
            }

            // create drawable strings for selected nodes
            for (LabelNode node : selectedNodes) {
                DrawableString string = new DrawableString(node.label, color);
                string.setCoordinates(node.location[0] + offsetX,
                        node.location[1] - offsetY);
                string.font = getFont(target);
                string.horizontalAlignment = HorizontalAlignment.CENTER;
                string.verticallAlignment = VerticalAlignment.MIDDLE;
                boolean add = true;

                IExtent strExtent = new PixelExtent(node.location[0],
                        node.location[0]
                                + (node.rect.getWidth() * worldToScreenRatio),
                        node.location[1],
                        node.location[1]
                                + ((node.rect.getHeight() - node.rect.getY())
                                        * worldToScreenRatio));

                if ((lastLabel != null) && lastLabel.equals(node.label)) {
                    // check intersection of extents
                    for (IExtent ext : extents) {
                        if (ext.intersects(strExtent)) {
                            add = false;
                            break;
                        }
                    }
                } else {
                    extents.clear();
                }
                lastLabel = node.label;
                extents.add(strExtent);

                if (add) {
                    strings.add(string);
                }
            }

            target.drawStrings(strings);
        }
    }

    /**
     * Checks if the potentialNode is too close to an already selected node
     *
     * @param potentialNode
     * @param selectedDrawList
     * @param minScreenDistance
     * @return true if should draw
     */
    protected boolean shouldDraw(LabelNode potentialNode,
            List<LabelNode> selectedDrawList, double minScreenDistance) {
        boolean rval = false;

        double x = potentialNode.getLocation()[0];
        double y = potentialNode.getLocation()[1];
        double minDistance = Double.MAX_VALUE;

        // check already selected labels
        for (LabelNode node : selectedDrawList) {
            double distance = Math.abs(node.getLocation()[0] - x)
                    + Math.abs(node.getLocation()[1] - y);
            minDistance = Math.min(distance, minDistance);
        }

        if (minDistance >= minScreenDistance) {
            rval = true;
        } else {
            rval = false;
        }

        return rval;
    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        super.project(crs);

        if (this.outlineShape != null) {
            outlineShape.dispose();
            this.outlineShape = null;
        }

        if (this.shadedShape != null) {
            shadedShape.dispose();
            this.shadedShape = null;
        }
    }

    @Override
    public Set<InterrogationKey<?>> getInterrogationKeys() {
        HashSet<InterrogationKey<?>> keys = new HashSet<>();
        /* the geometry at current simplification level */
        keys.add(Interrogator.GEOMETRY);
        /* the label if labeling is turned on */
        keys.add(LABEL_KEY);
        /* Also allow them to pull out any possible label fields */
        for (String label : getLabelFields()) {
            keys.add(new StringInterrogationKey<>(label, String.class));
        }
        /* Allow requesting different simplification levels. */
        for (double level : getLevels()) {
            keys.add(new StringInterrogationKey<>(getGeomField(level),
                    Geometry.class));
        }
        return keys;
    }

    @Override
    public InterrogateMap interrogate(ReferencedCoordinate coordinate,
            DataTime time, InterrogationKey<?>... keys) {
        /* Need to separate the geometry keys from the label keys */
        List<InterrogationKey<?>> keyList = Arrays.asList(keys);
        Map<InterrogationKey<Geometry>, String> geomKeys = new HashMap<>();
        Map<InterrogationKey<String>, String> labelKeys = new HashMap<>();
        if (keyList.contains(Interrogator.GEOMETRY) && (lastSimpLev != 0)) {
            geomKeys.put(Interrogator.GEOMETRY, getGeomField(lastSimpLev));
        }
        if (keyList.contains(LABEL_KEY)) {
            String labelField = getCapability(LabelableCapability.class)
                    .getLabelField();
            if (labelField != null) {
                labelKeys.put(LABEL_KEY, labelField);
            }
        }
        for (String label : getLabelFields()) {
            StringInterrogationKey<String> key = new StringInterrogationKey<>(
                    label, String.class);
            if (keyList.contains(key)) {
                labelKeys.put(key, label);
            }
        }
        /* Allow requesting different simplification levels. */
        for (double level : getLevels()) {
            String geomField = getGeomField(level);
            StringInterrogationKey<Geometry> key = new StringInterrogationKey<>(
                    geomField, Geometry.class);
            if (keyList.contains(key)) {
                geomKeys.put(key, geomField);
            }
        }
        InterrogateMap map = new InterrogateMap();
        if (labelKeys.isEmpty() && geomKeys.isEmpty()) {
            return map;
        }
        /* Prepare a db request */
        Point boundingGeom = null;
        try {
            boundingGeom = new GeometryFactory()
                    .createPoint(coordinate.asLatLon());
        } catch (TransformException | FactoryException e) {
            statusHandler
                    .error("Unable to transform coordinate for interrogate", e);
            return map;
        }
        String geomField = null;
        List<String> fields = new ArrayList<>(labelKeys.values());
        if (geomKeys.isEmpty()) {
            geomField = getGeomField(lastSimpLev);
        } else {
            geomField = geomKeys.values().iterator().next();
            fields.add(GID);
        }
        List<String> constraints = new ArrayList<>();
        if (resourceData.getConstraints() != null) {
            constraints.addAll(Arrays.asList(resourceData.getConstraints()));
        }
        QueryResult mappedResult = null;
        try {
            mappedResult = DbMapQueryFactory
                    .getMapQuery(resourceData.getTable(), geomField)
                    .queryWithinGeometry(boundingGeom, fields, constraints);
        } catch (VizException e) {
            statusHandler.error("Unable to query database for interrogate", e);
            return map;
        }
        if (mappedResult.getResultCount() == 0) {
            return map;
        }
        /* Process all labels */
        for (Entry<InterrogationKey<String>, String> entry : labelKeys
                .entrySet()) {
            map.put(entry.getKey(), mappedResult
                    .getRowColumnValue(0, entry.getValue()).toString());
        }
        /* Process all geoms */
        for (Entry<InterrogationKey<Geometry>, String> entry : geomKeys
                .entrySet()) {
            Geometry geom = GeometryCache.getGeometry(resourceData.getTable(),
                    mappedResult.getRowColumnValue(0, GID).toString(),
                    entry.getValue());
            map.put(entry.getKey(), geom);
        }
        return map;
    }

}
