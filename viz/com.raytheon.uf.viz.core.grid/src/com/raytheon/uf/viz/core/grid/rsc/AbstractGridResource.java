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
package com.raytheon.uf.viz.core.grid.rsc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.unit.Unit;
import javax.measure.unit.UnitFormat;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters.PersistedParameters;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.interpolation.BilinearInterpolation;
import com.raytheon.uf.common.geospatial.interpolation.GridSampler;
import com.raytheon.uf.common.geospatial.interpolation.Interpolation;
import com.raytheon.uf.common.geospatial.interpolation.NearestNeighborInterpolation;
import com.raytheon.uf.common.numeric.DataUtilities;
import com.raytheon.uf.common.numeric.DataUtilities.MinMax;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.style.AbstractStylePreferences;
import com.raytheon.uf.common.style.MatchCriteria;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleManager.StyleType;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.arrow.ArrowPreferences;
import com.raytheon.uf.common.style.contour.ContourPreferences;
import com.raytheon.uf.common.style.image.ColorMapParameterFactory;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.drawables.ColorMapLoader;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.PaintStatus;
import com.raytheon.uf.viz.core.drawables.ext.IImagingExtension.ImageProvider;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.grid.display.AbstractGriddedDisplay;
import com.raytheon.uf.viz.core.grid.display.GriddedIconDisplay;
import com.raytheon.uf.viz.core.grid.display.GriddedVectorDisplay;
import com.raytheon.uf.viz.core.grid.rsc.data.GeneralGridData;
import com.raytheon.uf.viz.core.grid.rsc.data.GridDataRequestRunner;
import com.raytheon.uf.viz.core.grid.rsc.data.LogArrowScaler;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory;
import com.raytheon.uf.viz.core.rsc.RenderingOrderFactory.ResourceOrder;
import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.DensityCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.OutlineCapability;
import com.raytheon.uf.viz.core.tile.DataSourceTileImageCreator;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.viz.core.contours.ContourRenderable;
import com.raytheon.viz.core.contours.rsc.displays.GriddedContourDisplay;
import com.raytheon.viz.core.contours.rsc.displays.GriddedStreamlineDisplay;
import com.vividsolutions.jts.geom.Coordinate;

/**
 * 
 * A single resource that can be easily extended to draw contours, images, wind
 * barbs, arrows, streamlines, or icons for any data that can be represented as
 * a grid.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Mar 09, 2011           bsteffen    Initial creation
 * May 08, 2013  1980     bsteffen    Set paint status in GridResources for
 *                                    KML.
 * Jul 15, 2013  2107     bsteffen    Fix sampling of grid vector arrows.
 * Aug 27, 2013  2287     randerso    Added new parameters required by 
 *                                    GriddedVectorDisplay and
 *                                    GriddedIconDisplay
 * Sep 24, 2013  2404     bclement    colormap params now created using match
 *                                    criteria
 * Sep 23, 2013  2363     bsteffen    Add more vector configuration options.
 * Jan 14, 2014  2594     bsteffen    Switch vector mag/dir to use data source
 *                                    instead of raw float data.
 * Feb 28, 2014  2791     bsteffen    Switch all data to use data source.
 * Aug 21, 2014  DR 17313 jgerth      Implements ImageProvider
 * Oct 07, 2014  3668     bclement    Renamed requestJob to requestRunner
 * Dec 09, 2014  5056     jing        Added data access interfaces
 * May 14, 2015  4079     bsteffen    Move to core.grid, add getDisplayUnit
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 * @param <T>
 */
public abstract class AbstractGridResource<T extends AbstractResourceData>
        extends AbstractVizResource<T, IMapDescriptor> implements ImageProvider {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractGridResource.class);

    /* Unknown source, provides acceptable vector size. */
    private static final double VECTOR_SIZE = 25.6;

    /* Unknown source, provides acceptable density. */
    private static final double VECTOR_DENSITY_FACTOR = 1.875;

    private static final int IMAGE_TILE_SIZE = 1024;

    public static final String INTERROGATE_VALUE = "value";

    public static final String INTERROGATE_UNIT = "unit";

    public static final String INTERROGATE_DIRECTION = "direction";

    private final GridDataRequestRunner requestRunner;

    private Map<DataTime, List<PluginDataObject>> pdoMap = new ConcurrentHashMap<DataTime, List<PluginDataObject>>();

    private Map<DataTime, List<IRenderable>> renderableMap = new ConcurrentHashMap<DataTime, List<IRenderable>>();

    /**
     * This is a local cache of data that is used when sampling or reprojected.
     */
    private Map<DataTime, List<GeneralGridData>> dataMap = new ConcurrentHashMap<DataTime, List<GeneralGridData>>();

    /**
     * StylePreferences from the styleManager appropriate for the display type
     * provided.
     */
    protected AbstractStylePreferences stylePreferences = null;

    /**
     * The format used by the default inspect method
     */
    protected DecimalFormat sampleFormat = new DecimalFormat("0.00");

    protected AbstractGridResource(T resourceData, LoadProperties loadProperties) {
        super(resourceData, loadProperties);
        resourceData.addChangeListener(new IResourceDataChanged() {
            @Override
            public void resourceChanged(ChangeType type, Object object) {
                if (type == ChangeType.DATA_UPDATE) {
                    if (object instanceof PluginDataObject) {
                        addDataObject((PluginDataObject) object);
                    } else if (object instanceof PluginDataObject[]) {
                        for (PluginDataObject pdo : (PluginDataObject[]) object) {
                            addDataObject(pdo);
                        }
                    } else if (object instanceof Object[]) {
                        for (Object obj : (Object[]) object) {
                            if (obj instanceof PluginDataObject) {
                                addDataObject((PluginDataObject) obj);
                            }
                        }
                    }
                } else if (type == ChangeType.CAPABILITY) {
                    if (object instanceof AbstractCapability) {
                        AbstractCapability capability = (AbstractCapability) object;
                        synchronized (renderableMap) {
                            for (List<IRenderable> renderableList : renderableMap
                                    .values()) {
                                for (IRenderable renderable : renderableList) {
                                    updataRenderableCapabilities(renderable,
                                            capability);
                                }
                            }
                        }
                    }
                }
            }
        });
        dataTimes = new ArrayList<DataTime>();
        requestRunner = new GridDataRequestRunner(this);
        // Capabilities need to be inited in construction for things like the
        // image combination tool.
        initCapabilities();
    }

    /**
     * Adds the pdo to the appropriate time and removes any renderable or data
     * cached for that time.
     * 
     * @param pdo
     */
    protected void addDataObject(PluginDataObject pdo) {
        DataTime time = pdo.getDataTime();
        if (this.resourceData instanceof AbstractRequestableResourceData) {
            AbstractRequestableResourceData resourceData = (AbstractRequestableResourceData) this.resourceData;
            if (resourceData.getBinOffset() != null) {
                time = resourceData.getBinOffset().getNormalizedTime(time);
            }
        }
        List<PluginDataObject> pdos = this.pdoMap.get(time);
        if (pdos == null) {
            pdos = new ArrayList<PluginDataObject>();
            this.pdoMap.put(time, pdos);
        }
        if (pdos.contains(pdo)) {
            pdos.remove(pdo);
        }
        pdos.add(pdo);
        synchronized (renderableMap) {
            if (renderableMap.containsKey(time)) {
                List<IRenderable> renderableList = this.renderableMap
                        .remove(time);
                if (renderableList != null) {
                    for (IRenderable renderable : renderableList) {
                        disposeRenderable(renderable);
                    }
                }
            }
        }
        requestRunner.remove(time);
        dataMap.remove(time);
        if (!dataTimes.contains(dataTimes)) {
            dataTimes.add(time);
        }
    }

    /**
     * Should be called immediately after construction if this resource was
     * created from another GridResource to prevent requesting data again.
     * 
     * @param data
     */
    protected void setData(Map<DataTime, List<GeneralGridData>> data) {
        if (this.dataMap.isEmpty()) {
            this.dataMap.putAll(data);
        }
    }

    /**
     * Update a renderable to reflect a changed capability.
     * 
     * @param renderable
     * @param capability
     */
    protected void updataRenderableCapabilities(IRenderable renderable,
            AbstractCapability capability) {
        if (renderable instanceof AbstractGriddedDisplay<?>) {
            AbstractGriddedDisplay<?> gridDisplay = (AbstractGriddedDisplay<?>) renderable;
            if (capability instanceof ColorableCapability) {
                gridDisplay.setColor(getCapability(ColorableCapability.class)
                        .getColor());
            }
            if (capability instanceof MagnificationCapability) {
                gridDisplay.setMagnification(getCapability(
                        MagnificationCapability.class).getMagnification());
            }
            if (capability instanceof DensityCapability) {
                gridDisplay.setDensity(getCapability(DensityCapability.class)
                        .getDensity());
            }
            if (gridDisplay instanceof GriddedVectorDisplay) {
                GriddedVectorDisplay vectorDisplay = (GriddedVectorDisplay) gridDisplay;
                vectorDisplay.setLineStyle(getCapability(
                        OutlineCapability.class).getLineStyle());
                vectorDisplay.setLineWidth(getCapability(
                        OutlineCapability.class).getOutlineWidth());
            }
        } else if (renderable instanceof ContourRenderable) {
            ContourRenderable contourRenderable = (ContourRenderable) renderable;
            if (capability instanceof ColorableCapability) {
                contourRenderable.setColor(getCapability(
                        ColorableCapability.class).getColor());
            }
            if (capability instanceof MagnificationCapability) {
                contourRenderable.setMagnification(getCapability(
                        MagnificationCapability.class).getMagnification());
            }
            if (capability instanceof DensityCapability) {
                contourRenderable.setDensity(getCapability(
                        DensityCapability.class).getDensity());
            }
            if (capability instanceof OutlineCapability) {
                contourRenderable.setLineStyle(getCapability(
                        OutlineCapability.class).getLineStyle());
                contourRenderable.setOutlineWidth(getCapability(
                        OutlineCapability.class).getOutlineWidth());
            }
        }
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        try {
            initStylePreferences();
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }
        initSampling();
    }

    /**
     * Ensure we have any capabilities that will be used for the renderables for
     * this display type.
     */
    protected void initCapabilities() {
        DisplayType displayType = getDisplayType();
        List<DisplayType> altDisplayTypes = new ArrayList<DisplayType>();
        switch (displayType) {
        case IMAGE:
            if (!hasCapability(ImagingCapability.class)) {
                this.getCapability(ImagingCapability.class).setBrightness(0.5f);
                this.getCapability(ImagingCapability.class)
                        .setInterpolationState(true);
            }
            this.getCapability(ImagingCapability.class).setProvider(this);
            altDisplayTypes.add(DisplayType.CONTOUR);
            break;
        case BARB:
        case ARROW:
        case STREAMLINE:
            altDisplayTypes.add(DisplayType.BARB);
            altDisplayTypes.add(DisplayType.ARROW);
            altDisplayTypes.add(DisplayType.STREAMLINE);
        case DUALARROW:
            altDisplayTypes.add(DisplayType.ARROW);
        case CONTOUR:
            altDisplayTypes.add(DisplayType.IMAGE);
            getCapability(ColorableCapability.class);
            getCapability(DensityCapability.class);
            getCapability(MagnificationCapability.class);
            getCapability(OutlineCapability.class);
            break;
        case ICON:
            getCapability(ColorableCapability.class);
            getCapability(DensityCapability.class);
            getCapability(MagnificationCapability.class);
            break;
        }
        this.getCapability(DisplayTypeCapability.class)
                .setAlternativeDisplayTypes(altDisplayTypes);
    }

    /**
     * Gets style preferences from the StyleManager
     * 
     * @throws StyleException
     */
    protected void initStylePreferences() throws StyleException {
        DisplayType displayType = getDisplayType();
        StyleRule styleRule = null;
        MatchCriteria criteria = getMatchCriteria();
        if (criteria != null) {
            switch (displayType) {
            case IMAGE:
                styleRule = StyleManager.getInstance().getStyleRule(
                        StyleType.IMAGERY, criteria);
                break;
            case CONTOUR:
            case STREAMLINE:
                styleRule = StyleManager.getInstance().getStyleRule(
                        StyleType.CONTOUR, criteria);
                break;
            case BARB:
            case ARROW:
            case DUALARROW:
                styleRule = StyleManager.getInstance().getStyleRule(
                        StyleType.ARROW, criteria);
                break;
            }
        }
        if (styleRule != null) {
            stylePreferences = styleRule.getPreferences();
            if (stylePreferences instanceof ImagePreferences) {
                boolean interpolationState = ((ImagePreferences) styleRule
                        .getPreferences()).isInterpolate();
                this.getCapability(ImagingCapability.class)
                        .setInterpolationState(interpolationState);
            }
        }
    }

    /**
     * Create an interpolation and format to be used when sampling
     */
    protected void initSampling() {
        if (stylePreferences != null
                && stylePreferences instanceof ImagePreferences) {
            ImagePreferences prefs = (ImagePreferences) stylePreferences;
            if (prefs.getSamplePrefs() != null
                    && prefs.getSamplePrefs().getFormatString() != null) {
                try {
                    int numDecimalPlaces = Integer.parseInt(prefs
                            .getSamplePrefs().getFormatString());
                    char[] zeroes = new char[numDecimalPlaces];
                    Arrays.fill(zeroes, '0');
                    sampleFormat = new DecimalFormat("0."
                            + String.copyValueOf(zeroes));

                } catch (NumberFormatException e) {
                    statusHandler.handle(Priority.INFO,
                            "Invalid sample format in style rules, expected an integer but recieved "
                                    + prefs.getSamplePrefs().getFormatString(),
                            e);
                }
            }
        }
    }

    /**
     * Create a renderable for this data.
     * 
     * @param target
     * @param data
     * @return
     * @throws VizException
     */
    public IRenderable createRenderable(IGraphicsTarget target,
            GeneralGridData data) throws VizException {

        IRenderable renderable = null;

        GridGeometry2D gridGeometry = data.getGridGeometry();

        DisplayType displayType = getDisplayType();

        switch (displayType) {
        case IMAGE:
            ColorMapCapability colorMapCap = getCapability(ColorMapCapability.class);
            ImagingCapability imagingCap = getCapability(ImagingCapability.class);
            ColorMapParameters params = null;
            if (renderableMap.isEmpty()) {
                params = createColorMapParameters(data);
                if (params.getColorMap() == null) {
                    if (params.getColorMapName() == null) {
                        params.setColorMapName("Grid/gridded data");
                    }
                    params.setColorMap(ColorMapLoader.loadColorMap(params
                            .getColorMapName()));
                }
                colorMapCap.setColorMapParameters(params);
            } else {
                params = colorMapCap.getColorMapParameters();
            }
            if (params.getDataMapping() != null) {
                data.convert(params.getColorMapUnit());
            }
            TileImageCreator creator = new DataSourceTileImageCreator(
                    data.getScalarData(), data.getDataUnit(),
                    ColorMapDataType.FLOAT, colorMapCap);
            TileSetRenderable tsr = new TileSetRenderable(imagingCap,
                    gridGeometry, creator, 1, IMAGE_TILE_SIZE);
            tsr.project(descriptor.getGridGeometry());
            renderable = tsr;
            break;
        case BARB:
        case ARROW:
        case DUALARROW:
            convertData(data);
            VectorGraphicsConfig config = new VectorGraphicsConfig();
            config.setBaseSize(VECTOR_SIZE);
            if (displayType != DisplayType.BARB) {
                config.setArrowHeadSizeRatio(0.15625);
                config.setMinimumMagnitude(VECTOR_SIZE
                        * config.getArrowHeadSizeRatio());
                config.disableCalmCircle();
                if (stylePreferences != null
                        && stylePreferences instanceof ArrowPreferences) {
                    double scale = ((ArrowPreferences) stylePreferences)
                            .getScale();
                    if (scale >= 0.0) {
                        config.setLinearArrowScaleFactor(scale);
                    } else {
                        config.setArrowScaler(new LogArrowScaler(-1 * scale));
                    }
                } else {
                    config.setLinearArrowScaleFactor(1.0);
                }
            }
            GriddedVectorDisplay vectorDisplay = new GriddedVectorDisplay(
                    data.getMagnitude(), data.getDirectionFrom(), descriptor,
                    gridGeometry, VECTOR_DENSITY_FACTOR, true, displayType,
                    config);
            vectorDisplay.setColor(getCapability(ColorableCapability.class)
                    .getColor());
            vectorDisplay.setLineStyle(getCapability(OutlineCapability.class)
                    .getLineStyle());
            vectorDisplay.setLineWidth(getCapability(OutlineCapability.class)
                    .getOutlineWidth());
            vectorDisplay.setDensity(getCapability(DensityCapability.class)
                    .getDensity());
            vectorDisplay.setMagnification(getCapability(
                    MagnificationCapability.class).getMagnification());
            renderable = vectorDisplay;
            break;
        case ICON:
            GriddedIconDisplay iconDisplay = new GriddedIconDisplay(
                    data.getScalarData(), descriptor, gridGeometry, 80, 0.75);
            iconDisplay.setColor(getCapability(ColorableCapability.class)
                    .getColor());
            iconDisplay.setDensity(getCapability(DensityCapability.class)
                    .getDensity());
            iconDisplay.setMagnification(getCapability(
                    MagnificationCapability.class).getMagnification());
            renderable = iconDisplay;
            break;
        case CONTOUR:
        case STREAMLINE:
            convertData(data);
            GriddedContourDisplay contourRenderable = null;
            if (displayType == DisplayType.CONTOUR) {
                contourRenderable = new GriddedContourDisplay(descriptor,
                        gridGeometry, data.getScalarData());
            } else {
                contourRenderable = new GriddedStreamlineDisplay(descriptor,
                        gridGeometry, data.getUComponent(),
                        data.getVComponent());
            }
            contourRenderable.setColor(getCapability(ColorableCapability.class)
                    .getColor());
            contourRenderable.setLineStyle(getCapability(
                    OutlineCapability.class).getLineStyle());
            contourRenderable.setOutlineWidth(getCapability(
                    OutlineCapability.class).getOutlineWidth());
            contourRenderable.setDensity(getCapability(DensityCapability.class)
                    .getDensity());
            contourRenderable.setMagnification(getCapability(
                    MagnificationCapability.class).getMagnification());
            if (stylePreferences != null
                    && stylePreferences instanceof ContourPreferences) {
                contourRenderable
                        .setPreferences((ContourPreferences) stylePreferences);
            }
            renderable = contourRenderable;
            break;

        }
        return renderable;
    }

    private void convertData(GeneralGridData data) {
        if (stylePreferences != null) {
            data.convert(stylePreferences.getDisplayUnits());
        }
    }

    /**
     * Called the first time an image is drawn to initialize the color map
     * parameters
     * 
     * @param data
     * @return
     * @throws VizException
     */
    protected ColorMapParameters createColorMapParameters(GeneralGridData data)
            throws VizException {
        ParamLevelMatchCriteria criteria = getMatchCriteria();
        ColorMapParameters newParameters;
        GridEnvelope2D range = data.getGridGeometry().getGridRange2D();
        DataSource source = data.getScalarData();
        MinMax mm = DataUtilities.getMinMax(source, range.getSpan(0),
                range.getSpan(1));
        try {
            newParameters = ColorMapParameterFactory.build((float) mm.getMin(),
                    (float) mm.getMax(), data.getDataUnit(), criteria);
        } catch (StyleException e) {
            throw new VizException("Unable to build colormap parameters", e);
        }
        ColorMapParameters oldParameters = this.getCapability(
                ColorMapCapability.class).getColorMapParameters();
        if (oldParameters != null
                && oldParameters.getColorMapMin() <= newParameters
                        .getColorMapMin()
                && oldParameters.getColorMapMax() >= newParameters
                        .getColorMapMax()) {
            // if the oldParameters have a larger range than the new parameters,
            // reuse the old parameters. This is useful when the resource is
            // sharing capabilities, for example in an FFGVizGroupResource.
            newParameters = oldParameters;
        } else if (oldParameters != null) {
            newParameters.setColorMapName(oldParameters.getColorMapName());
            newParameters.setColorMap(oldParameters.getColorMap());
            PersistedParameters persisted = oldParameters.getPersisted();
            if (persisted != null) {
                newParameters.applyPersistedParameters(persisted);
            }
        }
        return newParameters;
    }

    /**
     * Get the match criteria used for looking up style rules and/or colormaps.
     * 
     * @return
     */
    public abstract ParamLevelMatchCriteria getMatchCriteria();

    /**
     * This method should return a data object for the given time and/or pdos.
     * 
     * @param pdos
     *            Any pdos that have been added for this time.
     * @return
     * @throws VizException
     */
    public abstract List<GeneralGridData> getData(DataTime time,
            List<PluginDataObject> pdos) throws VizException;

    public List<GeneralGridData> requestData(DataTime time) {
        synchronized (requestRunner) {
            List<GeneralGridData> data = this.dataMap.get(time);
            if (data == null) {
                data = requestRunner.requestData(time, pdoMap.get(time));
                if (data != null) {
                    data = mergeData(data);
                    this.dataMap.put(time, data);
                }
            }
            return data;
        }
    }

    /**
     * Combine data records that are in the same grid space to avoid areas of
     * overlapping data.
     * 
     * @param dataList
     * @return
     */
    protected List<GeneralGridData> mergeData(List<GeneralGridData> dataList) {
        if (dataList == null || dataList.size() < 2) {
            return dataList;
        }
        for (int i = 0; i < dataList.size(); i += 1) {
            GeneralGridData data1 = dataList.get(i);
            for (int j = i + 1; j < dataList.size(); j += 1) {
                GeneralGridData data2 = dataList.get(j);
                GeneralGridData merged = GeneralGridData
                        .mergeData(data1, data2);
                if (merged != null) {
                    data1 = merged;
                    dataList.set(i, merged);
                    dataList.remove(j);
                    j -= 1;
                }

            }
        }
        return dataList;
    }

    @Override
    protected void disposeInternal() {
        clearRequestedData();
    }

    /**
     * Dispose of a renderable.
     * 
     * @param renderable
     */
    private void disposeRenderable(final IRenderable renderable) {
        VizApp.runAsync(new Runnable() {

            @Override
            public void run() {
                if (renderable instanceof TileSetRenderable) {
                    ((TileSetRenderable) renderable).dispose();
                } else if (renderable instanceof AbstractGriddedDisplay<?>) {
                    ((AbstractGriddedDisplay<?>) renderable).dispose();
                } else if (renderable instanceof ContourRenderable) {
                    ((ContourRenderable) renderable).dispose();
                } else {
                    System.err.println("Undisposed renderable of type: "
                            + renderable.getClass().getSimpleName());
                }
            }

        });

    }

    @Override
    public void project(CoordinateReferenceSystem crs) throws VizException {
        synchronized (renderableMap) {
            Iterator<List<IRenderable>> iter = renderableMap.values()
                    .iterator();
            while (iter.hasNext()) {
                List<IRenderable> renderableList = iter.next();
                boolean remove = false;
                for (IRenderable renderable : renderableList) {
                    if (!projectRenderable(renderable)) {
                        remove = true;
                        break;
                    }
                }
                // If any one renderable fails to reproject then dispose them
                // all, so that the whole frame gets regenerated.
                if (remove) {
                    for (IRenderable renderable : renderableList) {
                        disposeRenderable(renderable);
                    }
                    iter.remove();
                }
            }
        }
    }

    /**
     * Attempt to reproject the renderable, if the renderable cannot be
     * reprojected return false and it will be disposed and a new one made for
     * the new projection.
     * 
     * @param renderable
     * @return
     * @throws VizException
     */
    protected boolean projectRenderable(IRenderable renderable)
            throws VizException {
        if (renderable instanceof TileSetRenderable) {
            ((TileSetRenderable) renderable).project(descriptor
                    .getGridGeometry());
            return true;
        } else if (renderable instanceof AbstractGriddedDisplay<?>) {
            ((AbstractGriddedDisplay<?>) renderable).reproject();
            return true;
        }
        return false;
    }

    protected DataTime getTimeForResource() {
        return descriptor.getTimeForResource(this);
    }

    protected List<GeneralGridData> getCurrentData() {
        DataTime time = getTimeForResource();
        if (time == null) {
            return null;
        }
        return requestData(time);
    }

    protected Interpolation getInspectInterpolation() {
        Interpolation sampleInterpolion = null;
        if (this.hasCapability(ImagingCapability.class)) {
            ImagingCapability imagingCap = this
                    .getCapability(ImagingCapability.class);
            if (imagingCap.isInterpolationState()) {
                sampleInterpolion = new BilinearInterpolation();
            } else {
                sampleInterpolion = new NearestNeighborInterpolation();
            }
        } else {
            sampleInterpolion = new BilinearInterpolation();
        }
        return sampleInterpolion;
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        Map<String, Object> map = interrogate(coord);
        if (map == null) {
            return "NO DATA";
        }
        double value = (Double) map.get(INTERROGATE_VALUE);
        String result = sampleFormat.format(value) + map.get(INTERROGATE_UNIT);
        // Data mapping images.
        if (hasCapability(ColorMapCapability.class)) {
            ColorMapParameters cmp = getCapability(ColorMapCapability.class)
                    .getColorMapParameters();
            if (cmp.getDataMapping() != null) {
                double imageVal = cmp.getDisplayToImageConverter().convert(
                        value);
                String mapResult = cmp.getDataMapping()
                        .getLabelValueForDataValue(imageVal);
                if (mapResult != null && !mapResult.isEmpty()) {
                    return mapResult;
                }
            }
        }
        if (map.containsKey(INTERROGATE_DIRECTION)) {
            double dir = (Double) map.get(INTERROGATE_DIRECTION);
            result = String.format("%.0f\u00B0 ", dir) + result;
        }
        return result;
    }

    protected Map<String, Object> interrogate(ReferencedCoordinate coord,
            GeneralGridData data) throws VizException {
        Coordinate pixel = null;
        try {
            pixel = coord.asPixel(data.getGridGeometry());
        } catch (TransformException e) {
            // this should never happen, if your data geometry and screen
            // geometry are incompatible then you probably already saw piles of
            // errors in paint.
            throw new VizException(
                    "Error transforming coordinate for interrogate", e);
        } catch (FactoryException e) {
            // again, this should never hit.
            throw new VizException(
                    "Error transforming coordinate for interrogate", e);
        }
        Interpolation interpolation = getInspectInterpolation();
        GridSampler sampler = null;
        if (data.isVector()) {
            sampler = new GridSampler(data.getMagnitude(), interpolation);
        } else {
            sampler = new GridSampler(data.getScalarData(), interpolation);
        }
        double value = sampler.sample(pixel.x, pixel.y);
        if (Double.isNaN(value)) {
            return null;
        }
        String unitString = null;
        Unit<?> unit = data.getDataUnit();
        if (stylePreferences != null) {
            Unit<?> styleUnit = stylePreferences.getDisplayUnits();
            if (unit != null && styleUnit != null
                    && unit.isCompatible(styleUnit)) {
                value = (float) unit.getConverterTo(styleUnit).convert(value);
                unit = styleUnit;
                unitString = stylePreferences.getDisplayUnitLabel();
            }
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(INTERROGATE_VALUE, value);
        if (unitString != null) {
            result.put(INTERROGATE_UNIT, unitString);
        } else if (unit != null && !unit.equals(Unit.ONE)) {
            result.put(INTERROGATE_UNIT,
                    UnitFormat.getUCUMInstance().format(unit));
        } else {
            result.put(INTERROGATE_UNIT, "");
        }
        if (data.isVector()) {
            sampler.setSource(data.getDirectionFrom());
            Double dir = sampler.sample(pixel.x, pixel.y);
            result.put(INTERROGATE_DIRECTION, dir);
        }
        return result;
    }

    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        List<GeneralGridData> dataList = getCurrentData();
        if (dataList == null) {
            return null;
        }
        for (GeneralGridData data : dataList) {
            Map<String, Object> result = interrogate(coord, data);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    @Override
    public void remove(DataTime dataTime) {
        pdoMap.remove(dataTime);
        dataMap.remove(dataTime);
        requestRunner.remove(dataTime);
        dataTimes.remove(dataTime);
        synchronized (renderableMap) {
            List<IRenderable> renderableList = renderableMap.remove(dataTime);
            if (renderableList != null) {
                for (IRenderable renderable : renderableList) {
                    disposeRenderable(renderable);
                }
            }
        }
    }

    /**
     * Shorthand method to get the DisplayType from the capability.
     * 
     * @return
     */
    public DisplayType getDisplayType() {
        return getCapability(DisplayTypeCapability.class).getDisplayType();
    }

    @Override
    public ResourceOrder getResourceOrder() {
        ResourceOrder order = super.getResourceOrder();
        if (order.equals(ResourceOrder.UNKNOWN)) {
            switch (getDisplayType()) {
            case IMAGE:
                order = RenderingOrderFactory.getRenderingOrder("IMAGE_WORLD");
                break;
            default:
                order = RenderingOrderFactory.getRenderingOrder("CONTOUR");
            }
        }
        return order;
    }

    /**
     * Reset renderables and any other data caches, data will be rerequested
     * next time it is needed.
     * 
     */
    protected void clearRequestedData() {
        requestRunner.stopAndClear();
        synchronized (renderableMap) {
            for (List<IRenderable> renderableList : renderableMap.values()) {
                for (IRenderable renderable : renderableList) {
                    disposeRenderable(renderable);
                }
            }
            renderableMap.clear();
        }
        dataMap.clear();
    }

    protected List<PluginDataObject> getCurrentPluginDataObjects() {
        return getPluginDataObjects(getTimeForResource());
    }

    protected List<PluginDataObject> getPluginDataObjects(DataTime time) {
        if (time == null) {
            return null;
        }
        List<PluginDataObject> list = pdoMap.get(time);
        if (list == null) {
            return null;
        }
        return new ArrayList<PluginDataObject>(list);
    }

    public Collection<DrawableImage> getImages(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        if (getCapability(DisplayTypeCapability.class).getDisplayType() != DisplayType.IMAGE) {
            throw new VizException(
                    "Grid resource not configured for image rendering");
        }
        Collection<IRenderable> renderables = getOrCreateRenderables(target,
                paintProps);
        if (renderables.isEmpty()) {
            return Collections.emptyList();
        }

        List<DrawableImage> images = new ArrayList<DrawableImage>();
        for (IRenderable renderable : renderables) {
            images.addAll(((TileSetRenderable) renderable).getImagesToRender(
                    target, paintProps));
        }
        return images;
    }

    protected Collection<IRenderable> getOrCreateRenderables(
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        DataTime time = paintProps.getDataTime();
        if (time == null) {
            time = getTimeForResource();
        }
        if (time == null) {
            return Collections.emptyList();
        }

        List<IRenderable> renderables;

        synchronized (renderableMap) {
            if (renderableMap.containsKey(time)) {
                renderables = renderableMap.get(time);
            } else {
                List<GeneralGridData> dataList = requestData(time);
                if (dataList == null) {
                    updatePaintStatus(PaintStatus.INCOMPLETE);
                    return Collections.emptyList();
                }

                renderables = new ArrayList<IRenderable>(dataList.size());
                for (GeneralGridData data : dataList) {
                    IRenderable renderable = createRenderable(target, data);
                    if (renderable != null) {
                        renderables.add(renderable);
                    }
                }
                renderableMap.put(time, renderables);
            }
        }
        return renderables;
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        for (IRenderable renderable : getOrCreateRenderables(target, paintProps)) {
            renderable.paint(target, paintProps);
        }
    }

    public Unit<?> getDisplayUnit() {
        if (stylePreferences != null) {
            return stylePreferences.getDisplayUnits();
        } else {
            return null;
        }
    }
}
