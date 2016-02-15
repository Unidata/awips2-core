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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.raytheon.uf.edex.core.EDEXUtil;

/**
 * Registry for Database Monitors. Should be run once in a cluster.
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

public class DatabaseMonitorRegistry {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ConcurrentMap<String, DatabaseMonitor> registry = new ConcurrentHashMap<>();

    private final static DatabaseMonitorRegistry instance = new DatabaseMonitorRegistry();

    private DatabaseMonitorRegistry() {

    }

    public static DatabaseMonitorRegistry getInstance() {
        return instance;
    }

    public DatabaseMonitorRegistry registerMonitor(String name,
            DatabaseMonitor monitor) {
        if (registry.putIfAbsent(name, monitor) != null) {
            throw new IllegalArgumentException(
                    "Database Monitor already registered under name: " + name);
        }

        return this;
    }

    public void runMonitors() {
        logger.info("Running Database Monitors");
        for (Map.Entry<String, DatabaseMonitor> entry : registry.entrySet()) {
            if (EDEXUtil.isRunning()) {
                try {
                    entry.getValue().runMonitor();
                } catch (Exception e) {
                    logger.error("Error occurred running database monitor: "
                            + entry.getKey(), e);
                }
            }
        }
        logger.info("Finsihed running Database Monitors");
    }
}
