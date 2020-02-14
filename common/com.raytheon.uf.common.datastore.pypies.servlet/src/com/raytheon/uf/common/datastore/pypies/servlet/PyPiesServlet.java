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
package com.raytheon.uf.common.datastore.pypies.servlet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.zip.GZIPInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.common.datastorage.DataStoreFactory;
import com.raytheon.uf.common.datastorage.IDataStore;
import com.raytheon.uf.common.datastorage.IDataStoreFactory;
import com.raytheon.uf.common.datastorage.Request;
import com.raytheon.uf.common.datastorage.StorageException;
import com.raytheon.uf.common.datastorage.StorageStatus;
import com.raytheon.uf.common.datastorage.records.ByteDataRecord;
import com.raytheon.uf.common.datastorage.records.DoubleDataRecord;
import com.raytheon.uf.common.datastorage.records.FloatDataRecord;
import com.raytheon.uf.common.datastorage.records.IDataRecord;
import com.raytheon.uf.common.datastorage.records.IntegerDataRecord;
import com.raytheon.uf.common.datastorage.records.LongDataRecord;
import com.raytheon.uf.common.datastorage.records.ShortDataRecord;
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
import com.raytheon.uf.common.pypies.response.DeleteResponse;
import com.raytheon.uf.common.pypies.response.FileActionResponse;
import com.raytheon.uf.common.pypies.response.RetrieveResponse;
import com.raytheon.uf.common.pypies.response.StoreResponse;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.serialization.SerializationUtil;
import com.raytheon.uf.common.util.ByteArrayOutputStreamPool;
import com.raytheon.uf.common.util.PooledByteArrayOutputStream;

/**
 * 
 * A servlet that can handle requests sent by a PyPies client and handles them
 * with an underlying {@link IDataStore}
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 18, 2019  7628     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class PyPiesServlet extends HttpServlet {

    protected static final long serialVersionUID = 1L;

    private static final Logger logger = LoggerFactory
            .getLogger(PyPiesServlet.class);

    private final IDataStoreFactory factory;

    private final boolean useLocking;

    public PyPiesServlet() {
        this(DataStoreFactory.getInstance().getUnderlyingFactory());
    }

    public PyPiesServlet(IDataStoreFactory factory) {
        this(factory, true);
    }

    public PyPiesServlet(IDataStoreFactory factory, boolean useLocking) {
        this.factory = factory;
        this.useLocking = useLocking;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        try {
            long t0 = System.currentTimeMillis();
            AbstractRequest request = SerializationUtil.transformFromThrift(
                    AbstractRequest.class, req.getInputStream());
            long t1 = System.currentTimeMillis();
            Object response = null;
            IDataStore dataStore = factory
                    .getDataStore(new File(request.getFilename()), useLocking);
            if (request instanceof CopyRequest) {
                response = handleCopyRequest(dataStore, (CopyRequest) request);
            } else if (request instanceof CreateDatasetRequest) {
                response = handleCreateDatasetRequest(dataStore,
                        (CreateDatasetRequest) request);
            } else if (request instanceof DatasetDataRequest) {
                response = handleDatasetDataRequest(dataStore,
                        (DatasetDataRequest) request);
            } else if (request instanceof DatasetNamesRequest) {
                response = handleDatasetNamesRequest(dataStore,
                        (DatasetNamesRequest) request);
            } else if (request instanceof DeleteFilesRequest) {
                response = handleDeleteFilesRequest(dataStore,
                        (DeleteFilesRequest) request);
            } else if (request instanceof DeleteOrphansRequest) {
                response = handleDeleteOrphansRequest(dataStore,
                        (DeleteOrphansRequest) request);
            } else if (request instanceof DeleteRequest) {
                response = handleDeleteRequest(dataStore,
                        (DeleteRequest) request);
            } else if (request instanceof GroupsRequest) {
                response = handleGroupsRequest(dataStore,
                        (GroupsRequest) request);
            } else if (request instanceof RepackRequest) {
                response = handleRepackRequest(dataStore,
                        (RepackRequest) request);
            } else if (request instanceof RetrieveRequest) {
                response = handleRetrieveRequest(dataStore,
                        (RetrieveRequest) request);
            } else if (request instanceof StoreRequest) {
                response = handleStoreRequest(dataStore,
                        (StoreRequest) request);
            } else {
                throw new ServletException(
                        "Unhandled request of type: " + request.getClass());
            }
            long t2 = System.currentTimeMillis();
            if (response != null) {
                SerializationUtil.transformToThriftUsingStream(response,
                        resp.getOutputStream());
            }
            long t3 = System.currentTimeMillis();
            long deserializeTime = t1 - t0;
            long processTime = t2 - t1;
            long serializeTime = t3 - t2;
            long totalTime = t3 - t0;
            if (totalTime > 3000) {
                logger.warn("Spent " + totalTime + "ms processing "
                        + request.getClass().getSimpleName() + "("
                        + deserializeTime + ", " + processTime + ", "
                        + serializeTime + ") on " + request.getFilename());
            }
        } catch (SerializationException | StorageException e) {
            throw new ServletException(e);
        }
    }

    protected FileActionResponse handleCopyRequest(IDataStore dataStore,
            CopyRequest request) throws StorageException {
        dataStore.copy(request.getOutputDir(), request.getRepackCompression(),
                request.getMinMillisSinceLastChange(),
                request.getMaxMillisSinceLastChange());
        return new FileActionResponse();
    }

    protected StoreResponse handleCreateDatasetRequest(IDataStore dataStore,
            CreateDatasetRequest request)
            throws StorageException, FileNotFoundException {
        dataStore.createDataset(request.getRecord());

        return new StoreResponse();
    }

    protected RetrieveResponse handleDatasetDataRequest(IDataStore dataStore,
            DatasetDataRequest request)
            throws StorageException, FileNotFoundException {
        IDataRecord[] records = dataStore.retrieveDatasets(
                request.getDatasetGroupPath(), request.getRequest());
        RetrieveResponse response = new RetrieveResponse();
        response.setRecords(records);
        return response;
    }

    protected String[] handleDatasetNamesRequest(IDataStore dataStore,
            DatasetNamesRequest request)
            throws StorageException, FileNotFoundException {
        return dataStore.getDatasets(request.getGroup());
    }

    protected DeleteResponse handleDeleteFilesRequest(IDataStore dataStore,
            DeleteFilesRequest request)
            throws StorageException, FileNotFoundException {
        dataStore.deleteFiles(request.getDatesToDelete());
        DeleteResponse response = new DeleteResponse();
        response.setSuccess(true);
        return response;
    }

    protected FileActionResponse handleDeleteOrphansRequest(
            IDataStore dataStore, DeleteOrphansRequest request)
            throws StorageException {
        dataStore.deleteOrphanData(request.getOldestDateMap());
        return new FileActionResponse();
    }

    protected DeleteResponse handleDeleteRequest(IDataStore dataStore,
            DeleteRequest request)
            throws StorageException, FileNotFoundException {
        String[] datasets = request.getDatasets();
        String[] groups = request.getGroups();
        if (datasets != null && datasets.length > 0) {
            dataStore.deleteDatasets(datasets);
        }
        if (groups != null && groups.length > 0) {
            dataStore.deleteGroups(groups);
        }
        DeleteResponse response = new DeleteResponse();
        response.setSuccess(true);
        return response;
    }

    protected RetrieveResponse handleGroupsRequest(IDataStore dataStore,
            GroupsRequest request) throws IOException, StorageException {
        IDataRecord[] records = dataStore.retrieveGroups(request.getGroups(),
                request.getRequest());
        RetrieveResponse response = new RetrieveResponse();
        response.setRecords(records);
        return response;
    }

    protected FileActionResponse handleRepackRequest(IDataStore dataStore,
            RepackRequest request) throws StorageException {
        dataStore.repack(request.getCompression());
        return new FileActionResponse();
    }

    protected RetrieveResponse handleRetrieveRequest(IDataStore dataStore,
            RetrieveRequest request)
            throws StorageException, FileNotFoundException {
        String group = request.getGroup();
        String dataset = request.getDataset();
        Request req = request.getRequest();
        IDataRecord[] records;
        if (dataset == null && req == null) {
            records = dataStore.retrieve(group);
        } else {
            records = new IDataRecord[1];
            records[0] = dataStore.retrieve(group, dataset, req);
        }
        RetrieveResponse response = new RetrieveResponse();
        response.setRecords(records);
        return response;
    }

    protected StoreResponse handleStoreRequest(IDataStore dataStore,
            StoreRequest request) throws StorageException {
        long t0 = System.currentTimeMillis();
        for (IDataRecord record : request.getRecords()) {
            /*
             * Check for PyPies specific types of Record that are not supported
             * by the IDataStore API and convert them back to something
             * standard.
             */
            if (record instanceof CompressedDataRecord) {
                try {
                    record = uncompress((CompressedDataRecord) record);
                } catch (IOException e) {
                    throw new StorageException("Error handling compression",
                            record, e);
                }
            }

            dataStore.addDataRecord(record);
        }
        long t1 = System.currentTimeMillis();
        StorageStatus ss = dataStore.store(request.getOp());
        long t2 = System.currentTimeMillis();
        long uncompressTime = t1 - t0;
        long totalTime = t2 - t0;
        if (uncompressTime > 1000 | totalTime > 5000) {
            logger.warn("Spent " + totalTime + "ms(" + uncompressTime
                    + "ms uncompressing) storing " + request.getRecords().size()
                    + " records");
        }
        StoreResponse response = new StoreResponse();
        response.setStatus(ss);
        response.setExceptions(new String[0]);
        return response;
    }

    /**
     * The uncompression of CompressedDataRecord is handled by the server so
     * there isn't an existing java implementation.
     * 
     * @param record
     *            a compressed record.
     * @return an uncompressed record.
     */
    protected static IDataRecord uncompress(CompressedDataRecord record)
            throws IOException {
        IDataRecord result = null;
        try (InputStream inStream = new GZIPInputStream(
                new ByteArrayInputStream(record.getCompressedData()));
                PooledByteArrayOutputStream outStream = ByteArrayOutputStreamPool
                        .getInstance().getStream()) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = inStream.read(buf)) > 0) {
                outStream.write(buf, 0, n);
            }
            byte[] raw = outStream.toByteArray();
            switch (record.getType()) {
            case BYTE:
                result = new ByteDataRecord(null, null, raw);
                break;
            case SHORT:
                short[] sdata = new short[raw.length / 2];
                ShortBuffer.wrap(sdata)
                        .put(ByteBuffer.wrap(raw).asShortBuffer());
                result = new ShortDataRecord(null, null, sdata);
                break;
            case INT:
                int[] idata = new int[raw.length / 4];
                IntBuffer.wrap(idata).put(ByteBuffer.wrap(raw).asIntBuffer());
                result = new IntegerDataRecord(null, null, idata);
                break;
            case LONG:
                long[] ldata = new long[raw.length / 8];
                LongBuffer.wrap(ldata).put(ByteBuffer.wrap(raw).asLongBuffer());
                result = new LongDataRecord(null, null, ldata);
                break;
            case FLOAT:
                float[] fdata = new float[raw.length / 4];
                FloatBuffer.wrap(fdata)
                        .put(ByteBuffer.wrap(raw).asFloatBuffer());
                result = new FloatDataRecord(null, null, fdata);
                break;
            case DOUBLE:
                double[] ddata = new double[raw.length / 8];
                DoubleBuffer.wrap(ddata)
                        .put(ByteBuffer.wrap(raw).asDoubleBuffer());
                result = new DoubleDataRecord(null, null, ddata);
                break;
            }
        }
        result.setName(record.getName());
        result.setDimension(record.getDimension());
        result.setSizes(record.getSizes());
        result.setMaxSizes(record.getMaxSizes());
        result.setProperties(record.getProperties());
        result.setMinIndex(record.getMinIndex());
        result.setGroup(record.getGroup());
        result.setDataAttributes(record.getDataAttributes());
        result.setFillValue(record.getFillValue());
        result.setMaxChunkSize(record.getMaxChunkSize());
        result.setCorrelationObject(record.getCorrelationObject());
        return result;
    }

}
