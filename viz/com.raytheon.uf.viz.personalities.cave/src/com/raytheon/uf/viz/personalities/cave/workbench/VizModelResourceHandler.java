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
package com.raytheon.uf.viz.personalities.cave.workbench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.internal.workbench.ResourceHandler;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspectiveStack;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IModelResourceHandler;
import org.eclipse.e4.ui.workbench.IWorkbench;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.ui.views.IViewDescriptor;

/**
 * Implementation of {@link IModelResourceHandler} that attempts to correct
 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=486073
 * 
 * This implementation is based off of WorkbenchWindow#hideNonRestorableViews()
 * but it is executed against a copy of the model during periodic background
 * save tasks. It also contains checks similar to those in the CleanupAddon for
 * removing empty containers since the Addon is not run against unrendered
 * copies of the model.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Nov 23, 2016  6004     bsteffen  Initial creation
 * Jun 15, 2020  8000     bsteffen  Ensure perspectives are not hidden.
 * 
 * </pre>
 *
 * @author bsteffen
 */
@SuppressWarnings("restriction")
public class VizModelResourceHandler extends ResourceHandler {

    /**
     * Optional because it is not available when the model is loaded, but it
     * will be necessary when the model is saved.
     */
    @Inject
    @Optional
    private org.eclipse.ui.IWorkbench workbench;

    /**
     * Optional because it is not available when the model is loaded, but it
     * will be necessary when the model is saved.
     */
    @Inject
    @Optional
    private EModelService modelService;

    private Resource resource;

    @Inject
    public VizModelResourceHandler(
            @Named(IWorkbench.PERSIST_STATE) boolean saveAndRestore,
            @Named(IWorkbench.CLEAR_PERSISTED_STATE) boolean clearPersistedState) {
        super(saveAndRestore, clearPersistedState);
    }

    @Override
    public Resource loadMostRecentModel() {
        this.resource = super.loadMostRecentModel();
        return resource;
    }

    @Override
    public Resource createResourceWithApp(MApplication theApp) {
        if (workbench == null || modelService == null) {
            return super.createResourceWithApp(theApp);
        }
        recursivelySearchForNonRestorableViews(theApp, new ArrayList<>());

        return super.createResourceWithApp(theApp);
    }

    @Override
    public void save() throws IOException {
        if (this.resource != null) {
            MApplication application = (MApplication) this.resource
                    .getContents().get(0);
            recursivelySearchForNonRestorableViews(application,
                    new ArrayList<>());
        }
        super.save();
    }

    /**
     * Search for and remove non restorable views from a container.
     * 
     * @param container
     *            the container to search
     * @param sharedPartsToRemove
     *            as this method recursively searches through containers it must
     *            keep track of any views that are removed which are shared
     *            parts, these shared parts are stored in the window and must be
     *            removed from a window. As shared parts are removed they will
     *            be added to this list and after searching its children the
     *            window should remove any parts in the list form its shared
     *            parts.
     */
    public void recursivelySearchForNonRestorableViews(
            MElementContainer<?> container, List<MPart> sharedPartsToRemove) {
        List<MUIElement> toRemove = new ArrayList<>();

        for (MUIElement element : container.getChildren()) {
            if (!element.isToBeRendered()) {
                continue;
            } else if (element instanceof MElementContainer) {
                MElementContainer<?> childContainer = (MElementContainer<?>) element;
                recursivelySearchForNonRestorableViews(childContainer,
                        sharedPartsToRemove);
                if (!modelService.isLastEditorStack(childContainer)
                        && childContainer.getChildren().isEmpty()) {
                    toRemove.add(element);
                }
            } else if (element instanceof MPlaceholder) {
                MPlaceholder placeholder = (MPlaceholder) element;
                MUIElement ref = placeholder.getRef();
                if (ref instanceof MElementContainer) {
                    MElementContainer<?> childContainer = (MElementContainer<?>) ref;
                    recursivelySearchForNonRestorableViews(childContainer,
                            sharedPartsToRemove);
                } else if (ref instanceof MPart) {
                    MPart part = (MPart) ref;
                    if (isNonRestorablePart(part)) {
                        if (shouldLeavePlaceholder(part)) {
                            element.setToBeRendered(false);
                        } else {
                            sharedPartsToRemove.add(part);
                            toRemove.add(element);
                        }
                    }
                }
            } else if (element instanceof MPart) {
                MPart part = (MPart) element;
                if (isNonRestorablePart(part)) {
                    toRemove.add(part);
                }
            }
        }
        if (container instanceof MWindow) {
            MWindow window = (MWindow) container;
            window.getSharedElements().removeAll(sharedPartsToRemove);
            List<MWindow> windowsToRemove = new ArrayList<>();
            for (MWindow childWindow : window.getWindows()) {
                recursivelySearchForNonRestorableViews(childWindow,
                        sharedPartsToRemove);
                if (childWindow.getChildren().isEmpty()) {
                    windowsToRemove.add(childWindow);
                }
            }
            window.getWindows().removeAll(windowsToRemove);
        } else if (container instanceof MPerspective) {
            MPerspective perspective = (MPerspective) container;
            List<MWindow> windowsToRemove = new ArrayList<>();
            for (MWindow window : perspective.getWindows()) {
                recursivelySearchForNonRestorableViews(window,
                        sharedPartsToRemove);
                if (window.getChildren().isEmpty()) {
                    windowsToRemove.add(window);
                }
            }
            perspective.getWindows().removeAll(windowsToRemove);
        }
        container.getChildren().removeAll(toRemove);
        MUIElement selectedElement = container.getSelectedElement();
        if (selectedElement != null && (toRemove.contains(selectedElement)
                || !selectedElement.isToBeRendered())) {
            container.setSelectedElement(null);
        }

        /*
         * According to the comments in CleanupAddon "These elements should
         * neither be shown nor hidden based on their containment state"
         */
        if (!modelService.isLastEditorStack(container)
                && modelService.countRenderableChildren(container) == 0
                && !(container instanceof MPerspective)
                && !(container instanceof MPerspectiveStack)) {
            container.setToBeRendered(false);
        }
    }

    /**
     * Do not leave a placholder for a view with a secondary ID.
     */
    public static boolean shouldLeavePlaceholder(MPart part) {
        return part.getElementId().indexOf(':') < 0;
    }

    public boolean isNonRestorablePart(MPart part) {
        String id = part.getElementId();
        int colonIndex = id.indexOf(':');
        if (colonIndex > 0) {
            id = id.substring(0, colonIndex);
        }
        IViewDescriptor descriptor = workbench.getViewRegistry().find(id);
        return descriptor != null && !descriptor.isRestorable();
    }
}
