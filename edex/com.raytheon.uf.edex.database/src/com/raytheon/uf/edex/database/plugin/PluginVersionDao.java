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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.edex.core.EDEXUtil;
import com.raytheon.uf.edex.database.DataAccessLayerException;
import com.raytheon.uf.edex.database.DatabasePluginProperties;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import com.raytheon.uf.edex.database.query.DatabaseQuery;

/**
 * The dao implementation associated with the PluginVersion class used for all
 * database interaction.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------------------------------------------
 * Jul 24, 0007  353      bphillip  Initial Check in
 * Oct 06, 2014  3702     bsteffen  Create PluginVersion table in each database containing plugins.
 * Jul 10, 2015  4500     rjpeter   Changed to package scope, added runPluginScripts.
 * Jun 20, 2016  5679     rjpeter   Add admin database account.
 * 
 * </pre>
 * 
 * @author bphillip
 * @version 1
 */
class PluginVersionDao extends CoreDao {

    private static final String DB_INITIALIZED_SQL = "select relname from pg_class where relname = 'plugin_info'";

    private static final String PLUGIN_DIR = EDEXUtil.getEdexPlugins()
            + File.separator;

    /**
     * Creates a new PluginVersionDao.
     */
    public PluginVersionDao() {
        super(DaoConfig.forClass(PluginVersion.class));
    }

    /**
     * Creates a new PluginVersionDao.
     * 
     * @param database
     *            The database to connect to.
     */
    public PluginVersionDao(String database) {
        super(DaoConfig.forClass(database, PluginVersion.class));
    }

    /**
     * Creates a new PluginVersionDao.
     * 
     * @param database
     *            The database to connect to.
     * @param admin
     *            Whether to connect to be with admin privileges or not.
     */
    public PluginVersionDao(String database, boolean admin) {
        super(DaoConfig.forClass(database, PluginVersion.class, admin));
    }

    /**
     * Checks if the database has been initialized yet
     * 
     * @return True if database has been initialized, false if not
     */
    public boolean isDbInitialized() {
        return this.executeSQLQuery(DB_INITIALIZED_SQL).length != 0;
    }

    /**
     * Checks if a particular plugin has been initialized yet
     * 
     * @param plugin
     *            The plugin to check
     * @return true if the plugin has been initialized, else false
     * @throws DataAccessLayerException
     */
    public Boolean isPluginInitialized(String plugin)
            throws DataAccessLayerException {

        DatabaseQuery query = new DatabaseQuery(this.daoClass);
        query.addQueryParam("name", plugin);
        query.addReturnedField("initialized");
        List<?> results = queryByCriteria(query);
        if (results.isEmpty()) {
            return null;
        } else {
            return (Boolean) results.get(0);
        }
    }

    public PluginVersion getPluginInfo(String pluginName) {
        return (PluginVersion) this.queryById(pluginName);
    }

    /**
     * Loads all the plugin names currently registered in the plugin_info table
     * 
     * @return The names of the plugins
     * @throws DataAccessLayerException
     *             If problems occur during query
     */
    @SuppressWarnings("unchecked")
    public List<String> loadAllPluginNames() throws DataAccessLayerException {
        List<String> pluginNames = new ArrayList<String>();
        DatabaseQuery query = new DatabaseQuery(daoClass);
        query.addReturnedField("name");
        pluginNames = (List<String>) this.queryByCriteria(query);
        return pluginNames;
    }

    /**
     * Retrieves all plugins available in the system
     * 
     * @return A list of plugin names
     * @throws DataAccessLayerException
     *             If errors occur during query
     */
    @SuppressWarnings("unchecked")
    public List<String> getAvailablePlugins() throws DataAccessLayerException {
        DatabaseQuery query = new DatabaseQuery(daoClass);
        query.addDistinctParameter("name");
        List<String> availablePlugins = (List<String>) queryByCriteria(query);
        return availablePlugins;

    }

    /**
     * Deletes a plugin info entry
     * 
     * @param pluginName
     *            The plugin name of the entry to remove
     * @throws DataAccessLayerException
     *             If errors occur during the database operation
     */
    @SuppressWarnings("unchecked")
    public void deletePluginVersionByName(String pluginName)
            throws DataAccessLayerException {
        DatabaseQuery query = new DatabaseQuery(daoClass);
        query.addQueryParam("name", pluginName);
        List<PluginVersion> pv = (List<PluginVersion>) queryByCriteria(query);
        if (!pv.isEmpty()) {
            this.delete(pv.get(0));
        }
    }

    /**
     * Runs all scripts for a particular plugin.
     * 
     * @param pluginName
     *            The plugin to run the scripts for
     * @throws PluginException
     *             If errors occur accessing the database
     */
    protected void runPluginScripts(DatabasePluginProperties props)
            throws PluginException {
        final JarFile jar;
        String pluginFQN = props.getPluginFQN();

        try {
            File jarFile = new File(PLUGIN_DIR, pluginFQN + ".jar");
            if (!jarFile.exists()) {
                /* check for any jar files of the format pluginFQN_version.jar */
                Pattern p = Pattern.compile("^" + Pattern.quote(pluginFQN)
                        + "_.*\\.jar$");
                File pluginDir = new File(PLUGIN_DIR);
                for (File f : pluginDir.listFiles()) {
                    if (p.matcher(f.getName()).find()) {
                        jarFile = f;
                        break;
                    }
                }
            }
            jar = new JarFile(jarFile);
        } catch (IOException e) {
            throw new PluginException("Unable to find jar for plugin FQN "
                    + pluginFQN, e);
        }

        String name = null;

        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final JarEntry entry = entries.nextElement();
                name = entry.getName();

                /*
                 * run the script bundled in the jar file from the res/scripts
                 * folder inside the jar
                 */
                if (name.startsWith("res/scripts") && name.endsWith(".sql")) {
                    logger.info("Executing plugin script: " + name);
                    final List<String> statements = parseJarEntryForStatements(
                            jar, entry);

                    txTemplate.execute(new TransactionCallbackWithoutResult() {
                        @Override
                        public void doInTransactionWithoutResult(
                                TransactionStatus status) {
                            Session sess = getCurrentSession();
                            for (String statement : statements) {
                                /*
                                 * SQL Injection not a concern as the entire
                                 * statement was provided by file contained in
                                 * the jar.
                                 */
                                SQLQuery query = sess.createSQLQuery(statement);
                                query.executeUpdate();
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error executing script " + name, e);
            throw new PluginException("Unable to execute script " + name
                    + " for plugin FQN " + pluginFQN, e);
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException e) {
                    throw new PluginException(
                            "Unable to close jar for plugin FQN " + pluginFQN,
                            e);
                }
            }
        }
    }

    /**
     * Parses a given jar entry for sql statements, ignoring all comments.
     * 
     * @param jar
     * @param entry
     * @throws IOException
     */
    private List<String> parseJarEntryForStatements(JarFile jar, JarEntry entry)
            throws IOException {
        final List<String> rval = new LinkedList<>();
        BufferedReader reader = null;
        InputStream stream = null;

        try {
            stream = jar.getInputStream(entry);
            reader = new BufferedReader(new InputStreamReader(stream));

            String line = null;
            StringBuilder buffer = new StringBuilder();
            boolean ignoringLines = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (ignoringLines) {
                    // looking for end of comment block
                    if (line.endsWith("*/")) {
                        ignoringLines = false;
                    }
                } else {
                    if (line.startsWith("/*")) {
                        // skip just this line?
                        if (!line.endsWith("*/")) {
                            ignoringLines = true;
                        }
                    } else if (!line.startsWith("--")) {
                        // not a single line comment either
                        buffer.append(line).append('\n');

                        if (line.trim().endsWith(";")) {
                            // end of statement
                            rval.add(buffer.toString());
                            buffer.setLength(0);
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // ignore
                }
            }
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

        return rval;
    }
}
