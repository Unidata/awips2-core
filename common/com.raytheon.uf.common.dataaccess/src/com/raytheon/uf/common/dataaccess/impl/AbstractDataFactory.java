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
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.exception.InvalidIdentifiersException;
import com.raytheon.uf.common.dataaccess.exception.MethodNotSupportedYetException;
import com.raytheon.uf.common.dataaccess.exception.UnsupportedOutputTypeException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.util.SizeUtil;

/**
 *
 * An abstract data factory that can be used by implementing IGridDataFactories
 * or IGeometryDataFactories.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 13, 2012           njensen   Initial creation
 * Feb 14, 2013  1614     bsteffen  Refactor data access framework to use single
 *                                  request.
 * Feb 19, 2012  1552     mpduff    Implement IDataFactory.
 * Jan 14, 2014  2667     mnash     Change getGridData and getGeometryData
 *                                  methods to throw exception by default
 * Jul 14, 2014  3184     njensen   Added getAvailableParameters() and
 *                                  getAvailableLevels()
 * Jul 30, 2014  3184     njensen   Refactored validateRequest()
 * Jul 31, 2014  3184     njensen   Added validateParameters()
 * Jan 28, 2014  4009     mapeters  Added validateRequest() with boolean
 *                                  parameter
 * Feb 10, 2014  2866     nabowle   add MAX_RESPONSE_SIZE for limiting response
 *                                  sizes.
 * Apr 13, 2016  5379     tgurney   Add default impl for getIdentifierValues()
 * Jun 07, 2016  5587     tgurney   Change get*Identifiers() to take
 *                                  IDataRequest
 * Aug 04, 2016  2416     tgurney   Prevent dataURI constraint being combined
 *                                  with other constraints.
 * Jan 20, 2017  6095     tgurney   Fix NPE in validateRequest
 * </pre>
 *
 * @author njensen
 */

public abstract class AbstractDataFactory implements IDataFactory {
    /** Property used to specify the maximum response size in MB. */
    public static final String RESPONSE_PROP = "edex.requestsrv.byteLimitInMB";

    /** The maximum response size, in bytes. */
    public static final long MAX_RESPONSE_SIZE = Long.getLong(RESPONSE_PROP,
            100L) * SizeUtil.BYTES_PER_MB;

    protected static final String[] EMPTY = new String[0];

    /**
     * Returns the identifiers that must be set on a request for the request to
     * be processed. If a subclass does not override this, it will return an
     * array of size zero.
     *
     * @return the required identifiers
     */
    @Override
    public String[] getRequiredIdentifiers(IDataRequest request) {
        return EMPTY;
    }

    /**
     * Return the set of optional identifiers for a request. If a subclass does
     * not override this, it will return an array of size zero.
     *
     * @return the valid identifiers.
     */
    @Override
    public String[] getOptionalIdentifiers(IDataRequest request) {
        return EMPTY;
    }

    /**
     * Validates that a request is compatible with the factory, including
     * validating existence of parameters
     *
     * @param request
     *            the request to validate
     */
    public void validateRequest(IDataRequest request) {
        validateRequest(request, true);
    }

    /**
     * Validate that a request is compatible with the factory
     *
     * @param request
     *            the request to validate
     * @param validateParameters
     *            true if request must have parameters, false otherwise
     */
    public void validateRequest(IDataRequest request,
            boolean validateParameters) {
        if (validateParameters) {
            validateParameters(request);
        }
        Collection<String> missing = checkForMissingIdentifiers(request);
        Collection<String> invalid = checkForInvalidIdentifiers(request);
        if (!missing.isEmpty() || !invalid.isEmpty()) {
            throw new InvalidIdentifiersException(request.getDatatype(),
                    missing, invalid);
        }
        if (request.getIdentifiers() != null
                && request.getIdentifiers()
                        .containsKey(PluginDataObject.DATAURI_ID)
                && request.getIdentifiers().size() > 1) {
            throw new IncompatibleRequestException("Cannot specify "
                    + PluginDataObject.DATAURI_ID + " with other identifiers");
        }
    }

    /**
     * Checks for missing identifiers that are required to be on the request
     *
     * @param request
     * @return a collection of missing identifiers
     */
    protected Collection<String> checkForMissingIdentifiers(
            IDataRequest request) {
        String[] required = getRequiredIdentifiers(request);
        Collection<String> missing = Collections.emptySet();
        Map<String, Object> identifiers = request.getIdentifiers();
        if (identifiers != null && !identifiers.isEmpty()) {
            if (required != null && required.length > 0) {
                missing = new HashSet<>(Arrays.asList(required));
                missing.removeAll(identifiers.keySet());
            }
        } else if (required != null && required.length > 0) {
            missing = Arrays.asList(required);
        }
        return missing;
    }

    /**
     * Checks for invalid identifiers that are not compatible with the request
     *
     * @param request
     * @return a collection of invalid identifiers
     */
    protected Collection<String> checkForInvalidIdentifiers(
            IDataRequest request) {
        Collection<String> invalid = Collections.emptySet();
        Map<String, Object> identifiers = request.getIdentifiers();
        if (identifiers != null && !identifiers.isEmpty()) {
            String[] optional = getOptionalIdentifiers(request);
            String[] required = getRequiredIdentifiers(request);
            if (optional != null && optional.length > 0
                    || required != null && required.length > 0) {
                invalid = new HashSet<>(identifiers.keySet());
                if (optional != null) {
                    invalid.removeAll(Arrays.asList(optional));
                }
                if (required != null) {
                    invalid.removeAll(Arrays.asList(required));
                }
            }
        }
        return invalid;
    }

    /**
     * Validates that the parameters are ok
     *
     * @param request
     */
    protected void validateParameters(IDataRequest request)
            throws IncompatibleRequestException {
        /*
         * Note that getAvailableParameters() implementations should not call
         * this (they should pass false to validateRequest()).
         */
        if (request.getParameters() == null
                || request.getParameters().length == 0) {
            throw new IncompatibleRequestException("Requests of "
                    + request.getDatatype()
                    + " data must have at least one parameter specified");
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
    public String[] getAvailableParameters(IDataRequest request) {
        throw new MethodNotSupportedYetException(request.getDatatype()
                + " data requests do not yet support getting available parameters");
    }

    /**
     * Default implementation throws a {@link MethodNotSupportedYetException}
     */
    @Override
    public Level[] getAvailableLevels(IDataRequest request) {
        throw new MethodNotSupportedYetException(request.getDatatype()
                + " data requests do not yet support getting available levels");
    }

    /**
     * Default implementation throws a {@link MethodNotSupportedYetException}
     */
    @Override
    public String[] getIdentifierValues(IDataRequest request,
            String identifierKey) {
        throw new MethodNotSupportedYetException(request.getDatatype()
                + " data requests do not yet support getting identifier values");
    }
}
