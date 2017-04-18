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
package com.raytheon.uf.viz.localization.perspective.view;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.compare.CompareUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.raytheon.uf.common.localization.Checksum;
import com.raytheon.uf.common.localization.FileUpdatedMessage;
import com.raytheon.uf.common.localization.FileUpdatedMessage.FileChangeType;
import com.raytheon.uf.common.localization.ILocalizationFile;
import com.raytheon.uf.common.localization.ILocalizationPathObserver;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.localization.LocalizationManager;
import com.raytheon.uf.viz.localization.perspective.Activator;
import com.raytheon.uf.viz.localization.perspective.adapter.LocalizationPerspectiveAdapter;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorInput;
import com.raytheon.uf.viz.localization.perspective.editor.LocalizationEditorUtils;
import com.raytheon.uf.viz.localization.perspective.service.ILocalizationService;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationCompareEditorInput;
import com.raytheon.uf.viz.localization.perspective.ui.compare.LocalizationMergeEditorInput;
import com.raytheon.uf.viz.localization.perspective.view.actions.CopyToAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.DeleteAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.ImportFileAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.MoveFileAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.OpenAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.OpenWithAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.PasteFileAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.ShowAllAction;
import com.raytheon.uf.viz.localization.perspective.view.actions.ShowLevelsAction;

/**
 * File Tree View for the localization perspective.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 26, 2010           mnash     Initial creation
 * Feb 13, 2013  1610     mschenke  Fixed null pointer by repopulating
 *                                  LocalizationFileGroupData objects even if
 *                                  they weren't expanded
 * May 01, 2013  1967     njensen   Fix for pydev 2.7
 * Sep 17, 2013  2285     mschenke  Made openFile refresh items if file not
 *                                  found
 * Oct 09, 2013  2104     mschenke  Fixed file delete/add refresh issue and file
 *                                  change message found when testing
 *                                  scalesInfo.xml file
 * Sep 18, 2014  3531     bclement  fixed file delete/add refresh issue when
 *                                  paths share string prefixes
 * Feb 06, 2015  4028     mapeters  fixed file selection issue when reopening
 *                                  CAVE with files open
 * Apr 02, 2015  4288     randerso  Fix Widget is disposed error
 * Aug 24, 2015  4393     njensen   Updates for observer changes
 * Oct 13, 2015  4410     bsteffen  Allow localization perspective to mix files
 *                                  for multiple Localization Types.
 * Nov 12, 2015  4834     njensen   Changed LocalizationOpFailedException to
 *                                  LocalizationException
 * Nov 18, 2015  4834     njensen   Updated to register file observing on
 *                                  PathManager
 * Dec 03, 2015  4834     njensen   Updated for ILocalizationFile changes
 * Jan 06, 2016  4834     nabowle   Fix single-user edit-save-edit-save.
 * Jan 11, 2016  5242     kbisanz   Replaced calls to deprecated
 *                                  LocalizationFile methods
 * Jan 15, 2016  5242     kbisanz   Replaced LocalizationFile with
 *                                  ILocalizationFile where possible
 * Mar 25, 2016  5214     mapeters  Support deletion of directories containing
 *                                  only USER-level files,
 * Apr 14, 2016  4946     mapeters  Fix duplicate files listed in delete
 *                                  confirmation dialog improved tree's
 *                                  double-click handling
 * Apr 21, 2016  5214     mapeters  Fix incorrect ordering of files in multiple
 *                                  localization levels
 * May 23, 2016  4907     mapeters  Implement IWindowListener and update
 *                                  partActivated(), don't save if local file
 *                                  matches remote,
 * Jun 09, 2016  4907     mapeters  Prevent exception when double clicking with
 *                                  nothing selected
 * Aug 11, 2016  5816     randerso  Changed find to search for deepest match of
 *                                  nearest parent instead of first partial
 *                                  match
 * Apr 12, 2017  6188     bsteffen  Ensure that is possible to find and open
 *                                  files that have a PathData containing only
 *                                  '/', such as cave_config files.
 * 
 * </pre>
 * 
 * @author mnash
 */
public class FileTreeView extends ViewPart
        implements IPartListener2, IWindowListener, ILocalizationService,
        IResourceChangeListener, ILocalizationPathObserver {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(FileTreeView.class);

    private class FileUpdateRefresher implements Runnable {

        private final ILocalizationFile file;

        private final FileChangeType type;

        public FileUpdateRefresher(ILocalizationFile file,
                FileChangeType type) {
            this.file = file;
            this.type = type;
        }

        @Override
        public void run() {
            // Find and refresh file in tree
            Tree tree = getTree();
            if ((tree != null) && !tree.isDisposed()) {
                for (TreeItem appItem : tree.getItems()) {
                    for (TreeItem rootItem : appItem.getItems()) {
                        TreeItem found = find(rootItem, file.getContext(),
                                file.getPath(), false);
                        if (found != null) {
                            /*
                             * File found. If updated, set the time stamp to
                             * that of the file to avoid modification change
                             * discrepancies
                             */
                            if (type == FileChangeType.UPDATED) {
                                if (found
                                        .getData() instanceof LocalizationFileGroupData) {
                                    for (LocalizationFileEntryData data : ((LocalizationFileGroupData) found
                                            .getData()).getChildrenData()) {
                                        if (data.getFile().equals(file)) {
                                            try {
                                                data.getResource()
                                                        .setLocalTimeStamp(file
                                                                .getTimeStamp()
                                                                .getTime());
                                            } catch (CoreException e) {
                                                statusHandler.handle(
                                                        Priority.INFO,
                                                        "Could not update workspace file timestamp: "
                                                                + e.getLocalizedMessage(),
                                                        e);
                                            }
                                        }
                                    }
                                }
                            } else {
                                // ADD/DELETE, refresh the file
                                refresh(found);
                            }
                        }
                    }
                }
            }
        }

    }

    private static final Comparator<ILocalizationFile> LOCALIZATION_FILE_LEVEL_COMPARATOR = new Comparator<ILocalizationFile>() {
        @Override
        public int compare(ILocalizationFile o1, ILocalizationFile o2) {
            return o1.getContext().getLocalizationLevel()
                    .compareTo(o2.getContext().getLocalizationLevel());
        }
    };

    /** Flag for linking view to active editor */
    private boolean linkWithEditor = true;

    /** Determines what levels we should show in the view */
    private Set<LocalizationLevel> showSet = new HashSet<>();

    /** Set for determining what leves we should display all contexts for */
    private Set<LocalizationLevel> showAllSet = new HashSet<>();

    /** Cache of available contexts for each level */
    private Map<LocalizationLevel, Set<String>> contextMap = new HashMap<>();

    /** The File Tree widget. */
    private TreeViewer viewer;

    /** Image map for TreeItem icons */
    private Map<ImageDescriptor, Image> imageMap = new HashMap<>();

    /** TreeItem image icon for directories */
    private Image directoryImage;

    /** Waiting cursor */
    private Cursor waitCursor = null;

    /** Last file selected for "Copy" */
    private LocalizationFile copyFile = null;

    /** Workspace project used to store links to localization files */
    private IProject localizationProject;

    /**
     * @return the tree
     */
    public Tree getTree() {
        return viewer.getTree();
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

        localizationProject = Activator.getDefault().getLocalizationProject();
        directoryImage = getImageDescriptor("directory.gif").createImage();
        Display display = site.getShell().getDisplay();
        waitCursor = display.getSystemCursor(SWT.CURSOR_WAIT);

        site.getPage().addPartListener(this);

        site.getWorkbenchWindow().getWorkbench().addWindowListener(this);

        PathManagerFactory.getPathManager()
                .addLocalizationPathObserver(IPathManager.SEPARATOR, this);

        ResourcesPlugin.getWorkspace().addResourceChangeListener(this,
                IResourceChangeEvent.POST_CHANGE);

        // Show all levels by default
        showSet.addAll(Arrays.asList(LocalizationLevel.values()));
    }

    @Override
    public void dispose() {
        super.dispose();
        if (!directoryImage.isDisposed()) {
            directoryImage.dispose();
        }

        for (Image img : imageMap.values()) {
            if (!img.isDisposed()) {
                img.dispose();
            }
        }

        if (!getTree().isDisposed()) {
            getTree().dispose();
        }

        getSite().getPage().removePartListener(this);
        getSite().getPage().getWorkbenchWindow().getWorkbench()
                .removeWindowListener(this);
        PathManagerFactory.getPathManager()
                .removeLocalizationPathObserver(this);
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
    }

    @Override
    public void createPartControl(Composite parent) {
        // construct tree
        createFileTree(parent);
        populateTree();
    }

    /**
     * Creation of the tree component
     * 
     * @param parent
     *            composite to add tree to
     */
    private void createFileTree(Composite parent) {
        Tree tree = new Tree(parent, SWT.BORDER | SWT.MULTI);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        viewer = new TreeViewer(tree);

        // Tooltip listeners for showing tool tip for protected files
        final String[] toolTip = new String[1];
        tree.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseHover(MouseEvent e) {
                Tree tree = getTree();
                TreeItem item = tree.getItem(new Point(e.x, e.y));
                if (item != null) {
                    LocalizationLevel protectedLevel = null;
                    if (item.getData() instanceof LocalizationFileEntryData) {
                        protectedLevel = ((LocalizationFileEntryData) item
                                .getData()).getFile().getProtectedLevel();
                    } else if (item
                            .getData() instanceof LocalizationFileGroupData) {
                        for (LocalizationFileEntryData entry : ((LocalizationFileGroupData) item
                                .getData()).getChildrenData()) {
                            protectedLevel = entry.getFile()
                                    .getProtectedLevel();
                            break;
                        }
                    }
                    if (protectedLevel != null) {
                        String tip = "Protected @ " + protectedLevel.name();
                        tree.setToolTipText(tip);
                        toolTip[0] = tip;
                    }
                }
            }
        });

        tree.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                if (toolTip[0] != null) {
                    getTree().setToolTipText("");
                    toolTip[0] = null;
                }
            }
        });

        tree.addListener(SWT.Expand, new Listener() {
            @Override
            public void handleEvent(Event event) {
                setWaiting();
                try {
                    populateNode((TreeItem) event.item);
                } finally {
                    setDoneWaiting();
                }
            }
        });

        tree.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Tree tree = getTree();
                TreeItem[] items = tree.getSelection();
                if (items.length == 1) {
                    Object data = items[0].getData();
                    if (data instanceof LocalizationFileEntryData) {
                        IFile file = ((LocalizationFileEntryData) data)
                                .getResource();
                        IWorkbenchPage page = getSite().getPage();
                        for (IEditorReference ref : page
                                .getEditorReferences()) {
                            IEditorInput input = null;
                            try {
                                input = ref.getEditorInput();
                            } catch (PartInitException pie) {
                                statusHandler.handle(Priority.PROBLEM,
                                        pie.getLocalizedMessage(), pie);
                            }
                            if (input instanceof LocalizationEditorInput) {
                                if (file.equals(
                                        ((LocalizationEditorInput) input)
                                                .getFile())) {
                                    /*
                                     * Can only safely pass true to getEditor()
                                     * once above conditions are met.
                                     */
                                    IEditorPart part = ref.getEditor(true);
                                    page.activate(part);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        });

        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                setWaiting();
                try {
                    Tree tree = getTree();
                    TreeItem[] selection = tree.getSelection();
                    if (selection.length == 0) {
                        return;
                    }
                    TreeItem ti = selection[0];
                    if (ti.getData() instanceof LocalizationFileEntryData) {
                        LocalizationFileEntryData data = (LocalizationFileEntryData) ti
                                .getData();
                        new OpenAction(getSite().getPage(),
                                new LocalizationFileEntryData[] { data }).run();
                    } else {
                        populateNode(ti);
                        if (!ti.getExpanded()) {
                            ti.setExpanded(true);
                        } else {
                            ti.setExpanded(false);
                        }
                    }
                } finally {
                    setDoneWaiting();
                }
            }
        });

        MenuManager menuMgr = new MenuManager();
        menuMgr.setRemoveAllWhenShown(true);
        menuMgr.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager mgr) {
                fillContextMenu(mgr);
            }
        });
        Menu menu = menuMgr.createContextMenu(tree);
        tree.setMenu(menu);

        IToolBarManager mgr = getViewSite().getActionBars().getToolBarManager();
        Action linkAction = new Action("Link with Editor",
                IAction.AS_CHECK_BOX) {
            @Override
            public void run() {
                linkWithEditor = !linkWithEditor;
                if (linkWithEditor) {
                    IEditorPart part = getSite().getPage().getActiveEditor();
                    if (part != null) {
                        IEditorInput input = part.getEditorInput();
                        if (input instanceof LocalizationEditorInput) {
                            selectFile(((LocalizationEditorInput) input)
                                    .getLocalizationFile());
                        }
                    }
                }
                setChecked(linkWithEditor);
            }
        };
        linkAction.setChecked(linkWithEditor);
        linkAction.setImageDescriptor(getImageDescriptor("link.gif"));

        Action collapseAction = new Action("Collapse All",
                IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                Tree tree = getTree();
                for (TreeItem item : tree.getItems()) {
                    recursiveCollapse(item);
                }
            }

            private void recursiveCollapse(TreeItem item) {
                for (TreeItem ti : item.getItems()) {
                    recursiveCollapse(ti);
                }
                item.setExpanded(false);
            }
        };
        collapseAction.setImageDescriptor(getImageDescriptor("collapse.gif"));

        mgr.add(collapseAction);
        mgr.add(linkAction);

        IMenuManager viewMenu = getViewSite().getActionBars().getMenuManager();
        viewMenu.add(new ShowLevelsAction(this));
        viewMenu.add(new ShowAllAction(this));
        viewMenu.add(new Separator());
        viewMenu.add(linkAction);

        LocalizationFileDragNDropSource dragNDropSource = new LocalizationFileDragNDropSource(
                this, viewer);
        viewer.addDragSupport(DND.DROP_MOVE | DND.DROP_COPY,
                new Transfer[] { FileTransfer.getInstance() }, dragNDropSource);
        viewer.addDropSupport(DND.DROP_MOVE | DND.DROP_COPY,
                new Transfer[] { FileTransfer.getInstance() }, dragNDropSource);
    }

    /**
     * Repopulates all expanded tree items in the tree.
     * 
     * @param tree
     */
    private void repopulateTree(Tree tree) {
        setWaiting();
        try {
            for (TreeItem item : tree.getItems()) {
                repopulateTreeItem(item);
            }
        } finally {
            setDoneWaiting();
        }
    }

    /**
     * Repopulates all tree items from this item down.
     * 
     * @param item
     */
    private void repopulateTreeItem(TreeItem item) {
        Tree tree = item.getParent();
        // Get set of selected data objects
        Set<Object> selectionSet = new HashSet<>();
        buildSelectionSet(tree.getSelection(), selectionSet, item);
        // Recursively repopulate
        repopulateTreeItemRecursive(item);
        if (!item.isDisposed()) {
            // If item wasn't removed, reselect items
            List<TreeItem> selected = new ArrayList<>();
            select(selected, selectionSet, item);
            selected.addAll(Arrays.asList(tree.getSelection()));
            tree.setSelection(selected.toArray(new TreeItem[selected.size()]));
        }
    }

    private void repopulateTreeItemRecursive(TreeItem item) {
        if (item.getData() instanceof FileTreeEntryData) {
            // These are directory nodes
            FileTreeEntryData data = (FileTreeEntryData) item.getData();
            if (!((data instanceof LocalizationFileEntryData))
                    && data.hasRequestedChildren()) {
                // Item has been populated, refresh
                Map<FileTreeEntryData, Boolean> expandMap = new HashMap<>();
                buildExpandedMap(expandMap, item);
                item.removeAll();
                data.setRequestedChildren(false);
                new TreeItem(item, SWT.NONE);
                expand(expandMap, item);
                if ((item.getData() instanceof LocalizationFileGroupData)
                        && (item.getItemCount() == 0)) {
                    item.dispose();
                }
            }
        } else {
            for (TreeItem child : item.getItems()) {
                repopulateTreeItemRecursive(child);
            }
        }
    }

    private void buildSelectionSet(TreeItem[] selected, Set<Object> set,
            TreeItem root) {
        for (TreeItem selection : selected) {
            if (selection == root) {
                set.add(root.getData());
                break;
            }
        }

        for (TreeItem item : root.getItems()) {
            buildSelectionSet(selected, set, item);
        }
    }

    private void buildExpandedMap(Map<FileTreeEntryData, Boolean> map,
            TreeItem root) {
        FileTreeEntryData data = (FileTreeEntryData) root.getData();
        if (data != null) {
            map.put(data, root.getExpanded() || (root.getItemCount() == 0));
            for (TreeItem item : root.getItems()) {
                buildExpandedMap(map, item);
            }
        }
    }

    private void select(List<TreeItem> selections, Set<Object> set,
            TreeItem item) {
        if (item.getData() != null) {
            if (set.contains(item.getData())) {
                selections.add(item);
            }
        }
        for (TreeItem child : item.getItems()) {
            select(selections, set, child);
        }
    }

    private void expand(Map<FileTreeEntryData, Boolean> map, TreeItem item) {
        boolean wasExpanded = map.containsKey(item.getData())
                && map.get(item.getData());
        if (wasExpanded
                || (item.getData() instanceof LocalizationFileGroupData)) {
            populateNode(item);
            if (wasExpanded) {
                item.setExpanded(true);
                for (TreeItem child : item.getItems()) {
                    expand(map, child);
                }
            }
        }
    }

    /**
     * Initial population of the tree.
     */
    private void populateTree() {
        Collection<PathData> pathList = PathDataExtManager.getPathData();
        for (PathData pd : pathList) {
            addPathDataTreeNode(pd);
        }
    }

    /**
     * Adds nodes to tree for path data object
     * 
     * @param pd
     */
    private void addPathDataTreeNode(PathData pd) {
        String application = pd.getApplication();
        String name = pd.getName();
        if (application.contains(File.separator)
                || application.contains(IPathManager.SEPARATOR)) {
            statusHandler.handle(Priority.PROBLEM, "Problem adding "
                    + pd.getApplication()
                    + " folder: Path names cannot contain path separator string");
            return;
        }
        if (name.contains(File.separator)
                || name.contains(IPathManager.SEPARATOR)) {
            statusHandler.handle(Priority.PROBLEM, "Problem adding "
                    + pd.getName()
                    + " folder: Path names cannot contain path separator string");
            return;
        }

        Tree tree = getTree();
        TreeItem applicationItem = null;
        for (TreeItem item : tree.getItems()) {
            if (application.equals(item.getText())) {
                applicationItem = item;
            }
        }

        if (applicationItem == null) {
            // Create tree item for application
            applicationItem = new TreeItem(tree, SWT.NONE,
                    getInsertionIndex(tree.getItems(), application));
            applicationItem.setText(application);
            applicationItem.setImage(directoryImage);

            // Create folder in project for application
            IFolder folder = localizationProject.getFolder(application);
            if (!folder.exists()) {
                try {
                    folder.create(true, true, null);
                } catch (CoreException e) {
                    statusHandler.handle(Priority.PROBLEM,
                            "Error creating application folder", e);
                }
            }
            applicationItem.setData(folder);
        }

        FileTreeEntryData treeData = new FileTreeEntryData(pd, pd.getPath(),
                true);
        treeData.setName(pd.getName());
        TreeItem dataTypeItem = new TreeItem(applicationItem, SWT.NONE,
                getInsertionIndex(applicationItem.getItems(), name));
        dataTypeItem.setData(treeData);
        dataTypeItem.setText(name);
        dataTypeItem.setImage(directoryImage);
        // Add empty item so we get ability to expand
        new TreeItem(dataTypeItem, SWT.NONE);

        // Create folder for PathData
        treeData.setResource(createFolder((IFolder) applicationItem.getData(),
                treeData.getName()));
    }

    private IFolder createFolder(IFolder parent, String name) {
        IFolder folder = parent.getFolder(name);
        if (!folder.exists()) {
            try {
                folder.create(true, true, null);
            } catch (CoreException e) {
                statusHandler.handle(Priority.PROBLEM,
                        "Error creating application folder", e);
            }
        }
        return folder;
    }

    /**
     * Get the insertion index if one were to insert a tree item into the
     * curItems list with text of name
     * 
     * @param curItems
     * @param name
     * @return
     */
    private int getInsertionIndex(TreeItem[] curItems, String name) {
        /* add to end */
        int idx = curItems.length;
        for (int i = 0; i < curItems.length; ++i) {
            String text = curItems[i].getText();
            int comp = text.compareToIgnoreCase(name);
            if (comp >= 0) {
                idx = i;
                break;
            }
        }
        return idx;
    }

    /**
     * Fill the context menu with localization file operatiosn
     * 
     * @param mgr
     *            menu manager to fill
     */
    private void fillContextMenu(IMenuManager mgr) {
        Tree tree = getTree();
        final TreeItem[] selected = tree.getSelection();

        mgr.add(new MenuManager("New", LocalizationPerspectiveAdapter.NEW_ID));
        mgr.add(new Separator());

        if (selected.length > 0) {
            List<FileTreeEntryData> dataList = new ArrayList<>();

            // Get the selected items
            for (TreeItem item : selected) {
                if (item.getData() instanceof FileTreeEntryData) {
                    FileTreeEntryData data = (FileTreeEntryData) item.getData();
                    dataList.add(data);
                } else {
                    dataList.clear();
                    break;
                }
            }

            if (!dataList.isEmpty()) {
                // Verify the PathData objects are the same
                PathData prev = dataList.get(0).getPathData();
                for (FileTreeEntryData data : dataList) {
                    if (data.getPathData() != prev) {
                        dataList.clear();
                        break;
                    } else {
                        prev = data.getPathData();
                    }
                }

                if (!dataList.isEmpty()) {
                    // All the same... add adapter items
                    if (prev.getAdapter().addContextMenuItems(mgr, dataList
                            .toArray(new FileTreeEntryData[dataList.size()]))) {
                        mgr.add(new Separator());
                    }
                }
            }
        }

        final List<LocalizationFile> fileList = new ArrayList<>();
        final List<LocalizationFileEntryData> fileDataList = new ArrayList<>();
        // get list of files selected
        if (selected.length > 0) {
            // Open File(s) list
            for (TreeItem item : selected) {
                if (item.getData() instanceof LocalizationFileEntryData) {
                    LocalizationFileEntryData data = (LocalizationFileEntryData) item
                            .getData();
                    fileDataList.add(data);
                    fileList.add(data.getFile());
                }
            }
        }

        // Add "Open" group if all selected items have files
        if (fileList.size() == selected.length) {
            mgr.add(new OpenAction(getSite().getPage(), fileDataList
                    .toArray(new LocalizationFileEntryData[fileList.size()])));
            if (fileList.size() == 1) {
                // add open with...
                LocalizationFileEntryData data = (LocalizationFileEntryData) selected[0]
                        .getData();
                mgr.add(new OpenWithAction(getSite().getPage(),
                        fileDataList.get(0), data.getResource(),
                        data.getPathData().getAdapter()));
            }
            mgr.add(new Separator());
        }

        // Add Copy/Paste/Delete
        if ((fileList.size() == 1) && (selected.length == 1)) {
            mgr.add(new Action("Copy") {
                @Override
                public void run() {
                    copyFile = fileList.get(0);
                }
            });

            mgr.add(new CopyToAction(fileDataList.get(0), this));
            mgr.add(new DeleteAction(getSite().getPage(),
                    fileList.toArray(new LocalizationFile[0])));

            mgr.add(new Separator());
        } else if ((selected.length == 1)
                && (selected[0].getData() instanceof LocalizationFileGroupData)
                && (copyFile != null)) {
            mgr.add(new PasteFileAction(this, this.copyFile,
                    (LocalizationFileGroupData) selected[0].getData()));
            mgr.add(new Separator());
        } else if (fileList.size() == selected.length) {
            mgr.add(new DeleteAction(getSite().getPage(),
                    fileList.toArray(new LocalizationFile[fileList.size()])));
            mgr.add(new Separator());
        } else {
            LocalizationFile[] toDelete = buildDeleteFileList(selected);

            if (toDelete.length > 0) {
                mgr.add(new DeleteAction(getSite().getPage(), toDelete));
                mgr.add(new Separator());
            }
        }

        // Add the move to item
        if ((selected.length == 1) && (fileDataList.size() == 1)) {
            mgr.add(new MoveFileAction(getSite().getPage(), fileDataList.get(0),
                    this));
            mgr.add(new Separator());
        }

        // Add the compare item
        if ((selected.length == 2) && (fileList.size() == 2)) {
            mgr.add(new Separator());
            mgr.add(new Action("Compare") {
                @Override
                public void run() {
                    LocalizationFileEntryData left = (LocalizationFileEntryData) selected[0]
                            .getData();
                    LocalizationFileEntryData right = (LocalizationFileEntryData) selected[1]
                            .getData();
                    LocalizationCompareEditorInput editorInput = new LocalizationCompareEditorInput(
                            new LocalizationEditorInput(left.getFile(),
                                    left.getResource()),
                            new LocalizationEditorInput(right.getFile(),
                                    right.getResource()));
                    CompareUI.openCompareEditor(editorInput);
                }

            });
            mgr.add(new Separator());
        }

        mgr.add(new Action("Refresh") {
            @Override
            public void run() {
                setWaiting();
                try {
                    refresh(selected);
                } finally {
                    setDoneWaiting();
                }
            }
        });

        if (selected.length == 1) {
            Object data = selected[0].getData();
            if (data instanceof FileTreeEntryData) {
                FileTreeEntryData fdata = (FileTreeEntryData) data;
                if (fdata.isDirectory()) {
                    // We can import into true directories, not group datas
                    mgr.add(new Separator());
                    mgr.add(new ImportFileAction(fdata.getPathData().getTypes(),
                            fdata.getPath(), fdata.getPathData().getFilter()));
                }
            }
        }
    }

    /**
     * Build a list of files to be deleted based on the given array of selected
     * TreeItems. The returned list is in the order the files appear in the
     * Localization perspective.
     * 
     * @param selected
     *            an array of the selected TreeItems.
     * @return the list of LocalizationFiles to be deleted
     */
    private LocalizationFile[] buildDeleteFileList(TreeItem[] selected) {
        List<ILocalizationFile> toDelete = new ArrayList<>();

        for (TreeItem item : selected) {
            int prevSize = toDelete.size();

            if (item.getData() instanceof FileTreeEntryData) {
                FileTreeEntryData data = (FileTreeEntryData) item.getData();
                if (!data.isRoot()) {
                    buildFileList(item, toDelete);
                }
            }

            if (prevSize == toDelete.size()) {
                /*
                 * This selected item couldn't contribute a file, don't add the
                 * action to delete any of them
                 */
                toDelete.clear();
                break;
            }
        }

        /*
         * Convert to LinkedHashSet to remove duplicates (but maintain order)
         * before converting to array. LinkedHashSet isn't used until now
         * because if a selected TreeItem only contained duplicate files, the
         * if(prevSize == toDelete.size()) block would be entered and an empty
         * list would be returned, which should not be caused by duplicate
         * files.
         */
        return new LinkedHashSet<>(toDelete).toArray(new LocalizationFile[0]);
    }

    /**
     * Builds a list of {@link ILocalizationFile}s starting at the item passed
     * in
     * 
     * @param item
     * @param files
     *            (option) list to add entries to
     * @return list of files
     */
    private List<ILocalizationFile> buildFileList(TreeItem item,
            List<ILocalizationFile> files) {
        if (files == null) {
            files = new ArrayList<>();
        }

        FileTreeEntryData data = (FileTreeEntryData) item.getData();
        if (data instanceof LocalizationFileEntryData) {
            // single item
            files.add(((LocalizationFileEntryData) data).getFile());
        } else if (data instanceof LocalizationFileGroupData) {
            for (LocalizationFileEntryData child : ((LocalizationFileGroupData) data)
                    .getChildrenData()) {
                files.add(child.getFile());
            }
        } else if (data.isDirectory()) {
            // recursive part, ensure item populated
            populateNode(item);
            IPathManager pm = PathManagerFactory.getPathManager();
            ILocalizationFile directory = pm.getStaticLocalizationFile(
                    getTreeSearchContexts(data.getPathData().getTypes()),
                    data.getPath());
            files.add(directory);
            for (TreeItem childItem : item.getItems()) {
                buildFileList(childItem, files);
            }
        }
        return files;
    }

    /**
     * Refresh the selected tree items
     */
    public void refreshSelected() {
        setWaiting();
        try {
            refresh(getTree().getSelection());
        } finally {
            setDoneWaiting();
        }
    }

    /**
     * Refreshes the TreeItems passed in
     * 
     * @param items
     */
    private void refresh(TreeItem... items) {
        for (TreeItem item : items) {
            if (!item.isDisposed()) {
                // If item is disposed, it was a child of a previous item and
                // already refreshed
                if (!(item.getData() instanceof FileTreeEntryData)) {
                    // Application level node, refresh children
                    refresh(item.getItems());
                } else {
                    FileTreeEntryData data = (FileTreeEntryData) item.getData();
                    IResource rsc = data.getResource();
                    if (rsc instanceof IFile) {
                        try {
                            rsc.refreshLocal(IResource.DEPTH_INFINITE, null);
                        } catch (CoreException e) {
                            statusHandler.handle(Priority.PROBLEM,
                                    "Error refreshing file", e);
                        }
                    } else {
                        // rsc is folder (either directory or group of files)
                        IPathManager pm = PathManagerFactory.getPathManager();
                        ILocalizationFile file = pm.getStaticLocalizationFile(
                                getTreeSearchContexts(
                                        data.getPathData().getTypes()),
                                data.getPath());
                        if (file == null || !file.exists()) {
                            item.dispose();
                        } else {
                            repopulateTreeItem(item);
                        }
                    }
                }
            }
        }
    }

    /**
     * Populates the given tree node's child items requesting data as needed
     * 
     * @param parentItem
     *            The TreeItem node to populate
     * @return true if successful
     */
    private boolean populateNode(TreeItem parentItem) {
        if (parentItem == null) {
            return false;
        }

        // Top (Application) level has no data to request
        if (!(parentItem.getData() instanceof FileTreeEntryData)) {
            return false;
        }

        FileTreeEntryData data = (FileTreeEntryData) parentItem.getData();
        if ((data instanceof LocalizationFileEntryData)
                || data.hasRequestedChildren()) {
            // Can't expand a file, or we've already requested
            return true;
        }

        // Remove all children to get rid of placeholder child
        boolean checkName = !(data instanceof LocalizationFileGroupData);

        PathData pd = data.getPathData();
        String path = data.getPath();
        String[] filter = pd.getFilter();
        boolean recursive = pd.isRecursive();
        List<LocalizationType> types = pd.getTypes();

        IPathManager pathManager = PathManagerFactory.getPathManager();

        boolean success = false;
        List<LocalizationFile> currentList = new ArrayList<>();
        LocalizationFile[] files = pathManager.listFiles(
                getTreeSearchContexts(types), path, filter, false, !recursive);
        if (files == null) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error getting list of files");
        } else {
            parentItem.removeAll();

            for (LocalizationFile file : files) {
                if (checkName && (file.getPath().isEmpty()
                        || data.getPath().equals(file.getPath()))) {
                    continue;
                }
                if (file.exists()) {
                    currentList.add(file);
                }
            }

            if (data instanceof LocalizationFileGroupData) {
                populateGroupDataNode(parentItem, currentList);
            } else {
                populateDirectoryDataNode(parentItem, currentList);
            }
            success = true;
        }

        return success;
    }

    private LocalizationContext[] getTreeSearchContexts(
            List<LocalizationType> types) {
        IPathManager pathManager = PathManagerFactory.getPathManager();
        // Request for base/site/user

        List<LocalizationContext> searchContexts = new ArrayList<>();
        for (LocalizationType type : types) {
            LocalizationContext[] searchHierarchy = pathManager
                    .getLocalSearchHierarchy(type);
            for (LocalizationContext ctx : searchHierarchy) {
                if (showSet.contains(ctx.getLocalizationLevel())) {
                    searchContexts.add(ctx);
                }
            }
        }

        // Use of LocalizationLevels.values() in this case should be okay
        // since we are requesting all possible context names for the level,
        // doesn't matter if our local context for the level is set
        LocalizationLevel[] levels = pathManager.getAvailableLevels();
        for (LocalizationLevel level : levels) {
            if (showAllSet.contains(level) && showSet.contains(level)) {
                Set<String> contexts = contextMap.get(level);
                if (contexts == null) {
                    contexts = new HashSet<>(
                            Arrays.asList(pathManager.getContextList(level)));
                    contextMap.put(level, contexts);
                }
                String myContext = LocalizationManager.getContextName(level);

                for (String context : contexts) {
                    if (((myContext != null) && myContext.equals(context))
                            || ((myContext == null) && (context == null))) {
                        continue;
                    }
                    for (LocalizationType type : types) {
                        LocalizationContext ctx = pathManager.getContext(type,
                                level);
                        ctx.setContextName(context);
                        searchContexts.add(ctx);
                    }
                }
            }
        }
        return searchContexts
                .toArray(new LocalizationContext[searchContexts.size()]);
    }

    private void populateDirectoryDataNode(TreeItem parentItem,
            List<LocalizationFile> files) {
        FileTreeEntryData data = (FileTreeEntryData) parentItem.getData();
        data.setRequestedChildren(true);
        PathData pd = data.getPathData();
        Map<String, List<LocalizationFile>> processedFiles = new HashMap<>();
        Set<String> processedPaths = new HashSet<>();
        // we are expanding a folder
        for (LocalizationFile file : files) {
            String name = file.getPath();
            if (!processedPaths.contains(name)) {
                FileTreeEntryData treeData = null;
                if (file.isDirectory()) {
                    treeData = new FileTreeEntryData(pd, name);
                } else {
                    treeData = new LocalizationFileGroupData(pd, name);
                }

                addTreeItem(parentItem, treeData);
                processedPaths.add(name);
            }
            if (!file.isDirectory()) {
                List<LocalizationFile> entryFiles = processedFiles.get(name);
                if (entryFiles == null) {
                    entryFiles = new ArrayList<>();
                    processedFiles.put(name, entryFiles);
                }
                entryFiles.add(file);
            }
        }

        TreeItem[] children = parentItem.getItems();
        for (TreeItem child : children) {
            String path = ((FileTreeEntryData) child.getData()).getPath();
            if (processedFiles.containsKey(path)) {
                populateGroupDataNode(child, processedFiles.get(path));
            }
        }
    }

    private void populateGroupDataNode(TreeItem parentItem,
            List<LocalizationFile> files) {
        // Ensure files are in correct order based on localization level
        Collections.sort(files, LOCALIZATION_FILE_LEVEL_COMPARATOR);

        List<Object> oldData = new ArrayList<>();
        for (TreeItem oldItem : parentItem.getItems()) {
            oldData.add(oldItem.getData());
        }

        parentItem.removeAll();
        LocalizationFileGroupData fData = (LocalizationFileGroupData) parentItem
                .getData();
        fData.clearChildData();
        fData.setRequestedChildren(true);
        PathData pd = fData.getPathData();
        Set<LocalizationLevel> levels = new HashSet<>();
        Set<LocalizationLevel> redundantLevels = new HashSet<>();
        for (ILocalizationFile file : files) {
            LocalizationLevel level = file.getContext().getLocalizationLevel();
            if (!levels.add(level)) {
                redundantLevels.add(level);
            }
        }
        for (LocalizationFile file : files) {
            FileTreeEntryData treeData = null;
            if (!file.isDirectory()) {
                LocalizationLevel level = file.getContext()
                        .getLocalizationLevel();
                treeData = new LocalizationFileEntryData(pd, file,
                        redundantLevels.contains(level));
                addTreeItem(parentItem, treeData);
                fData.addChildData((LocalizationFileEntryData) treeData);
            }
        }

        for (Object oldItemData : oldData) {
            if (oldItemData == null) {
                continue;
            }
            LocalizationFileEntryData oldFileData = (LocalizationFileEntryData) oldItemData;
            boolean found = false;
            for (TreeItem item : parentItem.getItems()) {
                LocalizationFileEntryData newData = (LocalizationFileEntryData) item
                        .getData();
                if (newData.getFile().equals(oldFileData.getFile())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                IResource rsc = oldFileData.getResource();
                if (rsc != null) {
                    try {
                        rsc.delete(true, null);
                    } catch (CoreException e) {
                        statusHandler.handle(Priority.PROBLEM,
                                "Error deleting resource: "
                                        + e.getLocalizedMessage(),
                                e);
                    }
                }
            }
        }
    }

    /**
     * Adds a tree item given the data and the parent item
     * 
     * @param parentItem
     * @param treeData
     * @return new TreeItem
     */
    private TreeItem addTreeItem(TreeItem parentItem,
            FileTreeEntryData treeData) {
        String name = treeData.getName();
        ILocalizationFile file = null;
        int idx = parentItem.getItemCount();
        if (treeData instanceof LocalizationFileEntryData) {
            LocalizationFileEntryData entryData = (LocalizationFileEntryData) treeData;
            file = entryData.getFile();
            LocalizationContext ctx = file.getContext();
            LocalizationLevel level = ctx.getLocalizationLevel();
            StringBuilder nameBuilder = new StringBuilder(level.toString());
            if (entryData.isMultipleTypes()
                    || level != LocalizationLevel.BASE) {
                nameBuilder.append(" (");
                if (level != LocalizationLevel.BASE) {
                    nameBuilder.append(ctx.getContextName());
                }
                if (entryData.isMultipleTypes()) {
                    if (level != LocalizationLevel.BASE) {
                        nameBuilder.append(" - ");
                    }
                    nameBuilder.append(
                            ctx.getLocalizationType().name().toLowerCase());
                }
                nameBuilder.append(")");
            }
            name = nameBuilder.toString();
        } else {
            List<TreeItem> applicableItems = new ArrayList<>();
            int start = -1;
            TreeItem[] children = parentItem.getItems();
            for (int i = 0; i < children.length; ++i) {
                TreeItem item = children[i];
                if (treeData.getClass().equals(item.getData().getClass())) {
                    if (start == -1) {
                        start = i;
                    }
                    applicableItems.add(item);
                }
            }

            start = Math.max(start, 0);
            int insert = getInsertionIndex(
                    applicableItems
                            .toArray(new TreeItem[applicableItems.size()]),
                    treeData.getName());
            idx = start + insert;
        }

        TreeItem fileItem = new TreeItem(parentItem, SWT.NONE, idx);
        fileItem.setText(name);
        fileItem.setData(treeData);
        if (file != null) {
            fileItem.setImage(getImage(file));
        } else {
            if (treeData instanceof LocalizationFileGroupData) {
                fileItem.setImage(getImage(name));
            } else {
                fileItem.setImage(getImage((String) null));
            }
        }

        if (file == null) {
            new TreeItem(fileItem, SWT.NONE);
        }

        FileTreeEntryData parentData = (FileTreeEntryData) parentItem.getData();
        IFolder folder = (IFolder) parentData.getResource();
        IResource rsc = null;
        if (file != null) {
            LocalizationContext context = file.getContext();
            rsc = folder.getFile(context.getLocalizationType() + "_"
                    + context.getLocalizationLevel() + "_"
                    + context.getContextName() + "_" + parentItem.getText());
        } else {
            rsc = createFolder(folder, fileItem.getText());
        }
        treeData.setResource(rsc);

        return fileItem;
    }

    /**
     * Get the ImageDescriptor for the provided file name.
     * 
     * @param string
     *            The file name
     * @return The ImageDescriptor
     */
    private ImageDescriptor getImageDescriptor(String string) {
        String iconPath = "icons" + File.separator;
        URL url = FileLocator.find(Activator.getDefault().getBundle(),
                new Path(iconPath + string), null);
        if (url.getFile() == null) {
            url = FileLocator.find(Activator.getDefault().getBundle(),
                    new Path(".." + File.separator + iconPath + string), null);
        }

        return ImageDescriptor.createFromURL(url);
    }

    /**
     * Get the image for the provided ILocalizationFile.
     * 
     * @param file
     *            The ILocalizationFile
     * @return The image
     */
    private Image getImage(ILocalizationFile file) {
        String name = null;
        if (file != null) {
            name = file.getPath();
        }
        return getImage(name);
    }

    /**
     * Get the image for the provided path.
     * 
     * @param filePath
     *            The file path
     * @return The image
     */
    private Image getImage(String filePath) {
        if (filePath == null) {
            return directoryImage;
        }
        ImageDescriptor desc = LocalizationEditorUtils.getEditorRegistry()
                .getImageDescriptor(filePath);
        if (desc != null) {
            Image img = imageMap.get(desc);
            if (img != null) {
                return img;
            } else {
                img = desc.createImage();
                imageMap.put(desc, img);
            }
            return img;
        } else {
            return directoryImage;
        }
    }

    private TreeItem find(ILocalizationFile file, boolean populateToFind,
            boolean nearestParent) {
        return find(file.getPath(), file.getContext(), populateToFind,
                nearestParent);
    }

    private TreeItem find(String path, LocalizationContext context,
            boolean populateToFind, boolean nearestParent) {
        Tree tree = getTree();
        TreeItem[] items = tree.getItems();
        TreeItem bestMatch = null;
        int matchLen = 0;
        for (TreeItem item : items) {
            /*
             * item is an Application level node, for each child, we try to find
             * the desired path, find will return null if we are on the
             * incorrect path, otherwise it will return the lowest directory
             * found on the path of the item we are looking for.
             * 
             * If we find a partial match to the path, we check to see if it is
             * closer to the desired item than the previous partial match.
             * 
             * If null, keep looking, otherwise check the search method
             */
            for (TreeItem basePathItem : item.getItems()) {
                TreeItem found = find(basePathItem, context, path,
                        populateToFind);

                if (found != null) {
                    FileTreeEntryData foundData = (FileTreeEntryData) found
                            .getData();
                    if (nearestParent
                            && (foundData.getPath().length() > matchLen)) {
                        bestMatch = found;
                        matchLen = foundData.getPath().length();
                    }

                    if (foundData.getPath().equals(path)) {
                        // Found this item, check if group data
                        if (foundData instanceof LocalizationFileGroupData) {
                            // Check for context matching
                            for (TreeItem fileItem : found.getItems()) {
                                LocalizationFileEntryData fileData = (LocalizationFileEntryData) fileItem
                                        .getData();
                                if (fileData.getFile().getContext()
                                        .equals(context)) {
                                    return fileItem;
                                }
                            }
                        } else {
                            return found;
                        }
                    }
                }
            }
            if (bestMatch != null) {
                return bestMatch;
            }
        }
        return bestMatch;
    }

    /**
     * @see #find(TreeItem, LocalizationContext, java.nio.file.Path, boolean)
     * @param item
     * @param ctx
     * @param path
     * @param populateToFind
     * @return null if no item found
     */
    private TreeItem find(TreeItem item, LocalizationContext ctx, String path,
            boolean populateToFind) {
        return find(item, ctx, Paths.get(path), populateToFind);
    }

    /**
     * Recursively search tree for node matching path starting at item
     * 
     * @param item
     * @param ctx
     * @param path
     * @param populateToFind
     * @return null if no item found
     */
    private TreeItem find(TreeItem item, LocalizationContext ctx,
            java.nio.file.Path path, boolean populateToFind) {
        FileTreeEntryData data = (FileTreeEntryData) item.getData();
        if (data.getPathData().getTypes().contains(ctx.getLocalizationType())) {
            java.nio.file.Path itemPath = Paths.get(data.getPath());
            if (path.startsWith(itemPath) || itemPath.getNameCount() == 0) {
                if (path.equals(itemPath)
                        || (!data.hasRequestedChildren() && !populateToFind)) {
                    return item;
                } else {
                    if (!data.hasRequestedChildren()) {
                        populateNode(item);
                    }
                    for (TreeItem child : item.getItems()) {
                        TreeItem rval = find(child, ctx, path, populateToFind);
                        if (rval != null) {
                            return rval;
                        }
                    }
                }
                return item;
            }
        }
        return null;
    }

    /**
     * Select the TreeItem corresponding to the open/active editor.
     * 
     * @param partRef
     *            The IWorkbenchPartReference
     */
    private void selectItem(IEditorReference partRef) {
        IEditorInput input = null;
        try {
            input = partRef.getEditorInput();
        } catch (PartInitException e) {
            statusHandler.handle(Priority.PROBLEM,
                    "Error activating editor: " + e.getLocalizedMessage(), e);
        }
        if ((input instanceof LocalizationEditorInput) && linkWithEditor) {
            selectFile(((LocalizationEditorInput) input).getLocalizationFile());
        }
    }

    /**
     * Set the cursor for waiting
     */
    private void setWaiting() {
        getSite().getShell().setCursor(waitCursor);
    }

    /**
     * Sets the cursor back from waiting
     */
    private void setDoneWaiting() {
        getSite().getShell().setCursor(null);
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        for (IEditorReference ref : getSite().getPage().getEditorReferences()) {
            IEditorPart part = ref.getEditor(false);
            if (part != null) {
                IEditorInput editorInput = part.getEditorInput();
                if (editorInput instanceof LocalizationEditorInput) {
                    LocalizationEditorInput input = (LocalizationEditorInput) editorInput;
                    IFile inputFile = input.getFile();
                    IResourceDelta rootDelta = event.getDelta();
                    IResourceDelta docDelta = rootDelta
                            .findMember(inputFile.getFullPath());
                    if ((docDelta != null)
                            && (docDelta.getKind() == IResourceDelta.CHANGED)
                            && ((docDelta.getFlags()
                                    & IResourceDelta.CONTENT) == IResourceDelta.CONTENT)) {
                        LocalizationFile file = input.getLocalizationFile();
                        if (!file.getContext().getLocalizationLevel()
                                .isSystemLevel()) {
                            try {
                                boolean noChange = false;
                                try {
                                    // Check if local changes differ from server
                                    String localChecksum = Checksum
                                            .getMD5Checksum(
                                                    file.getFile(false));
                                    IPathManager pm = PathManagerFactory
                                            .getPathManager();
                                    String remoteChecksum = pm
                                            .getLocalizationFile(
                                                    file.getContext(),
                                                    file.getPath())
                                            .getCheckSum();
                                    noChange = localChecksum
                                            .equals(remoteChecksum);
                                } catch (IOException e) {
                                    statusHandler.handle(Priority.PROBLEM,
                                            "Error calculating checksum of local file: "
                                                    + e.getLocalizedMessage(),
                                            e);
                                }
                                /*
                                 * No need to save if local file matches server
                                 * file (prevents incorrect file version
                                 * conflict when we are simply re-syncing with
                                 * the local file system)
                                 * 
                                 * TODO can't easily replace the deprecated
                                 * save() call because if we use
                                 * openOutputStream() it will overwrite the file
                                 * contents
                                 */
                                if (noChange || file.save()) {
                                    input.refreshLocalizationFile();
                                }
                            } catch (LocalizationException e) {
                                statusHandler
                                        .handle(Priority.ERROR,
                                                "Error saving file: "
                                                        + e.getLocalizedMessage(),
                                                e);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void fileUpdated(FileUpdatedMessage message) {
        if (getTree().isDisposed()) {
            // Make sure we are not disposed
            return;
        }

        LocalizationContext context = message.getContext();

        Set<String> contexts = contextMap.get(context.getLocalizationLevel());
        if (contexts != null) {
            contexts.add(context.getContextName());
        }

        String filePath = message.getFileName();
        IPathManager pathManager = PathManagerFactory.getPathManager();

        FileChangeType type = message.getChangeType();
        ILocalizationFile file = pathManager.getLocalizationFile(context,
                filePath);

        if (file != null) {
            if ((!file.exists() && ((type == FileChangeType.ADDED)
                    || (type == FileChangeType.UPDATED)))
                    || (file.exists() && (type == FileChangeType.DELETED))) {
                statusHandler.debug("Got weird state in update for " + file
                        + ": exists=" + file.exists() + ", changeType="
                        + message.getChangeType());
            }

            VizApp.runAsync(new FileUpdateRefresher(file, type));
        }
    }

    @Override
    public void setFocus() {
        getTree().setFocus();
    }

    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
        if (partRef instanceof IEditorReference) {
            IEditorReference ref = (IEditorReference) partRef;

            selectItem(ref);

            // Check if compare/merge editors are in sync with file system
            IEditorPart part = ref.getEditor(false);
            if (part != null) {
                IEditorInput editorInput = part.getEditorInput();
                if (editorInput instanceof LocalizationCompareEditorInput) {
                    ((LocalizationCompareEditorInput) editorInput)
                            .handlePartActivated();
                } else if (editorInput instanceof LocalizationMergeEditorInput) {
                    ((LocalizationMergeEditorInput) editorInput)
                            .handlePartActivated();
                }
            }
        }
    }

    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
        if (partRef instanceof IEditorReference) {
            selectItem((IEditorReference) partRef);
        }
    }

    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
        if (partRef instanceof IEditorReference) {
            selectItem((IEditorReference) partRef);
        }
    }

    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
        //
    }

    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
        //
    }

    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
        //
    }

    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
        //
    }

    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
        //
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Object getAdapter(Class adapter) {
        if (adapter.equals(ILocalizationService.class)) {
            return this;
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void selectFile(LocalizationFile file) {
        TreeItem item = find(file, true, false);
        if (item != null) {
            getTree().setSelection(item);
        }
    }

    @Override
    public void refresh(LocalizationFile file) {
        if (file != null) {
            TreeItem item = find(file, false, false);
            if (item != null) {
                refresh(item);
            }
        }
    }

    @Override
    public void openFile(LocalizationFile file) {
        boolean fileOpened = false;
        IWorkbenchPage page = this.getSite().getPage();
        // Attempt to find file, populating tree as you search and returning the
        // nearest parent item
        TreeItem item = find(file, true, true);
        if (item != null) {
            // An item was found, if it is not an entry for this file, refresh
            // the item and search again
            FileTreeEntryData data = (FileTreeEntryData) item.getData();
            if (!(data instanceof LocalizationFileEntryData)) {
                refresh(item);
                item = find(file, true, true);
            }
            // Check for entry for this file in item, if is entry, open file
            if (item.getData() instanceof LocalizationFileEntryData) {
                LocalizationFileEntryData fileData = (LocalizationFileEntryData) item
                        .getData();
                LocalizationEditorUtils.openInEditor(page,
                        new LocalizationEditorInput(file,
                                fileData.getResource()));
                fileOpened = true;
            }
        }
        if (!fileOpened) {
            // File was not opened, send status
            VizException e = new VizException(
                    "Unable to find " + file + " in view to open");
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
        }
    }

    @Override
    public void activateEditor(IEditorPart editor) {
        selectItem(
                (IEditorReference) (getSite().getPage().getReference(editor)));
    }

    private void toggleSet(Set<LocalizationLevel> set,
            LocalizationLevel level) {
        if (set.contains(level)) {
            set.remove(level);
        } else {
            set.add(level);
        }
        repopulateTree(getTree());
    }

    /**
     * Toggle Show All for a level
     * 
     * @param level
     */
    public void toggleShowAllLevel(LocalizationLevel level) {
        toggleSet(showAllSet, level);
    }

    /**
     * Toggle Show for a level
     * 
     * @param level
     */
    public void toggleShowLevel(LocalizationLevel level) {
        toggleSet(showSet, level);
    }

    /**
     * Check if level is showing all values not just current
     * 
     * (e.g. Show all users, not just my user)
     * 
     * @param level
     * @return true if showing all values
     */
    public boolean isAllShown(LocalizationLevel level) {
        return showAllSet.contains(level);
    }

    /**
     * Check if level is shown
     * 
     * @param level
     * @return true if shown
     */
    public boolean isShown(LocalizationLevel level) {
        return showSet.contains(level);
    }

    @Override
    public void fileChanged(ILocalizationFile file) {
        /*
         * TODO rewrite this whole method to not use fileUpdated() and correctly
         * refresh/update nodes as necessary
         */
        FileChangeType t = null;
        if (ILocalizationFile.NON_EXISTENT_CHECKSUM
                .equals(file.getCheckSum())) {
            t = FileChangeType.DELETED;
        } else {
            /*
             * TODO We don't have the previous checksum available here so we
             * can't easily identify this change as an add vs update. We need to
             * handle this cleanly somehow.
             * 
             * Temporary fix: Go with ADDED
             */
            t = FileChangeType.ADDED;
        }

        FileUpdatedMessage fum = new FileUpdatedMessage(file.getContext(),
                file.getPath(), t, file.getTimeStamp().getTime(),
                file.getCheckSum());
        fileUpdated(fum);
    }

    @Override
    public void windowActivated(IWorkbenchWindow window) {
        if (window == getSite().getWorkbenchWindow()) {
            partActivated(getSite().getPage().getActivePartReference());
        }
    }

    @Override
    public void windowDeactivated(IWorkbenchWindow window) {
        // no-op
    }

    @Override
    public void windowClosed(IWorkbenchWindow window) {
        // no-op
    }

    @Override
    public void windowOpened(IWorkbenchWindow window) {
        // no-op
    }
}
