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
package com.raytheon.uf.common.dataaccess.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataaccess.INotificationFilter;
import com.raytheon.uf.common.dataaccess.exception.DataRetrievalException;
import com.raytheon.uf.common.dataaccess.exception.IncompatibleRequestException;
import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.dataplugin.PluginException;
import com.raytheon.uf.common.dataplugin.annotations.DataURIUtil;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Java implementation of Python DefaultNotificationFilter. Mainly used for
 * serialization.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 29, 2016 2416       rjpeter     Initial creation
 * Jul 28, 2016 2416       tgurney     Implement constructor and accept()
 * </pre>
 * 
 * @author rjpeter
 */

@DynamicSerialize
public class DefaultNotificationFilter implements INotificationFilter {
    /*
     * Constraints to be applied to the tokenized URI. The reason this is a List
     * rather than a Map (the latter being unordered) is that this is also used
     * in Python on the client side, which does not have any knowledge of
     * dataURI field names.
     */
    @DynamicSerializeElement
    protected List<RequestConstraint> constraints;

    public DefaultNotificationFilter() {
        /*
         * no-op for serialization
         */
    }

    /**
     * Create a new notification filter.
     * 
     * @param datatype
     *            Type of data to accept. i.e., plugin name.
     * @param constraintMap
     *            All constraints that must apply to received dataURIs for them
     *            to be accepted. Plugin name / datatype and datatime are
     *            ignored.
     */
    public DefaultNotificationFilter(String datatype,
            Map<String, RequestConstraint> constraintMap) {
        List<String> fields = null;
        try {
            fields = DataURIUtil.getDataURIFieldNamesInOrder(DataURIUtil
                    .getPluginRecordClass(datatype));
        } catch (Exception e) {
            throw new IncompatibleRequestException(
                    "Unable to look up URI fields for dataType " + datatype, e);
        }
        List<RequestConstraint> uriConstraints = new ArrayList<>(fields.size());
        Iterator<String> fieldIter = fields.iterator();
        // first field is plugin
        uriConstraints.add(new RequestConstraint(datatype));

        // second field is dataTime. skip it and add a wildcard instead
        uriConstraints.add(RequestConstraint.WILDCARD);
        fieldIter.next();

        while (fieldIter.hasNext()) {
            String field = fieldIter.next();
            RequestConstraint rc = constraintMap.get(field);
            if (rc == null) {
                rc = RequestConstraint.WILDCARD;
            }
            uriConstraints.add(rc);
        }
        constraints = uriConstraints;

    }

    @Override
    public boolean accept(String dataURI) {
        Map<String, Object> dataURIMap = null;
        List<String> fieldNames = null;
        try {
            dataURIMap = DataURIUtil.createDataURIMap(dataURI);
            String pluginName = (String) dataURIMap
                    .get(PluginDataObject.PLUGIN_NAME_ID);
            fieldNames = DataURIUtil.getDataURIFieldNamesInOrder(DataURIUtil
                    .getPluginRecordClass(pluginName));
        } catch (PluginException e) {
            throw new DataRetrievalException("Failed to parse dataURI: "
                    + dataURI);
        }

        /*
         * dataURIs may differ in number of elements between different data
         * types, so we can check the lengths as a fast way to reject different
         * types of data than the one we want.
         */
        if (constraints.size() != fieldNames.size()) {
            return false;
        }
        // Compare each element of the dataURI against its constraint.
        for (int i = 0; i < fieldNames.size(); i++) {
            Object fieldValue = dataURIMap.get(fieldNames.get(i));
            if (!constraints.get(i).evaluate(fieldValue)) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return constraints that determine whether to accept a dataURI or not.
     */
    public List<RequestConstraint> getConstraints() {
        return constraints;
    }

    /**
     * @param constraints
     *            The constraints to set. Must be in the same order as the
     *            dataURI elements of the datatype you want.
     */
    public void setConstraints(List<RequestConstraint> constraints) {
        this.constraints = constraints;
    }
}
