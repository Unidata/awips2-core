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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.CopyOnWriteArrayList;

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
 * Nov 12, 2015 4834        njensen     Added getActivePathManagers()
 * 
 * </pre>
 * 
 * @author chammack
 * @version 1.0
 */

public class PathManagerFactory {

    private static IPathManager currentPathMgr;

    private static ILocalizationAdapter adapter;

    private final static List<WeakReference<PathManager>> activePathMgrs = new CopyOnWriteArrayList<>();

    private static Observable internalObservable = new Observable() {
        @Override
        public synchronized void addObserver(Observer o) {
            super.addObserver(o);
            // mark changed if an observer is added
            setChanged();
        }
    };

    private PathManagerFactory() {
        // don't allow instantiation
    }

    /**
     * Creates or retrieves the appropriate {@link IPathManager}.
     * 
     * @return the appropriate {@link IPathManager} object for the context.
     */
    public static synchronized IPathManager getPathManager() {
        if (currentPathMgr == null) {
            if (adapter != null) {
                PathManager newPathMgr = new PathManager(adapter);
                activePathMgrs.add(new WeakReference<PathManager>(newPathMgr));
                currentPathMgr = newPathMgr;
                /*
                 * notify observers that the path manager adapter has been
                 * initialized.
                 */
                internalObservable.notifyObservers();
            } else {
                throw new RuntimeException(
                        "No localization adapter has been set on PathManagerFactory!");
            }
        }

        return currentPathMgr;
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
        PathManager pathMgr = new PathManager(locAdapter);
        activePathMgrs.add(new WeakReference<PathManager>(pathMgr));
        return pathMgr;
    }

    public static ILocalizationAdapter setAdapter(ILocalizationAdapter adapter) {
        PathManagerFactory.adapter = adapter;
        currentPathMgr = null;
        return adapter;
    }

    /**
     * Do not use this method. This should only be called by the singleton
     * ILocalizationNotificationObserver. Gets a copy of the active path
     * managers.
     * 
     * @return
     */
    public static List<PathManager> getActivePathManagers() {
        List<PathManager> result = new ArrayList<PathManager>(
                activePathMgrs.size());
        Iterator<WeakReference<PathManager>> itr = activePathMgrs.iterator();
        while (itr.hasNext()) {
            WeakReference<PathManager> wr = itr.next();
            PathManager pm = wr.get();
            if (pm != null) {
                result.add(pm);
            } else {
                itr.remove();
            }
        }
        return result;
    }
}
