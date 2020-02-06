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
import java.util.BitSet;
import java.util.List;

import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.impl.PackedCoordinateSequence;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.raytheon.uf.common.geospatial.MapUtil;

/**
 * Solves the problem of intersecting two envelopes in different coordinate
 * systems by reprojecting a grid of points and building polygons out of
 * adjacent grid cells. This can be significantly slower than other algorithms
 * but will always produce the best result possible for a given width and
 * height. Increasing the width/height will slow the algorithm but give better
 * results.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Nov 14, 2013  2528     bsteffen    Initial creation
 * Jan 29, 2015  3939     bsteffen    Add cross consistency checks.
 * May 27, 2015  4472     bsteffen    Ignore exceptions when building target
 *                                    coordinates.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
class BruteForceEnvelopeIntersection {

    public static final double PI_TIMES_2 = 2.0 * Math.PI;

    private final ReferencedEnvelope sourceEnvelope;

    private final ReferencedEnvelope targetEnvelope;

    private final WorldWrapChecker wwc;

    private final int width;

    private final int height;

    private final MathTransform sourceCRSToLatLon;

    private final MathTransform latLonToTargetCRS;

    private final CoordinateSequence latLonCoords;

    private final CoordinateSequence targetCoords;

    /**
     * Construct a new intersection. This will calculate all fields including
     * reprojecting all points in the grid.
     */
    private BruteForceEnvelopeIntersection(Envelope sourceEnvelope,
            Envelope targetEnvelope, int width, int height)
            throws FactoryException, TransformException {
        if (sourceEnvelope instanceof ReferencedEnvelope) {
            this.sourceEnvelope = (ReferencedEnvelope) sourceEnvelope;
        } else {
            this.sourceEnvelope = new ReferencedEnvelope(sourceEnvelope);
        }
        if (targetEnvelope instanceof ReferencedEnvelope) {
            this.targetEnvelope = (ReferencedEnvelope) targetEnvelope;
        } else {
            this.targetEnvelope = new ReferencedEnvelope(targetEnvelope);
        }
        this.width = width;
        this.height = height;
        wwc = new WorldWrapChecker(this.targetEnvelope);

        sourceCRSToLatLon = MapUtil.getTransformToLatLon(sourceEnvelope
                .getCoordinateReferenceSystem());
        latLonToTargetCRS = MapUtil.getTransformFromLatLon(targetEnvelope
                .getCoordinateReferenceSystem());

        double[] latLonCoords = buildLatLonCoords();
        this.latLonCoords = new PackedCoordinateSequence.Double(latLonCoords, 2, 0);

        double[] targetCoords = buildTargetCoords(latLonCoords);
        this.targetCoords = new PackedCoordinateSequence.Double(targetCoords, 2, 0);
    }

    /**
     * Construct a grid of coordinates and reproject to lat/lon CRS.
     */
    private double[] buildLatLonCoords() throws TransformException {
        double worldMinX = this.sourceEnvelope.getMinX();
        double worldMinY = this.sourceEnvelope.getMinY();
        double dXWorld = this.sourceEnvelope.getWidth() / (width - 1);
        double dYWorld = this.sourceEnvelope.getHeight() / (height - 1);

        int index = 0;
        double[] coordinates = new double[width * height * 2];

        for (int j = 0; j < height; ++j) {
            double y = worldMinY + j * dYWorld;
            for (int i = 0; i < width; ++i) {
                coordinates[index++] = worldMinX + i * dXWorld;
                coordinates[index++] = y;
            }
        }
        sourceCRSToLatLon.transform(coordinates, 0, coordinates, 0, width
                * height);
        return coordinates;
    }

    /**
     * Bulk conversion method which can ignore errors.
     */
    private double[] buildTargetCoords(double[] latLonCoords) {
        double[] targetCoords = new double[latLonCoords.length];
        try {
            latLonToTargetCRS.transform(latLonCoords, 0, targetCoords, 0, width
                    * height);
        } catch (TransformException e) {
            /*
             * Don't care about exceptions just ensure unconvertible points are
             * converted to NaN.
             */
            if (e.getLastCompletedTransform() != latLonToTargetCRS) {
                for (int i = 0; i < targetCoords.length; i += 2) {
                    try {
                        latLonToTargetCRS.transform(latLonCoords, i,
                                targetCoords, i, 1);
                    } catch (TransformException e1) {
                        targetCoords[i] = targetCoords[i + 1] = Double.NaN;
                    }
                }
            }
        }
        return targetCoords;
    }

    /**
     * Perform the actual intersection operation by merging all {@link Cell}s
     * into {@link SimplePolygon} and finally converting those into Polygons.
     */
    public Geometry reproject() throws TransformException {
        /*
         * Loop over each cell and split into two groups. First group is the
         * "simple" cases that do not world wrap and can all be merged into one
         * or more polygons. Second group contains the cells that need to be
         * world wrap corrected.
         */
        List<SimplePolygon> simpleCases = new ArrayList<SimplePolygon>();
        List<Cell> worldWrapCells = new ArrayList<Cell>();
        /*
         * Track the consistency of every row as we go so we don't have to
         * recaluclate the previous row on every iteration.
         */
        BitSet upperCrossConsistencySet = new BitSet(width);
        upperCrossConsistencySet.set(0, width);
        for (int j = 1; j < height; ++j) {
            boolean leftCrossConsistency = true;
            for (int i = 1; i < width; ++i) {
                boolean lowerRightCrossConsistency = checkCrossConsistency(i, j);
                boolean upperRightCrossConsistency = upperCrossConsistencySet
                        .get(i);
                boolean rightCrossConsistency = lowerRightCrossConsistency
                        & upperRightCrossConsistency;
                boolean crossConsistency = leftCrossConsistency
                        & rightCrossConsistency;
                leftCrossConsistency = rightCrossConsistency;
                upperCrossConsistencySet.set(i, lowerRightCrossConsistency);

                Cell cell = new Cell(i, j);
                if (!cell.isValid()) {
                    continue;
                } else if (cell.worldWrapCheck(wwc)) {
                    worldWrapCells.add(cell);
                } else if (crossConsistency
                        || cell.checkCenterConsistency(i, j)) {
                    for (SimplePolygon poly : simpleCases) {
                        if (poly.merge(cell)) {
                            cell = null;
                            break;
                        }
                    }
                    if (cell != null) {
                        simpleCases.add(new SimplePolygon(cell));
                    }
                }
            }
        }
        /* Convert all simple case polygons into JTS polygons */
        GeometryFactory gf = new GeometryFactory();
        List<Geometry> geoms = new ArrayList<Geometry>(simpleCases.size()
                + worldWrapCells.size() * 2);
        for (SimplePolygon poly : simpleCases) {
            Geometry g = poly.asGeometry(gf);
            geoms.add(g);
        }
        if(!worldWrapCells.isEmpty()){
            /*
             * World wrap correct the cells that need it and transform the
             * corrected geometries.
             */
            WorldWrapCorrector corrector = new WorldWrapCorrector(
                    targetEnvelope);
            for (Cell cell : worldWrapCells) {
                Geometry geom = cell.asLatLonGeometry(gf);
                geom = corrector.correct(geom);
                geom = JTS.transform(geom, latLonToTargetCRS);
                if (geom.isValid() && !geom.isEmpty()) {
                    geoms.add(geom);
                }
            }
        }
        return gf.createGeometryCollection(geoms.toArray(new Geometry[0]))
                .buffer(0);
    }

    /**
     * This method tries to detect a certain type of inconsistency in a
     * reprojection. To help explain this method here is a picture of some of
     * the cells which are used during reprojection:
     * 
     * <pre>
     * A---B---C
     * |   |   |
     * D---E---F
     * |   |   |
     * G---H---I
     * </pre>
     * 
     * Each letter(A-I) in the picture represents a coordinate that has been
     * reprojected, 4 coordinates make up the corners of a cell so this is 9
     * coordinates making 4 cells. This method is dealing specifically with 5
     * coordinates. Coordinate E is at the center of 'cross' and the 'arms' are
     * made up of coordinates BFHD.
     * 
     * Reprojection causes all sorts of normal distortion in size and shape of
     * the cells but if the arms of the cross are out of order then something
     * has gone terribly wrong and that is what this method attempts to detect.
     * This happens when a cell is directly over an inconsistency in the target
     * projection that did not exist in the source projection, for example the
     * inverse central meridian of a equidistant cylindrical projection(World
     * Wrapping) or the south pole in a north polar stereographic.
     * 
     * Here is a simple picture showing the type of distortion this method
     * detects:
     * 
     * <pre>
     *     G---H---I
     *    /   /   /
     * A-/-B-/-C /
     * |/  |/  |/
     * D---E---F
     * </pre>
     * 
     * If you start at coordinate B and list the coordinates of the cross
     * clockwise you get BHFD which is out of order meaning the reprojection is
     * invalid and one or more cells needs to be removed or corrected. This
     * method does not tell specifically which cells have problems. More tests
     * must be done to decide which cells and then those cells must be corrected
     * or removed. The picture shows that some of the cells are overlapping, map
     * projections should not cause this overlap. Any time this method reports
     * inconsistency, the visualization of the reprojection will show a similar
     * overlap.
     * 
     * In cases where this method cannot check for consistency it returns true
     * because no inconsistency could be detected(even though there might be
     * one). This happens if some coordinates are invalid or if the provided
     * index is on the edge of the grid. If the order of the arms is reversed,
     * true will be returned because it is legal for a map projection to change
     * directions.
     * 
     * There are two major cases where this method fails to find inconsistency.
     * <ol>
     * <li>If a distortion intersects the cross in such a way that two arms are
     * displaced then the ordering is reversed and it is not treated as an
     * inconsistency.
     * <li>Cells on the edge or near unreprojectable points cannot be verified
     * because there is not enough points to create the 'cross'.</li>
     * </ol>
     * 
     * Any time these two cases have occurred in real reprojections, the
     * {@link WorldWrapChecker} is able to detect it so it is not considered
     * worthwhile to add more consistency checks to try to validate these two
     * cases.
     * 
     * @param x
     *            the x index of the coordinate at the center of the 'cross'
     * @param y
     *            the y index of the coordinate at the center of the 'cross'
     * @return false if an inconstency is detected, true if no inconsistency is
     *         detected(This does not guarantee consistency)
     */
    private boolean checkCrossConsistency(int x, int y) {
        if (x < 1 || y < 1 || x >= width - 1 || y >= height - 1) {
            return true;
        }
        int centerindex = y * width + x;
        double centerx = targetCoords.getOrdinate(centerindex, 0);
        double centery = targetCoords.getOrdinate(centerindex, 1);

        /*
         * Calculate the angle from the center to each of the 4 vertices that
         * should define a plane.
         */
        int upindex = (y - 1) * width + x;
        double dx = targetCoords.getOrdinate(upindex, 0) - centerx;
        double dy = targetCoords.getOrdinate(upindex, 1) - centery;
        double angle_up = Math.atan2(dy, dx);

        int downindex = (y + 1) * width + x;
        dx = targetCoords.getOrdinate(downindex, 0) - centerx;
        dy = targetCoords.getOrdinate(downindex, 1) - centery;
        double angle_down = Math.atan2(dy, dx);

        int leftindex = y * width + x - 1;
        dx = targetCoords.getOrdinate(leftindex, 0) - centerx;
        dy = targetCoords.getOrdinate(leftindex, 1) - centery;
        double angle_left = Math.atan2(dy, dx);

        int rightindex = y * width + x + 1;
        dx = targetCoords.getOrdinate(rightindex, 0) - centerx;
        dy = targetCoords.getOrdinate(rightindex, 1) - centery;
        double angle_right = Math.atan2(dy, dx);

        /*
         * Calculate the rotation from each vertex to the next, forcing it
         * positive makes it a clockwise rotation.
         */
        double a1 = angle_up - angle_left;
        a1 = a1 >= 0 ? a1 : a1 + PI_TIMES_2;
        double a2 = angle_left - angle_down;
        a2 = a2 >= 0 ? a2 : a2 + PI_TIMES_2;
        double a3 = angle_down - angle_right;
        a3 = a3 >= 0 ? a3 : a3 + PI_TIMES_2;
        double a4 = angle_right - angle_up;
        a4 = a4 >= 0 ? a4 : a4 + PI_TIMES_2;

        double a = a1 + a2 + a3 + a4;

        /*
         * Theoretically the round is only necessary to handle any floating
         * point inaccuracies, the sum of the angles *should* be evenly
         * divisible by 2π
         */
        long revolutions = Math.round(a / PI_TIMES_2);

        /*
         * If the vertices are in order clockwise than revolutions will be 1. If
         * its counter clockwise then revolutions will be 3. If the vertices are
         * out of order it will be 2. Any other number should be impossible.
         */
        if (Double.isNaN(a) || revolutions % 2 == 1) {
            return true;
        }
        return false;
    }

    /**
     * A cell represents four connected grid points that form a continous piece
     * of an intersecting polygons. Most of the time a cell represents a
     * quadrilateral but for cases where one of the corners is invalid in the
     * target CRS a cell may represent a triangle. If any more points are
     * invalid then the cell is invalid.
     */
    private final class Cell {

        private final int[] indices;

        public Cell(int x, int y) {
            int[] indices = new int[] { y * width + x - 1,
                    (y - 1) * width + x - 1,
                    (y - 1) * width + x, y * width + x};
            int nanCount = 0;
            for (int index : indices) {
                if (isNaN(index)) {
                    nanCount += 1;
                }
            }
            if (nanCount == 0) {
                this.indices = indices;
            } else if (nanCount == 1) {
                this.indices = new int[3];
                int i = 0;
                for (int index : indices) {
                    if (!isNaN(index)) {
                        this.indices[i++] = index;
                    }
                }
            } else {
                this.indices = null;
            }
        }

        private boolean isNaN(int index){
            return Double.isNaN(targetCoords.getOrdinate(index, 0))
                    || Double.isNaN(targetCoords.getOrdinate(index, 1));
        }

        public boolean isValid() {
            return indices != null;
        }

        public boolean worldWrapCheck(WorldWrapChecker wwc) {
            double prevLon = latLonCoords.getOrdinate(
                    indices[indices.length - 1], 0);
            for (int index : indices) {
                double lon = latLonCoords.getOrdinate(index, 0);
                if (wwc.check(prevLon, lon)) {
                    return true;
                }
                prevLon = lon;
            }
            return false;

        }
        
        /**
         * Reprojects the center point of this cell from the source CRS to the
         * target CRS. If the center in targetCRS is not within the border of
         * the cell then something is wrong with the reprojection and this cell
         * should not be used.
         * 
         * @return true if the center point reprojection indicates the cell is
         *         valid, false otherwise.
         */
        public boolean checkCenterConsistency(int i, int j)
                throws TransformException {
            double worldMinX = sourceEnvelope.getMinX();
            double worldMinY = sourceEnvelope.getMinY();
            double dXWorld = sourceEnvelope.getWidth() / (width - 1);
            double dYWorld = sourceEnvelope.getHeight() / (height - 1);

            double y = worldMinY + (j - 0.5) * dYWorld;
            double x = worldMinX + (i - 0.5) * dXWorld;

            DirectPosition2D centerPoint = new DirectPosition2D(x, y);
            sourceCRSToLatLon.transform(centerPoint, centerPoint);
            latLonToTargetCRS.transform(centerPoint, centerPoint);
            GeometryFactory gf = new GeometryFactory();
            if (this.asTargetGeometry(gf)
                    .contains(
                            gf.createPoint(new Coordinate(centerPoint.x,
                                    centerPoint.y)))) {
                return true;
            } else {
                return false;
            }
        }

        public List<Coordinate> getTargetCoords() {
            List<Coordinate> result = new ArrayList<Coordinate>(4);
            for (int index : indices) {
                result.add(targetCoords.getCoordinate(index));
            }
            return result;
        }

        public Polygon asLatLonGeometry(GeometryFactory gf) {
            Coordinate[] coordinates = new Coordinate[indices.length + 1];
            for (int i = 0; i < indices.length; i += 1) {
                coordinates[i] = latLonCoords.getCoordinate(indices[i]);
            }
            coordinates[coordinates.length - 1] = coordinates[0];
            return gf.createPolygon(gf.createLinearRing(coordinates), null);
        }

        public Polygon asTargetGeometry(GeometryFactory gf) {
            Coordinate[] coordinates = new Coordinate[indices.length + 1];
            for (int i = 0; i < indices.length; i += 1) {
                coordinates[i] = targetCoords.getCoordinate(indices[i]);
            }
            coordinates[coordinates.length - 1] = coordinates[0];
            return gf.createPolygon(gf.createLinearRing(coordinates), null);
        }

        public Coordinate getTargetCoord(int index) {
            return targetCoords.getCoordinate(indices[index % indices.length]);
        }

        public int indexOfTarget(Coordinate c) {
            for (int i = 0; i < targetCoords.size(); i += 1) {
                if (targetCoords.getOrdinate(indices[i], 0) == c.x
                        && targetCoords.getOrdinate(indices[i], 1) == c.y) {
                    return i;
                }
            }
            return -1;
        }

        public int size() {
            return indices.length;
        }
    }

    /**
     * This class is used to represent a Polygon with no holes. Additionally it
     * provides fast method for merging a cell because it can safely assume that
     * any points it shares with the cell are identical.
     */
    private static final class SimplePolygon {

        private List<Coordinate> coords;

        public SimplePolygon(Cell cell) {
            this.coords = cell.getTargetCoords();
        }

        public boolean merge(Cell cell) {
            List<Coordinate> toCheck = cell.getTargetCoords();
            /*
             * Walk coords in order, if any identical coords are found we can
             * ceck nearby points to eliminate the duplicates
             */
            for (int i = 0; i < coords.size() - 1; i += 1) {
                if (toCheck.remove(coords.get(i))) {
                    int lastFoundIndex = -1;
                    if (i == 0 && toCheck.remove(coords.get(coords.size() - 1))) {
                        /*
                         * For the 0 index the end of coords must be checked,
                         * all other indices do not need to check previous
                         * coords.
                         */
                        while (toCheck.remove(coords.get(coords.size() - 2))) {
                            coords.remove(coords.size() - 1);
                        }
                        lastFoundIndex = 0;
                    } else if (i + 1 < coords.size()
                            && toCheck.remove(coords.get(i + 1))) {
                        /* This check ensures 2 points match. */
                        lastFoundIndex = i + 1;
                    }
                    if (lastFoundIndex != -1) {
                        while (lastFoundIndex + 1 < coords.size()
                                && toCheck.remove(coords
                                        .get(lastFoundIndex + 1))) {
                            /*
                             * If more than two points match, remove the common
                             * interior points.
                             */
                            coords.remove(lastFoundIndex);
                        }
                        if (!toCheck.isEmpty()) {
                            /*
                             * Add any exterior remaining points from the cell
                             * into this.
                             */
                            int cellLastFoundIndex = cell.indexOfTarget(coords
                                    .get(lastFoundIndex));
                            int prevIndex = cellLastFoundIndex + cell.size() - 1;
                            int nextIndex = cellLastFoundIndex + 1;
                            if (toCheck
                                    .contains(cell.getTargetCoord(nextIndex))) {
                                coords.add(lastFoundIndex,
                                        cell.getTargetCoord(nextIndex));
                                for (int j = nextIndex + 1; j < prevIndex; j += 1) {
                                    Coordinate c = cell.getTargetCoord(j);
                                    if (toCheck.contains(c)) {
                                        coords.add(lastFoundIndex, c);
                                    } else {
                                        break;
                                    }
                                }
                            } else if (toCheck.contains(cell
                                    .getTargetCoord(prevIndex))) {
                                coords.add(lastFoundIndex,
                                        cell.getTargetCoord(prevIndex));
                                for (int j = prevIndex - 1; j > nextIndex; j -= 1) {
                                    Coordinate c = cell.getTargetCoord(j);
                                    if (toCheck.contains(c)) {
                                        coords.add(lastFoundIndex, c);
                                    } else {
                                        break;
                                    }
                                }
                            }
                        }
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }

        public Polygon asGeometry(GeometryFactory gf) {
            Coordinate[] coordinates = new Coordinate[this.coords.size() + 1];
            this.coords.toArray(coordinates);
            coordinates[coordinates.length - 1] = coordinates[0];
            return gf.createPolygon(gf.createLinearRing(coordinates), null);
        }

    }

    /**
     * Computes an intersection {@link Geometry} between sourceEnvelope and
     * targetEnvelope in targetEnvelope's CRS space. The resulting
     * {@link Geometry} may contain multiple Geometries within it. But all
     * geometries will be {@link Polygon}s
     * 
     * @param sourceEnvelope
     * @param targetEnvelope
     * @param maxHorDivisions
     * @param maxVertDivisions
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    public static Geometry createEnvelopeIntersection(Envelope sourceEnvelope,
            Envelope targetEnvelope, int maxHorDivisions, int maxVertDivisions)
            throws FactoryException, TransformException {
        return new BruteForceEnvelopeIntersection(sourceEnvelope, targetEnvelope,
                maxHorDivisions, maxVertDivisions).reproject();
    }
}
