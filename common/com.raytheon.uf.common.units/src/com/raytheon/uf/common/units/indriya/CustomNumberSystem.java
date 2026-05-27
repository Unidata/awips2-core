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
package com.raytheon.uf.common.units.indriya;

import java.math.BigDecimal;
import java.math.BigInteger;

import tech.units.indriya.function.Calculus;
import tech.units.indriya.function.DefaultNumberSystem;

/**
 * Custom number system for the indriya units library to use. This tweaks the
 * default number system with a couple performance enhancements that were
 * specifically noticed to help speed up unit conversions when loading radial
 * velocity as grid data, and goes-r Day Convection data.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Mar 14, 2023 2031675    mapeters    Initial creation
 * Dec 20, 2023 2036519    mapeters    Override multiply()
 * Feb 20, 2024 2036778    mapeters    Simplify basic operations for performance,
 *                                     move to indriya subpackage
 * Oct 21, 2024 2037232    tgurney     Return NaN from narrow if NaN argument.
 *
 *
 * </pre>
 *
 * @author mapeters
 */
public class CustomNumberSystem extends DefaultNumberSystem {

    @Override
    public Number narrow(Number number) {
        if (isNaN(number)) {
            /*
             * super.narrow throws exception on NaN. We don't want that. Instead
             * we want the NaN to propagate because this is how floating-point
             * arithmetic is generally expected to behave.
             */
            return Double.NaN;
        }
        if (number instanceof BigDecimal decimal) {
            /*
             * The super method relies on exception catching to determine if
             * BigDecimals can be converted to integers, which is slow.
             *
             * This change has been applied to the default number system under
             * this indriya issue, so this override can eventually go away:
             * https://github.com/unitsofmeasurement/indriya/issues/393
             */
            decimal = decimal.stripTrailingZeros();
            if (decimal.scale() <= 0) {
                BigInteger integer = decimal.toBigInteger();
                return narrow(integer);
            }
            return number;
        }
        return super.narrow(number);
    }

    private static boolean isNaN(Number number) {
        return number instanceof Double d && d.isNaN()
                || number instanceof Float f && f.isNaN();
    }

    @Override
    public Number multiply(Number x, Number y) {
        // Simplified for performance and to prevent exceptions with NaNs
        return x.doubleValue() * y.doubleValue();
    }

    @Override
    public Number add(Number x, Number y) {
        // Simplified for performance and to prevent exceptions with NaNs
        return x.doubleValue() + y.doubleValue();
    }

    @Override
    public Number divide(Number x, Number y) {
        // Simplified for performance and to prevent exceptions with NaNs
        return x.doubleValue() / y.doubleValue();
    }

    @Override
    public Number subtract(Number x, Number y) {
        // Simplified for performance and to prevent exceptions with NaNs
        return x.doubleValue() - y.doubleValue();
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
