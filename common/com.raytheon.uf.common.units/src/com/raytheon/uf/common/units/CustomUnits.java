package com.raytheon.uf.common.units;

import javax.measure.Unit;
import javax.measure.quantity.Dimensionless;

import tec.uom.se.AbstractUnit;
import tec.uom.se.function.LogConverter;
import tec.uom.se.function.RationalConverter;

/**
 * 
 * Customs units. These units were previously a part of the javax.measure framework, but have since been removed
 * as of JSR-363. 
 * 
 * @see gov.noaa.nws.ncep.edex.common.metparameters.parameterconversion.NCUnits Contains additional custom units.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 13, 2019            lsingh     Initial creation
 *
 * </pre>
 *
 * @author lsingh
 */
public class CustomUnits {
  
    /**
     * A logarithmic unit used to describe a ratio
     * (standard name <code>dB</code>).
     */
    public static final Unit<Dimensionless> DECIBEL = AbstractUnit.ONE
            .transform(new LogConverter(10).inverse()
                    .concatenate(new RationalConverter(1, 10)));


}
