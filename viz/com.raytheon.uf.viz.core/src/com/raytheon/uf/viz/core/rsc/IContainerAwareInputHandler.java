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
package com.raytheon.uf.viz.core.rsc;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;

/**
 * 
 * Interface for an {@link IInputHandler} that needs to be notified when it is
 * used with a different {@link IDisplayPaneContainer}.
 * {@link #setContainer(IDisplayPaneContainer)} is called automatically by the
 * container when the handler is added.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 08, 2016  2676     bsteffen  Initial creation
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public interface IContainerAwareInputHandler extends IInputHandler {

    public void setContainer(IDisplayPaneContainer container);

}
