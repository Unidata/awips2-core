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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.part.ViewPart;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.productbrowser.jobs.ProductBrowserQueryJob;
import com.raytheon.uf.viz.productbrowser.jobs.ProductBrowserUpdateDataTypeJob;
import com.raytheon.uf.viz.productbrowser.pref.ProductBrowserPreferenceConstants;

/**
 * Product browser view implementation
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * May 03, 2010           mnash     Initial creation
 * May 02, 2013  1949     bsteffen  Switch Product Browser from uengine to
 *                                  DbQueryRequest.
 * May 13, 2014  3135     bsteffen  Make all queries async.
 * Jun 02, 2015  4153     bsteffen  Access data definition through an interface.
 * Aug 10, 2015  4717     mapeters  Added collapse all button, don't collapse on refresh,
 *                                  expand/collapse on double click.
 * Sep 03, 2015  4717     mapeters  Added maxDepth limitation to refresh.
 * Sep 11, 2015  4717     mapeters  Don't need to copy/dispose tree items when updating them.
 * Jan 07, 2016  5176     tgurney   Check for null on double-click of tree item.
 * Jun 13, 2016  5682     bsteffen  Fix double click empty selection
 * Jun 14, 2018  7026     bsteffen  Ensure Update job finishes before starting query jobs.
 * 
 * </pre>
 * 
 * @author mnash
 */
public class ProductBrowserView extends ViewPart {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(ProductBrowserView.class);

    public static final String ID = "com.raytheon.uf.viz.productbrowser.ProductBrowserView";

    public static final String LABEL_DATA_KEY = "label";

    public static final String DEF_DATA_KEY = "definition";

    private static IExtension[] extensions;

    private Tree productTree;

    private Action loadProductAction;

    private Action productInfoAction;

    private Action collapseAction;

    private Action refreshAction;

    @Override
    public void createPartControl(Composite parent) {
        Composite fullComp = new Composite(parent, SWT.FILL);
        fullComp.setLayout(new GridLayout(1, true));
        getDataTypes();
        createActions();
        createToolbar();
        createProductTree(fullComp);
        createProductBrowserContextMenu();
    }

    /**
     * Provides the actions available to the product tree
     */
    private void createActions() {
        collapseAction = new Action("Collapse All", IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                for (TreeItem item : productTree.getItems()) {
                    recursiveCollapse(item);
                }
            }

            private void recursiveCollapse(TreeItem item) {
                for (TreeItem child : item.getItems()) {
                    recursiveCollapse(child);
                }
                item.setExpanded(false);
            }
        };
        collapseAction.setImageDescriptor(
                ProductBrowserUtils.getImageDescriptor("collapse.gif"));

        refreshAction = new Action("Refresh Browser") {
            @Override
            public void run() {
                updateAvailableDataTypes(null);
            }
        };
        refreshAction.setId("refreshAction");
        refreshAction.setImageDescriptor(
                ProductBrowserUtils.getImageDescriptor("refresh.gif"));

        productInfoAction = new Action("Product Info") {
            @Override
            public void run() {
                displayInfo();
            }
        };
        productInfoAction.setId("productInfoAction");
        productInfoAction.setImageDescriptor(
                ProductBrowserUtils.getImageDescriptor("help.gif"));

        loadProductAction = new Action("Load Product") {
            @Override
            public void run() {
                loadProduct(null);
            }
        };
        loadProductAction.setId("loadProductAction");
        loadProductAction.setImageDescriptor(
                ProductBrowserUtils.getImageDescriptor("run.gif"));
    }

    /**
     * Refresh the available data types and their contents.
     * 
     * @param dataTypeName
     *            the name of the data type to be refreshed, or null if all
     *            should be refreshed
     * @param oldOrder
     *            the old order of items in the tree for the given data type
     */
    public void refresh(String dataTypeName, String[] oldOrder) {
        if (dataTypeName != null && oldOrder != null) {
            String[] newOrder = ProductBrowserPreferenceConstants
                    .getOrder(dataTypeName);
            if (newOrder != null) {
                int maxDepth = -1;
                /*
                 * Determine max depth at which to perform refreshes in tree
                 * based on where the old and new orders differ (should go as
                 * far as 1 level above first difference).
                 */
                for (int i = 0; i < oldOrder.length; i++) {
                    if (!newOrder[i].equals(oldOrder[i])) {
                        maxDepth = i;
                        break;
                    }
                }
                if (maxDepth >= 0) {
                    for (TreeItem item : productTree.getItems()) {
                        if (getLabel(item).getName()
                                .equalsIgnoreCase(dataTypeName)) {
                            closeChildren(item, maxDepth);
                        }
                    }
                }
            }
        }
        updateAvailableDataTypes(dataTypeName);
    }

    private void closeChildren(TreeItem item, int depth) {
        for (TreeItem child : item.getItems()) {
            if (depth == 0) {
                child.dispose();
            } else {
                closeChildren(child, depth - 1);
            }
        }
    }

    /**
     * Creates the product browser that spans the entire view...
     * 
     * @param parent
     */
    public void createProductTree(final Composite parent) {
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        productTree = new Tree(parent,
                SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
        productTree.setLayoutData(gridData);

        /*
         * Wrap productTree as TreeViewer to handle tree expansions and listen
         * for double clicks
         */
        TreeViewer productTreeViewer = new TreeViewer(productTree) {
            @Override
            protected void handleTreeExpand(TreeEvent event) {
                ProductBrowserQueryJob.startJob((TreeItem) event.item);
            }
        };
        productTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                TreeItem[] selection = productTree.getSelection();
                if (selection == null || selection.length == 0) {
                    return;
                }
                TreeItem selectedTreeItem = selection[0];
                if (getLabel(selectedTreeItem) != null) {
                    if (!getLabel(selectedTreeItem).isProduct()) {
                        selectedTreeItem
                                .setExpanded(!selectedTreeItem.getExpanded());
                        ProductBrowserQueryJob.startJob(selectedTreeItem);
                    } else {
                        loadProductAction.run();
                    }
                }
            }
        });

        // Populate product tree with available data types
        updateAvailableDataTypes(null);
    }

    /**
     * Builds the toolbar at the top of the view
     */
    private void createToolbar() {
        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        mgr.add(collapseAction);
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
                ProductBrowserDataDefinition def = getDataDef(selection[0]);
                String[] path = getProductURI(selection[0], false);
                Collection<DisplayType> displayTypes = def
                        .getValidDisplayTypes(path);
                if (displayTypes != null && displayTypes.size() > 1) {
                    MenuManager menuMgr = new MenuManager("Load As...",
                            ProductBrowserUtils.getImageDescriptor("run.gif"),
                            "");
                    for (DisplayType type : displayTypes) {
                        menuMgr.add(getDisplayTypeAction(type));
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

    /**
     * Update (or initialize) the data types shown in the product tree based on
     * the availability of their data. This function does not populate the tree
     * with the data, just the plugin names.
     * 
     * @param dataTypeName
     *            the name of the data type to be updated, or null if all should
     *            be updated
     */
    private void updateAvailableDataTypes(String dataTypeName) {
        boolean updateAll = (dataTypeName == null);
        for (IExtension ext : extensions) {
            IConfigurationElement[] config = ext.getConfigurationElements();
            for (IConfigurationElement element : config) {
                ProductBrowserDataDefinition prod = null;
                try {
                    prod = (ProductBrowserDataDefinition) element
                            .createExecutableExtension("class");
                } catch (CoreException e) {
                    statusHandler.error(
                            "A product browser data definition has failed to load.",
                            e);
                    continue;
                }
                List<ProductBrowserLabel> labels = prod
                        .getLabels(new String[0]);
                for (ProductBrowserLabel label : labels) {
                    String labelName = label.getName();
                    if (!updateAll
                            && !labelName.equalsIgnoreCase(dataTypeName)) {
                        continue;
                    }
                    TreeItem dataTypeToUpdate = null;
                    // Sort alphabetically
                    int index = 0;
                    for (TreeItem dataType : productTree.getItems()) {
                        int compareValue = getLabel(dataType).getName()
                                .compareToIgnoreCase(labelName);
                        if (compareValue == 0) {
                            dataTypeToUpdate = dataType;
                            break;
                        } else if (compareValue > 0) {
                            break;
                        } else {
                            index++;
                        }
                    }
                    if (dataTypeToUpdate == null) {
                        dataTypeToUpdate = new TreeItem(productTree, SWT.NONE,
                                index);
                        dataTypeToUpdate.setData(LABEL_DATA_KEY, label);
                        dataTypeToUpdate.setData(DEF_DATA_KEY, prod);
                        Font font = dataTypeToUpdate.getFont();
                        FontData fontData = font.getFontData()[0];
                        fontData = new FontData(fontData.getName(),
                                fontData.getHeight(), SWT.BOLD);
                        font = new Font(dataTypeToUpdate.getDisplay(),
                                fontData);
                        dataTypeToUpdate.setFont(font);
                    }
                    String displayText = "Checking Availability of " + labelName
                            + "...";
                    dataTypeToUpdate.setText(displayText);
                    new ProductBrowserUpdateDataTypeJob(dataTypeToUpdate)
                            .schedule();
                }
            }
        }
    }

    /**
     * Using reflection and the eclipse registry, agnostically finds the
     * available types to populate the tree
     */
    private static synchronized void getDataTypes() {
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
                    ProductBrowserDataDefinition def = getDataDef(item);
                    String[] path = getProductURI(item, false);
                    def.loadResource(path, type);
                }
            }
        }
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
            ProductBrowserDataDefinition prod = getDataDef(ti);
            String[] info = getProductURI(ti, false);
            stringBuilder.append(prod.getProductInfo(info));
            productTree.setToolTipText(stringBuilder.toString());
            productTree.addMouseMoveListener(new MouseMoveListener() {
                @Override
                public void mouseMove(MouseEvent e) {
                    productTree.setToolTipText("");
                    productTree.removeMouseMoveListener(this);
                }
            });
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
        List<String> data = new ArrayList<>();
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

    public static ProductBrowserDataDefinition getDataDef(TreeItem item) {
        return (ProductBrowserDataDefinition) item.getData(DEF_DATA_KEY);
    }

    @Override
    public void dispose() {
        super.dispose();
        Job.getJobManager().cancel(ProductBrowserQueryJob.class);
    }
}
