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

import java.math.BigDecimal;
import java.math.BigInteger;

import tech.units.indriya.function.Calculus;
import tech.units.indriya.function.DefaultNumberSystem;

/**
 * Custom number system for the indriya units library to use. This tweaks the
 * default number system with a couple performance enhancements that were
 * specifically noticed to help speed up the unit conversions in
 * RadarRequestableData when loading radial velocity as grid data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2023 2031675    mapeters    Initial creation
 * Dec 20, 2023 2036519    mapeters    Override multiply()
 *
 * </pre>
 *
 * @author mapeters
 */
public class CustomNumberSystem extends DefaultNumberSystem {

    @Override
    public Number narrow(Number number) {
        if (number instanceof BigDecimal) {
            /*
             * The super method relies on exception catching to determine if
             * BigDecimals can be converted to integers, which is slow.
             *
             * This change has been applied to the default number system under
             * this indriya issue, so this override can eventually go away:
             * https://github.com/unitsofmeasurement/indriya/issues/393
             */
            BigDecimal decimal = (BigDecimal) number;
            decimal = decimal.stripTrailingZeros();
            if (decimal.scale() <= 0) {
                BigInteger integer = decimal.toBigInteger();
                return narrow(integer);
            }
            return number;
        }
        return super.narrow(number);
    }

    @Override
    public Number multiply(Number x, Number y) {
        if (isNaN(x) || isNaN(y)) {
            /*
             * The super method throws a NumberFormatException with NaNs, and
             * catching the exceptions is slow.
             *
             * Indriya intentionally does not support NaN as discussed in this
             * issue: https://github.com/unitsofmeasurement/indriya/issues/287
             */
            return Float.NaN;
        }

        return super.multiply(x, y);
    }

    private static boolean isNaN(Number number) {
        return (number instanceof Double && ((Double) number).isNaN())
                || (number instanceof Float && ((Float) number).isNaN());
    }

    /**
     * Set an instance of this number system as the current units number system.
     *
     * @return null (has to be non-void to call from spring)
     */
    public static Object setAsCurrentNumberSystem() {
        Calculus.setCurrentNumberSystem(new CustomNumberSystem());
        return null;
    }
}
