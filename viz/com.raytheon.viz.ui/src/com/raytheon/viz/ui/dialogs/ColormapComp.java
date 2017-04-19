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
package com.raytheon.viz.ui.dialogs;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.colormap.ColorMapException;
import com.raytheon.uf.common.colormap.ColorMapLoader;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorMapCapability;
import com.raytheon.viz.ui.colormap.ColorMapTree;
import com.raytheon.viz.ui.colormap.ColorMapTreeFactory;
import com.raytheon.viz.ui.colormap.IRefreshColorMapTreeListener;

/**
 * Cascading control for colormaps
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 26, 2010           mschenke  Initial creation
 * Sep 18, 2013  2421     bsteffen  Use ColorMapTree for asyncronous loading.
 * Aug 28, 2014  3616     rferrel   Display ColorMapTree status while creating
 *                                  off the UI thread; and added refresh item.
 * Jan 13, 2016  5242     kbisanz   Replaced calls to deprecated
 *                                  LocalizationFile methods
 * Dec 01, 2016  5990     bsteffen  Ensure menu population does not pause UI
 * Apr 19, 2017  5852     tgurney   Label button "Untitled Colormap" if the
 *                                  selected colormap has no name
 *
 * </pre>
 *
 * @author mschenke
 */
public class ColormapComp {

    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ColormapComp.class);

    public static interface IColormapCompChangeListener {
        public void colormapChanged(String colorMap);
    }

    private Set<IColormapCompChangeListener> listeners = new HashSet<>();

    private Button cmapButton;

    private Shell shell;

    private Menu cmapPopupMenu;

    private ColorMapParameters params;

    private ColorMapCapability cap;

    private Menu parentMenu;

    public ColormapComp(Composite parent, ColorMapParameters params,
            ColorMapCapability cap) {
        this.params = params;
        this.cap = cap;
        this.shell = parent.getShell();
        cmapButton = new Button(parent, SWT.PUSH | SWT.DROP_DOWN);
        initializeComponents();
    }

    public ColormapComp(Menu parentMenu, ColorMapParameters params,
            ColorMapCapability cap) {
        this.parentMenu = parentMenu;
        this.params = params;
        this.cap = cap;
        this.shell = parentMenu.getShell();
        initializeComponents();
    }

    public void addChangeListener(IColormapCompChangeListener listener) {
        this.listeners.add(listener);
    }

    public Button getCMapButton() {
        return cmapButton;
    }

    public void setParams(ColorMapParameters params) {
        this.params = params;
    }

    public void setCap(ColorMapCapability cap) {
        this.cap = cap;
    }

    public void initItems() {
        if (cmapPopupMenu != null) {
            cmapPopupMenu.dispose();
        }

        if (cmapButton == null) {
            cmapPopupMenu = new Menu(parentMenu);
        } else {
            cmapPopupMenu = new Menu(cmapButton);
            if (params != null) {
                if (params.getColorMapName() != null) {
                    cmapButton.setText(params.getColorMapName());
                } else {
                    cmapButton.setText("Untitled Colormap");
                }
            }
        }

        cmapPopupMenu.setVisible(false);
        cmapPopupMenu.addMenuListener(new MenuPopulator(cmapPopupMenu,
                ColorMapTreeFactory.getInstance().getBaseTree()));
    }

    public void initializeComponents(Shell shell) {
        this.shell = shell;
        initializeComponents();
    }

    private void initializeComponents() {

        if (cmapButton != null) {
            GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, true, true);
            gd.widthHint = 250;
            cmapButton.setLayoutData(gd);

            /** Add listeners and when needed set up removal. */
            final IRefreshColorMapTreeListener riListener = new IRefreshColorMapTreeListener() {

                @Override
                public void refreshColorMapTree() {
                    if (!cmapButton.isDisposed()) {
                        initItems();
                    }
                }
            };

            shell.addDisposeListener(new DisposeListener() {

                @Override
                public void widgetDisposed(DisposeEvent e) {
                    ColorMapTreeFactory.getInstance()
                            .removeRefreshItemsListener(riListener);
                }
            });

            ColorMapTreeFactory.getInstance()
                    .addRefreshItemsListener(riListener);

            cmapButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    Point controlLoc = cmapButton.getDisplay().map(cmapButton,
                            null, e.x, e.y + cmapButton.getSize().y);
                    cmapPopupMenu.setLocation(controlLoc);
                    cmapPopupMenu.setVisible(true);
                }
            });
        }
        initItems();
    }

    public Menu getMenu() {
        return cmapPopupMenu;
    }

    public boolean isDisposed() {
        if (cmapButton != null && cmapButton.isDisposed()) {
            return true;
        }
        return false;
    }

    protected void changeColormap(String name) {
        if (cmapButton != null) {
            cmapButton.setText(name);
        }

        try {
            params.setColorMap(ColorMapLoader.loadColorMap(name));
            cap.notifyResources();
            for (IColormapCompChangeListener listener : listeners) {
                listener.colormapChanged(name);
            }
        } catch (ColorMapException e) {
            statusHandler.handle(Priority.ERROR, "Unable to change colormap.",
                    e);
        }
    }

    /**
     * Class to recursively populate a menu based off the contents of a
     * {@link ColorMapTree}. The menu is not populated until it is shown. This
     * class makes extensive use of {@link ColorMapTree#prepareAsync(Runnable)}
     * to ensure that there are no delays on the UI thread
     */
    private class MenuPopulator implements MenuListener {

        private final Menu menu;

        private ColorMapTree tree;

        public MenuPopulator(Menu menu, ColorMapTree tree) {
            this.menu = menu;
            this.tree = tree;
            /*
             * Make sure this tree is ready and as soon as it is prepare all the
             * subTrees, hopefully this runs fast enough that any empty trees
             * can be pruned before the user displays the menu.
             */
            tree.prepareAsync(Optional.of(this::prepareSubTrees));
        }

        private void prepareSubTrees() {
            for (ColorMapTree subTree : tree.getSubTrees()) {
                subTree.prepareAsync(Optional.empty());
            }
        }

        @Override
        public void menuHidden(MenuEvent e) {
            /* Do Nothing */
        }

        @Override
        public void menuShown(MenuEvent e) {
            if (menu.getItemCount() == 0) {
                /*
                 * Display a Loading menu item until the tree is ready, then
                 * fill in the menu. Hopeful the async prepare from the
                 * constructor is done by now and the user never sees the
                 * loading item.
                 */
                new MenuItem(menu, SWT.PUSH).setText("Loading...");
                tree.prepareAsync(Optional.of(this::populateMenu));
            } else {
                /*
                 * Since the initial population may occur before subtrees are
                 * ready there may be empty subtrees. These are a waste of space
                 * so they are pruned before showing the menu again.
                 */
                removeEmptyItems();
            }
        }

        private void populateMenu() {
            for (MenuItem item : menu.getItems()) {
                item.dispose();
            }

            if (tree.isEmpty()) {
                /*
                 * Do not remove empty menus since it can cause the menu to
                 * shift while the user is still over an item.
                 * removeEmptyItems() will be used to remove it when the user
                 * isn't looking at the menu.
                 */
                new MenuItem(menu, SWT.PUSH).setText("<Empty>");
                return;
            }

            List<ColorMapTree> subTrees = tree.getSubTrees();
            subTrees.sort((tree1, tree2) -> tree1.getName()
                    .compareToIgnoreCase(tree2.getName()));
            subTrees.forEach(this::addSubTree);
            List<ILocalizationFile> files = tree.getColorMapFiles();
            files.sort((file1, file2) -> file1.getPath()
                    .compareToIgnoreCase(file2.getPath()));
            files.forEach(this::addFile);

            if (menu == cmapPopupMenu) {
                new MenuItem(menu, SWT.SEPARATOR);

                ColorMapTreeFactory cmtf = ColorMapTreeFactory.getInstance();
                LocalizationLevel[] levels = PathManagerFactory.getPathManager()
                        .getAvailableLevels();
                /* Start at 1 to skip base. */
                for (int i = 1; i < levels.length; i += 1) {
                    LocalizationLevel level = levels[i];
                    ColorMapTree tree = cmtf.getTreeForLevel(level);
                    addSubTree(tree);
                }
            }
        }

        private void addSubTree(ColorMapTree tree) {
            if (tree.isReady() && tree.isEmpty()) {
                if (tree.isLevel()) {
                    new MenuItem(menu, SWT.PUSH)
                            .setText(tree.getName() + " ---");
                }
                return;
            }
            MenuItem item = new MenuItem(menu, SWT.CASCADE);
            item.setData(tree);
            item.setText(tree.getName());
            Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
            subMenu.setData(tree.getName());
            item.setMenu(subMenu);
            subMenu.addMenuListener(new MenuPopulator(subMenu, tree));
        }

        private void addFile(ILocalizationFile file) {
            MenuItem item = new MenuItem(menu, SWT.None);
            final String name = ColorMapLoader.shortenName(file);
            int start = name.lastIndexOf(IPathManager.SEPARATOR);
            if (start >= 0) {
                item.setText(name.substring(start + 1));
            } else {
                item.setText(name);
            }
            item.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    changeColormap(name);
                }

            });
        }

        private void removeEmptyItems() {
            if (!tree.isReady()) {
                return;
            }
            boolean allReady = true;
            for (MenuItem item : menu.getItems()) {
                if (item.getData() != null) {
                    ColorMapTree tree = (ColorMapTree) item.getData();
                    if (!tree.isReady()) {
                        allReady = false;
                    } else if (tree.isEmpty()) {
                        if (tree.isLevel()) {
                            item.setText(tree.getName() + " ---");
                            Menu menu = item.getMenu();
                            if (menu != null) {
                                menu.dispose();
                                item.setMenu(null);
                            }
                        } else {
                            item.dispose();
                        }
                    }
                }
            }
            /*
             * Once all the subtrees are ready this listener is no longer
             * needed.
             */
            if (allReady) {
                menu.removeMenuListener(this);
            }
        }

    }

}
