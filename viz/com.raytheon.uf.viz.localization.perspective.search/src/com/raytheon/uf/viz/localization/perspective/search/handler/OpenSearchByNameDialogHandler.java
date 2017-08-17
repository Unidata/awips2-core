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
package com.raytheon.uf.viz.localization.perspective.search.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.viz.localization.perspective.search.ui.LocalizationResourceSelectionDialog;
import com.raytheon.uf.viz.localization.perspective.service.LocalizationPerspectiveUtils;
import com.raytheon.uf.viz.localization.perspective.view.LocalizationFileEntryData;

/**
 * {@link IHandler} for the opening a
 * {@link LocalizationResourceSelectionDialog}.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Aug 17, 2017  6359     bsteffen  Initial creation
 * 
 * </pre>
 *
 * @author bsteffen
 */
public class OpenSearchByNameDialogHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Shell shell = HandlerUtil.getActiveShell(event);
        LocalizationResourceSelectionDialog dlg = new LocalizationResourceSelectionDialog(
                shell);
        dlg.open();
        Object result = dlg.getFirstResult();
        if (result != null) {
            IWorkbenchWindow window = HandlerUtil
                    .getActiveWorkbenchWindow(event);
            IWorkbenchPage page = window.getActivePage();
            LocalizationPerspectiveUtils.getService(page)
                    .openFile(((LocalizationFileEntryData) result).getFile());
        }
        return result;
    }

}
