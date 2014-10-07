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

package com.raytheon.uf.edex.database.plugin;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import com.raytheon.uf.common.dataplugin.persist.PersistableDataObject;

/**
 * A container class for plugin version records
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02/20/2007              garmendariz Initial check-in
 * 02/21/2007              bphillip    Added constructor
 * Oct 06, 2014 3702       bsteffen    Create PluginVersion table in each database containing plugins.
 * 
 * </pre>
 * 
 * @author garmendariz
 * @version 1.0
 */
@Entity
@Table(name = "plugin_info")
public class PluginVersion extends PersistableDataObject {

    private static final long serialVersionUID = 1L;

    /** The name of the plugin */
    @Id
    private String name;

    @Column
    private boolean initialized;

    /** The name of the table associated with this plugin */
    @Column
    private String tableName;

    /**
     * Default no-arg constructor. Required by Hibernate.
     * 
     */
    public PluginVersion() {
    }

    /**
     * Constructs a new PluginVersion with the given attributes
     * 
     * @param name
     *            The name of the plugin
     * @param initialized
     *            If the plugin has been initialized
     * @param retentionTime
     *            The retention time
     * @param tableName
     *            The primary table name for this plugin
     * @param version
     *            The version of the plugin
     */
    public PluginVersion(String name, boolean initialized,
 String tableName) {
        this.name = name;
        this.initialized = initialized;
        this.tableName = tableName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

}
