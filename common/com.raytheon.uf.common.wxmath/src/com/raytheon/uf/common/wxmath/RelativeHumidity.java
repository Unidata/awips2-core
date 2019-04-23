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
package com.raytheon.uf.common.wxmath;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import si.uom.NonSI;
import si.uom.SI;
import tec.uom.se.quantity.Quantities;
import tec.uom.se.unit.MetricPrefix;
import tec.uom.se.unit.Units;

import javax.measure.Unit;

/**
 * Contains calculations for calculating relative humidity from pressure,
 * temperature, and specific humidity.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 14, 2013 2260       bsteffen    Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class RelativeHumidity {

    public static final Unit<Pressure> PRESSURE_UNIT = MetricPrefix.HECTO(SI.PASCAL);

    public static final Unit<Temperature> TEMPERATURE_UNIT = SI.KELVIN;

    public static final Unit<Dimensionless> SPECIFIC_HUMIDITY_UNIT = SI.GRAM
            .divide(SI.KILOGRAM).asType(Dimensionless.class);

    public static final Unit<Dimensionless> RELATIVE_HUMIDITY_UNIT = Units.PERCENT;

    public static Quantity<Dimensionless> calculate(
            Quantity<Pressure> pressure, Quantity<Temperature> temperature,
            Quantity<Dimensionless> specificHumidity) {
        double pressureVal = pressure.to(PRESSURE_UNIT).getValue().doubleValue();
        double tempVal = temperature.to(TEMPERATURE_UNIT).getValue().doubleValue();
        double specHumVal = specificHumidity.to(SPECIFIC_HUMIDITY_UNIT).getValue()
                .doubleValue();
        double relHumVal = calculate(pressureVal, tempVal, specHumVal);
        return Quantities.getQuantity(relHumVal, RELATIVE_HUMIDITY_UNIT);
    }

    public static double calculate(double pressure, double temperature,
            double specificHumidity) {
        double a = 22.05565;
        double b = 0.0091379024;
        double c = 6106.396;
        double epsilonx1k = 622.0;

        double shxDenom = specificHumidity * 0.378;
        shxDenom += epsilonx1k;

        double tDenom = -b * temperature;
        tDenom += a;
        tDenom -= c / temperature;

        double RH = pressure * specificHumidity;
        RH /= shxDenom;
        RH /= Math.exp(tDenom);

        return RH;
    }
}
