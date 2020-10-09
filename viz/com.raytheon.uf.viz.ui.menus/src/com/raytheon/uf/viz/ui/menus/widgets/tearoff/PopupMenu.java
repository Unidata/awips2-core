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
package com.raytheon.uf.viz.ui.menus.widgets.tearoff;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IEditorPart;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;

/**
 * The popup menu for when items are populated out of the TearOffMenuDialog
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- -------------------------------------------
 * Dec 05, 2011           mnash      Initial creation
 * Apr 10, 2013  15185    dfriedman  Do not assume there is an active editor.
 * Apr 24, 2018  6703     bsteffen   Send simulated events more like EventTable.
 * Oct 01, 2020  8234     randerso   Added SelectionListenerWrapper to get
 *                                   tear-offs working again with Eclipse 4.16.
 *                                   Improved positioning of tear-off submenus.
 *
 * </pre>
 *
 * @author mnash
 */

public class PopupMenu {
    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PopupMenu.class);

    private static class SelectionListenerWrapper implements Listener {

        private Listener originalListener;

        private Widget originalWidget;

        public SelectionListenerWrapper(Listener originalListener,
                Widget originalWidget) {
            this.originalListener = originalListener;
            this.originalWidget = originalWidget;
        }

        @Override
        public void handleEvent(Event originalEvent) {
            /*
             * Create new event with original widget so it will get past the
             * check in org.eclipse.jface.action.ActionContributionItem.
             * handleWidgetSelection()
             */
            Event event = new Event();
            try {
                for (Field field : originalEvent.getClass().getFields()) {
                    event.getClass().getField(field.getName()).set(event,
                            field.get(originalEvent));
                }

                event.widget = originalWidget;
                originalListener.handleEvent(event);
            } catch (Exception e) {
                statusHandler.error("Unable to copy event", e);
            }
        }
    }

    private Listener selectionListener = null;

    private Listener updateListener = null;

    private Listener showListener = null;

    public PopupMenu() {
        // default constructor
    }

    protected void buildMenu(final MenuItem item, final Shell shell,
            final Menu menu) {
        String longest = "";
        for (final MenuItem menItem : item.getMenu().getItems()) {
            // building a new menu item that does what we want it to do, but
            // very similar to the original item in the menu
            final MenuItem mItem = new MenuItem(menu, menItem.getStyle());
            mItem.setText(menItem.getText());

            // check for length
            if (mItem.getText().length() > longest.length()) {
                longest = mItem.getText();
            }

            mItem.setEnabled(menItem.getEnabled());
            mItem.setSelection(menItem.getSelection());
            // still going to have the menItem TODO need to update if the
            // MenuItems are disposed
            mItem.setData(menItem);

            // adding all the selection listeners from the menu
            for (Listener list : menItem.getListeners(SWT.Selection)) {
                mItem.addListener(SWT.Selection,
                        new SelectionListenerWrapper(list, menItem));
            }

            mItem.addListener(SWT.Selection, new Listener() {
                @Override
                public void handleEvent(Event event) {
                    IEditorPart editor = EditorUtil.getActiveEditor();
                    if (editor instanceof AbstractEditor) {
                        ((AbstractEditor) editor).refresh();
                    }
                }
            });

            // a show listener, so when the menu is shown (in cave) it updates
            // the time in the tear off submenu
            // this tries to update all the times when the menu is opened
            showListener = new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (!mItem.isDisposed()) {
                        if (mItem.getData() == event.data) {
                            mItem.setText(((MenuItem) event.data).getText());
                        }
                    }
                }
            };
            menItem.getParent().addListener(SWT.Show, showListener);

            // modify listener gets fired from BundleContributionItem if the
            // times are updated, so we listen for that here so that item times
            // can get updated, in the submenus
            // this does the periodic updating of the menus through the
            // SWT.Modify event which is fired from BundleContributionItem
            updateListener = new Listener() {
                @Override
                public void handleEvent(Event event) {
                    if (!mItem.isDisposed()) {
                        if (mItem.getData() == event.data) {
                            mItem.setText(((MenuItem) event.data).getText());
                        }
                    }
                }
            };
            menItem.addListener(SWT.Modify, updateListener);

            // if it has a submenu, do the following
            if (mItem.getStyle() == SWT.CASCADE) {
                final Menu subMenu = new Menu(shell, SWT.DROP_DOWN);
                mItem.setMenu(subMenu);
                subMenu.addMenuListener(new MenuAdapter() {
                    @Override
                    public void menuShown(MenuEvent e) {
                        // if not empty
                        if (subMenu.getItemCount() == 0) {
                            // execute the show listeners on the menu of the
                            // stored off MenuItem, which will populate the Menu
                            // with the items and then we are able to get those
                            // items to populate our submenu
                            Event event = new Event();
                            event.widget = ((MenuItem) mItem.getData())
                                    .getMenu();
                            event.type = SWT.Show;
                            sendEvent(event);

                            // now that we have the items, we build the menu
                            buildMenu((MenuItem) mItem.getData(), shell,
                                    subMenu);
                            subMenu.setVisible(true);
                        }
                    }
                });
            }

            // add toggle button functionality
            if (mItem.getStyle() == SWT.CHECK) {
                selectionListener = new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        // set it to the actual menu item, so that next time it
                        // pops up it will hold the correct selection
                        menItem.setSelection(
                                ((MenuItem) event.widget).getSelection());
                    }
                };
                mItem.addListener(SWT.Selection, selectionListener);
            }

            menu.addMenuListener(new MenuAdapter() {
                @Override
                public void menuHidden(MenuEvent e) {
                    if (selectionListener != null) {
                        mItem.removeListener(SWT.Selection, selectionListener);
                    }
                    menItem.removeListener(SWT.Modify, updateListener);
                    menItem.getParent().removeListener(SWT.Show, showListener);

                    // execute the hide listener on the menu so that the
                    // TearOffMenuListener gets removed from the menu and you
                    // don't get duplicate tear off items
                    Event event = new Event();
                    event.type = SWT.Hide;
                    event.widget = menItem.getParent();
                    sendEvent(event);
                }
            });
        }
    }

    /**
     * Adds the popup menu that pops up off the main TearOffMenuDialog
     *
     * @param item
     * @param shell
     * @param y
     *            y - used for the y location
     */
    private void addPopupMenu(MenuItem item, Shell shell, int y) {
        Menu menu = new Menu(shell, SWT.POP_UP);
        buildMenu(item, shell, menu);
        // get the location for the popup menu
        int xOffset = shell.getLocation().x;
        int yOffset = shell.toDisplay(0, 0).y;
        Point p = new Point(xOffset + shell.getSize().x, yOffset + y);
        menu.setLocation(p);
        menu.setVisible(true);
    }

    protected void addSubmenus(MenuItem item, Shell shell, int y) {
        // showing the menu in cave (won't actually show), but executes all the
        // listeners to build the menus (since they are built on-demand)
        Event event = new Event();
        event.widget = item.getMenu();
        event.type = SWT.Show;
        sendEvent(event);

        addPopupMenu(item, shell, y);
    }

    /**
     * Send a simulated event to any listeners.
     *
     * @param event
     *            the event to send. The widget and type fields of the event
     *            must be populated to determine where to send the event.
     */
    private void sendEvent(Event event) {
        /*
         * This attempts to simulate what happens in
         * org.eclipse.swt.widgets.EventTable. Most importantly, it is possible
         * to add and remove listeners while an event listener is running, so
         * this must check the array of listeners after each listener is fired.
         * This really matters when a SubmenuContributionItem is adding
         * BundleContributionItems on Show events because the
         * BundleContributionItems are expecting to see the same Show event.
         */
        Set<Listener> firedListeners = new HashSet<>();
        boolean listenersRemaining = false;
        while (!listenersRemaining) {
            listenersRemaining = true;
            Listener[] listeners = event.widget.getListeners(event.type);
            for (Listener listener : listeners) {
                if (firedListeners.add(listener)) {
                    try {
                        listener.handleEvent(event);
                    } catch (Exception e) {
                        statusHandler.debug(
                                "Unexpected error simulating menu event.", e);
                    }
                    listenersRemaining = false;
                    break;
                }
            }
        }
    }
}
