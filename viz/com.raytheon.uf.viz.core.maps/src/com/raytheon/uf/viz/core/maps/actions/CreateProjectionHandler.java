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
package com.raytheon.uf.viz.core.maps.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.raytheon.uf.viz.core.maps.dialogs.CreateProjectionDialog;

/**
 * Handler class for controlling the Create Projection dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 16, 2012  1229     rferrel   Made dialog non-blocking.
 * Apr 25, 2016  5579     bsteffen  Extend AbstractMapHandler to enable only
 *                                  when a map is active.
 * 
 * </pre>
 * 
 * @author rferrel
 */
public class CreateProjectionHandler extends AbstractMapHandler {

    private CreateProjectionDialog dlg;

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow()
                .getShell();

        if (dlg == null || dlg.getShell() == null || dlg.isDisposed()) {
            dlg = new CreateProjectionDialog(shell);
            dlg.setBlockOnOpen(false);
            dlg.open();
        } else {
            dlg.bringToTop();
        }

        return null;
    }

}
