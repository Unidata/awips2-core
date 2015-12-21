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
package com.raytheon.viz.ui.widgets;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.WorkbenchMessages;
import org.eclipse.ui.plugin.AbstractUIPlugin;

/**
 * Creates within a specified {@link Composite} and handles user interaction
 * with a filtering component.
 * 
 * Code was copied from FilteredTree, as FilteredTree did not offer an advanced
 * enough matching capability
 * 
 * This creates a nice looking text widget with a button embedded in the widget
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 11, 2015 4401       bkowal      Initial creation
 * Aug 26, 2015 4800       bkowal      Prevent NPE if all required properties are
 *                                     not set in time before the first filter.
 * Dec 18, 2015 5216       dgilling    Use ModifyListener instead of KeyListener.
 * 
 * </pre>
 * 
 * @author bkowal
 * @version 1.0
 */

@SuppressWarnings("restriction")
public class FilterDelegate {

    private TreeViewer treeViewer;

    private final AbstractVizTreeFilter filter;

    private IFilterInput filterInput;

    private Text text;

    private Image inactiveImage = null;

    private Image activeImage = null;

    private Image pressedImage = null;

    public FilterDelegate(Composite parent, AbstractVizTreeFilter filter) {
        this(parent, null, filter, null);
    }

    public FilterDelegate(Composite parent, TreeViewer treeViewer,
            AbstractVizTreeFilter filter, IFilterInput filterInput) {
        this.treeViewer = treeViewer;
        this.filter = filter;
        this.filterInput = filterInput;
        this.createImages();

        Composite comp = new Composite(parent, SWT.BORDER);
        comp.setBackground(Display.getCurrent().getSystemColor(
                SWT.COLOR_LIST_BACKGROUND));
        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        comp.setLayout(layout);
        GridData gd = new GridData(SWT.FILL, SWT.NONE, true, false);
        comp.setLayoutData(gd);

        this.createFilterText(comp);
        this.createClearControl(parent, comp);
        parent.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                dispose();
            }
        });
    }

    private void createImages() {
        inactiveImage = AbstractUIPlugin.imageDescriptorFromPlugin(
                PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif")
                .createImage();
        activeImage = AbstractUIPlugin.imageDescriptorFromPlugin(
                PlatformUI.PLUGIN_ID, "$nl$/icons/full/etool16/clear_co.gif")
                .createImage();
        pressedImage = new Image(Display.getCurrent(), activeImage,
                SWT.IMAGE_GRAY);
    }

    private void createFilterText(Composite comp) {
        text = new Text(comp, SWT.SINGLE | SWT.NONE);
        GridData data = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        text.setLayoutData(data);
        text.addModifyListener(new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                filter();
            }
        });
    }

    private void createClearControl(Composite parent, Composite comp) {
        /*
         * only create the button if the text widget doesn't support one
         * natively
         */
        final Label clearControl = new Label(comp, SWT.NONE);
        clearControl.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER,
                false, false));
        clearControl.setImage(inactiveImage);
        clearControl.setBackground(parent.getDisplay().getSystemColor(
                SWT.COLOR_LIST_BACKGROUND));
        clearControl
                .setToolTipText(WorkbenchMessages.FilteredTree_ClearToolTip);
        clearControl.addMouseListener(new MouseAdapter() {
            private MouseMoveListener fMoveListener;

            @Override
            public void mouseDown(MouseEvent e) {
                clearControl.setImage(pressedImage);
                fMoveListener = new MouseMoveListener() {
                    private boolean fMouseInButton = true;

                    @Override
                    public void mouseMove(MouseEvent e) {
                        boolean mouseInButton = isMouseInButton(e);
                        if (mouseInButton != fMouseInButton) {
                            fMouseInButton = mouseInButton;
                            clearControl.setImage(mouseInButton ? pressedImage
                                    : inactiveImage);
                        }
                    }
                };
                clearControl.addMouseMoveListener(fMoveListener);
            }

            @Override
            public void mouseUp(MouseEvent e) {
                if (fMoveListener != null) {
                    clearControl.removeMouseMoveListener(fMoveListener);
                    fMoveListener = null;
                    boolean mouseInButton = isMouseInButton(e);
                    clearControl.setImage(mouseInButton ? activeImage
                            : inactiveImage);
                    if (mouseInButton) {
                        text.setText("");
                        filter();
                        text.setFocus();
                    }
                }
            }

            private boolean isMouseInButton(MouseEvent e) {
                Point buttonSize = clearControl.getSize();
                return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y
                        && e.y < buttonSize.y;
            }
        });
        clearControl.addMouseTrackListener(new MouseTrackAdapter() {
            @Override
            public void mouseEnter(MouseEvent e) {
                clearControl.setImage(activeImage);
            }

            @Override
            public void mouseExit(MouseEvent e) {
                clearControl.setImage(inactiveImage);
            }
        });
        clearControl.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                inactiveImage.dispose();
                activeImage.dispose();
                pressedImage.dispose();
            }
        });
        clearControl.getAccessible().addAccessibleListener(
                new AccessibleAdapter() {
                    @Override
                    public void getName(AccessibleEvent e) {
                        e.result = WorkbenchMessages.FilteredTree_AccessibleListenerClearButton;
                    }
                });
        clearControl.getAccessible().addAccessibleControlListener(
                new AccessibleControlAdapter() {
                    @Override
                    public void getRole(AccessibleControlEvent e) {
                        e.detail = ACC.ROLE_PUSHBUTTON;
                    }
                });
    }

    private void dispose() {
        inactiveImage.dispose();
        activeImage.dispose();
        pressedImage.dispose();
    }

    private void filter() {
        if (this.treeViewer == null) {
            return;
        }

        // call refresh on the tree to get the most up-to-date children
        this.treeViewer.refresh(false);

        /*
         * Ensure that the filter has been applied to the tree viewer.
         */
        if (this.treeViewer.getFilters().length == 0) {
            ViewerFilter[] filters = new ViewerFilter[1];
            filters[0] = this.filter;
            this.treeViewer.setFilters(filters);
        }

        /*
         * set the current filter text so that it can be used when refresh is
         * called again.
         */
        final String currentText = this.text.getText();
        this.filter.setCurrentText(currentText);
        final boolean expandedState = (currentText.isEmpty() == false);
        if (this.filterInput != null) {
            for (Object ob : this.filterInput.getObjects()) {
                this.treeViewer.setExpandedState(ob, expandedState);
            }
        }

        // call refresh on the tree after things are expanded
        this.treeViewer.refresh(false);
    }

    /**
     * @param treeViewer
     *            the treeViewer to set
     */
    public void setTreeViewer(TreeViewer treeViewer) {
        this.treeViewer = treeViewer;
    }

    /**
     * @param filterInput
     *            the filterInput to set
     */
    public void setFilterInput(IFilterInput filterInput) {
        this.filterInput = filterInput;
    }
}