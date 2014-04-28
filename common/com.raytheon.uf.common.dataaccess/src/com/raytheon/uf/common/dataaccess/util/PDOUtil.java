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
package com.raytheon.uf.common.dataaccess.util;

import java.io.File;
import java.io.FileNotFoundException;

import org.geotools.coverage.grid.GridGeometry2D;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.persist.IHDFFilePathProvider;
import com.raytheon.uf.common.dataplugin.persist.IPersistable;
import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.geospatial.ISpatialEnabled;
import com.raytheon.uf.common.geospatial.MapUtil;
import com.raytheon.uf.common.localization.IPathManager;

/**
 * The methods in this utility may eventually be added to an abstract class that
 * will be extended by any factory data types that interact with plugin data
 * objects and/or at least any grid-based factories that access the metadata
 * table.
 * 
 * This utility provides access to common methods that may be utilized when
 * interacting with a PluginDataObject (pdo).
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jan 03, 2013           bkowal      Initial creation
 * Jan 31, 2013  1555     bkowal      Made hdf5 variable generic
 * Jan 21, 2014  2667     bclement    added getHDF5File method
 * Feb 06, 2014  2762     bsteffen    Get geometry from IGridGeometryProvider.
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */
public final class PDOUtil {

    /**
	 * 
	 */
    private PDOUtil() {
    }
    
    /**
     * Get the hdf5 file for the data object
     * 
     * @param pdo
     * @return
     */
    public static File getHDF5File(PluginDataObject pdo){
        final String pluginName = pdo.getPluginName();
        final IPersistable persistable = (IPersistable) pdo;

        IHDFFilePathProvider pathProvider = pdo.getHDFPathProvider();
        String hdf5Path = pathProvider.getHDFPath(pluginName,
                persistable);
        String hdf5File = pathProvider.getHDFFileName(pluginName,
                persistable);
        return new File(pluginName + IPathManager.SEPARATOR
                + hdf5Path + IPathManager.SEPARATOR
                + hdf5File);
    }

    /**
     * Get the datastore for the data object
     * 
     * @param pdo
     * @return
     */
    public static IDataStore getDataStore(PluginDataObject pdo) {
        return DataStoreFactory.getDataStore(getHDF5File(pdo));
    }

    /**
     * Retrieves the IDataRecords associated with the provided PluginDataObject.
     * Unlike the other methods that already exist that perform a similar
     * function, this method can be used by both EDEX and CAVE.
     * 
     * @param pdo the PluginDataObject to retrieve the IDataRecords for
     * @return the retrieved IDataRecords
     * @throws FileNotFoundException
     * @throws StorageException
     */
    public static IDataRecord[] getDataRecords(PluginDataObject pdo)
            throws FileNotFoundException, StorageException {
        IDataStore dataStore = getDataStore(pdo);
        return dataStore.retrieve(pdo.getDataURI());
    }

    public static IDataRecord getDataRecord(PluginDataObject pdo,
            String dataset, Request request)
            throws FileNotFoundException, StorageException {
        IDataStore dataStore = getDataStore(pdo);
        return dataStore.retrieve(pdo.getDataURI(), dataset, request);
    }

    /**
     * A utility method to extract and return the GridGeometry2D associated with
     * the provided PluginDataObject.
     * 
     * @param pdo
     *            the pdo to extract the grid geometry from
     * @return the grid geometry
     */
    public static GridGeometry2D retrieveGeometry(PluginDataObject pdo) {
        if (pdo instanceof IGridGeometryProvider) {
            return ((IGridGeometryProvider) pdo).getGridGeometry();
        } else if (pdo instanceof ISpatialEnabled) {
            return MapUtil.getGridGeometry(((ISpatialEnabled) pdo)
                .getSpatialObject());
        } else {
            throw new IllegalArgumentException(
                    "Unable to extract a GridGeometry from an object of type: "
                            + pdo.getClass().getSimpleName());
        }
    }
}
