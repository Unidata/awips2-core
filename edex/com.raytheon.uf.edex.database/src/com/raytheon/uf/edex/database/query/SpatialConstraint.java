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

import org.locationtech.jts.geom.Geometry;

/**
 * This class represents a spatial (geometry-based) constraint for database
 * queries.
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
public class SpatialConstraint {

    public enum Type {
        INTERSECTS("intersects"), CONTAINS("contains"), WITHIN("within");

        private final String hqlFunction;

        private Type(String hqlFunction) {
            this.hqlFunction = hqlFunction;
        }
    }

    private final Geometry geom;

    private final Type type;

    private final String geomField;

    /**
     * @param geom
     *            the geometry to constrain by
     * @param type
     *            the type of constraint (e.g. intersection)
     * @param geomField
     *            the geometry field in the DB to constrain by
     */
    public SpatialConstraint(Geometry geom, Type type, String geomField) {
        this.geom = geom;
        this.type = type;
        this.geomField = geomField;
    }

    /**
     * Convert this SpatialConstraint to an HQL constraint string, using the
     * given geomParamName as the placeholder for the geometry value.
     *
     * @param geomParamName
     *            the placeholder for the geometry value
     * @return the HQL constraint string
     */
    public String toHqlString(String geomParamName) {
        return type.hqlFunction + "(:" + geomParamName + "," + geomField
                + ") = true";
    }

    public Geometry getGeometry() {
        return geom;
    }
}
