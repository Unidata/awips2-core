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
package com.raytheon.uf.viz.personalities.cave.presentation;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.MDirtyable;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.renderers.swt.StackRenderer;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.internal.e4.compatibility.CompatibilityEditor;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import com.raytheon.uf.viz.personalities.cave.menu.VizEditorSystemMenu;
import com.raytheon.viz.ui.IRenameablePart;

/**
 * Custom version of {@link StackRenderer} that allows the
 * {@link VizEditorSystemMenu} to add menu items whenever the right click menu
 * is opened for a tab with a {@link CompatibilityEditor}.
 * 
 * This also extends the tab naming convention so that instances of
 * {@link IRenameablePart} do not prepend an '*' to the name. Determining if a
 * part is renameable and dirty is complicated so there are 3 things needed to
 * suppress the '*'
 * <ol>
 * <li>When a tab is created if it is dirty and renameable then the '*' is
 * removed. In practice this doesn't seem to happen because a part is created
 * 'clean' and only marked dirty during initialization, also the actual Object
 * for the part is usually not set this early so it is impossible to tell if it
 * is renameable.</li>
 * <li>This class gets updates to the dirty flag or label of parts. If they are
 * renameable it will ignore dirty updates so that no '*' is added and it will
 * remove the '*' after a rename. During initialization this doesn't work
 * because the dirty flag can be set before the Object is set so we have no way
 * of knowing if it is renameable.</li>
 * <li>This class listens for when the object is set on a part and if the object
 * is dirty and renameable then remove the '*', the object is usually set
 * towards the end of initialization so this handles the flaw left in the
 * previous methods.</li>
 * </ol>
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Dec 23, 2015  5189     bsteffen    Initial Creation.
 * Jun 14, 2016  5681     bsteffen    Override hideChild
 * 
 * </pre>
 * 
 * @author bsteffen
 */
@SuppressWarnings("restriction")
public class VizStackRenderer extends StackRenderer {

    @Inject
    private IEventBroker eventBroker;

    /**
     * Custom dirty rendering part 3: When the contribution item of a part is
     * set remove the '*' if it is renameable.
     */
    private final EventHandler setContributionObjectHandler = new EventHandler() {

        @Override
        public void handleEvent(Event event) {
            MUIElement element = (MUIElement) event
                    .getProperty(UIEvents.EventTags.ELEMENT);
            if (!(element instanceof MPart)) {
                return;
            }
            Object attName = event.getProperty(UIEvents.EventTags.ATTNAME);
            if (!UIEvents.Contribution.OBJECT.equals(attName)) {
                return;
            }
            MPart part = (MPart) element;
            if (isDirty(element) && isRenameable(part)) {
                CTabItem tabItem = findItemForPart(part);
                if (tabItem != null) {
                    tabItem.setText(tabItem.getText().substring(1));
                }
            }
        }
    };

    private VizEditorSystemMenu editorMenu = null;

    /** Override to add custom menu items */
    @Override
    protected void populateTabMenu(Menu menu, MPart part) {
        super.populateTabMenu(menu, part);
        if (part.getObject() instanceof CompatibilityEditor) {
            if (editorMenu == null) {
                editorMenu = new VizEditorSystemMenu();
            }
            CompatibilityEditor editor = (CompatibilityEditor) part.getObject();
            editorMenu.show(menu, editor.getEditor());
        }
    }

    @PostConstruct
    public void startSubscriptions() {
        eventBroker.subscribe(UIEvents.Contribution.TOPIC_OBJECT,
                setContributionObjectHandler);
    }

    @PreDestroy
    public void stopSubscriptions() {
        eventBroker.unsubscribe(setContributionObjectHandler);
    }

    /**
     * Custom dirty rendering part 1: remove the '*' from dirty renameable parts
     * when the widget is created.
     */
    @Override
    protected void createTab(MElementContainer<MUIElement> stack,
            MUIElement element) {
        Object widget = element.getWidget();
        super.createTab(stack, element);
        if (widget == null && element instanceof MPart && isDirty(element)) {
            MPart part = (MPart) element;
            if (isRenameable(part)) {
                CTabItem tabItem = findItemForPart(part);
                if (tabItem != null) {
                    tabItem.setText(tabItem.getText().substring(1));
                }
            }

        }
    }

    /**
     * Custom dirty rendering part 2: Ignore dirty events from renameable parts
     * and remove '*' after label events.
     */
    @Override
    protected void updateTab(CTabItem cti, MPart part, String attName,
            Object newValue) {
        if (UIEvents.Dirtyable.DIRTY.equals(attName)) {
            if (isRenameable(part)) {
                return;
            }
        }
        super.updateTab(cti, part, attName, newValue);
        if (UIEvents.UILabel.LABEL.equals(attName)
                || UIEvents.UILabel.LOCALIZED_LABEL.equals(attName)) {
            if (isDirty(part) && isRenameable(part)) {
                CTabItem tabItem = findItemForPart(part);
                if (tabItem != null) {
                    tabItem.setText(tabItem.getText().substring(1));
                }
            }
        }
    }


    /**
     * This method is overridden to work around
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=469437. The implementation
     * of this method from StackRenderer can actually cause toolbars from the
     * hidden part to become visible, this implementation hides the toolbars if
     * the currently selected part is hidden.
     */
    @Override
    public void hideChild(MElementContainer<MUIElement> parentElement,
            MUIElement child) {

        CTabFolder ctf = (CTabFolder) parentElement.getWidget();
        if (ctf == null) {
            return;
        }

        // Check if we have to reset the currently active child for the stack
        CTabItem cti = null;
        for (CTabItem item : ctf.getItems()) {
            if (item.getData(OWNING_ME) == child) {
                cti = item;
                break;
            }
        }
        if (cti == ctf.getSelection()) {
            // If we're the selected part we need to clear the top right...

            /*
             * This should be equivalent to adjustTopRight when there is no
             * selection.
             */
            Composite trComp = (Composite) ctf.getTopRight();
            trComp.setData("thePart", null);
            trComp.setVisible(false);

            Control[] kids = trComp.getChildren();
            ToolBar menuTB = (ToolBar) kids[kids.length - 1];
            menuTB.getItem(0).setData("thePart", null);
            RowData rd = (RowData) menuTB.getLayoutData();
            rd.exclude = true;
            menuTB.setVisible(false);

            trComp.pack();
        }

        // find the 'stale' tab for this element and dispose it
        if (cti != null && !cti.isDisposed()) {
            cti.setControl(null);
            cti.dispose();
        }
    }

    private static boolean isDirty(MUIElement element) {
        return element instanceof MDirtyable
                && ((MDirtyable) element).isDirty();
    }

    private static boolean isRenameable(MPart part) {
        if (part.getObject() instanceof CompatibilityEditor) {
            CompatibilityEditor editor = (CompatibilityEditor) part.getObject();
            return (editor.getEditor() instanceof IRenameablePart);
        }
        return false;
    }
}
