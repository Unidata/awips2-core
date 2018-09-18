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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.viz.core.IDisplayPane;
import com.raytheon.uf.viz.core.drawables.IRenderableDisplay;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.viz.ui.UiUtil;
import com.raytheon.viz.ui.editor.AbstractEditor;
import com.raytheon.viz.ui.perspectives.AbstractVizPerspectiveManager;
import com.raytheon.viz.ui.perspectives.VizPerspectiveListener;

/**
 * Creates a new editor using serialization
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 25, 2010           mschenke  Initial creation
 * Mar 19, 2013  1808     njensen   Perspective specific behavior takes priority
 * Sep 18, 2018  7443     bsteffen  Revert previous change.
 * 
 * </pre>
 * 
 * @author mschenke
 */

public class NewAbstractEditor extends AbstractHandler {
    private static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(NewAbstractEditor.class);

    @Override
    public AbstractEditor execute(ExecutionEvent event)
            throws ExecutionException {

        IEditorPart part = HandlerUtil.getActiveEditor(event);
        if (part instanceof AbstractEditor) {
            AbstractEditor editor = (AbstractEditor) part;
            List<IRenderableDisplay> displays = new ArrayList<>();
            for (IDisplayPane pane : editor.getDisplayPanes()) {
                IRenderableDisplay toClone = pane.getRenderableDisplay();
                IRenderableDisplay newDisplay = toClone.createNewDisplay();
                if (newDisplay != null) {
                    displays.add(newDisplay);
                }
            }
            AbstractEditor ae = UiUtil.createEditor(part.getSite().getId(),
                    displays.toArray(new IRenderableDisplay[displays.size()]));
            if (ae != null) {
                // Reset extents on renderable displays when getting new editor
                for (IDisplayPane pane : ae.getDisplayPanes()) {
                    pane.getRenderableDisplay().getView().getExtent().reset();
                    pane.getRenderableDisplay()
                            .scaleToClientArea(pane.getBounds());
                }
            }
            return ae;
        } else {
            AbstractVizPerspectiveManager mgr = VizPerspectiveListener
                    .getCurrentPerspectiveManager();
            String msg = "Opening new empty editor not supported by perspective";
            if (mgr != null) {
                AbstractEditor ae = mgr.openNewEditor();
                if (ae != null) {
                    return ae;
                }
                msg += ": " + mgr.getPerspectiveId();
            }
            statusHandler.handle(Priority.PROBLEM, msg,
                    new VizException("Operation not supported"));
        }
        return null;
    }
}
