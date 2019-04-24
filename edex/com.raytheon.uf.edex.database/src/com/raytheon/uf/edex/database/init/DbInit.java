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

package com.raytheon.uf.edex.database.init;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.HibernateException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.Dialect;
import org.hibernate.jdbc.Work;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.DropCreateSqlUtil;
import com.raytheon.uf.edex.database.dao.SessionManagedDao;

/**
 * The DbInit class is responsible for ensuring that the appropriate tables are
 * present in the database implementation for the session factory.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Apr 30, 2013  1960     djohnson  Extracted and generalized from the registry
 *                                  DbInit.
 * May 29, 2013  1650     djohnson  Allow initDb() to be overridden, though
 *                                  should rarely be done.
 * Jun 24, 2013  2106     djohnson  initDb() always starts a fresh, shiny, new
 *                                  transaction.
 * Oct 11, 2013  1682     bphillip  Changed method visibility to allow access by
 *                                  subclasses
 * Oct 16, 2014  3454     bphillip  Upgrading to Hibernate 4
 * May 19, 2016  5666     tjensen   Fix isDbValid check
 * Aug 18, 2016  5810     tjensen   Added additional logging if going to drop
 *                                  tables
 * Feb 13, 2017  5899     rjpeter   Don't allow regeneration of tables by
 *                                  default.
 * Feb 26, 2019  6140     tgurney   Hibernate 5 upgrade
 *
 * </pre>
 *
 * @author djohnson
 */
public abstract class DbInit {
    /**
     * Check system property if table regeneration allowed. On production
     * systems it should NOT be enabled
     */
    private static final boolean DEBUG_ALLOW_TABLE_REGENERATION = Boolean
            .getBoolean("DbInit.allowTableRegeneration");

    /** Constant used for table regeneration */
    private static final Pattern DROP_TABLE_PATTERN = Pattern
            .compile("^drop[\\s]table[\\s](if[\\s]exists[\\s])?");

    /** Constant used for table regeneration */
    private static final Pattern DROP_SEQUENCE_PATTERN = Pattern
            .compile("^drop\\ssequence\\s");

    /** Constant used for table regeneration */
    private static final Pattern CASCADE_PATTERN = Pattern
            .compile("\\scascade$");

    /** Constant used for table regeneration */
    private static final String DROP_TABLE = "drop table ";

    /** Constant used for table regeneration */
    private static final String DROP_SEQUENCE = "drop sequence ";

    /** Constant used for table regeneration */
    private static final String IF_EXISTS = "if exists ";

    /** Constant used for table regeneration */
    private static final String DROP_TABLE_IF_EXISTS = DROP_TABLE + IF_EXISTS;

    /** Constant used for table regeneration */
    private static final String DROP_SEQUENCE_IF_EXISTS = DROP_SEQUENCE
            + IF_EXISTS;

    /** Constant used for table regeneration */
    private static final String CASCADE = " cascade";

    /** The logging application db name **/
    private final String application;

    /** The logger */
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /** The dao for executing database commands **/
    protected SessionManagedDao<?, ?> dao;

    /**
     * Constructor.
     *
     * @param application
     *            the application component the database is used in support of
     */
    protected DbInit(String application) {
        this.application = application;
    }

    /**
     * Initializes the database. This method compares the existing tables in the
     * database to verify that they match the tables that Hibernate is aware of.
     * If the existing tables in the database do not match the tables Hibernate
     * is expecting, the tables are regenerated. During the regeneration
     * process, the minimum database objects are reloaded into the database.
     *
     * @throws Exception
     *             on error initializing the database
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initDb() throws Exception {
        Collection<Class<?>> classes = getDbClasses();

        /*
         * Check to see if the database is valid.
         */
        logger.info("Verifying the database for application [" + application
                + "] against entity classes...");

        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
        builder.applySetting("hibernate.dialect", dao.getDialect());
        try (ServiceRegistry serviceRegistry = builder.build()) {

            Set<String> definedTables = getDefinedTables(classes,
                    serviceRegistry);
            Set<String> existingTables = getExistingTables();
            Set<String> missingTables = new HashSet<>(definedTables);
            missingTables.removeAll(existingTables);
            Set<String> unexpectedTables = new HashSet<>(existingTables);
            unexpectedTables.removeAll(definedTables);

            if (missingTables.isEmpty() && unexpectedTables.isEmpty()) {
                // Database is valid.
                logger.info("Database for application [" + application
                        + "] is up to date!");
            } else if (existingTables.isEmpty() && !definedTables.isEmpty()) {
                logger.info("Database for application [" + application
                        + "] has no tables.  Generating default database tables...");
                createTablesForApplication(classes, serviceRegistry);
            } else if (DEBUG_ALLOW_TABLE_REGENERATION) {
                /*
                 * Database is not valid. Drop and regenerate the tables defined
                 * by Hibernate
                 */
                logger.warn("Database for application [" + application
                        + "] is out of sync with defined java classes. DbInit.allowTableRegeneration property true, regenerating default database tables...");
                logger.info("Dropping tables...");
                dropTables(classes, serviceRegistry);
                createTablesForApplication(classes, serviceRegistry);
            } else {
                StringBuilder msg = new StringBuilder(1000);
                msg.append("Database for application [").append(application)
                        .append("] is out of sync with defined java classes. Upgrade script required to synchronize database tables. Missing tables [");
                msg.append(String.join(", ", missingTables));
                msg.append("], Unexpected tables [");
                msg.append(String.join(", ", unexpectedTables));
                msg.append(']');
                throw new DataAccessLayerException(msg.toString());
            }
        }
    }

    /**
     * Creates all tables and runs any additional sql for the application.
     *
     * @param classes
     *            Metadata for all Hibernate-aware classes
     * @param serviceRegistry
     * @throws Exception
     */
    protected void createTablesForApplication(Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry) throws Exception {
        logger.info("Creating tables...");
        createTables(classes, serviceRegistry);

        logger.info("Executing additional SQL...");
        executeAdditionalSql();

        logger.info("Database tables for application [" + application
                + "] have been successfully generated!");
    }

    /**
     * Hook method to execute any additional setup required.
     *
     * @throws Exception
     *             any exceptions may be thrown
     */
    protected void executeAdditionalSql() throws Exception {
    }

    /**
     * Creates the database tables based on the Class metadata that Hibernate is
     * aware of
     *
     * @param classes
     *            Metadata for all Hibernate-aware classes
     * @param serviceRegistry
     * @throws EbxmlRegistryException
     */
    protected void createTables(final Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry) {
        final List<String> createSqls = DropCreateSqlUtil.getCreateSql(classes,
                serviceRegistry);
        final Work work = new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement stmt = connection.createStatement()) {
                    for (String sql : createSqls) {
                        stmt.execute(sql);
                    }
                    connection.commit();
                }
            }
        };

        executeWork(work);
    }

    /**
     * @return the tables that currently exist in the database based on results
     *         of getTableCheckQuery.
     */
    protected Set<String> getExistingTables() {
        final Set<String> existingTables = new HashSet<>();
        final Work work = new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                try (Statement stmt = connection.createStatement();
                        ResultSet results = stmt
                                .executeQuery(getTableCheckQuery())) {
                    while (results.next()) {
                        existingTables.add(results.getString(1));
                    }
                }
            }
        };
        executeWork(work);

        return existingTables;
    }

    /**
     * @param classes
     *            Metadata for all Hibernate-aware classes
     * @param serviceRegistry
     * @return the tables that are defined in the Hibernate configuration.
     * @throws HibernateException
     */
    protected Set<String> getDefinedTables(Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry) throws HibernateException {
        final Set<String> definedTables = new HashSet<>();

        final List<String> dropSqls = DropCreateSqlUtil.getDropSql(classes,
                serviceRegistry);
        for (String sql : dropSqls) {
            Matcher matcher = DROP_TABLE_PATTERN.matcher(sql);
            if (matcher.find()) {
                /*
                 * Drop the table names to all lower case since this is the form
                 * the database expects
                 */
                sql = matcher.replaceFirst("").toLowerCase();

                // Replace any trailing cascades
                Matcher cascadeMatcher = CASCADE_PATTERN.matcher(sql);
                if (cascadeMatcher.find()) {
                    sql = cascadeMatcher.replaceFirst("");
                }

                // check for schema definition
                if (sql.indexOf('.') < 0) {
                    // no schema definition, add default
                    sql = "awips." + sql;
                }

                definedTables.add(sql);
            }
        }

        return definedTables;
    }

    /**
     * Drops the union set of tables defined by Hibernate and exist in the
     * database.
     *
     * @param classes
     *            Metadata for all Hibernate-aware classes
     * @param serviceRegistry
     * @throws EbxmlRegistryException
     */
    protected void dropTables(final Collection<Class<?>> classes,
            ServiceRegistry serviceRegistry) {

        final Work work = new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                final List<String> dropSqls = DropCreateSqlUtil
                        .getDropSql(classes, serviceRegistry);
                try (Statement stmt = connection.createStatement()) {
                    for (String sql : dropSqls) {
                        Matcher dropTableMatcher = DROP_TABLE_PATTERN
                                .matcher(sql);
                        if (dropTableMatcher.find()) {
                            executeDropSql(sql, dropTableMatcher,
                                    DROP_TABLE_IF_EXISTS, stmt, connection);
                        } else {
                            Matcher dropSequenceMatcher = DROP_SEQUENCE_PATTERN
                                    .matcher(sql);
                            if (dropSequenceMatcher.find()) {
                                executeDropSql(sql, dropSequenceMatcher,
                                        DROP_SEQUENCE_IF_EXISTS, stmt,
                                        connection);
                            }
                        }
                    }
                }
            }
        };

        executeWork(work);
    }

    /**
     * Convenience method to execute drop sql with parameters.
     *
     * @param sql
     * @param dropTextMatcher
     * @param replacementText
     * @param stmt
     * @param connection
     * @throws SQLException
     */
    private static void executeDropSql(String sql, Matcher dropTextMatcher,
            String replacementText, Statement stmt, Connection connection)
            throws SQLException {
        /*
         * Modify the drop string to add the 'if exists' and 'cascade' clauses
         * to avoid any errors if the tables do not exist already
         */
        if (!sql.contains(replacementText)) {
            sql = dropTextMatcher.replaceFirst(replacementText);
        }
        if (!sql.endsWith(CASCADE)) {
            sql += CASCADE;
        }
        stmt.execute(sql);
        connection.commit();
    }

    /**
     * Execute the work.
     *
     * @param work
     *            the work
     */
    protected void executeWork(final Work work) {
        dao.executeWork(work);
    }

    /**
     * Get the dialect.
     *
     * @return
     */
    protected Dialect getDialect() {
        return dao.getDialect();
    }

    public void setDao(SessionManagedDao<?, ?> dao) {
        this.dao = dao;
    }

    /**
     * Get the query that will return the list of current table names used for
     * this db init. Query should return table names in format of
     * schemaname.tablename
     *
     * @return the query
     */
    protected abstract String getTableCheckQuery();

    /**
     *
     * @return get the Metadata for all Hibernate-aware classes
     */
    protected abstract Collection<Class<?>> getDbClasses();
}
