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
package com.raytheon.uf.viz.core.grid.rsc.data;

import java.nio.FloatBuffer;

import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
import com.raytheon.uf.common.geospatial.data.UnitConvertingDataFilter;
import com.raytheon.uf.common.geospatial.interpolation.GridReprojection;
import com.raytheon.uf.common.geospatial.interpolation.GridReprojectionDataSource;
import com.raytheon.uf.common.geospatial.interpolation.GridSampler;
import com.raytheon.uf.common.geospatial.interpolation.Interpolation;
import com.raytheon.uf.common.geospatial.interpolation.PrecomputedGridReprojection;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.units.UnitConv;

/**
 *
 * A class which hold scalar data for a grid.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 16, 2019 67949      tjensen     Initial creation
 *
 * </pre>
 *
 * @author tjensen
 */
public class ScalarGridData extends GeneralGridData {

    private GeographicDataSource scalarData;

    /**
     * Create a scalar grid data object from float data.
     *
     * @param scalarData
     * @param dataUnit
     * @return
     */
    public static ScalarGridData createScalarData(
            GeneralGridGeometry gridGeometry, FloatBuffer scalarData,
            Unit<?> dataUnit) {
        DataSource scalarSource = new GeographicDataSource(scalarData,
                gridGeometry);
        return createScalarData(gridGeometry, scalarSource, dataUnit);
    }

    /**
     * Create a scalar grid data object from any data source
     *
     * @param scalarData
     * @param dataUnit
     * @return
     */
    public static ScalarGridData createScalarData(
            GeneralGridGeometry gridGeometry, DataSource scalarData,
            Unit<?> dataUnit) {
        return new ScalarGridData(gridGeometry, scalarData, dataUnit);
    }

    private ScalarGridData(GeneralGridGeometry gridGeometry,
            DataSource scalarData, Unit<?> dataUnit) {
        super(gridGeometry, dataUnit);
        this.scalarData = GeographicDataSource.wrap(scalarData,
                this.gridGeometry);
    }

    /**
     * Attempt to convert this data to the new unit. If this is successful then
     * the dataUnit and data will be changed.
     *
     * @param unit
     * @return true if units are compatible, false if data is unchanged.
     */
    @Override
    public boolean convert(Unit<?> unit) {
        if (dataUnit == null && unit == null) {
            return true;
        } else if (dataUnit == null || unit == null) {
            return false;
        }
        if (!dataUnit.isCompatible(unit)) {
            return false;
        }
        UnitConverter converter = UnitConv.getConverterToUnchecked(dataUnit,
                unit);
        if (converter.isIdentity()) {
            // no need to actually convert if they are the same.
            return true;
        }
        UnitConvertingDataFilter filter = new UnitConvertingDataFilter(
                converter);
        if (scalarData != null) {
            scalarData = scalarData.applyFilters(filter);
        }
        dataUnit = unit;
        return true;
    }

    /**
     * Create a new GeneralGridData that is a reprojected version of this data.
     *
     * @param newGridGeometry
     * @param interpolation
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    @Override
    public ScalarGridData reproject(GeneralGridGeometry newGridGeometry,
            Interpolation interpolation)
            throws FactoryException, TransformException {
        GridGeometry2D newGeom = GridGeometry2D.wrap(newGridGeometry);
        GridReprojection reproj = PrecomputedGridReprojection
                .getReprojection(gridGeometry, newGeom);
        GridSampler sampler = new GridSampler(interpolation);

        sampler.setSource(getScalarData());
        return createScalarData(newGridGeometry,
                new GridReprojectionDataSource(reproj, sampler), dataUnit);

    }

    public GeographicDataSource getScalarData() {
        return scalarData;
    }

    public void setScalarData(GeographicDataSource scalarData) {
        this.scalarData = scalarData;
    }

    @Override
    protected GeneralGridData mergeData(GeneralGridData other,
            GridEnvelope2D range1, GridEnvelope2D range2,
            GridGeometry2D geometry) {
        ScalarGridData sData2 = (ScalarGridData) other;
        DataSource newData = mergeData(getScalarData(), range1,
                sData2.getScalarData(), range2);
        return createScalarData(geometry, newData, getDataUnit());
    }

    @Override
    public GeographicDataSource getData() {
        return getScalarData();
    }

}
