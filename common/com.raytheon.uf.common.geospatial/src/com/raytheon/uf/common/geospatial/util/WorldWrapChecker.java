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
package com.raytheon.uf.common.geospatial.util;

import org.geotools.coverage.grid.GeneralGridGeometry;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Given a descriptor of a map, this class will check line segments for wrapping
 * around the world (crossing the inverse central meridian of the projection)
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 6, 2011            mschenke     Initial creation
 * Feb 17, 2015  4063     bsteffen     Use actual inverse central meridians for normalize longitudes.
 * 
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class WorldWrapChecker {

    /**
     * The longitude that is exactly 180° less than the central meridian
     * 
     * @see WorldWrapChecker#findAICM(MathTransform, double, double, double,
     *      double)
     */
    private final double idealLowInverseCentralMeridian;

    /**
     * The longitude that is exactly 180° more than the central meridian
     * 
     * @see WorldWrapChecker#findAICM(MathTransform, double, double, double,
     *      double)
     */
    private final double idealHighInverseCentralMeridian;

    /**
     * The longitude that is approximately 180° less than the central meridian.
     * Calculated as the lowest longitude value where a transform into crs space
     * will return a point that is not wrapped to the other side of the map.
     * 
     * @see WorldWrapChecker#findAICM(MathTransform, double, double, double,
     *      double)
     */
    private final double actualLowInverseCentralMeridian;

    /**
     * The longitude that is approximately 180° more than the central meridian.
     * Calculated as the highest longitude value where a transform into crs
     * space will return a point that is not wrapped to the other side of the
     * map.
     * 
     * @see WorldWrapChecker#findAICM(MathTransform, double, double, double,
     *      double)
     */
    private final double actualHighInverseCentralMeridian;

    private final boolean checkForWrapping;

    public WorldWrapChecker(Envelope worldEnvelope) {
        MapProjection worldProjection = CRS.getMapProjection(worldEnvelope
                .getCoordinateReferenceSystem());
        double centralMeridian = 0.0;
        double latitudeOfOrigin = 0.0;
        if (worldProjection != null) {
            ParameterValueGroup group = worldProjection.getParameterValues();
            try {
            centralMeridian = group.parameter(
                    AbstractProvider.CENTRAL_MERIDIAN.getName().getCode())
                    .doubleValue();
            } catch (ParameterNotFoundException e) {
                UFStatus.getHandler().handle(Priority.PROBLEM,
                        "Error determing world wrap checking", e);
                checkForWrapping = false;
                centralMeridian = Double.NaN;
                idealHighInverseCentralMeridian = idealLowInverseCentralMeridian = Double.NaN;
                actualHighInverseCentralMeridian = actualLowInverseCentralMeridian = Double.NaN;
                return;
            }
            try {
                latitudeOfOrigin = group
                        .parameter(
                                AbstractProvider.LATITUDE_OF_ORIGIN.getName()
                                        .getCode()).doubleValue();
            } catch (ParameterNotFoundException e) {
                latitudeOfOrigin = 0;
            }
        }

        idealHighInverseCentralMeridian = centralMeridian + 180.0;
        idealLowInverseCentralMeridian = centralMeridian - 180.0;

        PackedCoordinateSequence.Double testCoords = new PackedCoordinateSequence.Double(
                4, 2, 0);
        testCoords.setX(0, centralMeridian + 179.9);
        testCoords.setY(0, latitudeOfOrigin);
        testCoords.setX(1, centralMeridian + 179.8);
        testCoords.setY(1, latitudeOfOrigin);
        testCoords.setX(2, centralMeridian - 179.8);
        testCoords.setY(2, latitudeOfOrigin);
        testCoords.setX(3, centralMeridian - 179.9);
        testCoords.setY(3, latitudeOfOrigin);

        boolean checkForWrapping = false;
        double actualLowInverseCentralMeridian = idealLowInverseCentralMeridian;
        double actualHighInverseCentralMeridian = idealHighInverseCentralMeridian;

        try {
            MathTransform latLonToCRS = MapUtil
                    .getTransformFromLatLon(worldEnvelope
                            .getCoordinateReferenceSystem());

            latLonToCRS.transform(testCoords.getRawCoordinates(), 0,
                    testCoords.getRawCoordinates(), 0, 4);

            double furtherPoints = testCoords.getCoordinate(1).distance(
                    testCoords.getCoordinate(2));
            double closerPoints = testCoords.getCoordinate(0).distance(
                    testCoords.getCoordinate(3));

            checkForWrapping = closerPoints > furtherPoints;

            /*
             * When the central meridian is 0.0 than MapProjection does not roll
             * longitude so actual and ideal are the same.
             */
            if (checkForWrapping && centralMeridian != 0.0) {
                actualLowInverseCentralMeridian = findAICM(latLonToCRS,
                        idealLowInverseCentralMeridian, latitudeOfOrigin, -0.1,
                        testCoords.getX(3));

                actualHighInverseCentralMeridian = findAICM(latLonToCRS,
                        idealHighInverseCentralMeridian, latitudeOfOrigin, 0.1,
                        testCoords.getX(0));

            }
        } catch (Throwable t) {
            UFStatus.getHandler().handle(Priority.PROBLEM,
                    "Error determing world wrap checking", t);
        }
        this.checkForWrapping = checkForWrapping;
        this.actualLowInverseCentralMeridian = actualLowInverseCentralMeridian;
        this.actualHighInverseCentralMeridian = actualHighInverseCentralMeridian;
    }

    /**
     * Find the Actual Inverse Central Meridian(AICM)(either high or low) from a
     * provided ideal inverse central meridian. An ideal inverse central
     * meridian is the mathematically defined point which is 180° longitude from
     * the central meridian. The actual inverse central meridian is the
     * longitude value where the provided math transform will start converting
     * coordinates to the opposite side of the map. If the math transform was
     * perfect, and did not use floating point precision then the two values
     * would be the same, but since floats are very imperfect there is a slight
     * difference and it matters. For most ideal inverse central meridians the
     * result is only a few ULPs away from the ideal, the exception being an
     * ideal of 0.0 which has an actual that is quite hard to find. This method
     * is implemented as a binary search that will search the values in the
     * range of idealInverseCentralMeridian ± diff.
     * 
     * Here is an example to help explain: An equidistant cylindrical map
     * projection with a central meridian of -120° defines a valid CRS that
     * spans from -300° to 60°, these two endpoints are what are referred to as
     * the ideal low and high inverse central meridians. The x axis of the CRS
     * is defined so that the central meridian is at 0 and the inverse central
     * meridians are at ±2E7(half the circumference of the earth). Here is a
     * basic diagram of the area defined by the CRS with the top labeled in
     * Lat/Lon space and the bottom labeled in
     * 
     * <pre>
     * Ideal:
     * 
     * Longitude values: -300       -120         60
     *                     +----------+----------+
     *                     |          |          |
     *                     |          |          |
     *                     +----------+----------+
     * CRS x values:     -2E7         0         2E7
     * </pre>
     * 
     * This diagram represents the ideal view of the world, it matches the
     * mathematical definition of the CRS and it works great for most world wrap
     * checking. For example any line that goes from 50° to 70° will clearly
     * cross the 60° inverse central meridian so it needs special handling to
     * move the 70° point over to 290° without creating a line spanning the
     * world.
     * 
     * The problem is that the ideal does not represent what the math transform
     * actually does. In practice if 60° is converted to the crs the math
     * transform will give us -2E7. The cause of this discrepancy is the
     * inaccuracies that occur when dealing with floating point precision,
     * especially when degrees are converted to radians(which cannot be
     * perfectly represented). Even worse, if you take the next double value
     * smaller than 60, which is Math.nextAfter(60.0, 0) = 59.99999999999999,
     * this will also give a value of -2E7 even though it is clearly between
     * -120° and 60° and ideally it belongs on the positive side of the display.
     * The problem continues and there are actually 3 unique double values that
     * are less than 60 but still end up on the negative end of the map when
     * they are reprojected. It goes all the way down to 59.99999999999997 which
     * is the largest value that lands on the positive side of the map. This is
     * then the value we use as the actual high inverse central meridian, which
     * means that if there is a line segment from 50° to 59.99999999999998° we
     * know it will require special handling because it crosses the actual
     * inverse central meridian even though it never crosses the ideal inverse
     * central meridian. Here is a vasic diagram showing the actual area of the
     * CRS.
     * 
     * <pre>
     * Actual:
     * 
     * Longitude values: -299.99999999999994  -120  59.99999999999997
     *                               +----------+----------+
     *                               |          |          |
     *                               |          |          |
     *                               +----------+----------+
     * CRS x values:               -2E7         0         2E7
     * </pre>
     * 
     * On this particular map projection both ideal inverse central meridians
     * end up wrapping to the other side of the map. This means if a point
     * exactly on those lines is converted LatLon->crs->LatLon multiple times it
     * will continue to flip/flop back and forth. When this object shifts a
     * longitude into projection range, it will be shifted only once. This
     * ensures that any line segment that gets reprojected once is handled
     * correctly. Not all CRS have this behavior, many projections keep the
     * inverse central meridian on the ideal side of the map or even keep points
     * slightly further than the inverse central meridian on the same side of
     * the map. This method can detect any of these and return the actual value
     * where the map projection starts putting transformed coordinates on the
     * opposite side of the map.
     * 
     * 
     * @param latLonToCRS
     *            {@link MathTransform} to convert from Lat/Lon coordinates to
     *            crs.
     * @param idealInverseCentralMeridian
     *            - an ideal inverse central meridian.
     * @param latitudeOfOrigin
     *            the latitude of origin for the crs.
     * @param diff
     *            define the range and direction of the search. The search for
     *            values will cover the range idealInverseCentralMeridian ±
     *            diff. If the sign of this value should be positive for high
     *            inverse central meridian and negative for low.
     * @param sampleX
     *            the x value of a coordinate converted on the same side of the
     *            inverse central meridian being tested.
     * @return the highest(if diff is positive) or lowest(if diff is negative)
     *         longitude for which latLonToCRS will return a coordinate on the
     *         same side of the map as sampleX.
     * @throws TransformException
     * @see {@link Math#nextAfter(double, double)}.
     * @see Math#ulp(double)
     */
    private double findAICM(MathTransform latLonToCRS,
            double idealInverseCentralMeridian, double latitudeOfOrigin,
            double diff, double sampleX) throws TransformException {
        double sign = Math.signum(sampleX);
        double result = idealInverseCentralMeridian;
        double test = idealInverseCentralMeridian;
        double lasttest = Double.NaN;
        while (test != lasttest) {
            lasttest = test;
            double[] in = new double[] { test, latitudeOfOrigin };
            latLonToCRS.transform(in, 0, in, 0, 1);
            if (Math.signum(in[0]) == sign) {
                result = test;
                test += diff;
            } else {
                test -= diff;
            }
            diff /= 2;
        }
        return result;
    }

    public WorldWrapChecker(GeneralGridGeometry worldGeometry) {
        this(worldGeometry.getEnvelope());
    }

    /**
     * Checks to see if the line between point a and b wrap around the world
     * 
     * @param a
     *            lat/lon starting coordinate
     * @param b
     *            lat/lon ending coordinate
     * @return true if wraps, false otherwise
     */
    public boolean check(double aLon, double bLon) {
        if (!checkForWrapping) {
            return false;
        }

        aLon = toProjectionRange(aLon);
        bLon = toProjectionRange(bLon);

        return Math.abs(aLon - bLon) > 180.0;//
    }

    /**
     * @return the lowInverseCentralMeridian
     */
    public double getLowInverseCentralMeridian() {
        return idealLowInverseCentralMeridian;
    }

    /**
     * @return the highInverseCentralMeridian
     */
    public double getHighInverseCentralMeridian() {
        return idealHighInverseCentralMeridian;
    }

    public double toProjectionRange(double aLon) {
        /*
         * Do not just perform both corrections. If the difference between the
         * two actual inverse central meridians is larger than 360 we only want
         * to correct it one way.
         */
        if (aLon < actualLowInverseCentralMeridian) {
            while (aLon < actualLowInverseCentralMeridian) {
                aLon += 360.0;
            }
        } else {
            while (aLon > actualHighInverseCentralMeridian) {
                aLon -= 360.0;
            }
        }
        return aLon;
    }

    public boolean needsChecking() {
        return checkForWrapping;
    }

}
