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

import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

/**
 * Contains calculations for calculating dewpoint.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Aug 14, 2013  2260     bsteffen    Initial creation
 * May 04, 2015  4445     bsteffen    Added calculateFromTandRH
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */

public class Dewpoint {

    public static final Unit<Pressure> PRESSURE_UNIT = SI.HECTO(SI.PASCAL);

    public static final Unit<Dimensionless> SPECIFIC_HUMIDITY_UNIT = SI.GRAM
            .divide(SI.KILOGRAM).asType(Dimensionless.class);

    public static final Unit<Temperature> TEMPERATURE_UNIT = SI.KELVIN;

    public static final Unit<Dimensionless> RELATIVE_HUMIDITY_UNIT = NonSI.PERCENT;

    public static final Unit<Temperature> DEWPOINT_UNIT = SI.KELVIN;

    /**
     * @depreacted Use {@link #calculateFromPandSH(Measure, Measure)}
     */
    @Deprecated
    public static Measure<?, Temperature> calculate(Measure<?, Pressure> pressure,
            Measure<?, Dimensionless> specificHumidity) {
        return calculateFromPandSH(pressure, specificHumidity);
    }

    /**
     * @depreacted Use {@link #calculateFromPandSH(double, double)}
     */
    @Deprecated
    public static double calculate(double pressure, double specificHumidity) {
        return calculateFromPandSH(pressure, specificHumidity);
    }

    public static Measure<?, Temperature> calculateFromPandSH(
            Measure<?, Pressure> pressure,
            Measure<?, Dimensionless> specificHumidity) {
        double pressureVal = pressure.doubleValue(PRESSURE_UNIT);
        double specHumVal = specificHumidity
                .doubleValue(SPECIFIC_HUMIDITY_UNIT);
        double dewpointVal = calculateFromPandSH(pressureVal, specHumVal);
        return Measure.valueOf(dewpointVal,
                DEWPOINT_UNIT);
    }

    /* Math originally from the AWIPS 1 meteoLib calctd2.f */
    public static double calculateFromPandSH(double pressure,
            double specificHumidity) {
        double eee = pressure * specificHumidity
                / (622.0 + 0.378 * specificHumidity);
        double b = 26.66082 - Math.log(eee);
        return ((b - Math.sqrt(b * b - 223.1986)) / 0.0182758048);
    }

    public static Measure<?, Temperature> calculateFromTandRH(
            Measure<?, Temperature> temperature,
            Measure<?, Dimensionless> relativeHumidity) {
        double temperatureVal = temperature.doubleValue(TEMPERATURE_UNIT);
        double relHumVal = relativeHumidity.doubleValue(RELATIVE_HUMIDITY_UNIT);
        double dewpointVal = calculateFromTandRH(temperatureVal, relHumVal);
        return Measure.valueOf(dewpointVal, DEWPOINT_UNIT);
    }

    /* Math originally from the AWIPS 1 meteoLib calctd.f */
    public static double calculateFromTandRH(double temperature,
            double relaticeHumidity) {
        double b = 0.0091379024 * temperature;
        b += 6106.396 / temperature;
        b -= Math.log(relaticeHumidity / 100);
        double val = b * b;
        val -= 223.1986;
        val = Math.sqrt(val);
        double dewpoint = b - val;
        dewpoint /= 0.0182758048;
        return dewpoint;
    }
}
