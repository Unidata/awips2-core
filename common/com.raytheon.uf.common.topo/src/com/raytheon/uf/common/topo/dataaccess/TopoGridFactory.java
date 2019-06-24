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
package com.raytheon.uf.common.topo.dataaccess;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.INotificationFilter;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.exception.InvalidIdentifiersException;
import com.raytheon.uf.common.dataaccess.exception.ResponseTooLargeException;
import com.raytheon.uf.common.dataaccess.exception.TimeAgnosticDataException;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataaccess.impl.AbstractDataFactory;
import com.raytheon.uf.common.dataaccess.impl.AbstractGridDataPluginFactory;
import com.raytheon.uf.common.dataaccess.impl.DefaultGridData;
import com.raytheon.uf.common.dataaccess.util.DataWrapperUtil;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.CRSCache;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.topo.TopoUtils;
import org.locationtech.jts.geom.Envelope;

/**
 * Grid data access factory for Topographic data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 14, 2015 4608       nabowle     Initial creation
 * May 26, 2016 5587       tgurney     Support getIdentifierValues()
 * Jun 07, 2016 5587       tgurney     Change get*Identifiers() to take
 *                                     IDataRequest
 * Jul 05, 2016 5728       mapeters    Improved handling of invalid identifier
 *                                     values
 * Jul 27, 2016 2416       tgurney     Stub impl of getNotificationFilter()
 * Feb 19, 2018 7220       mapeters    Improve filtering of available identifier values
 *
 * </pre>
 *
 * @author nabowle
 */

public class TopoGridFactory extends AbstractDataFactory {

    private static final String TOPO_FILE = "topoFile";

    private static final String DATASET = "dataset";

    private static final String GROUP = "group";

    /**
     * All available topo hdf5 files. These are hardcoded because it is not
     * possible to dynamically list all available topo hdf5 files; they may
     * exist on a different system and no API is provided to list them
     */
    private static final String[] TOPO_FILENAMES = { "defaultTopo", "gmted2010",
            "gtopo30", "modelStaticTopo", "srtm30", "srtm30_plus",
            "staticTopo" };

    /*
     * The below groups and datasets could be generated dynamically, but since
     * all topo files have the same groups and datasets it is not strictly
     * necessary
     */
    private static final Map<String, String[]> groupsToDatasets;
    static {
        groupsToDatasets = new HashMap<>();
        groupsToDatasets.put("/", new String[] { "full" });
        groupsToDatasets.put("/interpolated",
                new String[] { "1", "2", "3", "4", "5" });
    }

    /**
     * Constructor.
     */
    public TopoGridFactory() {
        super();
    }

    @Override
    public IGridData[] getGridData(IDataRequest request, DataTime... times) {
        validateRequest(request);
        return getGridData(request);
    }

    @Override
    public IGridData[] getGridData(IDataRequest request, TimeRange timeRange) {
        validateRequest(request);
        return getGridData(request);
    }

    @Override
    public void validateRequest(IDataRequest request) {
        validateRequest(request, false);

        if (request.getEnvelope() == null) {
            throw new IncompatibleRequestException(
                    "Topo data requests must specify an envelope.");
        }
    }

    @Override
    public String[] getRequiredIdentifiers(IDataRequest request) {
        return new String[] { GROUP, DATASET };
    }

    @Override
    public String[] getOptionalIdentifiers(IDataRequest request) {
        return new String[] { TOPO_FILE };
    }

    /**
     * Executes the provided DbQueryRequest and returns an array of IGridData
     *
     * @param request
     *            the original grid request
     * @return an array of IGridData
     */
    protected IGridData[] getGridData(IDataRequest request) {
        Map<String, Object> identifiers = request.getIdentifiers();

        String topofile = getTopoFile(identifiers);
        String group = getStringIdentifierValue(identifiers, GROUP);
        String dataset = getStringIdentifierValue(identifiers, DATASET);
        Envelope requestEnv = request.getEnvelope();

        File hdf5File = new File(TopoUtils.getDefaultTopoFile().getParent(),
                topofile);
        IDataStore ds = DataStoreFactory.getDataStore(hdf5File);

        IDataRecord rec;
        int width;
        int height;
        GridGeometry2D recGeom;
        Map<String, Object> attributes;
        try {
            GridGeometry2D gridGeom = TopoUtils.getTopoGeometry(ds, "full");

            MathTransform llToTopoCRS = CRSCache.getInstance()
                    .findMathTransform(DefaultGeographicCRS.WGS84,
                            gridGeom.getCoordinateReferenceSystem());
            MathTransform topoCRSToGrid = gridGeom
                    .getGridToCRS(PixelInCell.CELL_CORNER).inverse();

            double[] latLon = new double[] { requestEnv.getMinX(),
                    requestEnv.getMinY(), requestEnv.getMaxX(),
                    requestEnv.getMaxY() };
            double[] topoCrs = new double[latLon.length];
            double[] topoGrid = new double[latLon.length];

            // transform user envelope to the topo's CRS
            llToTopoCRS.transform(latLon, 0, topoCrs, 0, 2);
            // transform envelope in topo CRS to grid coordinates
            topoCRSToGrid.transform(topoCrs, 0, topoGrid, 0, 2);

            int minX = (int) Math.min(topoGrid[0], topoGrid[2]);
            int maxX = (int) Math.max(topoGrid[0], topoGrid[2]);
            int minY = (int) Math.min(topoGrid[1], topoGrid[3]);
            int maxY = (int) Math.max(topoGrid[1], topoGrid[3]);
            width = maxX - minX;
            height = maxY - minY;

            checkResponseSize(width, height);

            Request req = Request.buildSlab(new int[] { minX, minY },
                    new int[] { maxX, maxY });

            rec = ds.retrieve(group, dataset, req);

            attributes = createRecordAttributes(width, height, minX, minY);
            recGeom = createRecordGeometry(gridGeom, topoCrs, minX, minY, width,
                    height);

        } catch (FileNotFoundException | StorageException | TransformException
                | FactoryException e) {
            throw new DataRetrievalException(
                    "Could not retrieve the requested topo data for " + topofile
                            + ". ",
                    e);
        }

        DataSource dataSource = DataWrapperUtil.constructArrayWrapper(rec,
                true);

        DefaultGridData retData = new DefaultGridData(dataSource, recGeom);
        retData.setAttributes(attributes);
        return new IGridData[] { retData };
    }

    /**
     * Checks the response size. If the estimated response size is too large, a
     * {@link ResponseTooLargeException} is thrown. If it's not too large,
     * nothing happens.
     *
     * @param width
     *            The width of the grid envelope requested.
     * @param height
     *            The height of the grid envelope requested.
     */
    private void checkResponseSize(long width, long height) {
        long estimatedSize = width * height
                * AbstractGridDataPluginFactory.SIZE_OF_POINT;
        if (estimatedSize > MAX_RESPONSE_SIZE) {
            throw new ResponseTooLargeException(estimatedSize,
                    MAX_RESPONSE_SIZE,
                    "Please specify a smaller request envelope.");
        }
    }

    private Map<String, Object> createRecordAttributes(int width, int height,
            int minX, int minY) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("width", width);
        attributes.put("height", height);
        attributes.put("minX", minX);
        attributes.put("minY", minY);
        return attributes;
    }

    /**
     * Creates the GridGeometry2D for the returned record.
     *
     * @param gridGeom
     *            The grid geometry from the req
     * @param topoCrs
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     * @return
     */
    private GridGeometry2D createRecordGeometry(GridGeometry2D gridGeom,
            double[] topoCrs, int minX, int minY, int width, int height) {
        GridEnvelope2D gridEnvelope = new GridEnvelope2D(minX, minY, width,
                height);
        GridGeometry2D recGeom = new GridGeometry2D(gridEnvelope,
                gridGeom.getGridToCRS(PixelInCell.CELL_CORNER),
                gridGeom.getCoordinateReferenceSystem());
        return recGeom;
    }

    /**
     * Get the topo file. If no topo file is specified, use defaultTopo.h5
     *
     * @param identifiers
     *            The request identifiers.
     * @return The topo file to use.
     */
    private String getTopoFile(Map<String, Object> identifiers) {
        String topofile = getStringIdentifierValue(identifiers, TOPO_FILE);
        if (topofile == null || topofile.isEmpty()) {
            topofile = TopoUtils.DEFAULT_TOPO_FILE;
        } else if (!topofile.endsWith(".h5")) {
            topofile = topofile + ".h5";
        }
        return topofile;
    }

    /**
     * Get the value of an identifier that must be provided as a String (or may
     * not be provided at all)
     *
     * @param identifiers
     * @param key
     * @return the trimmed string identifier value
     */
    private String getStringIdentifierValue(Map<String, Object> identifiers,
            String key) {
        Object value = identifiers.get(key);
        if (value == null) {
            return null;
        } else if (value instanceof String) {
            return ((String) value).trim();
        } else {
            throw new IncompatibleRequestException(
                    "Only string identifier values are valid for '" + key
                            + "'");
        }
    }

    @Override
    public String[] getIdentifierValues(IDataRequest request,
            String identifierKey) {
        String[] idValuesResult = null;
        if (identifierKey.equals(TOPO_FILE)) {
            idValuesResult = TOPO_FILENAMES;
        } else if (identifierKey.equals(GROUP)) {
            String dataset = getStringIdentifierValue(request.getIdentifiers(),
                    DATASET);
            if (dataset != null) {
                // List groups that contain the specified dataset
                idValuesResult = groupsToDatasets.entrySet().stream().filter(
                        e -> Arrays.asList(e.getValue()).contains(dataset))
                        .map(e -> e.getKey()).toArray(String[]::new);
            } else {
                idValuesResult = groupsToDatasets.keySet()
                        .toArray(new String[0]);
            }
        } else if (identifierKey.equals(DATASET)) {
            String group = getStringIdentifierValue(request.getIdentifiers(),
                    GROUP);
            if (group != null) {
                String[] datasets = groupsToDatasets.get(group);
                idValuesResult = datasets != null ? datasets : new String[0];
            } else {
                // Merge datasets for all groups
                idValuesResult = groupsToDatasets.values().stream()
                        .flatMap(Arrays::stream).distinct()
                        .toArray(String[]::new);
            }
        } else {
            throw new InvalidIdentifiersException(request.getDatatype(), null,
                    Arrays.asList(identifierKey));
        }
        Arrays.sort(idValuesResult);
        return idValuesResult;
    }

    // Unsupported methods.

    @Override
    public DataTime[] getAvailableTimes(IDataRequest request,
            boolean refTimeOnly) throws TimeAgnosticDataException {
        throw new TimeAgnosticDataException(request.getDatatype()
                + " data requests do not support getting available times.");
    }

    @Override
    public DataTime[] getAvailableTimes(IDataRequest request,
            BinOffset binOffset) throws TimeAgnosticDataException {
        throw new TimeAgnosticDataException(request.getDatatype()
                + " data requests do not support getting available times.");
    }

    @Override
    public String[] getAvailableLocationNames(IDataRequest request) {
        throw new IncompatibleRequestException(request.getDatatype()
                + " data requests do not support getting available location names.");
    }

    @Override
    public INotificationFilter getNotificationFilter(IDataRequest request) {
        throw new IncompatibleRequestException(
                "Cannot listen for updates to topography data");
    }

}
