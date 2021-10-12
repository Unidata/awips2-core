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

package com.raytheon.uf.common.datastorage;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import com.raytheon.uf.common.datastorage.StorageProperties.Compression;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IMetadataIdentifier;

/**
 * Defines the interface for operating against a hierarchical datastore
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 09, 2007           chammack    Initial Creation.
 * Apr 01, 2008           chammack    Added delete API
 * Aug 03, 2009           chammack    Modified to support Request
 * Sep 27, 2010  5091     njensen     Added deleteFiles(String)
 * Feb 12, 2013  1608     randerso    Added explicit methods for deleting
 *                                    groups and datasets
 * Sep 19, 2013  2309     bsteffen    Deprecate retrieve(String, boolean)
 * Nov 14, 2013  2393     bclement    removed interpolation
 * Nov 20, 2014  3853     njensen     Deprecated OVERWRITE StoreOp
 * Jul 30, 2015  1574     nabowle     Add #deleteOrphanData(Date)
 * Feb 24, 2016  5389     nabowle     Refactor to #deleteOrphanData(Map<String,Date>)
 * Feb 29, 2016  5420     tgurney     Remove timestampCheck arg from copy()
 * Sep 23, 2021  8608     mapeters    Add metadata identifier handling
 *
 * </pre>
 *
 * @author chammack
 */
public interface IDataStore {

    public enum HDF5_ITEM {
        DATASET, GROUP
    }

    public enum StoreOp {
        STORE_ONLY, REPLACE, APPEND, @Deprecated
        OVERWRITE
    }

    /**
     * Add a data record with optional properties.
     *
     * NOTE: Record is not written to disk until store method is called.
     *
     * @param dataset
     *            the data to add to the write
     * @param metadataIdentifier
     *            identifies the metadata entry that references the given
     *            dataset
     * @param properties
     *            the storage characteristics of the data (optional)
     * @throws StorageException
     */
    void addDataRecord(IDataRecord dataset,
            IMetadataIdentifier metadataIdentifier,
            StorageProperties properties) throws StorageException;

    /**
     * Add a data record
     *
     * NOTE: Record is not written to disk until store method is called.
     *
     * @param dataset
     *            the data to add to the write
     * @param metadataIdentifier
     *            identifies the metadata entry that references the dataset
     * @throws StorageException
     */
    void addDataRecord(IDataRecord dataset,
            IMetadataIdentifier metadataIdentifier) throws StorageException;

    /**
     * Add a data record
     *
     * NOTE: Record is not written to disk until store method is called.
     *
     * @param dataset
     *            the data to add to the write
     * @param metadataIdentifiers
     *            identifies the metadata entry that references the dataset -
     *            note that generally there is only one metadata identifier per
     *            dataset, but an example of where there may be multiple is with
     *            partial data. Each partial data piece has its own metadata
     *            identifier that all match except for trace ID, and once the
     *            pieces all get merged into one dataset, you have multiple
     *            metadata identifiers for that one dataset.
     * @throws StorageException
     */
    void addDataRecord(IDataRecord dataset,
            Collection<IMetadataIdentifier> metadataIdentifiers)
            throws StorageException;

    /**
     * Add a datarecord
     *
     * NOTE: Record is not written to disk until store method is called.
     *
     * @param dataset
     *            the data to add to the write
     * @param metadataIdentifiers
     *            identifies the metadata entry that references the dataset -
     *            note that generally there is only one metadata identifier per
     *            dataset, but an example of where there may be multiple is with
     *            partial data. Each partial data piece has its own metadata
     *            identifier that all match except for trace ID, and once the
     *            pieces all get merged into one dataset, you have multiple
     *            metadata identifiers for that one dataset.
     * @throws StorageException
     */
    void addDataRecord(IDataRecord dataset,
            Collection<IMetadataIdentifier> metadataIdentifiers,
            StorageProperties properties) throws StorageException;

    /**
     * Store all data records
     *
     * Stores all data added using the addDataRecord methods.
     *
     * @throws StorageException
     */
    StorageStatus store() throws StorageException;

    /**
     * Delete one or more datasets. If all datasets have been deleted from a
     * file, the file will be deleted also.
     *
     * @param datasets
     *            the full path to the dataset(s) to be deleted
     * @throws StorageException
     *             if deletion fails
     * @throws FileNotFoundException
     */
    void deleteDatasets(String... datasets)
            throws StorageException, FileNotFoundException;

    /**
     * Delete one or more groups and all subgroups/datasets they contain. If all
     * datasets have been deleted from a file, the file will be deleted also.
     *
     * @param groups
     *            the full path to the group(s) to be deleted
     * @throws StorageException
     *             if deletion fails
     * @throws FileNotFoundException
     */
    void deleteGroups(String... groups)
            throws StorageException, FileNotFoundException;

    /**
     * Store all data records to a given data group, or replace it the group
     * already exists. Works similarly to store, except for the ability to
     * replace data.
     *
     * @param storeOp
     *            store operation
     * @throws StorageException
     */
    StorageStatus store(StoreOp storeOp) throws StorageException;

    /**
     * Convenience method for retrieve.
     *
     * Retrieves all data (except interpolated tilesets) at a given group.
     *
     * @param group
     *            the group of data to retrieve
     * @return the data records
     * @throws StorageException
     * @throws FileNotFoundException
     */
    IDataRecord[] retrieve(String group)
            throws StorageException, FileNotFoundException;

    /**
     * Retrieve a single dataset with optional subsetting
     *
     * @param group
     *            the data group name
     * @param dataset
     *            the dataset name
     * @param request
     *            the request type to perform
     * @return the data record
     * @throws StorageException
     * @throws FileNotFoundException
     */
    IDataRecord retrieve(String group, String dataset, Request request)
            throws StorageException, FileNotFoundException;

    /**
     * Retrieve multiple datasets from a single file
     *
     *
     * @param datasetGroupPath
     *            the full path to a dataset.
     * @param request
     *            the request type to perform
     * @return a set of datarecords
     * @throws StorageException
     * @throws FileNotFoundException
     */
    IDataRecord[] retrieveDatasets(String[] datasetGroupPath, Request request)
            throws StorageException, FileNotFoundException;

    /**
     * Retrieve multiple groups from a single file, retrieves all datasets from
     * each group.
     *
     * NOTE: The request is applied to every group
     *
     * @param groups
     *            the group names
     * @param request
     *            The request type to perform
     * @return the data records
     * @throws StorageException
     * @throws FileNotFoundException
     */
    IDataRecord[] retrieveGroups(String[] groups, Request request)
            throws StorageException, FileNotFoundException;

    /**
     * List all the datasets available inside a group
     *
     * @param group
     *            the group
     * @return a list of datasets available
     * @throws StorageException
     * @throws FileNotFoundException
     *
     */
    String[] getDatasets(String group)
            throws StorageException, FileNotFoundException;

    public static class LinkLocation {
        /** Optional: Used for when the link is in another file */
        public String fileName;

        /** Required: The point to where the link should be made */
        public String linkTarget;
    }

    /**
     * Create links from a point in the current file to another point in the
     * same file, or a point in another file.
     *
     * @param links
     *            the links to create
     */
    void createLinks(Map<String, LinkLocation> links)
            throws StorageException, FileNotFoundException;

    /**
     * Deletes the provided list of dates. The directory from which to delete
     * the hdf5 files is created by appending to the base HDF5 directory path.
     * All files named according to the provided list of dates is deleted.
     *
     * @param datesToDelete
     *            The dates to delete
     * @throws StorageException
     *             If errors occur while deleting data from the HDF5 file
     * @throws FileNotFoundException
     *             If the HDF5 file does not exist
     */
    void deleteFiles(String[] datesToDelete)
            throws StorageException, FileNotFoundException;

    /**
     * Creates an empty dataset with the specified dimensions, type, and fill
     * value
     *
     * @param rec
     *            an empty record containing the attributes of the dataset
     */
    void createDataset(IDataRecord rec)
            throws StorageException, FileNotFoundException;

    /**
     * Recursively repacks all files of a certain directory. Presumes that the
     * IDataStore instance is tied to a directory, not a specific file.
     *
     * @param compression
     *            the type of compression to repack with
     */
    void repack(Compression compression) throws StorageException;

    /**
     * Recursively copies all files of a certain directory. If compression is
     * specified the file will be repacked to the specified compression.
     * Presumes that the IDataStore instance is tied to a directory, not a
     * specific file.
     *
     * @param outputDir
     *            the output directory to put the copied files
     * @param compression
     *            If specified will repack the output file with a given
     *            compression
     * @param minMillisSinceLastChange
     *            if greater than 0, the last modified time on the file cannot
     *            be within minMillisSinceLastChange from current time. This is
     *            used to not repack files that have changed within a recent
     *            threshold.
     * @param maxMillisSinceLastChange
     *            if greater than 0, the last modified time on the file must be
     *            within maxMillisSinceLastChange from current time. This is
     *            used to ignore files that have not changed within a recent
     *            threshold.
     */
    void copy(String outputDir, Compression compression,
            int minMillisSinceLastChange, int maxMillisSinceLastChange)
            throws StorageException;

    /**
     * Deletes orphaned data.
     *
     * @param dateMap
     *            A map of the oldest dates which should be kept for distinct
     *            subsets of the plugin's data identified by the keys of this
     *            map.
     *
     *            For each entry in this map, any data that matches the key will
     *            be deleted if it is older than the mapped Date .
     *
     *            The plugin name should be used to specify a single date for
     *            the plugin's entire set of data if specific dates do not
     *            apply.
     * @throws StorageException
     *             if any orphan data failed to be deleted.
     */
    void deleteOrphanData(Map<String, Date> dateMap) throws StorageException;
}
