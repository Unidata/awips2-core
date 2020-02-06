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
package com.raytheon.uf.common.units;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Arrays;

import javax.measure.UnitConverter;

import tec.uom.se.AbstractConverter;

/**
 * TODO Add Description
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#    Engineer    Description
 * ------------  ---------- ----------- --------------------------
 *  Apr 15, 2019  7596       lsingh      Updated the javax.measure framework to JSR-363.
 *                                       UnitConverter has been replaced with AbstractConverter. 
 *                                       Updated methods and implemented additional methods.
 * 
 * </pre>
 * 
 * @author randerso
 */

public class PiecewiseLinearConverter extends AbstractConverter {

    private static final long serialVersionUID = 1L;

    private double[] xVals;

    private double[] yVals;

    public PiecewiseLinearConverter(double[] xVals, double[] yVals) {
        this.xVals = xVals;
        this.yVals = yVals;
    }

    @Override
    public double convert(double x) {
        if (Double.isNaN(x)) {
            return Double.NaN;
        }

        int i;
        for (i = 0; i < xVals.length - 1; i++) {
            if ((x >= xVals[i]) && (x <= xVals[i + 1]))
                break;
        }

        double y;

        if (i >= xVals.length - 1) {
            y = Double.NaN;
        } else {
            // interpolate
            y = (x - xVals[i]) * (yVals[i + 1] - yVals[i])
                    / (xVals[i + 1] - xVals[i]) + yVals[i];
        }

        return y;
    }

    @Override
    public AbstractConverter inverse() {
        return new PiecewiseLinearConverter(yVals, xVals);
    }

    @Override
    public boolean isLinear() {
        return false;
    }

    @Override
    public UnitConverter concatenate(UnitConverter converter) {
        // TODO Auto-generated method stub
        UnitConverter result = super.concatenate(converter);
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = Float.floatToIntBits((float)convert(1.0));
        result = prime * result + Arrays.hashCode(xVals);
        result = prime * result + Arrays.hashCode(yVals);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (getClass() != obj.getClass())
            return false;
        final PiecewiseLinearConverter other = (PiecewiseLinearConverter) obj;
        if (!Arrays.equals(xVals, other.xVals))
            return false;
        if (!Arrays.equals(yVals, other.yVals))
            return false;
        return true;
    }

    @Override
    public BigDecimal convert(BigDecimal value, MathContext ctx)
            throws ArithmeticException {
        return BigDecimal.valueOf(convert(value.doubleValue()));
    }

}
