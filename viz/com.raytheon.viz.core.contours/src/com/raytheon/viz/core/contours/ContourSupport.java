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
package com.raytheon.viz.core.contours;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang.ArrayUtils;
import org.eclipse.swt.graphics.RGB;
import org.geotools.coverage.grid.GeneralGridEnvelope;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.CRSCache;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
import com.raytheon.uf.common.geospatial.util.GridGeometryWrapChecker;
import com.raytheon.uf.common.geospatial.util.WorldWrapChecker;
import com.raytheon.uf.common.numeric.DataUtilities;
import com.raytheon.uf.common.numeric.DataUtilities.MinMax;
import com.raytheon.uf.common.numeric.buffer.FloatBufferWrapper;
import com.raytheon.uf.common.numeric.dest.DataDestination;
import com.raytheon.uf.common.numeric.filter.FillValueFilter;
import com.raytheon.uf.common.numeric.filter.InverseFillValueFilter;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.numeric.source.OffsetDataSource;
import com.raytheon.uf.common.status.IPerformanceStatusHandler;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.PerformanceStatus;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.BaseLabelingPreferences;
import com.raytheon.uf.common.style.ContourLabelingPreferences;
import com.raytheon.uf.common.style.IncrementLabelingPreferences;
import com.raytheon.uf.common.style.ValuesLabelingPreferences;
import com.raytheon.uf.common.style.contour.ContourPreferences;
import com.raytheon.uf.common.util.GridUtil;
import com.raytheon.uf.common.wxmath.Constants;
import com.raytheon.uf.common.wxmath.DistFilter;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.viz.core.contours.cache.SubGridCacheKey;
import com.raytheon.viz.core.contours.util.ContourContainer;
import com.raytheon.viz.core.contours.util.FortConBuf;
import com.raytheon.viz.core.contours.util.FortConConfig;
import com.raytheon.viz.core.contours.util.StreamLineContainer;
import com.raytheon.viz.core.contours.util.StreamLineContainer.StreamLinePoint;
import com.raytheon.viz.core.contours.util.StrmPak;
import com.raytheon.viz.core.contours.util.StrmPakConfig;
import com.raytheon.viz.core.interval.XFormFunctions;

/**
 * Provides contouring wrapper.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ ----------------------------------------
 * Oct 22, 2007           chammack     Initial Creation.
 * May 26, 2009  2172     chammack     Use zoomLevel to calculate label spacing
 * Apr 26, 2010  4583     rjpeter      Replaced fortran fortconbuf with java
 *                                     port.
 * Mar 04, 2011  7747     njensen      Cached subgrid envelopes
 * Jul 09, 2012  14940    M.Porricelli Adjust arrow size for streamlines
 * Feb 15, 2013  1638     mschenke     Moved edex.common Util functions into
 *                                     common Util
 * Jun 26, 2013  1999     dgilling     Replace native fortran strmpak call
 *                                     with java port.
 * Jul 18, 2013  2199     mschenke     Ensured contouring is only occurring
 *                                     over visible area
 * Jul 23, 2013  2157     dgilling     Remove legacy stream line drawing code.
 * Feb 27, 2014  2791     bsteffen     Switch from IDataRecord to DataSource
 * Mar 17, 2015  4261     nabowle      Move performance logging to the
 *                                     performance log.
 * Mar 19, 2015  4292     nabowle      Add contour range using A1 configuration
 *                                     rules.
 * Apr 30, 2018  6697     bsteffen     Support zoomLock.
 * May 07, 2019  65510    ksunil       multiple changes to support customized contour support
 * Jun 27, 2019  65510    ksunil       refactor smoothData
 * Jul 11, 2019  65905    ksunil       fixed the issue with labelFormat within the values tag
 * Jul 31, 2019  66719    ksunil       Make sure the lat is within the +/-90 range
 * Sep 17, 2019  68196    ksunil       added fixContourWorldPoints
 * Oct 28, 2019  68196    ksunil       code tweak to apply world wrapping correction to streamLines.
 * Jun 09, 2020  79241    pbutler      Removed unnecessary loop to speed up contour processing/loading
 * Oct 19, 2020  83998    tjensen      Fix rendering of negative contours
 * </pre>
 *
 * @author chammack
 */
public class ContourSupport {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ContourSupport.class);

    private static final transient IPerformanceStatusHandler perfLog = PerformanceStatus
            .getHandler("ContourSupport:");

    /*
     * By default contour any data source that is passed in. This is much more
     * efficient than copying the data and allows us to contour any DataSource.
     *
     * The downside is that FortConBuf accesses each data point multiple times
     * and it is possible that DataSources with lots of transformation will have
     * poor performance. Since different sources will exhibit different behavior
     * and it is impossible to test all sources this field provides a runtime
     * flag to copy all the data so that slow sources will only need to access
     * each point once at the cost of more memory and taking the time to copy.
     * Hopefully this is just paranoia and this flag can be removed in the
     * future. If it turns out this is necessary we will likely want to find a
     * more fine grained approach to tuning this.
     */
    private static boolean copyData = Boolean.getBoolean(
            ContourSupport.class.getPackage().getName() + ".copyData");

    private static float smallestContourValue = GridUtil.GRID_FILL_VALUE - 1;

    private static float largestContourValue = GridUtil.GRID_FILL_VALUE + 1;

    private static LRUMap subgridCache = new LRUMap(10);

    private ContourSupport() {
        // No constructor
    }

    /**
     * Data structure for contouring
     */
    public static class ContourGroup {
        public double zoomLevel;

        public IWireframeShape posValueShape;

        public IWireframeShape negValueShape;

        // holds map of unique labeling preferences to frame shape
        public Map<BaseLabelingPreferences, IWireframeShape> labeledValuesMap = new HashMap<>();

        public ContourGroup parent;

        public PixelExtent lastUsedPixelExtent;

        public double lastDensity;

        public double lastPixelDensity;

        public Geometry contourGeometry;

        public String minMark;

        public String[] minVals;

        public double[][] minLabelPoints;

        public String maxMark;

        public String[] maxVals;

        public double[][] maxLabelPoints;

        public void drawContours(IGraphicsTarget target, RGB color,
                float lineWidth, LineStyle posLineStyle, LineStyle negLineStyle,
                IFont contourLabelFont, IFont minMaxLabelFont)
                throws VizException {

            target.drawWireframeShape(posValueShape, color, lineWidth,
                    posLineStyle, contourLabelFont);
            target.drawWireframeShape(negValueShape, color, lineWidth,
                    negLineStyle, contourLabelFont);

            for (Map.Entry<BaseLabelingPreferences, IWireframeShape> entry : labeledValuesMap
                    .entrySet()) {
                RGB col = color;
                Float thickness = lineWidth;
                LineStyle lStyle;
                // Items with no specific styles are bucketed in the
                // posValueShape/negValueShape
                BaseLabelingPreferences label = entry.getKey();

                try {
                    lStyle = LineStyle.valueOf(label.getLinePattern());
                } catch (Exception e) {
                    lStyle = LineStyle.DEFAULT;
                }

                if (label.getColor() != null) {
                    // if incorrect color is specified, "white" is returned.
                    col = RGBColors.getRGBColor(label.getColor());
                }
                if (label.getThickness() > 0) {
                    thickness = (float) label.getThickness();
                }
                target.drawWireframeShape(entry.getValue(), col, thickness,
                        lStyle, contourLabelFont);
            }

            drawLabels(target, minMaxLabelFont, color, maxLabelPoints, maxVals,
                    maxMark);
            drawLabels(target, minMaxLabelFont, color, minLabelPoints, minVals,
                    minMark);
        }

        private void drawLabels(IGraphicsTarget target, IFont labelFont,
                RGB color, double[][] labelPoints, String[] vals, String mark)
                throws VizException {
            if (labelPoints != null) {
                int size = labelPoints.length;
                boolean isMark = mark != null;
                boolean isVal = vals != null;
                VerticalAlignment markVert = VerticalAlignment.MIDDLE;
                VerticalAlignment valVert = VerticalAlignment.MIDDLE;
                if (isMark && isVal) {
                    markVert = VerticalAlignment.BOTTOM;
                    valVert = VerticalAlignment.TOP;
                }
                List<DrawableString> strings = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    double point[] = labelPoints[i];
                    if (isMark) {
                        DrawableString string = new DrawableString(mark, color);
                        string.font = labelFont;
                        string.setCoordinates(point[0], point[1], 1.0);
                        string.horizontalAlignment = HorizontalAlignment.CENTER;
                        string.verticallAlignment = markVert;
                        strings.add(string);
                    }
                    if (isVal) {
                        DrawableString string = new DrawableString(vals[i],
                                color);
                        string.font = labelFont;
                        string.setCoordinates(point[0], point[1], 1.0);
                        string.horizontalAlignment = HorizontalAlignment.CENTER;
                        string.verticallAlignment = valVert;
                        strings.add(string);
                    }
                }
                // draw all strings in a bulk operation for better performance
                target.drawStrings(strings);
            }
        }

    }

    /**
     * Create contours from provided parameters
     *
     * @param sources
     * @param level
     * @param extent
     * @param currentDensity
     * @param worldGridToCRSTransform
     * @param imageGridGeometry
     * @param mapGridGeometry
     * @param target
     * @param descriptor
     * @param prefs
     * @param zoom
     * @return the ContourGroup
     * @throws VizException
     */
    public static ContourGroup createContours(DataSource[] sources, float level,
            IExtent extent, double currentDensity, double currentMagnification,
            GeneralGridGeometry imageGridGeometry, IGraphicsTarget target,
            IMapDescriptor descriptor, ContourPreferences prefs, float zoom)
            throws VizException {

        ContourGroup contourGroup = new ContourGroup();
        contourGroup.lastDensity = currentDensity;
        contourGroup.lastPixelDensity = zoom;
        contourGroup.posValueShape = target.createWireframeShape(false,
                descriptor);
        contourGroup.negValueShape = target.createWireframeShape(false,
                descriptor);
        if (prefs != null && prefs.getContourLabeling() != null) {

            for (ValuesLabelingPreferences labelPrefs : prefs
                    .getContourLabeling().getValues()) {
                if (!labelPrefs.noStylesSet()) {
                    contourGroup.labeledValuesMap.put(labelPrefs,
                            target.createWireframeShape(false, descriptor));
                }

            }

            for (IncrementLabelingPreferences labelPrefs : prefs
                    .getContourLabeling().getIncrement()) {
                if (!labelPrefs.noStylesSet()) {
                    contourGroup.labeledValuesMap.put(labelPrefs,
                            target.createWireframeShape(false, descriptor));
                }

            }
        }
        contourGroup.zoomLevel = level;

        // Copy the pixel extent (deep copy required!)
        // expand by 50% to cover the subgrid expansion
        PixelExtent workingExtent = (PixelExtent) extent.clone();
        workingExtent.getEnvelope().expandBy(workingExtent.getWidth() * .5,
                workingExtent.getHeight() * .5);

        // Recontour whenever outside by more than 25%
        contourGroup.lastUsedPixelExtent = (PixelExtent) extent.clone();
        contourGroup.lastUsedPixelExtent.getEnvelope().expandBy(
                contourGroup.lastUsedPixelExtent.getWidth() * .25,
                contourGroup.lastUsedPixelExtent.getHeight() * .25);

        GeneralGridGeometry mapGridGeometry = descriptor.getGridGeometry();

        // Step 0: Set up necessary math transforms
        MathTransform rastPosToWorldGrid = null;
        MathTransform rastPosToLatLon = null;
        try {
            DefaultMathTransformFactory factory = new DefaultMathTransformFactory();

            CoordinateReferenceSystem rastCrs = imageGridGeometry
                    .getCoordinateReferenceSystem();
            CoordinateReferenceSystem mapCrs = mapGridGeometry
                    .getCoordinateReferenceSystem();

            MathTransform rastGridToCrs = imageGridGeometry
                    .getGridToCRS(PixelInCell.CELL_CENTER);
            MathTransform mapCrsToGrid = mapGridGeometry
                    .getGridToCRS(PixelInCell.CELL_CENTER).inverse();

            MathTransform rastCrsToLatLon = MapUtil
                    .getTransformToLatLon(rastCrs);

            MathTransform crs2crs = CRSCache.getInstance()
                    .findMathTransform(rastCrs, mapCrs);

            rastPosToWorldGrid = factory.createConcatenatedTransform(
                    factory.createConcatenatedTransform(rastGridToCrs, crs2crs),
                    mapCrsToGrid);

            rastPosToLatLon = factory.createConcatenatedTransform(rastGridToCrs,
                    rastCrsToLatLon);
        } catch (Exception e) {
            throw new VizException("Error building Transforms", e);
        }

        // Step 1:First determine the subgrid to contour
        long tsg0 = System.currentTimeMillis();
        SubGridCacheKey key = new SubGridCacheKey(workingExtent,
                mapGridGeometry, imageGridGeometry);
        GeneralEnvelope env = (GeneralEnvelope) subgridCache.get(key);
        if (env == null) {
            env = calculateSubGrid(workingExtent, mapGridGeometry,
                    imageGridGeometry);
            subgridCache.put(key, env);
        }
        long tsg1 = System.currentTimeMillis();
        perfLog.logDuration("Calculating sub grid", tsg1 - tsg0);

        // Step 3: Get the actual data

        if (sources.length == 1 && sources[0] != null) {
            // Step 4: Determine the subgrid, if any
            GridEnvelope imageRange = imageGridGeometry.getGridRange();

            int minX = (int) Math.floor(Math.max(env.getMinimum(0), 0));
            int minY = (int) Math.floor(Math.max(env.getMinimum(1), 0));
            int maxX = (int) Math
                    .ceil(Math.min(env.getMaximum(0), imageRange.getHigh(0)));
            int maxY = (int) Math
                    .ceil(Math.min(env.getMaximum(1), imageRange.getHigh(1)));
            int szX = (maxX - minX) + 1;
            int szY = (maxY - minY) + 1;
            if (szX * szY <= 0) {
                return contourGroup;
            }

            /* Make contours continuous for world wrapping grids. */
            int wrapNumber = GridGeometryWrapChecker
                    .checkForWrapping(imageGridGeometry);
            if (wrapNumber >= szX - 1) {
                szX = wrapNumber + 1;
            }

            DataSource subgridSource = sources[0];
            if (minX != 0 || minY != 0) {
                subgridSource = new OffsetDataSource(sources[0], minX, minY);
            }

            if (copyData) {
                subgridSource = DataUtilities.copy(subgridSource,
                        new FloatBufferWrapper(szX, szY), szX, szY);
            }

            // Use ported legacy code to determine contour interval
            ContourContainer contours = null;
            FortConConfig config = new FortConConfig();
            long t0 = System.currentTimeMillis();
            String minMaxLabelFormat = null;
            String minLabel = null;
            String maxLabel = null;
            String labelFormat = null;
            int labelTrimLeft = 0;
            int maxMinTrimLeft = 0;
            config.badlo = smallestContourValue;
            config.badhi = largestContourValue;
            // The +/- 0.5 is necessary to match the contouring location on
            // Awips 1
            config.xOffset = minX;
            config.yOffset = minY;
            config.labelSpacingLine = szX / 3;
            if (config.labelSpacingLine < 1) {
                config.labelSpacingLine = 1;
            }

            config.labelSpacingOverall = (int) (szX * 60 * currentMagnification
                    / ((PixelExtent) extent).getWidth() + 0.5);

            boolean hasLabelingPrefs = true;
            // If nothing provided, attempt to get approximately 50 contours
            if (prefs == null || prefs.getContourLabeling() == null) {

                hasLabelingPrefs = false;

                // TODO this is fairly inefficient to do every time.
                MinMax mm = DataUtilities.getMinMax(subgridSource, szX, szY);
                float interval = XFormFunctions.newDataIntervalFromZoom(
                        (float) mm.getSpan() / 50,
                        (float) (contourGroup.lastDensity * zoom), true, "",
                        10);
                config.seed = new float[] { interval };
                config.mode = -50;
                contours = FortConBuf.contour(subgridSource, szX, szY, config);
            } else {
                ContourLabelingPreferences contourLabeling = prefs
                        .getContourLabeling();
                if (contourLabeling.getLabelSpacing() > 0) {
                    config.labelSpacingLine = szX
                            / contourLabeling.getLabelSpacing();
                    if (config.labelSpacingLine < 1) {
                        config.labelSpacingLine = 1;
                    }
                }

                // setup min/max processing
                minMaxLabelFormat = contourLabeling.getMinMaxLabelFormat();
                minLabel = contourLabeling.getMinLabel();
                maxLabel = contourLabeling.getMaxLabel();
                labelFormat = contourLabeling.getLabelFormat();
                labelTrimLeft = contourLabeling.getLabelTrimLeft();
                maxMinTrimLeft = contourLabeling.getMaxMinTrimLeft();
                config.labelFormat = labelFormat;
                config.generateMins = minLabel != null && (!".".equals(minLabel)
                        || (minMaxLabelFormat != null && ".".equals(minLabel)));
                config.generateMaxes = maxLabel != null
                        && (!".".equals(maxLabel) || (minMaxLabelFormat != null
                                && ".".equals(maxLabel)));

                config.minMaxRadius = (int) (currentMagnification * 3 * szX
                        / 90);
                if (config.minMaxRadius < 1) {
                    config.minMaxRadius = 1;
                }
                if (config.minMaxRadius > 7) {
                    config.minMaxRadius = 7;
                }
                float dataZoom = (float) ((level) * contourGroup.lastDensity);
                if (prefs.getContourLabeling().isZoomLock()) {
                    dataZoom = (float) contourGroup.lastDensity;
                }

                for (IncrementLabelingPreferences labelPrefs : prefs
                        .getContourLabeling().getIncrement()) {

                    float initialInterval = labelPrefs.getValues()[0];
                    float interval;
                    if (contourLabeling.getNumberOfContours() > 0) {
                        MinMax mm = DataUtilities.getMinMax(subgridSource, szX,
                                szY);
                        interval = (float) mm.getSpan()
                                / contourLabeling.getNumberOfContours();
                        if (interval < 0) {
                            interval = -interval;
                        }
                        if (interval > initialInterval) {
                            initialInterval = interval;
                            labelPrefs.setValuesString(
                                    Float.toHexString(initialInterval));

                        }

                        // removed +1 to fix EPTC plan view and T_std
                        interval = XFormFunctions.newDataIntervalFromZoom(
                                initialInterval, dataZoom, false, "", 10);
                    } else {
                        interval = XFormFunctions.newDataIntervalFromZoom(
                                initialInterval, dataZoom, true, "", 10);
                    }

                    float[] controls = new float[] { interval,
                            labelPrefs.getMin(), labelPrefs.getMax() };
                    // This is a special market to restrict contours between min
                    // and max
                    config.mode = 1000;
                    config.seed = controls;

                    contours = concatContourContainers(contours, FortConBuf
                            .contour(subgridSource, szX, szY, config));

                }
                if (contourLabeling.getValues().size() > 0) {
                    // explicit contouring values provided. First we merge them.

                    float[] vals = {};
                    for (ValuesLabelingPreferences labelPrefs : prefs
                            .getContourLabeling().getValues()) {
                        vals = ArrayUtils.addAll(vals, labelPrefs.getValues());
                    }

                    /*
                     * A contour range is specified by setting
                     * numberOfContours=1000 and values={interval, minContour,
                     * maxContour} based on AWIPS1 configuration rules. As of
                     * 5/2019, You don't have to do this anymore. Left for
                     * backward compatibility. You can now specify a min/max xml
                     * attribute directly on the increment label.
                     */
                    if (vals.length == 2
                            && contourLabeling.getNumberOfContours() == 1000) {
                        // non-positive increments treated as 1 by ForConBuf
                        vals = new float[] { contourLabeling.getIncrement()
                                .get(0).getValues()[0], vals[0], vals[1] };
                    } else {
                        /*
                         * the +0.1 makes it so numbers greater than 0.9 are
                         * rounded up and all other numbers are rounded down.
                         * This seems to make it match A1 better when the
                         * density is 0.67 than 2.98507 is rounded to 3 instead
                         * of 2
                         */
                        int d = (int) (2 / dataZoom + 0.1);
                        if (d > 1) {
                            int myCount = (vals.length + d - 1) / d;
                            float[] newVals = new float[myCount];
                            int i, j;
                            for (i = j = 0; j < myCount; i += d, j++) {
                                newVals[j] = vals[i];
                            }
                            vals = newVals;
                        }
                        if (prefs.getContourLabeling()
                                .isCreateNegativeValues()) {
                            float[] newVals = new float[vals.length * 2];
                            for (int i = 0; i < vals.length; i++) {
                                newVals[vals.length + i] = vals[i];
                                newVals[vals.length - i - 1] = -1 * vals[i];
                            }
                            vals = newVals;
                        }
                    }
                    config.seed = vals;
                    if (contourLabeling.getNumberOfContours() > 0) {
                        config.mode = contourLabeling.getNumberOfContours();
                    } else {
                        config.mode = vals.length;
                    }
                    if (contours == null) {
                        contours = FortConBuf.contour(subgridSource, szX, szY,
                                config);
                    } else {
                        // means we have both "values" and "increment" specified
                        // in the contourLabeling element.
                        ContourContainer valueContours = FortConBuf
                                .contour(subgridSource, szX, szY, config);

                        contours = concatContourContainers(contours,
                                valueContours);
                    }
                }
            }

            if (contours == null) {
                throw new VizException(
                        "Unable to create contours. Possible empty contourLabeling in XML");
            }
            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Contouring", t1 - t0);

            float contourValue = 0;

            long tTransformAccum = 0;
            long tLabelAccum = 0;
            long tMinMaxAccum = 0;
            List<double[]> labelPoints = new ArrayList<>(512);

            try {
                DecimalFormat dfLabel = null;

                if (labelFormat != null) {
                    dfLabel = new DecimalFormat(labelFormat);
                } else {
                    dfLabel = determineLabelFormat(contours.contourVals);
                }

                long tZ0 = System.currentTimeMillis();
                // process min/max
                processMinMaxLabels(contours, minMaxLabelFormat, minLabel,
                        maxLabel, rastPosToWorldGrid, contourGroup, labelPoints,
                        maxMinTrimLeft);
                long tZ1 = System.currentTimeMillis();
                tMinMaxAccum += tZ1 - tZ0;

                correctWorldWrapping(contours, descriptor, rastPosToLatLon,
                        rastPosToWorldGrid);

                int size = contours.xyContourPoints.size();
                // total coordinates
                int totalPosCoords = 0;
                int totalNegCoords = 0;
                for (int i = 0; i < size; i++) {
                    if (contours.contourVals.get(i) > 0) {
                        totalPosCoords += contours.xyContourPoints.get(i).length
                                / 2;
                    } else {
                        totalNegCoords += contours.xyContourPoints.get(i).length
                                / 2;
                    }
                }

                contourGroup.posValueShape.allocate(totalPosCoords);
                contourGroup.negValueShape.allocate(totalNegCoords);

                for (int i = 0; i < size; i++) {
                    boolean prepareLabel = false;
                    float[] contourGridPoints = contours.xyContourPoints.get(i);
                    float[] contourWorldPoints = new float[contourGridPoints.length];

                    try {
                        int contourPoints = contourGridPoints.length / 2;
                        tZ0 = System.currentTimeMillis();
                        rastPosToWorldGrid.transform(contourGridPoints, 0,
                                contourWorldPoints, 0, contourPoints);
                        tZ1 = System.currentTimeMillis();
                        tTransformAccum += (tZ1 - tZ0);
                        double[][] contourWorldPointsArr = new double[contourPoints][2];
                        int index = 0;
                        for (int j = 0; j < contourPoints; j += 1) {
                            contourWorldPointsArr[j][0] = contourWorldPoints[index];
                            index += 1;
                            contourWorldPointsArr[j][1] = contourWorldPoints[index];
                            index += 1;
                        }
                        IWireframeShape shapeToAddTo = null;
                        contourValue = contours.contourVals.get(i);
                        String label = dfLabel.format(contourValue);
                        label = label.substring(labelTrimLeft);

                        if (prefs != null && prefs.getContourLabeling() != null
                                && prefs.getContourLabeling().getValues()
                                        .size() > 0) {
                            // If we have explicit values and no explicit format
                            // use the default String representation of the
                            // given values
                            boolean found = false;
                            for (int i1 = 0; i1 < prefs.getContourLabeling()
                                    .getValues().size() && !found; i1++) {

                                ValuesLabelingPreferences currentPref = prefs
                                        .getContourLabeling().getValues()
                                        .get(i1);
                                float[] values = currentPref.getValues();
                                for (float value : values) {
                                    if (Float.compare(contourValue,
                                            value) == 0) {
                                        label = dfLabel.format(value);
                                        found = true;
                                        prepareLabel = true;
                                        break;
                                    }
                                }
                                if (currentPref.noStylesSet()) {
                                    if (contourValue >= 0) {
                                        shapeToAddTo = contourGroup.posValueShape;
                                    } else {
                                        shapeToAddTo = contourGroup.negValueShape;
                                    }
                                }

                                else {
                                    shapeToAddTo = contourGroup.labeledValuesMap
                                            .get(currentPref);
                                }
                                shapeToAddTo
                                        .addLineSegment(contourWorldPointsArr);
                            }

                        }

                        if (prefs != null && prefs.getContourLabeling() != null
                                && prefs.getContourLabeling().getIncrement()
                                        .size() > 0) {
                            // pick only values that are within the min/max set
                            // in contourLabeling.increment element
                            boolean found = false;
                            for (int i1 = 0; i1 < prefs.getContourLabeling()
                                    .getIncrement().size() && !found; i1++) {
                                IncrementLabelingPreferences currentPref = prefs
                                        .getContourLabeling().getIncrement()
                                        .get(i1);
                                float min = currentPref.getMin();
                                float max = currentPref.getMax();

                                if (contourValue <= max
                                        && contourValue >= min) {
                                    if (currentPref.noStylesSet()) {
                                        if (contourValue >= 0) {
                                            shapeToAddTo = contourGroup.posValueShape;
                                        } else {
                                            shapeToAddTo = contourGroup.negValueShape;
                                        }
                                    } else {
                                        shapeToAddTo = contourGroup.labeledValuesMap
                                                .get(currentPref);
                                    }
                                    shapeToAddTo.addLineSegment(
                                            contourWorldPointsArr);
                                    found = true;
                                    prepareLabel = true;
                                }
                            }
                        }
                        /*
                         * can happen if there is no labeling specification in
                         * the XML.
                         */
                        if (!hasLabelingPrefs) {
                            if (contourValue >= 0) {
                                shapeToAddTo = contourGroup.posValueShape;
                            } else {
                                shapeToAddTo = contourGroup.negValueShape;
                            }
                            shapeToAddTo.addLineSegment(contourWorldPointsArr);
                            prepareLabel = true;
                        }

                        // process contour labels
                        // TODO perform formatting
                        tZ0 = System.currentTimeMillis();
                        // prepare Label only if this value is going to be in
                        // play.
                        if (prepareLabel) {
                            prepareLabel(shapeToAddTo, zoom, label, labelPoints,
                                    contourWorldPointsArr);
                        }
                        tZ1 = System.currentTimeMillis();
                        tLabelAccum += tZ1 - tZ0;
                    } catch (TransformException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Error transforming points: "
                                        + Arrays.toString(contourGridPoints),
                                e);
                    }
                }

                perfLog.logDuration("Min/Max processing", tMinMaxAccum);
                perfLog.logDuration("Labeling", tLabelAccum);
                perfLog.logDuration("Transformation", tTransformAccum);
            } catch (Throwable e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error postprocessing contours", e);
                throw new VizException("Error postprocessing contours", e);
            }
        } else if (sources.length == 2) {
            GridEnvelope range = imageGridGeometry.getGridRange();
            long[] sz = { range.getSpan(0), range.getSpan(1) };

            // Step 4: Determine the subgrid, if any

            int minX = (int) Math.floor(Math.max(env.getMinimum(0), 0));
            int minY = (int) Math.floor(Math.max(env.getMinimum(1), 0));
            int maxX = (int) Math.ceil(Math.min(env.getMaximum(0), sz[0] - 1));
            int maxY = (int) Math.ceil(Math.min(env.getMaximum(1), sz[1] - 1));

            makeStreamLines(sources[0], sources[1], minX, minY, maxX, maxY, sz,
                    contourGroup, currentMagnification, zoom,
                    contourGroup.lastDensity, rastPosToWorldGrid,
                    rastPosToLatLon, descriptor);
        }

        return contourGroup;

    }

    public static GeneralEnvelope calculateSubGrid(IExtent workingExtent,
            GeneralGridGeometry mapGridGeometry,
            GeneralGridGeometry imageGridGeometry) {
        GeneralEnvelope env = new GeneralEnvelope(2);
        try {
            GridGeometry2D imageGeometry2D = GridGeometry2D
                    .wrap(imageGridGeometry);

            GridGeometry2D mapGeometry2D = GridGeometry2D.wrap(mapGridGeometry);

            // Start with a grid envelope in screen space0
            GridEnvelope2D screenGridEnvelope = new GridEnvelope2D(
                    (int) Math.floor(workingExtent.getMinX()),
                    (int) Math.floor(workingExtent.getMinY()),
                    (int) Math.ceil(workingExtent.getWidth()),
                    (int) Math.ceil(workingExtent.getHeight()));
            // intersect with mapGeometry so we only have points on the actual
            // display
            screenGridEnvelope = new GridEnvelope2D(screenGridEnvelope
                    .intersection(mapGeometry2D.getGridRange2D()));
            // convert from screen grid space to screen crs space.
            Envelope2D screenCRSEnvelope = mapGeometry2D
                    .gridToWorld(screenGridEnvelope);

            org.opengis.geometry.Envelope subgridCRSEnvelope = MapUtil
                    .reprojectAndIntersect(screenCRSEnvelope,
                            imageGridGeometry.getEnvelope());

            GridEnvelope2D subgridEnv = imageGeometry2D
                    .worldToGrid(new Envelope2D(subgridCRSEnvelope));
            // Add a 1 pixel border since worldToGrid is only guaranteed to
            // include a cell if the cell center is in the envelope but we want
            // to include the data in the subgrid if even a tiny bit of the edge
            // overlaps.
            subgridEnv.grow(1, 1);
            if (!subgridEnv.isEmpty()) {
                // Convert GridEnvelope to general envelope.
                env.setRange(0, subgridEnv.getMinX(), subgridEnv.getMaxX());
                env.setRange(1, subgridEnv.getMinY(), subgridEnv.getMaxY());
            }
        } catch (Exception e) {
            statusHandler.handle(Priority.WARN,
                    "Cannot compute subgrid, contouring may be slow.", e);
            // Don't use a subgrid, just contour the complete image.
            // This may result in doing way too much contouring which can be
            // slow, but it is better than no contouring at all. It's also worth
            // noting that it gets slower as you zoom in and more contours are
            // generated, but as you zoom in it also becomes more likely you
            // will be successful in your transformation since the smaller area
            // is less likely to contain invalid points.
            env.setRange(0, imageGridGeometry.getGridRange().getLow(0),
                    imageGridGeometry.getGridRange().getHigh(0));
            env.setRange(1, imageGridGeometry.getGridRange().getLow(1),
                    imageGridGeometry.getGridRange().getHigh(1));
        }
        statusHandler.handle(Priority.DEBUG, "Subgrid: " + env);
        return env;
    }

    /**
     * Create labels for a linestring
     *
     * @param shapeToAddTo
     * @param levelOffset
     * @param contourValue
     * @param labelPoints
     * @param valsArr
     */
    private static void prepareLabel(IWireframeShape shapeToAddTo,
            double screenToPixel, String label, List<double[]> labelPoints,
            double[][] valsArr) {
        double[] lastPoint = new double[] { Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY };

        double d = 0.0;

        final double threshold1 = (200.0 / screenToPixel);
        final double threshold2 = (50.0 / screenToPixel);

        double q1, q2, p1, p2;
        for (double[] vals : valsArr) {

            // Distance approximation between last label point
            // and current point

            // Absolute value logic inlined for performance
            q1 = lastPoint[0] - vals[0];
            q2 = lastPoint[1] - vals[1];
            q1 = (q1 <= 0.0D) ? 0.0D - q1 : q1;
            q2 = (q2 <= 0.0D) ? 0.0D - q2 : q2;

            d = q1 + q2;

            // If distance has been enough, add a label
            if (d > (threshold1)
            /* || (labeledAtLeastOnce == false && n == valsArr.length - 1) */) {
                // Search for any labels that are too close
                // to the current one
                boolean tooClose = false;
                p1 = vals[0];
                p2 = vals[1];
                q1 = 0;
                q2 = 0;
                for (double[] test : labelPoints) {
                    // Distance approximation between each label
                    // point and current point
                    // Absolute value logic inlined for performance
                    q1 = test[0] - p1;
                    q2 = test[1] - p2;
                    q1 = (q1 <= 0.0D) ? -1.0D * q1 : q1;
                    q2 = (q2 <= 0.0D) ? -1.0D * q2 : q2;
                    d = q1 + q2;
                    if (d < threshold2) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose
                /*
                 * || (labeledAtLeastOnce == false && n == valsArr.length - 1)
                 */) {
                    shapeToAddTo.addLabel(label, vals);
                    labelPoints.add(vals);
                    lastPoint = vals;
                }
            }
        }

    }

    public static ContourGroup createContours(Object data, int level,
            IExtent extent, double currentDensity,
            GeneralGridGeometry imageGridGeometry, IGraphicsTarget target,
            ContourPreferences prefs) throws VizException {

        GeneralEnvelope imageEnvelope = new GeneralEnvelope(
                new double[] { extent.getMinX(), extent.getMinY() },
                new double[] { extent.getMaxX(), extent.getMaxY() });
        GeneralGridEnvelope imageRange = new GeneralGridEnvelope(
                new int[] { 0, 0 },
                new int[] { imageGridGeometry.getGridRange().getSpan(0),
                        imageGridGeometry.getGridRange().getSpan(1) },
                false);
        GridGeometry2D imageGeom = new GridGeometry2D(imageRange,
                imageEnvelope);
        imageGeom = GridGeometry2D.wrap(imageGridGeometry);
        MathTransform gridToPixel = imageGeom
                .getGridToCRS(PixelInCell.CELL_CENTER);

        ContourGroup contourGroup = new ContourGroup();
        contourGroup.lastDensity = currentDensity;
        contourGroup.zoomLevel = 1.0 / Math.pow(2.0, level);

        GeneralEnvelope pixelEnvelope = new GeneralEnvelope(
                new double[] { extent.getMinX(), extent.getMinY() },
                new double[] { extent.getMaxX(), extent.getMaxY() });

        GeneralGridEnvelope range = new GeneralGridEnvelope(
                new int[] { (int) extent.getMinX(), (int) extent.getMinY() },
                new int[] { (int) (extent.getMaxX()), (int) extent.getMaxY() },
                false);
        GridGeometry2D pixelGeom = new GridGeometry2D(range, pixelEnvelope);

        contourGroup.posValueShape = target.createWireframeShape(false,
                pixelGeom);
        contourGroup.posValueShape.allocate(1000);
        contourGroup.negValueShape = target.createWireframeShape(false,
                pixelGeom);
        contourGroup.negValueShape.allocate(1000);
        boolean haslabelingXML = true;
        if (prefs != null && prefs.getContourLabeling() != null) {

            for (ValuesLabelingPreferences labelPrefs : prefs
                    .getContourLabeling().getValues()) {
                contourGroup.labeledValuesMap.put(labelPrefs,
                        target.createWireframeShape(false, pixelGeom));

            }

            for (IncrementLabelingPreferences labelPrefs : prefs
                    .getContourLabeling().getIncrement()) {
                contourGroup.labeledValuesMap.put(labelPrefs,
                        target.createWireframeShape(false, pixelGeom));

            }
        }
        // Step 3: Get the actual data

        float[] data1D = null;
        long[] sz = new long[] { imageGridGeometry.getGridRange().getSpan(0),
                imageGridGeometry.getGridRange().getSpan(1) };

        if (data instanceof float[]) {
            data1D = (float[]) data;

            int minX = 0;
            int minY = 0;
            int maxX = (int) (sz[0] - 1);
            int maxY = (int) (sz[1] - 1);

            int szX = (maxX - minX) + 1;
            int szY = (maxY - minY) + 1;
            int totalSz = szX * szY;
            if (totalSz <= 0) {
                return contourGroup;
            }

            float[][] subgriddedData = new float[szX][szY];

            for (int j = 0; j < szY; j++) {
                for (int i = 0; i < szX; i++) {
                    subgriddedData[i][j] = data1D[((int) sz[0] * (j + minY))
                            + (i + minX)];
                }
            }

            // Use ported legacy code to determine contour interval
            ContourContainer contours = null;
            FortConConfig config = new FortConConfig();
            long t0 = System.currentTimeMillis();
            String minMaxLabelFormat = null;
            String minLabel = null;
            String maxLabel = null;
            String labelFormat = null;
            config.badlo = smallestContourValue;
            config.badhi = largestContourValue;
            config.xOffset = minX;
            config.yOffset = minY;
            config.labelSpacingLine = subgriddedData.length / 3;
            if (config.labelSpacingLine < 1) {
                config.labelSpacingLine = 1;
            }

            // If nothing provided, attempt to get approximately 50 contours
            if (prefs == null || prefs.getContourLabeling() == null) {
                haslabelingXML = false;
                float interval = XFormFunctions.newDataIntervalFromZoom(0.5f,
                        (float) ((level + 1) * contourGroup.lastDensity), true,
                        "", 10);

                config.seed = new float[] { interval };
                config.mode = -50;
                contours = FortConBuf.contour(subgriddedData, config);
            } else {
                ContourLabelingPreferences contourLabeling = prefs
                        .getContourLabeling();
                if (contourLabeling.getLabelSpacing() > 0) {
                    config.labelSpacingLine = subgriddedData.length
                            / contourLabeling.getLabelSpacing();
                    if (config.labelSpacingLine < 1) {
                        config.labelSpacingLine = 1;
                    }
                }

                // setup min/max processing.
                minMaxLabelFormat = contourLabeling.getMinMaxLabelFormat();
                minLabel = contourLabeling.getMinLabel();
                maxLabel = contourLabeling.getMaxLabel();
                labelFormat = contourLabeling.getLabelFormat();
                config.generateMins = minLabel != null && (!".".equals(minLabel)
                        || (minMaxLabelFormat != null && ".".equals(minLabel)));
                config.generateMaxes = maxLabel != null
                        && (!".".equals(maxLabel) || (minMaxLabelFormat != null
                                && ".".equals(maxLabel)));

                config.minMaxRadius = (int) (1.0 * 3
                        * ((PixelExtent) extent).getWidth() / 90);
                if (config.minMaxRadius < 1) {
                    config.minMaxRadius = 1;
                }
                if (config.minMaxRadius > 7) {
                    config.minMaxRadius = 7;
                }

                for (IncrementLabelingPreferences labelPrefs : prefs
                        .getContourLabeling().getIncrement()) {
                    // interval provided
                    float initialInterval = labelPrefs.getValues()[0];
                    float interval;
                    if (contourLabeling.getNumberOfContours() > 0) {
                        // use numberOfContours
                        float minData = 1e37f;
                        float maxData = -1e37f;
                        for (float[] dataRow : subgriddedData) {
                            for (float dataCell : dataRow) {
                                if (dataCell < minData
                                        && dataCell != -999_999) {
                                    minData = dataCell;
                                } else if (dataCell < 999_998
                                        && dataCell > maxData) {
                                    maxData = dataCell;
                                }
                            }
                        }
                        interval = (maxData - minData)
                                / contourLabeling.getNumberOfContours();
                        if (interval < 0) {
                            interval = -interval;
                        }

                        if (interval > initialInterval) {
                            initialInterval = interval;
                            // why would this be done we want to maintain the
                            // configured minimum, it will cause problems when
                            // showing the same parameter on cross section and
                            // time
                            // height?

                            labelPrefs.setValuesString(
                                    Float.toHexString(initialInterval));
                        }

                        interval = XFormFunctions.newDataIntervalFromZoom(
                                initialInterval,
                                (float) ((level + 1)
                                        * contourGroup.lastDensity),
                                false, "", 10);
                    } else {
                        interval = XFormFunctions.newDataIntervalFromZoom(
                                initialInterval,
                                (float) ((level + 1)
                                        * contourGroup.lastDensity),
                                true, "", 10);
                    }

                    float[] controls = new float[] { interval,
                            labelPrefs.getMin(), labelPrefs.getMax() };
                    config.mode = 1000;
                    config.seed = controls;

                    contours = concatContourContainers(contours,
                            FortConBuf.contour(subgriddedData, config));
                }
                if (prefs.getContourLabeling().getValues().size() > 0) {
                    // explicit contouring values provided

                    // convert Float[] to float[]
                    float[] vals = {};
                    for (ValuesLabelingPreferences labelPrefs : prefs
                            .getContourLabeling().getValues()) {

                        vals = ArrayUtils.addAll(vals, labelPrefs.getValues());
                    }
                    // the +0.1 makes it so numbers greater than 0.9 are rounded
                    // up
                    // and all other numbers are rounded down.
                    int d = (int) (2 / ((level + 1) * contourGroup.lastDensity)
                            + 0.1);
                    if (d > 1) {
                        int myCount = (vals.length + d - 1) / d;
                        float[] newVals = new float[myCount];
                        int i, j;
                        for (i = j = 0; j < myCount; i += d, j++) {
                            newVals[j] = vals[i];
                        }
                        vals = newVals;
                    }
                    if (prefs.getContourLabeling().isCreateNegativeValues()) {
                        float[] newVals = new float[vals.length * 2];
                        for (int i = 0; i < vals.length; i++) {
                            newVals[vals.length + i] = vals[i];
                            newVals[vals.length - i - 1] = -1 * vals[i];
                        }
                        vals = newVals;
                    }
                    config.seed = vals;
                    if (contourLabeling.getNumberOfContours() > 0) {
                        config.mode = contourLabeling.getNumberOfContours();
                    } else {
                        config.mode = vals.length;
                    }
                    if (contours == null) {
                        contours = FortConBuf.contour(subgriddedData, config);
                    }

                    else {
                        // means we have both "values" and "increment" specified
                        // in the contourLabeling element.
                        ContourContainer valueContours = FortConBuf
                                .contour(subgriddedData, config);

                        contours = concatContourContainers(contours,
                                valueContours);

                    }
                }
            }

            if (contours == null) {
                statusHandler.handle(Priority.PROBLEM,
                        "Unable to create contours. Possible empty contourLabeling in XML");
                throw new VizException(
                        "Unable to create contours. Possible empty contourLabeling in XML");
            }

            long t1 = System.currentTimeMillis();
            perfLog.logDuration("Contouring", t1 - t0);

            double levelOffset = Math.pow(2, (level - 1));

            float contourValue = 0;
            long tTransformAccum = 0;
            long tLabelAccum = 0;
            long tMinMaxAccum = 0;
            List<double[]> labelPoints = new ArrayList<>();

            try {
                int size = contours.xyContourPoints.size();

                if (size > 0) {
                    DecimalFormat dfLabel = null;

                    if (labelFormat != null) {
                        dfLabel = new DecimalFormat(labelFormat);
                    } else {
                        dfLabel = determineLabelFormat(contours.contourVals);
                    }

                    long tZ0 = System.currentTimeMillis();
                    // process min/max
                    processMinMaxLabels(contours, minMaxLabelFormat, minLabel,
                            maxLabel, gridToPixel, contourGroup, labelPoints,
                            0);
                    long tZ1 = System.currentTimeMillis();
                    tMinMaxAccum += tZ1 - tZ0;

                    for (int i = 0; i < size; i++) {
                        boolean prepareLabel = false;
                        float[] contourGridPoints = contours.xyContourPoints
                                .get(i);
                        float[] contourWorldPoints = new float[contourGridPoints.length];

                        try {
                            int contourPoints = contourGridPoints.length / 2;
                            tZ0 = System.currentTimeMillis();
                            gridToPixel.transform(contourGridPoints, 0,
                                    contourWorldPoints, 0, contourPoints);
                            tZ1 = System.currentTimeMillis();
                            tTransformAccum += (tZ1 - tZ0);
                            double[][] contourWorldPointsArr = new double[contourPoints][2];
                            int index = 0;
                            for (int j = 0; j < contourPoints; j += 1) {
                                contourWorldPointsArr[j][0] = contourWorldPoints[index];
                                index += 1;
                                contourWorldPointsArr[j][1] = contourWorldPoints[index];
                                index += 1;
                            }
                            contourValue = contours.contourVals.get(i);
                            IWireframeShape shapeToAddTo = null;
                            String label = dfLabel.format(contourValue);
                            if (prefs != null
                                    && prefs.getContourLabeling() != null
                                    && prefs.getContourLabeling().getValues()
                                            .size() > 0) {
                                // If we have explicit values and no explicit
                                // format
                                // use the default String representation of the
                                // given values
                                boolean found = false;
                                for (int i1 = 0; i1 < prefs.getContourLabeling()
                                        .getValues().size() && !found; i1++) {
                                    ValuesLabelingPreferences currentPref = prefs
                                            .getContourLabeling().getValues()
                                            .get(i1);
                                    float[] values = currentPref.getValues();
                                    for (float value : values) {
                                        if (Float.compare(contourValue,
                                                value) == 0) {
                                            label = dfLabel.format(value);
                                            found = true;
                                            prepareLabel = true;
                                            break;
                                        }
                                    }
                                    if (currentPref.noStylesSet()) {
                                        if (contourValue >= 0) {
                                            shapeToAddTo = contourGroup.posValueShape;
                                        } else {
                                            shapeToAddTo = contourGroup.negValueShape;
                                        }
                                    }

                                    else {
                                        shapeToAddTo = contourGroup.labeledValuesMap
                                                .get(currentPref);
                                    }
                                    shapeToAddTo.addLineSegment(
                                            contourWorldPointsArr);
                                }
                            }
                            if (prefs != null
                                    && prefs.getContourLabeling() != null
                                    && prefs.getContourLabeling().getIncrement()
                                            .size() > 0) {
                                // pick only values that are within the min/max
                                // set
                                // in contourLabeling.increment element
                                boolean found = false;
                                for (int i1 = 0; i1 < prefs.getContourLabeling()
                                        .getIncrement().size()
                                        && !found; i1++) {
                                    IncrementLabelingPreferences currentPref = prefs
                                            .getContourLabeling().getIncrement()
                                            .get(i1);
                                    float min = currentPref.getMin();
                                    float max = currentPref.getMax();

                                    if (contourValue <= max
                                            && contourValue >= min) {
                                        if (currentPref.noStylesSet()) {
                                            if (contourValue >= 0) {
                                                shapeToAddTo = contourGroup.posValueShape;
                                            } else {
                                                shapeToAddTo = contourGroup.negValueShape;
                                            }
                                        } else {
                                            shapeToAddTo = contourGroup.labeledValuesMap
                                                    .get(currentPref);
                                        }
                                        shapeToAddTo.addLineSegment(
                                                contourWorldPointsArr);
                                        found = true;
                                        prepareLabel = true;
                                    }
                                }
                            }
                            /*
                             * can happen only if there is no labeling
                             * specification in the XML.
                             */
                            if (!haslabelingXML) {
                                if (contourValue >= 0) {
                                    shapeToAddTo = contourGroup.posValueShape;
                                } else {
                                    shapeToAddTo = contourGroup.negValueShape;
                                }
                                shapeToAddTo
                                        .addLineSegment(contourWorldPointsArr);
                                prepareLabel = true;
                            }
                            if (prepareLabel) {
                                prepareLabel(shapeToAddTo, levelOffset, label,
                                        labelPoints, contourWorldPointsArr);
                            }
                        } catch (TransformException e) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Error transforming points: " + Arrays
                                            .toString(contourGridPoints),
                                    e);
                        }
                    }
                }
                perfLog.logDuration("Min/Max processing", tMinMaxAccum);
                perfLog.logDuration("Labeling", tLabelAccum);
                perfLog.logDuration("Transformation", tTransformAccum);
            } catch (Exception e) {
                throw new VizException("Error postprocessing contours", e);
            }

            return contourGroup;
        } else if (data instanceof List && ((List<?>) data).size() == 2) {
            // Step 1:First determine the subgrid to contour

            // Step 3: Get the actual data

            float[] uW = (float[]) ((List<?>) data).get(0);
            float[] vW = (float[]) ((List<?>) data).get(1);

            // Step 4: Determine the subgrid, if any

            int minX = 0;
            int minY = 0;
            int maxX = (int) (sz[0] - 1);
            int maxY = (int) (sz[1] - 1);

            DataSource uWSource = new FloatBufferWrapper(uW, (int) sz[0],
                    (int) sz[1]);
            DataSource vWSource = new FloatBufferWrapper(vW, (int) sz[0],
                    (int) sz[1]);

            makeStreamLines(uWSource, vWSource, minX, minY, maxX, maxY, sz,
                    contourGroup, 1, 1, contourGroup.lastDensity * 2,
                    gridToPixel, null, null);

            return contourGroup;
        } else {
            throw new UnsupportedOperationException(
                    "Contouring is not supported for data type: "
                            + data.getClass().getName());
        }

    }

    private static void makeStreamLines(DataSource uW, DataSource vW, int minX,
            int minY, int maxX, int maxY, long[] sz, ContourGroup contourGroup,
            double currentMagnification, float zoom, double density,
            MathTransform rastPosToWorldGrid, MathTransform rastPosToLatLon,
            IMapDescriptor descriptor) throws VizException {

        int szX = (maxX - minX) + 1;
        int szY = (maxY - minY) + 1;
        int totalSz = szX * szY;
        if (totalSz <= 0) {
            return;
        }
        int x = (int) sz[0];
        int y = (int) sz[1];

        // Use ported legacy code to determine contour interval
        long t0 = System.currentTimeMillis();

        double[] center = new double[2];
        double[] offCenter = new double[2];

        try {
            rastPosToWorldGrid.transform(new double[] { x / 2.0, y / 2.0 }, 0,
                    center, 0, 1);
            rastPosToWorldGrid.transform(
                    new double[] { (x / 2.0) + 1.0, y / 2.0 }, 0, offCenter, 0,
                    1);
        } catch (TransformException e) {
            throw new VizException(
                    "Could not perform center calculations for stream lines.",
                    e);
        }

        double gridPixelSize = offCenter[0] - center[0];
        double gridPixelMax = 2000.;

        // If gridPixelSize is large, arrows on streamline will be too small, so
        // adjust here
        if (gridPixelSize > gridPixelMax) {
            gridPixelSize = gridPixelSize / 5;
        }
        float arrowSize = (float) (currentMagnification * 5 / zoom
                / gridPixelSize);

        double spadiv = zoom * density * gridPixelSize / 25;

        double minSpacing = 1.0 / spadiv;
        double maxSpacing = 3.0 / spadiv;
        float minspc = 0;
        float maxspc = 0;

        if (minSpacing > 1) {
            minspc = (float) Math.sqrt(minSpacing);
        }
        if (minspc < 0.1) {
            minspc = 0.1f;
        }
        if (maxSpacing > 1) {
            maxspc = (float) Math.sqrt(maxSpacing);
        }
        if (maxspc < 0.25) {
            maxspc = 0.25f;
        }

        if (minX != 0 || minY != 0) {
            uW = new OffsetDataSource(uW, minX, minY);
            vW = new OffsetDataSource(vW, minX, minY);
        }

        StrmPakConfig config = new StrmPakConfig(arrowSize, minspc, maxspc,
                -1000000f, -999998f);
        StreamLineContainer container = StrmPak.strmpak(uW, vW, szX, szY,
                config);
        try {
            if (rastPosToLatLon != null) {
                correctWorldWrappingStreamLine(container, descriptor,
                        rastPosToLatLon, rastPosToWorldGrid);
            }
        } catch (TransformException e1) {
            throw new VizException(
                    "Could not transform stream line coordinate into world grid space.",
                    e1);
        }
        long t1 = System.currentTimeMillis();
        perfLog.logDuration("Contouring", t1 - t0);

        long tAccum = 0;

        try {
            for (List<StreamLinePoint> points : container.streamLines) {
                double[][] valsArr = new double[points.size()][2];

                for (int i = 0; i < points.size(); i++) {
                    StreamLinePoint point = points.get(i);
                    try {
                        long tZ0 = System.currentTimeMillis();
                        rastPosToWorldGrid.transform(
                                new double[] { point.getX() + minX,
                                        point.getY() + minY },
                                0, valsArr[i], 0, 1);
                        long tZ1 = System.currentTimeMillis();
                        tAccum += (tZ1 - tZ0);
                    } catch (TransformException e) {
                        throw new VizException(
                                "Could not transform stream line coordinate into world grid space.",
                                e);
                    }
                }

                contourGroup.posValueShape.addLineSegment(valsArr);
            }

            perfLog.logDuration("Streamline transformation", tAccum);
        } catch (Throwable e) {
            throw new VizException("Error postprocessing contours", e);
        }
    }

    private static void processMinMaxLabels(ContourContainer contours,
            String minMaxLabelFormat, String minLabel, String maxLabel,
            MathTransform transform, ContourGroup contourGroup,
            List<double[]> labelPoints, int maxMinTrimLeft)
            throws TransformException {
        if (contours.minLabelPoints != null) {
            if (minLabel != null && !".".equals(minLabel)) {
                contourGroup.minMark = minLabel;
            }
            if (minMaxLabelFormat != null) {
                if (minMaxLabelFormat.trim().length() > 0) {
                    DecimalFormat df = new DecimalFormat(minMaxLabelFormat);
                    int size = contours.minVals.size();
                    contourGroup.minVals = new String[size];
                    for (int i = 0; i < size; i++) {
                        String label = df.format(contours.minVals.get(i));
                        label = label.substring(maxMinTrimLeft);
                        contourGroup.minVals[i] = label;
                    }
                } else {
                    int size = contours.minVals.size();
                    contourGroup.minVals = new String[size];
                    for (int i = 0; i < size; i++) {
                        float val = contours.minVals.get(i);
                        DecimalFormat df = determineLabelFormat(val);
                        contourGroup.minVals[i] = df.format(val);
                    }
                }
            }
            contourGroup.minLabelPoints = transformPointList(
                    contours.minLabelPoints, transform);
            for (double[] vals : contourGroup.minLabelPoints) {
                labelPoints.add(vals);
            }
        }
        if (contours.maxLabelPoints != null) {
            if (maxLabel != null && !".".equals(maxLabel)) {
                contourGroup.maxMark = maxLabel;
            }
            if (minMaxLabelFormat != null) {
                if (minMaxLabelFormat.trim().length() > 0) {
                    DecimalFormat df = new DecimalFormat(minMaxLabelFormat);
                    int size = contours.maxVals.size();
                    contourGroup.maxVals = new String[size];
                    for (int i = 0; i < size; i++) {
                        String label = df.format(contours.maxVals.get(i));
                        label = label.substring(maxMinTrimLeft);
                        contourGroup.maxVals[i] = label;
                    }
                } else {
                    int size = contours.maxVals.size();
                    contourGroup.maxVals = new String[size];
                    for (int i = 0; i < size; i++) {
                        float val = contours.maxVals.get(i);
                        DecimalFormat df = determineLabelFormat(val);
                        contourGroup.maxVals[i] = df.format(val);
                    }
                }
            }
            contourGroup.maxLabelPoints = transformPointList(
                    contours.maxLabelPoints, transform);
            for (double[] vals : contourGroup.maxLabelPoints) {
                labelPoints.add(vals);
            }
        }

    }

    private static double[][] transformPointList(List<float[]> xyPoints,
            MathTransform transformToApply) throws TransformException {
        int size = xyPoints.size();
        float in[] = new float[size * 2];
        int index = 0;
        for (float[] xy : xyPoints) {
            in[index] = xy[0];
            index += 1;
            in[index] = xy[1];
            index += 1;
        }

        float out[] = new float[in.length];

        transformToApply.transform(in, 0, out, 0, size);

        double[][] rval = new double[size][2];
        index = 0;
        for (int i = 0; i < rval.length; i += 1) {
            rval[i][0] = out[index];
            index += 1;
            rval[i][1] = out[index];
            index += 1;
        }

        return rval;
    }

    // determine based on spread of values
    private static DecimalFormat determineLabelFormat(List<Float> vals) {
        // determine format based on the average difference between the contours
        SortedSet<Float> sortedValues = new TreeSet<>(vals);
        Iterator<Float> iter = sortedValues.iterator();
        if (iter.hasNext()) {
            float prevVal = iter.next();
            int count = 0;
            double diffSum = 0;
            while (iter.hasNext()) {
                float val = iter.next();
                diffSum += val - prevVal;
                count++;
                prevVal = val;
            }
            return determineLabelFormat((float) (diffSum / count));
        } else {
            return determineLabelFormat(0.0f);
        }

    }

    // determine based on spread of values
    private static DecimalFormat determineLabelFormat(float val) {
        float absval = Math.abs(val);

        DecimalFormat rval = null;
        if (absval == 0) {
            rval = new DecimalFormat("0.#");
        } else if (absval < 0.001 || absval > 1e7) {
            rval = new DecimalFormat("0.###E0");
        } else if (absval < 0.01) {
            rval = new DecimalFormat("0.###");
        } else if (absval < 0.1) {
            rval = new DecimalFormat("0.##");
        } else {
            rval = new DecimalFormat("0.#");
        }

        return rval;
    }

    /**
     * Check the contour lines in a ContourContainer and split any lines that
     * need to wrap over the "seam" in the display crs. If changes are needed
     * the lines within contours are modified directly.
     *
     * @param contours
     *            container holding contour lines
     * @param descriptor
     *            the descriptor for the display
     * @param rastPosToLatLon
     *            transform for converting the coordinates within contours to
     *            LatLon coordinates.
     * @param rastPosToWorldGrid
     *            transform for converting the coordinates within contours to
     *            worldGrid coordinates.
     * @throws TransformException
     */
    private static void correctWorldWrapping(ContourContainer contours,
            IMapDescriptor descriptor, MathTransform rastPosToLatLon,
            MathTransform rastPosToWorldGrid) throws TransformException {
        long tZ0 = System.currentTimeMillis();

        WorldWrapChecker wwc = new WorldWrapChecker(
                descriptor.getGridGeometry());
        if (!wwc.needsChecking()) {
            return;
        }

        List<float[]> splitLines = new ArrayList<>();
        List<Float> dupValues = new ArrayList<>();

        Iterator<float[]> lineIter = contours.xyContourPoints.iterator();
        Iterator<Float> valueIter = contours.contourVals.iterator();

        while (lineIter.hasNext()) {
            float[] data = lineIter.next();
            Float val = valueIter.next();
            double[] ll = new double[data.length];
            rastPosToLatLon.transform(data, 0, ll, 0, data.length / 2);
            boolean remove = false;
            int startIndex = 0;
            double lastLon = ll[0];
            for (int i = 2; i < ll.length; i += 2) {
                double lon = ll[i];
                if (wwc.check(lastLon, lon)) {
                    remove = true;
                    if (startIndex + 1 < i) {
                        float[] cwp = fixContourWorldPoints(data, startIndex, i,
                                rastPosToWorldGrid);
                        if (cwp != null) {
                            splitLines.add(cwp);
                            dupValues.add(val);
                        }

                    }
                    startIndex = i;
                }
                lastLon = lon;
            }
            if (remove) {
                lineIter.remove();
                valueIter.remove();
                if (startIndex + 1 < data.length) {
                    float[] cwp = fixContourWorldPoints(data, startIndex,
                            data.length, rastPosToWorldGrid);
                    if (cwp != null) {
                        splitLines.add(cwp);
                        dupValues.add(val);
                    }
                }
            }

        }
        contours.xyContourPoints.addAll(splitLines);
        contours.contourVals.addAll(dupValues);

        long tZ1 = System.currentTimeMillis();

        perfLog.logDuration("Checking world wrapping", tZ1 - tZ0);
    }

    private static void correctWorldWrappingStreamLine(
            StreamLineContainer container, IMapDescriptor descriptor,
            MathTransform rastPosToLatLon, MathTransform rastPosToWorldGrid)
            throws TransformException {
        long tZ0 = System.currentTimeMillis();

        WorldWrapChecker wwc = new WorldWrapChecker(
                descriptor.getGridGeometry());
        if (!wwc.needsChecking()) {
            return;
        }

        List<List<StreamLinePoint>> splitLines = new ArrayList<>();

        Iterator<List<StreamLinePoint>> lineIter = container.streamLines
                .iterator();

        while (lineIter.hasNext()) {
            List<StreamLinePoint> data = lineIter.next();
            float[] floatData = convertSLPointListToFloatArray(data);
            double[] ll = new double[floatData.length];
            rastPosToLatLon.transform(floatData, 0, ll, 0,
                    floatData.length / 2);
            boolean remove = false;
            int startIndex = 0;
            double lastLon = ll[0];
            for (int i = 2; i < ll.length; i += 2) {
                double lon = ll[i];
                if (wwc.check(lastLon, lon)) {
                    remove = true;
                    if (startIndex + 1 < i) {
                        float[] cwp = fixContourWorldPoints(floatData,
                                startIndex, i, rastPosToWorldGrid);
                        if (cwp != null) {
                            splitLines.add(convertFloatArrayToSLPointList(cwp));
                        }
                    }
                    startIndex = i;
                }
                lastLon = lon;
            }
            if (remove) {
                lineIter.remove();
                if (startIndex + 1 < floatData.length) {
                    float[] cwp = fixContourWorldPoints(floatData, startIndex,
                            floatData.length, rastPosToWorldGrid);
                    if (cwp != null) {
                        splitLines.add(convertFloatArrayToSLPointList(cwp));
                    }
                }
            }

        }
        container.streamLines.addAll(splitLines);

        long tZ1 = System.currentTimeMillis();

        perfLog.logDuration("Checking world wrapping", tZ1 - tZ0);
    }

    private static float[] convertSLPointListToFloatArray(
            List<StreamLinePoint> data) {

        float[] ret = new float[data.size() * 2];
        for (int i = 0; i < data.size(); i++) {
            ret[(i * 2) + 0] = data.get(i).getX();
            ret[(i * 2) + 1] = data.get(i).getY();

        }
        return ret;
    }

    private static List<StreamLinePoint> convertFloatArrayToSLPointList(
            float[] data) {

        List<StreamLinePoint> ret = new ArrayList<>();
        for (int i = 0; i < data.length; i = i + 2) {
            StreamLinePoint pt = new StreamLinePoint(data[i], data[i + 1]);
            ret.add(pt);
        }
        return ret;
    }

    private static float[] fixContourWorldPoints(float[] data, int startIndex,
            int incr, MathTransform rastPosToWorldGrid)
            throws TransformException {

        float[] contourGridPoints = Arrays.copyOfRange(data, startIndex, incr);
        if (contourGridPoints.length == 2) {
            return contourGridPoints;
        }
        float[] contourWorldPoints = new float[contourGridPoints.length];

        int contourPoints = contourGridPoints.length / 2;
        rastPosToWorldGrid.transform(contourGridPoints, 0, contourWorldPoints,
                0, contourPoints);

        /*
         * See if the first 2 elements are either both +ve or -ve. If so, see if
         * the last element has the same sign. If so, line is good. Else, remove
         * the last element and return the rest. Similar logic for the last 2
         * elements
         *
         * Since this code gets called only if the logic in
         * correctWorldWrapping(...) is being applied, we can assume that only
         * the first or the last element in the contourWorldPoints is
         * conflicting with the rest. And we eliminate that point (not the
         * entire line).
         *
         * For lines that don't need to be split to begin with, the following
         * doesn't apply anyway.
         */

        if ((contourWorldPoints[0] * contourWorldPoints[2]) > 0) {
            if ((contourWorldPoints[0]
                    * contourWorldPoints[contourGridPoints.length - 2]) > 0) {
                // nothing to change
                return contourGridPoints;
            } else {
                // remove the last value and return the rest
                return (Arrays.copyOfRange(contourGridPoints, 0,
                        contourGridPoints.length - 2));
            }
        }
        // the last 2 points are either both +ve or both -ve
        else if ((contourWorldPoints[contourGridPoints.length - 2]
                * contourWorldPoints[contourGridPoints.length - 4]) > 0) {
            if ((contourWorldPoints[contourGridPoints.length - 2]
                    * contourWorldPoints[0]) > 0) {
                // nothing to change
                return contourGridPoints;
            } else {
                // remove the first value
                return (Arrays.copyOfRange(contourGridPoints, 2,
                        contourGridPoints.length));
            }
        }
        /*
         * special cases. First and last points are the same. Or lines that has
         * one end world wrapping that needs to be fixed and the other end goes
         * all the way to the prime meridian and then ends with one point on the
         * other side of the prime meridian.
         */
        else {
            return (Arrays.copyOfRange(contourGridPoints, 2,
                    contourGridPoints.length - 2));
        }
    }

    private static ContourContainer concatContourContainers(
            ContourContainer one, ContourContainer two) {

        if (one == null) {
            return two;
        }

        ContourContainer finalContour = new ContourContainer(
                one.contourVals.size() + two.contourVals.size());
        finalContour.contourVals.addAll(Stream
                .concat(one.contourVals.stream(), two.contourVals.stream())
                .collect(Collectors.toList()));
        finalContour.xyContourPoints.addAll(Stream
                .concat(one.xyContourPoints.stream(),
                        two.xyContourPoints.stream())
                .collect(Collectors.toList()));
        finalContour.minLabelPoints.addAll(Stream
                .concat(one.minLabelPoints.stream(),
                        two.minLabelPoints.stream())
                .collect(Collectors.toList()));
        finalContour.maxLabelPoints.addAll(Stream
                .concat(one.maxLabelPoints.stream(),
                        two.maxLabelPoints.stream())
                .collect(Collectors.toList()));
        finalContour.minVals.addAll(
                Stream.concat(one.minVals.stream(), two.minVals.stream())
                        .collect(Collectors.toList()));
        finalContour.maxVals.addAll(
                Stream.concat(one.maxVals.stream(), two.maxVals.stream())
                        .collect(Collectors.toList()));
        return finalContour;

    }

    public static GeographicDataSource[] smoothData(DataSource[] dataRecord,
            GeneralGridGeometry gridGeometry, double smoothingDistance)
            throws VizException {
        // Calculate the Diagonal Distance of the Grid In Meters.
        DirectPosition2D upperCorner = new DirectPosition2D(
                gridGeometry.getEnvelope().getUpperCorner());
        DirectPosition2D lowerCorner = new DirectPosition2D(
                gridGeometry.getEnvelope().getLowerCorner());
        double distanceInM;
        try {
            MathTransform crs2ll = MapUtil.getTransformToLatLon(
                    gridGeometry.getCoordinateReferenceSystem());
            crs2ll.transform(upperCorner, upperCorner);
            crs2ll.transform(lowerCorner, lowerCorner);
            upperCorner.x = MapUtil.correctLon(upperCorner.x);
            lowerCorner.x = MapUtil.correctLon(lowerCorner.x);

            upperCorner.y = MapUtil.correctLat(upperCorner.y);
            lowerCorner.y = MapUtil.correctLat(lowerCorner.y);

            distanceInM = JTS.orthodromicDistance(
                    new Coordinate(lowerCorner.x, lowerCorner.y),
                    new Coordinate(upperCorner.x, upperCorner.y),
                    MapUtil.getLatLonProjection());
        } catch (Exception e) {
            throw new VizException(e);
        }
        // Calculate the Diagonal Distance in Points
        GridEnvelope range = gridGeometry.getGridRange();
        int nx = range.getSpan(0);
        int ny = range.getSpan(1);
        double distanceInPoints = Math.sqrt(nx * nx + ny * ny);
        // Determine the number of points to smooth, assume
        // smoothingDistance is in km
        float npts = (float) (distanceInPoints * smoothingDistance
                / (distanceInM / 1000));
        FloatBufferWrapper data = new FloatBufferWrapper(nx, ny);
        DataDestination dest = InverseFillValueFilter
                .apply((DataDestination) data, Constants.LEGACY_NAN);
        DataUtilities.copy(dataRecord[0], dest, nx, ny);
        float[] dataArray = data.getArray();
        dataArray = DistFilter.filter(dataArray, npts, nx, ny, 1);
        data = new FloatBufferWrapper(dataArray, nx, ny);
        DataSource source = FillValueFilter.apply((DataSource) data,
                Constants.LEGACY_NAN);
        GeographicDataSource gdSource = new GeographicDataSource(source,
                gridGeometry);
        return new GeographicDataSource[] { gdSource };

    }

}
