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
package com.raytheon.uf.common.datastore.ignite.pypies;

import java.io.File;
import java.io.Serializable;

import org.apache.ignite.configuration.IgniteConfiguration;

import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.pypies.PyPiesDataStore;
import com.raytheon.uf.common.pypies.PyPiesDataStoreFactory;
import com.raytheon.uf.common.pypies.PypiesProperties;

/**
 *
 * {@link PyPiesDataStoreFactory} and {@link PypiesProperties} do not implement
 * {@link Serializable} which leads to problems when they are used within an
 * {@link IgniteConfiguration}. This class duplicates PyPiesDataStoreFactory but
 * implements Serializable, it should eventually be deleted and Serializable
 * should be added to PyPiesDataStoreFactory.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 14, 2019  7628     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class SerializablePyPiesDataStoreFactory
        implements IDataStoreFactory, Serializable {

    private static final long serialVersionUID = 1L;

    private String address;

    public SerializablePyPiesDataStoreFactory() {

    }

    public SerializablePyPiesDataStoreFactory(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public IDataStore getDataStore(File file, boolean useLocking) {
        PypiesProperties props = new PypiesProperties();
        props.setAddress(address);
        return new PyPiesDataStore(file, useLocking, props);
    }

}
