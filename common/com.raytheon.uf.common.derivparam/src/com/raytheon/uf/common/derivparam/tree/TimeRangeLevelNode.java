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
package com.raytheon.uf.common.derivparam.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.inventory.data.AggregateRequestableData;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.inventory.TimeAndSpace;
import com.raytheon.uf.common.inventory.TimeAndSpaceMatcher;
import com.raytheon.uf.common.inventory.TimeAndSpaceMatcher.MatchResult;
import com.raytheon.uf.common.inventory.tree.AbstractRequestableNode;
import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.derivparam.inv.AvailabilityContainer;
import com.raytheon.uf.common.derivparam.library.DerivParamDesc;
import com.raytheon.uf.common.derivparam.library.DerivParamMethod;
import com.raytheon.uf.common.time.DataTime;

/**
 * 
 * A Node which build AggregateRecords containing all the records for a given
 * time range in seconds.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 8, 2010            bsteffen     Initial creation
 * Feb 13, 2013 1621      bsteffen     update getDataDependency to correctly handle sources with time ranges.
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class TimeRangeLevelNode extends AbstractAliasLevelNode {

    private Integer startTime;

    private Integer endTime;

    private int dt;

    public TimeRangeLevelNode(TimeRangeLevelNode that) {
        super(that);
        this.startTime = that.startTime;
        this.endTime = that.endTime;
        this.dt = that.dt;
    }

    public TimeRangeLevelNode(AbstractRequestableNode sourceNode,
            DerivParamDesc desc, DerivParamMethod method, String modelName,
            Integer startTime, int endTime, int dt, Level level) {
        super(sourceNode, desc, method, modelName, level);
        this.startTime = startTime;
        this.endTime = endTime;
        this.dt = dt;
    }

    @Override
    public Set<AbstractRequestableData> getData(
            Set<TimeAndSpace> availability,
            Map<AbstractRequestableNode, Set<AbstractRequestableData>> dependencyData)
            throws DataCubeException {
        TimeAndSpaceMatcher matcher = new TimeAndSpaceMatcher();
        matcher.setIgnoreRange(true);
        Map<TimeAndSpace, AbstractRequestableData> dataMap = new HashMap<TimeAndSpace, AbstractRequestableData>();
        for (AbstractRequestableData data : dependencyData.get(sourceNode)) {
            dataMap.put(data.getTimeAndSpace(), data);
        }
        Set<AbstractRequestableData> records = new HashSet<AbstractRequestableData>();
        for (TimeAndSpace ast : availability) {
            Set<TimeAndSpace> needed = calculateNeededAvailability(ast);
            if (needed.isEmpty()) {
                continue;
            }
            Map<TimeAndSpace, MatchResult> matched = matcher.match(needed,
                    dataMap.keySet());
            if (TimeAndSpaceMatcher.getAll1(matched).containsAll(needed)) {
                List<AbstractRequestableData> dataList = new ArrayList<AbstractRequestableData>();
                for (TimeAndSpace dataTime : TimeAndSpaceMatcher
                        .getAll2(matched)) {
                    dataList.add(dataMap.get(dataTime));

                }
                AggregateRequestableData newRecord = new AggregateRequestableData(
                        dataList);
                newRecord.setDataTime(ast.getTime());
                newRecord.setSpace(ast.getSpace());
                modifyRequest(newRecord);
                records.add(newRecord);
            }
        }
        return records;
    }

    @Override
    public Set<TimeAndSpace> getAvailability(
            Map<AbstractRequestableNode, Set<TimeAndSpace>> availability)
            throws DataCubeException {
        TimeAndSpaceMatcher matcher = new TimeAndSpaceMatcher();
        matcher.setIgnoreRange(true);
        Set<TimeAndSpace> allAvail = availability.get(sourceNode);
        Set<TimeAndSpace> goodAvail = new HashSet<TimeAndSpace>();
        for (TimeAndSpace ast : allAvail) {
            Set<TimeAndSpace> needed = calculateNeededAvailability(ast);
            if (!needed.isEmpty()) {
                Set<TimeAndSpace> matchedNeeded = TimeAndSpaceMatcher
                        .getAll1(matcher.match(needed, allAvail));
                if (matchedNeeded.containsAll(needed)) {
                    goodAvail.add(ast);
                }
            }
        }
        return goodAvail;
    }

    /**
     * The only data dependency for this node will be the sourceNode and the
     * times needed will contain the TimeAndSpace objects for which the source
     * node must provide data to fill the desired time range for each
     * TimeAndSpace in availability
     * 
     * 
     * @param availability
     *            a set of TimeAndSpace for which data is needed
     * @param availabilityContainer
     *            an availability container used for querying the actual
     *            availability of the sourceNode
     * @return a map which only has one entry for the sourceNode, containing all
     *         the times needed to expand each TimeAndSpace in availability to
     *         include the correct range.
     */
    @Override
    public Map<AbstractRequestableNode, Set<TimeAndSpace>> getDataDependency(
            Set<TimeAndSpace> availability,
            AvailabilityContainer availabilityContainer)
            throws DataCubeException {
        // neededAvailability will contain all of the TimeAndSpace needed to
        // construct
        // the TimeAndSpace requested in availability.
        Set<TimeAndSpace> neededAvailability = new HashSet<TimeAndSpace>();
        for (TimeAndSpace ast : availability) {
            // For every TimeAndSpace in availability, calculate what is needed
            // to fill the range.
            Set<TimeAndSpace> needed = calculateNeededAvailability(ast);
            neededAvailability.addAll(needed);
        }
        // Get the actual TimeAndSpace where the source is available.
        Set<TimeAndSpace> sourceAvailability = availabilityContainer
                .getAvailability(sourceNode);

        TimeAndSpaceMatcher matcher = new TimeAndSpaceMatcher();
        matcher.setIgnoreRange(true);
        // use a matcher to match the needed TimeAndSpace to what the source
        // really has, this is necessary because precip parameters sometimes
        // have a range on the time and sometimes don't so there is no way to
        // correctly populate it in calculateNeededAvailability
        Map<TimeAndSpace, MatchResult> matchResults = matcher.match(
                neededAvailability, sourceAvailability);

        neededAvailability = TimeAndSpaceMatcher.getAll2(matchResults);

        // The only dependency is the source node that this time range is
        // gathering.
        Map<AbstractRequestableNode, Set<TimeAndSpace>> result = new HashMap<AbstractRequestableNode, Set<TimeAndSpace>>();
        result.put(sourceNode, neededAvailability);
        return result;
    }

    /**
     * For a given TimeAndSpace calculate all times needed to fill the time
     * range required by startTime, endTime, and dt.
     * 
     * @param ast
     * @return
     */
    private Set<TimeAndSpace> calculateNeededAvailability(TimeAndSpace ast) {
        Set<TimeAndSpace> result = new HashSet<TimeAndSpace>();
        int start = dt;
        if (startTime != null) {
            start = ast.getTime().getFcstTime() + this.startTime;
        }
        for (int i = start; i <= ast.getTime().getFcstTime() + this.endTime; i += dt) {
            DataTime time = new DataTime(ast.getTime().getRefTime(), i);
            result.add(new TimeAndSpace(time, ast.getSpace()));
        }
        return result;
    }

    @Override
    public TimeRangeLevelNode clone() {
        return new TimeRangeLevelNode(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result
                + ((startTime == null) ? 0 : startTime.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimeRangeLevelNode other = (TimeRangeLevelNode) obj;
        if (endTime == null) {
            if (other.endTime != null)
                return false;
        } else if (!endTime.equals(other.endTime))
            return false;
        if (startTime == null) {
            if (other.startTime != null)
                return false;
        } else if (!startTime.equals(other.startTime))
            return false;
        return true;
    }

}
