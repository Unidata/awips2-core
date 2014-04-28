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

package com.raytheon.edex.db.dao;

import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.edex.database.plugin.PluginDao;

/**
 * Base implementation of a Plugin data access object. This class is used when
 * the HDF5 functionality is not used for a particular plugin.
 * <p>
 * Plugins may extend this class override methods on this class if specific
 * functionality is desired
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 2/6/09       1990       bphillip    Initial creation
 * </pre>
 * 
 * @author bphillip
 * @version 1.0
 */
public class DefaultPluginDao extends PluginDao {

    /**
     * Constructs a DefaultPluginDao for the given plugin
     * 
     * @param pluginName
     *            The plugin name
     * @throws PluginException
     *             If errors occur during construction
     */
    public DefaultPluginDao(String pluginName) throws PluginException {
        super(pluginName);
    }

    @Override
    protected IDataStore populateDataStore(IDataStore dataStore,
            IPersistable obj) throws Exception {
        // Default no op
        return null;
    }
}
