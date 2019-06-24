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
package com.raytheon.uf.edex.database.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import com.raytheon.uf.common.dataquery.requests.DbQueryRequest.OrderBy;
import com.raytheon.uf.common.dataquery.requests.DbQueryRequest.RequestField;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint;
import com.raytheon.uf.common.dataquery.requests.RequestConstraint.ConstraintType;
import com.raytheon.uf.common.dataquery.responses.DbQueryResponse;
import com.raytheon.uf.common.geospatial.ISpatialQuery.SearchMode;
import com.raytheon.uf.common.geospatial.request.SpatialDbQueryRequest;
import com.raytheon.uf.common.serialization.comm.IRequestHandler;
import com.raytheon.uf.edex.database.dao.CoreDao;
import com.raytheon.uf.edex.database.dao.DaoConfig;
import org.locationtech.jts.geom.Geometry;

/**
 * Handler for spatial db queries
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 29, 2011           mschenke  Initial creation
 * Aug 07, 2018  6642     randerso  Changed to use
 *                                  RequestConstraint.toSqlString() so added
 *                                  ConstraintTypes are handled. Tried to
 *                                  cleaned up spatial constraint code without
 *                                  breaking compatibility.
 *
 * </pre>
 *
 * @author mschenke
 */

public class SpatialDbQueryHandler
        implements IRequestHandler<SpatialDbQueryRequest> {

    private static final Pattern IN_PATTERN = Pattern.compile(",\\s?");

    @Override
    public Object handleRequest(SpatialDbQueryRequest request)
            throws Exception {
        String database = request.getDatabase();
        String schema = request.getSchema();
        String table = request.getTable();
        SearchMode mode = request.getSearchMode();
        Geometry geom = request.getGeometry();
        Map<String, RequestConstraint> constraints = request.getConstraints();
        List<RequestField> fields = request.getFields();
        Integer limit = request.getLimit();
        OrderBy orderBy = request.getOrderBy();
        String geometryField = request.getGeometryField();
        boolean returnGeom = request.isReturnGeometry();

        // Check arguments
        if (database == null) {
            throw new IllegalArgumentException("Database must be specified");
        }
        if (table == null) {
            throw new IllegalArgumentException("Table must be specified");
        }
        if (fields.isEmpty() && !returnGeom) {
            throw new IllegalArgumentException(
                    "Must provide request fields or return geometry");
        }
        if (geometryField == null && (geom != null || returnGeom)) {
            throw new IllegalArgumentException(
                    "Must specify geometry field if geometry object set or returning geometry");
        }
        if (geom != null && mode == null) {
            throw new IllegalArgumentException(
                    "Must specify SearchMode for geometry query");
        }
        if (orderBy != null
                && (orderBy.mode == null || orderBy.field == null)) {
            throw new IllegalArgumentException(
                    "Order By field not properly set, mode and field cannot be null");
        }
        if (orderBy != null) {
            boolean found = false;
            for (RequestField field : fields) {
                if (orderBy.field.equalsIgnoreCase(field.field)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                throw new IllegalArgumentException(
                        "Order By field must be added as a request field");
            }
        }

        StringBuilder query = new StringBuilder(1000);
        query.append("SELECT ");

        boolean first = true;
        String[] fieldNames = new String[fields.size() + (returnGeom ? 1 : 0)];
        int i = 0;
        if (returnGeom) {
            fieldNames[i] = geometryField;
            i++;
            query.append("ST_AsBinary(").append(geometryField).append(") as ")
                    .append(geometryField);
            first = false;
        }

        for (RequestField field : fields) {
            fieldNames[i] = field.field;
            i++;
            if (!first) {
                query.append(", ");
            }
            if (field.max) {
                query.append("MAX(");
            }
            query.append(field.field);
            if (field.max) {
                query.append(") as ").append(field.field);
            }
            first = false;
        }

        query.append(" FROM ");
        if (schema != null) {
            query.append(schema).append('.');
        }
        query.append(table);

        if (constraints.size() > 0 || geom != null) {
            query.append(" WHERE ");
        }

        first = true;
        for (Entry<String, RequestConstraint> entry : constraints.entrySet()) {
            String key = entry.getKey();
            RequestConstraint constraint = entry.getValue();
            if (!first) {
                query.append(" AND ");
            }
            first = false;

            // perform fix ups of constraint values
            /*
             * FIXME: this should never have been here since it's just fixing up
             * improperly formatted arguments but it's too risky to remove it
             */
            ConstraintType constraintType = constraint.getConstraintType();
            String value = constraint.getConstraintValue();

            switch (constraintType) {
            case LIKE:
                /*
                 * FIXME: the wildcards should be in the constraint value not
                 * added here.
                 */
                if (!value.contains("%") && !(value.contains("_"))) {
                    value = "%" + value + "%";
                    constraint = new RequestConstraint(value, constraintType);
                }
                break;

            case NOT_IN:
            case IN:
                // Some people pass in the value with quotes
                // like this "'thing1','thing2'"
                //
                // Some people pass in the value without quotes
                // like this "thing1,thing2"
                //
                // This will attempt to remove quotes as they are automatically
                // added by RequestConstraint.toSqlString().
                //
                // There will not necessarily catch every case but it
                // grabs the obvious one
                value = value.trim();
                if (value.startsWith("'") || value.endsWith("'")) {
                    String[] items = IN_PATTERN.split(value);
                    StringJoiner newValue = new StringJoiner(", ");
                    for (String item : items) {
                        if (item.startsWith("'")) {
                            item = item.substring(1);
                        }
                        if (item.endsWith("'")) {
                            item = item.substring(0, item.length() - 1);
                        }
                        newValue.add(item);
                    }
                    constraint = new RequestConstraint(newValue.toString(),
                            constraintType);
                }
                break;

            default:
                // no value fix up required
                break;

            }
            query.append(key).append(constraint.toSqlString());
        }

        if (geom != null) {
            if (!first) {
                query.append(" AND ");
                first = false;
            }

            String geomText = getGeomText(geom);
            String prefix;
            String suffix = ")";
            switch (mode) {
            case INTERSECTS:
                prefix = " ST_Intersects(";
                break;
            case CONTAINS:
                prefix = " ST_Contains(";
                break;
            case WITHIN:
                prefix = " ST_Within(";
                break;
            case CLOSEST:
                /*
                 * TODO: this did not return the closest feature, rather all
                 * features within a hard-coded 4.5 units sorted by distance.
                 *
                 * I could find no instance of CLOSEST being used in the
                 * baseline so I've removed the ORDER BY so it's actually a
                 * valid spatial constraint.
                 *
                 * We should consider removing or renaming this function or
                 * making it actually return the CLOSEST feature.
                 */
                prefix = " ST_DWithin(";
                suffix = ", 4.5)";
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported search mode: " + mode);
            }
            query.append(prefix);
            query.append(geomText);
            query.append(", ");
            query.append(geometryField);
            query.append(suffix);
        }

        if (limit != null) {
            query.append(" LIMIT ").append(limit);
        }

        if (orderBy != null) {
            query.append("ORDER BY ").append(orderBy.field).append(" ")
                    .append(orderBy.mode);
        }

        CoreDao dao = new CoreDao(DaoConfig.forDatabase(database));
        Object[] results = dao.executeSQLQuery(query.toString());
        List<Map<String, Object>> resultMaps = new ArrayList<>(results.length);
        for (Object obj : results) {
            if (!(obj instanceof Object[])) {
                obj = new Object[] { obj };
            }
            Object[] objs = (Object[]) obj;
            if (objs.length != fieldNames.length) {
                throw new Exception(
                        "Column count returned does not match expected column count");
            }
            Map<String, Object> resultMap = new HashMap<>(objs.length * 2);
            for (i = 0; i < fieldNames.length; ++i) {
                resultMap.put(fieldNames[i], objs[i]);
            }
            resultMaps.add(resultMap);
        }

        DbQueryResponse response = new DbQueryResponse();
        response.setResults(resultMaps);
        return response;
    }

    private String getGeomText(Geometry geometry) {
        StringBuilder sb = new StringBuilder();
        sb.append("ST_GeomFromText('");
        sb.append(geometry.toText());
        sb.append("', 4326)");
        return sb.toString();
    }
}
