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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.IncommensurableException;
import javax.measure.Quantity;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.colormap.ColorMapException;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.image.ColorMapData.ColorMapDataType;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters.PersistedParameters;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
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
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.style.image.SampleFormat;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.VizApp;
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
import com.raytheon.uf.viz.core.grid.rsc.data.ScalarGridData;
import com.raytheon.uf.viz.core.grid.rsc.data.VectorGridData;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.point.display.VectorGraphicsConfig;
import com.raytheon.uf.viz.core.rsc.AbstractRequestableResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
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
import com.raytheon.uf.viz.core.rsc.interrogation.ClassInterrogationKey;
import com.raytheon.uf.viz.core.rsc.interrogation.Interrogatable;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogateMap;
import com.raytheon.uf.viz.core.rsc.interrogation.InterrogationKey;
import com.raytheon.uf.viz.core.rsc.interrogation.Interrogator;
import com.raytheon.uf.viz.core.rsc.interrogation.StringInterrogationKey;
import com.raytheon.uf.viz.core.tile.DataSourceTileImageCreator;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;
import com.raytheon.viz.core.contours.ContourRenderable;
import com.raytheon.viz.core.contours.ContourSupport;
import com.raytheon.viz.core.contours.rsc.displays.GriddedContourDisplay;
import com.raytheon.viz.core.contours.rsc.displays.GriddedStreamlineDisplay;

import tech.units.indriya.AbstractUnit;
import tech.units.indriya.format.SimpleUnitFormat;
import tech.units.indriya.quantity.Quantities;

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
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Mar 09, 2011  7738     bsteffen     Initial creation
 * May 08, 2013  1980     bsteffen     Set paint status in GridResources for
 *                                     KML.
 * Jul 15, 2013  2107     bsteffen     Fix sampling of grid vector arrows.
 * Aug 27, 2013  2287     randerso     Added new parameters required by
 *                                     GriddedVectorDisplay and
 *                                     GriddedIconDisplay
 * Sep 24, 2013  2404     bclement     colormap params now created using match
 *                                     criteria
 * Sep 23, 2013  2363     bsteffen     Add more vector configuration options.
 * Jan 14, 2014  2594     bsteffen     Switch vector mag/dir to use data source
 *                                     instead of raw float data.
 * Feb 28, 2014  2791     bsteffen     Switch all data to use data source.
 * Aug 21, 2014  17313    jgerth       Implements ImageProvider
 * Oct 07, 2014  3668     bclement     Renamed requestJob to requestRunner
 * Dec 09, 2014  5056     jing         Added data access interfaces
 * May 11, 2015  4384     dgilling     Add arrow style preference for minimum
 *                                     magnitude.
 * May 14, 2015  4079     bsteffen     Move to core.grid, add getDisplayUnit
 * Aug 30, 2016  3240     bsteffen     Implement Interrogatable
 * Apr 26, 2017  6247     bsteffen     Provide getter/setter for style
 *                                     preferences.
 * Nov 28, 2017  5863     bsteffen     Change dataTimes to a NavigableSet
 * Feb 15, 2018  6902     njensen      Added interrogate support for Direction
 *                                     To
 * Mar 21, 2018  7157     njensen      Improved if statement in
 *                                     createColorMapParameters()
 * Apr 04, 2018  6889     njensen      Use brightness from ImagePreferences if
 *                                     present but missing in ImagingCapability
 * Nov 15, 2018  57905    edebebe      Enabled configurable 'Wind Barb'
 *                                     properties
 * Feb 28, 2019  7713     tjensen      Fix wind barb config
 * Apr 15, 2019  7596     lsingh       Updated units framework to JSR-363
 * Jun 27, 2019  65510    ksunil       Support color fill through XML entries,
 *                                     support smoothData
 * Jul 30, 2019  66477    mapeters     Don't set minimumMagnitude based off
 *                                     arrowHeadSizeRatio
 * Aug 29, 2019  67949    tjensen      Refactor to support additional GFE
 *                                     products
 * Nov 11, 2019  7596     mrichardson  Fix unit conversion for sampling
 * Dec 02, 2019  71870    tjensen      Make disposeRenderable visible to be
 *                                     overridden
 * Apr 16, 2020  8145     randerso     Updated to allow new sample formatting
 * Jul 19, 2021  8462     randerso     Updated to use DataMappingPreferences
 *                                     sample value if defined.
 * Aug 31, 2021  8651     njensen      Changed ConcurrentHashMaps' visibilities
 *                                     from private to protected
 * Dec 06, 2021  8341     randerso     Added use of getResourceId for contour
 *                                     logging
 * Dec 20, 2023  2036519  mapeters     Prevent keeping unnecessary data in memory
 *
 * </pre>
 *
 * @author bsteffen
 * @param <T>
 */
public abstract class AbstractGridResource<T extends AbstractResourceData>
        extends AbstractVizResource<T, IMapDescriptor>
        implements ImageProvider, Interrogatable {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractGridResource.class);

    // Parameters used to construct 'VectorGraphicsConfig'
    private static final String PLUGIN_NAME = "GeolocatedGridDataDisplays";

    private static final String CLASS_NAME = "AbstractGridResource";

    /* Unknown source, provides acceptable density. */
    private static final double VECTOR_DENSITY_FACTOR = 1.875;

    private static final int IMAGE_TILE_SIZE = 1024;

    /** @deprecated Use {@link #DIRECTION_FROM_INTERROGATE_KEY} instead */
    @Deprecated
    public static final InterrogationKey<Number> DIRECTION_INTERROGATE_KEY = new StringInterrogationKey<>(
            "GridVectorDirection", Number.class);

    public static final InterrogationKey<Number> DIRECTION_FROM_INTERROGATE_KEY = new StringInterrogationKey<>(
            "GridVectorDirectionFrom", Number.class);

    public static final InterrogationKey<Number> DIRECTION_TO_INTERROGATE_KEY = new StringInterrogationKey<>(
            "GridVectorDirectionTo", Number.class);

    /**
     * This is needed because the user wants to see the unit exactly as it
     * appears in the database and/or style rules. The Unit<?> object in
     * {@link Interrogator#VALUE} has been parsed from this String but it is not
     * guaranteed to get reformated the same way.
     */
    protected static final InterrogationKey<String> UNIT_STRING_INTERROGATE_KEY = new StringInterrogationKey<>(
            "unitString", String.class);

    private static final InterrogationKey<GeographicDataSource> DATA_SOURCE_INTERROGATE_KEY = new ClassInterrogationKey<>(
            GeographicDataSource.class);

    /** @deprecated Use {@link Interrogator#VALUE} */
    @Deprecated
    protected static final String INTERROGATE_VALUE = "value";

    /** @deprecated Use {@link Interrogator#VALUE} */
    @Deprecated
    protected static final String INTERROGATE_UNIT = "unit";

    /** @deprecated Use {@link #DIRECTION_FROM_INTERROGATE_KEY} */
    @Deprecated
    protected static final String INTERROGATE_DIRECTION = "direction";

    protected final GridDataRequestRunner requestRunner;

    protected final Map<DataTime, List<PluginDataObject>> pdoMap = new ConcurrentHashMap<>();

    protected final Map<DataTime, List<IRenderable>> renderableMap = new ConcurrentHashMap<>();

    /**
     * This is a local cache of data that is used when sampling or reprojected.
     */
    protected final Map<DataTime, List<GeneralGridData>> dataMap = new ConcurrentHashMap<>();

    /**
     * StylePreferences from the styleManager appropriate for the display type
     * provided.
     */
    protected AbstractStylePreferences stylePreferences = null;

    /**
     * The format used by the default inspect method
     */
    protected SampleFormat sampleFormat = new NumericFormat("0.00");

    protected AbstractGridResource(T resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties, false);
        resourceData.addChangeListener((type, object) -> {
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
        });
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
        if (getStatus() == ResourceStatus.DISPOSED) {
            return;
        }
        DataTime time = pdo.getDataTime();
        if (this.resourceData instanceof AbstractRequestableResourceData) {
            AbstractRequestableResourceData resourceData = (AbstractRequestableResourceData) this.resourceData;
            if (resourceData.getBinOffset() != null) {
                time = resourceData.getBinOffset().getNormalizedTime(time);
            }
        }
        List<PluginDataObject> pdos = this.pdoMap.get(time);
        if (pdos == null) {
            pdos = new ArrayList<>();
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
        dataTimes.add(time);
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
                gridDisplay.setColor(
                        getCapability(ColorableCapability.class).getColor());
            }
            if (capability instanceof MagnificationCapability) {
                gridDisplay.setMagnification(
                        getCapability(MagnificationCapability.class)
                                .getMagnification());
            }
            if (capability instanceof DensityCapability) {
                gridDisplay.setDensity(
                        getCapability(DensityCapability.class).getDensity());
            }
            if (gridDisplay instanceof GriddedVectorDisplay) {
                GriddedVectorDisplay vectorDisplay = (GriddedVectorDisplay) gridDisplay;
                vectorDisplay.setLineStyle(
                        getCapability(OutlineCapability.class).getLineStyle());
                vectorDisplay
                        .setLineWidth(getCapability(OutlineCapability.class)
                                .getOutlineWidth());
            }
        } else if (renderable instanceof ContourRenderable) {
            ContourRenderable contourRenderable = (ContourRenderable) renderable;
            if (capability instanceof ColorableCapability) {
                contourRenderable.setColor(
                        getCapability(ColorableCapability.class).getColor());
            }
            if (capability instanceof MagnificationCapability) {
                contourRenderable.setMagnification(
                        getCapability(MagnificationCapability.class)
                                .getMagnification());
            }
            if (capability instanceof DensityCapability) {
                contourRenderable.setDensity(
                        getCapability(DensityCapability.class).getDensity());
            }
            if (capability instanceof OutlineCapability) {
                contourRenderable.setLineStyle(
                        getCapability(OutlineCapability.class).getLineStyle());
                contourRenderable
                        .setOutlineWidth(getCapability(OutlineCapability.class)
                                .getOutlineWidth());
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
        List<DisplayType> altDisplayTypes = new ArrayList<>();
        switch (displayType) {
        case IMAGE:
            /*
             * if this was loaded from a bundle, image capability will already
             * exist
             */
            if (!hasCapability(ImagingCapability.class)) {
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
            altDisplayTypes.add(DisplayType.IMAGE);
            getCapability(ColorableCapability.class);
            getCapability(DensityCapability.class);
            getCapability(MagnificationCapability.class);
            getCapability(OutlineCapability.class);
            break;
        case DUALARROW:
            altDisplayTypes.add(DisplayType.ARROW);
            altDisplayTypes.add(DisplayType.IMAGE);
            getCapability(ColorableCapability.class);
            getCapability(DensityCapability.class);
            getCapability(MagnificationCapability.class);
            getCapability(OutlineCapability.class);
            break;
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
                styleRule = StyleManager.getInstance()
                        .getStyleRule(StyleType.IMAGERY, criteria);
                break;
            case CONTOUR:
            case STREAMLINE:
                styleRule = StyleManager.getInstance()
                        .getStyleRule(StyleType.CONTOUR, criteria);
                break;
            case BARB:
            case ARROW:
            case DUALARROW:
                styleRule = StyleManager.getInstance()
                        .getStyleRule(StyleType.ARROW, criteria);
                break;
            }
        }
        if (styleRule != null) {
            stylePreferences = styleRule.getPreferences();
            if (stylePreferences instanceof ImagePreferences) {
                ImagePreferences imgPrefs = (ImagePreferences) stylePreferences;
                ImagingCapability imgCap = getCapability(
                        ImagingCapability.class);
                boolean interpolationState = imgPrefs.isInterpolate();
                imgCap.setInterpolationState(interpolationState);
                if (!imgCap.isBrightnessSet()
                        && imgPrefs.getBrightness() != null) {
                    imgCap.setBrightness(imgPrefs.getBrightness());
                }
            }
        }

        /*
         * Handle case where it's grid imagery and image brightness did not come
         * from style rules or a bundle
         */
        if (hasCapability(ImagingCapability.class)) {
            ImagingCapability imgCap = getCapability(ImagingCapability.class);
            if (!imgCap.isBrightnessSet()) {
                imgCap.setBrightness(0.5f);
            }
        }
    }

    /**
     * Create an interpolation and format to be used when sampling
     */
    protected void initSampling() {
        if (stylePreferences instanceof ImagePreferences) {
            ImagePreferences prefs = (ImagePreferences) stylePreferences;
            sampleFormat = prefs.getSampleFormat();
        }
    }

    /**
     * Create a renderable for this data.
     *
     * @param target
     * @param data
     * @param time
     * @return
     * @throws VizException
     */
    public IRenderable createRenderable(IGraphicsTarget target,
            GeneralGridData data, DataTime time) throws VizException {
        IRenderable renderable = null;

        GridGeometry2D gridGeometry = data.getGridGeometry();

        DisplayType displayType = getDisplayType();

        switch (displayType) {
        case IMAGE:
            try {
                StyleRule sr = StyleManager.getInstance().getStyleRule(
                        StyleManager.StyleType.IMAGERY, getMatchCriteria());
                if (sr != null) {
                    ImagePreferences preferences = (ImagePreferences) sr
                            .getPreferences();
                    if (gridGeometry != null && preferences != null
                            && preferences.getSmoothingDistance() != null) {
                        ((ScalarGridData) data)
                                .setScalarData(ContourSupport.smoothData(
                                        new DataSource[] {
                                                ((ScalarGridData) data)
                                                        .getScalarData() },
                                        gridGeometry,
                                        preferences.getSmoothingDistance())[0]);
                    }
                }
            } catch (Exception e1) {
                statusHandler.handle(Priority.WARN,
                        "Unable to honor the specified smoothing request. ",
                        e1);
            }

            ColorMapCapability colorMapCap = getCapability(
                    ColorMapCapability.class);
            ImagingCapability imagingCap = getCapability(
                    ImagingCapability.class);
            ColorMapParameters params = null;
            if (renderableMap.isEmpty()) {
                params = createColorMapParameters(data);
                if (params.getColorMap() == null) {
                    if (params.getColorMapName() == null) {
                        params.setColorMapName("Grid/gridded data");
                    }
                    try {
                        params.setColorMap(ColorMapLoader
                                .loadColorMap(params.getColorMapName()));
                    } catch (ColorMapException e) {
                        throw new VizException(e);
                    }
                }
                colorMapCap.setColorMapParameters(params);
            } else {
                params = colorMapCap.getColorMapParameters();
            }
            if (params.getDataMapping() != null) {
                data.convert(params.getColorMapUnit());
            }
            TileImageCreator creator = new DataSourceTileImageCreator(
                    data.getData(), data.getDataUnit(), ColorMapDataType.FLOAT,
                    colorMapCap);
            TileSetRenderable tsr = new TileSetRenderable(imagingCap,
                    gridGeometry, creator, 1, IMAGE_TILE_SIZE);
            tsr.project(descriptor.getGridGeometry());
            renderable = tsr;
            break;
        case BARB:
        case ARROW:
        case DUALARROW:
            convertData(data);
            VectorGraphicsConfig config = new VectorGraphicsConfig(PLUGIN_NAME,
                    CLASS_NAME);
            if (displayType != DisplayType.BARB) {
                config.disableCalmCircle();
                if (stylePreferences instanceof ArrowPreferences) {
                    double scale = ((ArrowPreferences) stylePreferences)
                            .getScale();
                    if (scale >= 0.0) {
                        config.setLinearArrowScaleFactor(scale);
                    } else {
                        config.setArrowScaler(new LogArrowScaler(-1 * scale));
                    }

                    double minMagnitude = ((ArrowPreferences) stylePreferences)
                            .getMinimumMagnitude();
                    if (!Double.isNaN(minMagnitude)) {
                        config.setMinimumMagnitude(minMagnitude);
                    }
                }
            }
            if (data instanceof VectorGridData) {
                GriddedVectorDisplay vectorDisplay = new GriddedVectorDisplay(
                        ((VectorGridData) data).getMagnitude(),
                        ((VectorGridData) data).getDirectionFrom(), descriptor,
                        gridGeometry, VECTOR_DENSITY_FACTOR, true, displayType,
                        config);
                vectorDisplay.setColor(
                        getCapability(ColorableCapability.class).getColor());
                vectorDisplay.setLineStyle(
                        getCapability(OutlineCapability.class).getLineStyle());
                vectorDisplay
                        .setLineWidth(getCapability(OutlineCapability.class)
                                .getOutlineWidth());
                vectorDisplay.setDensity(
                        getCapability(DensityCapability.class).getDensity());
                vectorDisplay.setMagnification(
                        getCapability(MagnificationCapability.class)
                                .getMagnification());
                renderable = vectorDisplay;
            } else {
                throw new VizException(
                        "Unexpected data type for arrow/barb in createRenderable: "
                                + data.getClass());
            }
            break;
        case ICON:
            GriddedIconDisplay iconDisplay = new GriddedIconDisplay(
                    data.getData(), descriptor, gridGeometry, 80, 0.75);
            iconDisplay.setColor(
                    getCapability(ColorableCapability.class).getColor());
            iconDisplay.setDensity(
                    getCapability(DensityCapability.class).getDensity());
            iconDisplay.setMagnification(
                    getCapability(MagnificationCapability.class)
                            .getMagnification());
            renderable = iconDisplay;
            break;
        case CONTOUR:
        case STREAMLINE:
            convertData(data);
            GriddedContourDisplay contourRenderable = null;
            if (displayType == DisplayType.CONTOUR) {
                contourRenderable = new GriddedContourDisplay(descriptor,
                        getResourceId(time), gridGeometry, data.getData());
            } else if (data instanceof VectorGridData) {
                contourRenderable = new GriddedStreamlineDisplay(descriptor,
                        getSafeName(), gridGeometry,
                        ((VectorGridData) data).getUComponent(),
                        ((VectorGridData) data).getVComponent());
            } else {
                throw new VizException(
                        "Unexpected data type for streamline in createRenderable: "
                                + data.getClass());
            }

            contourRenderable.setColor(
                    getCapability(ColorableCapability.class).getColor());
            contourRenderable.setLineStyle(
                    getCapability(OutlineCapability.class).getLineStyle());
            contourRenderable.setOutlineWidth(
                    getCapability(OutlineCapability.class).getOutlineWidth());
            contourRenderable.setDensity(
                    getCapability(DensityCapability.class).getDensity());
            contourRenderable.setMagnification(
                    getCapability(MagnificationCapability.class)
                            .getMagnification());
            if (stylePreferences instanceof ContourPreferences) {
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
        DataSource source = data.getData();
        MinMax mm = DataUtilities.getMinMax(source, range.getSpan(0),
                range.getSpan(1));
        try {
            newParameters = ColorMapParameterFactory.build((float) mm.getMin(),
                    (float) mm.getMax(), data.getDataUnit(), criteria);
        } catch (StyleException e) {
            throw new VizException("Unable to build colormap parameters", e);
        }
        ColorMapParameters oldParameters = this
                .getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        if (oldParameters != null
                && oldParameters.getColorMapMin() <= newParameters
                        .getColorMapMin()
                && oldParameters.getColorMapMax() >= newParameters
                        .getColorMapMax()
                && oldParameters.getColorMapMax() != oldParameters
                        .getColorMapMin()) {
            /*
             * If the oldParameters have a larger range than the new parameters,
             * reuse the old parameters. This is useful when the resource is
             * sharing capabilities, for example in an FFGVizGroupResource.
             */
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
        return requestData(time, false);
    }

    /**
     * Request data for the given time.
     *
     * @param time
     *            the time to request data for
     * @param fromCacheOnly
     *            true to only get the data if it's already cached and to do
     *            nothing otherwise, false to trigger a request for the data if
     *            it's not yet cached
     * @return the data if it's immediately available, otherwise null
     */
    protected List<GeneralGridData> requestData(DataTime time,
            boolean fromCacheOnly) {
        synchronized (requestRunner) {
            List<GeneralGridData> data = this.dataMap.get(time);
            if (data == null) {
                data = requestRunner.requestData(time, pdoMap.get(time),
                        fromCacheOnly);
                if (data != null) {
                    data = mergeData(data);
                    cacheData(time, data);
                }
            }
            return data;
        }
    }

    protected void cacheData(DataTime time, List<GeneralGridData> data) {
        this.dataMap.put(time, data);
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
                GeneralGridData merged = GeneralGridData.mergeData(data1,
                        data2);
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
    protected void disposeRenderable(final IRenderable renderable) {
        VizApp.runAsync(() -> {
            if (renderable instanceof TileSetRenderable) {
                ((TileSetRenderable) renderable).dispose();
            } else if (renderable instanceof AbstractGriddedDisplay<?>) {
                ((AbstractGriddedDisplay<?>) renderable).dispose();
            } else if (renderable instanceof ContourRenderable) {
                ((ContourRenderable) renderable).dispose();
            } else {
                statusHandler.warn("Undisposed renderable of type: "
                        + renderable.getClass().getSimpleName());
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
            ((TileSetRenderable) renderable)
                    .project(descriptor.getGridGeometry());
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
        InterrogateMap map = interrogate(coord, getTimeForResource(),
                Interrogator.VALUE, UNIT_STRING_INTERROGATE_KEY,
                DIRECTION_FROM_INTERROGATE_KEY);
        if (map == null || map.isEmpty()) {
            return "NO DATA";
        }
        Quantity<?> value = map.get(Interrogator.VALUE);
        if (value == null) {
            return "NO DATA";
        }
        String unit = map.get(UNIT_STRING_INTERROGATE_KEY);
        String result = sampleFormat.format(value.getValue(), unit);
        // Data mapping images.
        if (hasCapability(ColorMapCapability.class)) {
            ColorMapParameters cmp = getCapability(ColorMapCapability.class)
                    .getColorMapParameters();
            if (cmp.getDataMapping() != null) {
                double imageVal = cmp.getDisplayToColorMapConverter()
                        .convert(value.getValue().doubleValue());
                String mapResult = cmp.getDataMapping()
                        .getSampleOrLabelValueForDataValue(imageVal);
                if (mapResult != null && !mapResult.isEmpty()) {
                    return mapResult;
                }
            }
        }
        if (map.containsKey(DIRECTION_FROM_INTERROGATE_KEY)) {
            double dir = map.get(DIRECTION_FROM_INTERROGATE_KEY).doubleValue();
            result = String.format("%.0f\u00B0 ", dir) + result;
        }
        return result;
    }

    @Override
    public Set<InterrogationKey<?>> getInterrogationKeys() {
        Set<InterrogationKey<?>> result = new HashSet<>();
        result.add(Interrogator.VALUE);
        result.add(DIRECTION_INTERROGATE_KEY);
        result.add(DIRECTION_FROM_INTERROGATE_KEY);
        result.add(DIRECTION_TO_INTERROGATE_KEY);
        result.add(DATA_SOURCE_INTERROGATE_KEY);
        return result;
    }

    protected InterrogateMap interrogate(ReferencedCoordinate coordinate,
            GeneralGridData data, InterrogationKey<?>... keys) {
        InterrogateMap result = new InterrogateMap();
        Set<InterrogationKey<?>> keySet = new HashSet<>(Arrays.asList(keys));
        if (keySet.contains(DATA_SOURCE_INTERROGATE_KEY)) {
            result.put(DATA_SOURCE_INTERROGATE_KEY, data.getData());
            keySet.remove(DATA_SOURCE_INTERROGATE_KEY);
        }
        if (!keySet.isEmpty()) {
            Coordinate pixel = null;
            try {
                pixel = coordinate.asPixel(data.getGridGeometry());
            } catch (FactoryException | TransformException e) {
                statusHandler.error(
                        "Error transforming coordinate for interrogate", e);
                return result;
            }
            Interpolation interpolation = getInspectInterpolation();
            if (keySet.contains(Interrogator.VALUE)) {
                GridSampler sampler = new GridSampler(data.getData(),
                        interpolation);
                double value = sampler.sample(pixel.x, pixel.y);
                if (!Double.isNaN(value)) {
                    Unit<?> unit = data.getDataUnit();
                    String unitString = null;
                    if (stylePreferences != null) {
                        Unit<?> styleUnit = stylePreferences.getDisplayUnits();
                        if (unit != null && styleUnit != null
                                && unit.isCompatible(styleUnit)) {
                            try {
                                value = (float) unit
                                        .getConverterToAny(styleUnit)
                                        .convert(value);
                            } catch (UnconvertibleException
                                    | IncommensurableException e) {
                                SimpleUnitFormat stringFormat = SimpleUnitFormat
                                        .getInstance(
                                                SimpleUnitFormat.Flavor.ASCII);
                                statusHandler.handle(Priority.ERROR,
                                        "Unable to convert data unit "
                                                + stringFormat.format(unit)
                                                + " to style unit "
                                                + stringFormat.format(styleUnit)
                                                + ".",
                                        e);
                            }
                            unit = styleUnit;
                            unitString = stylePreferences.getDisplayUnitLabel();
                        }
                    }
                    result.put(Interrogator.VALUE,
                            Quantities.getQuantity(value, unit));
                    if (keySet.contains(UNIT_STRING_INTERROGATE_KEY)) {
                        if (unitString != null) {
                            result.put(UNIT_STRING_INTERROGATE_KEY, unitString);
                        } else if (unit != null
                                && !unit.equals(AbstractUnit.ONE)) {
                            result.put(UNIT_STRING_INTERROGATE_KEY,
                                    SimpleUnitFormat.getInstance(
                                            SimpleUnitFormat.Flavor.ASCII)
                                            .format(unit));
                        } else {
                            result.put(UNIT_STRING_INTERROGATE_KEY, "");
                        }
                    }
                }
            }

            if ((keySet.contains(DIRECTION_INTERROGATE_KEY)
                    || keySet.contains(DIRECTION_FROM_INTERROGATE_KEY)
                    || keySet.contains(DIRECTION_TO_INTERROGATE_KEY))
                    && data instanceof VectorGridData) {
                VectorGridData vectorData = (VectorGridData) data;

                if (keySet.contains(DIRECTION_INTERROGATE_KEY)
                        || keySet.contains(DIRECTION_FROM_INTERROGATE_KEY)) {
                    GridSampler samplerFrom = new GridSampler(
                            vectorData.getDirectionFrom(), interpolation);
                    Double dir = samplerFrom.sample(pixel.x, pixel.y);
                    result.put(DIRECTION_INTERROGATE_KEY, dir);
                    result.put(DIRECTION_FROM_INTERROGATE_KEY, dir);
                }

                if (keySet.contains(DIRECTION_TO_INTERROGATE_KEY)) {
                    GridSampler samplerTo = new GridSampler(
                            vectorData.getDirectionTo(), interpolation);
                    Double dir = samplerTo.sample(pixel.x, pixel.y);
                    result.put(DIRECTION_TO_INTERROGATE_KEY, dir);
                }

            }
        }
        return result;
    }

    @Override
    public InterrogateMap interrogate(ReferencedCoordinate coordinate,
            DataTime time, InterrogationKey<?>... keys) {
        if (time == null) {
            return null;
        }
        List<GeneralGridData> dataList = requestData(time);
        if (dataList == null) {
            return null;
        }
        for (GeneralGridData data : dataList) {
            InterrogateMap result = interrogate(coordinate, data, keys);
            if (result != null) {
                return result;
            }
        }
        return new InterrogateMap();
    }

    /**
     * @deprecated use
     *             {@link #interrogate(ReferencedCoordinate, DataTime, InterrogationKey...)}
     *             instead.
     */
    @Deprecated
    @Override
    public Map<String, Object> interrogate(ReferencedCoordinate coord)
            throws VizException {
        InterrogateMap map = interrogate(coord, getTimeForResource(),
                Interrogator.VALUE, UNIT_STRING_INTERROGATE_KEY,
                DIRECTION_FROM_INTERROGATE_KEY);
        if (map == null || map.isEmpty()) {
            return null;
        }
        Quantity<?> value = map.get(Interrogator.VALUE);
        String unitString = map.get(UNIT_STRING_INTERROGATE_KEY);
        Number direction = map.get(DIRECTION_FROM_INTERROGATE_KEY);

        Map<String, Object> result = new HashMap<>();
        if (value != null) {
            result.put(INTERROGATE_VALUE, value.getValue());
        }
        if (unitString != null) {
            result.put(INTERROGATE_UNIT, unitString);
        }
        if (direction != null) {
            result.put(INTERROGATE_DIRECTION, direction);
        }
        return result;
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
     * Reset renderables and any other data caches, data will be re-requested
     * next time it is needed.
     */
    protected void clearRequestedData() {
        requestRunner.clearRequests();
        synchronized (renderableMap) {
            for (List<IRenderable> renderableList : renderableMap.values()) {
                for (IRenderable renderable : renderableList) {
                    disposeRenderable(renderable);
                }
            }
            renderableMap.clear();
        }
        dataMap.clear();
        if (getStatus() == ResourceStatus.DISPOSED) {
            pdoMap.clear();
        }
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
        return new ArrayList<>(list);
    }

    @Override
    public Collection<DrawableImage> getImages(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        if (getCapability(DisplayTypeCapability.class)
                .getDisplayType() != DisplayType.IMAGE) {
            throw new VizException(
                    "Grid resource not configured for image rendering");
        }
        Collection<IRenderable> renderables = getOrCreateRenderables(target,
                paintProps);
        if (renderables.isEmpty()) {
            return Collections.emptyList();
        }

        List<DrawableImage> images = new ArrayList<>();
        for (IRenderable renderable : renderables) {
            images.addAll(((TileSetRenderable) renderable)
                    .getImagesToRender(target, paintProps));
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

                renderables = new ArrayList<>(dataList.size());
                for (GeneralGridData data : dataList) {
                    IRenderable renderable = createRenderable(target, data,
                            time);
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
        for (IRenderable renderable : getOrCreateRenderables(target,
                paintProps)) {
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

    public AbstractStylePreferences getStylePreferences() {
        return stylePreferences.clone();
    }

    public void setStylePreferences(AbstractStylePreferences stylePreferences) {
        this.stylePreferences = stylePreferences.clone();
        synchronized (renderableMap) {
            for (List<IRenderable> renderableList : renderableMap.values()) {
                for (IRenderable renderable : renderableList) {
                    disposeRenderable(renderable);
                }
            }
            renderableMap.clear();
        }
    }
}
