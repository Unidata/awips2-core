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
package com.raytheon.viz.ui.tools;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBar;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarElement;
import org.eclipse.e4.ui.model.application.ui.menu.MToolBarSeparator;
import org.eclipse.e4.ui.model.application.ui.menu.MToolItem;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;

/**
 * When all the tool items in a toolbar are hidden then the toolbar is still
 * being rendered. It is rendered as a tiny box approximately 4x4 pixels. Most
 * of the time this causes just a little extra spacing that is unnoticed but we
 * seem to have alot of these empty toolbars and sometimes they all end next to
 * eachother which leads to a large and bothersome gap in the toolbar. This
 * addon solves the problem by listening to model events involving tool items
 * and toolbars and hiding any toolbars which have no visible toolitems.
 * 
 * If https://bugs.eclipse.org/bugs/show_bug.cgi?id=201589 was fixed then most
 * of the toolbars would be able to hide themselves.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- ----------------------------
 * May 11, 2016  5644     bsteffen  Initial creation
 * Jun 02, 2016  5640     bsteffen  Don't modify part toolbars.
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class HideEmptyToolBarAddon {

    @Inject
    private EModelService modelService;

    @Inject
    @Optional
    public void subscribeTopicContainerChildren(
            @UIEventTopic(UIEvents.ElementContainer.TOPIC_CHILDREN) Event event) {
        if (!UIEvents.isADD(event)) {
            return;
        }
        Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (element instanceof MTrimBar) {
            Object child = event.getProperty(UIEvents.EventTags.NEW_VALUE);
            if (child instanceof MToolBar) {
                updateVisibility((MToolBar) child);
            }
        } else if (element instanceof MToolBar) {
            updateVisibility((MToolBar) element);
        }
    }

    @Inject
    @Optional
    public void subscribeTopicVisible(
            @UIEventTopic(UIEvents.UIElement.TOPIC_VISIBLE) Event event) {
        Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
        if (element instanceof MToolItem) {
            MToolItem item = (MToolItem) element;
            MElementContainer<? extends MUIElement> parent = item.getParent();
            if (parent instanceof MToolBar) {
                updateVisibility((MToolBar) parent);
            }
        }
    }

    private void updateVisibility(MToolBar toolbar) {
        if (modelService.getContainer(toolbar) instanceof MPart) {
            /*
             * The SWT renderers don't handle toggling part toolbar visibility
             * correctly(as of eclipse 4.5.1). When the toolbar goes invisible
             * it is moved to the limbo shell and when it is made visible again
             * it is reparented onto the CTabFolder of the part stack. This is
             * wrong, it belongs in the top right composite of the CTabFolder.
             * This behavior is currently seen when an extension is adding
             * toolbar items because initially the toolbar is empty and when the
             * extension is processed it is no longer empty.
             * 
             * To work around this bug, simply don't touch the toolbars of
             * parts. The problems of multiple empty toolbars that this addon is
             * fixing don't apply to parts because there is only one part
             * toolbar rendered on the partstack at a time.
             */
            return;
        }
        for (MToolBarElement element : toolbar.getChildren()) {
            if (element instanceof MToolBarSeparator) {
                continue;
            } else if (element.isVisible()) {
                toolbar.setVisible(true);
                return;
            }
        }
        toolbar.setVisible(false);
    }
}
