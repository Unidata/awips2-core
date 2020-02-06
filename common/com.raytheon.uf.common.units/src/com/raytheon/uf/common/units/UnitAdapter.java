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

import java.rmi.UnmarshalException;

import javax.measure.Unit;
import javax.measure.format.ParserException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import tec.uom.se.AbstractUnit;
import tec.uom.se.format.SimpleUnitFormat;

/**
 * Serialization adapter for Unit
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 12, 2008  1446     njensen   Initial creation
 * Aug 08, 2014  3503     bclement  moved from common.serialization to
 *                                  common.units
 * Mar 25, 2016  5439     bsteffen  Include unparseable unit in exception text.
 * Jul 28, 2016  5738     bsteffen  Remove unused ISerializationTypeAdapter methods.
 * Apr 15, 2019  7596     lsingh    Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author njensen
 */
public class UnitAdapter extends XmlAdapter<String, Unit<?>> {

    @Override
    public String marshal(Unit<?> v) throws Exception {
        if (v == null) {
            return "";
        } else {
            return v.toString();
        }
    }

    @Override
    public Unit<?> unmarshal(String unit) throws Exception {
        Unit<?> retVal = AbstractUnit.ONE;

        if (unit != null) {
            if (!unit.equals("")) {
                try {
                    retVal = (Unit<?>) SimpleUnitFormat
                            .getInstance(SimpleUnitFormat.Flavor.ASCII)
                            .parse(unit);
                } catch (ParserException e) {
                    throw new UnmarshalException(
                            "Error parsing unit from string " + unit, e);
                }
            }
        }
        return retVal;
    }

}
