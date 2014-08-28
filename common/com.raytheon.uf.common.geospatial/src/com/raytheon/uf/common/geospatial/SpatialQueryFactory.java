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

/**
 * SpatialQueryFactory
 * 
 * <pre>
 * 
 *    SOFTWARE HISTORY
 *   
 *    Date         Ticket#     Engineer    Description
 *    ------------ ----------  ----------- --------------------------
 *    Dec 07, 2007             chammack    Initial Creation.
 *    Aug 27, 2014  3356       njensen     Inject class implementation through spring
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1
 */
public class SpatialQueryFactory {

    private static Class<ISpatialQuery> clazz;

    private static String PROP = "eclipse.product";

    private static String PROP_VAL = "com.raytheon.viz.product.awips.CAVE";

    private SpatialQueryFactory() {

    }

    public static ISpatialQuery create() throws SpatialException {
        if (clazz == null) {
            throw new IllegalStateException(
                    "SpatialQueryFactory has a null ISpatialQuery implementation!");
        }

        ISpatialQuery query = null;
        try {
            query = clazz.newInstance();
        } catch (Exception e) {
            throw new SpatialException("Unable to create spatial query", e);
        }
        return query;
    }

    /**
     * TODO: remove this method, it should have never existed
     * 
     * @return the word CAVE or EDEX
     */
    @Deprecated
    public static String getType() {

        String prop = System.getProperty(PROP);
        String type = null;

        try {
            if (prop != null && prop.equals(PROP_VAL)) {
                type = "CAVE";
            } else {
                type = "EDEX";
            }
            return type;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Setter to inject the underlying query implementation through spring
     * 
     * @param impl
     * @return to make spring happy
     */
    public static Class<ISpatialQuery> setImplementation(
            Class<ISpatialQuery> impl) {
        clazz = impl;
        return impl;
    }
}
