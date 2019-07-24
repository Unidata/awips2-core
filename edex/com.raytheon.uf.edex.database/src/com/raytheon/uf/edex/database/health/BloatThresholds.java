package com.raytheon.uf.edex.database.health;
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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Java class definition template for all Bloat Monitor Threshold configuration
 * xml files.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 18, 2019   7840      mroos      Initial creation
 * 
 * </pre>
 * 
 * @author mroos
 */

@XmlRootElement(name = "thresholdList")
@XmlAccessorType(XmlAccessType.NONE)
public class BloatThresholds {

    @XmlElements({ @XmlElement(name = "threshold", type = Threshold.class) })
    private List<Threshold> thresholdList;

    @XmlAttribute(name = "id")
    private String id;

    @XmlAttribute(name = "priority")
    private float priority;

    @XmlAttribute(name = "database")
    private String database;

    @XmlAttribute(name = "schema")
    private String schema;

    @XmlAttribute(name = "table")
    private String table;

    /*
     * These 3 Patterns exist in order to create the 'matches()' method. This
     * allows for the bloat monitor to check for specific conditionals without
     * needing to list all possible permutations.
     */
    private Pattern databasePattern;

    private Pattern schemaPattern;

    private Pattern tablePattern;

    /**
     * Returns the list of Threshold objects
     * 
     * @return the thresholdList
     */
    public List<Threshold> getThresholdList() {
        return thresholdList;
    }

    /**
     * Sets the list of Threshold objects
     * 
     * @param thresholdList
     */
    public void setThresholdList(List<Threshold> thresholdList) {
        this.thresholdList = thresholdList;
    }

    /**
     * Returns the database name
     * 
     * @return the database name
     */
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the database name and the database RegEx pattern
     * 
     * @param database
     */
    public void setDatabase(String database) {
        this.database = database;
        this.databasePattern = Pattern.compile(database);
    }

    /**
     * Returns the schema name
     * 
     * @return the schema name
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Sets the schema name and the schema RegEx pattern
     * 
     * @param schema
     */
    public void setSchema(String schema) {
        this.schema = schema;
        this.schemaPattern = Pattern.compile(schema);
    }

    /**
     * Returns the table name
     * 
     * @return the table name
     */
    public String getTable() {
        return table;
    }

    /**
     * Sets the table name and the table name RegEx pattern
     * 
     * @param table
     */
    public void setTable(String table) {
        this.table = table;
        this.tablePattern = Pattern.compile(table);
    }

    /**
     * Returns the ID
     * 
     * @return the ID
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the ID
     * 
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns the priority
     * 
     * @return the priority
     */
    public float getPriority() {
        return priority;
    }

    /**
     * Sets the priority
     * 
     * @param priority
     */
    public void setPriority(float priority) {
        this.priority = priority;
    }

    /**
     * Returns the databasePattern, sets it if null.
     * 
     * @return the database pattern
     */
    private Pattern getDatabasePattern() {
        if (databasePattern == null) {
            this.databasePattern = Pattern.compile(getDatabase());
        }
        return databasePattern;
    }

    /**
     * Returns the schemaPattern, sets it if null.
     * 
     * @return the schema pattern
     */
    private Pattern getSchemaPattern() {
        if (schemaPattern == null) {
            this.schemaPattern = Pattern.compile(getSchema());
        }
        return schemaPattern;
    }

    /**
     * Returns the tablePattern, sets it if null.
     * 
     * @return the table pattern
     */
    private Pattern getTablePattern() {
        if (tablePattern == null) {
            this.tablePattern = Pattern.compile(getTable());
        }
        return tablePattern;
    }

    /**
     * Checks to see whether the passed in strings match the RegEx patterns for
     * checking against other file system calls
     * 
     * @param database
     *            the database name to compare to databasePattern.
     * @param schema
     *            the schema name to compare to schemaPattern.
     * @param table
     *            the table name to compare to tablePattern.
     * @return whether the passed strings ALL match the respective patterns
     */
    public boolean matches(String database, String schema, String table) {
        Matcher databaseMatcher = getDatabasePattern().matcher(database);
        Matcher schemaMatcher = getSchemaPattern().matcher(schema);
        Matcher tableMatcher = getTablePattern().matcher(table);
        return databaseMatcher.matches() && schemaMatcher.matches()
                && tableMatcher.matches();
    }

}