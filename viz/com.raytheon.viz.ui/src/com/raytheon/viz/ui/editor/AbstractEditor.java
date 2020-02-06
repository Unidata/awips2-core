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
package com.raytheon.viz.ui.editor;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;

import com.raytheon.uf.viz.core.AbstractTimeMatcher;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener.DisplayChangeType;
import com.raytheon.uf.viz.core.VizApp;
import com.raytheon.uf.viz.core.datastructure.LoopProperties;
import com.raytheon.uf.viz.core.drawables.AbstractRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.IInputHandler;
import com.raytheon.uf.viz.core.rsc.IInputHandler.InputPriority;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceProperties;
import com.raytheon.viz.ui.IRenameablePart;
import com.raytheon.viz.ui.color.BackgroundColor;
import com.raytheon.viz.ui.color.IBackgroundColorChangedListener;
import com.raytheon.viz.ui.input.InputManager;
import com.raytheon.viz.ui.panes.PaneManager;
import com.raytheon.viz.ui.perspectives.AbstractCAVEPerspectiveManager;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;
import org.locationtech.jts.geom.Coordinate;

/**
 * Provides the basis for editors in viz
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Oct 10, 2006           chammack   Initial Creation.
 * Jun 12, 2014  3264     bsteffen   Make listeners thread safe.
 * Mar 02, 2015  4204     njensen    Deprecated setTabTitle and overrode
 *                                   setPartName
 * Jan 13, 2016  18480    dfriedman  Synchronize time matching before
 *                                   initializing displays.
 * Jun 24, 2016  5708     bsteffen   Notify listeners when dirty status changes.
 * Nov 29, 2016  6014     bsteffen   Update dirty state on UI thread.
 * Jul 09, 2018  7315     bsteffen   Ensure close confirmation prompt uses a
 *                                   visible shell.
 * 
 * </pre>
 * 
 * @author chammack
 */
public abstract class AbstractEditor extends EditorPart
        implements IDisplayPaneContainer, IBackgroundColorChangedListener,
        ISaveablePart2, IRenameablePart {

    /** The set of those listening for IRenderableDisplay changes */
    private final Set<IRenderableDisplayChangedListener> renderableDisplayListeners;

    /** The editor input for the editor */
    protected EditorInput editorInput;

    /** The renderable displays to load when constructed */
    protected IRenderableDisplay[] displaysToLoad;

    /**
     * If not null will prevent user from closing the editor and display this
     * Message
     */
    private String closeMessage = null;

    protected BackgroundColor backgroundColor;

    /**
     * Constructor
     */
    public AbstractEditor() {
        renderableDisplayListeners = new CopyOnWriteArraySet<>();
        renderableDisplayListeners.add(new DirtyListener());
    }

    private IRenderableDisplay[] getRenderableDisplays() {
        IRenderableDisplay[] displays = new IRenderableDisplay[getDisplayPanes().length];
        int i = 0;
        for (IDisplayPane pane : getDisplayPanes()) {
            displays[i] = pane.getRenderableDisplay();
            i += 1;
        }
        return displays;
    }

    @Override
    public IDisplayPane[] getDisplayPanes() {
        return editorInput.getPaneManager().getDisplayPanes();
    }

    @Override
    public void refresh() {
        editorInput.getPaneManager().refresh();
    }

    @Override
    public IDisplayPane getActiveDisplayPane() {
        return editorInput.getPaneManager().getActiveDisplayPane();
    }

    @Override
    public Coordinate translateClick(double x, double y) {
        return editorInput.getPaneManager().translateClick(x, y);
    }

    @Override
    public double[] translateInverseClick(Coordinate c) {
        return editorInput.getPaneManager().translateInverseClick(c);
    }

    /**
     * Validates the editor input on init, default implementation checks to make
     * sure renderable displays are not null
     * 
     * @param input
     *            the input for the editor
     * @throws PartInitException
     *             on unexpected or invalid input
     */
    protected void validateEditorInput(EditorInput input)
            throws PartInitException {
        boolean valid = false;
        if (input.getRenderableDisplays() != null) {
            valid = true;
            for (IRenderableDisplay display : input.getRenderableDisplays()) {
                if (display == null) {
                    valid = false;
                }
            }
        }

        if (!valid) {
            throw new PartInitException(
                    "Renderable displays for input must not be null");
        }
    }

    @Override
    public void dispose() {
        if (backgroundColor != null) {
            backgroundColor.removeListener(BGColorMode.EDITOR, this);
        }
        super.dispose();
    }

    @Override
    public void init(IEditorSite site, IEditorInput input)
            throws PartInitException {
        if (input != null) {
            if (!(input instanceof EditorInput)) {
                throw new PartInitException("Input is of wrong type");
            }
        }

        backgroundColor = BackgroundColor
                .getInstance(site.getPage().getPerspective());
        backgroundColor.addListener(BGColorMode.EDITOR, this);

        editorInput = (EditorInput) input;
        validateEditorInput(editorInput);
        displaysToLoad = editorInput.getRenderableDisplays();

        setSite(site);
        setInput(input);

        initDisplays();

        PaneManager paneManager = editorInput.getPaneManager();
        if (paneManager == null) {
            editorInput.setPaneManager(getNewPaneManager());
        }

        addCustomHandlers(getMouseManager());
    }

    protected void initDisplays() {
        AbstractTimeMatcher timeMatcher = null;
        List<AbstractRenderableDisplay> displayList = null;
        if (displaysToLoad.length > 1) {
            for (IRenderableDisplay display : displaysToLoad) {
                if (display != null) {
                    timeMatcher = display.getDescriptor().getTimeMatcher();
                    if (timeMatcher != null) {
                        break;
                    }
                }
            }
            if (timeMatcher != null) {
                displayList = new ArrayList<>(displaysToLoad.length);
                for (IRenderableDisplay display : displaysToLoad) {
                    if (display instanceof AbstractRenderableDisplay) {
                        displayList.add((AbstractRenderableDisplay) display);
                    }
                }
            }
        }
        if (timeMatcher != null && displayList != null) {
            List<AbstractRenderableDisplay> orderedDisplays = timeMatcher
                    .getDisplayLoadOrder(displayList);
            for (AbstractRenderableDisplay display : orderedDisplays) {
                display.getDescriptor().synchronizeTimeMatching(
                        orderedDisplays.get(0).getDescriptor());
                initDisplay(display);
            }
        } else {
            for (IRenderableDisplay display : displaysToLoad) {
                if (display != null) {
                    initDisplay(display);
                }
            }
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        editorInput.getPaneManager().initializeComponents(this, parent);
        for (IRenderableDisplay display : displaysToLoad) {
            addPane(display);
        }

        contributePerspectiveActions();
    }

    /**
     * Contribute perspective specific actions
     * 
     * This should occur on startup and also when the perspective changes
     */
    protected void contributePerspectiveActions() {
        // Find the site of this container and it's
        // enclosing window
        IWorkbenchWindow window = getSite().getWorkbenchWindow();

        VizPerspectiveListener listener = VizPerspectiveListener
                .getInstance(window);
        if (listener != null) {
            AbstractVizPerspectiveManager manager = listener
                    .getActivePerspectiveManager();
            if (manager instanceof AbstractCAVEPerspectiveManager) {
                IInputHandler[] handlers = ((AbstractCAVEPerspectiveManager) manager)
                        .getPerspectiveInputHandlers(this);
                getMouseManager().firePerspectiveChanged(handlers);
            }
        }
    }

    public IDisplayPane addPane(IRenderableDisplay renderableDisplay) {
        return editorInput.getPaneManager().addPane(renderableDisplay);
    }

    /**
     * @param display
     */
    protected void initDisplay(IRenderableDisplay display) {
        display.setBackgroundColor(
                backgroundColor.getColor(BGColorMode.EDITOR));
        display.getDescriptor().getResourceList()
                .instantiateResources(display.getDescriptor(), true);
    }

    /**
     * Get the pane manager to use for this editor
     * 
     * @return
     */
    protected abstract PaneManager getNewPaneManager();

    /**
     * Add any custom mouse handlers in this function
     * 
     * @param manager
     */
    protected void addCustomHandlers(InputManager manager) {

    }

    @Override
    protected void setInput(IEditorInput input) {
        super.setInputWithNotify(input);
    }

    @Override
    public IEditorInput getEditorInput() {
        editorInput.setRenderableDisplays(getRenderableDisplays());
        return editorInput;
    }

    @Override
    public LoopProperties getLoopProperties() {
        return editorInput.getLoopProperties();
    }

    @Override
    public void setLoopProperties(LoopProperties loopProperties) {
        editorInput.setLoopProperties(loopProperties);
    }

    /**
     * Set the title of the tab
     * 
     * @deprecated Use setPartName(String) instead
     * 
     * @param title
     */
    @Deprecated
    public void setTabTitle(String title) {
        this.setPartName(title);
    }

    public InputManager getMouseManager() {
        return editorInput.getPaneManager().getMouseManager();
    }

    @Override
    public void registerMouseHandler(IInputHandler handler,
            InputPriority priority) {
        editorInput.getPaneManager().registerMouseHandler(handler, priority);
    }

    @Override
    public void registerMouseHandler(IInputHandler handler) {
        editorInput.getPaneManager().registerMouseHandler(handler);
    }

    @Override
    public void unregisterMouseHandler(IInputHandler handler) {
        editorInput.getPaneManager().unregisterMouseHandler(handler);
    }

    public BufferedImage screenshot() {
        return editorInput.getPaneManager().screenshot();
    }

    public String getDefaultTool() {
        return "com.raytheon.viz.ui.tools.nav.PanTool";
    }

    @Override
    public void setColor(BGColorMode mode, RGB newColor) {
        setColor(getDisplayPanes(), newColor);
    }

    protected void setColor(IDisplayPane[] panes, RGB newColor) {
        for (IDisplayPane pane : getDisplayPanes()) {
            IRenderableDisplay disp = pane.getRenderableDisplay();
            if (disp != null) {
                disp.setBackgroundColor(newColor);
            }
        }
        this.refresh();
    }

    /**
     * Use getSite().getService(MPart.class).setCloseable(false) instead because
     * that method will remove the close button from the UI.
     * 
     * @deprecated
     */
    @Deprecated
    public void disableClose(String reason) {
        closeMessage = reason;
        firePropertyChange(ISaveablePart2.PROP_DIRTY);
    }

    public boolean isCloseable() {
        return closeMessage == null;
    }

    @Override
    public int promptToSaveOnClose() {
        if (PlatformUI.getWorkbench().isClosing()) {
            return ISaveablePart2.NO;
        }

        Shell shell = getSite().getShell();
        if (!shell.isVisible()) {
            /* When the editor is hidden use the workbench shell. */
            shell = getSite().getWorkbenchWindow().getShell();
        }

        if (!isCloseable()) {
            // Let the user know why we refuse to close the editor
            MessageDialog.openError(shell, "Closing Disabled", closeMessage);
            // Cancel the clsoe
            return ISaveablePart2.CANCEL;
        } else {
            boolean close = MessageDialog.openQuestion(shell, "Close Editor?",
                    "Are you sure you want to close this editor?");
            return close ? ISaveablePart2.NO : ISaveablePart2.CANCEL;
        }
    }

    @Override
    public boolean isDirty() {
        if (!isCloseable()) {
            return true;
        } else {
            for (IDisplayPane pane : getDisplayPanes()) {
                IRenderableDisplay display = pane.getRenderableDisplay();
                if (display != null) {
                    if (display.isSwapping()) {
                        // never prompt on a swap
                        return false;
                    }
                    for (ResourcePair rp : display.getDescriptor()
                            .getResourceList()) {
                        ResourceProperties props = rp.getProperties();
                        if (!props.isSystemResource() && !props.isMapLayer()) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    @Override
    public void addRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener) {
        renderableDisplayListeners.add(displayChangedListener);
    }

    @Override
    public void removeRenderableDisplayChangedListener(
            IRenderableDisplayChangedListener displayChangedListener) {
        renderableDisplayListeners.remove(displayChangedListener);
    }

    @Override
    public void notifyRenderableDisplayChangedListeners(IDisplayPane pane,
            IRenderableDisplay display, DisplayChangeType type) {
        for (IRenderableDisplayChangedListener listener : renderableDisplayListeners) {
            listener.renderableDisplayChanged(pane, display, type);
        }
    }

    public BackgroundColor getBackgroundColor() {
        return backgroundColor;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {

    }

    @Override
    public void doSaveAs() {

    }

    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }

    @Override
    public void setFocus() {
        editorInput.getPaneManager().setFocus();
    }

    @Override
    public void setPartName(String partName) {
        super.setPartName(partName);
    }

    /**
     * Listen to changes in the display and the resources and fire a property
     * change event when resources change so that the eclipse platform can
     * accuratly track the editor dirty state.
     */
    private class DirtyListener implements IRenderableDisplayChangedListener,
            ResourceList.AddListener, ResourceList.RemoveListener {

        @Override
        public void renderableDisplayChanged(IDisplayPane pane,
                IRenderableDisplay newRenderableDisplay,
                DisplayChangeType type) {
            ResourceList resourceList = newRenderableDisplay.getDescriptor()
                    .getResourceList();
            switch (type) {
            case ADD:
                resourceList.addPostAddListener(this);
                resourceList.addPostRemoveListener(this);
                break;
            case REMOVE:
                resourceList.removePostAddListener(this);
                resourceList.removePostRemoveListener(this);
                break;
            }
            fireDirtyPropertyChange();
        }

        @Override
        public void notifyAdd(ResourcePair rp) throws VizException {
            fireDirtyPropertyChange();
        }

        @Override
        public void notifyRemove(ResourcePair rp) throws VizException {
            fireDirtyPropertyChange();
        }

        protected void fireDirtyPropertyChange() {
            VizApp.runAsync(new Runnable() {

                @Override
                public void run() {
                    firePropertyChange(ISaveablePart.PROP_DIRTY);
                }
            });
        }

    }
}
