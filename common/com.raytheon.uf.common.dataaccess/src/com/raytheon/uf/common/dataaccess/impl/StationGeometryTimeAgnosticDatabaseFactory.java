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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;

/**
 * {@code IDataFactory} implementation for retrieving geometry data and metadata
 * from station database tables. Data will be time and level agnostic.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 24, 2015  #4585     dgilling     Initial creation
 * 
 * </pre>
 * 
 * @author dgilling
 * @version 1.0
 */

public class StationGeometryTimeAgnosticDatabaseFactory extends
        AbstractGeometryTimeAgnosticDatabaseFactory {

    /*
     * we don't want to let them use the_geom as a parameter since it will come
     * back in the IGeometryData object.
     */
    private static final String GET_PARAMETERS_QUERY = "select column_name from information_schema.columns where table_schema = '%s' and table_name = '%s' and column_name != '%s';";

    private final String schemaName;

    private final String tableName;

    private final String geometryColumn;

    private final String locationColumn;

    private final ThreadLocal<WKBReader> wkbReader = new ThreadLocal<WKBReader>() {
        @Override
        protected WKBReader initialValue() {
            return new WKBReader();
        }
    };

    public StationGeometryTimeAgnosticDatabaseFactory(String schemaName,
            String databaseName, String tableName, String geometryColumn,
            String locationColumn) {
        super(databaseName, EMPTY, new String[] { COL_NAME_OPTION });
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.geometryColumn = geometryColumn;
        this.locationColumn = locationColumn;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.raytheon.uf.common.dataaccess.impl.
     * AbstractGeometryTimeAgnosticDatabaseFactory
     * #assembleGetData(com.raytheon.uf.common.dataaccess.IDataRequest)
     */
    @Override
    protected String assembleGetData(IDataRequest request) {
        return assembleQuery(request, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.dataaccess.impl.AbstractGeometryDatabaseFactory
     * #assembleGetAvailableLocationNames
     * (com.raytheon.uf.common.dataaccess.IDataRequest)
     */
    @Override
    protected String assembleGetAvailableLocationNames(IDataRequest request) {
        return assembleQuery(request, false);
    }

    @Override
    protected String assembleGetAvailableParameters(IDataRequest request) {
        return String.format(GET_PARAMETERS_QUERY, schemaName, tableName,
                geometryColumn);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.common.dataaccess.impl.AbstractGeometryDatabaseFactory
     * #makeGeometry(java.lang.Object[], java.lang.String[], java.util.Map)
     */
    @Override
    protected IGeometryData makeGeometry(Object[] data, String[] paramNames,
            Map<String, Object> attrs) {
        Object geomWKB = data[0];
        if (!(geomWKB instanceof byte[])) {
            throw new DataRetrievalException(
                    "Retrieved Geometry was not the expected type; was expecting byte[], received: "
                            + geomWKB.getClass().getName());
        }
        Geometry geometry = null;
        try {
            geometry = (wkbReader.get()).read((byte[]) geomWKB);
        } catch (ParseException e) {
            throw new DataRetrievalException("Failed to parse the geometry.", e);
        }

        String location = (String) data[1];

        return super.buildGeometryData(null, null, geometry, location, attrs,
                2, data, paramNames);
    }

    private String assembleQuery(IDataRequest request, boolean dataQuery) {
        StringBuilder query = new StringBuilder("SELECT ");

        // the first column will always be the geometry.
        if (dataQuery) {
            query.append("AsBinary(").append(geometryColumn).append("), ");
        }

        // location field
        if (!dataQuery) {
            query.append("DISTINCT ");
        }
        query.append(locationColumn);

        // additional parameters
        if (dataQuery && (request.getParameters() != null)) {
            for (String parameter : request.getParameters()) {
                query.append(", ").append(parameter);
            }
        }

        query.append(" FROM ").append(tableName);

        List<String> constraints = new ArrayList<>();
        if (!dataQuery) {
            constraints.add(locationColumn + " IS NOT NULL");
        } else {
            if ((request.getLocationNames() != null)
                    && (request.getLocationNames().length > 0)) {
                constraints.add(buildInConstraint(locationColumn,
                        request.getLocationNames()));
            }
        }
        if (request.getEnvelope() != null) {
            constraints.add(buildGeospatialConstraint(geometryColumn,
                    request.getEnvelope()));
        }
        if (request.getIdentifiers() != null) {
            for (Entry<String, Object> entry : request.getIdentifiers()
                    .entrySet()) {
                String identifier = entry.getKey();
                Object value = entry.getValue();
                if (value instanceof Object[]) {
                    Object[] array = (Object[]) value;
                    constraints.add(buildInConstraint(identifier, array));
                } else if (value instanceof Collection) {
                    Collection<?> collection = (Collection<?>) value;
                    constraints.add(buildInConstraint(identifier,
                            collection.toArray()));
                } else {
                    constraints.add(buildEqualsConstraint(identifier,
                            value.toString()));
                }
            }
        }

        if (!constraints.isEmpty()) {
            query.append(" WHERE ");
        }

        // add constraints
        Iterator<String> iter = constraints.iterator();
        query.append(iter.next());
        while (iter.hasNext()) {
            query.append(" AND ");
            query.append(iter.next());
        }

        query.append(';');
        return query.toString();
    }

    private static String buildEqualsConstraint(String key, String value) {
        StringBuilder stringBuilder = new StringBuilder(key);
        stringBuilder.append(" = '");
        stringBuilder.append(value);
        stringBuilder.append("'");
        return stringBuilder.toString();
    }

    private static String buildInConstraint(String fieldName, Object[] elements) {
        StringBuilder stringBuilder = new StringBuilder(fieldName);
        stringBuilder.append(" IN ('");

        // add the 0th location
        stringBuilder.append(elements[0]);
        stringBuilder.append('\'');
        for (int i = 1; i < elements.length; i++) {
            stringBuilder.append(", '");
            stringBuilder.append(elements[i]);
            stringBuilder.append('\'');
        }
        stringBuilder.append(')');

        return stringBuilder.toString();
    }

    private static String buildGeospatialConstraint(String geomField,
            Envelope env) {
        StringBuilder constraint = new StringBuilder(geomField);
        constraint.append(" && ST_SetSrid(");
        constraint.append(String.format("'BOX3D(%f %f, %f %f)'::box3d",
                env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()));
        constraint.append(", 4326)");
        return constraint.toString();
    }
}
