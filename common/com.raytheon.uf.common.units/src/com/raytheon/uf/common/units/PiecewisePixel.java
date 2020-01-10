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

import java.util.Arrays;
import java.util.Map;

import javax.measure.Dimension;
import javax.measure.Quantity;
import javax.measure.Unit;
import javax.measure.UnitConverter;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

import tec.uom.se.AbstractUnit;

/**
 * TODO Add Description
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 15, 2019 7596       lsingh      Updated the javax.measure framework to JSR-363.
 *                                     DerivedUnit has been replaced with AbstractUnit.
 *                                     Updated method names and implemented additional methods.
 * Oct 28, 2019 7961       tgurney     Change getDimension to return
 *                                     stdUnit.getDimension() instead of NONE.
 *
 * </pre>
 *
 * @author randerso
 */

@DynamicSerialize
public class PiecewisePixel<Q extends Quantity<Q>> extends AbstractUnit<Q> {

    @DynamicSerializeElement
    private final Unit<Q> stdUnit;

    @DynamicSerializeElement
    private final double[] pixelValues;

    @DynamicSerializeElement
    private final double[] stdValues;

    public PiecewisePixel(Unit<Q> dispUnit, double[] pixelValues,
            double[] dispValues) {
        super();
        if (pixelValues.length != dispValues.length) {
            throw new IllegalArgumentException(
                    "pixelValues and stdValues arrays must be of equal length");
        }
        this.pixelValues = pixelValues;

        if (dispUnit instanceof AbstractUnit && !((AbstractUnit<Q>) dispUnit)
                .getSystemConverter().isLinear()) {
            stdUnit = dispUnit;
        } else {
            this.stdUnit = dispUnit.getSystemUnit();
        }

        UnitConverter toStd = dispUnit.getConverterTo(stdUnit);
        this.stdValues = new double[dispValues.length];
        for (int i = 0; i < dispValues.length; i++) {
            stdValues[i] = toStd.convert(dispValues[i]);
        }
    }

    private static final long serialVersionUID = 1L;

    @SuppressWarnings("unchecked")
    @Override
    protected Unit<Q> toSystemUnit() {
        return stdUnit.getSystemUnit();
    }

    @Override
    public UnitConverter getSystemConverter() {
        if (stdUnit instanceof AbstractUnit && !((AbstractUnit<Q>) stdUnit)
                .getSystemConverter().isLinear()) {
            return ((AbstractUnit<Q>) stdUnit).getSystemConverter().concatenate(
                    new PiecewiseLinearConverter(pixelValues, stdValues));
        }
        return new PiecewiseLinearConverter(pixelValues, stdValues);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(pixelValues);
        result = prime * result + (stdUnit == null ? 0 : stdUnit.hashCode());
        result = prime * result + Arrays.hashCode(stdValues);
        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PiecewisePixel<Q> other = (PiecewisePixel<Q>) obj;
        if (!Arrays.equals(pixelValues, other.pixelValues)) {
            return false;
        }
        if (stdUnit == null) {
            if (other.stdUnit != null) {
                return false;
            }
        } else if (!stdUnit.equals(other.stdUnit)) {
            return false;
        }
        if (!Arrays.equals(stdValues, other.stdValues)) {
            return false;
        }
        return true;
    }

    @Override
    public Map<? extends Unit<?>, Integer> getBaseUnits() {
        return stdUnit.getBaseUnits();
    }

    @Override
    public Dimension getDimension() {
        return stdUnit.getDimension();
    }

}
