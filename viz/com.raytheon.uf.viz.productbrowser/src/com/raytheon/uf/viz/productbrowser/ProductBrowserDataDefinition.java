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

import java.util.Collection;
import java.util.List;

import com.raytheon.uf.viz.core.rsc.DisplayType;

/**
 * 
 * Interface for adding new data types to the product browser.
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
public interface ProductBrowserDataDefinition {

    /**
     * @return true if there is data available for this defintion, false
     *         otherwise. False causes nothing to display in the product browser
     *         for this definition.
     */
    public boolean checkAvailability();

    /**
     * Given a user selection return all the Labels that should be displayed.
     * This is called whenever the user expands on a TreeItem to get a listing
     * of the child items that should be displayed.
     * 
     * When the product browser is first opened, the selection will be empty and
     * the returned list should contain a single item for the definition. For
     * any subsequent calls the first item in the selection specifies the
     * definition.
     */
    public List<ProductBrowserLabel> getLabels(String[] selection);

    /**
     * Given a current user selection, provide a brief description of the
     * selection to be displayed to the user.
     */
    public String getProductInfo(String[] selection);

    /**
     * Get the display types that are valid for a selection. If a data type does
     * not have a concept of display types then it should return an empty
     * collection.
     */
    public Collection<DisplayType> getValidDisplayTypes(String[] selection);

    /**
     * Given a "product" selection and a display type(which may be null) this
     * method should load a resource on the dispaly.
     */
    public void loadResource(String[] selection, DisplayType displayType);

}
