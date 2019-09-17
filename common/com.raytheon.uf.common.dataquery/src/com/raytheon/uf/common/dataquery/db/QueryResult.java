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

package com.raytheon.uf.common.dataquery.db;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Encapsulates results from a database query. This class is essentially a
 * simplified version of the standard JDBC ResultSet
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Nov 07, 2008  1673     bphillip    Initial Creation
 * Dec 18, 2013  2579     bsteffen    Remove ISerializableObject
 * Jul 13, 2015  4500     rjpeter     Allow names to not be mapped
 *                                    if access by column count only.
 * Sep 17, 2019  7931     tgurney     Guarantee case-insensitivity of column
 *                                    names by using a TreeMap
 * </pre>
 *
 * @author bphillip
 */
@DynamicSerialize
public class QueryResult {

    /** A mapping of the column names to their index in the result */
    @DynamicSerializeElement
    private Map<String, Integer> columnNames;

    /** The result data */
    @DynamicSerializeElement
    private QueryResultRow[] rows;

    /**
     * Constructs an empty QueryResult
     */
    public QueryResult() {
        columnNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        rows = new QueryResultRow[0];
    }

    /**
     * Constructs a new Query result with the given column names and result rows
     *
     * @param columnNames
     *            The names of the columns returned
     * @param rows
     *            The result rows
     */
    public QueryResult(Map<String, Integer> columnNames,
            QueryResultRow[] rows) {
        this.columnNames = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.columnNames.putAll(columnNames);
        this.rows = rows;
    }

    /**
     * Adds a column name to the column name map
     *
     * @param name
     *            The column name. Name is case-insensitive
     * @param index
     *            The index of the result in the select clause
     */
    public void addColumnName(String name, Integer index) {
        columnNames.put(name, index);
    }

    /**
     * Gets the column value in the specified row
     *
     * @param rowNumber
     *            The row number to examine
     * @param columnName
     *            The column name to retrieve. Name is case-insensitive
     * @return The column value
     */
    public Object getRowColumnValue(int rowNumber, String columnName) {
        if (rowNumber > rows.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "Cannot get result for row: " + rowNumber
                            + " Result size is: " + rows.length);
        }
        if (!columnNames.containsKey(columnName)) {
            throw new IllegalArgumentException(
                    "Column: " + columnName + " is not present in Result Set");
        }
        return rows[rowNumber].getColumn(columnNames.get(columnName));
    }

    /**
     * Gets the column value in the specified row
     *
     * @param rowNumber
     *            The row number to examine
     * @param columnIndex
     *            The column index to retrieve
     * @return The column value
     */
    public Object getRowColumnValue(int rowNumber, int columnIndex) {
        if (rowNumber > rows.length) {
            throw new ArrayIndexOutOfBoundsException(
                    "Cannot get result for row: " + rowNumber
                            + " Result size is: " + rows.length);
        }
        if (!columnNames.isEmpty() && !columnNames.containsValue(columnIndex)) {
            throw new ArrayIndexOutOfBoundsException(
                    "Cannot get result for column index: " + columnIndex
                            + " Row size is: " + columnNames.size());

        }
        return rows[rowNumber].getColumn(columnIndex);
    }

    /**
     * Gets the number of rows retrieved
     *
     * @return The number of rows retrieved
     */
    public int getResultCount() {
        return rows.length;
    }

    /**
     * Gets the number of columns retrieved
     *
     * @return The number of columns retrieved
     */
    public int getColumnCount() {
        return columnNames.size();
    }

    /**
     * Gets the column name/index map
     *
     * @return The column name/index map
     */
    public Map<String, Integer> getColumnNames() {
        return columnNames;
    }

    /**
     * Gets the column names as an array
     *
     * @return The column names
     */
    public String[] getColumnNameArray() {
        String[] names = new String[columnNames.size()];
        for (Entry<String, Integer> e : columnNames.entrySet()) {
            names[e.getValue()] = e.getKey();
        }
        return names;
    }

    /**
     * Sets the column name map
     *
     * @param columnNames
     *            The column name map to set
     */
    public void setColumnNames(Map<String, Integer> columnNames) {
        this.columnNames = columnNames;
    }

    /**
     * Gets the rows retrieved in the query
     *
     * @return
     */
    public QueryResultRow[] getRows() {
        return rows;
    }

    /**
     * Sets the rows
     *
     * @param rows
     *            Thw rows to set
     */
    public void setRows(QueryResultRow[] rows) {
        this.rows = rows;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.getResultCount() == 0) {
            sb.append("No Results returned");
        } else {
            sb.append("Column Order: ")
                    .append(Arrays.toString(this.getColumnNameArray()))
                    .append("\n");
            for (int i = 0; i < this.getResultCount(); i++) {
                sb.append("Row ").append(i).append(": ");
                sb.append(rows[i].toString());
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
