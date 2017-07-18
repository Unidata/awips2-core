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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.derivparam.data.LatLonRequestableData;
import com.raytheon.uf.common.geospatial.IGridGeometryProvider;
import com.raytheon.uf.common.inventory.TimeAndSpace;
import com.raytheon.uf.common.inventory.data.AbstractRequestableData;
import com.raytheon.uf.common.inventory.exception.DataCubeException;
import com.raytheon.uf.common.inventory.tree.AbstractRequestableNode;

/**
 * {@link AbstractRequestableNode} that returns the Latitude or Longitude for
 * the data.
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
public abstract class LatLonDataLevelNode extends AbstractBaseDataNode {

    public static enum LatOrLon {
        LATITUDE, LONGITUDE;
    }

    protected final LatOrLon parameter;

    protected final String source;

    public LatLonDataLevelNode(String source, LatOrLon parameter, Level level) {
        super(level);
        this.source = source;
        this.parameter = parameter;
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
        for (IGridGeometryProvider space : getAvailableSpaces(
                originalConstraints)) {
            result.add(new TimeAndSpace(space));
        }
        return result;
    }

    public abstract Collection<? extends IGridGeometryProvider> getAvailableSpaces(
            Map<String, RequestConstraint> originalConstraints)
            throws DataCubeException;

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
            AbstractRequestableData result = createData();
            result.setLevel(getLevel());
            result.setParameter(parameter.toString().toLowerCase());
            result.setSource(source);
            result.setDataTime(ast.getTime());
            result.setSpace(ast.getSpace());
            results.add(result);
        }
        return results;
    }

    protected AbstractRequestableData createData() {
        return new LatLonRequestableData(parameter);
    }

    @Override
    public boolean isConstant() {
        return true;
    }

}
