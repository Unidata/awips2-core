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
package com.raytheon.uf.viz.core.tile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.measure.Unit;
import javax.measure.format.UnitFormat;

import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.geospatial.CRSCache;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.units.UnitConv;
import com.raytheon.uf.viz.core.Activator;
import com.raytheon.uf.viz.core.DrawableImage;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IMesh;
import com.raytheon.uf.viz.core.drawables.IColormappedImage;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.drawables.IImage.Status;
import com.raytheon.uf.viz.core.drawables.IRenderable;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.jobs.JobPool;
import com.raytheon.uf.viz.core.localization.HierarchicalPreferenceStore;
import com.raytheon.uf.viz.core.preferences.PreferenceConstants;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;

import tec.uom.se.format.SimpleUnitFormat;

/**
 * Renderable tile set class that creates a {@link TileSet} and renders images
 * for tiles displayed using the {@link TileImageCreator} passed in at
 * construction
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 08, 2012           mschenke  Initial creation
 * May 28, 2013  2037     njensen   Made imageMap concurrent to fix leak
 * Jun 20, 2013  2122     mschenke  Fixed null pointer in interrogate and made
 *                                  canceling jobs safer
 * Oct 16, 2013  2333     mschenke  Added auto NaN checking for interrogation
 * Nov 14, 2013  2492     mschenke  Added more interrogate methods that take
 *                                  units
 * Feb 07, 2014  2211     bsteffen  Fix sampling units when data mapping is
 *                                  enabled.
 * Aug 03, 2016  5786     bsteffen  Add method for scheduling tile loading
 * Mar 29, 2017  6202     bsteffen  Add pixel density preference.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class TileSetRenderable implements IRenderable {

    public static interface TileImageCreator {

        /**
         * Create a complete DrawableImage for the given tile on the
         * targetGeometry
         */
        public DrawableImage createTileImage(IGraphicsTarget target, Tile tile,
                GeneralGridGeometry targetGeometry) throws VizException;

    }

    private class TileImageCreatorTask implements Runnable {

        private IGraphicsTarget target;

        private Tile tile;

        private TileImageCreatorTask(IGraphicsTarget target, Tile tile) {
            this.target = target;
            this.tile = tile;
        }

        @Override
        public void run() {
            try {
                DrawableImage di = tileCreator.createTileImage(target, tile,
                        tileSet.getTargetGeometry());
                if (di != null) {
                    di.getImage().stage();
                }
                addTileImage(tile, di);
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(),
                        e);
            }
        }
    }

    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(TileSetRenderable.class);

    /** Screen pixel to image pixel threshold at which we change tile levels */
    protected double levelChangeThreshold;

    private IPropertyChangeListener levelChangeThresholdListener;

    /** Job pool for tile creation */
    protected static final JobPool tileCreationPool = new JobPool(
            "Creating Image Tiles", 10, false);

    /** Job map, should only have one job running per tile at a time */
    protected Map<Tile, Runnable> jobMap = new ConcurrentHashMap<>();

    /** Image map for tiles */
    protected Map<Tile, DrawableImage> imageMap = new ConcurrentHashMap<>();

    /** Full resolution tile set GridGeometry2D */
    protected final GridGeometry2D tileSetGeometry;

    protected final TileImageCreator tileCreator;

    /** Desired size for each tile */
    protected final int tileSize;

    /** Number of tile levels to create */
    protected final int tileLevels;

    /** {@link TileSet} object, manages tiles */
    protected TileSet tileSet;

    /** Transform for tileset CRS to lat/lon */
    protected final MathTransform localProjToLL;

    /** Transform for lat/lon to tileset CRS */
    protected final MathTransform llToLocalProj;

    /** Stored imaging capability */
    protected final ImagingCapability imaging;

    /** Last painted tile level, used for interrogating proper level */
    protected int lastPaintedLevel;

    /** The ratio of target grid pixels / image pixel for each tile level */
    protected double[] pixelWidth;

    /**
     * Constructs a tile set renderable, creators needs to call
     * {@link #project(GeneralGridGeometry)} before the renderable can be used
     * 
     * @param resource
     * @param tileSetGeometry
     * @param tileCreator
     * @param tileLevels
     * @param tileSize
     */
    public TileSetRenderable(ImagingCapability imaging,
            GridGeometry2D tileSetGeometry, TileImageCreator tileCreator,
            int tileLevels, int tileSize) {
        this.tileSetGeometry = tileSetGeometry;
        this.tileCreator = tileCreator;
        this.tileLevels = tileLevels;
        this.tileSize = tileSize;
        this.pixelWidth = new double[tileLevels];
        this.imaging = imaging;

        try {
            // Set lat/lon math transforms for tile set
            llToLocalProj = CRSCache.getInstance().findMathTransform(
                    DefaultGeographicCRS.WGS84,
                    tileSetGeometry.getCoordinateReferenceSystem());
            localProjToLL = llToLocalProj.inverse();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Could not get tranform from tile crs to lat/lon", e);
        }
        HierarchicalPreferenceStore prefStore = Activator.getDefault()
                .getPreferenceStore();
        levelChangeThreshold = prefStore
                .getFloat(PreferenceConstants.P_PIXEL_DENSITY);
        levelChangeThresholdListener = new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                levelChangeThreshold = prefStore
                        .getFloat(PreferenceConstants.P_PIXEL_DENSITY);
            }
        };
        prefStore.addPropertyChangeListener(levelChangeThresholdListener);

    }

    /**
     * Projects the tile set for use with the target geometry
     * 
     * @param targetGeometry
     */
    public synchronized void project(GeneralGridGeometry targetGeometry) {
        // dispose the old TileSet
        if (tileSet != null) {
            tileSet.dispose();
        }
        // Create TileSet for new target geometry
        tileSet = TileSet.getTileSet(tileSetGeometry, targetGeometry,
                tileLevels, tileSize);

        // Clear out meshes and create new ones cloning old ones
        for (DrawableImage di : imageMap.values()) {
            if (di != null) {
                IMesh currentMesh = di.getCoverage().getMesh();
                if (currentMesh != null) {
                    try {
                        di.getCoverage()
                                .setMesh(currentMesh.clone(targetGeometry));
                    } catch (VizException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                e.getLocalizedMessage(), e);
                    }
                    currentMesh.dispose();
                }
            }
        }

        // Get the pixel densities for each tile level. This is approximately
        // how many target grid pixels per image pixel there are for the level
        for (int level = 0; level < tileLevels; ++level) {
            TileLevel tileLevel = tileSet.getTileLevel(level);
            pixelWidth[level] = tileLevel.getPixelDensity();
        }
    }

    /**
     * Returns the {@link GeneralGridGeometry} the {@link TileSet} is currently
     * projected for
     * 
     * @return
     */
    public GeneralGridGeometry getTargetGeometry() {
        return tileSet != null ? tileSet.getTargetGeometry() : null;
    }

    /**
     * Returns the {@link GridGeometry2D} of the {@link TileSet}
     * 
     * @return
     */
    public GridGeometry2D getTileSetGeometry() {
        return tileSetGeometry;
    }

    @Override
    public void paint(IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        Collection<DrawableImage> images = getImagesToRender(target,
                paintProps);
        target.drawRasters(paintProps,
                images.toArray(new DrawableImage[images.size()]));
    }

    /**
     * Disposes the tile set and any data associated with it
     */
    public synchronized void dispose() {
        // Make sure any lingering jobs are canceled and joined on
        for (Runnable job : jobMap.values()) {
            tileCreationPool.cancel(job);
        }
        tileCreationPool.join();

        // Dispose the tile set
        if (tileSet != null) {
            tileSet.dispose();
            tileSet = null;
        }

        // Dispose of all the images for the tile set
        for (DrawableImage image : imageMap.values()) {
            if (image != null) {
                image.dispose();
            }
        }
        imageMap.clear();

        if (levelChangeThresholdListener != null) {
            HierarchicalPreferenceStore prefStore = Activator.getDefault()
                    .getPreferenceStore();
            prefStore
                    .removePropertyChangeListener(levelChangeThresholdListener);
            levelChangeThresholdListener = null;
        }
    }

    /**
     * Get the {@link DrawableImage} list to display for the given target and
     * paint properties
     * 
     * @param target
     * @param paintProps
     * @return
     * @throws VizException
     */
    public synchronized Collection<DrawableImage> getImagesToRender(
            IGraphicsTarget target, PaintProperties paintProps)
            throws VizException {
        IExtent extent = paintProps.getView().getExtent();

        lastPaintedLevel = getTileLevel(extent, paintProps.getCanvasBounds());

        return getImagesWithinExtent(target, extent, lastPaintedLevel);
    }

    /**
     * Schedule the loading of data for all tiles that are needed to render
     * within the specified extent. The canvasBounds is used to determine the
     * appropriate tile level to display for the extent.
     * 
     * @param target
     *            The target to use when creating images.
     * @param extent
     *            the area over which to create tiles
     * @param canvasBounds
     *            the size of the area of the physical screen that is used to
     *            display the extent, used for determining the best tile level.
     * @return Returns true if all images are available and false if some images
     *         may still be loading in the background.
     */
    public synchronized boolean scheduleImagesWithinExtent(
            IGraphicsTarget target, IExtent extent, Rectangle canvasBounds) {
        getImagesWithinExtent(target, extent,
                getTileLevel(extent, canvasBounds));
        return jobMap.isEmpty();
    }

    protected int getTileLevel(IExtent extent, Rectangle canvasBounds) {

        double screenToWorldRatio = canvasBounds.width / extent.getWidth();

        int usedTileLevel = tileLevels - 1;

        /*
         * pixelRatios[usedTileLevel] * screenToWorldRatio gives us
         * canvasPixels/image pixel at the level, We should use the level if
         * there are less than LEVEL_CHANGE_THRESHOLD canvas pixels per image
         * pixel
         */
        while ((pixelWidth[usedTileLevel]
                * screenToWorldRatio > levelChangeThreshold)
                && usedTileLevel > 0) {
            usedTileLevel--;
        }
        return usedTileLevel;
    }

    /**
     * Gets the images to render within the extent in target grid space
     * 
     * @param target
     * @param extent
     * @param level
     * @return
     * @throws VizException
     */
    protected List<DrawableImage> getImagesWithinExtent(IGraphicsTarget target,
            IExtent extent, int level) {
        return getImagesWithinExtent(target, extent, level, 0);
    }

    /**
     * 
     * @param target
     * @param paintProps
     * @param level
     * @param depth
     * @return
     * @throws VizException
     */
    private List<DrawableImage> getImagesWithinExtent(IGraphicsTarget target,
            IExtent extent, int level, int depth) {
        if (tileSet == null) {
            // Early exit condition, disposed or haven't projected yet
            return Collections.emptyList();
        }

        // Flag to determine if we should draw lower tile levels. This will be
        // the case if we don't have all the images for all intersecting tiles
        // at this level
        boolean needDrawLower = false;
        // Get the intersecting tiles for the level
        Collection<Tile> intersecting = tileSet.getIntersectingTiles(level,
                extent);

        // These Tiles still need images created for them
        List<Tile> tilesNeedingImage = new ArrayList<>(intersecting.size());
        List<DrawableImage> drawableImages = new ArrayList<>(
                intersecting.size());

        for (Tile tile : intersecting) {
            // Flag to indicate if a tile needs an image created for it
            boolean needsImage = false;
            DrawableImage di = imageMap.get(tile);
            if (di != null) {
                IImage image = di.getImage();
                if (image.getStatus() == Status.FAILED
                        || image.getStatus() == Status.INVALID) {
                    // Image is invalid, re-request creation
                    needsImage = true;
                } else {
                    image.setBrightness(imaging.getBrightness());
                    image.setContrast(imaging.getContrast());
                    image.setInterpolated(imaging.isInterpolationState());

                    if (image.getStatus() != Status.LOADED) {
                        needDrawLower = true;
                    }
                    drawableImages.add(di);
                }
            } else {
                needsImage = true;
            }

            if (needsImage) {
                tilesNeedingImage.add(tile);
                needDrawLower = true;
            }
        }

        if (depth == 0) {
            // Only request images to be created if we are at the desired level
            // i.e. the recursion depth is 0
            if (tilesNeedingImage.isEmpty()) {
                // All intersecting tiles are loaded for this level, cancel any
                // jobs running for tiles we don't need anymore (may be case if
                // zooming or panning)
                Iterator<Runnable> iterator = jobMap.values().iterator();
                while (iterator.hasNext()) {
                    Runnable job = iterator.next();
                    if (tileCreationPool.cancel(job)) {
                        iterator.remove();
                    }
                }
            } else {
                target.setNeedsRefresh(true);
                // Create tiles needing images
                createTileImages(target, tilesNeedingImage);
            }
        }

        // Draw lower resolution data first
        if (needDrawLower && (level + 1) < tileLevels) {
            // put lower levels first in the list so they are drawn first.
            List<DrawableImage> lowerImages = getImagesWithinExtent(target,
                    extent, level + 1, depth + 1);
            lowerImages.addAll(drawableImages);
            drawableImages = lowerImages;
        }

        return drawableImages;
    }

    /**
     * Create tile images for the specified tiles
     * 
     * @param target
     * @param tilesToCreate
     */
    protected void createTileImages(IGraphicsTarget target,
            Collection<Tile> tilesToCreate) {
        for (Tile tile : tilesToCreate) {
            if (jobMap.get(tile) == null) {
                // No job already running for tile, create and schedule one
                TileImageCreatorTask job = new TileImageCreatorTask(target,
                        tile);
                jobMap.put(tile, job);
                tileCreationPool.schedule(job);
            }
        }
    }

    /**
     * Adds a DrawableImage for the specified Tile. Disposes of any old image
     * 
     * @param tile
     * @param image
     */
    public void addTileImage(Tile tile, DrawableImage image) {
        DrawableImage oldImage = imageMap.put(tile, image);
        if (oldImage != null) {
            oldImage.dispose();
        }
        Runnable task = jobMap.remove(tile);
        if (task != null) {
            tileCreationPool.cancel(task);
        }
    }

    /**
     * Returns the raw image value from tile image that contains the lat/lon
     * coordinate
     * 
     * @param coordinate
     *            in lat/lon space
     * @return
     * @throws VizException
     */
    public double interrogate(Coordinate coordinate) throws VizException {
        return interrogate(coordinate, null);
    }

    /**
     * Returns the raw image value from tile image that contains the lat/lon
     * coordinate
     * 
     * @param coordinate
     *            in lat/lon space
     * @param resultUnit
     *            unit result from interrogate will be returned is. If unit is
     *            not compatible with data unit, {@link Double#NaN} will be
     *            returned. Null indicates data will not be converted
     * @param nanValue
     *            if interrogated value is equal to nanValue, {@link Double#NaN}
     *            will be returned
     * @return
     * @throws VizException
     */
    public double interrogate(Coordinate coordinate, Unit<?> resultUnit)
            throws VizException {
        return interrogate(coordinate, resultUnit, Double.NaN);
    }

    /**
     * Returns the raw image value from tile image that contains the lat/lon
     * coordinate
     * 
     * @param coordinate
     *            in lat/lon space
     * @param nanValue
     *            if interrogated value is equal to nanValue, {@link Double#NaN}
     *            will be returned
     * @return
     * @throws VizException
     */
    public double interrogate(Coordinate coordinate, double nanValue)
            throws VizException {
        return interrogate(coordinate, null, nanValue);
    }

    /**
     * Returns the raw image value from tile image that contains the lat/lon
     * coordinate. Any values matching nanValue will return {@link Double#NaN}
     * 
     * @param coordinate
     *            in lat/lon space
     * @param resultUnit
     *            unit result from interrogate will be returned is. If unit is
     *            not compatible with data unit, {@link Double#NaN} will be
     *            returned. Null indicates data will not be converted
     * @param nanValue
     *            if interrogated value is equal to nanValue, {@link Double#NaN}
     *            will be returned
     * @return
     * @throws VizException
     */
    public double interrogate(Coordinate coordinate, Unit<?> resultUnit,
            double nanValue) throws VizException {
        double dataValue = Double.NaN;
        TileLevel level = tileSet.getTileLevel(lastPaintedLevel);

        double[] grid = null;
        try {
            double[] local = new double[2];
            llToLocalProj.transform(new double[] { coordinate.x, coordinate.y },
                    0, local, 0, 1);
            grid = level.crsToGrid(local[0], local[1]);
        } catch (TransformException e) {
            throw new VizException("Error interrogating ", e);
        }

        IColormappedImage cmapImage = null;

        Tile tile = level.getTile(grid[0], grid[1]);
        if (tile != null) {
            DrawableImage di = imageMap.get(tile);
            if (di != null) {
                IImage image = di.getImage();
                if (image instanceof IColormappedImage) {
                    cmapImage = (IColormappedImage) image;
                }
            }
        }

        if (cmapImage != null) {
            dataValue = cmapImage.getValue((int) grid[0] % tileSize,
                    (int) grid[1] % tileSize);
            if (dataValue == nanValue) {
                dataValue = Double.NaN;
            } else {
                ColorMapParameters parameters = cmapImage
                        .getColorMapParameters();
                Unit<?> dataUnit = cmapImage.getDataUnit();
                if (parameters.getDataMapping() != null) {
                    /*
                     * Ignore dataUnit, use colorMapUnit which is derived from
                     * the data mapping
                     */
                    dataUnit = parameters.getColorMapUnit();
                }
                if (resultUnit != null && dataUnit != null
                        && !dataUnit.equals(resultUnit)) {
                    if (resultUnit.isCompatible(dataUnit)) {
                        dataValue = UnitConv
                                .getConverterToUnchecked(dataUnit, resultUnit)
                                    .convert(dataValue);
                    } else {
                        UnitFormat uf = SimpleUnitFormat
                                .getInstance(SimpleUnitFormat.Flavor.ASCII);
                        String message = String.format(
                                "Unable to interrogate tile set.  "
                                        + "Desired unit (%s) is not compatible "
                                        + "with data unit (%s).",
                                uf.format(resultUnit), uf.format(dataUnit));
                        throw new IllegalArgumentException(message);
                    }
                }
            }
        }

        return dataValue;
    }

    public void setLevelChangeThreshold(double levelChangeThreshold) {
        if (levelChangeThresholdListener != null) {
            HierarchicalPreferenceStore prefStore = Activator.getDefault()
                    .getPreferenceStore();
            prefStore
                    .removePropertyChangeListener(levelChangeThresholdListener);
            levelChangeThresholdListener = null;
        }
        this.levelChangeThreshold = levelChangeThreshold;
    }

}
