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
package com.raytheon.viz.ui.views;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.MContext;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.MUILabel;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.osgi.service.event.Event;

/**
 * A model addon that ensures windows containing {@link CaveFloatingView}s are
 * rendered as top level windows with shell trim. It is also responsible for
 * managing the title of these windows by watching for part activation within
 * them.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jan 07, 2016  5190     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class FloatingWindowAddon {

    /**
     * Whenever this processor makes a floating window it is tagged so that the
     * title can be updated.
     */
    public static final String FLOATING_WINDOW_TAG = "VizFloatingWindow";

    /**
     * Determine when the renderer is set on a new window and make it a floating
     * window if it contains only floating parts. Setting the renderer is always
     * done immediately before window creation so it is the most reliable time
     * to handle the styling.
     */
    @Inject
    @Optional
    public void subscribeTopicRenderer(
            @UIEventTopic(UIEvents.UIElement.TOPIC_RENDERER) Event event) {
        Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (element instanceof MWindow) {
            MWindow window = (MWindow) element;
            List<String> tags = window.getTags();
            if (tags.contains(FLOATING_WINDOW_TAG)) {
                return;
            }
            IWorkbenchPage page = window.getContext().get(IWorkbenchPage.class);
            if (page == null) {
                return;
            }

            EModelService modelService = window.getContext()
                    .get(EModelService.class);
            List<MPart> parts = modelService.findElements(window, null,
                    MPart.class, null);
            boolean allFloating = true;
            for (MPart part : parts) {
                if (part.getWidget() == null) {
                    /*
                     * This method is specifically trying to detect the case
                     * where an existing view was reparented into a new window.
                     * If there is no widget then the view is being restored
                     * from the model in which case we don't need to do
                     * anything.
                     */
                    allFloating = false;
                    break;
                }
                IViewPart view = page.findView(part.getElementId());
                if (!(view instanceof CaveFloatingView)) {
                    allFloating = false;
                    break;
                }
            }
            if (allFloating) {
                window.getPersistedState().put(
                        IPresentationEngine.STYLE_OVERRIDE_KEY,
                        Integer.toString(SWT.SHELL_TRIM));
                tags.add(IPresentationEngine.WINDOW_TOP_LEVEL);
                tags.add(FLOATING_WINDOW_TAG);
            }
        }
    }

    /**
     * Monitor for {@link MPart}s to be selected and change the title of
     * floating windows. Also responsible for updating the state of
     * {@link CaveFloatingView}s if they are attached or detached by dragging
     * the view.
     */
    @Inject
    @Optional
    public void subscribeTopicSelected(
            @UIEventTopic(UIEvents.ElementContainer.TOPIC_SELECTEDELEMENT) Event event) {
        Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (!(element instanceof MPartStack)) {
            return;
        }
        MPartStack partStack = (MPartStack) element;
        MUIElement selected = partStack.getSelectedElement();
        if (selected instanceof MPlaceholder) {
            selected = ((MPlaceholder) selected).getRef();
        }
        if (selected instanceof MContext && selected instanceof MUILabel) {
            IEclipseContext context = ((MContext) selected).getContext();
            if (context == null) {
                return;
            }
            MWindow window = context.get(MWindow.class);
            boolean floating = window.getTags().contains(FLOATING_WINDOW_TAG);
            if (selected instanceof MPart) {
                setFloating((MPart) selected, floating);
            }
            if (floating) {
                MUILabel label = (MUILabel) selected;
                window.setLabel(label.getLabel());
                window.setIconURI(label.getIconURI());
                window.setTooltip(label.getTooltip());
            }
        }
    }

    private void setFloating(MPart part, boolean floating) {
        IWorkbenchPage page = part.getContext().get(IWorkbenchPage.class);
        IViewPart view = page.findView(part.getElementId());
        if (view instanceof CaveFloatingView) {
            ((CaveFloatingView) view).setFloating(floating);
        }
    }
}
