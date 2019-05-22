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
package com.raytheon.uf.viz.ui.menus.widgets.tearoff;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuListener2;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 * Menu listener that adds item to menu which will open dialog which is the menu
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Sep 14, 2011            mschenke    Initial creation
 * Apr 10, 2013 DR 15185   D. Friedman Preserve tear-offs over perspective switches.
 * May 15, 2019 7850       tgurney     Calculate menu item text width with
 *                                     GC.textExtent() instead of String.length()
 *
 * </pre>
 *
 * @author mschenke
 */

public class TearOffMenuListener implements IMenuListener2 {

    private List<Object> openDialogs = new ArrayList<>();

    private static final String ID = "tearOffMenuContributionItem";

    private static final String ACTION_ID = "tearOffMenuAction";

    public static final String TEAROFF_PREFERENCE_ID = "tearoffmenus";

    private static final String TEAROFF_DISABLED_PERSPECTIVES = "com.raytheon.uf.viz.ui.menus.tearoffperspective";

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(TearOffMenuListener.class);

    private static boolean enabled;

    private static List<String> enabledPerspectives;
    static {
        final IPreferenceStore store = com.raytheon.uf.viz.core.Activator
                .getDefault().getPreferenceStore();
        enabled = store.getBoolean(TEAROFF_PREFERENCE_ID);
        enabledPerspectives = new ArrayList<>();

        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry
                .getExtensionPoint(TEAROFF_DISABLED_PERSPECTIVES);
        if (point != null) {
            IExtension[] extensions = point.getExtensions();
            for (IExtension extension : extensions) {
                for (IConfigurationElement element : extension
                        .getConfigurationElements()) {
                    if (Boolean.valueOf(element.getAttribute("enabled"))) {
                        enabledPerspectives
                                .add(element.getAttribute("perspectiveId"));
                    }
                }
            }
        }
        store.addPropertyChangeListener(
                event -> enabled = store.getBoolean(TEAROFF_PREFERENCE_ID));
    }

    public TearOffMenuListener() {

    }

    public TearOffMenuListener(IMenuManager mgr) {
        register(mgr.getItems(), this);
    }

    /**
     * When the menu is about to show, check if the list contains that menu
     * manager and if it doesn't add a TearOffContributionItem
     */
    @Override
    public void menuAboutToShow(final IMenuManager manager) {
        register(manager.getItems(), this);
        if (!openDialogs.contains(getPerspectiveKey(manager))) {
            // We need to add our item to be first so we need to remove others
            // then add ourself
            IContributionItem[] items = manager.getItems();
            manager.removeAll();
            manager.add(new TearOffContributionItem(manager));
            for (IContributionItem item : items) {
                manager.add(item);
            }
        }
    }

    /**
     * Remove the menu manager from the list, so that the menu item will show
     * back up
     */
    @Override
    public void menuAboutToHide(IMenuManager manager) {
        if (!openDialogs.contains(getPerspectiveKey(manager))) {
            manager.remove(ID);
            manager.remove(ACTION_ID);
            unregister(manager.getItems(), this);
        }
    }

    public static void register(IContributionItem[] items,
            IMenuListener listener) {
        for (IContributionItem item : items) {
            if (item instanceof IMenuManager) {
                ((IMenuManager) item).addMenuListener(listener);
            }
        }
    }

    public static void unregister(IContributionItem[] items,
            IMenuListener listener) {
        for (IContributionItem item : items) {
            if (item instanceof IMenuManager) {
                ((IMenuManager) item).removeMenuListener(listener);
            }
        }
    }

    private Object getPerspectiveKey(IMenuManager manager) {
        String perspectiveId = "";
        try {
            perspectiveId = VizWorkbenchManager.getInstance().getCurrentWindow()
                    .getActivePage().getPerspective().getId();
        } catch (Exception e) {
            statusHandler.handle(Priority.EVENTA,
                    "Failed to get current perspective ID", e);
        }
        return perspectiveId + "::" + getKey(manager);
    }

    private static Object getKey(IMenuManager manager) {
        Object key = manager;
        if (manager.getId() != null) {
            key = manager.getId();
        }
        return key;
    }

    private class TearOffContributionItem extends ContributionItem {

        private static final int MIN_DASHES = 10;

        private IMenuManager manager;

        public TearOffContributionItem(IMenuManager manager) {
            super(ID);
            this.manager = manager;
        }

        @Override
        public void fill(Menu menu, int index) {
            int maxWidth = 0;
            int numDashes = MIN_DASHES;
            GC gc = new GC(menu.getParent());
            try {
                for (MenuItem item : menu.getItems()) {
                    int itemWidth = gc.textExtent(item.getText()).x;
                    maxWidth = Math.max(maxWidth, itemWidth);
                }
                int dashWidth = gc.textExtent("-").x;
                numDashes = Math.max(MIN_DASHES, maxWidth / dashWidth);
            } finally {
                gc.dispose();
            }
            String dashes = "-".repeat(numDashes);
            new ActionContributionItem(new TearOffAction(dashes, manager, menu))
                    .fill(menu, index);
        }

        @Override
        public boolean isVisible() {
            String currPerspective = VizPerspectiveListener
                    .getCurrentPerspectiveManager().getPerspectiveId();
            boolean currPerspectiveEnabled = false;
            if (enabledPerspectives.contains(currPerspective)) {
                currPerspectiveEnabled = true;
            }
            return super.isVisible() && enabled && currPerspectiveEnabled;
        }

    }

    private class TearOffAction extends Action {

        private IMenuManager manager;

        private Menu menu;

        private TearOffAction(String text, final IMenuManager manager,
                Menu menu) {
            super(text);
            this.manager = manager;
            this.menu = menu;
        }

        @Override
        public void run() {
            final Object key = getPerspectiveKey(manager);
            TearOffMenuDialog dialog = new TearOffMenuDialog(menu);
            dialog.addListener(SWT.Dispose, event -> {
                openDialogs.remove(key);
                manager.remove(ID);
                manager.remove(ACTION_ID);
                unregister(manager.getItems(), TearOffMenuListener.this);
            });
            openDialogs.add(key);
            register(manager.getItems(), TearOffMenuListener.this);
            dialog.open();
        }

        @Override
        public String getId() {
            return ACTION_ID;
        }
    }
}
