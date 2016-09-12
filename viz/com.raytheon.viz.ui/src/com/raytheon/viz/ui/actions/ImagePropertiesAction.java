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

package com.raytheon.viz.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;

import com.raytheon.uf.viz.core.IDisplayPaneContainer;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.viz.ui.EditorUtil;
import com.raytheon.viz.ui.VizWorkbenchManager;
import com.raytheon.viz.ui.dialogs.ImagingDialog;

/**
 * Get imaging dialog
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Nov 22, 2006           chammack  Initial Creation.
 * Oct 17, 2012  1229     rferrel   Changes for non-blocking ImagingDialog.
 * Sep 12, 2016  3241     bsteffen  Remove image combination from core imaging
 *                                  dialog
 * 
 * </pre>
 * 
 * @author chammack
 */
public class ImagePropertiesAction extends AbstractHandler {

    private static ImagingDialog dialog = null;

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
        IDisplayPaneContainer container = EditorUtil.getActiveVizContainer();

        AbstractVizResource<?, ?> selected = getSelectedResource(arg0);

        if (container == null && selected == null) {
            return null;
        }

        if (dialog == null || dialog.getShell() == null
                || dialog.isDisposed()) {
            if (selected != null) {
                dialog = new ImagingDialog(VizWorkbenchManager.getInstance()
                        .getCurrentWindow().getShell(), selected);
            } else {
                dialog = new ImagingDialog(VizWorkbenchManager.getInstance()
                        .getCurrentWindow().getShell(), container);
            }
            // initalize
            dialog.open();
        } else {
            if (selected != null) {
                dialog.setResource(selected);
            } else {
                dialog.setContainer(container);
            }
            dialog.refreshComponents();
            dialog.bringToTop();
        }

        return null;
    }

    protected static AbstractVizResource<?, ?> getSelectedResource(
            ExecutionEvent event) {
        Object contextObj = event.getApplicationContext();
        if (contextObj instanceof IEvaluationContext) {
            IEvaluationContext context = (IEvaluationContext) contextObj;
            Object componentObj = context
                    .getVariable(AbstractVizResource.class.getName());
            if (componentObj instanceof AbstractVizResource) {
                return (AbstractVizResource<?, ?>) componentObj;
            }
        }
        return null;
    }
}
