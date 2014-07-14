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
package com.raytheon.uf.common.dataaccess.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

import com.raytheon.uf.common.dataaccess.IDataFactory;
import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.InvalidIdentifiersException;
import com.raytheon.uf.common.dataaccess.exception.MethodNotSupportedYetException;
import com.raytheon.uf.common.dataaccess.exception.UnsupportedOutputTypeException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;

/**
 * 
 * An abstract data factory that can be used by implementing IGridDataFactories
 * or IGeometryDataFactories.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 13, 2012            njensen     Initial creation
 * Feb 14, 2013 1614       bsteffen    Refactor data access framework to use
 *                                     single request.
 * Feb 19, 2012 1552       mpduff      Implement IDataFactory.
 * Jan 14, 2014 2667       mnash       Change getGridData and getGeometryData methods
 *                                     to throw exception by default
 * Jul 14, 2014 3184       njensen     Added getAvailableParameters() and getAvailableLevels()
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public abstract class AbstractDataFactory implements IDataFactory {

    private static final String[] EMPTY = new String[0];

    /**
     * Returns the identifiers that must be set on a request for the request to
     * be processed. If a subclass does not override this, it will return an
     * array of size zero.
     * 
     * @return the required identifiers
     */
    @Override
    public String[] getRequiredIdentifiers() {
        return EMPTY;
    }

    /**
     * Return the complete set of all valid identifiers for a request. If a
     * subclass does not override this, it will return an array of size zero.
     * 
     * @return the valid identifiers.
     */
    @Override
    public String[] getValidIdentifiers() {
        return EMPTY;
    }

    /**
     * Validates that a request is compatible with the factory
     * 
     * @param request
     *            the request to validate
     */
    public void validateRequest(IDataRequest request) {
        String[] required = getRequiredIdentifiers();
        Collection<String> missing = Collections.emptySet();
        Collection<String> invalid = Collections.emptySet();
        Map<String, Object> identifiers = request.getIdentifiers();
        if (identifiers != null && !identifiers.isEmpty()) {
            if (required != null && required.length > 0) {
                missing = new HashSet<String>(Arrays.asList(required));
                missing.removeAll(identifiers.keySet());
            }
            String[] valid = getValidIdentifiers();
            if (valid != null && valid.length > 0) {
                invalid = new HashSet<String>(identifiers.keySet());
                invalid.removeAll(Arrays.asList(valid));
            }
        } else if (required != null && required.length > 0) {
            missing = Arrays.asList(required);
        }

        if (!missing.isEmpty() || !invalid.isEmpty()) {
            throw new InvalidIdentifiersException(request.getDatatype(),
                    missing, invalid);
        }
    }

    /**
     * Default implementation throws an {@link UnsupportedOutputTypeException}
     */
    @Override
    public IGridData[] getGridData(IDataRequest request, DataTime... times) {
        throw new UnsupportedOutputTypeException(request.getDatatype(), "grid");
    }

    /**
     * Default implementation throws an {@link UnsupportedOutputTypeException}
     */
    @Override
    public IGridData[] getGridData(IDataRequest request, TimeRange timeRange) {
        throw new UnsupportedOutputTypeException(request.getDatatype(), "grid");
    }

    /**
     * Default implementation throws an {@link UnsupportedOutputTypeException}
     */
    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            DataTime... times) {
        throw new UnsupportedOutputTypeException(request.getDatatype(),
                "geometry");
    }

    /**
     * Default implementation throws an {@link UnsupportedOutputTypeException}
     */
    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            TimeRange timeRange) {
        throw new UnsupportedOutputTypeException(request.getDatatype(),
                "geometry");
    }
    
    /**
     * Default implementation throws a {@link MethodNotSupportedYetException}
     */
    @Override
    public String[] getAvailableParameters(IDataRequest request)
    {
        throw new MethodNotSupportedYetException(request.getDatatype()
                        + " data requests do not yet support getting available parameters");
    }


    /**
     * Default implementation throws a {@link MethodNotSupportedYetException}
     */
    @Override
    public Level[] getAvailableLevels(IDataRequest request)
    {
        throw new MethodNotSupportedYetException(request.getDatatype()
                + " data requests do not yet support getting available levels");
    }


}
