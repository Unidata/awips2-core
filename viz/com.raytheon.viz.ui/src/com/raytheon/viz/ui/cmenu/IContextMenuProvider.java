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
package com.raytheon.viz.ui.cmenu;

import org.eclipse.jface.action.IMenuManager;

/**
 * Interfaces for classes that will fill the context menu by theirselves. It is
 * up to the caller of the interface to make sure that they are the only
 * interface providing to the context menu. Meant to allow classes to specify
 * they want to be the only entries in the context menu when it is being
 * populated
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 20, 2010            mschenke     Initial creation
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public interface IContextMenuProvider {

    /**
     * Allow the resource to provide all the menu items at the given x,y
     * 
     * @param menuManager
     *            the display pane's context menu manager
     * @param x
     *            the x location in widget space of the click
     * @param y
     *            the y location in widget space of the click
     * @return resource-specific editor contributions
     */
    public abstract void provideContextMenuItems(IMenuManager menuManager,
            int x, int y);

}
