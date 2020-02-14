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
package com.raytheon.uf.common.datastore.ignite;

import java.io.File;
import java.util.Arrays;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.multicast.TcpDiscoveryMulticastIpFinder;

import com.raytheon.uf.common.datastorage.IDataStoreFactory;

/**
 * 
 * {@link IDataStoreFactory} for making {@link IgniteDataStore}s.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 29, 2019  7628     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class IgniteDataStoreFactory implements IDataStoreFactory {

    private final Ignite ignite;

    private final CachePluginRegistry pluginRegistry;

    public IgniteDataStoreFactory() {
        this(getDefaultConfig());
    }

    public IgniteDataStoreFactory(IgniteConfiguration config) {
        this(Ignition.getOrStart(config));
    }

    public IgniteDataStoreFactory(Ignite ignite) {
        this(ignite, new CachePluginRegistry());
    }

    public IgniteDataStoreFactory(IgniteConfiguration config,
            CachePluginRegistry pluginRegistry) {
        this(Ignition.getOrStart(config), pluginRegistry);
    }

    public IgniteDataStoreFactory(Ignite ignite,
            CachePluginRegistry pluginRegistry) {
        this.ignite = ignite;
        this.pluginRegistry = pluginRegistry;
        this.pluginRegistry.setIgnite(ignite);
    }

    @Override
    public IgniteDataStore getDataStore(File file, boolean useLocking) {
        return new IgniteDataStore(file,
                ignite.getOrCreateCache(pluginRegistry.getCacheName(file)));
    }

    private static IgniteConfiguration getDefaultConfig() {
        IgniteConfiguration config = new IgniteConfiguration();
        config.setClassLoader(IgniteDataStoreFactory.class.getClassLoader());
        config.setClientMode(true);
        TcpDiscoverySpi discoSpi = new TcpDiscoverySpi();
        discoSpi.setJoinTimeout(5000);
        TcpDiscoveryMulticastIpFinder ipFinder = new TcpDiscoveryMulticastIpFinder();
        ipFinder.setAddresses(Arrays.asList("localhost"));
        discoSpi.setIpFinder(ipFinder);
        config.setDiscoverySpi(discoSpi);

        return config;
    }

}
