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
package com.raytheon.uf.common.dataaccess.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.EnvelopeProjectionException;
import com.raytheon.uf.common.dataaccess.exception.ResponseTooLargeException;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataaccess.response.GridResponseData;
import com.raytheon.uf.common.dataaccess.util.DataWrapperUtil;
import com.raytheon.uf.common.dataaccess.util.PDOUtil;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.util.SubGridGeometryCalculator;
import com.raytheon.uf.common.numeric.source.DataSource;
import org.locationtech.jts.geom.Envelope;

/**
 * An abstract factory for getting grid data from plugins that use
 * PluginDataObject.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Jan 17, 2013           bsteffen    Initial creation
 * Feb 14, 2013  1614     bsteffen    Refactor data access framework to use
 *                                    single request.
 * Jan 14, 2014  2667     mnash       Remove getGeometryData methods
 * Feb 04, 2014  2672     bsteffen    Enable subgridding when envelopes are
 *                                    requested
 * Oct 29, 2014  3755     nabowle     Ignore results that do not have a grid
 *                                    geometry.
 * Jan 28, 2015  2866     nabowle     Estimate response sizes and throw an
 *                                    exception if too large.
 * Feb 23, 2015  2866     nabowle     Add response sizes to exception.
 * Jul 31, 2018  6389     mapeters    Extracted getDataRecord() from
 *                                    getDataSource()
 *
 * </pre>
 *
 * @author bsteffen
 */

public abstract class AbstractGridDataPluginFactory
        extends AbstractDataPluginFactory {
    /** Number of bytes. Based on {@link GridResponseData} using floats. */
    public static final int SIZE_OF_POINT = 4;

    /**
     * Executes the provided DbQueryRequest and returns an array of IGridData
     *
     * @param request
     *            the original grid request
     * @param dbQueryRequest
     *            the db query request to execute
     * @return an array of IGridData
     */
    @Override
    protected IGridData[] getGridData(IDataRequest request,
            DbQueryResponse dbQueryResponse) {
        Envelope envelope = request.getEnvelope();
        List<CollectedGridGeometry> collectedGrids = collectGridGeometries(
                dbQueryResponse.getResults(), envelope);

        checkResponseSize(collectedGrids);

        List<IGridData> gridData = new ArrayList<>();
        GridGeometry2D gridGeometry;
        DataSource dataSource;
        for (CollectedGridGeometry grid : collectedGrids) {
            dataSource = null;
            gridGeometry = grid.getGridGeometry();

            if (envelope != null) {
                if (grid.getSubgrid() == null || !grid.getSubgrid().isEmpty()) {
                    dataSource = getDataSource(grid.getPdo(),
                            grid.getSubgrid());
                    if (grid.getSubgrid() != null) {
                        gridGeometry = grid.getSubgrid()
                                .getZeroedSubGridGeometry();
                    }
                }
            } else {
                dataSource = getDataSource(grid.getPdo(), null);
            }

            if (dataSource != null) {
                gridData.add(this.constructGridDataResponse(request,
                        grid.getPdo(), gridGeometry, dataSource));
            }
        }

        return gridData.toArray(new IGridData[gridData.size()]);
    }

    /**
     * Collect the pdo, grid, and subgrid if needed for each result, ignoring
     * results that do not have grid data, in order to avoid having to calculate
     * grids/subgrids multiple times.
     *
     * @param results
     *            The request results.
     * @param envelope
     *            collectGridGeometriesenvelope.
     * @return The pdo, grid geometry, and subgrid geometry for a result.
     */
    private List<CollectedGridGeometry> collectGridGeometries(
            List<Map<String, Object>> results, Envelope envelope) {
        ReferencedEnvelope requestEnv = envelope == null ? null
                : new ReferencedEnvelope(envelope, DefaultGeographicCRS.WGS84);
        List<CollectedGridGeometry> grids = new ArrayList<>();
        PluginDataObject pdo;
        GridGeometry2D gridGeometry;
        for (Map<String, Object> resultMap : results) {

            if (!resultMap.containsKey(null)) {
                throw new DataRetrievalException(
                        "The results of the DbQueryRequest do not consist of PluginDataObject objects as expected.");
            }
            if (!((resultMap.get(null) instanceof PluginDataObject))) {
                throw new DataRetrievalException(
                        "The objects returned by the DbQueryRequest are not of type PluginDataObject as expected.");
            }

            pdo = (PluginDataObject) resultMap.get(null);

            gridGeometry = getGridGeometry(pdo);
            if (gridGeometry == null) {
                continue;
            }

            if (requestEnv != null) {
                grids.add(new CollectedGridGeometry(pdo, gridGeometry,
                        calculateSubGrid(requestEnv, gridGeometry)));
            } else {
                grids.add(new CollectedGridGeometry(pdo, gridGeometry, null));
            }
        }

        return grids;
    }

    /**
     * Estimates the memory size of the response, and throws a
     * {@link ResponseTooLargeException} if the response would be too large.
     *
     * @param grids
     */
    protected void checkResponseSize(List<CollectedGridGeometry> grids) {
        long estimatedSize = 0;
        GridGeometry2D gridGeom;
        SubGridGeometryCalculator subGrid;
        for (CollectedGridGeometry grid : grids) {
            gridGeom = grid.getGridGeometry();
            subGrid = grid.getSubgrid();

            if (subGrid == null) {
                estimatedSize += gridGeom.getGridRange().getSpan(0)
                        * gridGeom.getGridRange().getSpan(1);
            } else if (!subGrid.isEmpty()) {
                estimatedSize += estimateSubgridSize(gridGeom, subGrid);
            }
        }

        estimatedSize *= SIZE_OF_POINT;

        // TODO: This estimation does not include extra bytes that are included
        // at serialization. It's possible the response may fail if a
        // LimitingOutputStream is used.
        if (estimatedSize > MAX_RESPONSE_SIZE) {
            throw new ResponseTooLargeException(estimatedSize,
                    MAX_RESPONSE_SIZE);
        }
    }

    /**
     * Estimate the subgrid memory size.
     *
     * @param gridGeom
     * @param subGrid
     * @return
     */
    protected long estimateSubgridSize(GridGeometry2D gridGeom,
            SubGridGeometryCalculator subGrid) {
        long size = subGrid.getSubGridGeometry().getGridRange().getSpan(0)
                * subGrid.getSubGridGeometry().getGridRange().getSpan(1);
        return size;
    }

    /**
     * Generate a SubGridGeometryCalculator appropriate for determining what
     * area of data to request for this dataType. A return type of null can be
     * used to indicate the entire gridGeometry should be used.
     *
     * @param envelope
     *            The requested envelope in WGS84
     * @param gridGeometry
     *            The gridGeometry.
     * @return a SubGridGeometryCalculator.
     * @throws EnvelopeProjectionException
     */
    protected SubGridGeometryCalculator calculateSubGrid(
            ReferencedEnvelope envelope, GridGeometry2D gridGeometry)
            throws EnvelopeProjectionException {
        try {
            return new SubGridGeometryCalculator(envelope, gridGeometry);
        } catch (TransformException e) {
            throw new EnvelopeProjectionException(
                    "Error determining subgrid from envelope: " + envelope, e);
        }
    }

    /**
     * Request the raw data for a pdo.
     *
     * @param pdo
     *            the pdo with metadata popualted
     * @param subGrid
     *            object describing area requested.
     * @return a DataSource holding the raw data.
     */
    protected DataSource getDataSource(PluginDataObject pdo,
            SubGridGeometryCalculator subGrid) {
        IDataRecord dataRecord = getDataRecord(pdo, subGrid);
        if (dataRecord == null) {
            return null;
        }
        return DataWrapperUtil.constructArrayWrapper(dataRecord, false);

    }

    protected IDataRecord getDataRecord(PluginDataObject pdo,
            SubGridGeometryCalculator subGrid) {
        try {
            IDataRecord dataRecord = null;
            if (subGrid == null || subGrid.isFull()) {
                dataRecord = PDOUtil.getDataRecord(pdo, "Data", Request.ALL);
            } else if (!subGrid.isEmpty()) {
                Request dataStoreReq = Request.buildSlab(
                        subGrid.getGridRangeLow(true),
                        subGrid.getGridRangeHigh(false));
                dataRecord = PDOUtil.getDataRecord(pdo, "Data", dataStoreReq);
            }
            return dataRecord;
        } catch (Exception e) {
            throw new DataRetrievalException(
                    "Failed to retrieve the IDataRecord for PluginDataObject: "
                            + pdo.toString(),
                    e);
        }
    }

    protected GridGeometry2D getGridGeometry(PluginDataObject pdo) {
        return PDOUtil.retrieveGeometry(pdo);
    }

    /**
     * Builds an IGridData with the information that is supplied
     *
     * @param request
     *            the original grid request
     * @param pdo
     *            a record that was retrieved from the database
     * @param gridGeometry
     *            the geometry extracted from the pdo
     * @param dataRecord
     *            the raw data
     * @return the IGridData that was constructed
     */
    protected abstract IGridData constructGridDataResponse(IDataRequest request,
            PluginDataObject pdo, GridGeometry2D gridGeometry,
            DataSource dataSource);

}
