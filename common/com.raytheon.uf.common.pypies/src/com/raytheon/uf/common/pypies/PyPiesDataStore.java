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
package com.raytheon.uf.common.pypies;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.conn.HttpHostConnectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.comm.CommunicationException;
import com.raytheon.uf.common.comm.HttpClient;
import com.raytheon.uf.common.datastorage.DuplicateRecordStorageException;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageProperties;
import com.raytheon.uf.common.datastorage.StorageProperties.Compression;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.pypies.records.CompressedDataRecord;
import com.raytheon.uf.common.pypies.request.AbstractRequest;
import com.raytheon.uf.common.pypies.request.CopyRequest;
import com.raytheon.uf.common.pypies.request.CreateDatasetRequest;
import com.raytheon.uf.common.pypies.request.DatasetDataRequest;
import com.raytheon.uf.common.pypies.request.DatasetNamesRequest;
import com.raytheon.uf.common.pypies.request.DeleteFilesRequest;
import com.raytheon.uf.common.pypies.request.DeleteOrphansRequest;
import com.raytheon.uf.common.pypies.request.DeleteRequest;
import com.raytheon.uf.common.pypies.request.GroupsRequest;
import com.raytheon.uf.common.pypies.request.RepackRequest;
import com.raytheon.uf.common.pypies.request.RetrieveRequest;
import com.raytheon.uf.common.pypies.request.StoreRequest;
import com.raytheon.uf.common.pypies.response.ErrorResponse;
import com.raytheon.uf.common.pypies.response.FileActionResponse;
import com.raytheon.uf.common.pypies.response.RetrieveResponse;
import com.raytheon.uf.common.pypies.response.StoreResponse;
import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.util.FileUtil;
import com.raytheon.uf.common.util.format.BytesFormat;

/**
 * Data Store implementation that communicates with a PyPIES server over http.
 * The requests and responses are all DynamicSerialized.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer     Description
 * ------------- -------- ------------ -----------------------------------------
 * May 27, 2010           njensen      Initial creation
 * Oct 01, 2010           rjpeter      Added logging of requests over 300ms
 * Mon 07, 2013  15294    D. Friedman  Stream large requests
 * Feb 11, 2013  1526     njensen      use HttpClient.postDynamicSerialize() for
 *                                     memory efficiency
 * Feb 12, 2013  1608     randerso     Added explicit deletes for groups and
 *                                     datasets
 * Nov 14, 2013  2393     bclement     removed interpolation
 * Jul 30, 2015  1574     nabowle      Add #deleteOrphanData(Date[])
 * Jan 27, 2016  5170     tjensen      Added logging of stats to doSendRequests
 * Feb 24, 2016  5389     nabowle      Refactor to
 *                                     #deleteOrphanData(Map<String,Date>)
 * Feb 29, 2016  5420     tgurney      Remove timestampCheck arg from copy()
 * Nov 15, 2016  5992     bsteffen     Compress large records
 * Oct 19, 2017  6367     tgurney      Use logger instead of stdout
 * Sep 19, 2018  7435     ksunil       Eliminate compression/decompression on
 *                                     HDF5
 * Jan 15, 2020  8005     drogalla     Update sendRequest to keep trying until
 *                                     there's a response.
 * Jan 28, 2020  7985     ksunil       Removed the compression changes
 *                                     introduced in 7435
 * Dec 11, 2020  8299     tgurney      Log before and after each request is sent
 * Mar 18, 2021  8349     randerso     Code cleanup
 *
 * </pre>
 *
 * @author njensen
 */
public class PyPiesDataStore implements IDataStore {

    private static final long HUGE_REQUEST = BytesFormat
            .parseSystemProperty("pypies.limits.streaming", "25MiB");

    private static final long COMPRESSION_LIMIT = BytesFormat
            .parseSystemProperty("pypies.limits.compression", "5MiB");

    protected static String address = null;

    protected List<IDataRecord> records = new ArrayList<>();

    protected String filename;

    protected PypiesProperties props;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final AtomicLong requestSequence = new AtomicLong(0);

    /**
     * Constructor
     *
     * @param file
     *            the hdf5 file
     * @param useLocking
     *            true to enable file locking
     * @param props
     *            PyPies DataStore properties
     */
    public PyPiesDataStore(final File file, final boolean useLocking,
            final PypiesProperties props) {
        this.filename = FileUtil.edexPath(file.getPath()); // Win32
        this.props = props;
    }

    @Override
    public void addDataRecord(IDataRecord dataset, StorageProperties properties)
            throws StorageException {
        if (dataset.validateDataSet()) {
            if (dataset.getSizeInBytes() > COMPRESSION_LIMIT) {
                dataset = CompressedDataRecord.convert(dataset);
            }
            dataset.setProperties(properties);
            records.add(dataset);
        } else {
            throw new StorageException(
                    "Invalid dataset " + dataset.getName() + " :" + dataset,
                    null);
        }

    }

    @Override
    public void addDataRecord(final IDataRecord dataset)
            throws StorageException {
        addDataRecord(dataset, dataset.getProperties());
    }

    @Override
    public void createLinks(final Map<String, LinkLocation> links)
            throws StorageException, FileNotFoundException {
        throw new UnsupportedOperationException(
                "pypies does not support this yet!");
    }

    @Override
    public void deleteDatasets(final String... datasets)
            throws StorageException, FileNotFoundException {
        DeleteRequest delete = new DeleteRequest();
        delete.setDatasets(datasets);
        sendRequest(delete);
    }

    @Override
    public void deleteGroups(final String... groups)
            throws StorageException, FileNotFoundException {
        DeleteRequest delete = new DeleteRequest();
        delete.setGroups(groups);
        sendRequest(delete);
    }

    @Override
    public String[] getDatasets(final String group)
            throws StorageException, FileNotFoundException {
        DatasetNamesRequest req = new DatasetNamesRequest();
        req.setGroup(group);
        String[] result = (String[]) cachedRequest(req);
        return result;
    }

    @Override
    public IDataRecord[] retrieve(final String group)
            throws StorageException, FileNotFoundException {
        RetrieveRequest req = new RetrieveRequest();
        req.setGroup(group);
        RetrieveResponse resp = (RetrieveResponse) cachedRequest(req);
        return resp.getRecords();
    }

    @Override
    public IDataRecord retrieve(final String group, final String dataset,
            final Request request)
            throws StorageException, FileNotFoundException {
        RetrieveRequest req = new RetrieveRequest();
        req.setGroup(group);
        req.setDataset(dataset);
        req.setRequest(request);
        RetrieveResponse resp = (RetrieveResponse) cachedRequest(req);
        return resp.getRecords()[0];
    }

    @Override
    public IDataRecord[] retrieveDatasets(final String[] datasetGroupPath,
            final Request request)
            throws StorageException, FileNotFoundException {
        DatasetDataRequest req = new DatasetDataRequest();
        req.setDatasetGroupPath(datasetGroupPath);
        req.setRequest(request);
        RetrieveResponse result = (RetrieveResponse) cachedRequest(req);
        return result.getRecords();
    }

    @Override
    public IDataRecord[] retrieveGroups(final String[] groups,
            final Request request)
            throws StorageException, FileNotFoundException {
        GroupsRequest req = new GroupsRequest();
        req.setGroups(groups);
        req.setRequest(request);

        RetrieveResponse resp = (RetrieveResponse) cachedRequest(req);
        return resp.getRecords();
    }

    @Override
    public StorageStatus store() throws StorageException {
        return store(StoreOp.STORE_ONLY);
    }

    @Override
    public StorageStatus store(final StoreOp storeOp) throws StorageException {
        StoreRequest req = new StoreRequest();
        req.setOp(storeOp);
        req.setRecords(records);

        boolean huge = false;
        long totalSize = 0;
        for (IDataRecord rec : records) {
            totalSize += rec.getSizeInBytes();
            if (totalSize >= HUGE_REQUEST) {
                huge = true;
                break;
            }
        }

        StorageStatus ss = null;
        try {
            StoreResponse sr = (StoreResponse) sendRequest(req, huge);
            ss = sr.getStatus();
            String[] exc = sr.getExceptions();
            IDataRecord[] failed = sr.getFailedRecords();

            // need to set the correlation object
            if (failed != null) {
                for (IDataRecord rec : failed) {
                    Iterator<IDataRecord> recordIter = records.iterator();
                    while (recordIter.hasNext()) {
                        IDataRecord oldRec = recordIter.next();
                        if (oldRec.getGroup().equals(rec.getGroup())
                                && oldRec.getName().equals(rec.getName())) {
                            rec.setCorrelationObject(
                                    oldRec.getCorrelationObject());
                            recordIter.remove();
                            break;
                        }
                    }
                }
            }

            records.clear();
            StorageException[] jexc = new StorageException[exc.length];
            for (int i = 0; i < exc.length; i++) {
                // checking for duplicates based on what is in the string...
                if (exc[i].contains("already exists")) {
                    jexc[i] = new DuplicateRecordStorageException(exc[i],
                            failed[i]);
                } else {
                    jexc[i] = new StorageException(exc[i], failed[i]);
                }
            }

            ss.setExceptions(jexc);
        } catch (StorageException e) {
            ss = new StorageStatus();
            ss.setOperationPerformed(storeOp);
            int size = records.size();
            StorageException[] jexc = new StorageException[size];
            for (int i = 0; i < size; i++) {
                jexc[i] = new StorageException(e.getMessage(), records.get(i),
                        e);
            }
            ss.setExceptions(jexc);
        }
        return ss;
    }

    protected Object sendRequest(final AbstractRequest obj)
            throws StorageException {
        return sendRequest(obj, false);
    }

    protected Object sendRequest(final AbstractRequest obj, boolean huge)
            throws StorageException {
        obj.setFilename(filename);

        initializeProperties();

        Object ret = null;
        long t0 = System.currentTimeMillis();

        boolean logged = false;
        long seqNum = requestSequence.getAndIncrement();
        while (ret == null) {

            try {
                logger.info("Sending " + obj.getClass().getSimpleName()
                        + " (request " + seqNum + ") on file "
                        + obj.getFilename());
                ret = doSendRequest(obj, huge);
            } catch (CommunicationException ce) {
                if (ce.getCause() instanceof HttpHostConnectException) {
                    if (!logged) {
                        logger.error(
                                "Unable to connect with pypies. Check the PyPies logs to see if it is running. Waiting 6 seconds to try again...",
                                ce);
                        logged = true;
                    }

                } else {
                    logger.error(
                            "A CommunicationException occurred trying to communicate with pypies.",
                            ce);
                    break;
                }

            } catch (Exception e) {
                logger.error("Error communicating with pypies server", e);
                break;
            }
        }

        long time = System.currentTimeMillis() - t0;

        logger.info("Took " + time + " ms to receive response for "
                + obj.getClass().getSimpleName() + " (request " + seqNum
                + ") on file " + obj.getFilename());

        if (ret instanceof ErrorResponse) {
            throw new StorageException("(request " + seqNum + ") "
                    + ((ErrorResponse) ret).getError(), null);
        }

        return ret;
    }

    protected Object doSendRequest(final AbstractRequest obj, boolean huge)
            throws Exception {
        if (huge) {
            byte[] resp = HttpClient.getInstance().postBinary(address, os -> {
                try {
                    DynamicSerializationManager
                            .getManager(SerializationType.Thrift)
                            .serialize(obj, os);
                } catch (SerializationException e) {
                    throw new CommunicationException(e);
                }
            });
            return SerializationUtil.transformFromThrift(Object.class, resp);
        } else {
            // can't stream to pypies due to WSGI spec not handling chunked http
            Object response = HttpClient.getInstance()
                    .postDynamicSerialize(address, obj, false);
            /**
             * Log that we have a message. Size information in NOT logged here.
             * Sending a '1' for sent to trigger request increment.
             */
            HttpClient.getInstance().getStats()
                    .log(obj.getClass().getSimpleName(), 1, 0);
            return response;
        }
    }

    /**
     * By default this method simply passes the request to
     * sendRequest(AbstractRequest). Method exists to be overridden for
     * implementations that cache data responses..
     *
     * @param obj
     * @return response object
     * @throws StorageException
     */
    protected Object cachedRequest(final AbstractRequest obj)
            throws StorageException {
        return this.sendRequest(obj);
    }

    protected byte[] serializeRequest(final AbstractRequest request)
            throws StorageException {
        try {
            return SerializationUtil.transformToThrift(request);
        } catch (SerializationException e) {
            throw new StorageException("Error serializing request", null, e);
        }
    }

    protected Object deserializeResponse(final byte[] response)
            throws StorageException {
        try {
            return SerializationUtil.transformFromThrift(Object.class,
                    response);
        } catch (SerializationException e) {
            throw new StorageException(
                    "Error deserializing response from pypies server", null, e);
        }
    }

    protected synchronized void initializeProperties() {
        if (address == null) {
            address = props.getAddress();
        }
    }

    @Override
    public void deleteFiles(final String[] datesToDelete)
            throws StorageException, FileNotFoundException {
        DeleteFilesRequest req = new DeleteFilesRequest();
        req.setDatesToDelete(datesToDelete);
        sendRequest(req);
    }

    @Override
    public void createDataset(final IDataRecord rec)
            throws StorageException, FileNotFoundException {
        CreateDatasetRequest req = new CreateDatasetRequest();
        req.setRecord(rec);
        sendRequest(req);
    }

    @Override
    public void repack(final Compression compression) throws StorageException {
        RepackRequest req = new RepackRequest();
        req.setFilename(this.filename);
        req.setCompression(compression);
        FileActionResponse resp = (FileActionResponse) sendRequest(req);

        /*
         * TODO do we really want to make this an exception? reasoning is if the
         * repack fails for some reason, the original file is left as is, just
         * isn't as efficiently packed
         */
        if ((resp != null) && (resp.getFailedFiles() != null)
                && (resp.getFailedFiles().length > 0)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error repacking the following files: ");
            String[] failed = resp.getFailedFiles();
            for (int i = 0; i < failed.length; i++) {
                sb.append(failed[i]);
                if (i < failed.length - 1) {
                    sb.append(", ");
                }
            }
            throw new StorageException(sb.toString(), null);
        }
    }

    @Override
    public void copy(final String outputDir, final Compression compression,
            final int minMillisSinceLastChange,
            final int maxMillisSinceLastChange) throws StorageException {
        CopyRequest req = new CopyRequest();
        req.setFilename(this.filename);
        if (compression != null) {
            req.setRepack(true);
            req.setRepackCompression(compression);
        } else {
            req.setRepack(false);
        }
        req.setOutputDir(outputDir);
        req.setMinMillisSinceLastChange(minMillisSinceLastChange);
        FileActionResponse resp = (FileActionResponse) sendRequest(req);

        if ((resp != null) && (resp.getFailedFiles() != null)
                && (resp.getFailedFiles().length > 0)) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error copying the following files: ");
            String[] failed = resp.getFailedFiles();
            for (int i = 0; i < failed.length; i++) {
                sb.append(failed[i]);
                if (i < failed.length - 1) {
                    sb.append(", ");
                }
            }
            throw new StorageException(sb.toString(), null);
        }
    }

    @Override
    public void deleteOrphanData(Map<String, Date> oldestDateMap)
            throws StorageException {
        if (oldestDateMap == null || oldestDateMap.isEmpty()) {
            return;
        }

        DeleteOrphansRequest req = new DeleteOrphansRequest(filename,
                oldestDateMap);
        FileActionResponse resp = (FileActionResponse) sendRequest(req);

        String[] failed = resp.getFailedFiles();
        if (failed != null && failed.length > 0) {
            StringBuilder sb = new StringBuilder();
            sb.append("Error deleting the following orphaned files: ");
            for (int i = 0; i < failed.length; i++) {
                sb.append(failed[i]);
                if (i < failed.length - 1) {
                    sb.append(", ");
                }
            }
            throw new StorageException(sb.toString(), null);
        }
    }

}
