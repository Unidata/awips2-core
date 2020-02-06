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

package com.raytheon.uf.edex.database.dao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.NamedQuery;

import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Example;
import org.hibernate.criterion.Property;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IPersistableDataObject;
import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;
import com.raytheon.uf.common.dataquery.db.QueryParam;
import com.raytheon.uf.common.dataquery.db.QueryResult;
import com.raytheon.uf.common.dataquery.db.QueryResultRow;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.processor.IDatabaseProcessor;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 * The base implementation of all daos. This implementation provides basic
 * database interaction functionality necessary to most persistable data types.
 * These functions include basic persistance and retrieval methods. Any data
 * type specific operations may be implemented by extending this class.
 * <p>
 * Data types which must be persisted to the database must have an associated
 * dao which extends this class. Each class needing a dao must also extend the
 * PersistableDataObject<T> class.
 * <p>
 * NOTE: Direct instantiation of this class is discouraged. Use
 * DaoPool.getInstance().borrowObject() for retrieving all data access objects
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 24, 2007  353      bphillip  Initial Check in
 * May 14, 2008  1076     brockwoo  Fix for distinct with multiple properties
 * Oct 10, 2012  1261     djohnson  Incorporate changes to DaoConfig, add
 *                                  generic to {@link IPersistableDataObject}.
 * Apr 15, 2013  1868     bsteffen  Rewrite mergeAll in PluginDao.
 * Nov 08, 2013  2361     njensen   Changed method signature of saveOrUpdate to
 *                                  take Objects, not PersistableDataObjects
 * Dec 13, 2013  2555     rjpeter   Added processByCriteria and fixed Generics
 *                                  warnings.
 * Jan 23, 2014  2555     rjpeter   Updated processByCriteria to be a row at a
 *                                  time using ScrollableResults.
 * Apr 23, 2014  2726     rjpeter   Updated processByCriteria to throw
 *                                  exceptions back up to caller.
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * Oct 28, 2014  3454     bphillip  Fix usage of getSession()
 * Feb 23, 2015  4127     dgilling  Added bulkSaveOrUpdateAndDelete().
 * Jul 09, 2015  4500     rjpeter   Added parameterized executeSQLQuery,
 *                                  executeSQLUpdate, and executeMappedSQLQuery.
 * Aug 04, 2015  4500     rjpeter   Removed executeNativeSql.
 * Aug 18, 2015  4758     rjpeter   Update MAPPED_SQL_PATTERN to work with
 *                                  multiline sql queries.
 * Aug 19, 2015  4763     rjpeter   Update mappedSql to remove distinct and
 *                                  function definitions from name mapping.
 * Nov 20, 2015  5140     bsteffen  Update mappedSql to ignore comma in function
 *                                  argument lists.
 * Nov 29, 2016  5937     tgurney   Add maxRowCount param to executeSQLQuery
 * Jan 31, 2018  6945     tgurney   Add maxResults param to executeHQLQuery
 * Mar 26, 2018  6711     randerso  Added support for named queries
 * Feb 26, 2019  6140     tgurney   Hibernate 5 upgrade
 * Sep 09, 2019  6140     randerso  Fix queries with maxResults.
 * Sep 11, 2019  7931     tgurney   Add default aliases for returned columns
 *                                  that don't have aliases
 * Nov 04, 2019  7960     mapeters  Added {@link #createAll}
 *
 * </pre>
 *
 * @author bphillip
 */
public class CoreDao {

    protected final IUFStatusHandler logger = UFStatus.getHandler(getClass());

    protected static final Pattern MAPPED_SQL_PATTERN = Pattern.compile(
            "select (.+?) FROM .*",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    /* Pattern used by postgis that need to be escaped */
    protected static final Pattern COLONS = Pattern.compile("::");

    protected static final String COLON_REPLACEMENT = Matcher
            .quoteReplacement("\\:\\:");

    protected SessionFactory sessionFactory;

    protected HibernateTransactionManager txManager;

    /** The convenience wrapper for the Hibernate transaction manager */
    protected final TransactionTemplate txTemplate;

    /** The class associated with this dao */
    protected Class<?> daoClass;

    /**
     * Creates a new dao instance not associated with a specific class. A class
     * may be associated with this dao later by calling the setDaoClass method.
     * <p>
     * This constructor is used to create a generic dao when a Mule service bean
     * is constructed. When the service bean is constructed, the bean has no
     * knowledge about which data type it is handling. Therefore, the querying
     * capabilities provided by this class are not accessible until the daoClass
     * has been assigned
     */
    public CoreDao(DaoConfig config) {
        this.txManager = config.getTxManager();
        txTemplate = new TransactionTemplate(txManager);
        setSessionFactory(config.getSessionFactory());
        this.daoClass = config.getDaoClass();
    }

    /**
     * Persists an object to the database using the provided Hibernate mapping
     * file for the object
     *
     * @param obj
     *            The object to be persisted to the database
     */
    public void persist(final Object obj) throws TransactionException {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                getCurrentSession().saveOrUpdate(obj);
            }
        });
    }

    /**
     * Persists or updates an object to the database using the provided
     * Hibernate mapping file for the object
     *
     * @param obj
     *            The object to be persisted to the database
     */
    public void saveOrUpdate(final Object obj) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                getCurrentSession().saveOrUpdate(obj);
            }
        });
    }

    /**
     * Creates the object entry in the database
     *
     * @param obj
     *            The object to be created in the database
     */
    public void create(final Object obj) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                getCurrentSession().save(obj);
            }
        });
    }

    /**
     * Creates the object entries in the database as a single transaction. If
     * any fail, none will be created.
     *
     * @param objs
     *            The objects to be created in the database
     */
    public void createAll(List<?> objs) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Session session = getCurrentSession();
                for (Object obj : objs) {
                    session.save(obj);
                }
            }
        });
    }

    /**
     * Updates an object in the database using the provided Hibernate mapping
     * file for the object
     *
     * @param obj
     *            The object to be persisted to the database
     */
    public <T> void update(final PersistableDataObject<T> obj) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                getCurrentSession().update(obj);
            }
        });
    }

    /**
     * Persists all objects in collection using a single transaction.
     *
     * @param obj
     *            The object to be persisted to the database
     */
    public void persistAll(final Collection<? extends Object> objs) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Session session = getCurrentSession();
                for (Object obj : objs) {
                    session.saveOrUpdate(obj);
                }
            }
        });
    }

    /**
     * Deletes an object from the database
     *
     * @param obj
     *            The object to delete
     */
    public <T> void delete(final Object obj) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                getCurrentSession().delete(obj);
            }
        });
    }

    /**
     * Retrieves a unique object based on the id field specified in the
     * Hibernate mapping file for the specified object
     *
     * @param id
     *            The id value of the object
     * @return The object with the matching id.<br>
     *         Null if not found
     */
    public <T> PersistableDataObject<T> queryById(final Serializable id) {
        PersistableDataObject<T> retVal = txTemplate
                .execute(new TransactionCallback<PersistableDataObject<T>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public PersistableDataObject<T> doInTransaction(
                            TransactionStatus status) {
                        return (PersistableDataObject<T>) getCurrentSession()
                                .get(daoClass, id);
                    }
                });
        return retVal;
    }

    /**
     * Retrieves a persitant object based on the given id
     *
     * @param id
     *            The id
     * @return The object
     */
    public <T> PersistableDataObject<T> queryById(final PluginDataObject id) {
        PersistableDataObject<T> retVal = txTemplate
                .execute(new TransactionCallback<PersistableDataObject<T>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public PersistableDataObject<T> doInTransaction(
                            TransactionStatus status) {
                        DetachedCriteria criteria = DetachedCriteria
                                .forClass(id.getClass())
                                .add(Property.forName("dataURI")
                                        .eq(id.getDataURI()));
                        List<?> list = criteria
                                .getExecutableCriteria(getCurrentSession())
                                .list();
                        if (!list.isEmpty()) {
                            return (PluginDataObject) list.get(0);
                        } else {
                            return null;
                        }
                    }
                });
        return retVal;
    }

    /**
     * Retrieves a list of objects based on a partially populated class.
     * Hibernate will find objects similar to the partially populated object
     * passed in. This method places a limit on the maximum number of results
     * returned.
     *
     * @param obj
     *            The partially populated object
     * @param maxResults
     *            Maximum number of results to return
     * @return A list of similar objects
     */
    public <T> List<PersistableDataObject<T>> queryByExample(
            final PersistableDataObject<T> obj, final int maxResults) {
        List<PersistableDataObject<T>> retVal = txTemplate.execute(
                new TransactionCallback<List<PersistableDataObject<T>>>() {
                    @Override
                    @SuppressWarnings("unchecked")
                    public List<PersistableDataObject<T>> doInTransaction(
                            TransactionStatus status) {
                        return getCurrentSession()
                                .createCriteria(obj.getClass())
                                .add(Example.create(obj)).list();
                    }
                });
        return retVal;
    }

    /**
     * Retrieves a list of objects based on a partially populated class.
     * Hibernate will find objects similar to the partially populated object
     * passed in. This method does not place a limit on the maximum number of
     * results returned.
     *
     * @param obj
     *            The partially populated object
     * @return A list of similar objects
     */
    public <T> List<PersistableDataObject<T>> queryByExample(
            PersistableDataObject<T> obj) {
        return queryByExample(obj, -1);
    }

    /**
     * Deletes data from the database using a DatabaseQuery object
     *
     * @param query
     *            The query object
     * @return The results of the query
     * @throws DataAccessLayerException
     *             If the query fails
     */
    public int deleteByCriteria(final DatabaseQuery query)
            throws DataAccessLayerException {
        int rowsDeleted = 0;
        try {
            // Get a session and create a new criteria instance
            rowsDeleted = txTemplate
                    .execute(new TransactionCallback<Integer>() {
                        @Override
                        public Integer doInTransaction(
                                TransactionStatus status) {
                            String queryString = query.createHQLDelete();
                            Query hibQuery = getCurrentSession()
                                    .createQuery(queryString);
                            try {
                                query.populateHQLQuery(hibQuery,
                                        getSessionFactory());
                            } catch (DataAccessLayerException e) {
                                throw new org.hibernate.TransactionException(
                                        "Error populating delete statement", e);
                            }
                            return hibQuery.executeUpdate();
                        }
                    });
        } catch (TransactionException e) {
            throw new DataAccessLayerException("Transaction failed", e);
        }
        return rowsDeleted;
    }

    /**
     * Queries the database using a DatabaseQuery object
     *
     * @param query
     *            The query object
     * @return The results of the query
     * @throws DataAccessLayerException
     *             If the query fails
     */
    public List<?> queryByCriteria(final DatabaseQuery query)
            throws DataAccessLayerException {
        List<?> queryResult = null;
        try {
            // Get a session and create a new criteria instance
            queryResult = txTemplate
                    .execute(new TransactionCallback<List<?>>() {
                        @Override
                        public List<?> doInTransaction(
                                TransactionStatus status) {
                            String queryString = query.createHQLQuery();
                            Query hibQuery = getCurrentSession()
                                    .createQuery(queryString);
                            try {
                                query.populateHQLQuery(hibQuery,
                                        getSessionFactory());
                            } catch (DataAccessLayerException e) {
                                throw new org.hibernate.TransactionException(
                                        "Error populating query", e);
                            }
                            // hibQuery.setCacheMode(CacheMode.NORMAL);
                            // hibQuery.setCacheRegion(QUERY_CACHE_REGION);
                            Integer maxResults = query.getMaxResults();
                            if (maxResults != null && maxResults > 0) {
                                hibQuery.setMaxResults(maxResults);
                            }
                            List<?> results = hibQuery.list();
                            return results;
                        }
                    });

        } catch (TransactionException e) {
            throw new DataAccessLayerException("Transaction failed", e);
        }
        return queryResult;
    }

    /**
     * Queries the database in batches using a DatabaseQuery object and send
     * each batch to processor.
     *
     * @param query
     *            The query object
     * @param processor
     *            The processor object
     * @return The number of results processed
     * @throws DataAccessLayerException
     *             If the query fails
     */
    public <T> int processByCriteria(final DatabaseQuery query,
            final IDatabaseProcessor<T> processor)
            throws DataAccessLayerException {
        int rowsProcessed = 0;
        try {
            // Get a session and create a new criteria instance
            rowsProcessed = txTemplate
                    .execute(new TransactionCallback<Integer>() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public Integer doInTransaction(
                                TransactionStatus status) {
                            String queryString = query.createHQLQuery();
                            Query hibQuery = getCurrentSession()
                                    .createQuery(queryString);
                            try {
                                query.populateHQLQuery(hibQuery,
                                        getSessionFactory());
                            } catch (DataAccessLayerException e) {
                                throw new org.hibernate.TransactionException(
                                        "Error populating query", e);
                            }

                            int batchSize = processor.getBatchSize();
                            if (batchSize <= 0) {
                                batchSize = 1000;
                            }

                            hibQuery.setFetchSize(processor.getBatchSize());

                            int count = 0;
                            ScrollableResults rs = hibQuery
                                    .scroll(ScrollMode.FORWARD_ONLY);
                            boolean continueProcessing = true;

                            try {
                                while (rs.next() && continueProcessing) {
                                    Object[] row = rs.get();
                                    if (row.length > 0) {
                                        continueProcessing = processor
                                                .process((T) row[0]);
                                    }
                                    count++;
                                    if (count % batchSize == 0) {
                                        getCurrentSession().clear();
                                    }
                                }
                                processor.finish();
                            } catch (Exception e) {
                                /*
                                 * Only way to propogate the error to the caller
                                 * is to throw a runtime exception
                                 */
                                throw new RuntimeException(
                                        "Error occurred during processing", e);
                            }
                            return count;
                        }
                    });

        } catch (Exception e) {
            throw new DataAccessLayerException(
                    "Error occurred during processing", e);
        }

        return rowsProcessed;
    }

    public void deleteAll(final List<?> objs) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Session session = getCurrentSession();
                for (Object obj : objs) {
                    session.delete(obj);
                }
            }
        });
    }

    /**
     *
     * @param fields
     * @param values
     * @param operands
     * @param resultCount
     * @param orderBy
     * @param orderAscending
     * @param distinctProperties
     * @return
     * @throws DataAccessLayerException
     */
    public List<?> queryByCriteria(final List<String> fields,
            final List<Object> values, final List<String> operands,
            final Integer resultCount, final String orderBy,
            final boolean orderAscending, final List<String> distinctProperties)
            throws DataAccessLayerException {

        DatabaseQuery query = new DatabaseQuery(this.daoClass.getName());
        query.addDistinctParameter(distinctProperties);
        query.addOrder(orderBy, orderAscending);
        for (int i = 0; i < fields.size(); i++) {
            QueryParam param = new QueryParam(fields.get(i), values.get(i));
            if (operands != null) {
                param.setOperand(operands.get(i));
            }
            query.addQueryParam(param);
        }

        return queryByCriteria(query);
    }

    /**
     * Retrieves a list of objects based on field names, values, and
     * operands.<br>
     * This method is the core query method.
     *
     * @param parameterMap
     *            A map containing <fieldName,value> pairs
     * @param operandMap
     *            A map containing <fieldName,operand> pairs. This is an
     *            optional argument and may be null. All operands will be
     *            assumed as =
     * @param resultCount
     *            The limiting number of results. This is an optional argument
     *            and may be null.
     * @param orderBy
     *            The field to order the results on. This is an optional
     *            argument and may be null.
     * @param orderAscending
     *            If an property to order by is specified, this argument must be
     *            provided. True for ascending order. False for descending
     *            order.
     * @return The list of results matching specified criteria
     */
    public List<?> queryByCriteria(final List<String> fields,
            final List<Object> values, final List<String> operands,
            final Integer resultCount, final String orderBy,
            final boolean orderAscending) throws DataAccessLayerException {
        return queryByCriteria(fields, values, operands, resultCount, orderBy,
                orderAscending, null);
    }

    /**
     * Convenience method if result limiting is not necessary.
     *
     * @param fields
     *            The fields to query against
     * @param values
     *            The corresponding values for these fields
     * @param operands
     *            The operands to use during the query
     * @param orderBy
     *            The field to order the results on. This is an optional
     *            argument and may be null.
     * @param orderAscending
     *            If an property to order by is specified, this argument must be
     *            provided. True for ascending order. False for descending
     *            order.
     * @return The list of results matching specified criteria
     */
    public List<?> queryByCriteria(List<String> fields, List<Object> values,
            List<String> operands, String orderBy, boolean orderAscending)
            throws DataAccessLayerException {
        return queryByCriteria(fields, values, operands, null, orderBy,
                orderAscending);
    }

    /**
     * Convenience method if sorting of results is not necessary
     *
     * @param fields
     *            The fields to query against
     * @param values
     *            The corresponding values for these fields
     * @param operands
     *            The operands to use during the query
     * @return The list of results matching specified criteria
     */
    public List<?> queryByCriteria(List<String> fields, List<Object> values,
            List<String> operands) throws DataAccessLayerException {
        return queryByCriteria(fields, values, operands, null, null, false);
    }

    /**
     * Convenience method if sorting of results and equality is assumed for all
     * operands is not necessary
     *
     * @param fields
     *            The fields to query against
     * @param values
     *            The corresponding values for these fields
     * @param operands
     *            The operands to use during the query
     * @return The list of results matching specified criteria
     */
    public List<?> queryByCriteria(List<String> fields, List<Object> values)
            throws DataAccessLayerException {
        return queryByCriteria(fields, values, null, null, null, false);
    }

    /**
     * Retrieves a list of objects based on a single field, value, and operand.
     *
     * @param field
     *            The field to query against
     * @param value
     *            The value to query for
     * @param operand
     *            The operand to apply
     * @return The list of results matching the specified criteria
     */
    public List<?> queryBySingleCriteria(String field, String value,
            String operand) throws DataAccessLayerException {

        DatabaseQuery query = new DatabaseQuery(this.daoClass.getName());
        query.addQueryParam(field, value, QueryParam.translateOperand(operand));
        return queryByCriteria(query);
    }

    /**
     * Retrieves a list of objects based on a single field/value pair.
     * Convenience method assuming equality operand is used.
     *
     * @param field
     *            The field to query against
     * @param value
     *            The value to query for
     * @return The list of results matching the specified criteria
     */
    public List<?> queryBySingleCriteria(String field, String value)
            throws DataAccessLayerException {
        return queryBySingleCriteria(field, value, "=");
    }

    /**
     * Executes a catalog query
     *
     * @param parameterMap
     *            The parameters names and values to query against
     * @param operandMap
     *            The parameter name and operands to use
     * @param distinctName
     *            The name of the parameter to search for distinct values for
     * @return The list of objects found by the query
     */
    public List<?> queryCatalog(final List<String> fields,
            final List<Object> values, final List<String> operands,
            final String distinctName) throws DataAccessLayerException {
        List<String> distinctProperties = new ArrayList<>();
        distinctProperties.add(distinctName);
        return this.queryByCriteria(fields, values, operands, null, null, false,
                distinctProperties);
    }

    public List<?> queryCatalog(final List<String> fields,
            final List<Object> values, final List<String> operands,
            final List<String> distinctNames) throws DataAccessLayerException {
        return this.queryByCriteria(fields, values, operands, null, null, false,
                distinctNames);
    }

    /**
     * Executes an HQL query
     *
     * @param hqlQuery
     *            The HQL query string
     * @return The list of objects returned by the query
     */
    public QueryResult executeHQLQuery(final String hqlQuery) {
        return executeHQLQuery(hqlQuery, null);
    }

    /**
     * Executes an HQL query
     *
     * @param hqlQuery
     *            The HQL query string
     * @return The list of objects returned by the query
     */
    public QueryResult executeHQLQuery(final String hqlQuery,
            final Map<String, Object> paramMap) {
        return executeHQLQuery(hqlQuery, paramMap, 0);
    }

    /**
     * Executes an HQL query
     *
     * @param hqlQuery
     *            The HQL query string
     * @param paramMap
     *            The parameters names and values to query against
     * @param maxResults
     *            Maximum number of objects to return (less than 1 means
     *            unlimited)
     *
     * @return The list of objects returned by the query
     */
    public QueryResult executeHQLQuery(final String hqlQuery,
            final Map<String, Object> paramMap, int maxResults) {
        QueryResult result = txTemplate
                .execute(new TransactionCallback<QueryResult>() {
                    @Override
                    public QueryResult doInTransaction(
                            TransactionStatus status) {
                        Query hibQuery = getCurrentSession()
                                .createQuery(hqlQuery);
                        // hibQuery.setCacheMode(CacheMode.NORMAL);
                        // hibQuery.setCacheRegion(QUERY_CACHE_REGION);
                        hibQuery.setCacheable(true);
                        if (maxResults > 0) {
                            hibQuery.setMaxResults(maxResults);
                        }
                        addParamsToQuery(hibQuery, paramMap);

                        List<?> queryResult = hibQuery.list();

                        QueryResultRow[] rows = new QueryResultRow[queryResult
                                .size()];
                        if (!queryResult.isEmpty()) {
                            if (queryResult.get(0) instanceof Object[]) {
                                for (int i = 0; i < queryResult.size(); i++) {
                                    QueryResultRow row = new QueryResultRow(
                                            (Object[]) queryResult.get(i));
                                    rows[i] = row;
                                }

                            } else {
                                for (int i = 0; i < queryResult.size(); i++) {
                                    QueryResultRow row = new QueryResultRow(
                                            new Object[] {
                                                    queryResult.get(i) });
                                    rows[i] = row;
                                }
                            }
                        }
                        QueryResult result = new QueryResult();
                        String[] returnAliases = hibQuery.getReturnAliases();
                        if (returnAliases == null) {
                            result.addColumnName("record", 0);
                        } else {
                            for (int i = 0; i < returnAliases.length; i++) {
                                if (returnAliases[i] == null) {
                                    String suffix = UUID.randomUUID().toString()
                                            .substring(0, 8);
                                    result.addColumnName("column_" + suffix, i);
                                } else {
                                    result.addColumnName(returnAliases[i], i);
                                }
                            }
                        }
                        result.setRows(rows);
                        return result;
                    }
                });
        return result;
    }

    /**
     * Executes an HQL statement
     *
     * @param hqlStmt
     *            The HQL statement string
     * @return The results of the statement
     */
    public int executeHQLStatement(String hqlStmt) {
        return executeHQLStatement(hqlStmt, null);
    }

    /**
     * Executes an HQL statement
     *
     * @param hqlStmt
     *            The HQL statement string
     * @return The results of the statement
     */
    public int executeHQLStatement(final String hqlStmt,
            final Map<String, Object> paramMap) {

        int queryResult = txTemplate
                .execute(new TransactionCallback<Integer>() {
                    @Override
                    public Integer doInTransaction(TransactionStatus status) {
                        Query hibQuery = getCurrentSession()
                                .createQuery(hqlStmt);
                        addParamsToQuery(hibQuery, paramMap);
                        return hibQuery.executeUpdate();
                    }
                });

        return queryResult;
    }

    /**
     * Executes a single SQL query. NOTE: Do not use String building to put
     * values in to a query. Use the parameterized method to prevent SQL
     * Injection.
     *
     * @param sql
     *            An SQL query to execute
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql) {
        return executeSQLQuery(sql, null);
    }

    /**
     * Executes a single SQL query. NOTE: Do not use String building to put
     * values in to a query. Use the parameterized method to prevent SQL
     * Injection.
     *
     * @param sql
     *            An SQL query to execute
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql, int maxRowCount) {
        return executeSQLQuery(sql, null, maxRowCount);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @param param
     * @param val
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql, final String param,
            final Object val) {
        return executeSQLQuery(sql, param, val, 0);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @param param
     * @param val
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql, final String param,
            final Object val, int maxRowCount) {
        Map<String, Object> paramMap = new HashMap<>(1, 1);
        paramMap.put(param, val);
        return executeSQLQuery(sql, paramMap, maxRowCount);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @param paramMap
     *            Map of parameter names to values
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql,
            final Map<String, Object> paramMap) {
        return executeSQLQuery(sql, paramMap, 0);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @param paramMap
     *            Map of parameter names to values
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    public Object[] executeSQLQuery(final String sql,
            final Map<String, Object> paramMap, int maxRowCount) {
        long start = System.currentTimeMillis();
        List<?> queryResult = txTemplate
                .execute(new TransactionCallback<List<?>>() {
                    @Override
                    public List<?> doInTransaction(TransactionStatus status) {
                        String replaced = COLONS.matcher(sql)
                                .replaceAll(COLON_REPLACEMENT);
                        SQLQuery query = getCurrentSession()
                                .createSQLQuery(replaced);
                        addParamsToQuery(query, paramMap);
                        if (maxRowCount > 0) {
                            query.setMaxResults(maxRowCount);
                        }
                        return query.list();
                    }
                });
        logger.debug("executeSQLQuery took: "
                + (System.currentTimeMillis() - start) + " ms");
        return queryResult.toArray();
    }

    /**
     * Executes a single SQL query. NOTE: Do not use String building to put
     * values in to a query. Use the parameterized method to prevent SQL
     * Injection.
     *
     * @param sql
     *            An SQL query to execute
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    @Deprecated
    public QueryResult executeMappedSQLQuery(final String sql) {
        return executeMappedSQLQuery(sql, null);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    @Deprecated
    public QueryResult executeMappedSQLQuery(final String sql,
            final String param, final Object val) {
        Map<String, Object> paramMap = new HashMap<>(1, 1);
        paramMap.put(param, val);
        return executeMappedSQLQuery(sql, paramMap);
    }

    /**
     * Executes a single parameterized SQL query.
     *
     * @param sql
     *            An SQL query to execute
     * @return An array objects (multiple rows are returned as Object [ Object
     *         [] ]
     */
    @Deprecated
    public QueryResult executeMappedSQLQuery(final String sql,
            final Map<String, Object> paramMap) {
        Object[] queryResult = executeSQLQuery(sql, paramMap);
        QueryResultRow[] rows = new QueryResultRow[queryResult.length];
        if (queryResult.length > 0) {
            if (queryResult[0] instanceof Object[]) {
                for (int i = 0; i < queryResult.length; i++) {
                    QueryResultRow row = new QueryResultRow(
                            (Object[]) queryResult[i]);
                    rows[i] = row;
                }

            } else {
                for (int i = 0; i < queryResult.length; i++) {
                    QueryResultRow row = new QueryResultRow(
                            new Object[] { queryResult[i] });
                    rows[i] = row;
                }
            }
        }

        QueryResult result = new QueryResult();
        result.setRows(rows);
        Matcher m = MAPPED_SQL_PATTERN.matcher(sql);
        if (m.matches()) {
            String group = m.group(1);
            int colIndex = 0;
            /*
             * Split the columns on commas, ignore commas in quotes and
             * parenthesis
             */
            List<String> columns = new ArrayList<>();
            int columnStart = 0;
            int parenthesisDepth = 0;
            boolean inQoutes = false;
            boolean escape = false;
            for (int i = 0; i < group.length(); i += 1) {
                char c = group.charAt(i);
                if (inQoutes) {
                    if (!escape && c == '\'') {
                        inQoutes = false;
                    } else if (!escape && c == '\\') {
                        escape = true;
                    } else {
                        escape = false;
                    }
                } else if (c == '\'') {
                    inQoutes = true;
                } else if (c == '(') {
                    parenthesisDepth += 1;
                } else if (c == ')') {
                    parenthesisDepth -= 1;
                } else if (parenthesisDepth == 0 && !inQoutes && c == ',') {
                    columns.add(group.substring(columnStart, i));
                    columnStart = i + 1;
                }
            }
            columns.add(group.substring(columnStart));
            for (String col : columns) {
                col = col.toLowerCase().trim();

                // remove distinct from name return
                if (col.startsWith("distinct ")) {
                    col = col.substring(9);
                }

                int asIndex = col.indexOf(" as ");
                if (asIndex > 0) {
                    col = col.substring(asIndex + 4);
                } else {
                    /*
                     * check for function and remove function definition if
                     * present
                     */
                    int parenIndex = col.indexOf('(');

                    if (parenIndex > 0) {
                        col = col.substring(0, parenIndex);
                    }
                }

                result.addColumnName(col.trim(), colIndex);
                colIndex++;
            }
        } else {
            logger.error("Unable to map query columns for query [" + sql + "]");
        }

        return result;
    }

    public List<?> executeCriteriaQuery(final List<Criterion> criterion) {

        long start = System.currentTimeMillis();
        List<?> queryResult = txTemplate
                .execute(new TransactionCallback<List<?>>() {
                    @Override
                    public List<?> doInTransaction(TransactionStatus status) {

                        Criteria crit = getCurrentSession()
                                .createCriteria(daoClass);
                        for (Criterion cr : criterion) {
                            crit.add(cr);
                        }
                        return crit.list();
                    }
                });
        logger.debug("executeCriteriaQuery took: "
                + (System.currentTimeMillis() - start) + " ms");
        return queryResult;
    }

    public List<?> executeCriteriaQuery(final Criterion criterion) {
        List<Criterion> criterionList = new ArrayList<>();
        criterionList.add(criterion);
        return executeCriteriaQuery(criterionList);
    }

    /**
     * Executes a single SQL statement. The SQL should not be a select
     * statement. NOTE: Do not use String building to put values in to a
     * statement. Use the parameterized method to prevent SQL Injection.
     *
     * @param sql
     *            An SQL statement to execute
     * @return Number of rows affected.
     */
    public int executeSQLUpdate(final String sql) {
        return executeSQLUpdate(sql, null);
    }

    /**
     * Executes a single SQL statement. The SQL should not be a select
     * statement.
     *
     * @param sql
     *            An SQL statement to execute
     * @param param
     *            Named parameter
     * @param val
     *            Value of named parameter
     * @return Number of rows affected.
     */
    public int executeSQLUpdate(final String sql, final String param,
            final Object val) {
        Map<String, Object> paramMap = new HashMap<>(1, 1);
        paramMap.put(param, val);
        return executeSQLUpdate(sql, paramMap);
    }

    /**
     * Executes a single SQL statement. The SQL should not be a select
     * statement.
     *
     * @param sql
     *            An SQL statement to execute
     * @param paramMap
     *            Map of named parameter to its value
     * @return Number of rows affected.
     */
    public int executeSQLUpdate(final String sql,
            final Map<String, Object> paramMap) {
        long start = System.currentTimeMillis();
        int updateResult = txTemplate
                .execute(new TransactionCallback<Integer>() {
                    @Override
                    public Integer doInTransaction(TransactionStatus status) {
                        String replaced = COLONS.matcher(sql)
                                .replaceAll(COLON_REPLACEMENT);
                        SQLQuery query = getCurrentSession()
                                .createSQLQuery(replaced);
                        addParamsToQuery(query, paramMap);
                        return query.executeUpdate();
                    }
                });
        logger.debug("executeSQLUpdate took: "
                + (System.currentTimeMillis() - start) + " ms");
        return updateResult;
    }

    /**
     * Adds the specified parameter to the query. Checks for Collection an Array
     * types for use with in lists.
     *
     * @param query
     * @param paramName
     * @param paramValue
     */
    protected static void addParamToQuery(Query query, String paramName,
            Object paramValue) {
        if (paramValue instanceof Collection) {
            query.setParameterList(paramName, (Collection<?>) paramValue);
        } else if (paramValue instanceof Object[]) {
            query.setParameterList(paramName, (Object[]) paramValue);
        } else {
            query.setParameter(paramName, paramValue);
        }
    }

    /**
     * Adds the specified parameters to the query. Checks for Collection an
     * Array types for use with in lists.
     *
     * @param query
     * @param paramMap
     */
    protected static void addParamsToQuery(Query query,
            Map<String, Object> paramMap) {
        if (paramMap != null && !paramMap.isEmpty()) {
            for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                String paramName = entry.getKey();
                Object paramValue = entry.getValue();
                addParamToQuery(query, paramName, paramValue);
            }
        }
    }

    /**
     * Gets the object class associated with this dao
     *
     * @return The object class associated with this dao
     */
    public Class<?> getDaoClass() {
        return daoClass;
    }

    /**
     * Sets the object class associated with this dao
     *
     * @param daoClass
     *            The object class to assign to this dao
     */
    public void setDaoClass(Class<?> daoClass) {
        this.daoClass = daoClass;
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    /**
     * Sets the dao class given a fully qualified class name
     *
     * @param fqn
     *            The fully qualified class name
     */
    public void setDaoClass(String fqn) {
        try {
            daoClass = this.getClass().getClassLoader().loadClass(fqn);
        } catch (ClassNotFoundException e) {
            logger.warn("Unable to load class: " + fqn, e);
        }
    }

    public ClassMetadata getDaoClassMetadata() {
        if (daoClass == null) {
            return null;
        } else {
            return getSessionFactory().getClassMetadata(daoClass);
        }
    }

    /**
     * Updates/saves a set of records and deletes a set of records in the
     * database in a single transaction.
     *
     * @param updates
     *            Records to update or add.
     * @param deletes
     *            Records to delete.
     */
    public void bulkSaveOrUpdateAndDelete(
            final Collection<? extends Object> updates,
            final Collection<? extends Object> deletes) {
        txTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) {
                Session session = getCurrentSession();
                for (Object obj : updates) {
                    session.saveOrUpdate(obj);
                }
                for (Object obj : deletes) {
                    session.delete(obj);
                }
            }
        });
    }

    /**
     * Gets the session associated with the current thread. This method does not
     * create a new session if one does not exist
     *
     * @return The current thread-bound session
     */
    public Session getCurrentSession() {
        return getSessionFactory().getCurrentSession();
    }

    /**
     * Creates a new Hibernate session. Sessions returned by this method must be
     * explicitly closed
     *
     * @return
     */
    public Session getSession() {
        return getSession(true);
    }

    /**
     * Gets a Hibernate session. If allowCreate is true, a new session is
     * opened, and therefore must be explicitly closed. If allowCreate is false,
     * this method will attempt to retrieve the current thread-bound session
     *
     * @param allowCreate
     * @return
     */
    public Session getSession(boolean allowCreate) {
        if (allowCreate) {
            return getSessionFactory().openSession();
        } else {
            return getCurrentSession();
        }
    }

    /**
     * Execute the named query
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @return list of objects
     */
    public List<?> findByNamedQuery(final String queryName) {
        return findByNamedQuery(queryName, 0);
    }

    /**
     * Execute the named query with limited results
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return list of objects
     */
    public List<?> findByNamedQuery(final String queryName, int maxRowCount) {
        return txTemplate.execute(new TransactionCallback<List<?>>() {
            @Override
            public List<?> doInTransaction(TransactionStatus status) {
                Session session = getCurrentSession();
                Query query = session.getNamedQuery(queryName);
                if (maxRowCount > 0) {
                    query.setMaxResults(maxRowCount);
                }
                return query.list();
            }
        });
    }

    /**
     * Execute the named query with the supplied parameter name and value
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @param paramName
     *            name of parameter to be replaced by paramValue
     * @param paramValue
     *            value of parameter, can be Collection or array
     * @return list of objects
     */
    public List<?> findByNamedQueryAndNamedParam(final String queryName,
            final String paramName, final Object paramValue) {
        return findByNamedQueryAndNamedParam(queryName, paramName, paramValue,
                0);
    }

    /**
     * Execute the named query with the supplied parameter name and value and
     * limited results
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @param paramName
     *            name of parameter to be replaced by paramValue
     * @param paramValue
     *            value of parameter, can be Collection or array
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return list of objects
     */
    public List<?> findByNamedQueryAndNamedParam(final String queryName,
            final String paramName, final Object paramValue, int maxRowCount) {
        return txTemplate.execute(new TransactionCallback<List<?>>() {
            @Override
            public List<?> doInTransaction(TransactionStatus status) {
                Session session = getCurrentSession();
                Query query = session.getNamedQuery(queryName);
                addParamToQuery(query, paramName, paramValue);
                if (maxRowCount > 0) {
                    query.setMaxResults(maxRowCount);
                }
                return query.list();
            }
        });
    }

    /**
     * Execute the named query with the supplied parameter names and values
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @param paramMap
     *            map of parameter names to values. Values can be Collections or
     *            arrays
     * @return list of objects
     */
    public List<?> findByNamedQueryAndNamedParams(final String queryName,
            final Map<String, Object> paramMap) {
        return findByNamedQueryAndNamedParams(queryName, paramMap, 0);
    }

    /**
     * Execute the named query with the supplied parameter names and values and
     * limited results
     *
     * @param queryName
     *            name of {@link NamedQuery}
     * @param paramMap
     *            map of parameter names to values. Values can be Collections or
     *            arrays
     * @param maxRowCount
     *            Maximum number of rows to return (less than 1 means unlimited)
     * @return list of objects
     */
    public List<?> findByNamedQueryAndNamedParams(final String queryName,
            final Map<String, Object> paramMap, int maxRowCount) {
        if (paramMap == null) {
            throw new IllegalArgumentException("paramMap must not be null");

        }
        return txTemplate.execute(new TransactionCallback<List<?>>() {
            @Override
            public List<?> doInTransaction(TransactionStatus status) {
                Session session = getCurrentSession();
                Query query = session.getNamedQuery(queryName);
                addParamsToQuery(query, paramMap);
                if (maxRowCount > 0) {
                    query.setMaxResults(maxRowCount);
                }
                return query.list();
            }
        });
    }
}
