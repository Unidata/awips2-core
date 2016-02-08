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
package com.raytheon.viz.ui.actions;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.menu.MHandledItem;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolItem;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * 
 * For toolbar items that execute commands, eclipse will attempt to put the
 * keybinding for the command in the tooltip text of the tool item. Viz
 * keybindings are very sensitive to context changes and eclipse does not keep
 * the tooltip text in sync. To workaround this problem we add a mouse listener
 * and force eclipse to regenerate tooltips whenever the mouse is over the tool
 * item.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Feb 08, 2016  5326     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class ToolbarToolTipProcessor {

    @Execute
    void addTooltipHandler(IEventBroker broker) {
        broker.subscribe(UIEvents.UIElement.TOPIC_WIDGET, new EventHandler() {

            @Override
            public void handleEvent(Event event) {
                Object element = event.getProperty(UIEvents.EventTags.ELEMENT);
                if (!(element instanceof MHandledItem)) {
                    return;
                }
                Object newValue = event
                        .getProperty(UIEvents.EventTags.NEW_VALUE);
                if (!(newValue instanceof ToolItem)) {
                    return;
                }
                new TooltipUpdateListener((ToolItem) newValue,
                        (MHandledItem) element);

            }
        });
    }

    private static class TooltipUpdateListener implements Listener {

        private final MHandledItem modelItem;

        public TooltipUpdateListener(ToolItem item, MHandledItem modelItem) {
            this.modelItem = modelItem;
            /*
             * Mouse enter is not called on the item but is called on the parent
             */
            item.getParent().addListener(SWT.MouseEnter, this);
            /* listen for dispose to remove listener from parent. */
            item.addListener(SWT.Dispose, this);
        }

        @Override
        public void handleEvent(org.eclipse.swt.widgets.Event event) {
            if (event.type == SWT.MouseEnter) {
                /*
                 * Set the tooltip to something different, this triggers
                 * regeneration of the keybinding on the tooltip as well.
                 */
                String toolTip = modelItem.getTooltip();
                if (toolTip == null) {
                    modelItem.setTooltip("");
                } else {
                    modelItem.setTooltip(null);
                }
                modelItem.setTooltip(toolTip);
            } else if (event.type == SWT.Dispose) {
                ((ToolItem) event.widget).getParent()
                        .removeListener(SWT.MouseEnter, this);
            }
        }

    }
}
