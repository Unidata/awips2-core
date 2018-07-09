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
package com.raytheon.viz.ui.perspectives;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPlaceholderResolver;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.common.localization.exception.LocalizationException;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.SizeUtil;
import com.raytheon.uf.viz.core.ContextManager;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.procedures.Procedure;
import com.raytheon.uf.viz.core.procedures.ProcedureXmlManager;
import com.raytheon.viz.ui.actions.LoadPerspectiveHandler;
import com.raytheon.viz.ui.color.BackgroundColor;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.tools.AbstractModalTool;
import com.raytheon.viz.ui.tools.ModalToolManager;

/**
 * Manager for generic perspectives. Default implementation for general GUI
 * interface management.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 22, 2008  1223     randerso  Initial creation
 * Mar 26, 2013  1799     bsteffen  Fix pan/zoom when in views.
 * Jun 19, 2013  2116     bsteffen  Do not deactivate contexts for parts when
 *                                  closing an inactive perspective.
 * Jan 14, 2014  2594     bclement  added low memory notification
 * Jun 05, 2015  4401     bkowal    Renamed LoadSerializedXml to
 *                                  LoadPerspectiveHandler.
 * Dec 14, 2015  5193     bsteffen  Updates to handle changed eclipse 4 listener
 *                                  calls.
 * Feb 09, 2016  5267     bsteffen  Workaround eclipse 4 poor support for non
 *                                  restorable views.
 * Feb 10, 2016  5329     bsteffen  Close saved editors when deactivating while
 *                                  closing.
 * Jul 11, 2016  5751     bsteffen  Fix timing of tool activation when a part is
 *                                  opened.
 * Sep 01, 2016  5854     bsteffen  Fix closing saved editor in hidden
 *                                  perspective when workbench is closing.
 * Oct 25, 2016  5929     bsteffen  Ensure compatibility layer listeners fire
 *                                  when editors swap
 * Nov 23, 2016  6004     bsteffen  Move handling of nonrestorable views out of
 *                                  this class.
 * Mar 02, 2017  6162     bsteffen  activate/deactivate tools when changing
 *                                  perspectives.
 * Jul 09, 2018  7315     bsteffen  Keep hidden editors in the model so they can
 *                                  prompt to save on close.
 * 
 * </pre>
 * 
 * @author randerso
 */
public abstract class AbstractVizPerspectiveManager
        implements IBackgroundColorChangedListener {
    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(AbstractVizPerspectiveManager.class);

    private static class PerspectivePageListener implements IPageListener {

        @Override
        public void pageActivated(IWorkbenchPage page) {
        }

        @Override
        public void pageClosed(IWorkbenchPage page) {
            page.removePartListener(partListener);
        }

        @Override
        public void pageOpened(IWorkbenchPage page) {
            page.addPartListener(partListener);
        }

    }

    private static class PerspectivePartListener implements IPartListener {

        @Override
        public void partActivated(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                if (mgr != null) {
                    for (AbstractModalTool tool : mgr.getToolManager()
                            .getSelectedModalTools()) {
                        if (tool != null && tool.getCurrentEditor() != part) {
                            tool.deactivate();
                            tool.setEditor((IDisplayPaneContainer) part);
                            tool.activate();
                        }
                    }
                }

            }
        }

        @Override
        public void partBroughtToTop(IWorkbenchPart part) {
            partActivated(part);
        }

        @Override
        public void partClosed(IWorkbenchPart part) {
        }

        @Override
        public void partDeactivated(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                IWorkbenchPart newPart = part.getSite().getPage()
                        .getActivePart();
                if (newPart instanceof IEditorPart) {
                    if (mgr != null) {
                        for (AbstractModalTool tool : mgr.getToolManager()
                                .getSelectedModalTools()) {
                            if (tool.getCurrentEditor() == part) {
                                tool.deactivate();
                            }
                        }
                    }
                }
            }
        }

        @Override
        public void partOpened(IWorkbenchPart part) {
            // update editor on last selected modal tool
            if (part instanceof IEditorPart
                    && part instanceof IDisplayPaneContainer) {
                final AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                        .getCurrentPerspectiveManager();
                if (mgr != null && !mgr.opened && mgr.getToolManager()
                        .getSelectedModalTools().isEmpty()) {
                    final AbstractEditor editor = (AbstractEditor) part;
                    /*
                     * Need to delay activation so that other part listeners
                     * have time to run and activate part specific contexts
                     * before activating tools. Otherwise tools may not be valid
                     * because the context is not active.
                     */
                    VizApp.runAsync(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                mgr.activateDefaultTool(
                                        editor.getDefaultTool());
                                if (mgr.getToolManager().getSelectedModalTools()
                                        .isEmpty()) {
                                    /*
                                     * Hack due to tool activation not sending
                                     * whether it should be activated or
                                     * deactivated and is just toggling instead.
                                     * TODO: Make AbstractModalTool required
                                     * command parameter for activate or
                                     * deactivate
                                     */
                                    mgr.activateDefaultTool(
                                            editor.getDefaultTool());
                                }
                            } catch (VizException e) {
                                statusHandler.handle(Priority.SIGNIFICANT,
                                        "Error activating tool set", e);
                            }

                        }
                    });
                }
            }
        }
    }

    public static final IPartListener partListener = new PerspectivePartListener();

    public static final IPageListener pageListener = new PerspectivePageListener();

    /** The window the perspective is loaded to */
    protected IWorkbenchWindow perspectiveWindow;

    protected IWorkbenchPage page;

    private IEditorPart activeEditor;

    /** Has the perspective been opened */
    protected boolean opened = false;

    /** StatusLineManager for the window */
    protected IStatusLineManager statusLine;

    /** Perspective id the manager is managing */
    protected String perspectiveId;

    /** The tool manager for the perspective */
    protected ModalToolManager toolManager;

    /** True if the editors should be saved when switching perspectives */
    protected boolean saveEditors = false;

    /** List of perspective dialogs */
    protected List<IPerspectiveSpecificDialog> perspectiveDialogs;

    private List<IContributionItem> items = new ArrayList<>();

    private BackgroundColor backgroundColor;

    private String title;

    /**
     * Remove the perspective editor area from a perspective on close. This must
     * be done here instead of the close() method because close() is not called
     * when the application is exiting.
     */
    private final EventHandler closeHandler = new EventHandler() {

        @Override
        public void handleEvent(Event event) {
            if (event.getProperty(UIEvents.EventTags.NEW_VALUE) != null) {
                return;
            }
            Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
            if (!(element instanceof MPerspective)) {
                return;
            }
            MPerspective perspective = (MPerspective) element;
            if (!perspective.getElementId().equals(perspectiveId)) {
                return;
            }
            EModelService modelService = perspectiveWindow
                    .getService(EModelService.class);
            List<MArea> localEditorAreas = modelService.findElements(
                    perspective, getPerpectiveEditorAreaId(), MArea.class,
                    null);
            if (localEditorAreas != null) {
                for (MArea localEditorArea : localEditorAreas) {
                    localEditorArea.setToBeRendered(false);
                    MElementContainer<MUIElement> parent = localEditorArea
                            .getParent();
                    if (parent != null) {
                        parent.getChildren().remove(localEditorArea);
                    }
                }
            }
        }
    };

    public AbstractVizPerspectiveManager() {
        // new up a tool manager for the perspective
        toolManager = new ModalToolManager();
        perspectiveDialogs = new CopyOnWriteArrayList<>();
    }

    /**
     * Override this method to have items automatically added/removed from the
     * status line on activation/deactivation. This method will be called when
     * the perspective is opened. Items will be disposed and removed when closed
     * and just removed when deactivated. Always return new items from this
     * method as they will be automatically disposed
     * 
     * @return
     */
    protected List<ContributionItem> getStatusLineItems() {
        return new ArrayList<>();
    }

    /**
     * This is called when the perspective is opened from scratch
     */
    protected abstract void open();

    public void close() {
        if (opened) {
            opened = false;

            closeDialogs();
            deactivateContexts();
            removeFromStatusLine();

            if (backgroundColor != null) {
                backgroundColor.removeListener(BGColorMode.GLOBAL, this);
            }
            perspectiveWindow.getService(IEventBroker.class)
                    .unsubscribe(closeHandler);
        }
    }

    /**
     * Activate the perspective. This will call open if the perspective has not
     * been activated yet or has been closed, otherwise restores saved editors
     */
    public void activate() {
        if (perspectiveWindow.getActivePage() == null) {
            // don't attempt to load until there is an active page
            return;
        }

        page = perspectiveWindow.getActivePage();

        showEditors();

        if (!opened) {
            backgroundColor = BackgroundColor
                    .getInstance(page.getPerspective());
            backgroundColor.addListener(BGColorMode.GLOBAL, this);
            open();
            perspectiveWindow.getService(IEventBroker.class)
                    .subscribe(UIEvents.UIElement.TOPIC_WIDGET, closeHandler);
            opened = true;
        } else {
            activateInternal();
        }
        activateContexts();
        for (AbstractModalTool tool : toolManager.getSelectedModalTools()) {
            tool.activate();
        }
        contributeToStatusLine();

        perspectiveWindow.getShell().setText(getTitle(title));
    }

    /**
     * Create an editor stack for the compatibility layer, based off of code in
     * org.eclipse.e4.ui.internal.workbench.PlaceholderResolver and
     * org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout.
     */
    private MPartStack createDefaultEditorStack() {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        MPartStack editorStack = modelService
                .createModelElement(MPartStack.class);
        editorStack.getTags().add("org.eclipse.e4.primaryDataStack");
        editorStack.getTags().add("EditorStack");
        editorStack.setElementId("org.eclipse.e4.primaryDataStack");
        return editorStack;
    }

    /**
     * Hide editors when a perspective is deactivated. This will move anything
     * in the shared editor area into a perspective specific area so that it is
     * not visible in other perspectives but it will still be part of the model.
     * The shared editor area will be populated with a default empty part stack.
     * 
     */
    private void hideEditors() {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);

        MArea localEditorArea = modelService.createModelElement(MArea.class);
        localEditorArea.setElementId(getPerpectiveEditorAreaId());

        MPlaceholder editorPlaceholder = findEditorPlaceholder();

        editorPlaceholder.getParent().getChildren().add(localEditorArea);

        MArea editorArea = (MArea) editorPlaceholder.getRef();
        moveAllChildren(editorArea, localEditorArea);
        editorArea.getChildren().add(createDefaultEditorStack());

        editorPlaceholder.setToBeRendered(false);
        editorPlaceholder.setRef(null);

        /*
         * Changing the id hides it from IWorkbenchPage.getEditorReferences().
         */
        @SuppressWarnings("restriction")
        String oldId = CompatibilityEditor.MODEL_ELEMENT_ID;
        String newId = oldId + ".hidden";
        List<MPart> parts = modelService.findElements(localEditorArea, oldId,
                MPart.class, null);
        parts.forEach((part) -> part.setElementId(newId));
    }

    /**
     * Show editors that were previously hidden by {@link #hideEditors()}. This
     * discards the current contents of the shared editor area, which should be
     * fine as long as other perspectives are also clearing out the editor area
     * when deactivating.
     */
    private void showEditors() {
        MPlaceholder editorPlaceholder = findEditorPlaceholder();
        MArea editorArea = (MArea) editorPlaceholder.getRef();
        if (editorArea == null) {
            /*
             * The placeholder resolver will grab the editor area from the
             * shared elements in the window and set it in the placeholder
             * correctly.
             */
            MWindow window = perspectiveWindow.getService(MWindow.class);
            EPlaceholderResolver resolver = perspectiveWindow
                    .getService(EPlaceholderResolver.class);
            resolver.resolvePlaceholderRef(editorPlaceholder, window);
            editorArea = (MArea) editorPlaceholder.getRef();
        }
        editorPlaceholder.setToBeRendered(true);

        List<MPartSashContainerElement> editorChildren = editorArea
                .getChildren();
        editorChildren.forEach((child) -> child.setToBeRendered(false));
        editorChildren.clear();

        if (editorArea.getWidget() == null) {
            /*
             * Intermittent problems occur if the editorArea doesn't have a GUI
             * after this, so force it.
             */
            perspectiveWindow.getService(IPresentationEngine.class)
                    .createGui(editorArea);
        }

        MPerspective perspective = getPerspectiveModel();
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        List<MArea> localEditorAreas = modelService.findElements(perspective,
                getPerpectiveEditorAreaId(), MArea.class, null);
        if (localEditorAreas != null && !opened) {
            /* Remove old local editors for a fresh perspective. */
            for (MArea area : localEditorAreas) {
                area.setToBeRendered(false);
                area.getParent().getChildren().remove(area);
            }
            localEditorAreas = null;
        }
        if (localEditorAreas == null || localEditorAreas.isEmpty()) {
            MPartSashContainerElement defaultStack = createDefaultEditorStack();
            editorChildren.add(defaultStack);
            /*
             * Visibility events for new editors are not firing correctly if the
             * default stack gui isn't created before the new editors.
             */
            perspectiveWindow.getService(IPresentationEngine.class)
                    .createGui(defaultStack);
            if (defaultStack.getWidget() == null) {
                perspectiveWindow.getService(IPresentationEngine.class)
                        .createGui(defaultStack);
            }
        } else {
            MArea localEditorArea = localEditorAreas.get(0);
            moveAllChildren(localEditorArea, editorArea);
            localEditorArea.setToBeRendered(false);
            localEditorArea.getParent().getChildren().remove(localEditorArea);
            @SuppressWarnings("restriction")
            String newId = CompatibilityEditor.MODEL_ELEMENT_ID;
            String oldId = newId + ".hidden";
            List<MPart> parts = modelService.findElements(localEditorArea,
                    oldId, MPart.class, null);
            parts.forEach((part) -> part.setElementId(newId));
        }
    }

    /**
     * @return the unique id to use for the area that holds the editors when a
     *         perspective is deactivated.
     * @see #hideEditors()
     */
    private String getPerpectiveEditorAreaId() {
        return perspectiveId + ".editors";
    }

    /**
     * @return the editor placeholder within the current perspective.
     * @throws IllegalStateException
     *             if there is no editor placeholder
     */
    private MPlaceholder findEditorPlaceholder() throws IllegalStateException {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        MPerspective perspective = getPerspectiveModel();
        List<MPlaceholder> placeholders = modelService.findElements(perspective,
                IPageLayout.ID_EDITOR_AREA, MPlaceholder.class, null);
        if (placeholders == null || placeholders.isEmpty()) {
            throw new IllegalStateException(
                    "No MPlaceholder for editors found in "
                            + perspective.getElementId());

        }
        return placeholders.get(0);
    }

    /**
     * @return the perspective model for this perspective.
     * @throws IllegalStateException
     *             if the perspective is not found
     */
    private MPerspective getPerspectiveModel() throws IllegalStateException {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        MWindow window = perspectiveWindow.getService(MWindow.class);
        List<MPerspective> perspectives = modelService.findElements(window,
                perspectiveId, MPerspective.class, null);
        if (perspectives == null || perspectives.isEmpty()) {
            throw new IllegalStateException(
                    "No MPerspective found for " + perspectiveId);
        }
        return perspectives.get(0);
    }

    /**
     * Move all the children from one MArea to another.
     * 
     * @param source
     *            one MArea
     * @param dest
     *            another MArea
     */
    private void moveAllChildren(MArea source, MArea dest) {
        EModelService modelService = perspectiveWindow
                .getService(EModelService.class);
        List<MPartSashContainerElement> sourceChildren = source.getChildren();
        /*
         * copy the list to avoid concurrent modification as children are
         * removed.
         */
        sourceChildren = new ArrayList<>(sourceChildren);
        sourceChildren.forEach((child) -> modelService.move(child, dest));
    }

    /**
     * Overridable set title method, takes original window title and
     * perspectives can display what they want. Default implementation sets
     * title to be "title - perspective"
     * 
     * @param title
     */
    protected String getTitle(String title) {
        return title + " - " + getLabel();
    }

    /**
     * Get the label for the perspective
     * 
     * @return
     */
    protected final String getLabel() {
        return page.getPerspective().getLabel();
    }

    protected void activateInternal() {
        if (activeEditor != null) {
            page.activate(activeEditor);
            activeEditor = null;
        }

        // Activate any perspective dialogs
        activateDialogs();
    }

    /**
     * Deactivate the perspective, stores editors to be opened again
     */
    public void deactivate() {

        activeEditor = page.getActiveEditor();

        hideEditors();

        deactivateDialogs();
        for (AbstractModalTool tool : toolManager.getSelectedModalTools()) {
            tool.deactivate();
        }
        deactivateContexts();
        removeFromStatusLine();
    }

    /**
     * Can be overridden to allow perspectives to override the editor's default
     * tool to have a default perspective tool
     * 
     * @param tool
     * @throws VizException
     */
    protected void activateDefaultTool(String tool) throws VizException {
        toolManager.activateToolSet(tool);
    }

    /**
     * WorkbenchWindow setter, should be called immediately after construction.
     * not passed in through constructor bc class will be instantiated through
     * eclipse extension point
     * 
     * @param window
     */
    public void setPerspectiveWindow(IWorkbenchWindow window) {
        this.perspectiveWindow = window;
        this.title = window.getShell().getText();
    }

    /**
     * Set the status line manager so the perspective can add to the status line
     * 
     * @param statusLine
     */
    public void setStatusLineManager(IStatusLineManager statusLine) {
        this.statusLine = statusLine;
    }

    /**
     * Set the perspective id registered with the manager in the plugin.xml
     * 
     * @param perspectiveId
     */
    void setPerspectiveId(String perspectiveId) {
        this.perspectiveId = perspectiveId;
    }

    public ModalToolManager getToolManager() {
        return toolManager;
    }

    /**
     * Add a perspective dialog
     * 
     * @param dialog
     */
    public void addPerspectiveDialog(IPerspectiveSpecificDialog dialog) {
        if (!perspectiveDialogs.contains(dialog)) {
            perspectiveDialogs.add(dialog);
        }
    }

    /**
     * Remove a perspective dialog
     * 
     * @param dialog
     */
    public void removePespectiveDialog(IPerspectiveSpecificDialog dialog) {
        perspectiveDialogs.remove(dialog);
    }

    private void activateDialogs() {
        for (IPerspectiveSpecificDialog dialog : perspectiveDialogs) {
            dialog.restore(true);
        }
    }

    private void deactivateDialogs() {
        for (IPerspectiveSpecificDialog dialog : perspectiveDialogs) {
            dialog.hide(true);
        }
    }

    private void closeDialogs() {
        List<IPerspectiveSpecificDialog> dialogsToClose = new ArrayList<>();
        dialogsToClose.addAll(perspectiveDialogs);
        perspectiveDialogs.clear();
        for (IPerspectiveSpecificDialog dialog : dialogsToClose) {
            dialog.close();
        }
    }

    protected static void loadDefaultBundle(String filePath) {
        LocalizationFile defaultBundle = PathManagerFactory.getPathManager()
                .getStaticLocalizationFile(filePath);
        try (InputStream is = defaultBundle.openInputStream()) {
            Procedure proc = (Procedure) ProcedureXmlManager.getInstance()
                    .unmarshal(is);
            LoadPerspectiveHandler.loadProcedureToScreen(proc, true);
        } catch (VizException | IOException | LocalizationException
                | SerializationException e) {
            statusHandler.handle(Priority.CRITICAL,
                    "Error activating perspective", e);
            return;
        }
    }

    public final void activateContexts() {
        activateContexts(ContextManager.getInstance(perspectiveWindow));
    }

    protected void activateContexts(ContextManager manager) {
        manager.activateContexts(this);
        if (page != null) {
            manager.activateContexts(page.getActivePart());
        }
    }

    public final void deactivateContexts() {
        deactivateContexts(ContextManager.getInstance(perspectiveWindow));
    }

    protected void deactivateContexts(ContextManager manager) {
        manager.deactivateContexts(this);
        if (page != null && page.getActivePart() != null
                && perspectiveId.equals(page.getPerspective().getId())) {
            manager.deactivateContexts(page.getActivePart());
        }
    }

    /**
     * Get an array of the editors in the perspective. Note: These editors may
     * not be visible and this perspective may not be the active perspective.
     * 
     * @return Array of availble editors for the perspective
     */
    public AbstractEditor[] getPerspectiveEditors() {
        List<AbstractEditor> editors = new ArrayList<>();
        if (page != null) {
            for (IEditorReference ref : page.getEditorReferences()) {
                IEditorPart part = ref.getEditor(false);
                if (part instanceof AbstractEditor) {
                    editors.add((AbstractEditor) part);
                }
            }
        }
        return editors.toArray(new AbstractEditor[editors.size()]);
    }

    /**
     * Get the perspective id the manager manages
     * 
     * @return
     */
    public String getPerspectiveId() {
        return perspectiveId;
    }

    /**
     * Have the perspecitve manager open a new editor for the perspective
     * 
     * @return the new editor or null if no editor was opened
     */
    public AbstractEditor openNewEditor() {
        // default does nothing
        return null;
    }

    private void contributeToStatusLine() {
        items.addAll(getStatusLineItems());
        for (int i = items.size() - 1; i >= 0; --i) {
            IContributionItem item = items.get(i);
            item.setVisible(true);
            statusLine.appendToGroup(StatusLineManager.MIDDLE_GROUP, item);
        }
        statusLine.update(true);
        // relayout the shell since we added the widget
        perspectiveWindow.getShell().layout(true, true);
    }

    private void removeFromStatusLine() {
        for (IContributionItem item : items) {
            statusLine.remove(item);
            item.dispose();
        }
        statusLine.update(true);
        // relayout the shell since we added the widget
        perspectiveWindow.getShell().layout(true, true);
        items.clear();
    }

    /**
     * Adds perspective specific context menu items to the specified
     * IMenuManager on the IDisplayPaneContainer for the IDisplayPane
     * 
     * @param menuManager
     * @param container
     * @param pane
     */
    public void addContextMenuItems(IMenuManager menuManager,
            IDisplayPaneContainer container, IDisplayPane pane) {
        // do nothing default implementation
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        for (AbstractEditor editor : getPerspectiveEditors()) {
            editor.setColor(mode, newColor);
        }
    }

    /**
     * Notify perspective manager when heap space is running low. Default action
     * is to pop up a warning to the user. Perspectives can override the default
     * behavior to take more extreme actions to reduce memory usage.
     * 
     * @param freeMemory
     *            free memory available in bytes
     * @return true if notification was displayed
     */
    public boolean notifyLowMemory(long availMemory) {
        final String msg = getLowMemoryMessage(availMemory);
        final boolean[] status = new boolean[1];
        VizApp.runSync(new Runnable() {
            @Override
            public void run() {
                Display display = Display.getDefault();
                status[0] = MessageDialog.open(MessageDialog.WARNING,
                        display.getActiveShell(), "Low Memory", msg, SWT.NONE);
            }
        });
        return status[0];
    }

    /**
     * Create the default low memory message to be displayed to the user
     * 
     * @param availMemory
     *            free memory available in bytes
     * @return
     */
    protected String getLowMemoryMessage(long availMemory) {
        return "This CAVE is nearing its maximum memory limit. "
                + "Performance may degrade significantly. "
                + SizeUtil.prettyByteSize(availMemory) + " available";
    }

}
