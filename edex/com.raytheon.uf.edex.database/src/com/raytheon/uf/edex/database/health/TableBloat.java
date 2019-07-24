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
package com.raytheon.uf.edex.database.health;

import com.raytheon.uf.common.util.SizeUtil;

/**
 * Data structure representing amount of bloat in a database table.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 10, 2016 4630       rjpeter     Initial creation
 * 
 * </pre>
 * 
 * @author rjpeter
 * @version 1.0
 */

public class TableBloat {
    private String schema;

    private String tableName;

    private long realSizeBytes;

    private double bloatPercent;

    private long bloatBytes;

    /**
     * @return the schema
     */
    public String getSchema() {
        return schema;
    }

    /**
     * @param schema
     *            the schema to set
     */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /**
     * @return the tableName
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * @param tableName
     *            the tableName to set
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * @return the realSizeBytes
     */
    public long getRealSizeBytes() {
        return realSizeBytes;
    }

    /**
     * @param realSizeBytes
     *            the realSizeBytes to set
     */
    public void setRealSizeBytes(long realSizeBytes) {
        this.realSizeBytes = realSizeBytes;
    }

    /**
     * @return the bloatPercent
     */
    public double getBloatPercent() {
        return bloatPercent;
    }

    /**
     * @param bloatPercent
     *            the bloatPercent to set
     */
    public void setBloatPercent(double bloatPercent) {
        this.bloatPercent = bloatPercent;
    }

    /**
     * @return the bloatBytes
     */
    public long getBloatBytes() {
        return bloatBytes;
    }

    /**
     * @param bloatBytes
     *            the bloatBytes to set
     */
    public void setBloatBytes(long bloatBytes) {
        this.bloatBytes = bloatBytes;
    }

    @Override
    public String toString() {
        return String.format(
                "TableBloatInformation [schema=%s, tableName=%s, realSizeBytes=%s, bloatPercent=%.2f, bloatBytes=%s]",
                schema, tableName, realSizeBytes, bloatPercent,
                SizeUtil.prettyByteSize(bloatBytes));
    }

}
