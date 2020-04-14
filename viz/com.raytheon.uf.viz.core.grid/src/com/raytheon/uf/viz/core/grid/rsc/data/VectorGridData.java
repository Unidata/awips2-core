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
import org.geotools.geometry.DirectPosition2D;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
import com.raytheon.uf.common.geospatial.data.UnitConvertingDataFilter;
import com.raytheon.uf.common.geospatial.interpolation.GridReprojection;
import com.raytheon.uf.common.geospatial.interpolation.GridSampler;
import com.raytheon.uf.common.geospatial.interpolation.Interpolation;
import com.raytheon.uf.common.geospatial.interpolation.PrecomputedGridReprojection;
import com.raytheon.uf.common.numeric.buffer.FloatBufferWrapper;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.units.UnitConv;

/**
 *
 * A class which hold vector data for a grid.
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
public class VectorGridData extends GeneralGridData {

    private GeographicDataSource uComponent = null;

    private GeographicDataSource vComponent = null;

    /**
     * Create gridData for a vector by providing the magnitude and direction of
     * the vector as floats.
     *
     * @param magnitude
     * @param direction
     * @param dataUnit
     * @return
     */
    public static VectorGridData createVectorData(
            GeneralGridGeometry gridGeometry, FloatBuffer magnitude,
            FloatBuffer direction, Unit<?> dataUnit) {
        magnitude.rewind();
        direction.rewind();
        FloatBuffer vComponent = FloatBuffer.allocate(magnitude.capacity());
        FloatBuffer uComponent = FloatBuffer.allocate(magnitude.capacity());
        while (magnitude.hasRemaining()) {
            /*
             * add 180 degrees because meteorology uses direction from for
             * vectors
             */
            double angle = Math.toRadians(direction.get() + 180f);
            double mag = magnitude.get();
            vComponent.put((float) (Math.cos(angle) * mag));
            uComponent.put((float) (Math.sin(angle) * mag));
        }
        return createVectorDataUV(gridGeometry, uComponent, vComponent,
                dataUnit);
    }

    /**
     * Create gridData for a vector by providing the u and v components of the
     * vector as floats.
     *
     * @param uComponent
     * @param vComponent
     * @param dataUnit
     * @return
     */
    public static VectorGridData createVectorDataUV(
            GeneralGridGeometry gridGeometry, FloatBuffer uComponent,
            FloatBuffer vComponent, Unit<?> dataUnit) {
        DataSource uSource = new GeographicDataSource(uComponent, gridGeometry);
        DataSource vSource = new GeographicDataSource(vComponent, gridGeometry);
        return createVectorDataUV(gridGeometry, uSource, vSource, dataUnit);
    }

    /**
     * Create gridData for a vector by providing the u and v components of the
     * vector as any data source.
     *
     * @param uComponent
     * @param vComponent
     * @param dataUnit
     * @return
     */
    public static VectorGridData createVectorDataUV(
            GeneralGridGeometry gridGeometry, DataSource uComponent,
            DataSource vComponent, Unit<?> dataUnit) {
        return new VectorGridData(gridGeometry, uComponent, vComponent,
                dataUnit);
    }

    private VectorGridData(GeneralGridGeometry gridGeometry,
            DataSource uComponent, DataSource vComponent, Unit<?> dataUnit) {
        super(gridGeometry, dataUnit);
        this.uComponent = GeographicDataSource.wrap(uComponent,
                this.gridGeometry);
        this.vComponent = GeographicDataSource.wrap(vComponent,
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

        if (uComponent != null) {
            uComponent = uComponent.applyFilters(filter);

        }
        if (vComponent != null) {
            vComponent = vComponent.applyFilters(filter);
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
    public VectorGridData reproject(GeneralGridGeometry newGridGeometry,
            Interpolation interpolation)
            throws FactoryException, TransformException {
        GridGeometry2D newGeom = GridGeometry2D.wrap(newGridGeometry);
        GridReprojection reproj = PrecomputedGridReprojection
                .getReprojection(gridGeometry, newGeom);
        GridSampler sampler = new GridSampler(interpolation);
        sampler.setSource(getUComponent());
        float[] udata = reproj
                .reprojectedGrid(sampler,
                        new FloatBufferWrapper(newGeom.getGridRange2D()))
                .getArray();
        sampler.setSource(getVComponent());
        float[] vdata = reproj
                .reprojectedGrid(sampler,
                        new FloatBufferWrapper(newGeom.getGridRange2D()))
                .getArray();
        // When reprojecting it is necessary to recalculate the
        // direction of vectors based off the change in the "up"
        // direction
        GridEnvelope2D targetRange = newGeom.getGridRange2D();

        MathTransform grid2crs = newGeom.getGridToCRS();
        MathTransform crs2ll = MapUtil
                .getTransformToLatLon(newGeom.getCoordinateReferenceSystem());

        for (int i = 0; i < targetRange.width; i++) {
            for (int j = 0; j < targetRange.height; j++) {
                int index = i + j * targetRange.width;
                if (udata[index] > -9999) {
                    DirectPosition2D dp = new DirectPosition2D(i, j);
                    grid2crs.transform(dp, dp);
                    crs2ll.transform(dp, dp);
                    Coordinate ll = new Coordinate(dp.x, dp.y);
                    double rot = MapUtil.rotation(ll, newGeom);
                    double rot2 = MapUtil.rotation(ll, gridGeometry);
                    double cos = Math.cos(Math.toRadians(rot - rot2));
                    double sin = Math.sin(Math.toRadians(rot - rot2));
                    double u = udata[index];
                    double v = vdata[index];
                    udata[index] = (float) (cos * u - sin * v);
                    vdata[index] = (float) (sin * u + cos * v);
                }
            }
        }
        return createVectorDataUV(newGridGeometry, FloatBuffer.wrap(udata),
                FloatBuffer.wrap(vdata), dataUnit);

    }

    public GeographicDataSource getMagnitude() {
        DataSource rawSource = new MagnitudeDataSource(uComponent, vComponent);
        return new GeographicDataSource(rawSource, this.gridGeometry);
    }

    /**
     * @return the direction from which the vector originates. This is commonly
     *         used in meteorology, espesially for winds. For example if a
     *         meteorologist says "The wind direction is North" it means the
     *         wind is coming from the north and moving to the south.
     * @see #getDirectionTo()
     */
    public GeographicDataSource getDirectionFrom() {
        DataSource rawSource = new DirectionFromDataSource(uComponent,
                vComponent);
        return new GeographicDataSource(rawSource, this.gridGeometry);
    }

    /**
     * @return the direction a vector is going towards. This is the common
     *         mathematical deffinition of a vector.
     * @see #getDirectionFrom()
     */
    public GeographicDataSource getDirectionTo() {
        DataSource rawSource = new DirectionToDataSource(uComponent,
                vComponent);
        return new GeographicDataSource(rawSource, this.gridGeometry);
    }

    public GeographicDataSource getUComponent() {
        return uComponent;
    }

    public GeographicDataSource getVComponent() {
        return vComponent;
    }

    @Override
    protected GeneralGridData mergeData(GeneralGridData other,
            GridEnvelope2D range1, GridEnvelope2D range2,
            GridGeometry2D geometry) {

        VectorGridData vData2 = (VectorGridData) other;
        DataSource newU = mergeData(getUComponent(), range1,
                vData2.getUComponent(), range2);
        DataSource newV = mergeData(getVComponent(), range1,
                vData2.getVComponent(), range2);
        return createVectorDataUV(geometry, newU, newV, getDataUnit());
    }

    private abstract static class VectorDataSource implements DataSource {

        protected final DataSource uComponent;

        protected final DataSource vComponent;

        public VectorDataSource(DataSource uComponent, DataSource vComponent) {
            this.uComponent = uComponent;
            this.vComponent = vComponent;
        }

    }

    private static final class MagnitudeDataSource extends VectorDataSource {

        public MagnitudeDataSource(DataSource uComponent,
                DataSource vComponent) {
            super(uComponent, vComponent);
        }

        @Override
        public double getDataValue(int x, int y) {
            return Math.hypot(uComponent.getDataValue(x, y),
                    vComponent.getDataValue(x, y));
        }
    }

    private static final class DirectionFromDataSource
            extends VectorDataSource {

        public DirectionFromDataSource(DataSource uComponent,
                DataSource vComponent) {
            super(uComponent, vComponent);
        }

        @Override
        public double getDataValue(int x, int y) {
            return Math.toDegrees(Math.atan2(-uComponent.getDataValue(x, y),
                    -vComponent.getDataValue(x, y)));
        }
    }

    private static final class DirectionToDataSource extends VectorDataSource {

        public DirectionToDataSource(DataSource uComponent,
                DataSource vComponent) {
            super(uComponent, vComponent);
        }

        @Override
        public double getDataValue(int x, int y) {
            return Math.toDegrees(Math.atan2(uComponent.getDataValue(x, y),
                    vComponent.getDataValue(x, y)));
        }
    }

    @Override
    public GeographicDataSource getData() {
        return getMagnitude();
    }
}
