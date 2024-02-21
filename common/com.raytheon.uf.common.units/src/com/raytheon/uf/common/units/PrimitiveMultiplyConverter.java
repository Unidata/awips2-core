/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.common.units;

import java.util.Objects;

import javax.measure.UnitConverter;

import tech.units.indriya.function.AbstractConverter;
import tech.units.indriya.function.MultiplyConverter;

/**
 * This class represents a converter multiplying numeric values by a constant
 * scaling factor. This uses primitive doubles as much as possible, for improved
 * performance compared to indriya's built-in RationalConverter.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 19, 2024 2036778    mapeters    Initial creation
 *
 * </pre>
 *
 * @author mapeters
 */
public class PrimitiveMultiplyConverter extends AbstractConverter
        implements MultiplyConverter {

    private static final long serialVersionUID = 6588759878444545649L;

    /**
     * Holds the scale factor.
     */
    private final double doubleFactor;

    /**
     * Creates a multiply converter with the specified scale factor.
     *
     * @param factor
     *            the scaling factor.
     */
    public PrimitiveMultiplyConverter(double factor) {
        this.doubleFactor = factor;
    }

    @Override
    public boolean isIdentity() {
        return doubleFactor == 1.0;
    }

    @Override
    protected boolean canReduceWith(AbstractConverter that) {
        return that instanceof PrimitiveMultiplyConverter;
    }

    @Override
    protected AbstractConverter reduce(AbstractConverter that) {
        return new PrimitiveMultiplyConverter(
                doubleFactor * ((PrimitiveMultiplyConverter) that).doubleFactor);
    }

    @Override
    public PrimitiveMultiplyConverter inverseWhenNotIdentity() {
        return new PrimitiveMultiplyConverter(1.0 / doubleFactor);
    }

    @Override
    protected Number convertWhenNotIdentity(Number value) {
        return doubleFactor * value.doubleValue();
    }

    @Override
    public final String transformationLiteral() {
        return String.format("x -> x * %s", doubleFactor);
    }

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
        PrimitiveMultiplyConverter other = (PrimitiveMultiplyConverter) obj;
        return Double.doubleToLongBits(doubleFactor) == Double
                .doubleToLongBits(other.doubleFactor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(doubleFactor);
    }

    @Override
    public Double getValue() {
        return doubleFactor;
    }

    @Override
    public int compareTo(UnitConverter o) {
        if (this == o) {
            return 0;
        }
        if (getClass() == o.getClass()) {
            return Double.compare(doubleFactor,
                    ((PrimitiveMultiplyConverter) o).doubleFactor);
        }
        return this.getClass().getName().compareTo(o.getClass().getName());
    }
}
