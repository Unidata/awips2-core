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
package com.raytheon.uf.viz.core.localization;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Utilities for storing or retrieving a set of servers that have been
 * successfully connected to in the past.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 24, 2014 3236       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class ServerRemembrance {

    /**
     * Maximum number to remember
     */
    private static final int N_SERVERS_TO_REMEMBER = 7;

    private static final String SERVER_LIST_DELIMITER = ",";

    /**
     * Returns a list of servers that were successfully connected to in the past
     * and are stored in the preferences
     * 
     * @param prefs
     *            the preference store to lookup the servers
     * @param prefName
     *            the name of the preference to lookup
     * @return
     */
    public static String[] getServerOptions(IPreferenceStore prefs,
            String prefName) {
        String[] options = null;
        if (prefs.contains(prefName)) {
            options = prefs.getString(prefName).split(
                    ServerRemembrance.SERVER_LIST_DELIMITER);
        }

        if (options == null) {
            options = new String[0];
        }
        return options;
    }

    /**
     * Formats the list of servers that were connected to successfully in the
     * past and puts the latest one at the top of the list
     * 
     * @param selectedServer
     *            the most recent successful connection
     * @param prefs
     *            the preference store to store the server list
     * @param prefName
     *            the name of the preference to store
     * @return
     */
    public static String formatServerOptions(String selectedServer,
            IPreferenceStore prefs, String prefName) {
        String[] previousOptions = getServerOptions(prefs, prefName);
        if (previousOptions.length == 0) {
            return selectedServer;
        } else {
            List<String> servers = new LinkedList<String>(
                    Arrays.asList(previousOptions));

            // put the currently selected one at the front
            servers.remove(selectedServer);
            servers.add(0, selectedServer);

            // don't let the list get too long
            if (servers.size() > ServerRemembrance.N_SERVERS_TO_REMEMBER) {
                servers = servers.subList(0,
                        ServerRemembrance.N_SERVERS_TO_REMEMBER);
            }

            // format as a single string
            StringBuilder sb = new StringBuilder();
            Iterator<String> itr = servers.iterator();
            while (itr.hasNext()) {
                sb.append(itr.next());
                if (itr.hasNext()) {
                    sb.append(ServerRemembrance.SERVER_LIST_DELIMITER);
                }
            }
            return sb.toString();
        }

    }

}
