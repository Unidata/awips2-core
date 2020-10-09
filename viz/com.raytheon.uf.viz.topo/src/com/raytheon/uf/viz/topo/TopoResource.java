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

package com.raytheon.uf.viz.topo;

import java.io.File;
import java.text.DecimalFormat;
import java.util.Arrays;

import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.raytheon.uf.common.colormap.ColorMapException;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters.PersistedParameters;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.ReferencedCoordinate;
import com.raytheon.uf.common.style.ImageryLabelingPreferences;
import com.raytheon.uf.common.style.ParamLevelMatchCriteria;
import com.raytheon.uf.common.style.StyleException;
import com.raytheon.uf.common.style.StyleManager;
import com.raytheon.uf.common.style.StyleManager.StyleType;
import com.raytheon.uf.common.style.StyleRule;
import com.raytheon.uf.common.style.image.DataScale;
import com.raytheon.uf.common.style.image.ImagePreferences;
import com.raytheon.uf.common.style.image.NumericFormat;
import com.raytheon.uf.common.style.image.SampleFormat;
import com.raytheon.uf.common.topo.TopoUtils;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.map.IMapDescriptor;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.tile.TileSetRenderable;
import com.raytheon.uf.viz.core.tile.TileSetRenderable.TileImageCreator;

import si.uom.SI;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * Provides an SRTM hdf5-backed topographic map
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 14, 2007           chammack  Initial Creation.
 * Apr 03, 2013  1562     mschenke  Fix for custom colormaps
 * Apr 24, 2013  1638     mschenke  Made topo configurable for source data
 * Aug 06, 2013  2235     bsteffen  Added Caching version of TopoQuery.
 * Aug 05, 2016  4906     randerso  Added no data value to color map parameters
 * Apr 04, 2018  6889     njensen   Use brightness from ImagePreferences if
 *                                  present but missing in ImagingCapability
 * Apr 20, 2020  8145     randerso  Replace SamplePreferences with SampleFormat
 *
 * </pre>
 *
 * @author chammack
 */
public class TopoResource
        extends AbstractVizResource<TopoResourceData, IMapDescriptor> {

    private IResourceDataChanged changeListener = new IResourceDataChanged() {
        @Override
        public void resourceChanged(ChangeType type, Object object) {
            issueRefresh();
        }
    };

    protected File dataFile;

    protected TileSetRenderable topoTileSet;

    private double noDataValue;

    protected TopoResource(TopoResourceData topoData,
            LoadProperties loadProperties, File dataFile) {
        super(topoData, loadProperties);
        this.dataFile = dataFile;
    }

    @Override
    protected void disposeInternal() {
        if (topoTileSet != null) {
            topoTileSet.dispose();
            topoTileSet = null;
        }
        resourceData.removeChangeListener(changeListener);
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        resourceData.addChangeListener(changeListener);

        // TODO: create topo style rules for topo and bathymetric topo
        ParamLevelMatchCriteria criteria = new ParamLevelMatchCriteria();
        criteria.setParameterName(Arrays.asList(resourceData.getTopoFile()));
        StyleRule styleRule;
        try {
            styleRule = StyleManager.getInstance()
                    .getStyleRule(StyleType.IMAGERY, criteria);
        } catch (StyleException e) {
            throw new VizException(e.getLocalizedMessage(), e);
        }

        // Default colormap
        String colorMapName = "topo";

        ColorMapParameters params = getCapability(ColorMapCapability.class)
                .getColorMapParameters();
        PersistedParameters persisted = null;
        if (params == null) {
            params = new ColorMapParameters();
        } else {
            persisted = params.getPersisted();
        }

        // Set data unit, specify in resource data? Look up in data record?
        params.setDataUnit(SI.METRE);
        params.setDisplayUnit(SI.METRE);
        params.setColorMapUnit(SI.METRE);
        params.setColorMapMin(-19);
        params.setColorMapMax(5000);
        params.setDataMin(Short.MIN_VALUE);
        params.setDataMax(Short.MAX_VALUE);
        params.setFormatString("0");
        params.setNoDataValue(Short.MIN_VALUE);

        ImagingCapability imgCap = getCapability(ImagingCapability.class);

        if (styleRule != null) {
            // TODO: This basic logic should be extracted somewhere,
            // ColorMapParametersFactory has become overkill of any basic kind
            // of colormapping based on style rules and is extremely grib
            // specific
            ImagePreferences prefs = (ImagePreferences) styleRule
                    .getPreferences();
            Unit<?> prefDisplayUnit = prefs.getDisplayUnits();
            if (prefDisplayUnit != null) {
                params.setDisplayUnit(prefDisplayUnit);
            }

            DataScale scale = prefs.getDataScale();
            if (scale != null) {
                UnitConverter displayToColorMap = params
                        .getDisplayToColorMapConverter();
                Double minVal = scale.getMinValue();
                Double maxVal = scale.getMaxValue();
                if (minVal != null) {
                    params.setColorMapMin(
                            displayToColorMap.convert(minVal).floatValue());
                }
                if (maxVal != null) {
                    params.setColorMapMax(
                            displayToColorMap.convert(maxVal).floatValue());
                }
            }

            String defaultCmap = prefs.getDefaultColormap();
            if (defaultCmap != null) {
                colorMapName = defaultCmap;
            }

            SampleFormat sampleFormat = prefs.getSampleFormat();
            if (sampleFormat instanceof NumericFormat) {
                params.setFormatString(
                        ((NumericFormat) sampleFormat).getPattern());
            }

            ImageryLabelingPreferences labelPrefs = prefs.getColorbarLabeling();
            if ((labelPrefs != null) && (labelPrefs.getValues() != null)) {
                params.setColorBarIntervals(labelPrefs.getValues());
            }

            if (!imgCap.isBrightnessSet() && prefs.getBrightness() != null) {
                imgCap.setBrightness(prefs.getBrightness());
            }
        }

        if (params.getColorMap() == null) {
            if (params.getColorMapName() != null) {
                // Use one specified in params over style rules
                colorMapName = params.getColorMapName();
            }
            try {
                params.setColorMap(ColorMapLoader.loadColorMap(colorMapName));
            } catch (ColorMapException e) {
                statusHandler.error(e.getLocalizedMessage(), e);
            }
        }

        if (persisted != null) {
            params.applyPersistedParameters(persisted);
        }

        getCapability(ColorMapCapability.class).setColorMapParameters(params);

        IDataStore dataStore = DataStoreFactory.getDataStore(this.dataFile);
        try {
            IDataRecord rec = dataStore.retrieve("/", "full",
                    Request.buildPointRequest(new java.awt.Point(0, 0)));
            noDataValue = rec.getFillValue().doubleValue();
        } catch (Exception e) {
            statusHandler.error(e.getLocalizedMessage(), e);
            noDataValue = Double.NaN;
        }

        topoTileSet = new TileSetRenderable(imgCap, getTopoGeometry(),
                getTopoTileImageCreator(), getNumberOfTopoLevels(), 512);
        topoTileSet.project(descriptor.getGridGeometry());
    }

    protected TileImageCreator getTopoTileImageCreator() {
        return new TopoTileImageCreator(this, dataFile);
    }

    private int getNumberOfTopoLevels() throws VizException {
        IDataStore ds = DataStoreFactory.getDataStore(dataFile);
        try {
            return ds.getDatasets("/interpolated").length + 1;
        } catch (Exception e) {
            throw new VizException("Error getting interpolation levels", e);
        }
    }

    private GridGeometry2D getTopoGeometry() throws VizException {
        IDataStore ds = DataStoreFactory.getDataStore(dataFile);
        try {
            return TopoUtils.getTopoGeometry(ds,
                    TopoUtils.getDatasetForLevel(0));
        } catch (Exception e) {
            throw new VizException("Error getting grid geometry", e);
        }
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        if (topoTileSet != null) {
            topoTileSet.paint(target, paintProps);
        }
    }

    @Override
    public void project(CoordinateReferenceSystem mapData) throws VizException {
        if (topoTileSet != null) {
            topoTileSet.project(descriptor.getGridGeometry());
        }
    }

    @Override
    public String inspect(ReferencedCoordinate coord) throws VizException {
        double height;
        try {
            height = topoTileSet.interrogate(coord.asLatLon(), noDataValue);
        } catch (Exception e) {
            throw new VizException("Error transforming", e);
        }
        if (!Double.isNaN(height)) {
            ColorMapParameters parameters = getCapability(
                    ColorMapCapability.class).getColorMapParameters();
            UnitConverter cvt = parameters.getDataToDisplayConverter();

            DecimalFormat df = new DecimalFormat("0.00");
            return String.format("%s %s ", df.format(cvt.convert(height)),
                    SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII)
                            .format(parameters.getDisplayUnit()));
        }
        return "NO DATA";
    }

}
