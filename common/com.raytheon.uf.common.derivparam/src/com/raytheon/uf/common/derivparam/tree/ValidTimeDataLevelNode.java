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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.derivparam.inv.AbstractInventory;
import com.raytheon.uf.common.inventory.TimeAndSpace;
import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.inventory.data.DoubleRequestableData;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.inventory.tree.AbstractRequestableNode;
import com.raytheon.uf.common.time.DataTime;

/**
 * {@link AbstractRequestableNode} that returns the valid time for a
 * calculation.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Jul 17, 2017  6345     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class ValidTimeDataLevelNode extends AbstractBaseDataNode {

    protected final AbstractInventory inventory;

    protected final String source;

    public ValidTimeDataLevelNode(AbstractInventory inventory, String source,
            Level level) {
        super(level);
        this.inventory = inventory;
        this.source = source;
    }

    @Override
    public DbQueryRequest getAvailabilityRequest(
            Map<String, RequestConstraint> originalConstraints) {
        return null;
    }

    @Override
    public Set<TimeAndSpace> getAvailability(
            Map<String, RequestConstraint> originalConstraints, Object response)
            throws DataCubeException {
        Set<TimeAndSpace> result = new HashSet<>();
        for (DataTime time : inventory.timeAgnosticQuery(originalConstraints)) {
            result.add(new TimeAndSpace(time));
        }
        return result;
    }

    @Override
    public DbQueryRequest getDataRequest(
            Map<String, RequestConstraint> orignalConstraints,
            Set<TimeAndSpace> availability) {
        return null;
    }

    @Override
    public Set<AbstractRequestableData> getData(
            Map<String, RequestConstraint> orignalConstraints,
            Set<TimeAndSpace> availability, Object response)
            throws DataCubeException {
        Set<AbstractRequestableData> results = new HashSet<>();
        for (TimeAndSpace ast : availability) {
            double validTime = ast.getTime().getValidTime().getTimeInMillis()
                    / 1000.0;
            AbstractRequestableData result = new DoubleRequestableData(
                    validTime);
            result.setLevel(getLevel());
            result.setParameter("validTime");
            result.setSource(source);
            result.setDataTime(ast.getTime());
            result.setSpace(ast.getSpace());
            results.add(result);
        }
        return results;
    }

    @Override
    public boolean isConstant() {
        return true;
    }

}
