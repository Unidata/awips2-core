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
package com.raytheon.uf.common.localization;

import java.util.Observable;
import java.util.Observer;

/**
 * Handles {@link IPathManager} object creation depending on the context.
 * 
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * May 7, 2008              chammack    Initial creation
 * Jul 14, 2008 1250        jelkins     EDEX LocalizationAdapter additions.
 * Aug 25, 2014 3356        njensen     Inject adapter through spring
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class PathManagerFactory {

    private static IPathManager pathManager;

    private static ILocalizationAdapter adapter;

    private static Observable internalObservable = new Observable() {
        @Override
        public synchronized void addObserver(Observer o) {
            super.addObserver(o);
            // mark changed if an observer is added
            setChanged();
        }
    };

    private PathManagerFactory() {

    }

    /**
     * Creates or retrieves the appropriate {@link IPathManager}.
     * 
     * @return the appropriate {@link IPathManager} object for the context.
     */
    public static synchronized IPathManager getPathManager() {
        if (pathManager == null) {
            if (adapter != null) {
                pathManager = new PathManager(adapter);
                // notify observers that the path manager adapter has been
                // initialized.
                internalObservable.notifyObservers();
            } else {
                throw new RuntimeException(
                        "No localization adapter has been set on PathManagerFactory!");
            }
        }

        return pathManager;
    }

    /**
     * Add observers for path manager adaptor initialization.
     * 
     * @param o
     *            observer to be notified when the path manager adaptor has been
     *            initialized.
     */
    public static synchronized void addObserver(Observer o) {
        internalObservable.addObserver(o);
    }

    /**
     * Get a path manager using the specified adapter
     * 
     * @param locAdapter
     * @return the path manager with the specified adapter
     */
    public static IPathManager getPathManager(ILocalizationAdapter locAdapter) {
        return new PathManager(locAdapter);
    }

    public static ILocalizationAdapter setAdapter(ILocalizationAdapter adapter) {
        PathManagerFactory.adapter = adapter;
        pathManager = null;
        return adapter;
    }
}
