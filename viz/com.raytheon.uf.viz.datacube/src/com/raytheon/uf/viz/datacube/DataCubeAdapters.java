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
package com.raytheon.uf.viz.datacube;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains a registry of IDataCubeAdapter instances and provides the interface
 * for registering new adapters
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 01, 2014            ekladstrup  Initial creation
 * Jun 27, 2017  6316      njensen     Switched from ufstatus to slf4j for logging
 * Dec 11, 2017            mjames      Less logging (re-implemented 3/15/23)
 * 
 * </pre>
 * 
 * @author ekladstrup
 */

public class DataCubeAdapters {

    private final Logger logger = LoggerFactory
            .getLogger(DataCubeAdapters.class);

    protected Map<String, IDataCubeAdapter> adapters = new HashMap<>();

    protected Map<IDataCubeAdapter, Boolean> initializedMap = new HashMap<>();

    protected static DataCubeAdapters instance = new DataCubeAdapters();

    /**
     * Get singleton instance
     * 
     * @return
     */
    public static DataCubeAdapters getInstance() {
        return instance;
    }

    /**
     * Internal constructor, use getInstance()
     */
    protected DataCubeAdapters() {

    }

    /**
     * Add an adapter to the list of known adapters
     * 
     * @param adapter
     * @return the same adapter
     */
    public IDataCubeAdapter registerAdapter(IDataCubeAdapter adapter) {
        synchronized (adapters) {
            String[] supportedPlugins = adapter.getSupportedPlugins();
            for (String plugin : supportedPlugins) {
                IDataCubeAdapter registeredAdapter = adapters.get(plugin);
                if (registeredAdapter != null) {
                    logger.warn("Already registered "
                            + registeredAdapter.getClass().getCanonicalName()
                            + " IDataCubeAdapter for plugin \"" + plugin
                            + "\" ignoring "
                            + adapter.getClass().getCanonicalName());
                } else {
                    adapters.put(plugin, adapter);
                }
            }
        }
        synchronized (initializedMap) {
            initializedMap.put(adapter, false);
        }
        return adapter;
    }

    /**
     * Initialize the adapters inventory if it has not already, return true if
     * the inventory was initialized
     * 
     * @param adapter
     * @return true if the adapter was initialized false if it was not ( or is
     *         unknown to this service )
     */
    public boolean initIfNeeded(IDataCubeAdapter adapter) {
        boolean wasInitialized = false;

        if (adapter != null) {
            synchronized (adapter) {
                Boolean initialized = initializedMap.get(adapter);
                if (initialized == null || !initialized) {
                    adapter.initInventory();
                    initializedMap.put(adapter, true);
                    wasInitialized = true;
                }
            }
        }

        return wasInitialized;
    }

    /**
     * Search for an adapter for a given plugin
     * 
     * @param plugin
     * @return the first adapter that supports the plugin, null if none are
     *         found
     */
    public IDataCubeAdapter getAdapterForPlugin(String plugin) {
        IDataCubeAdapter result = null;

        synchronized (adapters) {
            result = adapters.get(plugin);
        }

        return result;
    }
}
