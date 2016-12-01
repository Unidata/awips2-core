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
package com.raytheon.viz.ui.colormap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.core.VizApp;

/**
 * Factory which can provide cached versions of {@link ColorMapTree} objects.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Sep 18, 2013  2421     bsteffen  Initial creation
 * Aug 28, 2014  3516     rferrel   Getting treesByLevel no longer on the UI
 *                                  thread. Converted to singleton. Added
 *                                  localized file observer.
 * Dec 01, 2016  5990     bsteffen  Ensure menu population does not pause UI
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class ColorMapTreeFactory {

    /**
     * The only allowed instance of this class.
     */
    private final static ColorMapTreeFactory instance = new ColorMapTreeFactory();

    /**
     * 
     * @return instance
     */
    public static ColorMapTreeFactory getInstance() {
        return instance;
    }

    /**
     * BASE localization tree must be handled differently from the other
     * localization levels. Its items are placed directly on the top menu while
     * the other levels are in their own pop up menu.
     */
    private ColorMapTree baseTree;

    /**
     * Trees for non-BASE localization levels.
     */
    private final Map<LocalizationLevel, ColorMapTree> treesByLevel = Collections
            .synchronizedMap(new HashMap<>());

    /**
     * Thread safe list of listeners to notify when localized colormaps
     * directories have a file change. Since the list is modified more often
     * then it is traversed CopyOnWriterArrayList is not recommended.
     * 
     */
    private final List<IRefreshColorMapTreeListener> refreshListeners = Collections
            .synchronizedList(new ArrayList<>());

    /**
     * Singleton constructor.
     */
    private ColorMapTreeFactory() {
        IPathManager pm = PathManagerFactory.getPathManager();
        LocalizationContext baseContext = pm.getContext(
                LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);
        baseTree = new ColorMapTree(pm, baseContext, ColorMapLoader.DIR_NAME);
        baseTree.prepareAsync(Optional.empty());

        LocalizationLevel[] levels = pm.getAvailableLevels();
        /* Start at 1 to skip base. */
        for (int i = 1; i < levels.length; i += 1) {
            LocalizationLevel level = levels[i];
            ColorMapTree tree = new ColorMapTree(pm, level,
                    ColorMapLoader.DIR_NAME);
            treesByLevel.put(level, tree);
            tree.prepareAsync(Optional.empty());
        }
        pm.addLocalizationPathObserver(ColorMapLoader.DIR_NAME,
                (file) -> refresh(file.getContext().getLocalizationLevel()));
    }

    /**
     * Get a tree for the BASE localization context. This tree is treated
     * differently from the other localization context trees. The tree items are
     * placed directly on the top menu while the other level trees are put in
     * their own pop up menu.
     */
    public ColorMapTree getBaseTree() {
        return baseTree;
    }

    /**
     * Return a {@link ColorMapTree}Tree for the provided level. The tree will
     * have the same name as the level and will have a subtree for each context
     * that exists at that level.
     */
    public ColorMapTree getTreeForLevel(LocalizationLevel level) {
        synchronized (treesByLevel) {
            ColorMapTree tree = treesByLevel.get(level);
            if (tree == null) {
                IPathManager pm = PathManagerFactory.getPathManager();
                tree = new ColorMapTree(pm, level, ColorMapLoader.DIR_NAME);
                treesByLevel.put(level, tree);
            }
            return tree;
        }
    }

    /**
     * Method used to inform listeners of any changes to any ColorMapTree.
     */
    protected void refresh(LocalizationLevel level) {
        IPathManager pm = PathManagerFactory.getPathManager();
        if (level == LocalizationLevel.BASE) {
            LocalizationContext baseContext = pm.getContext(
                    LocalizationType.COMMON_STATIC, LocalizationLevel.BASE);
            baseTree = new ColorMapTree(pm, baseContext,
                    ColorMapLoader.DIR_NAME);
            baseTree.prepareAsync(Optional.empty());
        } else {
            ColorMapTree tree = new ColorMapTree(pm, level,
                    ColorMapLoader.DIR_NAME);
            tree.prepareAsync(Optional.empty());
            treesByLevel.put(level, tree);
        }
        VizApp.runAsync(() -> {
            IRefreshColorMapTreeListener[] rListeners = refreshListeners
                    .toArray(new IRefreshColorMapTreeListener[0]);
            for (IRefreshColorMapTreeListener listener : rListeners) {
                listener.refreshColorMapTree();
            }
        });
    }

    /**
     * Thread safe removal of listener.
     * 
     * @param listener
     */
    public void removeRefreshItemsListener(
            IRefreshColorMapTreeListener listener) {
        refreshListeners.remove(listener);
    }

    /**
     * Thread safe adding listener.
     * 
     * @param listener
     */
    public void addRefreshItemsListener(IRefreshColorMapTreeListener listener) {
        refreshListeners.add(listener);
    }
}
