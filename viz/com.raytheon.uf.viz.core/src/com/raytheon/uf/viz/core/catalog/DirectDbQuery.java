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

package com.raytheon.uf.viz.core.catalog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.dataquery.db.QueryResultRow;
import com.raytheon.uf.common.dataquery.requests.QlServerRequest;
import com.raytheon.uf.common.dataquery.requests.QlServerRequest.QueryType;
import com.raytheon.uf.common.dataquery.requests.SaveOrUpdateRequest;
import com.raytheon.uf.common.message.response.AbstractResponseMessage;
import com.raytheon.uf.common.message.response.ResponseMessageError;
import com.raytheon.uf.common.message.response.ResponseMessageGeneric;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.exception.VizServerSideException;
import com.raytheon.uf.viz.core.requests.ThriftClient;

/**
 * Executes an arbitrary hql or sql query. Also contains functionality to insert
 * or update an object in a database
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 10/15/2008   1615       bphillip    Initial Creation
 * 12/11/2008   1777       bphillip    Added insert/update functionality
 * Nov 08, 2013 2361       njensen     Refactored/improved saveOrUpdateList()
 * Jul 13, 2015 4500       rjpeter     Fix SQL Injection concerns.
 * </pre>
 * 
 * @author bphillip
 * @version 1.0
 */
public class DirectDbQuery {

    /** The language of the query */
    public static enum QueryLanguage {
        SQL, HQL
    };

    /** The hql Query */
    private final String query;

    /** The database name */
    private final String database;

    /** The language the query is written in */
    private final QueryLanguage queryLanguage;

    /**
     * Parameter to value map
     */
    private final Map<String, Object> paramMap;

    /**
     * Executes a database query
     * 
     * @param query
     *            The query
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @return The results
     * @throws VizException
     *             If the query fails
     */
    public static List<Object[]> executeQuery(String query, String database,
            QueryLanguage language) throws VizException {
        return executeQuery(query, database, language, null);
    }

    /**
     * Executes a database query
     * 
     * @param query
     *            The query
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @param paramMap
     *            The parameter mapping
     * @return The results
     * @throws VizException
     *             If the query fails
     */
    public static List<Object[]> executeQuery(String query, String database,
            QueryLanguage language, Map<String, Object> paramMap)
            throws VizException {
        return new DirectDbQuery(query, database, language, paramMap)
                .performQuery();
    }

    /**
     * Executes a database query. The results are returned in a QueryResult
     * object
     * 
     * @param query
     *            The query
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @return The results
     * @throws VizException
     *             If the query fails
     */
    public static QueryResult executeMappedQuery(String query, String database,
            QueryLanguage language) throws VizException {
        return executeMappedQuery(query, database, language, null);
    }

    /**
     * Executes a database query. The results are returned in a QueryResult
     * object
     * 
     * @param query
     *            The query
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @param paramMap
     *            The parameter mapping
     * @return The results
     * @throws VizException
     *             If the query fails
     */
    public static QueryResult executeMappedQuery(String query, String database,
            QueryLanguage language, Map<String, Object> paramMap)
            throws VizException {
        return new DirectDbQuery(query, database, language, paramMap)
                .performMappedQuery();
    }

    /**
     * Executes a non-query database statement. The number of rows modified is
     * returned.
     * 
     * @param statement
     *            The statement
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @return The number of rows modified
     * @throws VizException
     *             If the statement fails
     */
    public static int executeStatement(String statement, String database,
            QueryLanguage language) throws VizException {
        return new DirectDbQuery(statement, database, language, null)
                .performStatement();
    }

    /**
     * Executes a non-query database statement. The number of rows modified is
     * returned.
     * 
     * @param statement
     *            The statement
     * @param database
     *            The database name
     * @param language
     *            The query language
     * @param paramMap
     *            The parameter mapping
     * @return The number of rows modified
     * @throws VizException
     *             If the statement fails
     */
    public static int executeStatement(String statement, String database,
            QueryLanguage language, Map<String, Object> paramMap)
            throws VizException {
        return new DirectDbQuery(statement, database, language, null)
                .performStatement();
    }

    /**
     * Saves or updates an object into the specified database
     * 
     * @param obj
     *            The object to save or update
     * @param database
     *            The database in which to modify
     * @return The number of objects inserted/updated
     * @throws VizException
     *             If errors occur
     */
    public static int saveOrUpdate(Object obj, String database)
            throws VizException {
        List<Object> objList = new ArrayList<Object>(1);
        objList.add(obj);
        return saveOrUpdate(objList, database);
    }

    /**
     * Saves or updates an object into the specified database
     * 
     * @param objList
     *            The list of objects to save or update
     * @param database
     *            The database in which to modify
     * @return The number of objects inserted/updated
     * @throws VizException
     *             If errors occur
     */
    public static int saveOrUpdate(List<Object> objList, String database)
            throws VizException {
        return new DirectDbQuery().saveOrUpdateList(objList, database);
    }

    /**
     * Constructs a new DirectDbQuery
     * 
     * @param query
     *            The query
     * @param database
     *            The database
     * @param language
     *            The query language
     */
    private DirectDbQuery() {
        this.query = null;
        this.database = null;
        this.queryLanguage = null;
        this.paramMap = null;
    }

    /**
     * Constructs a new DirectDbQuery
     * 
     * @param query
     *            The query
     * @param database
     *            The database
     * @param language
     *            The query language
     */
    private DirectDbQuery(String query, String database,
            QueryLanguage language, Map<String, Object> paramMap) {
        this.query = query;
        this.database = database;
        this.queryLanguage = language;
        this.paramMap = paramMap;
    }

    /**
     * Performs the mapped query
     * 
     * @return The results
     * @throws VizException
     *             If the query fails
     */
    private QueryResult performMappedQuery() throws VizException {

        if (database == null) {
            throw new VizException("Database not specified for query");
        }
        if (query == null) {
            throw new VizException("Cannot execute null query");
        }

        QlServerRequest request = new QlServerRequest(query);
        request.setDatabase(database);
        request.setType(QueryType.QUERY);
        request.setParamMap(paramMap);

        // set the mode so the handler knows what to do
        if (queryLanguage == null) {
            throw new VizException("Query language not specified");
        } else if (queryLanguage.equals(QueryLanguage.HQL)) {
            request.setLang(QlServerRequest.QueryLanguage.HQL);
        } else {
            request.setLang(QlServerRequest.QueryLanguage.SQL);
        }

        // create request object
        QueryResult retVal = null;
        // get result
        AbstractResponseMessage response = (AbstractResponseMessage) ThriftClient
                .sendRequest(request);

        if (response instanceof ResponseMessageGeneric) {
            retVal = (QueryResult) ((ResponseMessageGeneric) response)
                    .getContents();

        } else if (response instanceof ResponseMessageError) {
            ResponseMessageError rme = (ResponseMessageError) response;
            VizServerSideException innerException = new VizServerSideException(
                    rme.toString());
            throw new VizServerSideException(rme.getErrorMsg(), innerException);
        }

        return retVal;
    }

    /**
     * Performs the query
     * 
     * @return The results
     * @throws VizException
     *             query error
     */
    private List<Object[]> performQuery() throws VizException {
        QueryResult result = performMappedQuery();
        List<Object[]> unmappedResults = new ArrayList<Object[]>();

        for (QueryResultRow row : result.getRows()) {
            unmappedResults.add(row.getColumnValues());
        }
        return unmappedResults;
    }

    /**
     * Performs the statement
     * 
     * @return The number of rows modified by the statement
     * @throws VizException
     *             If the statement fails
     */
    private int performStatement() throws VizException {

        if (database == null) {
            throw new VizException("Database not specified for statement");
        }
        if (query == null) {
            throw new VizException("Cannot execute null statement");
        }

        QlServerRequest request = new QlServerRequest(query);
        request.setDatabase(database);
        request.setType(QueryType.STATEMENT);
        request.setParamMap(paramMap);

        // set the mode so the handler knows what to do
        if (queryLanguage == null) {
            throw new VizException("Query language not specified");
        } else if (queryLanguage.equals(QueryLanguage.HQL)) {
            request.setLang(QlServerRequest.QueryLanguage.HQL);
        } else {
            request.setLang(QlServerRequest.QueryLanguage.SQL);
        }

        int retVal = 0;
        AbstractResponseMessage response = (AbstractResponseMessage) ThriftClient
                .sendRequest(request);

        if (response instanceof ResponseMessageGeneric) {
            retVal = (Integer) ((ResponseMessageGeneric) response)
                    .getContents();
        } else if (response instanceof ResponseMessageError) {
            ResponseMessageError rme = (ResponseMessageError) response;
            VizServerSideException innerException = new VizServerSideException(
                    rme.toString());
            throw new VizServerSideException(rme.getErrorMsg(), innerException);
        }
        return retVal;
    }

    /**
     * Communicates with the server to insert/update objects
     * 
     * @param objList
     *            The list of objects to insert/update
     * @param database
     *            The database to use
     * @return How many items were inserted/updated
     * @throws VizException
     *             If errors occur
     */
    private int saveOrUpdateList(List<Object> objList, String database)
            throws VizException {
        SaveOrUpdateRequest req = new SaveOrUpdateRequest();
        req.setDbName(database);
        req.setObjectsToUpdate(objList);

        Object result = ThriftClient.sendRequest(req);
        return (Integer) result;
    }
}
