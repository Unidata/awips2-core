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
package com.raytheon.uf.edex.stats.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.raytheon.uf.common.serialization.JAXBManager;
import com.raytheon.uf.common.stats.AggregateRecord;
import com.raytheon.uf.common.stats.StatsGrouping;
import com.raytheon.uf.common.stats.StatsGroupingColumn;
import com.raytheon.uf.common.stats.data.GraphData;
import com.raytheon.uf.common.stats.data.StatsBin;
import com.raytheon.uf.common.stats.data.StatsData;
import com.raytheon.uf.common.stats.data.StatsLabelData;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.TimeRange;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.uf.common.util.CollectionUtil;

/**
 * Accumulates the statistics data.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 15, 2012 728        mpduff      Initial creation
 * Jan 15, 2013 1487       djohnson    Use xml for the grouping information on an {@link AggregateRecord}.
 * Jan 17, 2013 1357       mpduff      Remove unit conversions, add time step, other cleanup.
 * May 22, 2013 1917       rjpeter     Made unmarshalGroupingColumnFromRecord public.
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class StatsDataAccumulator {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(StatsDataAccumulator.class);

    /**
     * Constant.
     */
    private static final String COLON = ":";

    /** JaxB manager instance. */
    private static final JAXBManager JAXB_MANAGER;
    static {
        try {
            JAXB_MANAGER = new JAXBManager(StatsGroupingColumn.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** List of records */
    private AggregateRecord[] records;

    /** List of groups */
    @VisibleForTesting
    List<String> groups;

    /** TimeRange oject */
    private TimeRange timeRange;

    /** Event type */
    private String eventType;

    /** Event data type/attribute */
    private String dataType;

    /** Display unit */
    private String displayUnit;

    /** Data's timestep */
    private long timeStep;

    /** Stats data map */
    @VisibleForTesting
    final Map<String, StatsData> statsDataMap = new HashMap<String, StatsData>();

    /** Map of time in ms -> StatsBin objects */
    @VisibleForTesting
    final Map<Long, StatsBin> bins = new TreeMap<Long, StatsBin>();

    /** Map of group name -> group members */
    @VisibleForTesting
    Map<String, Set<String>> groupMemberMap = new HashMap<String, Set<String>>();

    /**
     * Set the AggregateRecord[]
     * 
     * @param records
     *            array of AggregateRecord objects
     */
    public void setRecords(AggregateRecord[] records) {
        this.records = records;
    }

    /**
     * Set up the grouping objects.
     */
    @VisibleForTesting
    public void setupGroupings() {
        for (AggregateRecord aggRec : records) {
            StatsGroupingColumn columnValue = unmarshalGroupingColumnFromRecord(aggRec);

            final List<StatsGrouping> groups = columnValue.getGroup();
            if (CollectionUtil.isNullOrEmpty(groups)) {
                continue;
            }
            for (StatsGrouping group : groups) {
                final String groupName = group.getName();
                if (!groupMemberMap.containsKey(groupName)) {
                    groupMemberMap.put(groupName, Sets.<String> newTreeSet());
                }
                groupMemberMap.get(groupName).add(group.getValue());
            }
        }

        groups = Lists.newArrayList(groupMemberMap.keySet());
    }

    /**
     * Get the GraphData object
     * 
     * @param groups
     *            List of groups
     * @return The GraphData object
     */
    public GraphData getGraphData(List<String> groups) {
        // Create the StatsLableData object
        // Create the keys (owner:plugin:provider)
        // Loop backwards over the data
        StatsLabelData prevLabelData = null;
        StatsLabelData statsLabelData = null;

        for (int i = groups.size() - 1; i >= 0; i--) {
            String group = groups.get(i);
            statsLabelData = new StatsLabelData(group, new ArrayList<String>(
                    groupMemberMap.get(group)));
            if (prevLabelData != null) {
                statsLabelData.setStatsLabelData(prevLabelData);
            }
            prevLabelData = statsLabelData;
        }

        gather(groups);

        // StatsLabelData is created and holds all the keys
        GraphData graphData = new GraphData();
        graphData.setStatsLabelData(statsLabelData);
        graphData.setDisplayUnit(displayUnit);
        graphData.setStatsDataMap(statsDataMap);
        graphData.setTimeRange(timeRange);
        graphData.setKeys(new ArrayList<String>(this.statsDataMap.keySet()));
        graphData.setKeySequence(groups);
        graphData.setTimeStep(this.timeStep);
        graphData.setEventType(eventType);
        graphData.setDataType(dataType);

        return graphData;
    }

    /**
     * Create the StatsDataMap keys
     * 
     * @param unitUtils
     *            UnitUtils object
     * @param groups
     *            List of groups
     */
    @VisibleForTesting
    void createStatsDataMap(List<String> groups) {
        Map<String, String> keySequenceMap = new LinkedHashMap<String, String>();
        for (String key : groups) {
            keySequenceMap.put(key, "");
        }

        String builtKey = "";
        for (AggregateRecord record : records) {
            if (record.getEventType().equals(eventType)
                    && record.getField().equals(dataType)) {

                StatsGroupingColumn columnValue = unmarshalGroupingColumnFromRecord(record);

                final List<StatsGrouping> columnValueGroups = columnValue
                        .getGroup();
                if (CollectionUtil.isNullOrEmpty(columnValueGroups)) {
                    continue;
                }

                // Have a matching record
                for (String key : keySequenceMap.keySet()) {
                    keySequenceMap.put(key, "");
                }

                for (StatsGrouping group : columnValueGroups) {

                    for (String key : keySequenceMap.keySet()) {
                        if (group.getName().equals(key)) {
                            keySequenceMap.put(key, keySequenceMap.get(key)
                                    .concat(group.getValue() + COLON));
                            break;
                        }
                    }
                }

                for (String key : keySequenceMap.keySet()) {
                    builtKey = builtKey.concat(keySequenceMap.get(key));
                }

                if (builtKey.length() != 0) {
                    // Drop the last ":"
                    builtKey = builtKey.substring(0, builtKey.length() - 1);
                }

                if (!statsDataMap.containsKey(builtKey)) {
                    statsDataMap.put(builtKey, new StatsData(builtKey,
                            timeStep, this.timeRange));
                }

                statsDataMap.get(builtKey).addRecord(record);
                builtKey = "";
            }
        }
    }

    /**
     * Unmarshals the {@link StatsGroupingColumn} from the
     * {@link AggregateRecord}.
     * 
     * @param record
     *            the aggregate record
     * @return the unmarshalled column, or an empty column if unable to
     *         unmarshal
     */
    public static StatsGroupingColumn unmarshalGroupingColumnFromRecord(
            AggregateRecord record) {
        String groupingXmlAsString = record.getGrouping();
        try {
            return (StatsGroupingColumn) JAXB_MANAGER
                    .unmarshalFromXml(groupingXmlAsString);
        } catch (JAXBException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Unable to unmarshal stats grouping column, returning empty record, xml:\n"
                            + groupingXmlAsString, e);
            return new StatsGroupingColumn();
        }
    }

    /**
     * Gather the data together in each bin.
     * 
     * @param unitUtils
     *            UnitUtils object
     * @param groups
     *            List of groups
     */
    private void gather(List<String> groups) {
        createStatsDataMap(groups);
        calculateBins();
        Map<Long, StatsBin> newMap = new TreeMap<Long, StatsBin>();
        for (String key : statsDataMap.keySet()) {
            // Copy the bins object
            for (long lkey : bins.keySet()) {
                StatsBin sb = new StatsBin(bins.get(lkey));
                newMap.put(lkey, sb);
            }

            StatsData data = statsDataMap.get(key);
            data.setBins(newMap);
            data.accumulate();
            statsDataMap.put(key, data);
        }
    }

    /**
     * Calculate the bins for the graph.
     */
    @VisibleForTesting
    void calculateBins() {
        bins.clear();

        long duration = timeRange.getDuration();
        long startMillis = timeRange.getStart().getTime();
        long numBins = duration / (timeStep * TimeUtil.MILLIS_PER_MINUTE);
        for (long i = 0; i < numBins; i++) {
            StatsBin sb = new StatsBin();
            sb.setBinMillis(startMillis
                    + (i * timeStep * TimeUtil.MILLIS_PER_MINUTE));
            bins.put(i, sb);
        }
    }

    /**
     * @param timeRange
     *            the timeRange to set
     */
    public void setTimeRange(TimeRange timeRange) {
        this.timeRange = timeRange;
    }

    /**
     * @return the timeRange
     */
    public TimeRange getTimeRange() {
        return timeRange;
    }

    /**
     * @param eventType
     *            the eventType to set
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     * @param dataType
     *            the dataType to set
     */
    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    /**
     * Set the display units.
     * 
     * @param displayUnit
     *            the displayUnit to set
     */
    public void setDisplayUnits(String displayUnit) {
        this.displayUnit = displayUnit;
    }

    /**
     * TimeStep in minutes
     * 
     * @param timeStep
     */
    public void setTimeStep(int timeStep) {
        this.timeStep = timeStep;
    }
}
