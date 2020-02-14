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
package com.raytheon.uf.common.datastore.ignite.store;

import java.io.File;

import javax.cache.configuration.Factory;

import org.apache.ignite.resources.CacheNameResource;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;

/**
 * 
 * {@link Factory} for making {@link DataStoreCacheStore}.
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
public class DataStoreCacheStoreFactory
        implements Factory<DataStoreCacheStore> {

    private static final long serialVersionUID = 1L;

    @CacheNameResource
    private String cacheName;

    private boolean useLocking = true;

    private IDataStoreFactory dataStoreFactory;

    public DataStoreCacheStoreFactory() {

    }

    public DataStoreCacheStoreFactory(IDataStoreFactory dataStoreFactory) {
        this.dataStoreFactory = dataStoreFactory;
    }

    public DataStoreCacheStoreFactory(IDataStoreFactory dataStoreFactory,
            String cacheName) {
        this.dataStoreFactory = dataStoreFactory;
        this.cacheName = cacheName;
    }

    public DataStoreCacheStoreFactory(IDataStoreFactory dataStoreFactory,
            String cacheName, boolean useLocking) {
        this.dataStoreFactory = dataStoreFactory;
        this.cacheName = cacheName;
        this.useLocking = useLocking;
    }

    @Override
    public DataStoreCacheStore create() {
        if (dataStoreFactory == null) {
            dataStoreFactory = DataStoreFactory.getInstance()
                    .getUnderlyingFactory();
        }
        return new DataStoreCacheStore(dataStoreFactory, useLocking);
    }

    public IDataStore getDataStore(File file) {
        if (dataStoreFactory == null) {
            dataStoreFactory = DataStoreFactory.getInstance()
                    .getUnderlyingFactory();
        }
        return dataStoreFactory.getDataStore(file, useLocking);
    }

    public boolean isUseLocking() {
        return useLocking;
    }

    public void setUseLocking(boolean useLocking) {
        this.useLocking = useLocking;
    }

    public IDataStoreFactory getDataStoreFactory() {
        return dataStoreFactory;
    }

    public void setDataStoreFactory(IDataStoreFactory dataStoreFactory) {
        this.dataStoreFactory = dataStoreFactory;
    }

}
