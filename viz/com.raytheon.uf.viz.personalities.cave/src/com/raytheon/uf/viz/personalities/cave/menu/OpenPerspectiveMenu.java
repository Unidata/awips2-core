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
package com.raytheon.uf.viz.personalities.cave.menu;

import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.actions.PerspectiveMenu;

import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.viz.core.mode.CAVEMode;

/**
 * Menu to show perspective list for opening perspectives
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * May 12, 2011            mschenke     Initial creation
 * Aug 31, 2015 17970      yteng        Disable switch to GFE if not in real-time
 * 
 * </pre>
 * 
 * @author mschenke
 * @version 1.0
 */

public class OpenPerspectiveMenu extends PerspectiveMenu {

    public OpenPerspectiveMenu() {
        super(PlatformUI.getWorkbench().getActiveWorkbenchWindow(),
                "Perspective");
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.actions.PerspectiveMenu#run(org.eclipse.ui.
     * IPerspectiveDescriptor)
     */
    @Override
    protected void run(IPerspectiveDescriptor desc) {
        if (desc != null) {
            if (CAVEMode.getMode().equals(CAVEMode.OPERATIONAL) &&
                    !SimulatedTime.getSystemTime().isRealTime() &&
                    !CAVEMode.getFlagInDRT() &&
                    desc.getId().equals("com.raytheon.viz.ui.GFEPerspective")) {
                UFStatus.getHandler().handle(
                        Priority.WARN,
                        "GFE cannot be launched while CAVE is in OPERATIONAL mode and CAVE clock is not set to real-time.");
                return;
            }

            try {
                PlatformUI.getWorkbench().showPerspective(desc.getId(),
                        getWindow());
            } catch (WorkbenchException e) {
                UFStatus.getHandler().handle(
                        Priority.PROBLEM,
                        "Error opening perspective (" + desc.getId() + "): "
                                + e.getLocalizedMessage(), e);
            }
        }
    }

}
