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
package com.raytheon.uf.edex.database.query;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import org.hibernate.Query;
import org.hibernate.SessionFactory;

/**
 * This class extends the functionality of {@link DatabaseQuery} by allowing for
 * spatial (geometry-based) constraints on database queries.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket# Engineer    Description
 * ------------ ------- ----------- --------------------------
 * Jun 15, 2018 7310    mapeters    Initial creation
 * </pre>
 *
 * @author mapeters
 */
public class SpatialDatabaseQuery extends DatabaseQuery {

    private static final String GEOM_PARAM_PREFIX = "geom";

    private static final int STARTING_GEOM_PARAM_NUMBER = 0;

    private final List<SpatialConstraint> spatialConstraints = new ArrayList<>();

    public SpatialDatabaseQuery(Class<?> entityName) {
        super(entityName);
    }

    public void addSpatialConstraint(SpatialConstraint constraint) {
        spatialConstraints.add(constraint);
    }

    @Override
    protected String buildAdditionalHQLConstraints() {
        if (spatialConstraints.isEmpty()) {
            return null;
        }

        int geomNumber = STARTING_GEOM_PARAM_NUMBER;
        StringJoiner constraintsStr = new StringJoiner(QueryUtil.AND_CLAUSE);
        for (SpatialConstraint constraint : spatialConstraints) {
            constraintsStr.add(
                    constraint.toHqlString(GEOM_PARAM_PREFIX + geomNumber));
            ++geomNumber;
        }

        return constraintsStr.toString();
    }

    @Override
    protected void populateAdditionalHQLParams(Query query,
            SessionFactory sessionFactory) {
        int geomNumber = STARTING_GEOM_PARAM_NUMBER;
        for (SpatialConstraint constraint : spatialConstraints) {
            query.setParameter(GEOM_PARAM_PREFIX + geomNumber,
                    constraint.getGeometry());
            ++geomNumber;
        }
    }
}
