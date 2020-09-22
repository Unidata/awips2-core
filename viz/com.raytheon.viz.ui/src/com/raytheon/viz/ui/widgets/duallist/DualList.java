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
package com.raytheon.viz.ui.widgets.duallist;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;

import com.raytheon.viz.ui.widgets.duallist.ButtonImages.ButtonImage;

/**
 * SWT Dual List Widget.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Feb 13, 2012           mpduff    Initial creation
 * Feb 14, 2012           lvenable  Update code.
 * Aug 08, 2012  863      jpiatt    Added checks for selection changes.
 * Aug 10, 2012  1002     mpduff    Fixed sorting of numeric data on move left.
 * Sep 07, 2012  684      mpduff    Deselect selection prior to selecting new
 *                                  items.
 * Dec 17, 2012  1431     mpduff    Fix filter problem when deselecting items.
 * Jan 07, 2013  1456     bgonzale  Added setSelectedList(ArrayList<String>).
 * Aug 20, 2013  1733     mpduff    Search now uses SearchUtils.
 * Sep 27, 2013  2419     lvenable  Removed the ability to get the dual config
 *                                  list and added some convenience methods to
 *                                  clear and add to the include list.
 * May 04, 2015  4419     rferrel   Handle sorting of lists
 * May 05, 2016  5487     tjensen   Added additional sorting options
 * Feb 28, 2017  6121     randerso  Change DualListConfig to specify list height
 *                                  in items and width in characters
 * May 01, 2017  6217     randerso  Added getters for available and selected
 *                                  lists. Additional code cleanup.
 * Sep 26, 2017  6413     tjensen   Add pre-sorted options for lists
 * Jul 02, 2018  7329     mapeters  Prevent selected list with up/down arrows
 *                                  from being sorted
 * Sep 22, 2020  7926     randerso  Fixed move up/down
 *
 * </pre>
 *
 * @author mpduff
 */

public class DualList extends Composite {

    /**
     * Available List widget
     */
    private List availableList;

    /**
     * Selected List Widget
     */
    private List selectedList;

    /**
     * Dual List data/configuration object
     */
    private DualListConfig config = new DualListConfig();

    /**
     * Button to move an item from the left list to the right list.
     */
    private Button moveRightBtn;

    /**
     * Button to move all items from the left list to the right list.
     */
    private Button moveAllRightBtn;

    /**
     * Button to move an item from the right list to the left list.
     */
    private Button moveLeftBtn;

    /**
     * Button to move all items from the right list to the left list.
     */
    private Button moveAllLeftBtn;

    /**
     * Move a selected item in the selected list up in the list.
     */
    private Button moveUpBtn;

    /**
     * Move a selected item in the selected list down in the list.
     */
    private Button moveDownBtn;

    /**
     * Callback when data has been selected or unselected.
     */
    private IUpdate updateCallback = null;

    /**
     * Number of columns.
     */
    private int numberOfColumns = 3;

    /**
     * Button Images.
     */
    private ButtonImages btnImg;

    /**
     * Move left flag.
     */
    private boolean moveLeft = false;

    /**
     * Move All flag.
     */
    private boolean moveAllLeft = false;

    /**
     * Constructor
     *
     * @param parent
     *            Parent container
     * @param style
     *            SWT Style
     * @param config
     *            Data/Configuration object
     */
    public DualList(Composite parent, int style, DualListConfig config) {
        this(parent, style, config, null);
    }

    /**
     * Constructor
     *
     * @param parent
     *            Parent container
     * @param style
     *            SWT Style
     * @param config
     *            Data/Configuration object
     * @param cb
     *            Update Callback
     *
     */
    public DualList(Composite parent, int style, DualListConfig config,
            IUpdate cb) {
        super(parent, style);
        this.config = config;
        this.updateCallback = cb;
        init();
    }

    /**
     * Initialize the controls.
     */
    private void init() {
        // Create the button image class
        btnImg = new ButtonImages(this);

        // Determine how many columns need to be on the layout.
        // The default is three.
        if (config.isShowUpDownBtns()) {
            numberOfColumns = 4;
        }

        GridLayout gl = new GridLayout(numberOfColumns, false);
        gl.marginWidth = 2;
        gl.marginHeight = 2;
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        this.setLayout(gl);
        this.setLayoutData(gd);

        createListLabels();
        createAvailableListControl();
        createAddRemoveControls();
        createSelectedListControl();

        if (config.isShowUpDownBtns()) {
            createMoveUpDownControls();
        }

        populateLists();
    }

    /**
     * Create the labels that will appear above the list controls.
     */
    private void createListLabels() {
        // Available label
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
        gd.horizontalIndent = 3;

        Label availableLbl = new Label(this, SWT.NONE);
        availableLbl.setText(config.getAvailableListText());
        availableLbl.setLayoutData(gd);

        new Label(this, SWT.NONE);

        // Selected label
        gd = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gd.horizontalIndent = 3;

        Label selectedLbl = new Label(this, SWT.NONE);
        selectedLbl.setText(config.getSelectedListText());
        selectedLbl.setLayoutData(gd);

        if (numberOfColumns == 4) {
            new Label(this, SWT.NONE);
        }

    }

    /**
     * Create the available list control.
     */
    private void createAvailableListControl() {
        availableList = new List(this,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

        int listWidth = config.getListWidthInChars();
        GC gc = new GC(availableList);
        if (listWidth != SWT.DEFAULT) {
            listWidth = gc.getFontMetrics().getAverageCharWidth() * listWidth;
        } else if (config.getListWidth() != SWT.DEFAULT) {
            listWidth = config.getListWidth();
        } else {
            for (String item : config.getFullList()) {
                listWidth = Math.max(listWidth, gc.textExtent(item).x);
            }
        }
        gc.dispose();

        int visibleItems = config.getVisibleItems();
        int listSize = config.getFullList().size();
        int listHeight = config.getListHeight();
        if (visibleItems != SWT.DEFAULT) {
            visibleItems = Math.min(visibleItems, listSize);
            listHeight = availableList.getItemHeight() * visibleItems;
        } else if (listHeight == SWT.DEFAULT) {
            visibleItems = listSize;
            listHeight = availableList.getItemHeight() * visibleItems;
        }

        Rectangle trim = availableList.computeTrim(0, 0, listWidth, listHeight);
        GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
        listData.widthHint = trim.width;
        listData.heightHint = trim.height;
        availableList.setLayoutData(listData);

        availableList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectedList.deselectAll();
                enableDisableLeftRightButtons();
            }
        });

        availableList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent arg0) {
                if (arg0.button == 3) {
                    if (config.getMenuData() != null
                            && config.getMenuData().isShowMenu()) {
                        showAvailableListMenu();
                    }
                }
            }
        });
    }

    /**
     * Create the add and remove controls.
     */
    private void createAddRemoveControls() {
        /*
         * Add and Remove Buttons
         */
        GridData selectData = new GridData(SWT.DEFAULT, SWT.CENTER, false,
                true);
        GridLayout selectLayout = new GridLayout(1, false);
        selectLayout.verticalSpacing = 2;

        Composite selectComp = new Composite(this, SWT.NONE);
        selectComp.setLayout(selectLayout);
        selectComp.setLayoutData(selectData);

        // Left/Right buttons
        moveAllRightBtn = new Button(selectComp, SWT.PUSH);
        moveAllRightBtn.setImage(btnImg.getImage(ButtonImage.AddAll));
        moveAllRightBtn.setEnabled(false);
        moveAllRightBtn.setToolTipText("Move all items right");
        moveAllRightBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveAllRight();
                if (updateCallback != null) {
                    updateCallback.selectionChanged();
                }
            }
        });

        moveRightBtn = new Button(selectComp, SWT.PUSH);
        moveRightBtn.setImage(btnImg.getImage(ButtonImage.Add));
        moveRightBtn.setEnabled(false);
        moveRightBtn.setToolTipText("Move selected item(s) right");
        moveRightBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveRight();
                if (updateCallback != null) {
                    updateCallback.selectionChanged();
                }
            }
        });

        moveLeftBtn = new Button(selectComp, SWT.PUSH);
        moveLeftBtn.setImage(btnImg.getImage(ButtonImage.Remove));
        moveLeftBtn.setEnabled(false);
        moveLeftBtn.setToolTipText("Move selected item(s) left");
        moveLeftBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveLeft();
                if (updateCallback != null) {
                    updateCallback.selectionChanged();
                }
            }
        });

        moveAllLeftBtn = new Button(selectComp, SWT.PUSH);
        moveAllLeftBtn.setImage(btnImg.getImage(ButtonImage.RemoveAll));
        moveAllLeftBtn.setEnabled(false);
        moveAllLeftBtn.setToolTipText("Move all items left");
        moveAllLeftBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveAllLeft(true);
                if (updateCallback != null) {
                    updateCallback.selectionChanged();
                }
            }
        });
    }

    /**
     * Create the selected list control.
     */
    private void createSelectedListControl() {
        selectedList = new List(this,
                SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

        int listWidth = config.getListWidthInChars();
        GC gc = new GC(availableList);
        if (listWidth != SWT.DEFAULT) {
            listWidth = gc.getFontMetrics().getAverageCharWidth() * listWidth;
        } else if (config.getListWidth() != SWT.DEFAULT) {
            listWidth = config.getListWidth();
        } else {
            for (String item : config.getFullList()) {
                listWidth = Math.max(listWidth, gc.textExtent(item).x);
            }
        }
        gc.dispose();

        Rectangle trim = selectedList.computeTrim(0, 0, listWidth, 0);
        GridData listData = new GridData(SWT.FILL, SWT.FILL, true, true);
        // will fill to height of available list
        listData.heightHint = 0;
        listData.widthHint = trim.width;
        selectedList.setLayoutData(listData);

        selectedList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                availableList.deselectAll();
                enableDisableLeftRightButtons();
                enableDisableUpDownButtons();
            }
        });

        selectedList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDown(MouseEvent arg0) {
                if (arg0.button == 3) {
                    if (config.getMenuData() != null
                            && config.getMenuData().isShowMenu()) {
                        showSelectedListMenu();
                    }
                }
            }
        });

    }

    /**
     * Create the move up/down controls
     */
    private void createMoveUpDownControls() {

        GridData actionData = new GridData(SWT.DEFAULT, SWT.CENTER, false,
                true);
        GridLayout actionLayout = new GridLayout(1, false);
        Composite actionComp = new Composite(this, SWT.NONE);
        actionComp.setLayout(actionLayout);
        actionComp.setLayoutData(actionData);

        moveUpBtn = new Button(actionComp, SWT.PUSH);
        moveUpBtn.setImage(btnImg.getImage(ButtonImage.Up));
        moveUpBtn.setEnabled(false);
        moveUpBtn.setToolTipText("Move item up in the list");
        moveUpBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveUp();
            }
        });

        moveDownBtn = new Button(actionComp, SWT.PUSH);
        moveDownBtn.setImage(btnImg.getImage(ButtonImage.Down));
        moveDownBtn.setEnabled(false);
        moveDownBtn.setToolTipText("Move item down in the list");
        moveDownBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleMoveDown();
            }
        });
    }

    /**
     * Set SelectedList.
     *
     * @param selectedList
     *            all users listed in notification table
     */
    public void setSelectedList(java.util.List<String> selectedList) {
        config.setSelectedList(selectedList);
        populateLists();
    }

    /**
     * Set FullList.
     *
     * @param fullList
     *            all users listed in notification table
     */
    public void setFullList(java.util.List<String> fullList) {
        config.setFullList(fullList);
        populateLists();
    }

    /**
     * Populate the available and selected list controls.
     */
    private void populateLists() {
        availableList.removeAll();
        selectedList.removeAll();

        if (config.getFullList() == null | config.getFullList().isEmpty()) {
            return;
        }

        for (String s : config.getFullList()) {
            if (s == null) {
                continue;
            }
            if (!config.getSelectedList().contains(s)) {
                availableList.add(s);
            }
        }
        for (String s : config.getSelectedList()) {
            selectedList.add(s);
        }

        if (config.isSortList()) {
            sortListItems(availableList);
            if (!config.isShowUpDownBtns()) {
                sortListItems(selectedList);
            }
        }

        enableDisableLeftRightButtons();

        entriesUpdated();
    }

    /**
     * Move down event handler
     */
    private void handleMoveDown() {
        if (selectedList.getSelectionCount() == 1
                && selectedList.getItemCount() > 1) {
            int selectedIdx = selectedList.getSelectionIndex();
            String item = selectedList.getItem(selectedIdx);

            if (selectedIdx < selectedList.getItemCount() - 2) {
                selectedList.remove(selectedIdx);
                selectedList.add(item, selectedIdx + 1);
                selectedList.deselectAll();
                selectedList.select(selectedIdx + 1);
            } else if (selectedIdx < selectedList.getItemCount() - 1) {
                selectedList.deselect(selectedIdx);
                selectedList.remove(selectedIdx);
                selectedList.add(item);
                selectedList.deselectAll();
                selectedList.select(selectedList.getItemCount() - 1);
            }
            enableDisableUpDownButtons();
        }
    }

    /**
     * Move left event handler
     */
    private void handleMoveLeft() {
        int[] selectionIndices = selectedList.getSelectionIndices();

        moveLeft = true;

        if (selectionIndices.length == 0) {
            return;
        }

        int firstIndex = selectionIndices[0];

        Set<String> list = config.getIncludeList();

        if (list.contains(selectedList.getItem(firstIndex))) {

            return;
        }

        reloadAvailableList();

        selectedList.remove(selectionIndices);

        if (selectedList.getItemCount() > 0) {
            if (firstIndex < selectedList.getItemCount()) {
                selectedList.setSelection(firstIndex);
            } else {
                selectedList.setSelection(selectedList.getItemCount() - 1);
            }
        }

        entriesUpdated();
        enableDisableUpDownButtons();
        enableDisableLeftRightButtons();
    }

    /**
     * Move all left event handler
     *
     * @param callEntriesUpdated
     */
    private void handleMoveAllLeft(boolean callEntriesUpdated) {

        Set<String> list = config.getIncludeList();
        moveAllLeft = true;

        for (String sl : selectedList.getItems()) {
            if (!list.contains(sl)) {
                selectedList.remove(sl);
            }
        }

        reloadAvailableList();

        if (callEntriesUpdated) {
            entriesUpdated();
        }

        enableDisableUpDownButtons();
        enableDisableLeftRightButtons();
    }

    /**
     * Move up event handler
     */
    private void handleMoveUp() {
        if (selectedList.getSelectionCount() == 1) {
            int selectedIdx = selectedList.getSelectionIndex();
            String col = selectedList.getItem(selectedIdx);

            if (selectedIdx > 0) {
                selectedList.remove(selectedIdx);
                selectedList.add(col, selectedIdx - 1);
                selectedList.deselectAll();
                selectedList.select(selectedIdx - 1);
            }
            enableDisableUpDownButtons();
        }
    }

    /**
     * Move all right event handler
     */
    private void handleMoveAllRight() {
        String[] items = availableList.getItems();

        if (items.length == 0) {
            return;
        }

        moveRight(items);
        availableList.removeAll();

        entriesUpdated();
        enableDisableUpDownButtons();
        enableDisableLeftRightButtons();
    }

    /**
     * Move right event handler
     */
    private void handleMoveRight() {
        String[] items = availableList.getSelection();

        if (items.length == 0) {
            return;
        }

        int firstIdxSelected = availableList.indexOf(items[0]);

        moveRight(items);
        availableList.remove(availableList.getSelectionIndices());

        if (availableList.getItemCount() > 0) {
            if (firstIdxSelected > availableList.getItemCount() - 1) {
                availableList.select(availableList.getItemCount() - 1);
            } else {
                availableList.select(firstIdxSelected);
            }
        }

        entriesUpdated();
        enableDisableUpDownButtons();
        enableDisableLeftRightButtons();
    }

    /**
     * Place desired items to the right if needed sort and keep selected items.
     *
     * @param items
     */
    private void moveRight(String[] items) {

        // add the new items to the selected list
        for (String item : items) {
            selectedList.add(item);
        }

        if (config.isSortList()) {
            String[] selectedItems = selectedList.getSelection();
            sortListItems(selectedList);
            selectedList.setSelection(selectedItems);
        }
    }

    /**
     * Convenience method to handle the enabling/disabling of the up and down
     * buttons. This depends on how many items are in the list, how many items
     * are selected, where the selected it is in the list.
     */
    private void enableDisableUpDownButtons() {
        // Return if the buttons are now even visible.
        if (!config.isShowUpDownBtns()) {
            return;
        }

        if (selectedList.getSelectionCount() > 1
                || selectedList.getSelectionCount() == 0) {
            moveUpBtn.setEnabled(false);
            moveDownBtn.setEnabled(false);
        } else {
            if (selectedList.getSelectionIndex() == 0
                    || selectedList.getItemCount() == 0) {
                moveUpBtn.setEnabled(false);
            } else {
                moveUpBtn.setEnabled(true);
            }

            if (selectedList.getSelectionIndex() == selectedList.getItemCount()
                    - 1 || selectedList.getItemCount() == 0) {
                moveDownBtn.setEnabled(false);
            } else {
                moveDownBtn.setEnabled(true);
            }
        }
    }

    /**
     * Reload the availableList data preserving the original order of the list.
     */
    private void reloadAvailableList() {

        String[] selectedStrings = availableList.getSelection();
        java.util.List<String> selectedItemList = new ArrayList<>(
                Arrays.asList(selectedList.getItems()));

        // Check if search field text present
        if (config.getSearchField() == null) {

            // If no search field text
            if (moveAllLeft) {

                availableList.removeAll();

                for (String s : config.getFullList()) {
                    if (!selectedItemList.contains(s)) {
                        availableList.add(s);
                    }
                }
            } else if (moveLeft) {
                /*
                 * Add selected item matching search field text to available
                 * list
                 */
                for (String s : selectedList.getSelection()) {
                    availableList.add(s);
                }
            }

            // Sort the list();
            sortListItems(availableList);
        } else {
            if (moveAllLeft) {
                availableList.removeAll();

                String[] fullList = config.getFullList()
                        .toArray(new String[config.getFullList().size()]);
                java.util.List<String> filteredList = SearchUtils.search(
                        config.getSearchField(), fullList, config.getMatchAny(),
                        config.isCaseFlag(), config.isExcludeFlag());

                for (String s : filteredList) {
                    availableList.add(s);
                }
            } else if (moveLeft) {
                /*
                 * Add selected item matching search field text to available
                 * list
                 */
                for (String s : selectedList.getSelection()) {
                    availableList.add(s);
                }
            }

            // Sort the list
            sortListItems(availableList);
        }

        moveAllLeft = false;
        moveLeft = false;

        availableList.setSelection(selectedStrings);
    }

    private void showAvailableListMenu() {
        IMenuData menuData = config.getMenuData();
        if (menuData.isShowMenu() && availableList.getSelectionCount() > 0) {
            menuData.setListSelection(availableList.getSelection()[0]);
            menuData.showListMenu(getShell(),
                    config.getMenuData().getMenuText());
        }
    }

    private void showSelectedListMenu() {
        IMenuData menuData = config.getMenuData();
        if (menuData.isShowMenu() && selectedList.getSelectionCount() > 0) {
            menuData.setListSelection(selectedList.getSelection()[0]);
            menuData.showListMenu(getShell(),
                    config.getMenuData().getMenuText());
        }
    }

    /**
     * Sort the items in a list box preserving the selected items
     *
     * @param listBox
     */
    private void sortListItems(List listBox) {

        String[] selectedItems = listBox.getSelection();

        String[] items = listBox.getItems();
        java.util.List<String> availableListsorted;

        // Put available list in order
        if (config.isPreSorted()) {
            availableListsorted = new ArrayList<>(1);
            java.util.List<String> itemsList = Arrays.asList(items);
            for (String item : config.getFullList()) {
                if (itemsList.contains(item)) {
                    availableListsorted.add(item);
                }
            }
        } else if (config.isNumericData()) {
            // If data are numeric then must sort on the numeric value
            try {
                // Using TreeMap to sort by double values of strings
                SortedMap<Double, String> map = new TreeMap<>();
                for (String a : items) {
                    map.put(Double.parseDouble(a), a);
                }
                availableListsorted = new ArrayList<>(map.values());

            } catch (NumberFormatException e) {
                // numeric data not all numeric, string sorting
                availableListsorted = Arrays.asList(items);
                Collections.sort(availableListsorted);
            }
        } else {
            availableListsorted = Arrays.asList(items);
            if (config.isCaseFlag()) {
                Collections.sort(availableListsorted);
            } else {
                Collections.sort(availableListsorted,
                        String.CASE_INSENSITIVE_ORDER);
            }
        }
        if (config.isReverseSort()) {
            Collections.reverse(availableListsorted);
        }

        listBox.setItems(availableListsorted.toArray(items));
        listBox.setSelection(selectedItems);
    }

    /**
     * Calls the callback method to notify something has changed.
     */
    private void entriesUpdated() {
        if (updateCallback != null) {
            if (selectedList.getItemCount() == 0) {
                updateCallback.hasEntries(false);
            } else {
                updateCallback.hasEntries(true);
            }
        }
    }

    /**
     * Clear all users.
     */
    public void clearSelection() {
        handleMoveAllLeft(false);
    }

    /**
     * Clear Available Users list.
     *
     * @param clearSelectionList
     */
    public void clearAvailableList(boolean clearSelectionList) {
        this.availableList.removeAll();

        if (clearSelectionList) {
            clearSelection();
        }
    }

    /**
     * Set the Available List items.
     *
     * @param items
     *            the list items.
     */
    public void setAvailableItems(java.util.List<String> items) {
        this.availableList.setItems(items.toArray(new String[items.size()]));
        if (config.isSortList()) {
            sortListItems(this.availableList);
        }
    }

    /**
     * Get the number of items in the list.
     *
     * @return the number of items.
     */
    public int getItemCount() {
        return selectedList.getItemCount();
    }

    /**
     * Get the Selected List items.
     *
     * @return the items in the selected list.
     */
    public String[] getSelectedListItems() {
        return selectedList.getItems();
    }

    /**
     * Get the selection.
     *
     * @return the selections.
     */
    public String[] getSelectedSelection() {
        return selectedList.getSelection();
    }

    /**
     * Get the configuration.
     *
     * @return items in available list.
     */
    public String[] getAvailableListItems() {
        return availableList.getItems();
    }

    /**
     * Set the Selected List items.
     *
     * @param items
     *            the list items.
     */
    public void setSelectedItems(String[] items) {
        selectedList.setItems(items);
        reloadAvailableList();
    }

    /**
     * Selected User items.
     *
     * @param selection
     *            selected user items
     */
    public void selectItems(String[] selection) {
        availableList.deselectAll();
        int[] selectedIndices = new int[selection.length];
        for (int i = 0; i < selection.length; i++) {
            selectedIndices[i] = availableList.indexOf(selection[i]);
        }

        availableList.select(selectedIndices);
        handleMoveRight();
    }

    /**
     * Clear the include list. The include list is the list of items that will
     * always be present in the selected list control.
     */
    public void clearIncludeList() {
        config.getIncludeList().clear();
    }

    /**
     * Add an item to the include list.
     *
     * @param item
     *            Item to add to the include list.
     */
    public void addToIncludeList(String item) {
        config.getIncludeList().add(item);
    }

    /**
     * Enable/disable left/right movement buttons
     */
    public void enableDisableLeftRightButtons() {
        moveAllRightBtn.setEnabled(availableList.getItemCount() > 0);
        moveAllLeftBtn.setEnabled(selectedList.getItemCount() > 0);
        moveRightBtn.setEnabled(availableList.getSelectionCount() > 0);
        moveLeftBtn.setEnabled(selectedList.getSelectionCount() > 0);
    }

    /**
     * @return the availableList
     */
    public List getAvailableList() {
        return availableList;
    }

    /**
     * @return the selectedList
     */
    public List getSelectedList() {
        return selectedList;
    }

    /**
     * @return the config
     */
    public DualListConfig getConfig() {
        return config;
    }
}