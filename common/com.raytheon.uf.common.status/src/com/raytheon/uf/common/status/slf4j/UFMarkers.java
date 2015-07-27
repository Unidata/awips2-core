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
package com.raytheon.uf.common.status.slf4j;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import com.raytheon.uf.common.status.UFStatus.Priority;

/**
 * Frequently used constants for markers.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 13, 2015  4473      njensen     Initial creation
 *
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class UFMarkers {

    private static final String DELIMINATOR = "=";

    public static final String SOURCE_START = "SOURCE" + DELIMINATOR;

    public static final String CATEGORY_START = "CATEGORY" + DELIMINATOR;

    public static final String PLUGIN_START = "PLUGIN" + DELIMINATOR;

    public static final String PRIORITY_START = "PRIORITY" + DELIMINATOR;

    /**
     * A String for a marker indicating the log message is related to
     * configuration of the system
     */
    public static final String CONFIG = "CONFIG";

    /**
     * A String for a marker indicating the log message is related to
     * performance of the system
     */
    public static final String PERFORMANCE = "PERFORMANCE";

    /**
     * A String for a marker indicating the log message is related to network
     * connectivity.
     */
    public static final String NETWORK = "NETWORK";

    /**
     * A String for a marker indicates that the primary purpose of the log
     * message is to show a message to the user in some fashion, not to record
     * an event in the logs. Most applications will allow a user to view logs
     * somewhere, a normal log message should NOT use this marker. For these
     * reasons, logging events using this marker should have coherent and
     * messages that can be easily understood by a user.
     */
    public static final String SHOW = "SHOW";

    /**
     * A String for a marker that indicates that a user should not see this
     * message, i.e. it is for logging purposes only. Normal log messages should
     * NOT use this marker. It should only be used in situations where the code
     * has adequate fallback behavior but wishes to log AND doesn't need to
     * inform the user that it is using fallback behavior.
     */
    public static final String NOSHOW = "NOSHOW";

    public static Marker getSourceMarker(String source) {
        return MarkerFactory.getMarker(SOURCE_START.concat(source));
    }

    public static Marker getCategoryMarker(String category) {
        return MarkerFactory.getMarker(CATEGORY_START.concat(category));
    }

    public static Marker getPluginMarker(String plugin) {
        return MarkerFactory.getMarker(PLUGIN_START.concat(plugin));
    }

    public static Marker getUFPriorityMarker(Priority p) {
        return MarkerFactory.getMarker(PRIORITY_START.concat(p.toString()));
    }

    private UFMarkers() {
        // don't allow instantiation
    }

}
