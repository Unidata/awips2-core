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
import java.util.Date;
import java.util.Map;

import com.raytheon.uf.common.datastorage.StorageProperties.Compression;
import com.raytheon.uf.common.datastorage.records.IDataRecord;

/**
 * Lazily-created {@link IDataStore}. Underlying datastore is not created until
 * first method call
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 9, 2020  8299      tgurney     Initial creation
 *
 * </pre>
 *
 * @author tgurney
 */

public abstract class LazyDataStore implements IDataStore {

    private volatile IDataStore delegate;

    private final Object delegateLock = new Object();

    protected abstract IDataStore createDataStore();

    private IDataStore getDelegate() {
        synchronized (delegateLock) {
            if (delegate == null) {
                delegate = createDataStore();
            }
            return delegate;
        }
    }

    @Override
    public void addDataRecord(IDataRecord dataset, StorageProperties properties)
            throws StorageException {
        getDelegate().addDataRecord(dataset, properties);
    }

    @Override
    public void addDataRecord(IDataRecord dataset) throws StorageException {
        getDelegate().addDataRecord(dataset);
    }

    @Override
    public StorageStatus store() throws StorageException {
        return getDelegate().store();
    }

    @Override
    public void deleteDatasets(String... datasets)
            throws StorageException, FileNotFoundException {
        getDelegate().deleteDatasets(datasets);
    }

    @Override
    public void deleteGroups(String... groups)
            throws StorageException, FileNotFoundException {
        getDelegate().deleteGroups(groups);
    }

    @Override
    public StorageStatus store(StoreOp storeOp) throws StorageException {
        return getDelegate().store(storeOp);
    }

    @Override
    public IDataRecord[] retrieve(String group)
            throws StorageException, FileNotFoundException {
        return getDelegate().retrieve(group);
    }

    @Override
    public IDataRecord retrieve(String group, String dataset, Request request)
            throws StorageException, FileNotFoundException {
        return getDelegate().retrieve(group, dataset, request);
    }

    @Override
    public IDataRecord[] retrieveDatasets(String[] datasetGroupPath,
            Request request) throws StorageException, FileNotFoundException {
        return getDelegate().retrieveDatasets(datasetGroupPath, request);
    }

    @Override
    public IDataRecord[] retrieveGroups(String[] groups, Request request)
            throws StorageException, FileNotFoundException {
        return getDelegate().retrieveGroups(groups, request);
    }

    @Override
    public String[] getDatasets(String group)
            throws StorageException, FileNotFoundException {
        return getDelegate().getDatasets(group);
    }

    @Override
    public void createLinks(Map<String, LinkLocation> links)
            throws StorageException, FileNotFoundException {
        getDelegate().createLinks(links);
    }

    @Override
    public void deleteFiles(String[] datesToDelete)
            throws StorageException, FileNotFoundException {
        getDelegate().deleteFiles(datesToDelete);
    }

    @Override
    public void createDataset(IDataRecord rec)
            throws StorageException, FileNotFoundException {
        getDelegate().createDataset(rec);
    }

    @Override
    public void repack(Compression compression) throws StorageException {
        getDelegate().repack(compression);
    }

    @Override
    public void copy(String outputDir, Compression compression,
            int minMillisSinceLastChange, int maxMillisSinceLastChange)
            throws StorageException {
        getDelegate().copy(outputDir, compression, minMillisSinceLastChange,
                maxMillisSinceLastChange);
    }

    @Override
    public void deleteOrphanData(Map<String, Date> dateMap)
            throws StorageException {
        getDelegate().deleteOrphanData(dateMap);
    }

}
