/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 * 
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 * 
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 * 
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.uf.edex.pointdata.ignite;

import com.raytheon.uf.common.dataplugin.IPluginRegistryChanged;
import com.raytheon.uf.common.dataplugin.PluginProperties;
import com.raytheon.uf.common.datastore.ignite.plugin.CachePluginRegistry;
import com.raytheon.uf.edex.core.dataplugin.PluginRegistry;
import com.raytheon.uf.edex.pointdata.PointDataPluginDao;

/**
 * 
 * Detect Plugins that use {@link PointDataPluginDao} and assign them to a
 * dedicated ignite cache. Since pointdata uses frequent appends it has a
 * significantly different usage patterns compared to other plugins so the
 * overall system performs better by keeping pointdata separate.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Feb 14, 2020  7628     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgnitePointDataCacheConfigurator
        implements IPluginRegistryChanged {

    protected final PluginRegistry pluginRegistry;

    protected final CachePluginRegistry cacheRegistry;

    protected final String cacheName;

    public IgnitePointDataCacheConfigurator(PluginRegistry pluginRegistry,
            CachePluginRegistry cacheRegistry, String cacheName) {
        this.pluginRegistry = pluginRegistry;
        this.cacheRegistry = cacheRegistry;
        this.cacheName = cacheName;
        pluginRegistry.addListener(this);
        for (String pluginName : pluginRegistry.getRegisteredObjects()) {
            pluginAdded(pluginName);
        }
    }

    @Override
    public void pluginAdded(String pluginName) {
        PluginProperties props = pluginRegistry.getRegisteredObject(pluginName);
        if (props != null) {
            Class<?> dao = props.getDao();
            if (dao != null && PointDataPluginDao.class.isAssignableFrom(dao)) {
                cacheRegistry.registerPluginCacheName(pluginName, cacheName);
            }
        }
    }

}
