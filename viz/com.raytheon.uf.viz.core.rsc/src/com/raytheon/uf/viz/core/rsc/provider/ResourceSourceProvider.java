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
package com.raytheon.uf.viz.core.rsc.provider;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.commands.Command;
import org.eclipse.ui.AbstractSourceProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.ISources;
import org.eclipse.ui.IWindowListener;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.IRenderableDisplayChangedListener;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceDataChanged;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.ResourceList.AddListener;
import com.raytheon.uf.viz.core.rsc.ResourceList.RemoveListener;
import com.raytheon.uf.viz.core.rsc.capabilities.AbstractCapability;

/**
 * Provides four variables to be used in the {@link Command} framework:
 * activePartResources, activePartCapabilities, activeEditorResources, and
 * activeEditorCapabilities.
 * 
 * Each of these variables provides a {@link Set} of either
 * {@link AbstractVizResource}s or {@link AbstractCapability}s that are
 * currently living on the active {@link IDisplayPaneContainer}
 * 
 * This object maintains a cached list of {@link IDisplayPaneContainer}s, each
 * mapped to a {@link ListenerContainer} which listens on various objects that
 * have the potential of resource or capability modification. The
 * {@link ListenerContainer}'s dispose method must be called on
 * {@link IDisplayPaneContainer} destruction to properly dispose of all
 * listeners
 * 
 * When a request for the state of a variable is made, the response will deliver
 * a {@link Set} of {@link Object}s that are mapped to the identity of the
 * active {@link IDisplayPaneContainer}. This set of Objects are assumed to be
 * used in plugin.xml expressions, namely "instanceOf"
 * 
 * plugin.xml examples:
 * 
 * 
 * 
 * 
 * <code>
    <visibleWhen checkEnabled="false">
        <or>
        <with variable="activeEditorCapabilities">
            <iterate operator="or">
                <instanceof value="<qualified-capability-class>" />
            </iterate>
        </with>
        <with variable="activeEditorResources">
            <iterate operator="or">
                <instanceof value="<qualified-resource-class>" />
            </iterate>
        </with>
        </or>
    </visibleWhen>
</code> <code>
    <visibleWhen checkEnabled="false">
        </with>
        <with variable="activePartResources">
            <iterate operator="or">
                <instanceof value="<qualified-resource-class>" />
            </iterate>
        </with>
    </visibleWhen>
</code>
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 24, 2014 3462       abenak      Initial creation
 * 
 * </pre>
 * 
 * @author abenak
 * @version 1.0
 */

public class ResourceSourceProvider extends AbstractSourceProvider {

    /**
     * Listens on various components of an {@link IDisplayPaneContainer}. These
     * components that are being listened to are able to modify the existence of
     * resources and capabilities, and must fire an update notification when a
     * modification has been detected
     */
    private final class ListenerContainer implements
            IRenderableDisplayChangedListener, AddListener, RemoveListener,
            IResourceDataChanged {

        /** The container that we are constructing listeners for */
        private final IDisplayPaneContainer container;

        public ListenerContainer(IDisplayPaneContainer container) {
            this.container = container;

            initialize();
        }

        /**
         * Initializes the relevant component listeners for this container
         */
        private void initialize() {
            container.addRenderableDisplayChangedListener(this);

            for (IDisplayPane pane : container.getDisplayPanes()) {
                ResourceList rscList = pane.getDescriptor().getResourceList();

                rscList.addPostAddListener(this);
                rscList.addPreRemoveListener(this);

                for (ResourcePair rp : rscList) {
                    rp.getResourceData().addChangeListener(this);
                }
            }
        }

        /**
         * Gets all {@link AbstractVizResource}s that are currently existing on
         * this {@link IDisplayPaneContainer}
         * 
         * @return {@link Set} of {@link AbstractVizResource}s
         */
        public Collection<AbstractVizResource<?, ?>> getContainerResources() {
            Map<Class<?>, AbstractVizResource<?, ?>> rscs = new HashMap<Class<?>, AbstractVizResource<?, ?>>();

            for (IDisplayPane pane : container.getDisplayPanes()) {
                for (ResourcePair rp : pane.getDescriptor().getResourceList()) {
                    AbstractVizResource<?, ?> rsc = rp.getResource();
                    if (rsc != null) {
                        rscs.put(rsc.getClass(), rsc);
                    }
                }
            }
            return rscs.values();
        }

        /**
         * Gets all {@link AbstractCapability} that are currently existing on
         * this {@link IDisplayPaneContainer}
         * 
         * @return {@link Set} of {@link AbstractCapability}s
         */
        public Collection<AbstractCapability> getContainerCapabilities() {
            Map<Class<?>, AbstractCapability> caps = new HashMap<Class<?>, AbstractCapability>();

            for (IDisplayPane pane : container.getDisplayPanes()) {
                for (ResourcePair rp : pane.getDescriptor().getResourceList()) {
                    AbstractVizResource<?, ?> rsc = rp.getResource();
                    if (rsc != null) {
                        Iterator<AbstractCapability> iterator = rsc
                                .getCapabilities().iterator();
                        while (iterator.hasNext()) {
                            AbstractCapability cap = iterator.next();
                            caps.put(cap.getClass(), cap);
                        }
                    }
                }
            }
            return caps.values();
        }

        /**
         * Performs necessary clean up. It is important that this method is
         * called when the {@link IDisplayPaneContainer} is being destroyed on
         * the window to prevent memory leaks.
         */
        public void dispose() {
            // Remove myself as listener to everything in initialize
            container.removeRenderableDisplayChangedListener(this);

            for (IDisplayPane pane : container.getDisplayPanes()) {
                ResourceList rscList = pane.getDescriptor().getResourceList();

                rscList.removePostAddListener(this);
                rscList.removePreRemoveListener(this);

                for (ResourcePair rp : rscList) {
                    rp.getResourceData().removeChangeListener(this);
                }
            }
        }

        @Override
        public void renderableDisplayChanged(IDisplayPane pane,
                IRenderableDisplay newRenderableDisplay, DisplayChangeType type) {
            if (type == DisplayChangeType.ADD) {
                ResourceList rscList = pane.getDescriptor().getResourceList();

                rscList.addPostAddListener(this);
                rscList.addPreRemoveListener(this);

                for (ResourcePair rp : rscList) {
                    rp.getResourceData().addChangeListener(this);
                }
            } else if (type == DisplayChangeType.REMOVE) {
                ResourceList rscList = pane.getDescriptor().getResourceList();

                rscList.removePostAddListener(this);
                rscList.removePreRemoveListener(this);

                for (ResourcePair rp : rscList) {
                    rp.getResourceData().removeChangeListener(this);
                }
            }
            // Notify of a change
            fireSourceChanged();
        }

        @Override
        public void notifyAdd(ResourcePair rp) throws VizException {
            // Setup listener then fire update
            rp.getResourceData().addChangeListener(this);

            fireSourceChanged();
        }

        @Override
        public void notifyRemove(ResourcePair rp) throws VizException {
            // Remove listener from the rscData and fire update
            rp.getResourceData().removeChangeListener(this);

            fireSourceChanged();
        }

        @Override
        public void resourceChanged(ChangeType type, Object object) {
            if (type == ChangeType.CAPABILITY) {
                // Simply notify of update
                fireSourceChanged();
            }
        }
    }

    private static final String PART_RESOURCES = "activePartVizResources";

    private static final String PART_CAPABILITIES = "activePartVizCapabilities";

    private static final String EDITOR_RESOURCES = "activeEditorVizResources";

    private static final String EDITOR_CAPABILITIES = "activeEditorVizCapabilities";

    private final Map<IDisplayPaneContainer, ListenerContainer> srcMap = new IdentityHashMap<IDisplayPaneContainer, ListenerContainer>();

    /** The window listener that handles when CAVE window states are modified */
    private final IWindowListener windowListener = new IWindowListener() {

        @Override
        public void windowOpened(IWorkbenchWindow window) {
            if (window != null) {
                // New window, attach the partListener
                window.getPartService().addPartListener(partListener);
            }
        }

        @Override
        public void windowClosed(IWorkbenchWindow window) {
            if (window != null) {
                // Window was destroyed, stop listening to it
                window.getPartService().removePartListener(partListener);
            }
        }

        @Override
        public void windowDeactivated(IWorkbenchWindow window) {
            // noop
        }

        @Override
        public void windowActivated(IWorkbenchWindow window) {
            // noop
        }
    };

    /** The partListener listening to all {@link IWorkbenchPart}s */
    private final IPartListener partListener = new IPartListener() {

        public final void partActivated(final IWorkbenchPart part) {
            // Part changed, get update for new activepart
            fireSourceChanged();
        }

        public final void partDeactivated(final IWorkbenchPart part) {
            // noop
        }

        public final void partOpened(final IWorkbenchPart part) {
            // We only care about parts that may have resources & capabilities
            if (part instanceof IDisplayPaneContainer) {

                IDisplayPaneContainer container = (IDisplayPaneContainer) part;

                srcMap.put(container, new ListenerContainer(container));

                // There was a change, fire src changed
                fireSourceChanged();
            }
        }

        public final void partClosed(final IWorkbenchPart part) {

            if (part instanceof IDisplayPaneContainer) {
                IDisplayPaneContainer container = (IDisplayPaneContainer) part;

                ListenerContainer dc = srcMap.remove(container);
                if (dc != null) {
                    dc.dispose();
                }

                // Src has changed
                fireSourceChanged();
            }
        }

        public final void partBroughtToTop(final IWorkbenchPart part) {
            // noop
        }

    };

    /** Executes a sourceChanged on the UI thread */
    private Runnable fireSourceChangedRunnable;

    /** The active workbench */
    private IWorkbench workbench;

    @Override
    public void dispose() {
        // Remove the highest-level listener
        workbench.removeWindowListener(windowListener);
    }

    @Override
    public void initialize(IServiceLocator locator) {
        super.initialize(locator);

        workbench = PlatformUI.getWorkbench();

        workbench.addWindowListener(windowListener);
    }

    @Override
    public Map<String, Object> getCurrentState() {

        // Initially populate for each defined variable as returning NULL for
        // any source key may give incorrect expression results
        Collection<?> editorResources = new HashSet<Object>();
        Collection<?> editorCapabilities = new HashSet<Object>();
        Collection<?> partResources = new HashSet<Object>();
        Collection<?> partCapabilities = new HashSet<Object>();

        IWorkbenchWindow activeWindow = null;
        IWorkbenchPage activePage = null;
        if (workbench != null) {
            activeWindow = workbench.getActiveWorkbenchWindow();
        }
        if (activeWindow != null) {
            activePage = activeWindow.getActivePage();
        }
        if (activePage != null) {
            IWorkbenchPart activePart = activePage.getActivePart();
            // We only care if it's an IDPC
            if (activePart instanceof IDisplayPaneContainer) {
                ListenerContainer dc = srcMap.get(activePart);

                if (dc != null) {
                    partResources = dc.getContainerResources();
                    partCapabilities = dc.getContainerCapabilities();
                }
            }

            IEditorPart activeEditor = activePage.getActiveEditor();
            // We only care if it's an IDPC
            if (activeEditor instanceof IDisplayPaneContainer) {
                if (activePart != activeEditor) {
                    ListenerContainer dc = srcMap.get(activeEditor);

                    if (dc != null) {
                        editorResources = dc.getContainerResources();
                        editorCapabilities = dc.getContainerCapabilities();
                    }
                } else {
                    editorCapabilities = partCapabilities;
                    editorResources = partResources;
                }
            }
        }

        Map<String, Object> map = new HashMap<String, Object>();
        map.put(EDITOR_RESOURCES, editorResources);
        map.put(EDITOR_CAPABILITIES, editorCapabilities);
        map.put(PART_RESOURCES, partResources);
        map.put(PART_CAPABILITIES, partCapabilities);

        return map;
    }

    /**
     * Creates a new {@link Runnable} to update sources with the current state
     * of the variables
     */
    private void fireSourceChanged() {
        if (fireSourceChangedRunnable == null) {
            fireSourceChangedRunnable = new Runnable() {
                @Override
                public void run() {
                    fireSourceChangedRunnable = null;
                    fireSourceChanged(ISources.ACTIVE_WORKBENCH_WINDOW,
                            getCurrentState());
                }
            };
            workbench.getDisplay().asyncExec(fireSourceChangedRunnable);
        }
    }

    @Override
    public String[] getProvidedSourceNames() {
        return new String[] { PART_RESOURCES, PART_CAPABILITIES,
                EDITOR_RESOURCES, EDITOR_CAPABILITIES };
    }

}
