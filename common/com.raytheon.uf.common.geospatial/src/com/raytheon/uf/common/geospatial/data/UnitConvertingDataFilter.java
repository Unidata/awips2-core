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
package com.raytheon.uf.common.geospatial.data;

import javax.measure.UnitConverter;

import com.raytheon.uf.common.numeric.dest.DataDestination;
import com.raytheon.uf.common.numeric.dest.FilteredDataDestination;
import com.raytheon.uf.common.numeric.filter.DataFilter;
import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.numeric.source.FilteredDataSource;

/**
 * 
 * A filter that converts to a unit with the specified unit converter.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Mar 07, 2014  2791     bsteffen    Initial creation
 * Oct 12, 2022  8905     lsingh      Check for NaN when converting units.
 * Nov 02, 2023  2036360  lsingh      Updated check for NaN when converting units.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class UnitConvertingDataFilter implements DataFilter {

    protected UnitConverter unitConverter;

    public UnitConvertingDataFilter(UnitConverter converter) {
        this.unitConverter = converter;
    }

    @Override
    public double filter(double value) {
        try {
            if(Double.isNaN(value)) {
                return Double.NaN;
            } else {
                 return unitConverter.convert(value);
            }
        } catch(NumberFormatException e ) {
            return Double.NaN;
        }
    }

    public static DataSource apply(DataSource source, UnitConverter converter) {
        return FilteredDataSource.addFilters(source,
                new UnitConvertingDataFilter(converter));
    }

    public static DataDestination apply(DataDestination destination,
            UnitConverter converter) {
        return FilteredDataDestination.addFilters(destination,
                new UnitConvertingDataFilter(converter));
    }

}
