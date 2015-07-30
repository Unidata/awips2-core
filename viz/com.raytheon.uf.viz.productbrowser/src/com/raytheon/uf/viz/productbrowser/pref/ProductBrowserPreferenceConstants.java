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
package com.raytheon.uf.viz.productbrowser.pref;

import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference;
import com.raytheon.uf.viz.productbrowser.ProductBrowserPreference.PreferenceType;

/**
 * 
 * Many {@link PreferenceBasedDataDefinition}s use similar preferences so this
 * class provides some convenience constants/methods for reusing common
 * preferences.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jun 02, 2015  4153     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class ProductBrowserPreferenceConstants {

    /**
     * the preference to enable or disable the definition
     */
    public static final String ENABLE_PLUGIN = "Enable";

    /**
     * the preference to enable or disable formatting of data
     */
    public static final String FORMAT_DATA = "Format Data";

    /**
     * The preference to rearrange the recursion order of the labels.
     */
    public static final String ORDER = "Order";

    /**
     * Create a preference for the {@link #ENABLE_PLUGIN} preference, defaults
     * to true.
     */
    public static ProductBrowserPreference createEnabledPreference() {
        ProductBrowserPreference preference = new ProductBrowserPreference();
        preference.setLabel(ENABLE_PLUGIN);
        preference.setPreferenceType(PreferenceType.BOOLEAN);
        preference.setTooltip("Enables this datatype in the product browser");
        preference.setValue(true);
        return preference;
    }

    /**
     * Create a preference for the {@link #ORDER} preference, must be provided
     * with a default order.
     */
    public static ProductBrowserPreference createOrderPreference(String[] defaultOrder) {
        ProductBrowserPreference preference = new ProductBrowserPreference();
        preference.setLabel(ORDER);
        preference.setPreferenceType(PreferenceType.STRING_ARRAY);
        preference.setValue(defaultOrder);
        preference.setTooltip("Change the order of items in the product browser tree");
        return preference;
    }

    /**
     * Create a preference for the {@link #FORMAT_DATA} preference, defaults to
     * true.
     */
    public static ProductBrowserPreference createFormatPreference() {
        ProductBrowserPreference preference = new ProductBrowserPreference();
        preference.setLabel(FORMAT_DATA);
        preference.setPreferenceType(PreferenceType.BOOLEAN);
        preference.setTooltip("Enables prettier names of parameters in the product browser (instead of the raw names)");
        preference.setValue(true);
        return preference;
    }
}
