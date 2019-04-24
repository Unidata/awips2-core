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

package com.raytheon.uf.edex.database.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.AnnotationException;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.StringUtil;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.DatabasePluginProperties;
import com.raytheon.uf.edex.database.DatabasePluginRegistry;
import com.raytheon.uf.edex.database.DatabaseSessionFactoryBean;
import com.raytheon.uf.edex.database.DropCreateSqlUtil;
import com.raytheon.uf.edex.database.IDatabasePluginRegistryChanged;
import com.raytheon.uf.edex.database.cluster.ClusterLockUtils.LockState;
import com.raytheon.uf.edex.database.cluster.ClusterLocker;
import com.raytheon.uf.edex.database.cluster.ClusterTask;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;

/**
 * Manages the ddl statements used to generate the database tables
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * Oct 08, 2008  1532     bphillip     Initial checkin
 * Feb 09, 2009  1990     bphillip     Fixed index creation
 * Mar 20, 0009           njensen      Implemented IPluginRegistryChanged
 * Mar 29, 2013  1841     djohnson     Remove unused method, warnings, and close
 *                                     streams with utility method.
 * Mar 02, 2013  1970     bgonzale     Added check for abstract entities in sql
 *                                     index naming. Removed unused private
 *                                     method populateSchema.
 * Oct 14, 2013  2361     njensen      Moved to plugin uf.edex.database Replaced
 *                                     use of SerializableManager
 * Jul 10, 2014  2914     garmendariz  Remove EnvProperties
 * Oct 06, 2014  3702     bsteffen     Create PluginVersion table in each
 *                                     database containing plugins.
 * Oct 23, 2014  3454     bphillip     Fix table creation error introduced from
 *                                     Hibernate 4 upgrade
 * Jul 13, 2015  4500     rjpeter      Fix SQL Injection concerns.
 * Dec 17, 2015  5166     kbisanz      Update logging to use SLF4J
 * Jun 20, 2016  5679     rjpeter      Add admin database account.
 * Dec 08, 2016  3440     njensen      Cleanup error message
 * Feb 26, 2019  6140     tgurney      Hibernate 5 upgrade
 *
 * </pre>
 *
 * @author bphillip
 */
public class SchemaManager implements IDatabasePluginRegistryChanged {

    /** The logger */
    private static final Logger logger = LoggerFactory
            .getLogger(SchemaManager.class);

    private static final String resourceSelect = "select relname from pg_class where relname = '";

    /**
     * Plugin lock time out override, 2 minutes
     */

    private static final long pluginLockTimeOutMillis = 2
            * TimeUtil.MILLIS_PER_MINUTE;

    private static final String TABLE = "%table%";

    /** The singleton instance */
    private static SchemaManager instance;

    private final DatabasePluginRegistry dbPluginRegistry;

    private final Map<String, ArrayList<String>> pluginCreateSql = new HashMap<>();

    private final Map<String, ArrayList<String>> pluginDropSql = new HashMap<>();

    private final Pattern createResourceNamePattern = Pattern.compile(
            "^create (?:table |index |sequence )(?:[A-Za-z_0-9]*\\.)?(.+?)(?: .*)?$");

    private final Pattern createIndexTableNamePattern = Pattern
            .compile("^create index %table%.+? on (.+?) .*$");

    private volatile ServiceRegistry schemaGenServiceRegistry = null;

    /**
     * Gets the singleton instance
     *
     * @return The singleton instance
     */
    public static synchronized SchemaManager getInstance() {
        if (instance == null) {
            instance = new SchemaManager();
        }
        return instance;
    }

    private ServiceRegistry getSchemaGenServiceRegistry(
            DatabaseSessionFactoryBean sessionFactory) {
        if (schemaGenServiceRegistry == null) {
            synchronized (this) {
                if (schemaGenServiceRegistry == null) {
                    StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();
                    String dialect = sessionFactory.getConfiguration()
                            .getProperty("dialect");
                    builder.applySetting("hibernate.dialect", dialect);
                    schemaGenServiceRegistry = builder.build();
                }
            }
        }
        return schemaGenServiceRegistry;
    }

    /**
     * Creates a new SchemaManager instance<br>
     * This constructor creates a temporary file and exports the ddl statements
     * into this file. These statements are subsequently read back in and
     * assigned to the correct PluginSchema container object based on the plugin
     */
    private SchemaManager() {
        dbPluginRegistry = DatabasePluginRegistry.getInstance();
    }

    @Override
    public void pluginAdded(String pluginName) throws PluginException {
        boolean haveLock = false;
        DatabasePluginProperties props = DatabasePluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        ClusterLocker cl = null;
        ClusterTask ct = null;

        try {
            String sessFactoryName = "&admin_" + props.getDatabase()
                    + "SessionFactory";
            DatabaseSessionFactoryBean sessFactory = (DatabaseSessionFactoryBean) EDEXUtil
                    .getESBComponent(sessFactoryName);
            PluginVersionDao pvd = new PluginVersionDao(props.getDatabase(),
                    true);

            // handle plugin versioning
            if (props.isForceCheck()) {
                // use direct dialog to figure out
                int rowsUpdated = exportSchema(props, sessFactory, true);
                if (rowsUpdated > 0) {
                    pvd.runPluginScripts(props);
                }
            } else {
                cl = new ClusterLocker(props.getDatabase());
                ct = cl.lock("pluginVersion", props.getPluginFQN(),
                        pluginLockTimeOutMillis, true);
                int failedCount = 0;

                while (!LockState.SUCCESSFUL.equals(ct.getLockState())) {
                    switch (ct.getLockState()) {
                    case FAILED: {
                        failedCount++;
                        if (failedCount > 5) {
                            logger.error(
                                    "Unable to grab cluster lock for plugin versioning plugin: "
                                            + pluginName);
                            return;
                        }
                        break;
                    }
                    case OLD: {
                        // no need to check plugin version
                        return;
                    }
                    default:
                        // NOOP
                        break;
                    }

                    ct = cl.lock("pluginVersion", props.getPluginFQN(),
                            pluginLockTimeOutMillis, true);
                }

                haveLock = true;

                Boolean initialized = pvd
                        .isPluginInitialized(props.getPluginName());

                if (initialized == null) {
                    logger.info(
                            "Exporting DDL for " + pluginName + " plugin...");
                    exportSchema(props, sessFactory, false);
                    pvd.runPluginScripts(props);
                    PluginVersion pv = new PluginVersion(props.getPluginName(),
                            true, props.getTableName());
                    pvd.saveOrUpdate(pv);
                    logger.info(
                            pluginName + " plugin initialization complete!");
                } else if (!initialized) {
                    logger.info(
                            "Exporting DDL for " + pluginName + " plugin...");
                    dropSchema(props, sessFactory);
                    exportSchema(props, sessFactory, false);
                    pvd.runPluginScripts(props);
                    PluginVersion pv = pvd.getPluginInfo(props.getPluginName());
                    pv.setInitialized(true);
                    pv.setTableName(props.getTableName());
                    pvd.saveOrUpdate(pv);
                    logger.info(
                            pluginName + " plugin initialization complete!");
                }
            }
        } catch (Exception e) {
            logger.error("Error processing hibernate objects for plugin "
                    + pluginName, e);
            throw new PluginException(e);
        } finally {
            if (haveLock) {
                cl.unlock(ct, false);
            }
        }
    }

    /**
     *
     * @param props
     * @param sessFactory
     * @param hibernatables
     * @return
     */
    protected List<String> getRawCreateSql(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory) throws AnnotationException {
        String fqn = props.getPluginFQN();
        ArrayList<String> createSql = pluginCreateSql.get(fqn);
        if (createSql == null) {
            // need the full dependency tree to generate the sql
            Collection<Class<?>> dbClasses = getTablesAndDependencies(props,
                    sessFactory.getAnnotatedClasses());
            ServiceRegistry serviceRegistry = getSchemaGenServiceRegistry(
                    sessFactory);
            List<String> sqlList = DropCreateSqlUtil.getCreateSql(dbClasses,
                    serviceRegistry);
            createSql = new ArrayList<>(sqlList.size());
            for (String sql : sqlList) {
                createSql.add(sql.toLowerCase());
            }

            for (int i = 0; i < createSql.size(); i++) {
                String sql = createSql.get(i);
                if (sql.startsWith("create index")) {
                    Matcher matcher = createIndexTableNamePattern.matcher(sql);
                    if (matcher.matches()) {
                        createSql.set(i, StringUtil.replace(sql, TABLE,
                                matcher.group(1)));
                    } else if (sql.contains(TABLE)) {
                        // replace %TABLE% in sql statements with an empty
                        // string
                        createSql.set(i, StringUtil.replace(sql, TABLE, ""));
                    }
                }
            }
            createSql.trimToSize();

            // only truly want the sql for just this plugin
            removeAllDependentCreateSql(props, sessFactory, createSql);

            pluginCreateSql.put(fqn, createSql);
        }
        return createSql;
    }

    /**
     *
     * @param props
     * @param sessFactory
     * @param hibernatables
     * @return
     */
    protected List<String> getRawDropSql(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory) throws AnnotationException {
        String fqn = props.getPluginFQN();
        ArrayList<String> dropSql = pluginDropSql.get(fqn);
        if (dropSql == null) {
            // need the full dependency tree to generate the sql
            Collection<Class<?>> dbClasses = getTablesAndDependencies(props,
                    sessFactory.getAnnotatedClasses());
            ServiceRegistry serviceRegistry = getSchemaGenServiceRegistry(
                    sessFactory);
            List<String> sqlList = DropCreateSqlUtil.getDropSql(dbClasses,
                    serviceRegistry);
            dropSql = new ArrayList<>(sqlList.size());
            for (String sql : sqlList) {
                dropSql.add(sql);
            }

            // only truly want the sql for just this plugin
            removeAllDependentDropSql(props, sessFactory, dropSql);

            dropSql.trimToSize();

            pluginDropSql.put(fqn, dropSql);
        }
        return dropSql;
    }

    /**
     * Searches the classes from the session factory to see if they match the
     * plugin FQN. Recursively searches for the classes associated wtih
     * dependent plugins.
     *
     * @param props
     *            the plugin to find DB classes and dependencies for
     * @param allPossibleClasses
     *            all the classes associated with the session factory
     * @return
     */
    protected Set<Class<?>> getTablesAndDependencies(
            DatabasePluginProperties props, Class<?>[] allPossibleClasses) {
        Set<Class<?>> result = new HashSet<>();
        // add a . to the end to ensure the package name exactly matches
        // and we don't pick up incorrect packages,
        // e.g. common.dataplugin.cwa. vs common.dataplugin.cwat.
        // There will always be a . since we're looking at class names.
        Pattern p = Pattern.compile(props.getPluginFQN() + "\\.");
        for (Class<?> clazz : allPossibleClasses) {
            if (p.matcher(clazz.getName()).find()) {
                result.add(clazz);
            }
        }
        List<String> fqns = props.getDependencyFQNs();
        if (CollectionUtils.isNotEmpty(fqns)) {
            for (String fqn : fqns) {
                DatabasePluginProperties dProps = dbPluginRegistry
                        .getRegisteredObject(fqn);

                // recurse, may need to add short circuit logic by tracking
                // plugins already processed
                result.addAll(this.getTablesAndDependencies(dProps,
                        allPossibleClasses));
            }
        }

        return result;
    }

    protected void removeAllDependentCreateSql(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory, List<String> createSql) {
        List<String> fqns = props.getDependencyFQNs();
        if (CollectionUtils.isNotEmpty(fqns)) {
            for (String fqn : fqns) {
                DatabasePluginProperties dProps = dbPluginRegistry
                        .getRegisteredObject(fqn);
                createSql.removeAll(getRawCreateSql(dProps, sessFactory));
                // recurse to all dependents
                removeAllDependentCreateSql(dProps, sessFactory, createSql);
            }
        }
    }

    protected void removeAllDependentDropSql(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory, List<String> dropSql) {
        List<String> fqns = props.getDependencyFQNs();
        if (CollectionUtils.isNotEmpty(fqns)) {
            for (String fqn : fqns) {
                DatabasePluginProperties dProps = dbPluginRegistry
                        .getRegisteredObject(fqn);
                dropSql.removeAll(getRawDropSql(dProps, sessFactory));
                // recurse to all dependents
                removeAllDependentDropSql(dProps, sessFactory, dropSql);
            }
        }
    }

    protected int exportSchema(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory, boolean forceResourceCheck)
            throws PluginException {
        List<String> ddls = getRawCreateSql(props, sessFactory);
        CoreDao dao = new CoreDao(
                DaoConfig.forDatabase(props.getDatabase(), true));
        int rows = 0;

        for (String sql : ddls) {
            boolean valid = true;
            // sequences should always be checked
            if (forceResourceCheck || sql.startsWith("create sequence ")) {
                valid = false;
                Matcher matcher = createResourceNamePattern.matcher(sql);
                if (matcher.matches() && matcher.groupCount() >= 1) {
                    String resourceName = matcher.group(1).toLowerCase();
                    StringBuilder tmp = new StringBuilder(resourceSelect);
                    tmp.append(resourceName);
                    tmp.append("'");
                    try {
                        Object[] vals = dao.executeSQLQuery(tmp.toString());
                        if (vals.length == 0) {
                            valid = true;
                        }
                    } catch (RuntimeException e) {
                        logger.warn("Error occurred checking if resource ["
                                + resourceName + "] exists", e);
                    }
                } else {
                    logger.warn("Matcher could not find name for create sql ["
                            + sql + "]");
                }
            }
            if (valid) {
                try {
                    dao.executeSQLUpdate(sql);
                    rows++;
                } catch (RuntimeException e) {
                    throw new PluginException(
                            "Error occurred exporting schema, sql [" + sql
                                    + "]",
                            e);
                }
            }
        }

        return rows;
    }

    protected void dropSchema(DatabasePluginProperties props,
            DatabaseSessionFactoryBean sessFactory) throws PluginException {
        List<String> ddls = getRawDropSql(props, sessFactory);
        CoreDao dao = new CoreDao(
                DaoConfig.forDatabase(props.getDatabase(), true));

        for (String sql : ddls) {
            boolean valid = true;

            // never drop sequences
            if (sql.startsWith("drop sequence ")) {
                valid = false;
            } else if (sql.startsWith("drop table ")) {
                if (!sql.startsWith("drop table if exists")) {
                    sql = sql.replace("drop table ", "drop table if exists ");
                }
                sql = sql.replace(";", " cascade;");
            } else if (sql.startsWith("alter table")) {
                // dropping the table drops the index
                valid = false;
            }
            if (valid) {
                try {
                    dao.executeSQLUpdate(sql);
                } catch (RuntimeException e) {
                    throw new PluginException(
                            "Error occurred dropping schema, sql [" + sql + "]",
                            e);
                }
            }
        }
    }

}
