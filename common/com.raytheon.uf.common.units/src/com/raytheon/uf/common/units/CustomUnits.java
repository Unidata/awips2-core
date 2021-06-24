package com.raytheon.uf.common.units;

import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;

import systems.uom.quantity.Information;
import tec.uom.se.AbstractUnit;
import tec.uom.se.function.LogConverter;
import tec.uom.se.function.RationalConverter;
import tec.uom.se.unit.AlternateUnit;

/**
 *
 * Customs units. These units were previously a part of the javax.measure
 * framework, but have since been removed as of JSR-363.
 *
 * @see gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NCUnits
 *      Contains additional custom units.
 *
 *      <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 13, 2019  7596     lsingh    Initial creation
 * Jun 24, 2021  8570     randerso  Added BIT unit definition
 *
 *      </pre>
 *
 * @author lsingh
 */
public class CustomUnits {

    /**
     * A logarithmic unit used to describe a ratio (standard name
     * <code>dB</code>).
     */
    public static final Unit<Dimensionless> DECIBEL = AbstractUnit.ONE
            .transform(new LogConverter(10).inverse()
                    .concatenate(new RationalConverter(1, 10)));

    /**
     * The unit for binary information (standard name <code>bit</code>).
     */
    public static final Unit<Information> BIT = new AlternateUnit<>(
            AbstractUnit.ONE, "bit");

}
