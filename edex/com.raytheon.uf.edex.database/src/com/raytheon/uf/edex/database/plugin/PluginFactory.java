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

import java.util.HashMap;
import java.util.Map;

import javax.persistence.Table;

import org.apache.commons.beanutils.ConstructorUtils;

import com.raytheon.uf.common.dataplugin.IPluginClassMapper;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.PluginProperties;
import com.raytheon.uf.common.dataplugin.persist.IHDFFilePathProvider;
import com.raytheon.uf.edex.core.dataplugin.PluginRegistry;

/**
 * Factory class for retrieving elements common to all uFrame data plugins
 * 
 * <pre>
 * 
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 14, 2006            garmendariz Initial check-in
 * May 29, 2007 312        bphillip    Removed unused methods
 * Feb 06, 2009 1990       bphillip    Refactored to use spring container
 * Mar 20, 2009            njensen     Refactored to use PluginProperties
 * May 16, 2013 1869       bsteffen    Rewrite dataURI property mappings.
 * Mar 19, 2014 2726       rjpeter     Added defaultPathProvider field.
 * </pre>
 * 
 * @author garmendariz
 * @version 1.0
 */
public class PluginFactory implements IPluginClassMapper {

    /** The instance of the PluginFactory class */
    private static final PluginFactory instance = new PluginFactory();

    /**
     * Gets the singleton instance of the PluginFactory
     * 
     * @return The singleton instance of the PluginFactory
     * @throws PluginException
     *             If errors occur during instantiation of the singleton
     *             instance
     */
    public static PluginFactory getInstance() {
        return instance;
    }

    private Map<String, PluginDao> pluginDaoMap = new HashMap<String, PluginDao>();

    private final Object daoMapLock = new Object();

    private IHDFFilePathProvider defaultPathProvider = null;

    /**
     * Private constructor
     * 
     * @throws PluginException
     *             If errors occur loading environment properties or while
     *             initializing the plugins
     */
    private PluginFactory() {
    }

    public boolean isRegistered(String pluginName) {
        return PluginRegistry.getInstance().getRegisteredObject(pluginName) != null;
    }

    /**
     * Gets the data access object for the specified plugin
     * 
     * @param pluginName
     *            The name of the plugin
     * @return An instance of the dao
     * @throws PluginException
     *             If the dao cannot be instantiated
     */
    public PluginDao getPluginDao(String pluginName) throws PluginException {
        PluginDao dao = pluginDaoMap.get(pluginName);
        if (dao == null) {
            PluginProperties props = PluginRegistry.getInstance()
                    .getRegisteredObject(pluginName);
            if (props != null) {
                try {
                    synchronized (daoMapLock) {
                        // Create dao
                        dao = (PluginDao) ConstructorUtils.invokeConstructor(
                                props.getDao(), pluginName);

                        // Copy dao mapping
                        Map<String, PluginDao> pluginDaoMapCopy = new HashMap<String, PluginDao>(
                                pluginDaoMap);
                        // Add dao
                        pluginDaoMapCopy.put(pluginName, dao);
                        // Reset mapping
                        pluginDaoMap = pluginDaoMapCopy;
                    }
                } catch (Exception e) {
                    throw new PluginException("Error instantiating DAO for "
                            + pluginName + " plugin!", e);
                }
            } else {
                throw new PluginException("Plugin " + pluginName
                        + " is not registered with the PluginRegistry");
            }
        }
        return dao;
    }

    /**
     * Gets the initializer for the specified plugin
     * 
     * @param pluginName
     *            The plugin name
     * @return An instance of the initializer
     * @throws PluginException
     *             If the initializer cannot be instantiated
     */
    @SuppressWarnings("unchecked")
    public IPluginInitializer getPluginInitializer(String pluginName)
            throws PluginException {
        PluginProperties props = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        if (props != null) {
            try {
                Class<IPluginInitializer> clz = (Class<IPluginInitializer>) props
                        .getInitializer();
                return (IPluginInitializer) ConstructorUtils.invokeConstructor(
                        clz, pluginName);
            } catch (Exception e) {
                throw new PluginException(
                        "Error instantiating initializer for " + pluginName
                                + " plugin!", e);
            }
        } else {
            throw new PluginException("Plugin " + pluginName
                    + " is not registered with the PluginRegistry");
        }
    }

    /**
     * Gets the record class object for the specified plugin
     * 
     * @param pluginName
     *            The plugin name
     * @return The record class object for the specified plugin
     * @throws PluginException
     * @throws PluginException
     *             If the class cannot be determined
     */
    @Override
    public Class<PluginDataObject> getPluginRecordClass(String pluginName)
            throws PluginException {
        PluginProperties props = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        if (props != null) {
            try {
                return props.getRecord();
            } catch (Exception e) {
                throw new PluginException("Unable to find record class for "
                        + pluginName + " plugin!", e);
            }

        } else {
            throw new PluginException("Plugin " + pluginName
                    + " is not registered with the PluginRegistry");
        }
    }

    /**
     * Gets the record class name for the specified plugin
     * 
     * @param pluginName
     *            The plugin name
     * @return The record class name for the specified plugin
     * @throws PluginException
     * @throws PluginException
     * @throws PluginException
     *             If the class cannot be determined
     */
    public String getPluginRecordClassName(String pluginName)
            throws PluginException {
        return getPluginRecordClass(pluginName).getName();
    }

    /**
     * Gets the name of the database this plugin stores its data
     * 
     * @param pluginName
     *            The plugin name
     * @return The name of the database this plugin stores its data
     * @throws PluginException
     */
    public String getDatabase(String pluginName) throws PluginException {
        PluginProperties props = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        if (props != null) {
            return props.getDatabase();
        } else {
            throw new PluginException("Plugin " + pluginName
                    + " is not registered with the PluginRegistry");
        }
    }

    public IHDFFilePathProvider getPathProvider(String pluginName) {
        IHDFFilePathProvider rval = null;
        PluginProperties props = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        if (props != null) {
            rval = props.getPathProvider();
        } else {
            rval = defaultPathProvider;
        }
        return rval;
    }

    /**
     * Gets the name of the primary table the specified plugin stores its data
     * to
     * 
     * @param pluginName
     *            The plugin name
     * @return The table name.
     * @throws PluginException
     *             If the table name cannot be determined
     */
    public String getPrimaryTable(String pluginName) throws PluginException {
        Class<PluginDataObject> clazz = this.getPluginRecordClass(pluginName);
        if (clazz == null) {
            return "<unspecified>";
        }
        Table table = clazz.getAnnotation(Table.class);
        if (table == null) {
            return "<unspecified>";
        } else {
            return table.name();
        }
    }

    /**
     * Gets the initial retention time of the plugin
     * 
     * @param pluginName
     *            the plugin name
     * @return the initial retention time in hours
     * @throws PluginException
     */
    public int getInitialRetentionTime(String pluginName)
            throws PluginException {
        PluginProperties props = PluginRegistry.getInstance()
                .getRegisteredObject(pluginName);
        if (props != null) {
            return props.getInitialRetentionTime();
        } else {
            throw new PluginException("Plugin " + pluginName
                    + " is not registered with the PluginRegistry");
        }
    }

    /**
     * Set the default path provider. Used by spring to configure the factory.
     * 
     * @param defaultPathProvider
     */
    public void setDefaultPathProvider(IHDFFilePathProvider defaultPathProvider) {
        this.defaultPathProvider = defaultPathProvider;
    }
}
