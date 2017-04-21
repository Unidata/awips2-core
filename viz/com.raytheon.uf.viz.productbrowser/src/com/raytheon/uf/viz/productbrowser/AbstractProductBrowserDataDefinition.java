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
package com.raytheon.uf.viz.productbrowser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.preference.IPreferenceStore;

import com.raytheon.uf.viz.core.rsc.AbstractResourceData;
import com.raytheon.uf.viz.core.rsc.DisplayType;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceType;
import com.raytheon.uf.viz.core.rsc.capabilities.DisplayTypeCapability;
import com.raytheon.uf.viz.productbrowser.pref.PreferenceBasedDataDefinition;
import com.raytheon.uf.viz.productbrowser.pref.ProductBrowserPreferenceConstants;

/**
 * @deprecated The design of this class makes excessive use of internal state
 *             that make it inherently thread-unsafe and confusing to implement.
 *             New development of data definitions should be done by
 *             implementing {@link ProductBrowserDataDefinition} directly
 *             instead of extending this class.
 * 
 *             <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 06, 2010           mnash     Initial creation
 * Jun 02, 2015  4153     bsteffen  Extract interface and deprecate.
 * 
 * </pre>
 * 
 * @author mnash
 * @version 1.0
 */
@Deprecated
public abstract class AbstractProductBrowserDataDefinition<T extends AbstractResourceData> implements
        PreferenceBasedDataDefinition {

    // display name of product for the tree
    public String displayName;

    // the resource data to construct the resource
    public T resourceData;

    // the load properties to tell the resource how to load
    public LoadProperties loadProperties;

    // most will have default values of true, only a select few (beta) will have
    // a default value of false
    protected boolean defaultEnabled = true;

    /** Use {@link ProductBrowserPreferenceConstants#ENABLE_PLUGIN} instead */
    @Deprecated
    protected static final String ENABLE_PLUGIN = ProductBrowserPreferenceConstants.ENABLE_PLUGIN;

    /** Use {@link ProductBrowserPreferenceConstants#FORMAT_DATA} instead */
    @Deprecated
    protected static final String FORMAT_DATA = ProductBrowserPreferenceConstants.FORMAT_DATA;
    
    protected List<ProductBrowserPreference> preferences = null;

    /**
     * Populates the first level of the tree
     * 
     * @return
     */
    public String populateInitial() {
        // if the preference says show, or if it doesn't have a preference page
        if (isEnabled()) {
            return displayName;
        }
        return null;
    }

    /**
     * Function for taking data and renaming it for the product browser tree
     * 
     * Should implement this for building the history list as well as for
     * building the tree correctly
     * 
     * NOTE : If overriden, it is the responsibility of the new data definition
     * to sort the data
     * 
     * @param param
     * @param parameters
     * @return
     */
    public List<ProductBrowserLabel> formatData(String param, String[] parameters) {
        List<ProductBrowserLabel> temp = new ArrayList<ProductBrowserLabel>();
        for (int i = 0; i < parameters.length; i++) {
            temp.add(new ProductBrowserLabel(parameters[i], null));
        }
        Collections.sort(temp);
        return temp;
    }

    /**
     * Populates the second and on levels of the tree
     * 
     * @param selection
     * @return
     */
    public abstract List<ProductBrowserLabel> populateData(String[] selection);

    public abstract List<String> buildProductList(List<String> historyList);

    /**
     * Builds the resource as it should be loaded based on the selection
     * 
     * @param selection
     * @param type
     */
    public abstract void constructResource(String[] selection, ResourceType type);

    /**
     * Returns the resource data that is built for each item within the product
     * browser tree
     */
    public abstract T getResourceData();

    /**
     * Used for any other things that need to be added to certain product types.
     * 
     * @return map of resourceType -> list of displayTypes
     */
    public Map<ResourceType, List<DisplayType>> getDisplayTypes() {
        return null;
    }

    /**
     * Used for adding awesome things to the product browser per plugin. This
     * level adds a boolean as to whether it is enabled or not (the default one)
     */
    protected List<ProductBrowserPreference> configurePreferences() {
        List<ProductBrowserPreference> widgets = new ArrayList<ProductBrowserPreference>();
        widgets.add(ProductBrowserPreferenceConstants.createEnabledPreference());
        return widgets;
    }

    @Override
    public List<ProductBrowserPreference> getPreferences() {
        IPreferenceStore store = Activator.getDefault().getPreferenceStore();
        if (preferences == null) {
            preferences = configurePreferences();
            for (ProductBrowserPreference pref : configurePreferences()) {
                switch (pref.getPreferenceType()) {
                case BOOLEAN:
                    store.setDefault(pref.getLabel() + displayName,
                            (Boolean) pref.getValue());
                    pref.setValue(store.getBoolean(pref.getLabel()
                            + displayName));
                    break;
                case EDITABLE_STRING:
                    store.setDefault(pref.getLabel() + displayName,
                            (String) pref.getValue());
                    pref.setValue(store.getString(pref.getLabel() + displayName));
                    break;
                case STRING_ARRAY:
                    String[] items = (String[]) pref.getValue();
                    String temp = "";
                    for (int i = 0; i < items.length; i++) {
                        if (temp.isEmpty()) {
                            temp += items[i];
                        } else {
                            temp += "," + items[i];
                        }
                    }
                    store.setDefault(pref.getLabel() + displayName, temp);
                    pref.setValue(store
                            .getString(pref.getLabel() + displayName)
                            .split(","));
                    break;
                }
            }
        }
        return preferences;
    }

    protected ProductBrowserPreference getPreference(String label) {
        for (ProductBrowserPreference pref : getPreferences()) {
            if (pref.getLabel().equals(label)) {
                IPreferenceStore store = Activator.getDefault()
                        .getPreferenceStore();
                switch (pref.getPreferenceType()) {
                case BOOLEAN:
                    pref.setValue(store.getBoolean(pref.getLabel()
                            + displayName));
                }
                return pref;
            }
        }
        return null;
    }

    protected boolean isEnabled() {
        return (Boolean) getPreference(ENABLE_PLUGIN).getValue();
    }

    @Override
    public boolean checkAvailability() {
        return populateInitial() != null;
    }

    @Override
    public Collection<DisplayType> getValidDisplayTypes(String[] selection) {
        Map<ResourceType, List<DisplayType>> displayTypeMap = getDisplayTypes();
        EnumSet<DisplayType> result = EnumSet.noneOf(DisplayType.class);
        if(displayTypeMap != null){
            for(List<DisplayType> displayTypes: displayTypeMap.values()){
                result.addAll(displayTypes);
            }
        }
        return result;
    }

    @Override
    public void loadResource(String[] selection, DisplayType displayType) {
        if (displayType != null) {
            loadProperties.getCapabilities().getCapability(resourceData, DisplayTypeCapability.class)
                    .setDisplayType(displayType);
        }
        constructResource(selection, (ResourceType) null);
    }

    @Override
    public List<ProductBrowserLabel> getLabels(String[] selection) {
        if (selection.length == 0) {
            return Collections.singletonList(new ProductBrowserLabel(displayName, displayName));
        }
        return populateData(selection);
    }

    @Override
    public String getProductInfo(String[] selection) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(displayName);
        for (int i = 1; i < selection.length; i++) {
            stringBuilder.append("\n");
            stringBuilder.append(selection[i]);
        }
        return stringBuilder.toString();
    }

}