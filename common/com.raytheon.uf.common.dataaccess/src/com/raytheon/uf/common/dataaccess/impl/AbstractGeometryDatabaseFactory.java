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
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataaccess.IDataRequest;
import com.raytheon.uf.common.dataaccess.INotificationFilter;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataaccess.exception.TimeAgnosticDataException;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.dataaccess.geom.IGeometryData.Type;
import com.raytheon.uf.common.dataaccess.util.DatabaseQueryUtil;
import com.raytheon.uf.common.dataaccess.util.DatabaseQueryUtil.QUERY_MODE;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.TimeRange;
import org.locationtech.jts.geom.Geometry;

/**
 * Abstracts the retrieval of geometry data by running queries directly against
 * the database.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jan 29, 2013           bkowal    Initial creation
 * Feb 14, 2013  1614     bsteffen  Refactor data access framework to use single
 *                                  request.
 * Jan 14, 2014  2667     mnash     Remove getGridData methods
 * Mar 03, 2014  2673     bsteffen  Add ability to query only ref times.
 * Jul 30, 2014  3184     njensen   Added optional identifiers Overrode
 *                                  checkForInvalidIdentifiers()
 * Jan 28, 2015  4009     mapeters  Overrode getAvailableParameters(), added
 *                                  assembleGetAvailableParameters().
 * Feb 03, 2015  4009     mapeters  Overrode getAvailableLevels().
 * Mar 04, 2015  4217     mapeters  Available times are sorted in
 *                                  DataAccessLayer.
 * Mar 18, 2015  4227     mapeters  Add buildDataTimeFromQueryResults(), add
 *                                  checks for adding geom data, correctly get
 *                                  BinOffsetted times.
 * May 19, 2015  4409     mapeters  Ignore null DataTimes in executeTimeQuery().
 * Jun 29, 2015  4585     dgilling  Stop validating parameters in
 *                                  getAvailableLocationNames.
 * Aug 05, 2015  4486     rjpeter   Changed Timestamp to Date.
 * Apr 08, 2016  5553     bkowal    Ignore null identifiers in {@link
 *                                  #makeGeometries(List, String[], Map)}.
 * Apr 22, 2016  5596     tgurney   Fix getAvailableParameters() with
 *                                  unqualified table name
 * Apr 26, 2016  5587     tgurney   Support getIdentifierValues()
 * May 26, 2016  5587     njensen   assembleGetColumnValues() no longer allows
 *                                  nulls
 * Jun 07, 2016  5587     tgurney   Change get*Identifiers() to take
 *                                  IDataRequest
 * Jul 27, 2016  2416     tgurney   Stub impl of getNotificationFilter()
 * Oct 06, 2016  5926     dgilling  Add executeGetColumns.
 * Feb 19, 2018  7220     mapeters  Improve filtering of available identifier values
 * Jul 02, 2018  7327     mapeters  Prevent invalid constraints for non-table column
 *                                  identifiers when getting identifier values
 *
 * </pre>
 *
 * @author bkowal
 */

public abstract class AbstractGeometryDatabaseFactory
        extends AbstractDataFactory {

    /*
     * for now, we will assume that we will always be executing sql queries. If
     * this assumption ever becomes invalid, the type of query that will be
     * executed could be passed to the constructor or methods could be
     * overridden.
     */
    private static final QUERY_MODE queryMode = QUERY_MODE.MODE_SQLQUERY;

    protected static final String COL_NAME_OPTION = "**column name(s) of the table being queried";

    protected static final String DEFAULT_SCHEMA = "public";

    private static final String TABLE = "table";

    private final String databaseName;

    private final String[] requiredIdentifiers;

    private final String[] optionalIdentifiers;

    /**
     * Constructor
     *
     * @param databaseName
     *            the name of the database to execute queries against
     * @param requiredIdentifiers
     *            the identifiers that need to be included in the request
     *            (ifdef)
     * @param optionalIdentifiers
     *            the optional identifiers that can constrain the request
     *
     */
    public AbstractGeometryDatabaseFactory(String databaseName,
            String[] requiredIdentifiers, String[] optionalIdentifiers) {
        this.databaseName = databaseName;
        this.requiredIdentifiers = requiredIdentifiers;
        this.optionalIdentifiers = optionalIdentifiers;
    }

    @Override
    public DataTime[] getAvailableTimes(IDataRequest request,
            boolean refTimeOnly) throws TimeAgnosticDataException {
        this.validateRequest(request);
        return this.executeTimeQuery(
                this.assembleGetTimes(request, refTimeOnly), request);
    }

    @Override
    public DataTime[] getAvailableTimes(IDataRequest request,
            BinOffset binOffset) throws TimeAgnosticDataException {
        return FactoryUtil.getAvailableTimes(this, request, binOffset);
    }

    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            DataTime... times) {
        this.validateRequest(request);
        return this.executeDataQuery(this.assembleGetData(request, times),
                request);
    }

    @Override
    public IGeometryData[] getGeometryData(IDataRequest request,
            TimeRange timeRange) {
        this.validateRequest(request);
        return this.executeDataQuery(this.assembleGetData(request, timeRange),
                request);
    }

    @Override
    public String[] getIdentifierValues(IDataRequest request,
            String identifierKey) {
        List<Object[]> idValues = this.executeQuery(
                this.assembleGetIdentifierValues(request, identifierKey),
                request);
        List<String> idValStrings = new ArrayList<>(idValues.size());
        for (Object[] idValue : idValues) {
            idValStrings.add(idValue[0].toString());
        }
        return idValStrings.toArray(new String[0]);
    }

    /**
     * Runs a query to retrieve Data Times from the database.
     *
     * @param query
     *            the query to execute
     * @param request
     *            the original request that we are processing
     * @return an array of DataTimes
     */
    protected final DataTime[] executeTimeQuery(String query,
            IDataRequest request) {
        List<Object[]> results = this.executeQuery(query, request);
        List<DataTime> dataTimes = new ArrayList<>();
        for (Object[] objects : results) {
            DataTime dataTime = buildDataTimeFromQueryResults(objects);
            if (dataTime != null) {
                dataTimes.add(dataTime);
            }
        }

        return dataTimes.toArray(new DataTime[0]);
    }

    protected DataTime buildDataTimeFromQueryResults(Object[] results) {
        /*
         * verify that the object is one of the data types we are expecting.
         */
        if (results[0] instanceof Date) {
            return new DataTime((Date) results[0]);
        } else if (results[0] instanceof DataTime) {
            return (DataTime) results[0];
        } else {
            throw new DataRetrievalException("Unrecognized temporal object: "
                    + results[0].getClass().getName());
        }
    }

    /**
     * Runs a query to retrieve IGeometryData from the database.
     *
     * @param query
     *            the query to execute
     * @param request
     *            the original request that we are processing
     * @return an array of IGeometryData
     */
    protected final IGeometryData[] executeDataQuery(String query,
            IDataRequest request) {
        List<Object[]> results = this.executeQuery(query, request);
        return this.makeGeometries(results, request.getParameters(),
                request.getIdentifiers());
    }

    /**
     * Runs a query to retrieve raw data from the database.
     *
     * @param query
     *            the query to execute
     * @param request
     *            the original request that we are processing
     * @return the raw data retrieved from the database
     */
    protected final List<Object[]> executeQuery(String query,
            IDataRequest request) {
        return DatabaseQueryUtil.executeDatabaseQuery(queryMode, query,
                this.databaseName, request.getDatatype());
    }

    @Override
    public String[] getAvailableLocationNames(IDataRequest request) {
        this.validateRequest(request, false);
        List<Object[]> results = this.executeQuery(
                this.assembleGetAvailableLocationNames(request), request);
        List<String> locations = new ArrayList<>();
        for (Object[] objects : results) {
            locations.add((String) objects[0]);
        }

        Collections.sort(locations, String.CASE_INSENSITIVE_ORDER);
        return locations.toArray(new String[0]);
    }

    @Override
    public String[] getAvailableParameters(IDataRequest request) {
        this.validateRequest(request, false);
        List<Object[]> results = this.executeQuery(
                this.assembleGetAvailableParameters(request), request);
        List<String> parameters = new ArrayList<>();
        for (Object[] objects : results) {
            parameters.add((String) objects[0]);
        }
        return parameters.toArray(new String[0]);
    }

    @Override
    public Level[] getAvailableLevels(IDataRequest request) {
        throw new IncompatibleRequestException(request.getDatatype()
                + " data does not support the concept of levels");
    }

    @Override
    public String[] getRequiredIdentifiers(IDataRequest request) {
        return this.requiredIdentifiers;
    }

    @Override
    public String[] getOptionalIdentifiers(IDataRequest request) {
        return this.optionalIdentifiers;
    }

    /*
     * invoked to build the queries that will be executed.
     */

    /**
     * Builds a query that will be used to retrieve time from the database based
     * on the provided request.
     *
     * @param request
     *            the original request that we are processing
     * @param refTimeOnly
     *            true if only unique refTimes should be returned(without a
     *            forecastHr)
     *
     * @return the query
     */
    protected abstract String assembleGetTimes(IDataRequest request,
            boolean refTimeOnly);

    /**
     * Builds a query used to retrieve data from the database based on the
     * provided request and a list of DataTimes.
     *
     * @param request
     *            the original request that we are processing
     * @param times
     *            DataTimes to use when building the query; will most likely
     *            manifest as constraints
     * @return the query
     */
    protected abstract String assembleGetData(IDataRequest request,
            DataTime... times);

    /**
     * Builds a query used to retrieve data from the database based on the
     * provided request and the specified TimeRange.
     *
     * @param request
     *            the original request that we are processing
     * @param timeRange
     *            a TimeRange to use when building the query; will most likely
     *            manifest as a BETWEEN constraint
     * @return the query
     */
    protected abstract String assembleGetData(IDataRequest request,
            TimeRange timeRange);

    /**
     * Builds a query used to retrieve location information from the database
     * based on the provided request
     *
     * @param request
     *            the original request that we are processing
     * @return the query
     */
    protected abstract String assembleGetAvailableLocationNames(
            IDataRequest request);

    /**
     * Builds a query used to retrieve all valid identifier values based on the
     * provided request and identifier
     *
     * @param request
     *            the original request that we are processing
     * @param identifierKey
     *            the identifier to retrieve allowed values for
     * @return the query
     */
    protected String assembleGetIdentifierValues(IDataRequest request,
            String identifierKey) {
        String query;
        if (identifierKey.equals(TABLE)) {
            query = assembleGetTableNames();
        } else {
            query = assembleGetColumnValues(request, extractTableName(request),
                    identifierKey);
        }
        return query;
    }

    /**
     * Builds a postgres-specific query used to retrieve the names of all tables
     * in the current database, excluding information_schema and system
     * catalogs. Each table name is qualified with the name of its schema, even
     * if all are in schema 'public'.
     *
     * @return the query
     */
    protected String assembleGetTableNames() {
        StringBuilder query = new StringBuilder();
        query.append("select table_schema || '.' || table_name t ")
                .append("from information_schema.tables ")
                .append("where table_schema <> 'information_schema' ")
                .append("and table_schema not like 'pg_%' ")
                .append("order by t;");
        return query.toString();
    }

    /**
     * Builds a query used to retrieve all values in a single column of a table.
     *
     * @param tableName
     *            name of the table
     * @param columnName
     *            name of the column
     * @return the query
     */
    protected String assembleGetColumnValues(IDataRequest request,
            String tableName, String columnName) {
        StringBuilder sql = new StringBuilder("select distinct ");
        sql.append(columnName).append(" from ").append(tableName);
        sql.append(" where ").append(columnName).append(" is not null");

        for (Map.Entry<String, Object> entry : request.getIdentifiers()
                .entrySet()) {
            String key = entry.getKey();
            if (!isColumnIdentifier(key)) {
                continue;
            }
            Object value = entry.getValue();

            RequestConstraint rc;
            if (value instanceof RequestConstraint) {
                rc = (RequestConstraint) value;
            } else {
                rc = new RequestConstraint(value.toString());
            }

            sql.append(" and ").append(key).append(rc.toSqlString());
        }

        sql.append(" order by " + columnName + ";");

        return sql.toString();
    }

    /**
     * Determine if the given identifier is a column in the database table being
     * queried.
     *
     * @param identifier
     *            the identifier name
     * @return true if the identifier is a table column, false otherwise
     */
    protected boolean isColumnIdentifier(String identifier) {
        return !TABLE.equals(identifier);
    }

    /**
     * Extracts and returns value of "table" identifier from request. Never
     * returns null; throws exception instead.
     *
     * @param request
     *            the original request that we are processing
     * @return the table name
     * @throws IncompatibleRequestException
     *             if value for identifier "table" is null or empty
     */
    protected String extractTableName(IDataRequest request) {
        String tableName = (String) request.getIdentifiers().get(TABLE);
        if (tableName == null || tableName.isEmpty()) {
            throw new IncompatibleRequestException(
                    "You must provide a non-null, non-empty value for "
                            + "identifier 'table'");
        }
        return tableName;
    }

    /**
     * Extracts value of "table" identifier from request. Returns array of two
     * strings: schema name and table name. If the value of "table" in the
     * request does not include the schema, the returned schema will be the
     * default schema.
     *
     * @param request
     *            the original request that we are processing
     * @return array containing schema name and table name
     * @throws IncompatibleRequestException
     *             if value for identifier "table" is null or empty
     */
    protected String[] splitTableName(IDataRequest request) {
        String tableNameQualified = extractTableName(request);
        String[] tableParsed = tableNameQualified.split("\\.");
        String tableName = null;
        String schema = DEFAULT_SCHEMA;
        if (tableParsed.length == 1) {
            tableName = tableParsed[0];
        } else if (tableParsed.length == 2) {
            schema = tableParsed[0];
            tableName = tableParsed[1];
        } else {
            throw new IncompatibleRequestException(
                    tableNameQualified + " is not a valid table");
        }
        return new String[] { schema, tableName };
    }

    /**
     * Builds a postgres-specific SQL query used to retrieve available
     * parameters (columns) of the requested table from the database.
     *
     * @param request
     *            the request that we are processing
     * @return the query
     */
    protected String assembleGetAvailableParameters(IDataRequest request) {
        String[] table = splitTableName(request);
        String schema = table[0];
        String tableName = table[1];
        return String.format(
                "select column_name " + "from information_schema.columns "
                        + "where table_schema = '%s' and table_name = '%s';",
                schema, tableName);
    }

    /**
     * Builds the data objects that will be returned by calls to getData() on
     * the factory
     *
     * @param serverResult
     *            the results from the query run on the server
     * @param paramNames
     *            the names of the parameters that were requested
     * @param identifiers
     *            the identifiers from the data request
     * @return the IGeometryData based on the results of the query
     */
    protected IGeometryData[] makeGeometries(List<Object[]> serverResult,
            String[] paramNames, Map<String, Object> identifiers) {
        List<IGeometryData> resultList = new ArrayList<>();
        Map<String, Object> attrs = Collections.emptyMap();
        if (identifiers != null) {
            attrs = Collections.unmodifiableMap(identifiers);
        }

        // loop over each db row
        for (Object[] row : serverResult) {
            IGeometryData geom = this.makeGeometry(row, paramNames, attrs);
            if (geom != null) {
                resultList.add(geom);
            }
        }

        return resultList.toArray(new DefaultGeometryData[0]);
    }

    /**
     * Constructs a single IGeometryData
     *
     * @param data
     *            the raw data associated with a single row retrieved from the
     *            database
     * @param paramNames
     *            the parameters specified in the original IDataRequest
     * @param attrs
     *            the identifiers specified in the original IDataRequest
     * @return the constructed IGeometryData
     */
    protected abstract IGeometryData makeGeometry(Object[] data,
            String[] paramNames, Map<String, Object> attrs);

    /**
     * Constructs a DefaultGeometryData based on the provided information
     *
     * @param time
     *            the provided DataTime
     * @param level
     *            the provided Level
     * @param geometry
     *            the provided Geometry
     * @param locationName
     *            the provided Location
     * @param attributes
     *            the identifiers specified in the original IDataRequest
     * @param paramNames
     *            the parameters specified in the original IDataRequest
     * @return the constructed DefaultGeometryData
     */
    protected DefaultGeometryData buildGeometryData(DataTime time, Level level,
            Geometry geometry, String locationName,
            Map<String, Object> attributes, String[] paramNames) {
        return this.buildGeometryData(time, level, geometry, locationName,
                attributes, Integer.MAX_VALUE, null, paramNames);
    }

    /**
     * Constructs a DefaultGeometryData based on the provided information
     *
     * @param time
     *            the provided DataTime
     * @param level
     *            the provided Level
     * @param geometry
     *            the provided Geometry
     * @param locationName
     *            the provided Location
     * @param attributes
     *            identifiers specified in the original IDataRequest
     * @param dataIndex
     *            a numerical index indicating where user-specified parameters
     *            may start in the provided row of raw data
     * @param data
     *            a row of row data retrieved from the database; all
     *            user-specified parameters are extracted from it and added to
     *            the DefaultGeometryData using the addData method
     * @param paramNames
     *            the parameters specified in the original IDataRequest
     * @return the constructed DefaultGeometryData
     */
    protected DefaultGeometryData buildGeometryData(DataTime time, Level level,
            Geometry geometry, String locationName,
            Map<String, Object> attributes, int dataIndex, Object[] data,
            String[] paramNames) {
        DefaultGeometryData geometryData = new DefaultGeometryData();
        geometryData.setDataTime(time);
        geometryData.setLevel(level);
        geometryData.setGeometry(geometry);
        geometryData.setLocationName(locationName);
        geometryData.setAttributes(attributes);
        if (data != null && data.length > dataIndex) {
            for (int i = dataIndex; i < data.length; i++) {
                String name = paramNames[i - dataIndex];
                Object dataItem = data[i];
                if (dataItem instanceof Calendar) {
                    dataItem = ((Calendar) dataItem).getTimeInMillis();
                    geometryData.addData(name, dataItem, Type.LONG);
                } else if (dataItem instanceof Date) {
                    dataItem = ((Date) dataItem).getTime();
                    geometryData.addData(name, dataItem, Type.LONG);
                } else {
                    geometryData.addData(name, dataItem);
                }
            }
        }

        return geometryData;
    }

    @Override
    protected Collection<String> checkForInvalidIdentifiers(
            IDataRequest request) {
        /*
         * Specifically do not validate identifiers since the current subclass
         * implementations allow column names of specific tables, i.e. optional
         * identifiers are semi-undefined based on database tables
         */
        return Collections.emptyList();
    }

    /**
     * Queries the the table specified by the given request to determine which
     * of the given columnsToCheck it contains.
     *
     * @param columnsToCheck
     *            the columns to check the existence of
     * @param request
     *            the IDataRequest being performed
     * @return the names of the columns in columnsToCheck that the table has
     */
    protected Collection<String> executeGetColumnNames(String[] columnsToCheck,
            IDataRequest request) {
        StringBuilder existenceQuery = new StringBuilder(
                "select column_name from information_schema.columns where column_name in (");
        boolean first = true;
        for (String columnName : columnsToCheck) {
            if (!first) {
                existenceQuery.append(", ");
            } else {
                first = false;
            }
            existenceQuery.append("'").append(columnName).append("'");
        }

        // Extract the table name (ignore schema since there is only 1)
        String[] tableNameParts = splitTableName(request);
        String table = tableNameParts[1];
        existenceQuery.append(") and table_name = '").append(table)
                .append("';");
        List<Object[]> results = this.executeQuery(existenceQuery.toString(),
                request);

        Collection<String> foundColumns = new ArrayList<>(results.size());
        for (Object[] result : results) {
            foundColumns.add(result[0].toString());
        }
        return foundColumns;
    }

    @Override
    public INotificationFilter getNotificationFilter(IDataRequest request) {
        throw new IncompatibleRequestException("Cannot listen for updates to "
                + request.getDatatype() + " data");
    }

}
