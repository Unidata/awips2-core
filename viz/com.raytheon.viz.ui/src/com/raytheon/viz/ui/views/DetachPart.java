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

import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.services.IServiceLocator;

/**
 * Provides a pair of utility methods for detaching and reattaching an
 * {@link IWorkbenchPart} from the main workbench window by creating a new
 * window for the part.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jan 07, 2016  5190     bsteffen  Initial creation
 *
 * </pre>
 *
 * @author bsteffen
 */
public class DetachPart {

    /**
     * Remove a part from it's current container and put it in a new window.
     */
    public static void detach(IWorkbenchPart workbenchPart) {
        IServiceLocator services = workbenchPart.getSite();
        EModelService modelService = services.getService(EModelService.class);
        MPart modelPart = services.getService(MPart.class);

        /* First try to detach in a window the same size as the part. */
        Object rawWidget = modelPart.getWidget();
        if (rawWidget instanceof Control) {
            Control control = (Control) rawWidget;
            Rectangle bounds = control.getBounds();
            if (!bounds.isEmpty()) {
                Point corner = control.getParent().toDisplay(bounds.x,
                        bounds.y);
                modelService.detach(modelPart, corner.x, corner.y, bounds.width,
                        bounds.height);
                return;
            }
        }

        /*
         * If the part does not have a size yet then make it the same size as
         * the editor area or half the editor area if it is a view.
         */
        MWindow window = services.getService(MWindow.class);
        MUIElement editors = modelService.find(IPageLayout.ID_EDITOR_AREA,
                window);
        if (editors != null) {
            rawWidget = editors.getWidget();
            if (rawWidget instanceof Control) {
                Control control = (Control) rawWidget;
                Rectangle bounds = control.getBounds();
                if (!bounds.isEmpty()) {
                    Point corner = control.getParent().toDisplay(bounds.x,
                            bounds.y);
                    if (workbenchPart instanceof IViewPart) {
                        corner.x += bounds.width / 2;
                        bounds.width /= 2;
                    }
                    modelService.detach(modelPart, corner.x + bounds.width,
                            corner.y, bounds.width, bounds.height);
                    return;
                }
            }
        }

        /* if all else fails just open a generic window */
        if (workbenchPart instanceof IViewPart) {
            modelService.detach(modelPart, 0, 0, 300, 600);
        } else {
            modelService.detach(modelPart, 0, 0, 300, 300);
        }
    }

    /**
     * Remove a part from it's current container and put it in the default
     * position.
     */
    public static void attach(IWorkbenchPart workbenchPart) {
        IServiceLocator services = workbenchPart.getSite();
        MPart modelPart = services.getService(MPart.class);
        MPlaceholder placeholder = modelPart.getCurSharedRef();

        placeholder.getParent().getChildren().remove(placeholder);
        EPartService partService = services.getService(EPartService.class);
        partService.showPart(modelPart, PartState.ACTIVATE);
    }
}
