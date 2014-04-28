/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Stefan Xenos, IBM; Chris Torrence, ITT Visual Information Solutions - bug 51580
 *******************************************************************************/

package com.raytheon.viz.ui.views;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.jface.util.Geometry;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.ShellListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.internal.DetachedWindow;
import org.eclipse.ui.internal.EditorManager;
import org.eclipse.ui.internal.ILayoutContainer;
import org.eclipse.ui.internal.IWorkbenchConstants;
import org.eclipse.ui.internal.IWorkbenchHelpContextIds;
import org.eclipse.ui.internal.LayoutPart;
import org.eclipse.ui.internal.PartPane;
import org.eclipse.ui.internal.PartStack;
import org.eclipse.ui.internal.ViewPane;
import org.eclipse.ui.internal.ViewStack;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.internal.dnd.DragUtil;
import org.eclipse.ui.internal.dnd.IDragOverListener;
import org.eclipse.ui.internal.dnd.IDropTarget;
import org.eclipse.ui.internal.presentations.PresentationFactoryUtil;
import org.eclipse.ui.presentations.IPresentablePart;
import org.eclipse.ui.presentations.StackDropResult;

/**
 * TODO: Drag from detached to fast view bar back to detached causes NPE
 * 
 * @since 3.1
 */
public class CaveDetachedWindow extends DetachedWindow implements
        IDragOverListener {

    public static final int INFINITE = Integer.MAX_VALUE;

    private ViewStack folder;

    private WorkbenchPage page;

    private Rectangle bounds = new Rectangle(0, 0, 0, 0);

    private Shell windowShell;

    private boolean hideViewsOnClose = true;

    private ShellListener shellListener = new ShellAdapter() {
        public void shellClosed(ShellEvent e) {
            // only continue to close if the handleClose
            // wasn't canceled
            e.doit = handleClose();
        }

        @Override
        public void shellActivated(ShellEvent e) {
            page.getPartService().setActivePart(
                    folder.getSelection().getPartReference());
        }

    };

    private Listener resizeListener = new Listener() {
        public void handleEvent(Event event) {
            Shell shell = (Shell) event.widget;
            folder.setBounds(shell.getClientArea());
        }
    };

    private IPropertyListener propertyListener = new IPropertyListener() {
        public void propertyChanged(Object source, int propId) {
            if (propId == PartStack.PROP_SELECTION) {
                activePartChanged(getPartReference(folder.getSelection()));
            }
        }
    };

    private IWorkbenchPartReference activePart;

    private IPropertyListener partPropertyListener = new IPropertyListener() {
        public void propertyChanged(Object source, int propId) {
            if (propId == IWorkbenchPartConstants.PROP_TITLE) {
                updateTitle();
            }
        }
    };

    /**
     * Create a new FloatingWindow.
     */
    public CaveDetachedWindow(WorkbenchPage workbenchPage) {
        super(workbenchPage);
        this.page = workbenchPage;

        folder = new ViewStack(page, false, PresentationFactoryUtil.ROLE_VIEW,
                null) {

            /**
             * Override close because super.close expects to find the view in
             * the page.
             */
            @Override
            protected void close(IPresentablePart part) {
                if (!presentationSite.isCloseable(part)) {
                    return;
                }

                LayoutPart layoutPart = getPaneFor(part);
                if (layoutPart != null && layoutPart instanceof PartPane) {
                    PartPane viewPane = (PartPane) layoutPart;
                    IViewReference ref = (IViewReference) viewPane
                            .getPartReference();
                    CaveWorkbenchPageManager.getInstance(page).hideView(ref);
                }
            }

            /**
             * Override paneDragStart so we can check if the stack is empty and
             * close the window
             */
            @Override
            public void paneDragStart(LayoutPart pane, Point initialLocation,
                    boolean keyboard) {
                super.paneDragStart(pane, initialLocation, keyboard);
                if (pane == null) {
                    // This means the whole stack is getting dragged:
                    for (LayoutPart lpart : getChildren()) {
                        checkChild(lpart);
                    }
                } else {
                    checkChild(pane);
                }
            }

            /**
             * Use this after dragging to check if I still only my children,
             * normally the page removes them but that doesn't work for
             * CaveDetachedWindows so this removes it from the stack when
             * someone else gets it.
             * 
             * @param pane
             */
            private void checkChild(LayoutPart pane) {
                if (pane.getContainer() != this) {
                    // the part does nto get properly dereferenced since it is
                    // not part of the page
                    ILayoutContainer newContainer = pane.getContainer();
                    newContainer.remove(pane);
                    remove(pane);
                    newContainer.add(pane);
                    if (this.getItemCount() == 0 && windowShell != null) {
                        windowShell.dispose();
                    }
                }
            }

            /**
             * Override add so we can change floating status
             */
            @Override
            public void add(LayoutPart child, Object cookie) {
                super.add(child, cookie);
                setFloating(child, true);

            }

            /**
             * Override remove so we can change floating status
             */
            @Override
            public void remove(LayoutPart child) {
                super.remove(child);
                setFloating(child, false);
            }

            private void setFloating(LayoutPart child, boolean floating) {
                if (child instanceof ViewPane) {
                    ViewPane pane = (ViewPane) child;
                    IViewPart view = pane.getViewReference().getView(false);
                    if (view instanceof CaveFloatingView) {
                        ((CaveFloatingView) view).setFloating(floating);

                    }
                }
            }

            /**
             * Override set selection so we can fire appropriate activation
             * listeners.
             */
            @Override
            public void setSelection(LayoutPart part) {
                super.setSelection(part);
                if (part instanceof PartPane) {
                    page.getPartService().setActivePart(
                            ((PartPane) part).getPartReference());
                }
            }

        };
        folder.addListener(propertyListener);
    }

    protected void activePartChanged(IWorkbenchPartReference partReference) {
        if (activePart == partReference) {
            return;
        }

        if (activePart != null) {
            activePart.removePropertyListener(partPropertyListener);
        }
        activePart = partReference;
        if (partReference != null) {
            partReference.addPropertyListener(partPropertyListener);
        }
        updateTitle();
    }

    private void updateTitle() {
        if (activePart != null) {
            windowShell.setText(activePart.getPartName());
        }
    }

    /**
     * Ensure that the shell's minimum size is equal to the minimum size of the
     * first part added to the shell.
     */
    private void updateMinimumSize() {
        // We can only do this for 'Tabbed' stacked presentations.

        // Get the minimum space required for the part
        int width = folder.computePreferredSize(true, INFINITE, INFINITE, 0);
        int height = folder.computePreferredSize(false, INFINITE, INFINITE, 0);
        Rectangle bounds = folder.getChildren()[0].getControl().getBounds();

        // Take the current shell 'trim' into account
        int shellHeight = windowShell.getBounds().height
                - windowShell.getClientArea().height;
        int shellWidth = windowShell.getBounds().width
                - windowShell.getClientArea().width;

        windowShell.setMinimumSize(width + shellWidth, height + shellHeight);
    }

    private static IWorkbenchPartReference getPartReference(PartPane pane) {

        if (pane == null) {
            return null;
        }

        return pane.getPartReference();
    }

    public Shell getShell() {
        return windowShell;
    }

    public void create() {
        windowShell = new Shell(Display.getCurrent(), SWT.SHELL_TRIM);
        windowShell.addShellListener(shellListener);
        windowShell.setData(this);
        windowShell.setText("Detached Window");
        DragUtil.addDragTarget(windowShell, this);
        hideViewsOnClose = true;
        if (bounds.isEmpty()) {
            Point center = Geometry.centerPoint(page.getWorkbenchWindow()
                    .getShell().getBounds());
            Point size = new Point(300, 200);
            Point upperLeft = Geometry.subtract(center,
                    Geometry.divide(size, 2));

            bounds = Geometry.createRectangle(upperLeft, size);
        }

        // Force the rect into the current display
        Rectangle dispBounds = getShell().getDisplay().getBounds();
        if (bounds.width > dispBounds.width)
            bounds.width = dispBounds.width;
        if (bounds.height > dispBounds.height)
            bounds.height = dispBounds.height;
        if (bounds.x + bounds.width > dispBounds.width)
            bounds.x = dispBounds.width - bounds.width;
        if (bounds.y + bounds.height > dispBounds.height)
            bounds.y = dispBounds.height - bounds.height;

        getShell().setBounds(bounds);

        configureShell(windowShell);

        createContents(windowShell);
        windowShell.layout(true);
        folder.setBounds(windowShell.getClientArea());
    }

    /**
     * Closes this window and disposes its shell.
     */
    private boolean handleClose() {

        if (hideViewsOnClose) {
            List views = new ArrayList();
            collectViewPanes(views, getChildren());

            // Save any drty views
            if (!handleSaves(views)) {
                return false; // User canceled the save
            }

            // OK, go on with the closing
            Iterator itr = views.iterator();
            while (itr.hasNext()) {
                ViewPane child = (ViewPane) itr.next();

                // Only close if closable...
                if (child.isCloseable()) {
                    CaveWorkbenchPageManager.getInstance(page).hideView(
                            child.getViewReference());

                    // Was the close cancelled?
                    if (child.getContainer() != null)
                        return false;
                } else {
                    page.attachView(child.getViewReference());
                }
            }
        }

        if (folder != null) {
            folder.dispose();
        }

        if (windowShell != null) {
            windowShell.removeListener(SWT.Resize, resizeListener);
            DragUtil.removeDragTarget(windowShell, this);
            bounds = windowShell.getBounds();

            // Unregister this detached view as a window (for key bindings).
            final IContextService contextService = (IContextService) getWorkbenchPage()
                    .getWorkbenchWindow().getWorkbench()
                    .getService(IContextService.class);
            contextService.unregisterShell(windowShell);

            windowShell.setData(null);
            windowShell = null;
        }

        return true;
    }

    /**
     * Prompts for and handles the saving of dirty, saveable views
     * 
     * @param views
     *            The list of ViewPanes
     * @return <code>true</code> unless the user cancels the save(s)
     */
    private boolean handleSaves(List views) {
        List dirtyViews = new ArrayList();
        for (Iterator iterator = views.iterator(); iterator.hasNext();) {
            ViewPane pane = (ViewPane) iterator.next();
            IViewReference ref = pane.getViewReference();
            IViewPart part = ref.getView(false);
            if (part instanceof ISaveablePart) {
                ISaveablePart saveable = (ISaveablePart) part;
                if (saveable.isDirty() && saveable.isSaveOnCloseNeeded()) {
                    dirtyViews.add(part);
                }
            }
        }

        // If there are any prompt to save -before- any closing happens
        // FIXME: This code will result in a double prompt if the user
        // decides not to save a particular view at this stage they'll
        // get a second one from the 'hideView' call...
        if (dirtyViews.size() > 0) {
            IWorkbenchWindow window = page.getWorkbenchWindow();
            boolean success = EditorManager.saveAll(dirtyViews, true, true,
                    false, window);
            if (!success) {
                return false; // the user canceled.
            }
        }

        return true;
    }

    public void drop(PartPane sourcePart) {
        Rectangle displayBounds = DragUtil
                .getDisplayBounds(folder.getControl());
        IDropTarget target = folder.createDropTarget(sourcePart,
                new StackDropResult(displayBounds, null));
        target.drop();
        updateMinimumSize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.internal.dnd.IDragOverListener#drag(org.eclipse.swt.widgets
     * .Control, java.lang.Object, org.eclipse.swt.graphics.Point,
     * org.eclipse.swt.graphics.Rectangle)
     */
    public IDropTarget drag(Control currentControl, Object draggedObject,
            Point position, Rectangle dragRectangle) {

        if (!(draggedObject instanceof ViewPane)) {
            return null;
        }

        ViewPane sourcePart = (ViewPane) draggedObject;

        if (sourcePart.getWorkbenchWindow() != page.getWorkbenchWindow()) {
            return null;
        }

        // If you put views in a CaveDetachedWindow then you can't find them
        // using the workbenchPage, this is expected for FLoatingViews but
        // puttiing other views in the detached window would cause API problems
        // by unexpectedly losing views.
        if (!(sourcePart.getViewReference().getPart(false) instanceof CaveFloatingView)) {
            return null;
        }

        IDropTarget target = folder.getDropTarget(draggedObject, position);

        if (target == null) {
            Rectangle displayBounds = DragUtil.getDisplayBounds(folder
                    .getControl());
            if (displayBounds.contains(position)) {
                target = folder.createDropTarget(sourcePart,
                        new StackDropResult(displayBounds, null));
            } else {
                return null;
            }
        }
        return target;
    }

    /**
     * Answer a list of the view panes.
     */
    private void collectViewPanes(List result, LayoutPart[] parts) {
        for (int i = 0, length = parts.length; i < length; i++) {
            LayoutPart part = parts[i];
            if (part instanceof ViewPane) {
                result.add(part);
            }
        }
    }

    /**
     * This method will be called to initialize the given Shell's layout
     */
    protected void configureShell(Shell shell) {
        updateTitle();
        shell.addListener(SWT.Resize, resizeListener);

        // Register this detached view as a window (for key bindings).
        final IContextService contextService = (IContextService) getWorkbenchPage()
                .getWorkbenchWindow().getWorkbench()
                .getService(IContextService.class);
        contextService.registerShell(shell, IContextService.TYPE_WINDOW);

        page.getWorkbenchWindow().getWorkbench().getHelpSystem()
                .setHelp(shell, IWorkbenchHelpContextIds.DETACHED_WINDOW);
    }

    /**
     * Override this method to create the widget tree that is used as the
     * window's contents.
     */
    protected Control createContents(Composite parent) {
        // Create the tab folder.
        folder.createControl(parent);

        // Reparent each view in the tab folder.
        Vector detachedChildren = new Vector();
        collectViewPanes(detachedChildren, getChildren());
        Enumeration itr = detachedChildren.elements();
        while (itr.hasMoreElements()) {
            LayoutPart part = (LayoutPart) itr.nextElement();
            part.reparent(parent);
        }

        // Return tab folder control.
        return folder.getControl();
    }

    public LayoutPart[] getChildren() {
        return folder.getChildren();
    }

    public WorkbenchPage getWorkbenchPage() {
        return this.page;
    }

    /**
     * @see IPersistablePart
     */
    public void saveState(IMemento memento) {
        if (getShell() != null) {
            bounds = getShell().getBounds();
        }

        // Save the bounds.
        memento.putInteger(IWorkbenchConstants.TAG_X, bounds.x);
        memento.putInteger(IWorkbenchConstants.TAG_Y, bounds.y);
        memento.putInteger(IWorkbenchConstants.TAG_WIDTH, bounds.width);
        memento.putInteger(IWorkbenchConstants.TAG_HEIGHT, bounds.height);

        // Save the views.
        IMemento childMem = memento.createChild(IWorkbenchConstants.TAG_FOLDER);
        folder.saveState(childMem);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.internal.IWorkbenchDragDropPart#getControl()
     */
    public Control getControl() {
        return folder.getControl();
    }

    /**
     * Opens the detached window.
     */
    public int open() {

        if (getShell() == null) {
            create();
        }

        Rectangle bounds = getShell().getBounds();
        getShell().setVisible(true);

        if (!bounds.equals(getShell().getBounds())) {
            getShell().setBounds(bounds);
        }

        return Window.OK;
    }

    public void setBounds(Rectangle bounds) {
        this.bounds = bounds;
    }

}
