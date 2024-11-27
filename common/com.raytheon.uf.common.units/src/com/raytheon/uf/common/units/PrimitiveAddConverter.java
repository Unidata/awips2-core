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

/**
 * This class represents a converter adding a constant offset to numeric values.
 * This uses primitive doubles as much as possible, for improved performance
 * compared to indriya's built-in AddConverter.
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
public final class PrimitiveAddConverter extends AbstractConverter {

    private static final long serialVersionUID = -2981335308595652284L;

    /**
     * Holds the offset.
     */
    private final double offset;

    /**
     * Creates an additive converter having the specified offset.
     *
     * @param offset
     *            the offset value.
     */
    public PrimitiveAddConverter(double offset) {
        this.offset = offset;
    }

    /**
     * Returns the offset value for this add converter.
     *
     * @return the offset value.
     */
    public Number getOffset() {
        return offset;
    }

    @Override
    public boolean isIdentity() {
        return offset == 0d;
    }

    @Override
    protected boolean canReduceWith(AbstractConverter that) {
        return that instanceof PrimitiveAddConverter;
    }

    @Override
    protected AbstractConverter reduce(AbstractConverter that) {
        return new PrimitiveAddConverter(
                offset + ((PrimitiveAddConverter) that).offset);
    }

    @Override
    public PrimitiveAddConverter inverseWhenNotIdentity() {
        return new PrimitiveAddConverter(-offset);
    }

    @Override
    protected Number convertWhenNotIdentity(Number value) {
        return value.doubleValue() + offset;
    }

    @Override
    public String transformationLiteral() {
        double signum = Math.signum(offset);
        return String.format("x -> x %s %s", signum < 0 ? "-" : "+",
                Math.abs(offset));
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
        PrimitiveAddConverter other = (PrimitiveAddConverter) obj;
        return Double.doubleToLongBits(offset) == Double
                .doubleToLongBits(other.offset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset);
    }

    @Override
    public boolean isLinear() {
        return isIdentity();
    }

    @Override
    public int compareTo(UnitConverter o) {
        if (this == o) {
            return 0;
        }
        if (getClass() == o.getClass()) {
            return Double.compare(this.offset, ((PrimitiveAddConverter) o).offset);
        }
        return this.getClass().getName().compareTo(o.getClass().getName());
    }
}
