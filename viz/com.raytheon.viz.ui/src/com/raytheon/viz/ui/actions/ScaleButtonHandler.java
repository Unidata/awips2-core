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

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;

import com.raytheon.uf.viz.core.VizConstants;
import com.raytheon.uf.viz.core.globals.IGlobalChangedListener;

/**
 * Updates the scale
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 23, 2007            randerso    Initial Creation.
 * Oct 10, 2013       2104 mschenke    Will truncate text if too long
 * Mar 31, 2016 5519       bsteffen    Keep toolbar text constant width.
 * 
 * </pre>
 * 
 * @author randerso
 * 
 */
public class ScaleButtonHandler extends AbstractGlobalsButtonHandler implements
        IElementUpdater, IGlobalChangedListener {

    public ScaleButtonHandler() {
        super(VizConstants.SCALE_ID);
    }

    @Override
    protected void updateGlobalValue(IWorkbenchWindow changedWindow,
            UIElement element, Object value) {
        String scale = (String) value;
        String tooltip = scale;
        HandlerTextSizer sizer = new HandlerTextSizer(Display.getCurrent());
        sizer.setMinCharacters(10);
        sizer.setMaxCharacters(10);
        String text = sizer.createAdjustedText(scale);
        sizer.dispose();
        element.setText(text);
        element.setTooltip("Scale: " + tooltip);
    }

}
