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
package com.raytheon.viz.ui.views;

import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;

/**
 * Empty implementation of part listener so that classes can choose which
 * methods to override and which to leave empty.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 26, 2014 3539       bclement     Initial creation
 * 
 * </pre>
 * 
 * @author bclement
 * @version 1.0
 */
public abstract class PartAdapter2 implements IPartListener2 {

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partActivated(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partActivated(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partBroughtToTop(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partBroughtToTop(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partClosed(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partClosed(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partDeactivated(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partDeactivated(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partOpened(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partOpened(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partHidden(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partHidden(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partVisible(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partVisible(IWorkbenchPartReference partRef) {
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.IPartListener2#partInputChanged(org.eclipse.ui.IWorkbenchPartReference)
     */
    @Override
    public void partInputChanged(IWorkbenchPartReference partRef) {
    }

}
