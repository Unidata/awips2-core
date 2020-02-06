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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.EquidistantCylindrical;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.MapProjection.AbstractProvider;
import org.opengis.geometry.Envelope;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.geom.Triangle;

/**
 * Utility class capable of computing geometric intersection of one
 * {@link Envelope} into another. Resulting {@link Geometry} may consist of
 * multiple {@link Polygon}s
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Dec 14, 2012           mschenke  Initial creation
 * Sep 13, 2013  2309     bsteffen  Corrected Lines that are extrapolated to
 *                                  intersect the border will use projection
 *                                  factor from all 4 corners instead of 3.
 * Oct 08, 2013  2104     mschenke  Added case for where actual border is 
 *                                  inside out by checking interior point
 * Nov 18, 2013  2528     bsteffen  Fall back to brute force when corner
 *                                  points are not found.
 * Feb 23, 2015  4022     bsteffen  Return empty polygon when empty envelopes
 *                                  are used.
 * May 27, 2015  4472     bsteffen  Change the way the border is calculated to
 *                                  handle untransformable corners better.
 * Jun 11, 2015  4551     bsteffen  Add minNumDivs to calculateEdge
 * Aug 11, 2015  4713     bsteffen  Fall back to brute force for topology
 *                                  exception.
 * Sep 03, 2015  4831     bsteffen  Use approximateEquals when comparing
 *                                  coordinates that have been through the
 *                                  WorldWrapCorrector.
 * Jan 30, 2018  7172     bsteffen  Handle abnormal envelopes.
 * Jan 27, 2020  3375     bsteffen  Defer to Brute force when intersections
 *                                  fail. Only invert the area if it actually
 *                                  improves the result.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class EnvelopeIntersection {

    protected static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(EnvelopeIntersection.class);

    private static final GeometryFactory gf = new GeometryFactory();

    /**
     * Computes an intersection {@link Geometry} between sourceEnvelope and
     * targetEnvelope in targetEnvelope's CRS space. The resulting
     * {@link Geometry} may contain multiple Geometries within it. But all
     * geometries will be {@link Polygon}s
     * 
     * When reprojecting envelopes with significantly different CRSs there is a
     * trade off between accuracy and performance. This is easily visualized for
     * a reprojection where a straight line in the source CRS becomes a curve in
     * the target CRS. To accurately represent the curve you would need infinite
     * points, which would be slow to calculate. The fastest way to calculate it
     * would be to just use the end points, but then your curve becomes a
     * straight line and you will be missing a chunk of the source envelope in
     * the result. This particular method makes no strong guarantees about the
     * performance or accuracy of the reprojection. It attempts to use
     * reasonable thresholds for the reprojection. If more control is needed
     * consider using one of the other methods in this class that provides more
     * control.
     * 
     * @see #createEnvelopeIntersection(Envelope, Envelope, int)
     * @see #createEnvelopeIntersection(Envelope, Envelope, double, int, int)
     */
    public static Geometry createEnvelopeIntersection(Envelope sourceEnvelope,
            Envelope targetEnvelope)
            throws TransformException, FactoryException {
        return createEnvelopeIntersection(sourceEnvelope, targetEnvelope, 1000);
    }

    /**
     * Performs the same basic function as
     * {@link #createEnvelopeIntersection(Envelope, Envelope)} but provides a
     * single performance/accuracy tuning parameter.
     * 
     * The effort parameter is used to control the performance/accuracy
     * tradeoff. Smaller values for effort will complete faster but with a less
     * accurate result. As a general guideline, reasonable values for effort are
     * between 100 and 100000.
     * 
     * Values below 100 are possible but if the source and target have
     * significant warping then significant pieces of the sourceEnvelope may be
     * missing from the result.
     * 
     * Values above 100000 are also allowed but may take a long time to
     * calculate or use excessive memory. If a reprojection is not accurate
     * enough with an effort value of 100000 you should consider using
     * {@link #createEnvelopeIntersection(Envelope, Envelope, double, int, int)}
     * to tune specific parameters to get the desired result.
     * 
     * @see #createEnvelopeIntersection(Envelope, Envelope)
     * @see #createEnvelopeIntersection(Envelope, Envelope, double, int, int)
     */
    public static Geometry createEnvelopeIntersection(Envelope sourceEnvelope,
            Envelope targetEnvelope, int effort)
            throws TransformException, FactoryException {
        /*
         * Set maxHorDivisions and maxVertDivisions so that the aspect ratio of
         * the source envelope is preserved and
         * maxHorDivisions*maxVertDivisions=effort
         */
        double aspectRatio = sourceEnvelope.getSpan(0)
                / sourceEnvelope.getSpan(1);
        int maxHorDivisions = (int) Math.sqrt(effort * aspectRatio);
        int maxVertDivisions = effort / maxHorDivisions;
        /*
         * Set threshold so that the average length of an edge of the target
         * envelope is effort*threshold
         */
        double threshold = (targetEnvelope.getSpan(0)
                + targetEnvelope.getSpan(1)) / (2 * effort);
        return createEnvelopeIntersection(sourceEnvelope, targetEnvelope,
                threshold, maxHorDivisions, maxVertDivisions);
    }

    /**
     * Performs the same basic function as
     * {@link #createEnvelopeIntersection(Envelope, Envelope)} but provides
     * multiple performance/accuracy tuning parameter.
     * 
     * When computing the points along a curved reproject the line will be
     * divided until the resulting LineString is within threshold distance of
     * the actual curved line or until the maximum number of divisions is
     * reached.
     * 
     * Since threshold is used to compare the distance of points in the target
     * CRS, it is specified in the units of the target CRS(usually meters).
     * Smaller threshold values will result in a slower, more accurate
     * reprojection.
     * 
     * The maxHorDivisions and maxVertDivisions provide a bound for the number
     * of times to divide a line that is being reprojected. maxHorDivisions is
     * used for lines that are horizontal in the source envelope.
     * maxVertDivisions is used for lines that are vertical in the source
     * envelope. Specifying a larger number of divisions will result in a
     * slower, more accurate reprojection.
     * 
     * @see #createEnvelopeIntersection(Envelope, Envelope)
     * @see #createEnvelopeIntersection(Envelope, Envelope, int)
     */
    public static Geometry createEnvelopeIntersection(Envelope sourceEnvelope,
            Envelope targetEnvelope, double threshold, int maxHorDivisions,
            int maxVertDivisions) throws TransformException, FactoryException {
        ReferencedEnvelope sourceREnvelope = reference(sourceEnvelope);
        ReferencedEnvelope targetREnvelope = reference(targetEnvelope);
        if (isEmpty(sourceREnvelope) || isEmpty(targetREnvelope)) {
            /* return an empty polygon */
            return gf.createPolygon(null, null);
        }
        MathTransform sourceCRSToTargetCRS = CRS.findMathTransform(
                sourceREnvelope.getCoordinateReferenceSystem(),
                targetREnvelope.getCoordinateReferenceSystem());
        if (sourceCRSToTargetCRS.isIdentity()) {
            /*
             * Referenced envelope will only perform an intersection if the CRSs
             * are identical. However it is possible to get an identity math
             * transform with slight variances in the object types of the CRSs.
             * This is known to happen on Equidistant Cylindrical projections.
             * To get around this force the source envelope into the target CRS.
             */
            sourceREnvelope = new ReferencedEnvelope(sourceREnvelope,
                    targetREnvelope.getCoordinateReferenceSystem());
            org.locationtech.jts.geom.Envelope intersection = sourceREnvelope
                    .intersection(targetREnvelope);
            if (intersection == null) {
                return gf.createGeometryCollection(new Geometry[0]);
            } else {
                Coordinate[] border = new Coordinate[5];
                border[0] = new Coordinate(intersection.getMinX(),
                        intersection.getMinY());
                border[1] = new Coordinate(intersection.getMinX(),
                        intersection.getMaxY());
                border[2] = new Coordinate(intersection.getMaxX(),
                        intersection.getMaxY());
                border[3] = new Coordinate(intersection.getMaxX(),
                        intersection.getMinY());
                border[4] = border[0];
                return gf.createPolygon(gf.createLinearRing(border), null);
            }
        }
        ReferencedEnvelope[] normalSource = normalizeEnvelope(sourceREnvelope);
        if (normalSource.length == 1) {
            sourceREnvelope = normalSource[0];
        } else {
            Geometry[] splitResults = new Geometry[normalSource.length];
            for (int i = 0; i < normalSource.length; i += 1) {
                splitResults[i] = createEnvelopeIntersection(normalSource[i],
                        targetEnvelope, threshold, maxHorDivisions / 2,
                        maxVertDivisions);
            }
            return toPolygonal(gf.createGeometryCollection(splitResults));
        }

        Geometry border = null;
        WorldWrapCorrector corrector = new WorldWrapCorrector(targetREnvelope);
        MathTransform targetCRSToSourceCRS = sourceCRSToTargetCRS.inverse();
        MathTransform targetCRSToLatLon = MapUtil.getTransformToLatLon(
                targetREnvelope.getCoordinateReferenceSystem());

        // Create Polygon representing target envelope
        Coordinate ul = new Coordinate(targetREnvelope.getMinimum(0),
                targetREnvelope.getMinimum(1));
        Coordinate ur = new Coordinate(targetREnvelope.getMaximum(0),
                targetREnvelope.getMinimum(1));
        Coordinate lr = new Coordinate(targetREnvelope.getMaximum(0),
                targetREnvelope.getMaximum(1));
        Coordinate ll = new Coordinate(targetREnvelope.getMinimum(0),
                targetREnvelope.getMaximum(1));

        Polygon targetBorder = gf.createPolygon(
                gf.createLinearRing(new Coordinate[] { ul, ur, lr, ll, ul }),
                null);

        List<Coordinate> borderPoints = calculateBorder(sourceREnvelope,
                sourceCRSToTargetCRS, threshold, maxHorDivisions,
                maxVertDivisions);
        if (borderPoints == null) {
            return BruteForceEnvelopeIntersection.createEnvelopeIntersection(
                    sourceEnvelope, targetEnvelope, maxHorDivisions,
                    maxVertDivisions);
        }
        // Create valid continuous LineStrings for source border
        List<LineString> lineStrings = new ArrayList<>();
        List<Coordinate> currString = new ArrayList<>();
        boolean foundValid = false;
        for (Coordinate c : borderPoints) {
            if (!Double.isNaN(c.x) && !Double.isNaN(c.y)) {
                foundValid = true;
                currString.add(c);
            } else if (foundValid) {
                if (currString.size() > 1) {
                    lineStrings.add(gf.createLineString(
                            currString.toArray(new Coordinate[0])));
                    currString.clear();
                }
                foundValid = false;
            }
        }
        if (currString.size() > 1) {
            lineStrings.add(
                    gf.createLineString(currString.toArray(new Coordinate[0])));
        }

        MathTransform latLonToTargetCRS = targetCRSToLatLon.inverse();

        // If we are able to create a polygon from the lineStrings, save it in
        // this variable to use later if all else fails.
        Geometry correctedPolygon = null;

        int numStrings = lineStrings.size();
        if (numStrings == 1) {
            // Check for one continuous line string that starts and ends at same
            // point, if so, attempt to create single polygon and correct it
            LineString ls = lineStrings.get(0);
            Coordinate[] coords = ls.getCoordinates();
            if (coords[0].equals(coords[coords.length - 1])) {
                border = gf.createPolygon(gf.createLinearRing(coords), null);
                border = correctedPolygon = JTS.transform(
                        corrector.correct(
                                JTS.transform(border, targetCRSToLatLon)),
                        latLonToTargetCRS);
            }
        }

        if ((border == null || border.isEmpty() || !border.isValid())
                && numStrings > 0) {
            // This may happen if more than one valid line string found or
            // correcting the single line string produced invalid geometries and
            // therefore the border is empty

            // Here we check for a simple case where entire target CRS is within
            // source CRS making border equivalent to target CRS border
            // Convert corner points of target envelope into source envelope
            // space
            boolean bad = false;
            try {
                double[] in = new double[] { ul.x, ul.y, ur.x, ur.y, lr.x, lr.y,
                        ll.x, ll.y };
                double[] out = new double[in.length];
                targetCRSToSourceCRS.transform(in, 0, out, 0, 4);
                for (int i = 0, idx = 0; i < 4 && !bad; i++, idx += 2) {
                    if (!sourceREnvelope.contains(out[idx], out[idx + 1])) {
                        // if any point not within source envelope, this case is
                        // bad
                        bad = true;
                    }
                }
            } catch (TransformException e) {
                bad = true;
            }
            if (!bad) {
                // base case, use entire target border polygon
                border = targetBorder;
            } else {
                // Complicated case, take valid line strings and compute
                // intersection area with target border and combine all
                // intersections in collection
                Coordinate[] borderCorners = { ul, ur, lr, ll };

                // First, world wrap correct the line strings
                List<LineString> corrected = new ArrayList<>();
                for (LineString ls : lineStrings) {
                    extractLineStrings(corrected, JTS.transform(
                            corrector.correct(
                                    JTS.transform(ls, targetCRSToLatLon)),
                            latLonToTargetCRS));
                }

                // Second, connect any line strings that are continuous (start
                // point in one is end point in another)
                List<LineString> connected = new ArrayList<>();
                // Start with first in corrected in connected
                connected.add(corrected.get(corrected.size() - 1));
                corrected.remove(connected.get(0));
                boolean done = false;
                while (!done) {
                    LineString geomA = null, geomB = null;
                    LineString newGeom = null;
                    // For each LineString in connected, check if any in
                    // corrected connect to it. If it does, remove from
                    // corrected, connected with it and add back into connected
                    // for more processing
                    for (LineString ls1 : connected) {
                        geomA = ls1;
                        for (LineString ls2 : corrected) {
                            geomB = ls2;
                            Coordinate[] c1 = ls1.getCoordinates();
                            Coordinate c1_0 = c1[0];
                            Coordinate c1_l = c1[c1.length - 1];
                            Coordinate[] c2 = ls2.getCoordinates();
                            Coordinate c2_0 = c2[0];
                            Coordinate c2_l = c2[c2.length - 1];

                            boolean connectLS1toLS2 = approximateEquals(c1_l,
                                    c2_0);
                            boolean connectLS2toLS1 = approximateEquals(c1_0,
                                    c2_l);
                            if (connectLS2toLS1 || connectLS1toLS2) {
                                // ls1 and ls2 are connected, create new geom
                                if (connectLS2toLS1) {
                                    Coordinate[] tmp = c1;
                                    c1 = c2;
                                    c2 = tmp;
                                }
                                // These line strings can be connected
                                Coordinate[] newCoords = new Coordinate[c1.length
                                        + c2.length - 1];
                                for (int i = 0; i < c1.length; ++i) {
                                    newCoords[i] = c1[i];
                                }
                                for (int i = 1; i < c2.length; ++i) {
                                    newCoords[i + c1.length - 1] = c2[i];
                                }
                                newGeom = gf.createLineString(newCoords);
                                break;
                            }
                        }
                        if (newGeom != null) {
                            break;
                        }
                    }
                    if (newGeom != null) {
                        connected.remove(geomA);
                        corrected.remove(geomB);
                        connected.add(newGeom);
                        newGeom = null;
                    } else {
                        // Nothing found that can be connected, we are done
                        done = true;
                    }
                }
                corrected = connected;

                // Process all connected-corrected LineStrings into polygonal
                // intersections with the target border
                List<Geometry> borders = new ArrayList<>();
                for (Geometry correctedLs : corrected) {
                    if (targetBorder.intersects(correctedLs)) {
                        // Here we want to make sure there are points in the
                        // line string that intersects the tile border. This
                        // algorithm looks at first 2 and last 2 points in
                        // correctedLS and extrapolates to find intersections
                        // point with one of the LineSegments in the
                        // targetBorder. Those points are then added to the
                        // LineString
                        Coordinate[] lsCoords = correctedLs.getCoordinates();
                        List<Coordinate> lsACoords = new ArrayList<>(
                                Arrays.asList(lsCoords));
                        LineSegment one = new LineSegment(lsCoords[1],
                                lsCoords[0]);
                        LineSegment two = new LineSegment(
                                lsCoords[lsCoords.length - 2],
                                lsCoords[lsCoords.length - 1]);
                        double bestProjectionFactorOne = 1.0;
                        double bestProjectionFactorTwo = 1.0;
                        Coordinate cOne = null, cTwo = null;
                        for (Coordinate borderCorner : borderCorners) {
                            double factor = one.projectionFactor(borderCorner);
                            if (factor > bestProjectionFactorOne) {
                                cOne = one.pointAlong(factor);
                                bestProjectionFactorOne = factor;
                            }
                            factor = two.projectionFactor(borderCorner);
                            if (factor > bestProjectionFactorTwo) {
                                cTwo = two.pointAlong(factor);
                                bestProjectionFactorTwo = factor;
                            }
                        }
                        if (cOne != null) {
                            lsACoords.add(0, cOne);
                        }
                        if (cTwo != null) {
                            lsACoords.add(cTwo);
                        }
                        if (lsACoords.size() > lsCoords.length) {
                            // Points were added, recreate correctedLs
                            lsCoords = lsACoords.toArray(new Coordinate[0]);
                            correctedLs = gf.createLineString(lsCoords);
                        }

                        /* Intersect with targetBorder to trim LineString */
                        try {
                            correctedLs = targetBorder
                                    .intersection(correctedLs);
                        } catch (TopologyException e) {
                            /*
                             * This is known to happen when the target envelope
                             * is stereographic and the source envelope contains
                             * the point on the earth that is opposite of the
                             * stereographic center. It would be better to
                             * detect this case earlier but I don't know how.
                             */
                            return BruteForceEnvelopeIntersection
                                    .createEnvelopeIntersection(sourceEnvelope,
                                            targetEnvelope, maxHorDivisions,
                                            maxVertDivisions);
                        }
                    } else {
                        statusHandler.debug(
                                "LineString lives completely outside target extent");
                        continue;
                    }

                    // Intersection of correctedLS with targetBorder might
                    // result in MultiLineString if correctedLS entered and
                    // exited targetBorder multiple times so we extract them
                    // here
                    List<LineString> correctedLsArray = new ArrayList<>();
                    extractLineStrings(correctedLsArray, correctedLs);

                    for (LineString ls : correctedLsArray) {
                        // For each LineString, we know first and last
                        // coordinates are on the targetBorder so we difference
                        // targetBorder with the LineString to get a border with
                        // those points included. Then we can walk the border
                        // and create a polygon of the LineString and the
                        // border. At that point, we will either have a polygon
                        // where an interior point is within the sourceEnvelope
                        // or not. If not, we will use the targetBorder
                        // differenced with it to get the part that represents
                        // the sourceEnvelopes intersection
                        Coordinate[] boundaryCoords = targetBorder
                                .difference(ls).getCoordinates();
                        Coordinate[] lsCoords = ls.getCoordinates();
                        List<Coordinate> lsACoords = new ArrayList<>(
                                Arrays.asList(lsCoords));
                        Coordinate first = lsCoords[0];
                        Coordinate last = lsCoords[lsCoords.length - 1];
                        // Find index of last in boundaryCoords
                        int idx = 0;
                        for (; idx < boundaryCoords.length; ++idx) {
                            if (last.equals(boundaryCoords[idx])) {
                                break;
                            }
                        }
                        int startIdx = idx;
                        done = idx == boundaryCoords.length;
                        if (done) {
                            statusHandler
                                    .debug("Could not find intersection point in polygon "
                                            + "boundry for last point in LineString");
                            return BruteForceEnvelopeIntersection
                                    .createEnvelopeIntersection(sourceEnvelope,
                                            targetEnvelope, maxHorDivisions,
                                            maxVertDivisions);
                        }
                        while (!done) {
                            // Append points to the LineString until we find
                            // first coordinate which indicates we are done
                            idx = (idx + 1) % boundaryCoords.length;
                            if (idx != startIdx) {
                                lsACoords.add(boundaryCoords[idx]);
                                if (boundaryCoords[idx].equals(first)) {
                                    done = true;
                                }
                            } else {
                                done = true;
                            }
                        }
                        if (idx == startIdx) {
                            statusHandler
                                    .debug("Could not find intersection point in polygon "
                                            + "boundry for first point in LineString");
                            return BruteForceEnvelopeIntersection
                                    .createEnvelopeIntersection(sourceEnvelope,
                                            targetEnvelope, maxHorDivisions,
                                            maxVertDivisions);
                        }

                        // Create polygon out of LineString points and check if
                        // it is the half that is within the source CRS
                        Polygon lsA = gf.createPolygon(
                                gf.createLinearRing(
                                        lsACoords.toArray(new Coordinate[0])),
                                null);
                        Coordinate pointA = lsA.getInteriorPoint()
                                .getCoordinate();
                        double[] out = new double[2];
                        targetCRSToSourceCRS.transform(
                                new double[] { pointA.x, pointA.y }, 0, out, 0,
                                1);
                        if (sourceREnvelope.contains(out[0], out[1])) {
                            borders.add(lsA);
                        } else {
                            borders.add(targetBorder.difference(lsA));
                        }
                    }
                }

                // Combine any borders that intersect to create large
                // intersecting areas
                done = false;
                while (!done) {
                    Geometry newGeom = null;
                    Geometry oldA = null, oldB = null;
                    for (Geometry g1 : borders) {
                        oldA = g1;
                        for (Geometry g2 : borders) {
                            if (g2 != oldA) {
                                oldB = g2;
                                if (oldB.intersects(oldA)) {
                                    newGeom = oldB.intersection(oldA);
                                    break;
                                }
                            }
                        }
                        if (newGeom != null) {
                            break;
                        }
                    }
                    if (newGeom != null) {
                        borders.remove(oldA);
                        borders.remove(oldB);
                        borders.add(newGeom);
                        newGeom = null;
                    } else {
                        done = true;
                    }
                }

                // We are done!
                if (borders.size() == 1) {
                    border = borders.get(0);
                } else {
                    border = gf.createGeometryCollection(
                            borders.toArray(new Geometry[0]));
                }
            }
        }

        if (border == null || border.isEmpty() || !border.isValid()) {
            if (correctedPolygon != null) {
                // buffering will make an invalid polygon valid. This is known
                // to be the correct action for rounded grids such as lat/lon
                // source on a polar stereographic target or radial data.
                border = correctedPolygon.buffer(0);
            } else if (lineStrings.size() == 1) {
                // There is a last resort. There are no known envelopes that hit
                // this but just in case we have problems this will be slightly
                // better than nothing.
                border = lineStrings.get(0).getEnvelope();
            } else {
                // Also a last resort.
                List<Geometry> envelopes = new ArrayList<>(lineStrings.size());
                for (LineString ls : lineStrings) {
                    envelopes.add(ls.getEnvelope());
                }
                border = gf.createGeometryCollection(
                        envelopes.toArray(new Geometry[0]));
            }
        } else if (border instanceof Polygon && border != targetBorder) {
            // Simple polygonal border, ensure it accurately represents our
            // source envelope by checking interior point
            Coordinate interior = JTS
                    .transform(border.getInteriorPoint(), targetCRSToSourceCRS)
                    .getCoordinate();
            if (!sourceREnvelope.contains(interior)) {
                // Interior point does not fall inside the source envelope, use
                // the difference of the border
                Geometry inverted = targetBorder.difference(border);
                Coordinate invertedInterior = JTS
                        .transform(border.getInteriorPoint(),
                                targetCRSToSourceCRS)
                        .getCoordinate();
                if (sourceREnvelope.contains(invertedInterior)) {
                    border = inverted;
                }
            }
        }

        border = handleAbnormalTarget(targetREnvelope, targetBorder, border);

        // Convert border to polygonal based geometry (Polygon or MultiPolygon)
        return toPolygonal(border);
    }

    private static void extractLineStrings(List<LineString> lines,
            Geometry geom) {
        if (geom instanceof GeometryCollection) {
            for (int n = 0; n < geom.getNumGeometries(); ++n) {
                extractLineStrings(lines, geom.getGeometryN(n));
            }
        } else if (geom instanceof LineString) {
            lines.add((LineString) geom);
        }
    }

    /**
     * 
     * <p>
     * Attempt to transform the border of the source envelope into target CRS.
     * This transforms each of the 4 edges of the source envelope(see
     * {@link #calculateEdge(double[], double[], int, double, MathTransform)}
     * for details). In the simple case where all 4 edges of the source envelope
     * are valid points in the target CRS, this will simply join the 4 edges
     * into a single list and return.
     * </p>
     * 
     * <p>
     * This method attempts to detect and correct two types of transformation
     * where some edge coordinates are not valid in the target CRS.
     * <ol>
     * <li>If an entire edge is invalid then this will attempt to connect the
     * nearest valid points from the two adjacent edges. For example if the
     * source envelope is an Equidistant Cylindrical projection with one edge
     * along the north pole and the target CRS is a Mercator projection. Since
     * the north pole cannot be represented in a Mercator projection the entire
     * upper edge of the source envelope is invalid in the target CRS. This is
     * corrected by joining the northern most valid points on the left and right
     * edges. The end result is that the north pole is cut out of the
     * reprojected envelope.</li>
     * <li>If a corner point is invalid then this will attempt to connect the
     * nearest valid points from the two adjacent edges. This happens often when
     * the source envelope is in a Geostationary projection.</li>
     * </ol>
     * When performing these corrections an area of the source envelope
     * containing the invalid points is removed. The area that is removed is a
     * straight "cut" across the source envelope(removing either an edge or a
     * corner). Because the boundary between valid/invalid is often not a
     * straight line, it is likely that this cut will also remove some valid
     * area. Too avoid removing too much valid, if the area being removed is
     * unreasonably large the correction will be aborted and null will be
     * returned.(Unreasonable is calculated based off the maxDivision
     * arguments).
     * </p>
     * 
     * <p>
     * This does not currently detect cases where some points in the middle of
     * an edge are not valid in the target CRS. In this case there will be
     * Coordinates with NaN ordinates in the list.
     * </p>
     * 
     * <p>
     * For convenience, in the returned list the first and last Coordinate will
     * be the same so it is easy to construct a polygon from the sequence, but
     * this method does not guarantee that the results are a valid polygon.
     * Often there will be redundant points or self intersections that must be
     * corrected before the coordinates can be used to construct a polygon.
     * </p>
     * 
     * <p>
     * This method will return null for cases where there are too many invalid
     * points to determine an accurate border.
     * </p>
     */
    private static List<Coordinate> calculateBorder(
            ReferencedEnvelope sourceEnvelope,
            MathTransform sourceCRSToTargetCRS, double threshold,
            int maxHorDivisions, int maxVertDivisions)
            throws TransformException {
        MathTransform targetCRSToSourceCRS = sourceCRSToTargetCRS.inverse();

        /*
         * This is an attempt to determine a "reasonable" amount of the source
         * envelope to remove to compensate for untransformable areas. For the
         * case of removing a single edge the minimum amount that can be removed
         * is 1 division(hor or vert depending on which edge). "reasonable" is
         * currently slightly larger than the average of the two maxDivisions.
         */
        double maximumRemovalPercent = 3.0
                / (maxHorDivisions + maxVertDivisions);
        double maximumRemovalArea = sourceEnvelope.getArea()
                * maximumRemovalPercent;
        /*
         * This is probably too many, but unsure how to come up with a good
         * smaller number.
         */
        int maxDiagnolDivisions = (maxHorDivisions + maxVertDivisions) / 2;

        double[] UL = { sourceEnvelope.getMinX(), sourceEnvelope.getMinY() };
        double[] UR = { sourceEnvelope.getMaxX(), sourceEnvelope.getMinY() };
        double[] LR = { sourceEnvelope.getMaxX(), sourceEnvelope.getMaxY() };
        double[] LL = { sourceEnvelope.getMinX(), sourceEnvelope.getMaxY() };

        /*
         * The order of the coordinates passed in matters, this will generate a
         * clockwise sequence of points.
         */
        List<Coordinate> upperEdge = calculateEdge(UL, UR, maxHorDivisions,
                threshold, sourceCRSToTargetCRS);
        List<Coordinate> rightEdge = calculateEdge(UR, LR, maxVertDivisions,
                threshold, sourceCRSToTargetCRS);
        List<Coordinate> lowerEdge = calculateEdge(LR, LL, maxHorDivisions,
                threshold, sourceCRSToTargetCRS);
        List<Coordinate> leftEdge = calculateEdge(LL, UL, maxVertDivisions,
                threshold, sourceCRSToTargetCRS);

        /* No upper edge so attempt to add one. */
        if (upperEdge.isEmpty() && !rightEdge.isEmpty()
                && !leftEdge.isEmpty()) {
            Coordinate[] removedBounds = new Coordinate[5];
            removedBounds[0] = new Coordinate(UL[0], UL[1]);
            removedBounds[1] = new Coordinate(UR[0], UR[1]);

            Coordinate top = leftEdge.get(leftEdge.size() - 1);
            UL = new double[] { top.x, top.y };
            targetCRSToSourceCRS.transform(UL, 0, UL, 0, 1);

            top = rightEdge.get(0);
            UR = new double[] { top.x, top.y };
            targetCRSToSourceCRS.transform(UR, 0, UR, 0, 1);

            removedBounds[2] = new Coordinate(UR[0], UR[1]);
            removedBounds[3] = new Coordinate(UL[0], UL[1]);
            removedBounds[4] = removedBounds[0];
            double removedArea = gf.createPolygon(removedBounds).getArea();
            if (removedArea > maximumRemovalArea) {
                return null;
            }
            upperEdge = calculateEdge(UL, UR, maxHorDivisions, threshold,
                    sourceCRSToTargetCRS);
        }
        /* No lower edge so attempt to add one. */
        if (lowerEdge.isEmpty() && !rightEdge.isEmpty()
                && !leftEdge.isEmpty()) {
            Coordinate[] removedBounds = new Coordinate[5];
            removedBounds[0] = new Coordinate(LL[0], LL[1]);
            removedBounds[1] = new Coordinate(LR[0], LR[1]);

            Coordinate low = leftEdge.get(0);
            LL = new double[] { low.x, low.y };
            targetCRSToSourceCRS.transform(LL, 0, LL, 0, 1);

            low = rightEdge.get(rightEdge.size() - 1);
            LR = new double[] { low.x, low.y };
            targetCRSToSourceCRS.transform(LR, 0, LR, 0, 1);

            removedBounds[2] = new Coordinate(LR[0], LR[1]);
            removedBounds[3] = new Coordinate(LL[0], LL[1]);
            removedBounds[4] = removedBounds[0];
            double removedArea = gf.createPolygon(removedBounds).getArea();
            if (removedArea > maximumRemovalArea) {
                return null;
            }

            lowerEdge = calculateEdge(LR, LL, maxHorDivisions, threshold,
                    sourceCRSToTargetCRS);
        }
        /* No right edge so attempt to add one. */
        if (rightEdge.isEmpty() && !lowerEdge.isEmpty()
                && !upperEdge.isEmpty()) {
            Coordinate[] removedBounds = new Coordinate[5];
            removedBounds[0] = new Coordinate(UR[0], UR[1]);
            removedBounds[1] = new Coordinate(LR[0], LR[1]);

            Coordinate right = upperEdge.get(upperEdge.size() - 1);
            UR = new double[] { right.x, right.y };
            targetCRSToSourceCRS.transform(UR, 0, UR, 0, 1);

            right = lowerEdge.get(0);
            LR = new double[] { right.x, right.y };
            targetCRSToSourceCRS.transform(LR, 0, LR, 0, 1);

            removedBounds[2] = new Coordinate(LR[0], LR[1]);
            removedBounds[3] = new Coordinate(UR[0], UR[1]);
            removedBounds[4] = removedBounds[0];
            double removedArea = gf.createPolygon(removedBounds).getArea();
            if (removedArea > maximumRemovalArea) {
                return null;
            }
            rightEdge = calculateEdge(UR, LR, maxVertDivisions, threshold,
                    sourceCRSToTargetCRS);
        }
        /* No left edge so attempt to add one. */
        if (leftEdge.isEmpty() && !lowerEdge.isEmpty()
                && !upperEdge.isEmpty()) {
            Coordinate[] removedBounds = new Coordinate[5];
            removedBounds[0] = new Coordinate(UL[0], UL[1]);
            removedBounds[1] = new Coordinate(LL[0], LL[1]);

            Coordinate right = upperEdge.get(0);
            UL = new double[] { right.x, right.y };
            targetCRSToSourceCRS.transform(UL, 0, UL, 0, 1);

            right = lowerEdge.get(lowerEdge.size() - 1);
            LL = new double[] { right.x, right.y };
            targetCRSToSourceCRS.transform(LL, 0, LL, 0, 1);

            removedBounds[2] = new Coordinate(LL[0], LL[1]);
            removedBounds[3] = new Coordinate(UL[0], UL[1]);
            removedBounds[4] = removedBounds[0];
            double removedArea = gf.createPolygon(removedBounds).getArea();
            if (removedArea > maximumRemovalArea) {
                return null;
            }
            leftEdge = calculateEdge(LL, UL, maxVertDivisions, threshold,
                    sourceCRSToTargetCRS);
        }
        if (upperEdge.isEmpty() || lowerEdge.isEmpty() || rightEdge.isEmpty()
                || leftEdge.isEmpty()) {
            return null;
        }

        List<Coordinate> borderPoints = new ArrayList<>(
                maxVertDivisions * 2 + maxHorDivisions * 2);

        borderPoints.addAll(upperEdge.subList(0, upperEdge.size() - 1));
        Coordinate upper = upperEdge.get(upperEdge.size() - 1);
        Coordinate right = rightEdge.get(0);
        /* Missing Upper Right corner. */
        if (!upper.equals(right)) {
            double[] u = new double[] { upper.x, upper.y };
            double[] r = new double[] { right.x, right.y };
            targetCRSToSourceCRS.transform(u, 0, u, 0, 1);
            targetCRSToSourceCRS.transform(r, 0, r, 0, 1);

            double removedArea = Triangle.area(new Coordinate(UR[0], UR[1]),
                    new Coordinate(u[0], u[1]), new Coordinate(r[0], r[1]));
            if (removedArea > maximumRemovalArea) {
                return null;
            }

            List<Coordinate> diagnol = calculateEdge(u, r, maxDiagnolDivisions,
                    threshold, sourceCRSToTargetCRS);
            borderPoints.addAll(diagnol.subList(0, diagnol.size() - 1));
        }

        borderPoints.addAll(rightEdge.subList(0, rightEdge.size() - 1));
        right = rightEdge.get(rightEdge.size() - 1);
        Coordinate lower = lowerEdge.get(0);
        /* Missing Lower Right corner. */
        if (!right.equals(lower)) {
            double[] r = new double[] { right.x, right.y };
            double[] l = new double[] { lower.x, lower.y };
            targetCRSToSourceCRS.transform(r, 0, r, 0, 1);
            targetCRSToSourceCRS.transform(l, 0, l, 0, 1);

            double removedArea = Triangle.area(new Coordinate(LR[0], LR[1]),
                    new Coordinate(l[0], l[1]), new Coordinate(r[0], r[1]));
            if (removedArea > maximumRemovalArea) {
                return null;
            }

            List<Coordinate> diagnol = calculateEdge(r, l, maxDiagnolDivisions,
                    threshold, sourceCRSToTargetCRS);
            borderPoints.addAll(diagnol.subList(0, diagnol.size() - 1));
        }

        borderPoints.addAll(lowerEdge.subList(0, lowerEdge.size() - 1));
        lower = lowerEdge.get(lowerEdge.size() - 1);
        Coordinate left = leftEdge.get(0);
        /* Missing Lower Left corner. */
        if (!lower.equals(left)) {
            double[] l1 = new double[] { lower.x, lower.y };
            double[] l2 = new double[] { left.x, left.y };
            targetCRSToSourceCRS.transform(l1, 0, l1, 0, 1);
            targetCRSToSourceCRS.transform(l2, 0, l2, 0, 1);

            double removedArea = Triangle.area(new Coordinate(LL[0], LL[1]),
                    new Coordinate(l1[0], l1[1]), new Coordinate(l2[0], l2[1]));
            if (removedArea > maximumRemovalArea) {
                return null;
            }

            List<Coordinate> diagnol = calculateEdge(l1, l2,
                    maxDiagnolDivisions, threshold, sourceCRSToTargetCRS);
            borderPoints.addAll(diagnol.subList(0, diagnol.size() - 1));
        }

        borderPoints.addAll(leftEdge.subList(0, leftEdge.size() - 1));
        left = leftEdge.get(leftEdge.size() - 1);
        upper = upperEdge.get(0);
        /* Missing Upper Left corner. */
        if (!left.equals(upper)) {
            double[] l = new double[] { left.x, left.y };
            double[] u = new double[] { upper.x, upper.y };
            targetCRSToSourceCRS.transform(l, 0, l, 0, 1);
            targetCRSToSourceCRS.transform(u, 0, u, 0, 1);

            double removedArea = Triangle.area(new Coordinate(UL[0], UL[1]),
                    new Coordinate(l[0], l[1]), new Coordinate(u[0], u[1]));
            if (removedArea > maximumRemovalArea) {
                return null;
            }

            List<Coordinate> diagnol = calculateEdge(l, u, maxDiagnolDivisions,
                    threshold, sourceCRSToTargetCRS);
            borderPoints.addAll(diagnol);
        } else {
            borderPoints.add(left);
        }

        return borderPoints;
    }

    /**
     * Transform all the points needed(up to maxNumDivs) to get an accurate
     * representation of a transformed line segment from point1 to point2. An
     * accurate representation is one where the transformed line is within
     * threshold distance of a 'perfect' representation of the line. This method
     * is necessary because for many projections a straight line can be
     * converted to a curved line in a different projection. The result of this
     * method will be a set of points that follow the curve as accurately as
     * possible.
     * 
     * In addition to transforming the line, if the endpoints are not valid in
     * the target projection then they will be removed so that the endpoints of
     * the result are guaranteed to be valid(but are not guaranteed to be the
     * same as the points passed in). As a result, if there are no valid points
     * on the line in the target projection this will return an empty list.
     * Invalid points within the line are left in with the ordinates set to NaN.
     * 
     * This method is implemented as a recursive binary search. The midpoint of
     * the line is transformed and if it is within threshold then the line is
     * assumed to be straight and the algorithm is done. If however the midpoint
     * is outside threshold it is used as the endpoint of a shorter line and the
     * algorithm is repeated for each of the two line segments. After the search
     * has completed the invalid endpoints are removed.
     * 
     * This method sets up the inputs to the binary search and hands it off to
     * {@link #calculateEdge(List, double[], double[], double[], double[], int, double, MathTransform)}
     * to perform the recursion.
     */
    private static List<Coordinate> calculateEdge(double[] point1,
            double[] point2, int maxNumDivs, double threshold,
            MathTransform transform) {
        List<Coordinate> borderPoints = new ArrayList<>(maxNumDivs);

        double[] transformedPoint1 = transform(transform, point1);
        double[] transformedPoint2 = transform(transform, point2);

        borderPoints.add(
                new Coordinate(transformedPoint1[0], transformedPoint1[1]));
        /*
         * Minimum of 2 points are needed to ensure that very large envelopes
         * with no distortion still get the edge divided because the world wrap
         * corrector cannot handle a single line that spans more than 180°
         */
        calculateEdge(borderPoints, point1, transformedPoint1, point2,
                transformedPoint2, 2, maxNumDivs, threshold, transform);
        borderPoints.add(
                new Coordinate(transformedPoint2[0], transformedPoint2[1]));

        Coordinate c = borderPoints.get(borderPoints.size() - 1);
        while (Double.isNaN(c.x) || Double.isNaN(c.y)) {
            borderPoints.remove(borderPoints.size() - 1);
            if (borderPoints.isEmpty()) {
                break;
            }
            c = borderPoints.get(borderPoints.size() - 1);
        }

        if (!borderPoints.isEmpty()) {
            c = borderPoints.get(0);
            while (Double.isNaN(c.x) || Double.isNaN(c.y)) {
                borderPoints.remove(0);
                if (borderPoints.isEmpty()) {
                    break;
                }
                c = borderPoints.get(0);
            }
        }

        return borderPoints;
    }

    /**
     * @see {@link #calculateEdge(double[], double[], int, double, MathTransform)}
     *      for more information, this is the recursive component of the edge
     *      finding algorithm.
     */
    private static void calculateEdge(List<Coordinate> borderList,
            double[] point1, double[] transformedPoint1, double[] point3,
            double[] transformedPoint3, int minNumDivs, int maxNumDivs,
            double threshold, MathTransform transform) {
        if (transformedPoint1 == null) {
            transformedPoint1 = transform(transform, point1);
        }
        if (transformedPoint3 == null) {
            transformedPoint3 = transform(transform, point3);
        }

        double[] point2 = { (point1[0] + point3[0]) / 2,
                (point1[1] + point3[1]) / 2 };
        double[] transformedPoint2 = transform(transform, point2);
        double[] interp2 = { (transformedPoint1[0] + transformedPoint3[0]) / 2,
                (transformedPoint1[1] + transformedPoint3[1]) / 2 };
        double dX = transformedPoint2[0] - interp2[0];
        double dY = transformedPoint2[1] - interp2[1];
        double d = Math.hypot(dX, dY);
        if (minNumDivs > 1 || (d >= threshold && maxNumDivs >= 1)) {
            calculateEdge(borderList, point1, transformedPoint1, point2,
                    transformedPoint2, minNumDivs / 2, maxNumDivs / 2,
                    threshold, transform);
            borderList.add(
                    new Coordinate(transformedPoint2[0], transformedPoint2[1]));
            calculateEdge(borderList, point2, transformedPoint2, point3,
                    transformedPoint3, minNumDivs / 2, maxNumDivs / 2,
                    threshold, transform);
        }
    }

    /**
     * Find the normal range of x coordinates for a crs if it is easy to handle
     * x coordinates that are outside the normal range. The normal longitude
     * range for a projection is ±180° from the central meridian. For
     * cylindrical projections this longitude range maps linearly onto the x
     * coordinate in the CRS so it is easy to handle out of range values by just
     * shifting envelopes and geometries in the x direction.
     * 
     * This method returns the minimum and maximum crs x coordinates for the
     * normal range. The difference between these two values is the width of the
     * CRS, which should be used to shift things into or out of the normal
     * range.
     * 
     * This method usually returns {-2E7, 2E7}, since that is the circumference
     * of the earth in meters which is the spacing of most cylindrical
     * projections.
     * 
     * @param crs
     *            a coordinate reference system to check.
     * @return An array of length 2 containing a minimum and maximum value, or
     *         null if it isn't valid to shift things around in the provided
     *         crs.
     * @throws TransformException
     * @throws FactoryException
     */
    private static double[] findNormalXRange(CoordinateReferenceSystem crs)
            throws TransformException, FactoryException {
        MapProjection projection = CRS.getMapProjection(crs);
        if (!(projection instanceof EquidistantCylindrical)) {
            /*
             * May be valid for other cylindrical projections, equidistant
             * cylindrical is just the only one necessary.
             */
            return null;
        }
        double centralMeridian = 0.0;
        try {
            ParameterValueGroup group = projection.getParameterValues();
            centralMeridian = group.parameter(
                    AbstractProvider.CENTRAL_MERIDIAN.getName().getCode())
                    .doubleValue();
        } catch (ParameterNotFoundException e) {
            return null;
        }
        if (centralMeridian != 0) {
            /*
             * GeoTools is very aggressive about bringing all points within
             * range when the central meridian is not 0 so it is unlikely
             * anybody would provide envelopes out of range. In this code the
             * problem that we run into is that when highLon and lowLon are
             * transformed into the CRS they technically represent the same
             * point so you can get an empty range.
             */
            return null;
        }
        double lowLon = centralMeridian - 180;
        double highLon = centralMeridian + 180;
        double[] test = { lowLon, 0, highLon, 0 };
        MathTransform fromLatLon = CRS
                .findMathTransform(DefaultGeographicCRS.WGS84, crs);
        fromLatLon.transform(test, 0, test, 0, 2);
        return new double[] { test[0], test[2] };
    }

    /**
     * Shift an intersecting border outside of the normal projection range if
     * the target envelope was outside the normal range.
     * 
     * For example, with a central meridian of 0, the normal range is from -180°
     * to 180°. If the target envelope is defined from 170° to 190° then the
     * intersection may actually be found in the normal range on both sides,
     * from -180° to -170° and from 170° to 180°). The caller is expecting the
     * result to fall in the same range as the target envelope so this method
     * will copy the border and shift it outside the normal range to align it
     * with the original target envelope.
     * 
     * @param targetEnvelope
     *            The target envelope of the projection
     * @param targetBorder
     *            A simple poltgon representation of targetEnvelope.
     * @param border
     *            The intersection found in createEnvelopeIntersection.
     * @return a new border found by shifting border, or the original border if
     *         that is not necessary.
     * @throws TransformException
     * @throws FactoryException
     */
    private static Geometry handleAbnormalTarget(
            ReferencedEnvelope targetEnvelope, Geometry targetBorder,
            Geometry border) throws TransformException, FactoryException {
        double[] range = findNormalXRange(
                targetEnvelope.getCoordinateReferenceSystem());
        if (range == null) {
            return border;
        }
        List<Geometry> expandedBorders = new ArrayList<>(3);
        double lowX = range[0];
        double highX = range[1];
        double width = highX - lowX;
        if (targetEnvelope.getMaxX() > highX) {
            Geometry expanded = (Geometry) border.clone();

            for (Coordinate c : expanded.getCoordinates()) {
                c.x += width;
            }
            expanded.geometryChanged();
            expanded = expanded.intersection(targetBorder);
            expandedBorders.add(expanded);
        }
        if (targetEnvelope.getMinX() < lowX) {
            Geometry expanded = (Geometry) border.clone();
            for (Coordinate c : expanded.getCoordinates()) {
                c.x -= width;
            }
            expanded.geometryChanged();
            expanded = expanded.intersection(targetBorder);
            expandedBorders.add(expanded);
        }
        if (!expandedBorders.isEmpty()) {
            border = border.intersection(targetBorder);
            expandedBorders.add(border);
            border = gf.createGeometryCollection(
                    expandedBorders.toArray(new Geometry[0]));
        }
        return border;
    }

    /**
     * Deconstruct envelopes that are outside the normal range. For normal
     * envelopes or envelopes that can't be easily deconstructed the original
     * envelope is returned.
     * 
     * For example, with a central meridian of 0, the normal range is from -180°
     * to 180°. An envelope may be defined from 170° to 190°, logically this is
     * the exact same envelope as -170° to -190°. In this case the envelope can
     * be expressed entirely within the normal range by splitting the envelope
     * in half and putting each half on the side where it is within the normal
     * range. So the result is two envelopes, one from -180° to -170° and from
     * 170° to 180°.
     * 
     * Another use case is an envelope that goes from 0° to 361°. In this case
     * the 1° overlap is redundant and it is better to use a normal envelope
     * covering the whole normal range so an envelope from -180° to 180° is
     * returned.
     * 
     * Although the examples express everything in degrees, the real ranges are
     * expressed in the native units of the CRS.
     * 
     * @param envelope
     *            the envelope to normalize
     * @return An array of length one or two containing normalized envelope(s)
     *         or the original.
     * @throws TransformException
     * @throws FactoryException
     */
    private static ReferencedEnvelope[] normalizeEnvelope(
            ReferencedEnvelope envelope)
            throws TransformException, FactoryException {
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        double[] range = findNormalXRange(crs);
        if (range == null) {
            return new ReferencedEnvelope[] { envelope };
        }
        double lowX = range[0];
        double highX = range[1];

        double minX = envelope.getMinimum(0);
        double maxX = envelope.getMaximum(0);

        boolean extraLow = minX < lowX;
        boolean extraHigh = maxX > highX;

        if (!extraLow || !extraHigh) {
            return new ReferencedEnvelope[] { envelope };
        }

        double minY = envelope.getMinimum(1);
        double maxY = envelope.getMaximum(1);
        double width = lowX - highX;
        if (envelope.getSpan(0) > width) {
            ReferencedEnvelope result = new ReferencedEnvelope(lowX, highX,
                    minY, maxY, crs);
            return new ReferencedEnvelope[] { result };
        }
        ReferencedEnvelope extra = null;
        if (extraLow) {
            extra = new ReferencedEnvelope(minX, lowX, minY, maxY, crs);
            extra.translate(width, 0);
            minX = lowX;
        } else if (extraHigh) {
            extra = new ReferencedEnvelope(maxX, highX, minY, maxY, crs);
            extra.translate(-width, 0);
            maxX = highX;
        }
        ReferencedEnvelope remainder = new ReferencedEnvelope(minX, maxX, minY,
                maxY, crs);

        return new ReferencedEnvelope[] { extra, remainder };
    }

    /**
     * Transform method which converts errors into NaN. point is assumed to be a
     * single point that needs to be transformed(so point.size should be 2).
     */
    private static double[] transform(MathTransform transform, double[] point) {
        double[] transformedPoint = new double[point.length];
        try {
            transform.transform(point, 0, transformedPoint, 0, 1);
        } catch (TransformException e) {
            Arrays.fill(transformedPoint, Double.NaN);
        }
        return transformedPoint;
    }

    private static ReferencedEnvelope reference(Envelope envelope) {
        if (envelope instanceof ReferencedEnvelope) {
            return (ReferencedEnvelope) envelope;
        }
        return new ReferencedEnvelope(envelope);
    }

    /**
     * Check if an envelope is empty. The java doc for
     * {@link ReferencedEnvelope#isEmpty()} claims it will return true when all
     * lengths are empty however for the case where the min and max ordinates
     * are equal it does not return true even though the length is 0. We
     * specifically want to treat envelopes with identical min/max ordinates as
     * empty.
     * 
     */
    private static boolean isEmpty(ReferencedEnvelope envelope) {
        return envelope.isEmpty()
                || (envelope.getSpan(0) == 0 && envelope.getSpan(1) == 0);
    }

    private static Geometry toPolygonal(Geometry geometry) {
        if (!(geometry instanceof Polygon)) {
            List<Polygon> polygons = new ArrayList<>(
                    geometry.getNumGeometries());
            buildPolygonList(polygons, geometry);
            if (polygons.size() == 1) {
                geometry = polygons.get(0);
            } else {
                geometry = gf
                        .createMultiPolygon(polygons.toArray(new Polygon[0]));
            }
        }
        return geometry;
    }

    private static void buildPolygonList(List<Polygon> polygons,
            Geometry geometry) {
        if (geometry instanceof Polygon) {
            polygons.add((Polygon) geometry);
        } else {
            for (int n = 0; n < geometry.getNumGeometries(); ++n) {
                buildPolygonList(polygons, geometry.getGeometryN(n));
            }
        }
    }

    /**
     * Return true if two coordinates are very very close to eachother(within 4
     * ULP). This is necessary when the world wrap corrector introduces very
     * small rounding errors during the correction process.
     * 
     * @see Math#ulp(double)
     */
    private static boolean approximateEquals(Coordinate c1, Coordinate c2) {
        return approximateEquals(c1.x, c2.x) && approximateEquals(c1.y, c2.y);
    }

    /**
     * Return true if two doubles are very very close to eachother(within 4
     * ULP).
     * 
     * @see #approximateEquals(Coordinate, Coordinate)
     */
    private static boolean approximateEquals(double d1, double d2) {
        double test = d1;
        for (int i = 0; i < 4; i += 1) {
            if (test == d2) {
                return true;
            }
            test = Math.nextAfter(test, d2);
        }
        return false;
    }
}
