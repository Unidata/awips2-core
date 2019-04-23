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

import java.text.ParsePosition;
import java.util.Objects;

import javax.measure.IncommensurableException;
import javax.measure.UnconvertibleException;
import javax.measure.Unit;
import javax.measure.format.ParserException;

import tec.uom.se.AbstractUnit;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * 
 * Unit converter class
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 15, 2013 1638       mschenke    Moved from edex.common util package
 * Apr 15, 2019  7596      lsingh      Updated units framework to JSR-363. Added
 *                                     method to handle conversion exceptions
 *
 * </pre>
 *
 * @author unknown
 * @version 1.0
 */
public class UnitConv {

    public static Unit<?> deserializer(String unit) throws ParserException {
        if (!"".equals(unit)) {
            return SimpleUnitFormat.getInstance(SimpleUnitFormat.Flavor.ASCII)
                    .parseObject(unit, new ParsePosition(0));
        }
        return AbstractUnit.ONE;
    }

    public static String serializer(Unit<?> unit) {
        return unit == null ? "" : unit.toString();
    }

    /**
     * This method is intended to handle unit conversions between two unknown
     * Quantity types.
     * 
     * @deprecated: During the foss upgrade of javax.measure to JSR-363
     *              standards, the Unit.getConverterTo() method was changed to
     *              be type-safe so that both units in the equation need to have
     *              explicitly matching Quantity types (eg, Length, Speed).
     *              Since there were many instances in the code where a Unit was
     *              instantiated, and the Quantity type was assumed, but not
     *              explicitly assigned (ex. Unit<?> ), so after the
     *              javax.measure upgrade, the method getConverterTo() could not
     *              be used anymore. <br/>
     *              <br/>
     * 
     *              As a result, most usages of getConverterTo() in the code had
     *              to be changed to getConverterToAny(), which throws a checked
     *              exception. This is unnecessary because in many cases we DO
     *              know the Quantity type of a Unit, but since it was never
     *              explicitly stated, and the quantity type wasn't obvious to
     *              those refactoring the code, many instances of
     *              getConverterTo() had to be replaced with
     *              getConverterToAny(). Hence, this method was created to
     *              discourage the behavior going forward. <br/>
     *              <br/>
     * 
     *              What does this all mean? It means you should avoid using
     *              this method, and the method {@link Unit.getConverterToAny()}
     *              as much as possible, and instead define Units with explicit
     *              Quantity types. <br/>
     *              <br/>
     * 
     *              So, if you know the Quantity type of a Unit, AVOID this:
     *              <br/>
     *              <br/>
     * 
     *              <code>
     *              Unit<?> unit1 = SI.METRE; 
     *              Unit<?> unit2 = MetricPrefix.KILO(SI.METRE);
     *              try{
     *                  unit1.getConverterToAny(unit2); 
     *              } catch (IncommensurableException e) { .... }
     *              </code> <br/>
     *              <br/>
     * 
     *              DO this instead: <br/>
     *              <br/>
     * 
     *              <code>
     *              Unit<Length> unit1 = SI.METRE; 
     *              Unit<Length> unit2 = MetrixPrefix.KILO(SI.METRE); 
     *              unit1.getConverterTo(unit2);
     *              </code> <br/>
     *              <br/>
     * 
     *              It will save you from handling an unnecessary exception.
     * 
     * @param fromUnit
     * @param toUnit
     * @return a converter for two units of unknown quantity types.
     * @throws UnconvertibleException
     */
    @Deprecated
    public static javax.measure.UnitConverter getConverterToUnchecked(
            Unit<?> fromUnit, Unit<?> toUnit) throws UnconvertibleException {
        try {
            return fromUnit.getConverterToAny(toUnit);
        } catch (IncommensurableException e) {
            throw new UnconvertibleException(
                    "Failed to convert " + Objects.toString(fromUnit) + " to "
                            + Objects.toString(toUnit),
                    e);
        }
    }

}
