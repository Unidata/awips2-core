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
package com.raytheon.uf.common.dataplugin.maps.dataaccess;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataplugin.maps.dataaccess.util.MapsQueryUtil;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import org.locationtech.jts.geom.Envelope;

/**
 * Constructs a query to retrieve information from the maps database based on
 * the supplied information.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 28, 2013           bkowal    Initial creation
 * Feb 14, 2013  1614     bsteffen  Refactor data access framework to use single
 *                                  request.
 * Oct 30, 2013           mnash     Allow for no parameters to be set.
 * Aug 31, 2015  4569     mapeters  Alias automatically retrieved location
 *                                  columns.
 * Jun 10, 2016  5574     mapeters  Add advanced query support.
 * Jul 05, 2016  5728     mapeters  Use RequestConstraint to build IN
 *                                  constraints
 * 
 * </pre>
 * 
 * @author bkowal
 */

public class MapsQueryAssembler {
    public static final class REQUIRED_IDENTIFIERS {
        /*
         * The table to retrieve the data from.
         */
        public static final String IDENTIFIER_TABLE = "table";

        /*
         * The first field that will be selected - the geometry that we would
         * like to retrieve. We will verify that it is not already in the column
         * list.
         */
        public static final String IDENTIFIER_GEOM_FIELD = "geomField";
    }

    /*
     * Other common identifiers we may encounter.
     */
    public static final class IDENTIFIERS {
        /*
         * Used to specify if the factory should look for information that is
         * within the specified locations or information that excludes the
         * specified locations. If this identifier is not specified, the default
         * will be to look for information within the specified location.
         */
        public static final String IDENTIFIER_IN_LOCATION = "inLocation";

        /*
         * The name of the location field, defaults to "name".
         */
        public static final String IDENTIFIER_LOCATION_FIELD = "locationField";
    }

    private static final List<String> RESERVED_IDENTIFIERS = Arrays.asList(
            REQUIRED_IDENTIFIERS.IDENTIFIER_TABLE,
            REQUIRED_IDENTIFIERS.IDENTIFIER_GEOM_FIELD,
            IDENTIFIERS.IDENTIFIER_IN_LOCATION,
            IDENTIFIERS.IDENTIFIER_LOCATION_FIELD);

    /**
     * Constructor
     */
    private MapsQueryAssembler() {
    }

    /**
     * Retrieves a named identifier from the request that must be provided as a
     * string
     * 
     * @param request
     *            the original request that we are processing
     * @param identifierName
     *            the name of the identifier to extract
     * @return the identifier
     */
    private static String extractStringIdentifier(IDataRequest request,
            String identifierName) {
        Object identifier = request.getIdentifiers().get(identifierName);
        if (identifier instanceof String) {
            return (String) identifier;
        }

        throw new IncompatibleRequestException("'" + identifier.toString()
                + "' is not a valid identifier value for " + identifierName);
    }

    /**
     * Retrieves the table identifier
     * 
     * @param request
     *            the original request that we are processing
     * @return the table identifier
     */
    private static String extractTable(IDataRequest request) {
        return extractStringIdentifier(request,
                REQUIRED_IDENTIFIERS.IDENTIFIER_TABLE);
    }

    /**
     * Retrieves the geometry field identifier
     * 
     * @param request
     *            the original request that we are processing
     * @return the geometry identifier
     */
    private static String extractGeomField(IDataRequest request) {
        return extractStringIdentifier(request,
                REQUIRED_IDENTIFIERS.IDENTIFIER_GEOM_FIELD);
    }

    /**
     * Constructs a query to retrieve data from the maps database
     * 
     * @param request
     *            the original request that we are processing
     * @return the query
     */
    public static String assembleGetData(IDataRequest request) {
        return assembleQuery(request, Boolean.FALSE);
    }

    /**
     * Constructs a query to retrieve locations from the database
     * 
     * @param request
     *            the original request that we are processing
     * @return the query
     */
    public static String assembleGetAvailableLocationNames(IDataRequest request) {
        return assembleQuery(request, Boolean.TRUE);
    }

    /**
     * Constructs a query to retrieve information from the database
     * 
     * @param request
     *            the original request that we are processing
     * @param locationQuery
     *            indicates whether or not this query will be used to retrieve
     *            location information
     * @return the query
     */
    private static String assembleQuery(IDataRequest request,
            boolean locationQuery) {
        Envelope envelope = request.getEnvelope();
        String table = extractTable(request);
        String geomField = extractGeomField(request);

        List<String> columns = new ArrayList<>();
        if (locationQuery == false) {
            /*
             * The first column will always be the geometry. We don't need to
             * alias it because it can't be requested as a parameter since it
             * must be converted to binary.
             */
            columns.add("ST_AsBinary(" + geomField + ")");
        }

        String locationField = null;
        if (request.getIdentifiers().containsKey(
                IDENTIFIERS.IDENTIFIER_LOCATION_FIELD)) {
            /*
             * The second column will always be the location name. We must alias
             * it in case it is also included as a parameter (to make the two
             * differentiable).
             */
            locationField = extractStringIdentifier(request,
                    IDENTIFIERS.IDENTIFIER_LOCATION_FIELD);
            StringBuilder locationFieldAliased = new StringBuilder(
                    locationField).append(" as ")
                    .append(IDENTIFIERS.IDENTIFIER_LOCATION_FIELD)
                    .append(locationField);
            columns.add(locationFieldAliased.toString());
        }
        if (locationQuery == false) {
            /*
             * add any additional database columns the user has specified as
             * parameters
             */
            if (request.getParameters() != null) {
                for (String parameter : request.getParameters()) {
                    columns.add(parameter);
                }
            }
        }
        List<String> constraints = new ArrayList<>();
        // add location constraint (ifdef)
        if (request.getLocationNames() != null
                && request.getLocationNames().length > 0) {
            boolean inLocation = Boolean.TRUE;
            if (request.getIdentifiers().containsKey(
                    IDENTIFIERS.IDENTIFIER_IN_LOCATION)) {
                inLocation = BooleanUtils.toBoolean(request.getIdentifiers()
                        .get(IDENTIFIERS.IDENTIFIER_IN_LOCATION).toString());
            }

            if (locationField != null) {
                // Add IN or NOT IN constraint
                RequestConstraint rc = new RequestConstraint(
                        request.getLocationNames(), inLocation);
                constraints.add(locationField + rc.toSqlString());
            }
        }
        // add remaining identifiers to constraints (ifdef)
        for (Map.Entry<String, Object> entry : request.getIdentifiers()
                .entrySet()) {
            String key = entry.getKey();
            if (RESERVED_IDENTIFIERS.contains(key)) {
                continue;
            }

            Object value = entry.getValue();
            RequestConstraint requestConstraint;
            if (value instanceof RequestConstraint) {
                requestConstraint = (RequestConstraint) value;
            } else {
                requestConstraint = new RequestConstraint(value.toString());
            }

            constraints.add(key + requestConstraint.toSqlString());
        }

        return MapsQueryUtil.assembleMapsTableQuery(envelope, columns,
                constraints, table, geomField);
    }
}