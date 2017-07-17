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
package com.raytheon.uf.common.derivparam.data;

import java.lang.ref.SoftReference;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.derivparam.tree.LatLonDataLevelNode.LatOrLon;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.inventory.exception.DataCubeException;

/**
 * {@link AbstractRequestableData} that can provide the Latitude and Longitude
 * value for a specific {@link IGridGeometryProvider}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jul 17, 2017  6345     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class LatLonRequestableData extends AbstractRequestableData {

    private static final Cache cache = new Cache();

    private final LatOrLon parameter;

    public LatLonRequestableData(LatOrLon parameter) {
        this.parameter = parameter;
    }

    @Override
    public FloatDataRecord getDataValue(Object arg) throws DataCubeException {
        GridGeometry2D gridGeometry = getGridGeometry(arg);
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int nx = gridRange.width;
        int ny = gridRange.height;
        int npts = nx * ny;

        float[] llData = cache.get(gridGeometry);
        if (llData == null) {
            float[] points = new float[npts * 2];
            int index = 0;
            for (int j = 0; j < ny; j++) {
                for (int i = 0; i < nx; i++) {
                    points[index] = gridRange.x + i;
                    index += 1;
                    points[index] = gridRange.y + j;
                    index += 1;
                }
            }

            try {
                MathTransform toCRS = gridGeometry.getGridToCRS();
                toCRS.transform(points, 0, points, 0, npts);
                MathTransform toLatLon = CRS.findMathTransform(
                        gridGeometry.getCoordinateReferenceSystem(),
                        DefaultGeographicCRS.WGS84);
                toLatLon.transform(points, 0, points, 0, npts);
            } catch (FactoryException | TransformException e) {
                throw new DataCubeException("Failed to generate Lat/Lon data",
                        e);
            }
            llData = points;
            cache.put(gridGeometry, llData);
        }
        int offset = 0;
        if (parameter == LatOrLon.LATITUDE) {
            offset = 1;
        }
        float[] floatData = new float[npts];
        for (int i = offset; i < llData.length; i += 2) {
            floatData[i / 2] = llData[i];
        }
        long[] dimensions = { nx, ny };
        return new FloatDataRecord(getParameter(), "", floatData, 2,
                dimensions);

    }

    public GridGeometry2D getGridGeometry(Object arg) {
        return getSpace().getGridGeometry();
    }

    private static class Cache {

        private static GridGeometry2D cachedGeometry;

        private static SoftReference<float[]> cachedData;

        public synchronized float[] get(GridGeometry2D gridGeometry) {
            if (cachedGeometry != null && cachedGeometry.equals(gridGeometry)) {
                return cachedData.get();
            }
            return null;
        }

        public synchronized void put(GridGeometry2D gridGeometry,
                float[] llData) {
            cachedGeometry = gridGeometry;
            cachedData = new SoftReference<>(llData);
        }
    }

}