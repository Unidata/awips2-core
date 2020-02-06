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

import javax.measure.format.ParserException;

import org.apache.commons.beanutils.Converter;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

import tec.uom.se.format.SimpleUnitFormat;

/**
 * Custom converter implementation for converting Unit objects from Strings
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         bphillip    Initial Creation
 * Jan 23, 2019 7980       tgurney     Fix method signature
 * </pre>
 *
 * @author bphillip
 */
public class UnitConverter implements Converter {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(UnitConverter.class);

    @Override
    public <T> T convert(Class<T> clazz, Object value) {
        if (value instanceof String) {
            try {
                return clazz.cast(SimpleUnitFormat
                        .getInstance(SimpleUnitFormat.Flavor.ASCII)
                        .parseObject((String) value, new ParsePosition(0)));
            } catch (ParserException e) {
                statusHandler.debug(e.getLocalizedMessage(), e);
            }
        }
        return null;
    }

}
