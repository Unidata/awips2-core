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
package com.raytheon.uf.viz.core.rsc;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.commons.lang.Validate;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.annotations.DataURIUtil;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestableMetadataMarshaller;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.BinOffset;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.RecordFactory;
import com.raytheon.uf.viz.core.alerts.AbstractAlertMessageParser;
import com.raytheon.uf.viz.core.alerts.AlertMessage;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.exception.NoDataAvailableException;
import com.raytheon.uf.viz.core.exception.NoMatchingTimesException;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged.ChangeType;
import com.raytheon.uf.viz.datacube.DataCubeContainer;

/**
 * Provides a base implementation for data types that are requestable from the
 * EDEX backend and utilize PluginDataObjects.
 * 
 * This resource data provides the following capabilities:
 * <UL>
 * <LI>Storage of the request metadata (map)</LI>
 * <LI>Storage of request parameters for binning and updating</LI>
 * <LI>Interactions with the time matcher so that resources load with the
 * appropriate data from the start</LI>
 * <LI>Retrieval of PluginDataObjects from specific sets of DataTimes
 * <LI>Catalog retrievals for available DataTimes
 * </UL>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Feb 10, 2009           chammack    Initial creation
 * Feb 26, 2009  2032     jsanchez    Added loadWithNoData condition.
 * Apr 06, 2011           njensen     Moved binning times to edex
 * Apr 13, 2011           njensen     Caching available times
 * Mar 29, 2013  1638     mschenke    Switched to create PDO from dataURI
 *                                    mapping instead of dataURI string
 * May 14, 2013  1869     bsteffen    Get dataURI map directly from PDO.
 * Sep  9, 2013  2277     mschenke    Got rid of ScriptCreator references
 * Jun 12, 2014  3265     bsteffen    Harden getLatestPluginDataObjects
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public abstract class AbstractRequestableResourceData extends
        AbstractResourceData {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractRequestableResourceData.class);

    private static long CACHE_EXPIRATION = 60000;

    /**
     * if too many datatimes are used when requesting data Hibernate will throw
     * a stack overflow exception because of deep recursion. This value is used
     * to break requests into more manageble chunks for Hibernate.
     */
    private static int ENTRYTIMES_SLICE_SIZE = 500;

    private static class AlertMessageToPDOParser extends
            AbstractAlertMessageParser {

        @Override
        public Object parseAlertMessage(AlertMessage message,
                AbstractRequestableResourceData reqResourceData)
                throws VizException {
            Object objectToSend = null;
            Map<String, Object> attribs = new HashMap<String, Object>(
                    message.decodedAlert);

            if (reqResourceData.isUpdatingOnMetadataOnly()) {
                PluginDataObject record = RecordFactory.getInstance()
                        .loadRecordFromMap(attribs);
                objectToSend = record;
            } else {
                DbQueryRequest request = new DbQueryRequest(
                        RequestConstraint.toConstraintMapping(attribs));
                request.setLimit(1);
                DbQueryResponse response = (DbQueryResponse) ThriftClient
                        .sendRequest(request);
                PluginDataObject[] pdos = response
                        .getEntityObjects(PluginDataObject.class);
                if (pdos.length > 0) {
                    objectToSend = pdos[0];
                }
            }
            return objectToSend;
        }
    }

    private static AlertMessageToPDOParser defaultParser = new AlertMessageToPDOParser();

    /** the metadata criteria to retrieve the resource */
    @XmlJavaTypeAdapter(value = RequestableMetadataMarshaller.class)
    protected HashMap<String, RequestConstraint> metadataMap;

    protected ResourceType resourceType;

    /**
     * If requery is necessary on time matching, this should be set to true.
     * Note that this is an expensive operation.
     * 
     * Also note that time-based pruning will still be performed even if this is
     * set to false.
     */
    @XmlAttribute
    protected boolean isRequeryNecessaryOnTimeMatch = true;

    /**
     * If true, this indicates that only metadata will be passed in through
     * update methods, and no queries will be performed to retrieve the full
     * PluginDataObject.
     */
    @XmlAttribute
    protected boolean isUpdatingOnMetadataOnly;

    /**
     * Determines whether actual data retrieval should be performed
     */
    @XmlAttribute
    protected boolean retrieveData = true;

    /**
     * The bin offset of the data
     * 
     * The bin offset is the number of seconds in each direction from the data
     * time in which data is aggregated into bins.
     */
    @XmlElement
    protected BinOffset binOffset;

    @XmlElement
    protected AbstractAlertMessageParser alertParser = null;

    protected List<DataTime> cachedAvailableTimes = new ArrayList<DataTime>();

    private long cacheLastQueried = 0L;

    public AbstractAlertMessageParser getAlertParser() {
        return alertParser;
    }

    public void setAlertParser(AbstractAlertMessageParser alertParser) {
        this.alertParser = alertParser;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractResourceData#construct(com.raytheon
     * .uf.viz.core.comm.LoadProperties,
     * com.raytheon.uf.viz.core.drawables.IDescriptor)
     */
    @Override
    public AbstractVizResource<?, ?> construct(LoadProperties loadProperties,
            IDescriptor descriptor) throws VizException {
        // Implementation:
        // 1. Determine the DataTimes that are available on the server
        // 2. Utilizing the TimeMatcher, determine which DataTimes should be
        // part of the initial load
        // 3. Throw an exception of no data available and user has not specified
        // to suppress load errors
        // 4. Retrieve the PluginDataObjects for all DataTimes
        // 5. Construct resource with loaded pdos
        DataTime[] availableTimes = this.getAvailableTimes();
        DataTime[] dataTimes = descriptor.getTimeMatcher().initialLoad(
                loadProperties, availableTimes, descriptor);
        AbstractVizResource<? extends AbstractResourceData, ? extends IDescriptor> resource = null;

        if (dataTimes != null) {
            if (!loadProperties.isLoadWithoutData() && availableTimes != null
                    && availableTimes.length > 0) {
                boolean hasTime = false;
                for (DataTime time : dataTimes) {
                    if (time != null) {
                        hasTime = true;
                        break;
                    }
                }
                if (!hasTime) {
                    throw new NoMatchingTimesException();
                }
            }

            // Perform the initial load
            PluginDataObject[] data = getLatestPluginDataObjects(dataTimes,
                    new DataTime[0]);

            if (data.length == 0 && !loadProperties.isLoadWithoutData()
                    && this.retrieveData) {
                throw new NoDataAvailableException();
            }

            if (retrieveData && data.length > 0) {
                checkMetadataMap(getMetadataMap(), data[0]);
            }

            resource = constructResource(loadProperties, data);
        }

        return resource;
    }

    public static void checkMetadataMap(Map<String, RequestConstraint> map,
            PluginDataObject pdo) {
        try {
            Map<String, Object> dataURIMap = DataURIUtil.createDataURIMap(pdo);
            for (String key : map.keySet()) {
                if (dataURIMap.containsKey(key) == false) {
                    statusHandler
                            .handle(Priority.EVENTA,
                                    "metadata map key: "
                                            + key
                                            + " is not in datauri, updates may not properly work for resource");
                    System.out
                            .println("metadata map key: "
                                    + key
                                    + " is not in datauri, updates may not properly work for resource");
                }
            }
        } catch (PluginException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error parsing datauri into map", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.raytheon.uf.viz.core.rsc.AbstractResourceData#update(java.lang.Object
     * )
     */
    @Override
    public void update(Object updateData) {
        Validate.isTrue(updateData instanceof Object[],
                "Update expected Object[]");

        if (updateData instanceof AlertMessage[]) {
            update((AlertMessage[]) updateData);
        } else if (updateData instanceof PluginDataObject[]) {
            for (PluginDataObject pdo : (PluginDataObject[]) updateData) {
                DataTime time = pdo.getDataTime();
                if (binOffset != null) {
                    time = binOffset.getNormalizedTime(time);
                }
                synchronized (cachedAvailableTimes) {
                    if (!cachedAvailableTimes.contains(time)) {
                        cachedAvailableTimes.add(time);
                    }
                }
            }
            this.fireChangeListeners(ChangeType.DATA_UPDATE, updateData);
        } else {
            this.fireChangeListeners(ChangeType.DATA_UPDATE, updateData);
        }
    }

    protected void update(AlertMessage... messages) {
        List<Object> objectsToSend = new ArrayList<Object>(messages.length);
        boolean consistentCache = true;
        for (AlertMessage message : messages) {
            try {
                AbstractAlertMessageParser parser = getAlertParser();
                if (parser == null) {
                    parser = defaultParser;
                }
                Object timeObj = null;
                // do not try to maintain the time cache if the alert does not
                // parse.
                Object objectToSend = parser.parseAlertMessage(message, this);
                if (objectToSend != null) {
                    objectsToSend.add(objectToSend);
                    timeObj = message.decodedAlert.get("dataTime");
                }
                if (timeObj instanceof DataTime) {
                    DataTime time = (DataTime) timeObj;
                    if (binOffset != null) {
                        time = binOffset.getNormalizedTime(time);
                    }
                    synchronized (cachedAvailableTimes) {
                        if (!cachedAvailableTimes.contains(time)) {
                            cachedAvailableTimes.add(time);
                        }
                    }
                } else {
                    consistentCache = false;
                }
            } catch (VizException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error performing update: " + message.dataURI, e);
            }
        }
        if (!consistentCache) {
            invalidateAvailableTimesCache();
        }
        if (!objectsToSend.isEmpty()) {
            Class<?> componentType = objectsToSend.get(0).getClass();
            update(objectsToSend.toArray((Object[]) Array.newInstance(
                    componentType, objectsToSend.size())));
        }
    }

    /**
     * An abstract method that takes the PluginDataObject[] and a set of
     * LoadProperties and constructs the resource
     * 
     * @param loadProperties
     * @param objects
     * @return
     * @throws VizException
     */
    protected abstract AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects)
            throws VizException;

    /**
     * @return the metadataMap
     */
    public HashMap<String, RequestConstraint> getMetadataMap() {
        return metadataMap;
    }

    /**
     * @param metadataMap
     *            the metadataMap to set
     */
    public void setMetadataMap(HashMap<String, RequestConstraint> metadataMap) {
        this.metadataMap = metadataMap;
    }

    /**
     * @return the resourceType
     */
    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * @param resourceType
     *            the resourceType to set
     */
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * @return the isRequeryNecessaryOnTimeMatch
     */
    public boolean isRequeryNecessaryOnTimeMatch() {
        return isRequeryNecessaryOnTimeMatch;
    }

    /**
     * @param isRequeryNecessaryOnTimeMatch
     *            the isRequeryNecessaryOnTimeMatch to set
     */
    public void setRequeryNecessaryOnTimeMatch(
            boolean isRequeryNecessaryOnTimeMatch) {
        this.isRequeryNecessaryOnTimeMatch = isRequeryNecessaryOnTimeMatch;
    }

    /**
     * @return the isUpdatingOnMetadataOnly
     */
    public boolean isUpdatingOnMetadataOnly() {
        return isUpdatingOnMetadataOnly;
    }

    /**
     * @param isUpdatingOnMetadataOnly
     *            the isUpdatingOnMetadataOnly to set
     */
    public void setUpdatingOnMetadataOnly(boolean isUpdatingOnMetadataOnly) {
        this.isUpdatingOnMetadataOnly = isUpdatingOnMetadataOnly;
    }

    /**
     * @return the binOffset
     */
    public BinOffset getBinOffset() {
        return binOffset;
    }

    /**
     * @param binOffset
     *            the binOffset to set
     */
    public void setBinOffset(BinOffset binOffset) {
        this.binOffset = binOffset;
    }

    /**
     * Retrieve a set of PluginDataObjects for a desired set of times
     * 
     * The array returned is not guaranteed to be in the same order as the
     * DataTimes.
     * 
     * @param desired
     * @param current
     * @return
     * @throws VizException
     */
    public PluginDataObject[] getLatestPluginDataObjects(DataTime[] desired,
            DataTime[] current) throws VizException {
        if (desired == null || desired.length == 0 || !isRetrieveData()
                || !isRequeryNecessaryOnTimeMatch()) {
            return new PluginDataObject[0];
        }
        if (Arrays.asList(current).contains(null)) {
            statusHandler.handle(
                    Priority.VERBOSE,
                    "Resource contains unexpected null time: "
                            + this.getClass(),
                    new NullPointerException());
        }

        Set<DataTime> loadSet = new HashSet<DataTime>(Arrays.asList(desired));
        loadSet.removeAll(Arrays.asList(current));
        if (loadSet.contains(null)) {
            /*
             * The current time matcher implementations use null as a to
             * indicate frames where no matching was possible. null should be
             * expected but does not need to be requested.
             */
            loadSet.remove(null);
        }

        if (loadSet.isEmpty()) {
            return new PluginDataObject[0];
        }

        return requestPluginDataObjects(loadSet);
    }

    /**
     * Request plugin data objects for the passed in times. This method is
     * called from getLatestPluginDataObjects(DataTime[],DataTime[]) after time
     * filter from desired and current has been done. The times passed in is a
     * collection of new times needed
     * 
     * @param loadSet
     * @return
     * @throws VizException
     */
    protected PluginDataObject[] requestPluginDataObjects(
            Collection<DataTime> loadSet) throws VizException {
        Map<String, RequestConstraint> constraints = getMetadataMap();

        BinOffset binOffset = getBinOffset();

        List<DataTime> selectedEntryTimes = null;

        if (binOffset == null) {
            // Just ask for the data
            selectedEntryTimes = new ArrayList<DataTime>(loadSet);
        } else {
            // Find all the actual datatimes for the bins. TODO: Better way to
            // do this? Construct time range for each datatime in loadSet and
            // request data in that range?
            DataTime[] allDataTimes = getAvailableTimes(constraints, null);
            List<DataTime> trueDataTimes = new ArrayList<DataTime>();

            for (DataTime realDataTime : allDataTimes) {
                if (loadSet.contains(binOffset.getNormalizedTime(realDataTime))) {
                    trueDataTimes.add(realDataTime);
                }
            }

            if (trueDataTimes.size() == 0) {
                return new PluginDataObject[0];
            }

            selectedEntryTimes = trueDataTimes;
        }

        ArrayList<PluginDataObject> responses = new ArrayList<PluginDataObject>(
                selectedEntryTimes.size());

        for (int i = 0; i < selectedEntryTimes.size(); i += ENTRYTIMES_SLICE_SIZE) {
            int start = i;
            int end = i + ENTRYTIMES_SLICE_SIZE;
            if (end > selectedEntryTimes.size()) {
                end = selectedEntryTimes.size();
            }
            List<DataTime> slice = selectedEntryTimes.subList(start, end);

            PluginDataObject[] pdos;
            try {
                pdos = DataCubeContainer.getData(getMetadataMap(),
                        slice.toArray(new DataTime[0]));
            } catch (DataCubeException e) {
                throw new VizException(e);
            }
            responses.addAll(Arrays.asList(pdos));
        }

        Collections.sort(responses, layerComparator);
        return responses.toArray(new PluginDataObject[0]);
    }

    /**
     * Comparator for response array.
     */
    protected static Comparator<PluginDataObject> layerComparator = new Comparator<PluginDataObject>() {

        @Override
        public int compare(PluginDataObject arg0, PluginDataObject arg1) {
            return arg0.getDataTime().compareTo(arg1.getDataTime());
        }

    };

    /**
     * Given the times, filter them to only return times at or before the filter
     * 
     * @param times
     * @param filter
     * @return
     */
    public DataTime[] filterTimes(DataTime[] times, DataTime filter) {
        List<DataTime> validTimes = new ArrayList<DataTime>();
        for (DataTime time : times) {
            if (time.compareTo(filter) <= 0) {
                validTimes.add(time);
            }
        }
        return validTimes.toArray(new DataTime[validTimes.size()]);
    }

    /**
     * Retrieve a list of available times given a map of constraints.
     * Optionally, the data times may be binned by a provided binOffset.
     * 
     * @param constraintMap
     *            the request constraints
     * @param binOffset
     *            the binning constraints (optional, otherwise null)
     * @return a list of available data times
     * @throws VizException
     *             if error occurs
     */
    public static DataTime[] getAvailableTimes(
            Map<String, RequestConstraint> constraintMap, BinOffset binOffset)
            throws VizException {
        Validate.notNull(constraintMap);
        try {
            return DataCubeContainer.performTimeQuery(constraintMap, false,
                    binOffset);
        } catch (DataCubeException e) {
            throw new VizException(e);
        }
    }

    /**
     * Return a set of available times for the given resource data
     * 
     * @return
     * @throws VizException
     */
    public DataTime[] getAvailableTimes() throws VizException {
        DataTime[] available = queryForTimes(getMetadataMap());

        if (isFrozen()) {
            available = filterTimes(available, frozenTime);
        }

        return available;
    }

    /**
     * Queries for available times if the time cache has expired, or else
     * returns the time cache
     * 
     * @param map
     *            the metadata map to query against
     * @return the available times
     * @throws VizException
     */
    protected DataTime[] queryForTimes(Map<String, RequestConstraint> map)
            throws VizException {
        synchronized (cachedAvailableTimes) {
            if (((System.currentTimeMillis() - cacheLastQueried) > CACHE_EXPIRATION)) {
                DataTime[] retrieved = getAvailableTimes(map, getBinOffset());
                cacheLastQueried = System.currentTimeMillis();
                cachedAvailableTimes.clear();
                if (retrieved != null) {
                    cachedAvailableTimes.addAll(Arrays.asList(retrieved));
                }
            }
            return cachedAvailableTimes
                    .toArray(new DataTime[cachedAvailableTimes.size()]);
        }
    }

    protected void invalidateAvailableTimesCache() {
        synchronized (cachedAvailableTimes) {
            cacheLastQueried = 0l;
            cachedAvailableTimes.clear();
        }
    }

    /**
     * @return the retrieveData
     */
    public boolean isRetrieveData() {
        return retrieveData;
    }

    /**
     * @param retrieveData
     *            the retrieveData to set
     */
    public void setRetrieveData(boolean retrieveData) {
        this.retrieveData = retrieveData;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((binOffset == null) ? 0 : binOffset.hashCode());
        result = prime * result
                + ((metadataMap == null) ? 0 : metadataMap.hashCode());
        result = prime * result
                + ((resourceType == null) ? 0 : resourceType.hashCode());
        return result;
    }

    @Override
    public void configure(LoadProperties loadProperties, IDescriptor descriptor)
            throws VizException {
        super.configure(loadProperties, descriptor);
        descriptor.getTimeMatcher().initialLoad(loadProperties,
                getAvailableTimes(), descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        AbstractRequestableResourceData other = (AbstractRequestableResourceData) obj;

        if (!isObjectsEqual(binOffset, other.binOffset)) {
            return false;
        }

        if (!isObjectsEqual(metadataMap, other.metadataMap)) {
            return false;
        }

        if (!isObjectsEqual(resourceType, other.resourceType)) {
            return false;
        }

        return true;
    }

    /**
     * Compare two object to determine if they are equal.
     * <p>
     * The following logic is used to determine equality: (one == null ? two ==
     * null : one.equals(two))
     * 
     * @param one
     * @param two
     */
    protected boolean isObjectsEqual(Object one, Object two) {
        return (one == null ? two == null : one.equals(two));
    }

}
