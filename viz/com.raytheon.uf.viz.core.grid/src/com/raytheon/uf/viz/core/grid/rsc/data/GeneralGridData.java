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

import java.awt.geom.Rectangle2D;

import javax.measure.Unit;
import javax.measure.UnitConverter;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.data.GeographicDataSource;
import com.raytheon.uf.common.geospatial.interpolation.Interpolation;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.numeric.source.OffsetDataSource;

/**
 *
 * An abstract class for classes which hold data for a grid.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- -----------------------------------------
 * Mar 09, 2011           bsteffen    Initial creation
 * Jul 17, 2013  2185     bsteffen    Cache computed grid reprojections.
 * Aug 27, 2013  2287     randerso    Removed 180 degree adjustment required by
 *                                    error in Maputil.rotation
 * Dec 09, 2013  2617     bsteffen    Added 180 degree rotation into reproject
 *                                    so wind direction is calculated as
 *                                    direction wind is coming from.
 * Jan 14, 2014  2661     bsteffen    For vectors only keep uComponent and
 *                                    vComponent, calculate magnitude and
 *                                    direction on demand.
 * Feb 03, 2014  2764     bsteffen    Ensure that internal buffers are array
 *                                    backed heap buffers.
 * Feb 28, 2013  2791     bsteffen    Use DataSource instead of FloatBuffers
 *                                    for data access
 * May 14, 2015  4079     bsteffen    Move to core.grid
 * Mar 21, 2018  6936     dgilling    Apply 180 degree correction to
 *                                    createVectorData.
 * Jun 27, 2019  65510    ksunil      support fill colors through XML
 * Aug 29, 2019  67949    tjensen     Refactor to support additional GFE products
 *
 * </pre>
 *
 * @author bsteffen
 */
public abstract class GeneralGridData {

    protected final GridGeometry2D gridGeometry;

    protected Unit<?> dataUnit;

    protected GeneralGridData(GeneralGridGeometry gridGeometry,
            Unit<?> dataUnit) {
        this.gridGeometry = GridGeometry2D.wrap(gridGeometry);
        this.dataUnit = dataUnit;
    }

    /**
     * Attempt to convert this data to the new unit. If this is successful then
     * the dataUnit and data will be changed.
     *
     * @param unit
     * @return true if units are compatible, false if data is unchanged.
     */
    public abstract boolean convert(Unit<?> unit);

    /**
     * Create a new GeneralGridData that is a reprojected version of this data.
     *
     * @param newGridGeometry
     * @param interpolation
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    public abstract GeneralGridData reproject(
            GeneralGridGeometry newGridGeometry, Interpolation interpolation)
            throws FactoryException, TransformException;

    public GridGeometry2D getGridGeometry() {
        return gridGeometry;
    }

    public Unit<?> getDataUnit() {
        return dataUnit;
    }

    /**
     * Given two grid data with compatible geometries and compatible units, this
     * will combine them into a single data object. Compatible geometries will
     * have the same CRS and grid spacing. This function will create a larger
     * grid geometry which incorporates the data from both sources and fills in
     * NaN for any areas which are not covered by either source.
     *
     *
     * @param data1
     * @param data2
     * @return merged data or null if they are not compatible
     */
    public static GeneralGridData mergeData(GeneralGridData data1,
            GeneralGridData data2) {

        if (data1.getClass() != data2.getClass()) {
            // Cannot merge different types of data
            return null;
        }
        if (!data2.convert(data1.getDataUnit())) {
            // units are not compatible
            return null;
        }

        GridGeometry2D geometry1 = data1.getGridGeometry();
        GridGeometry2D geometry2 = data2.getGridGeometry();
        CoordinateReferenceSystem crs = geometry1
                .getCoordinateReferenceSystem();
        CoordinateReferenceSystem crs2 = geometry2
                .getCoordinateReferenceSystem();
        if (!crs.equals(crs2)) {
            // Coordinate System is different, incompatible
            return null;
        }
        Envelope2D envelope1 = geometry1.getEnvelope2D();
        GridEnvelope2D range1 = geometry1.getGridRange2D();
        double dx = envelope1.width / range1.width;

        Envelope2D envelope2 = geometry2.getEnvelope2D();
        GridEnvelope2D range2 = geometry2.getGridRange2D();
        double dx2 = envelope2.width / range2.width;

        if (Math.abs(dx - dx2) > 0.00001) {
            // X Spacing is different, incompatible
            return null;
        }
        double dy = envelope1.height / range1.height;
        double dy2 = envelope2.height / range2.height;
        if (Math.abs(dy - dy2) > 0.00001) {
            // Y Spacing is different, incompatible
            return null;
        }
        double xShift = (envelope1.getMinX() - envelope2.getMinX()) / dx;
        if (Math.abs(xShift - Math.round(xShift)) > 0.00001) {
            // grids are not aligned in the x direction
            return null;
        }
        double yShift = (envelope1.getMinY() - envelope2.getMinY()) / dy;
        if (Math.abs(yShift - Math.round(yShift)) > 0.00001) {
            // grids are not aligned in the y direction
            return null;
        }

        Rectangle2D rectangle = envelope1.createUnion(envelope2);
        Envelope2D envelope = new Envelope2D(crs, rectangle);
        int nx = (int) Math.round(rectangle.getWidth() / dx);
        int ny = (int) Math.round(rectangle.getHeight() / dy);
        GridEnvelope2D range = new GridEnvelope2D(0, 0, nx, ny);

        GridGeometry2D geometry = new GridGeometry2D((GridEnvelope) range,
                (Envelope) envelope);
        // Shift the ranges to be relative to the new geometry
        range1.x = (int) Math.round((envelope1.x - envelope.x) / dx);
        range2.x = (int) Math.round((envelope2.x - envelope.x) / dx);
        // y axis is swapped, our grids start at upper left and y increases down
        // and y axis increases up.
        range1.y = (int) Math
                .round((envelope.getMaxY() - envelope1.getMaxY()) / dy);
        range2.y = (int) Math
                .round((envelope.getMaxY() - envelope2.getMaxY()) / dy);

        return data1.mergeData(data2, range1, range2, geometry);

    }

    protected abstract GeneralGridData mergeData(GeneralGridData other,
            GridEnvelope2D range1, GridEnvelope2D range2,
            GridGeometry2D geometry);

    protected static DataSource mergeData(DataSource data1, GridEnvelope2D env1,
            DataSource data2, GridEnvelope2D env2) {
        if (env1.x != 0 || env1.y != 0) {
            data1 = new OffsetDataSource(data1, -env1.x, -env1.y);
        }
        if (env2.x != 0 || env2.y != 0) {
            data2 = new OffsetDataSource(data2, -env2.x, -env2.y);
        }
        return new MergedDataSource(data1, data2);
    }

    private static final class MergedDataSource implements DataSource {

        private final DataSource[] sources;

        public MergedDataSource(DataSource... sources) {
            this.sources = sources;
        }

        @Override
        public double getDataValue(int x, int y) {
            int count = 0;
            double total = 0;
            for (DataSource source : sources) {
                double val = source.getDataValue(x, y);
                if (!Double.isNaN(val)) {
                    total += val;
                    count += 1;
                }
            }
            if (count == 0) {
                return Double.NaN;
            }
            return total / count;
        }
    }

    public abstract GeographicDataSource getData();
}
