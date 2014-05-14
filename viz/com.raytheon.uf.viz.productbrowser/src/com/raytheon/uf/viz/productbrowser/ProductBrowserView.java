package com.raytheon.uf.viz.productbrowser;

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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.productbrowser.bookmarks.ProductBrowserBookmark;
import com.raytheon.uf.viz.productbrowser.bookmarks.ProductBrowserBookmarksContentProvider;
import com.raytheon.uf.viz.productbrowser.bookmarks.ProductBrowserBookmarksLabelProvider;
import com.raytheon.uf.viz.productbrowser.bookmarks.ProductBrowserFolder;
import com.raytheon.uf.viz.productbrowser.jobs.ProductBrowserInitializeJob;
import com.raytheon.uf.viz.productbrowser.jobs.ProductBrowserQueryJob;
import com.raytheon.uf.viz.productbrowser.plugins.ProductBrowserTextSelector;
import com.raytheon.uf.viz.productbrowser.xml.ProductBrowserBookmarksXML;

/**
 * Product browser view implementation
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * May 03, 2010           mnash       Initial creation
 * May 02, 2013  1949     bsteffen    Switch Product Browser from uengine to
 *                                    DbQueryRequest.
 * May 13, 2014  3135     bsteffen    Make all queries async.
 * 
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */
public class ProductBrowserView extends ViewPart {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProductBrowserView.class);

    public static final String ID = "com.raytheon.uf.viz.productbrowser.ProductBrowserView";

    public static final String LABEL_DATA_KEY = "label";

    public static final String DEF_DATA_KEY = "defintion";

    private Tree productTree;

    private TreeViewer bookmarkTree;

    private Text searchBar;

    private Action bookmarkProductsAction;

    private Action loadBookmarkAction;

    private Action removeBookmarkAction;

    private Action loadProductAction;

    private Action productInfoAction;

    private Action refreshAction;

    private static IConfigurationElement[] config;

    private static IExtension[] extensions;

    public ProductBrowserView() {
        super();
    }

    @Override
    public void createPartControl(Composite parent) {
        createFullVersion(parent);
    }

    /**
     * Creates the large version of the product browser view
     * 
     * @param parent
     */
    private void createFullVersion(Composite parent) {
        Composite fullComp = new Composite(parent, SWT.FILL);
        // folder = new CTabFolder(fullComp, SWT.BORDER);
        fullComp.setLayout(new GridLayout(1, true));
        getDataTypes();
        createActions();
        createToolbar();
        // createBookmarks(fullComp);
        // createBookmarkTreeContextMenu();
        createProductTree(fullComp);
        createProductBrowserContextMenu();
        // createSearchBar(fullComp);
    }

    /**
     * Provides the actions available to the product tree
     */
    private void createActions() {
        refreshAction = new Action("Refresh Browser") {
            @Override
            public void run() {
                // TODO, replace that (and fix it too)
                // searchBar.setText("");
                // // need to repopulate here eventually, but for speed sake as
                // // this just essentially minimizes at this time, just
                // minimize
                populateInitialProductTree();
                // productTree.setExpandedElements(new Object[0]);
            }
        };
        refreshAction.setId("refreshAction");
        refreshAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("refresh.gif"));

        productInfoAction = new Action("Product Info") {
            @Override
            public void run() {
                displayInfo();
            }
        };
        productInfoAction.setId("productInfoAction");
        productInfoAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("help.gif"));

        loadProductAction = new Action("Load Product") {
            @Override
            public void run() {
                loadProduct(null);
            }
        };
        loadProductAction.setId("loadProductAction");
        loadProductAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("run.gif"));

        loadBookmarkAction = new Action("Load Bookmark") {
            @Override
            public void run() {
                loadBookmark();
            }
        };
        loadBookmarkAction.setId("loadBookmarkAction");
        loadBookmarkAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("run.gif"));

        removeBookmarkAction = new Action("Remove Bookmark") {
            @Override
            @SuppressWarnings("unchecked")
            public void run() {
                ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>> bookmark = (ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>>) ((IStructuredSelection) bookmarkTree
                        .getSelection()).getFirstElement();
                ProductBrowserFolder folder = (ProductBrowserFolder) bookmarkTree
                        .getInput();
                folder.removeBookmark(bookmark);
                bookmarkTree.setInput(folder);
                ProductBrowserBookmarksXML
                        .writeXML((ProductBrowserFolder) bookmarkTree
                                .getInput());
                bookmarkTree.refresh();
            }
        };
        removeBookmarkAction.setId("removeBookmarkAction");
        removeBookmarkAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("minus.gif"));

        // FIXME change how products are named
        bookmarkProductsAction = new Action("Add Bookmark...") {
            @Override
            public void run() {
                TreeItem selection = productTree.getSelection()[0];
                AbstractRequestableProductBrowserDataDefinition<?> prod = (AbstractRequestableProductBrowserDataDefinition<?>) getDataDef(selection);
                // TODO fix this
                String[] temp = getProductURI(selection, true);
                String name = "";
                for (int i = 0; i < temp.length; i++) {
                    name += temp[i].trim() + File.separator;
                }
                name = name.substring(0, name.length() - 1);
                ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>> bookmark = new ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>>(
                        name, prod.getProductParameters(
                                getProductURI(selection, false), prod.order),
                        prod);
                ProductBrowserFolder folder = (ProductBrowserFolder) bookmarkTree
                        .getInput();
                folder.addBookmark(bookmark);
                bookmarkTree.setInput(folder);
                ProductBrowserBookmarksXML
                        .writeXML((ProductBrowserFolder) bookmarkTree
                                .getInput());
                bookmarkTree.refresh();
            }
        };
        bookmarkProductsAction.setId("bookmarkProductsAction");
        bookmarkProductsAction.setImageDescriptor(ProductBrowserUtils
                .getImageDescriptor("plus.gif"));
    }

    /**
     * TODO include bookmarks
     */
    private void createBookmarks(Composite parent) {
        // the expand bar
        ExpandBar bar = new ExpandBar(parent, SWT.FILL);
        bar.setLayout(new GridLayout(1, true));
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, false);
        bar.setLayoutData(gridData);
        ExpandItem item = new ExpandItem(bar, SWT.FILL);
        item.setText("Bookmarks");
        item.setImage(ProductBrowserUtils.getImageDescriptor("bkmrk.gif")
                .createImage());
        createBookmarkTree(bar, item);
    }

    /**
     * Creates the bookmark tree under the expand bar
     * 
     * @param bar
     * @param item
     */
    private void createBookmarkTree(final ExpandBar bar, final ExpandItem item) {
        final Composite group = new Composite(bar, SWT.NONE);
        GridLayout layout = new GridLayout(1, true);
        layout.marginBottom = 0;
        layout.verticalSpacing = 0;
        group.setLayout(layout);
        bookmarkTree = new TreeViewer(group, SWT.BORDER | SWT.V_SCROLL
                | SWT.H_SCROLL);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        bookmarkTree.getTree().setLayoutData(gridData);
        bookmarkTree
                .setContentProvider(new ProductBrowserBookmarksContentProvider());
        bookmarkTree
                .setLabelProvider(new ProductBrowserBookmarksLabelProvider());

        bar.addListener(SWT.Expand, new Listener() {
            @Override
            public void handleEvent(Event event) {
                int count = bookmarkTree.getTree().getItemCount();
                // had to figure on a maximum size and a minimum size
                count = Math.min(count, 8);
                count = Math.max(count, 3);
                int height = bookmarkTree.getTree().getBounds().height / count;
                GridData gridData = (GridData) item.getParent().getLayoutData();
                gridData.heightHint = height + item.getHeaderHeight();
                item.setHeight(gridData.heightHint - item.getHeaderHeight());
                group.setEnabled(true);
                bar.getParent().pack();
            }
        });

        bar.addListener(SWT.Collapse, new Listener() {
            @Override
            public void handleEvent(Event event) {
                group.setEnabled(false);
                bar.getParent().pack();
            }
        });

        bookmarkTree.getTree().addListener(SWT.MouseDoubleClick,
                new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        if (event.button == 1
                                && ((IStructuredSelection) bookmarkTree
                                        .getSelection()).getFirstElement() != null) {
                            loadBookmarkAction.run();
                        }
                    }
                });
        populateBookmarkTree();
        item.setControl(group);
    }

    public void createSearchBar(Composite parent) {

        Composite searchComp = new Composite(parent, SWT.NONE);
        searchComp.setLayout(new GridLayout(2, false));
        searchComp.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
                false));

        GridData gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);

        searchBar = new Text(searchComp, SWT.SINGLE | SWT.BORDER);
        searchBar.setEnabled(false);
        searchBar.setLayoutData(gridData);
        searchBar.setText("");
        buildHistoryList();
        gridData = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
    }

    /**
     * Creates the product browser that spans the entire view...
     * 
     * @param parent
     */
    public void createProductTree(final Composite parent) {
        // CTabItem productItem = new CTabItem(folder, SWT.CLOSE);
        // productItem.setText("Products");
        // productItem.setControl(parent);

        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        productTree = new Tree(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL
                | SWT.BORDER);
        productTree.setLayoutData(gridData);
        productTree.addListener(SWT.MouseDoubleClick, new Listener() {
            @Override
            public void handleEvent(Event event) {
                if (event.button == 1) {
                    loadProductAction.run();
                }
            }
        });

        productTree.addListener(SWT.Expand, new Listener() {
            @Override
            public void handleEvent(Event event) {
                ProductBrowserQueryJob.startJob((TreeItem) event.item);
            }
        });

        productTree.addListener(SWT.MouseMove, new Listener() {
            @Override
            public void handleEvent(Event event) {
                productTree.setToolTipText("");
            }
        });
        populateInitialProductTree();
    }

    /**
     * Builds the toolbar at the top of the view
     */
    private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(refreshAction);
    }

    /**
     * Creating the right click menu, for the product tree
     */
    private void createProductBrowserContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager mgr) {
                fillProductBrowserContextMenu(mgr);
            }
        });

        Menu menu = menuMgr.createContextMenu(productTree);
        productTree.setMenu(menu);
    }

    /**
     * Creating the right click menu, for the bookmark tree
     */
    private void createBookmarkTreeContextMenu() {
        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager mgr) {
                if (((IStructuredSelection) bookmarkTree.getSelection())
                        .getFirstElement() != null) {
                    fillBookmarkMenu(mgr);
                }
            }
        });
        Menu menu = menuMgr.createContextMenu(bookmarkTree.getControl());
        bookmarkTree.getControl().setMenu(menu);
        getSite().registerContextMenu(menuMgr, bookmarkTree);
    }

    /**
     * Takes the menu during the right click and fills it with actions pertinent
     * to the product tree and pertinent to the item that is selected
     * 
     * @param mgr
     */
    protected void fillProductBrowserContextMenu(IMenuManager mgr) {
        TreeItem[] selection = productTree.getSelection();
        if (selection != null && selection.length > 0) {
            ProductBrowserLabel label = getLabel(selection[0]);
            if (label == null) {
                return;
            }
            // if not a product, do not give opportunity to load things
            if (label.isProduct()) {
                AbstractProductBrowserDataDefinition<?> def = getDataDef(selection[0]);
                Map<ResourceType, List<DisplayType>> displayTypes = def
                        .getDisplayTypes();
                if (displayTypes != null && displayTypes.isEmpty() == false) {
                    MenuManager menuMgr = new MenuManager("Load As...",
                            ProductBrowserUtils.getImageDescriptor("run.gif"),
                            "");
                    for (ResourceType type : displayTypes.keySet()) {
                        if (displayTypes.keySet().size() <= 1) {
                            for (DisplayType types : displayTypes.get(type)) {
                                menuMgr.add(getDisplayTypeAction(types));
                            }
                        }
                    }
                    mgr.add(menuMgr);
                } else {
                    mgr.add(loadProductAction);
                }
            }
            mgr.add(productInfoAction);
        }
    }

    private Action getDisplayTypeAction(final DisplayType type) {
        char[] name = type.name().toLowerCase().toCharArray();
        name[0] = Character.toUpperCase(name[0]);
        Action action = new Action((String.valueOf(name))) {
            /*
             * (non-Javadoc)
             * 
             * @see org.eclipse.jface.action.Action#run()
             */
            @Override
            public void run() {
                loadProduct(type);
            }
        };
        return action;
    }

    protected void fillBookmarkMenu(IMenuManager mgr) {
        mgr.add(loadBookmarkAction);
        mgr.add(removeBookmarkAction);
    }

    private void populateBookmarkTree() {
        ProductBrowserFolder folders = ProductBrowserBookmarksXML.populate();
        if (folders != null) {
            ProductBrowserFolder main = new ProductBrowserFolder();
            main.addFolder(folders);
            bookmarkTree.setInput(folders);
        }
    }

    /**
     * Take the initial plugins available and populate them if they have any
     * data. This function does not populate the tree with the data, just the
     * plugin names.
     * 
     * @param popData
     */
    public void populateInitialProductTree() {
        productTree.removeAll();
        long time = System.currentTimeMillis();
        for (IExtension ext : extensions) {
            config = ext.getConfigurationElements();
            for (IConfigurationElement element : config) {
                AbstractProductBrowserDataDefinition<?> prod = null;
                try {
                    prod = (AbstractProductBrowserDataDefinition<?>) element
                            .createExecutableExtension("class");
                } catch (CoreException e) {
                    statusHandler
                            .error("A product browser data definition has failed to load.",
                                    e);
                    continue;
                }
                String displayText = "Checking Availability of "
                        + prod.displayName + "...";
                /* Sort alphabetically. */
                int index = 0;
                for (TreeItem items : productTree.getItems()) {
                    if (items.getText().compareToIgnoreCase(displayText) > 0) {
                        break;
                    } else {
                        index++;
                    }
                }
                TreeItem ti = new TreeItem(productTree, SWT.NONE, index);
                ti.setText(displayText);
                ProductBrowserLabel label = new ProductBrowserLabel(
                        prod.displayName, prod.displayName);
                if (prod instanceof AbstractRequestableProductBrowserDataDefinition<?>) {
                    AbstractRequestableProductBrowserDataDefinition<?> arpbdd = (AbstractRequestableProductBrowserDataDefinition<?>) prod;
                    label.setData(arpbdd.productName);
                    label.setProduct(arpbdd.order.length == 0);
                }
                ti.setData(LABEL_DATA_KEY, label);
                ti.setData(DEF_DATA_KEY, prod);
                Font font = ti.getFont();
                FontData fontData = font.getFontData()[0];
                fontData = new FontData(fontData.getName(),
                        fontData.getHeight(), SWT.BOLD);
                font = new Font(ti.getDisplay(), fontData);
                ti.setFont(font);
                new ProductBrowserInitializeJob(ti).schedule();
            }
        }
        System.out.println("Time to populate product browser : "
                + (System.currentTimeMillis() - time) + "ms");
    }

    private void buildHistoryList() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final List<String> history = new ArrayList<String>();
                // List<String> history = new ArrayList<String>();
                long time = System.currentTimeMillis();
                for (IExtension ext : extensions) {
                    config = ext.getConfigurationElements();
                    for (IConfigurationElement element : config) {
                        try {
                            AbstractProductBrowserDataDefinition<?> prod = (AbstractProductBrowserDataDefinition<?>) element
                                    .createExecutableExtension("class");
                            List<String> historyStrings = prod
                                    .buildProductList(history);
                            if (historyStrings != null) {
                                history.addAll(prod.buildProductList(history));
                            }
                        } catch (CoreException e) {
                            e.printStackTrace();
                        }
                    }
                }
                System.out.println(history.size());
                System.out.println("Time taken to build product catalog : "
                        + (System.currentTimeMillis() - time) + "ms");
                final Set<String> set = new HashSet<String>(history);
                history.clear();
                history.addAll(set);
                VizApp.runAsync(new Runnable() {
                    @Override
                    public void run() {
                        if (!searchBar.isDisposed()) {
                            new ProductBrowserTextSelector(searchBar, set
                                    .toArray(new String[history.size()]));
                            searchBar.setEnabled(true);
                            searchBar.setText("Search");
                        }
                    }
                });
            }
        });
        thread.start();
    }

    /**
     * Using reflection and the eclipse registry, agnostically finds the
     * available types to populate the tree
     */
    private void getDataTypes() {
        IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint point = registry
                .getExtensionPoint(ProductBrowserUtils.DATA_DEFINITION_ID);
        if (point != null) {
            extensions = point.getExtensions();
        } else {
            extensions = new IExtension[0];
        }
    }

    private void loadProduct(DisplayType type) {
        TreeItem[] selection = productTree.getSelection();
        if (selection != null) {
            for (TreeItem item : selection) {
                ProductBrowserLabel label = getLabel(selection[0]);
                if (label == null) {
                    return;
                }
                // if not a product, do not give opportunity to load things
                if (label.isProduct()) {
                    AbstractProductBrowserDataDefinition<?> def = getDataDef(item);
                    String[] path = getProductURI(item, false);
                    if (type != null) {
                        def.loadProperties
                                .getCapabilities()
                                .getCapability(def.resourceData,
                                        DisplayTypeCapability.class)
                                .setDisplayType(type);
                    }
                    def.constructResource(path, null);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void loadBookmark() {
        ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>> bookmark = (ProductBrowserBookmark<AbstractRequestableProductBrowserDataDefinition<?>>) ((IStructuredSelection) bookmarkTree
                .getSelection()).getFirstElement();
        bookmark.getResourceClass().constructResource(
                bookmark.getRequestConstraints());
    }

    /**
     * Adds tooltip text to the tree with information about the selected item
     */
    private void displayInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        for (TreeItem ti : productTree.getSelection()) {
            if (stringBuilder.length() > 0) {
                stringBuilder.append("\n---------------\n");
            }
            AbstractProductBrowserDataDefinition<?> prod = getDataDef(ti);
            String[] info = getProductURI(ti, false);
            if (prod instanceof AbstractRequestableProductBrowserDataDefinition<?>) {
                AbstractRequestableProductBrowserDataDefinition<?> aProd = (AbstractRequestableProductBrowserDataDefinition<?>) prod;
                stringBuilder.append(aProd.PLUGIN_NAME + " = "
                        + aProd.productName);
            } else {
                stringBuilder.append(prod.displayName);
            }
            for (int i = 1; i < info.length; i++) {
                stringBuilder.append("\n");
                if (prod instanceof AbstractRequestableProductBrowserDataDefinition<?>) {
                    stringBuilder
                            .append(((AbstractRequestableProductBrowserDataDefinition<?>) prod).order[i - 1]
                                    + " = " + info[i]);
                } else {
                    stringBuilder.append(info[i]);
                }
            }
            productTree.setToolTipText(stringBuilder.toString());
        }
    }

    @Override
    public void setFocus() {
        productTree.setFocus();
    }

    /**
     * Using the tree item, gets the URI based on where it is within the tree
     * 
     * @param item
     * @return
     */
    public static String[] getProductURI(TreeItem item, boolean isName) {
        List<String> data = new ArrayList<String>();
        while (item != null) {
            ProductBrowserLabel label = getLabel(item);
            if (isName || label.getData() == null) {
                data.add(label.getName());
            } else {
                data.add(label.getData());
            }
            item = item.getParentItem();
        }
        Collections.reverse(data);
        return data.toArray(new String[0]);
    }

    public static ProductBrowserLabel getLabel(TreeItem item) {
        return (ProductBrowserLabel) item.getData(LABEL_DATA_KEY);
    }

    public static AbstractProductBrowserDataDefinition<?> getDataDef(
            TreeItem item) {
        return (AbstractProductBrowserDataDefinition<?>) item
                .getData(DEF_DATA_KEY);
    }
}
