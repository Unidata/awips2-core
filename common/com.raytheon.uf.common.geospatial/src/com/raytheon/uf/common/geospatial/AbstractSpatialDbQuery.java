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
package com.raytheon.uf.common.geospatial;

import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.geospatial.request.SpatialDbQueryRequest;
import com.raytheon.uf.common.serialization.comm.RequestRouter;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;

/**
 * Spatial query class, converts query request into an sql string
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------
 * Dec 06, 2010           mschenke  Initial creation
 * Aug 07, 2018  6642     randerso  Made executeRequest() a public method
 *
 * </pre>
 *
 * @author mschenke
 */

public abstract class AbstractSpatialDbQuery extends AbstractSpatialQuery {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractSpatialDbQuery.class);

    /** dbname for maps **/
    public static final String MAPS_DB = "maps";

    @Override
    public SpatialQueryResult[] query(String dataSet, String theGeomField,
            String[] attributes, Geometry areaGeometry,
            Map<String, RequestConstraint> filter, SearchMode mode)
            throws SpatialException {
        return query(dataSet, theGeomField, attributes, areaGeometry, filter,
                mode, null);
    }

    @Override
    public SpatialQueryResult[] query(String dataSet, String theGeomField,
            String[] attributes, Geometry areaGeometry,
            Map<String, RequestConstraint> filter, SearchMode mode,
            Integer limit) throws SpatialException {
        long t0 = System.currentTimeMillis();
        SpatialDbQueryRequest request = new SpatialDbQueryRequest();
        request.setSchema("mapdata");
        request.setTable(dataSet);
        request.setDatabase(MAPS_DB);
        request.setConstraints(filter);
        if (attributes != null) {
            request.addFields(attributes);
        }
        request.setReturnGeometry(true);
        if (theGeomField != null) {
            request.setGeometryField(theGeomField);
        }
        request.setGeometry(areaGeometry);
        request.setLimit(limit);
        request.setSearchMode(mode);
        SpatialQueryResult[] sqrs = executeRequest(request);

        statusHandler.handle(Priority.INFO, "SpatialQuery took: "
                + (System.currentTimeMillis() - t0) + "ms");
        return sqrs;
    }

    @Override
    public SpatialQueryResult[] executeRequest(SpatialDbQueryRequest request)
            throws SpatialException {
        DbQueryResponse response = null;
        try {
            response = (DbQueryResponse) RequestRouter.route(request);
        } catch (Exception e) {
            throw new SpatialException(
                    "Error querying spatial data: " + e.getLocalizedMessage(),
                    e);
        }
        SpatialQueryResult[] sqrs = null;
        if (response != null) {
            WKBReader wkbReader = new WKBReader();
            List<Map<String, Object>> results = response.getResults();
            sqrs = new SpatialQueryResult[results.size()];
            int i = 0;
            for (Map<String, Object> result : results) {
                SpatialQueryResult r = new SpatialQueryResult();
                try {
                    byte[] bytes = (byte[]) result
                            .remove(request.getGeometryField());
                    if (bytes != null) {
                        r.geometry = wkbReader.read(bytes);
                    }
                } catch (ParseException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            e.getLocalizedMessage(), e);
                }
                r.attributes = result;
                sqrs[i] = r;
                i++;
            }
        }
        return sqrs;
    }
}
