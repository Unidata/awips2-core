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

package com.raytheon.uf.viz.core;

import org.eclipse.swt.graphics.Rectangle;
import org.opengis.coverage.grid.GridEnvelope;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;

/**
 * PixelExtent represents the coverage of an object in pixel space.
 * 
 * 
 * PixelExtents are PIXEL coordinates. Use Envelope for WORLD coordinates.
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ---------------------------
 * Jul 01, 0006           chammack  Initial Creation.
 * Dec 06, 2013  2599     njensen   Fix PixelExtent(Rectangle)
 * Nov 03, 2016  5976     bsteffen  Remove unused 3D support
 * 
 * </pre>
 * 
 * @author chammack
 * 
 */

public class PixelExtent implements IExtent {

    private Envelope envelope;

    private double aMinX = 0.0;

    private double aMaxX = 0.0;

    private double aMinY = 0.0;

    private double aMaxY = 0.0;

    public PixelExtent(GridEnvelope range) {
        this(range.getLow(0), range.getHigh(0), range.getLow(1), range
                .getHigh(1));
    }

    public PixelExtent(double aMinX, double aMaxX, double aMinY, double aMaxY) {
        envelope = new Envelope(aMinX, aMaxX, aMinY, aMaxY);
        this.aMinX = aMinX;
        this.aMaxX = aMaxX;
        this.aMinY = aMinY;
        this.aMaxY = aMaxY;
    }

    public PixelExtent(Coordinate[] coords) {

        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;

        for (int i = 0; i < coords.length; ++i) {

            if (coords[i].x < minX) {
                minX = coords[i].x;
            }

            if (coords[i].y < minY) {
                minY = coords[i].y;
            }

            if (coords[i].x > maxX) {
                maxX = coords[i].x;
            }

            if (coords[i].y > maxY) {
                maxY = coords[i].y;
            }

        }
        this.aMinX = minX;
        this.aMaxX = maxX;
        this.aMinY = minY;
        this.aMaxY = maxY;
        envelope = new Envelope(aMinX, aMaxX, aMinY, aMaxY);
    }

    @Override
    public void reset() {
        envelope = new Envelope(aMinX, aMaxX, aMinY, aMaxY);
    }

    public PixelExtent(Rectangle rect) {
        this(rect.x, rect.x + rect.width, rect.y, rect.y + rect.height);
    }

    @Override
    public double getMaxX() {

        return envelope.getMaxX();
    }

    @Override
    public double getMaxY() {

        return envelope.getMaxY();
    }

    @Override
    public double getMinX() {

        return envelope.getMinX();
    }

    @Override
    public double getMinY() {

        return envelope.getMinY();
    }

    @Override
    public double getWidth() {
        return getMaxX() - getMinX();
    }

    @Override
    public double getHeight() {
        return getMaxY() - getMinY();
    }

    @Override
    public void scale(double factor) {
        double deltaWidth = getWidth() * (factor - 1.0);
        double deltaHeight = getHeight() * (factor - 1.0);

        envelope.expandBy(deltaWidth / 2.0, deltaHeight / 2.0);
    }

    @Override
    public void scaleAndBias(double factor, double xCenter, double yCenter) {
        double deltaWidth = envelope.getWidth() * (factor - 1.0);
        double deltaHeight = envelope.getHeight() * (factor - 1.0);

        double midX = (getMaxX() + getMinX()) / 2.0f;
        double midY = (getMaxY() + getMinY()) / 2.0f;

        double deltaX = (midX - xCenter);
        double deltaY = (midY - yCenter);

        envelope.translate(-deltaX, -deltaY);
        envelope.expandBy(deltaWidth / 2.0, deltaHeight / 2.0);
        envelope.translate((deltaX * factor), (deltaY * factor));

    }

    @Override
    public double[] getCenter() {
        double midX = (getMaxX() + getMinX()) / 2.0f;
        double midY = (getMaxY() + getMinY()) / 2.0f;
        double midZ = 0.0;

        return new double[] { midX, midY, midZ };
    }

    @Override
    public double getScale() {
        return getWidth() / (aMaxX - aMinX);
    }

    @Override
    public void shift(double shiftX, double shiftY) {

        envelope.translate(shiftX, shiftY);

    }

    @Override
    public boolean equals(Object arg0) {
        if (arg0 == null || !(arg0 instanceof PixelExtent)) {
            return false;
        }

        return envelope.equals(((PixelExtent) arg0).envelope);
    }

    public boolean contains(double x, double y) {
        return this.contains(new double[] { x, y });
    }

    @Override
    public boolean contains(double[] pixel) {
        return envelope.contains(pixel[0], pixel[1]);
    }

    @Override
    public boolean intersects(IExtent pe) {
        return intersect((PixelExtent) pe);
    }

    public boolean intersect(PixelExtent pe) {

        if (pe == null) {
            return false;
        }

        return envelope.intersects(pe.envelope);

    }

    public boolean contains(Coordinate coord) {
        return envelope.contains(coord);
    }

    @Override
    public PixelExtent clone() {
        double[] myCenter = getCenter();
        PixelExtent pe = new PixelExtent(aMinX, aMaxX, aMinY, aMaxY);
        double[] curCenter = pe.getCenter();
        pe.shift(myCenter[0] - curCenter[0], myCenter[1] - curCenter[1]);
        pe.scaleAndBias(getScale(), myCenter[0], myCenter[1]);
        return pe;
    }

    @Override
    public String toString() {
        return "PIXELEXTENT { " + getMinX() + " " + getMaxX() + " " + getMinY()
                + " " + getMaxY() + " }";
    }

    /**
     * Get the underlying envelope object
     * 
     * @return
     */
    public Envelope getEnvelope() {
        return envelope;
    }

    /**
     * Serialize a pixel extent
     * 
     * @param pixelExtent
     * @return the serialized form
     */
    public static String serialize(PixelExtent pixelExtent) {
        if (pixelExtent == null) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(pixelExtent.getMinX());
        sb.append(" ");
        sb.append(pixelExtent.getMaxX());
        sb.append(" ");
        sb.append(pixelExtent.getMinY());
        sb.append(" ");
        sb.append(pixelExtent.getMaxY());
        return sb.toString();
    }

    /**
     * Deserialize a pixel extent from a string
     * 
     * @param data
     *            the serialized form fo the pixel extent
     * @return the pixel extent object
     */
    public static PixelExtent deserialize(String data) {
        if (data == null) {
            return null;
        }

        String[] parts = data.split(" ");
        if (parts.length != 4) {
            return null;
        }

        double[] vals = new double[4];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = Double.parseDouble(parts[i]);
        }

        return new PixelExtent(vals[0], vals[1], vals[2], vals[3]);

    }

    @Override
    public IExtent intersection(IExtent e) {
        IExtent ie = null;
        if (e instanceof PixelExtent) {
            PixelExtent pe = (PixelExtent) e;
            Envelope intersection = this.envelope.intersection(pe.envelope);
            ie = new PixelExtent(intersection.getMinX(),
                    intersection.getMaxX(), intersection.getMinY(),
                    intersection.getMaxY());
        }
        return ie;
    }

    @Override
    public int hashCode() {
        return envelope.hashCode();
    }

}
