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

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.services.IServiceLocator;

import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
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
public class ImagingAction extends AbstractRightClickAction {
    private ImagingDialog dialog;

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#run()
     */
    @Override
    public void run() {
        AbstractVizResource<?, ?> rsc = getTopMostSelectedResource();
        if (true) {
            IServiceLocator services = null;
            if (container instanceof IServiceLocator) {
                services = (IServiceLocator) container;

            } else {
                services = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            }

            IHandlerService handlerService = services
                    .getService(IHandlerService.class);
            ICommandService commandService = services
                    .getService(ICommandService.class);

            Command command = commandService
                    .getCommand("com.raytheon.viz.ui.imageProperties");
            if (command == null) {
                return;
            }


            IEvaluationContext context = handlerService
                    .createContextSnapshot(true);
            if (rsc != null) {
                context.addVariable(AbstractVizResource.class.getName(), rsc);
            }


            ParameterizedCommand parameterizedCommand = ParameterizedCommand
                    .generateCommand(command, null);
            try {
                handlerService.executeCommandInContext(parameterizedCommand,
                        null, context);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.action.Action#getText()
     */
    @Override
    public String getText() {
        return "Image Properties";
    }

}
