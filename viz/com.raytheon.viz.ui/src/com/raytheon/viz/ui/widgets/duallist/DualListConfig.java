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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.SWT;

/**
 * Config file for DualList class. Reused from Data Delivery.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 31, 2012           mpduff    Initial creation.
 * Aug 10, 2012  1002     mpduff    Added numeric flag for sorting.
 * Jan 07, 2013  1431     mpduff    Add case sensitive and exclude flags.
 * Aug 20, 2013  1733     mpduff    Add match flag.
 * Sep 27, 2013  2419     lvenable  Updated include description.
 * May 04, 2015  4419     rferrel   Add {@link #sortList} flag.
 * May 05, 2016  5487     tjensen   Added option for reverse sorting.
 * Feb 28, 2017  6121     randerso  Change DualListConfig to specify list height
 *                                  in items and width in characters
 * Sep 26, 2017  6413     tjensen   Add pre-sorted options for lists
 *
 * </pre>
 *
 * @author mpduff
 */

public class DualListConfig {

    /**
     * Available list text set to a default.
     */
    private String availableListText = "Available:";

    /**
     * Selected list text set to a default.
     */
    private String selectedListText = "Selected:";

    /**
     * Width of the list controls in pixels.
     */
    @Deprecated
    private int listWidth = 100;

    /**
     * Height of the list controls in pixels.
     */
    @Deprecated
    private int listHeight = 125;

    /**
     * Width of the list controls in characters.
     */
    private int listWidthInChars = SWT.DEFAULT;

    /**
     * Height of the list controls in items.
     */
    private int visibleItems = SWT.DEFAULT;

    /**
     * Flag to determine if the up/down buttons should be shown.
     */
    private boolean showUpDownBtns = false;

    /**
     * List of items that should initially appear in the selected item list.
     */
    private List<String> selectedList = new ArrayList<>();

    /**
     * Full list of available items.
     */
    private List<String> fullList = new ArrayList<>();

    /**
     * The include list is a set of items that will always be present the
     * selected list and cannot be removed from the selected list.
     */
    private Set<String> includeList = new HashSet<>();

    /**
     * The search field.
     */
    private String searchField = null;

    /**
     * Case Sensitive search flag.
     */
    private boolean caseFlag = false;

    /**
     * Exclude search flag.
     */
    private boolean excludeFlag = false;

    private boolean sortList = false;

    private boolean reverseSort = false;

    private IMenuData menuData;

    /** Flag for numeric data */
    private boolean numericData = false;

    /**
     * Flag for if the list is preSorted. Used for complex objects that have
     * unique sorting methods.
     */
    private boolean preSorted = false;

    /**
     * Match any/all flag. True is match any, false is match all. Only used when
     * searchField != null;
     */
    private boolean matchAny = true;

    /**
     * Constructor.
     */
    public DualListConfig() {

    }

    /**
     * Get the include list.
     *
     * @return the include list.
     */
    public Set<String> getIncludeList() {
        return includeList;
    }

    /**
     * Set the include list.
     *
     * @param includeList
     *            List to always include.
     */
    public void setIncludeList(Set<String> includeList) {
        this.includeList = includeList;
    }

    /**
     * Get the available list header text.
     *
     * @return Available list header text.
     */
    public String getAvailableListText() {
        return availableListText;
    }

    /**
     * Set the available list header text.
     *
     * @param availableListLabel
     *            Available list header text.
     */
    public void setAvailableListLabel(String availableListLabel) {
        this.availableListText = availableListLabel;
    }

    /**
     * Get the selected list header text.
     *
     * @return Selected list header text.
     */
    public String getSelectedListText() {
        return selectedListText;
    }

    /**
     * Set the selected list header text.
     *
     * @param selectedListLabel
     *            Selected list header text.
     */
    public void setSelectedListLabel(String selectedListLabel) {
        this.selectedListText = selectedListLabel;
    }

    /**
     * Get the list control width.
     *
     * @return The list width.
     *
     * @deprecated use {@link #getListWidthInChars()}
     */
    @Deprecated
    public int getListWidth() {
        return listWidth;
    }

    /**
     * Set the width of the list control.
     *
     * @param listWidth
     *            Width of the list control.
     *
     * @deprecated use {@link #setListWidthInChars(int)}
     */
    @Deprecated
    public void setListWidth(int listWidth) {
        this.listWidth = listWidth;
    }

    /**
     * Get the height of the list control.
     *
     * @return The height of the list control.
     *
     * @deprecated use {@link #getVisibleItems()}
     */
    @Deprecated
    public int getListHeight() {
        return listHeight;
    }

    /**
     * Set the height of the list control.
     *
     * @param listHeight
     *            The height of the list control.
     *
     * @deprecated use {@link #setVisibleItems(int)}
     */
    @Deprecated
    public void setListHeight(int listHeight) {
        this.listHeight = listHeight;
    }

    /**
     * Get the list control width.
     *
     * @return The list width in characters.
     */
    public int getListWidthInChars() {
        return listWidthInChars;
    }

    /**
     * Set the width of the list control.
     *
     * @param listWidthInChars
     *            Width of the list control in characters.
     */
    public void setListWidthInChars(int listWidthInChars) {
        this.listWidthInChars = listWidthInChars;
        this.listWidth = SWT.DEFAULT;
    }

    /**
     * Get the number of items to be visible in the list.
     *
     * @return number of visible list items.
     */
    public int getVisibleItems() {
        return visibleItems;
    }

    /**
     * Set the number of items to be visible in the list.
     *
     * @param visibleItems
     *            The number of visible list items.
     */
    public void setVisibleItems(int visibleItems) {
        this.visibleItems = visibleItems;
        this.listHeight = SWT.DEFAULT;
    }

    /**
     * Check if the up/down buttons should be shown.
     *
     * @return True if the buttons are shown, false if hidden.
     */
    public boolean isShowUpDownBtns() {
        return showUpDownBtns;
    }

    /**
     * Set the show up/down button flag.
     *
     * @param showUpDownBtns
     *            True to show the buttons, false to not show the buttons.
     */
    public void setShowUpDownBtns(boolean showUpDownBtns) {
        this.showUpDownBtns = showUpDownBtns;
    }

    /**
     * Get an array of selected items.
     *
     * @return An array of selected items.
     */
    public List<String> getSelectedList() {
        return selectedList;
    }

    /**
     * Set the array of selected items.
     *
     * @param selectedList
     *            Array of selected items.
     */
    public void setSelectedList(List<String> selectedList) {
        this.selectedList = selectedList;
    }

    /**
     * Get an array of all of the available items.
     *
     * @return The array of all available items.
     */
    public List<String> getFullList() {
        return new ArrayList<>(fullList);
    }

    /**
     * Set the array of all of the available items.
     *
     * @param fullList
     *            The array of all available items.
     */
    public void setFullList(List<String> fullList) {
        this.fullList = fullList;
    }

    /**
     * Get the search field text.
     *
     * @return the String the search field text.
     */
    public String getSearchField() {
        return searchField;
    }

    /**
     * Set the search field text.
     *
     * @param searchField
     *            the search field text.
     */
    public void setSearchField(String searchField) {
        this.searchField = searchField;
    }

    public IMenuData getMenuData() {
        return menuData;
    }

    public void setMenuData(IMenuData menuData) {
        this.menuData = menuData;
    }

    /**
     * @param numericData
     *            the numericData to set
     */
    public void setNumericData(boolean numericData) {
        this.numericData = numericData;
    }

    /**
     * @return the numericData
     */
    public boolean isNumericData() {
        return numericData;
    }

    /**
     * @return the caseFlag
     */
    public boolean isCaseFlag() {
        return caseFlag;
    }

    /**
     * @return the excludeFlag
     */
    public boolean isExcludeFlag() {
        return excludeFlag;
    }

    /**
     * @param caseFlag
     *            the caseFlag to set
     */
    public void setCaseFlag(boolean caseFlag) {
        this.caseFlag = caseFlag;
    }

    /**
     * @param excludeFlag
     *            the excludeFlag to set
     */
    public void setExcludeFlag(boolean excludeFlag) {
        this.excludeFlag = excludeFlag;
    }

    /**
     * true is match any, false is match all
     *
     * @return the matchAny
     */
    public boolean getMatchAny() {
        return matchAny;
    }

    /**
     * true is match any, false is match all
     *
     * @param matchAny
     *            the matchAny to set
     */
    public void setMatchAny(boolean matchAny) {
        this.matchAny = matchAny;
    }

    /**
     * When true the dual lists should be sorted. Default is false.
     *
     * @return sortList
     */
    public boolean isSortList() {
        return sortList;
    }

    /**
     * Set the sort state for the dual lists.
     *
     * @param sortList
     */
    public void setSortList(boolean sortList) {
        this.sortList = sortList;
    }

    public boolean isReverseSort() {
        return reverseSort;
    }

    public void setReverseSort(boolean reverseSort) {
        this.reverseSort = reverseSort;
    }

    public boolean isPreSorted() {
        return preSorted;
    }

    public void setPreSorted(boolean preSorted) {
        this.preSorted = preSorted;
    }
}
