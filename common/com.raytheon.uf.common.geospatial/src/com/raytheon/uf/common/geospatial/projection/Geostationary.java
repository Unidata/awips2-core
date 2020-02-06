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
 * further licenMath.sing information.
 **/
package com.raytheon.uf.common.geospatial.projection;

import java.awt.geom.Point2D;

import si.uom.SI;
import tec.uom.se.AbstractUnit;

import javax.measure.Unit;

import org.geotools.parameter.DefaultParameterDescriptor;
import org.geotools.parameter.DefaultParameterDescriptorGroup;
import org.geotools.referencing.operation.MathTransformProvider;
import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.referencing.operation.projection.ProjectionException;
import org.opengis.parameter.InvalidParameterNameException;
import org.opengis.parameter.InvalidParameterValueException;
import org.opengis.parameter.ParameterDescriptor;
import org.opengis.parameter.ParameterDescriptorGroup;
import org.opengis.parameter.ParameterNotFoundException;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;

/**
 * Geostationary map projection. Earth as viewed from space. The canonical
 * source for the equations is the GOES-R Product Definition
 * Guide(http://www.goes-r.gov/products/docs/PUG-L2+-vol5.pdf). Support for
 * sweep axis of 1.0 was created using equations based on Coordination Group for
 * Meteorological Satellites LRIT/HRIT Global
 * Specification(http://www.cgms-info.
 * org/documents/cgms-lrit-hrit-global-specification
 * -%28v2-8-of-30-oct-2013%29.pdf).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jun 27, 2013           mschenke    Initial creation
 * Oct 02, 2013  2333     mschenke    Converted from libproj to CGMS algorithm
 * Nov 18, 2013  2528     bsteffen    Add hashCode/equals.
 * Apr 14, 2015  4387     bsteffen    Fix the quadratic equation.
 * Mar 15, 2016  5456     bsteffen    Fix swapped sweep axis.
 * Apr 15, 2019  7596     lsingh      Updated units framework to JSR-363
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class Geostationary extends MapProjection {

    private static final long serialVersionUID = 4747155239658791357L;

    public static final String PROJECTION_NAME = "Geostationary";

    public static final String ORBITAL_HEIGHT = "orbital_height";

    /**
     * This is commonly referred to as sweep_axis_angle in other geospatial
     * libraries. A value of 0.0 is the default and indicates an x sweep angle
     * axis as used by the GOES satellites. A value of 1.0 indicates a y sweep
     * angle axis as used by the meteosat and himawari-8 satellites. All other
     * values are undefined and will currently default to x.
     */
    public static final String SWEEP_AXIS = "sweep_axis";

    static final double DEFAULT_PERSPECTIVE_HEIGHT = 35800000.0;

    private double orbitalHeight;

    private double perspectiveHeight;

    /**
     * When this is false, sweep axis angle is x, when it is true sweep axis
     * angle is y. A discussion of the sweep axis can be found at
     * http://trac.osgeo.org/proj/wiki/proj%3Dgeos
     * */
    private boolean ySweepAxis = false;

    private double rEq, rEq2;

    private double rPol, rPol2;

    private double height_ratio;

    private double e2;

    /**
     * @param values
     * @throws ParameterNotFoundException
     */
    protected Geostationary(ParameterValueGroup values)
            throws ParameterNotFoundException {
        super(values);
        this.orbitalHeight = Provider.getValue(Provider.ORBITAL_HEIGHT, values);
        this.perspectiveHeight = orbitalHeight + semiMajor;
        double sweepValue = Provider.getValue(Provider.SWEEP_AXIS, values);
        this.ySweepAxis = sweepValue == 1.0;

        this.rEq = semiMajor;
        this.rEq2 = rEq * rEq;
        this.rPol = semiMinor;
        this.rPol2 = rPol * rPol;
        this.e2 = (rEq2 - rPol2) / rEq2;

        this.height_ratio = rEq / orbitalHeight;
    }

    @Override
    public ParameterDescriptorGroup getParameterDescriptors() {
        return Provider.PARAMETERS;
    }

    @Override
    public ParameterValueGroup getParameterValues() {
        ParameterValueGroup values = super.getParameterValues();
        values.parameter(Provider.ORBITAL_HEIGHT.getName().getCode()).setValue(
                orbitalHeight);
        values.parameter(Provider.SWEEP_AXIS.getName().getCode()).setValue(
                ySweepAxis ? 1.0 : 0.0);
        return values;
    }

    @Override
    protected Point2D inverseTransformNormalized(double x, double y,
            Point2D ptDst) throws ProjectionException {
        double lam = Double.NaN, phi = Double.NaN;
        x *= height_ratio;
        y *= height_ratio;

        double sinX = Math.sin(x);
        double cosX = Math.cos(x);

        double sinY = Math.sin(y);
        double cosY = Math.cos(y);

        double a = sinX * sinX + cosX * cosX
                * (cosY * cosY + (rEq2 / rPol2) * sinY * sinY);
        double b = -2 * perspectiveHeight * cosX * cosY;
        double c = perspectiveHeight * perspectiveHeight - rEq2;

        double Vl = quadraticEquation(a, b, c);
        double Vx = Vl * cosX * cosY;

        double Vy, Vz;
        if (ySweepAxis) {
            // HIMAWARI
            Vy = Vl * sinX * cosY; 
            Vz = Vl * sinY;
        } else {
            // GOESR
            Vy = Vl * sinX; 
            Vz = Vl * sinY * cosX; 
        }

        double s1 = perspectiveHeight - Vx;

        lam = Math.atan(Vy / s1);
        phi = Math.atan((rEq2 / rPol2) * (Vz / Math.sqrt(s1 * s1 + Vy * Vy)));

        if (ptDst == null) {
            ptDst = new Point2D.Double();
        }
        ptDst.setLocation(lam, phi);
        return ptDst;
    }

    @Override
    protected Point2D transformNormalized(double lam, double phi, Point2D ptDst)
            throws ProjectionException {
        double x = Double.NaN, y = Double.NaN;

        double cPhi = Math.atan((rPol2 / rEq2) * Math.tan(phi));
        double cosPhi = Math.cos(cPhi);

        double rs = rPol / Math.sqrt(1 - e2 * cosPhi * cosPhi);
        double rPhi = rs * cosPhi;

        double Vx = perspectiveHeight - rPhi * Math.cos(lam);
        double Vy = rPhi * Math.sin(lam);
        double Vz = rs * Math.sin(cPhi);
        double Vn = Math.sqrt(Vx * Vx + Vy * Vy + Vz * Vz);

        if ((perspectiveHeight * (perspectiveHeight - Vx)) < (Vy * Vy + (rEq2 / rPol2)
                * Vz * Vz)) {
            x = y = Double.NaN;
        } else {
            if (ySweepAxis) {
                // HIMAWARI
                x = Math.atan(Vy / Vx);
                y = Math.asin(Vz / Vn);
            } else {
                // GOESR
                x = Math.asin(Vy / Vn);
                y = Math.atan(Vz / Vx);
            }
        }

        if (ptDst == null) {
            ptDst = new Point2D.Double();
        }
        ptDst.setLocation(x / height_ratio, y / height_ratio);
        return ptDst;
    }

    /**
     * Naive implementation of the quadratic equation.
     * 
     * @see #stableQuadraticEquation(double, double, double)
     */
    protected static double quadraticEquation(double a, double b, double c) {
        return (-b - Math.sqrt(b * b - 4 * a * c)) / (2 * a);
    }

    /**
     * A more stable implementation of
     * {@link #quadraticEquation(double, double, double)}. This implementation
     * is based off <a href=
     * "http://en.wikipedia.org/wiki/Loss_of_significance#A_better_algorithm">a
     * wikipedia article</a>, which is based off
     * 
     * <pre>
     * Press, William H.; Flannery, Brian P.; Teukolsky, Saul A.; Vetterling, William T. (1992), <a href="http://www.nrbook.com/a/bookcpdf.php">Numerical Recipes in C</a> (Second ed.), Section 5.6: "Quadratic and Cubic Equations.
     * </pre>
     * 
     * <p>
     * This implementation is not currently used because the naive
     * implementation is stable enough for the uses in this class. This method
     * was left in as a development/debugging aid which can be used if there are
     * stability concerns with the naive implementation.
     * </p>
     */
    protected static double stableQuadraticEquation(double a, double b, double c) {
        double x1 = (-b - Math.signum(b) * Math.sqrt(b * b - 4 * a * c))
                / (2 * a);
        double x2 = c / (a * x1);
        return x2;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(orbitalHeight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (ySweepAxis ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Geostationary other = (Geostationary) obj;
        if (Double.doubleToLongBits(orbitalHeight) != Double
                .doubleToLongBits(other.orbitalHeight))
            return false;
        if (ySweepAxis != other.ySweepAxis)
            return false;
        return true;
    }

    public static class Provider extends AbstractProvider {

        private static final long serialVersionUID = 3868187206568280453L;

        static final ParameterDescriptor<Double> ORBITAL_HEIGHT = DefaultParameterDescriptor
                .create(Geostationary.ORBITAL_HEIGHT,
                        DEFAULT_PERSPECTIVE_HEIGHT, 0, Double.MAX_VALUE,
                        SI.METRE);

        static final ParameterDescriptor<Double> SWEEP_AXIS = DefaultParameterDescriptor
                .create(Geostationary.SWEEP_AXIS, 0.0, 0.0, 1.0, AbstractUnit.ONE);

        static final ParameterDescriptorGroup PARAMETERS = new DefaultParameterDescriptorGroup(
                PROJECTION_NAME, new ParameterDescriptor[] { SEMI_MAJOR,
                        SEMI_MINOR, CENTRAL_MERIDIAN, LATITUDE_OF_ORIGIN,
                        FALSE_EASTING, FALSE_NORTHING, ORBITAL_HEIGHT,
                        SWEEP_AXIS });

        public Provider() {
            super(PARAMETERS);
        }

        @Override
        protected MathTransform createMathTransform(ParameterValueGroup values)
                throws InvalidParameterNameException,
                ParameterNotFoundException, InvalidParameterValueException,
                FactoryException {
            return new Geostationary(values);
        }

        static <T> T getValue(ParameterDescriptor<T> descriptor,
                ParameterValueGroup group) {
            return MathTransformProvider.value(descriptor, group);
        }

    }

}
