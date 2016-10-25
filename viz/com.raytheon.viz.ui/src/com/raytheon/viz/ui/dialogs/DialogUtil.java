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
package com.raytheon.viz.ui.dialogs;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

/**
 * Dialog Support Utilities
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Sep 21, 2016  5901     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 * @version 1.0
 */

public class DialogUtil {
    /*
     * Private constructor to prevent instantiation of this class as it just a
     * library of static methods
     */
    private DialogUtil() {
    }

    private static void centerInBounds(Shell shell, Rectangle bounds) {
        Point size = shell.getSize();
        int x = bounds.x + ((bounds.width - size.x) / 2);
        int y = bounds.y + ((bounds.height - size.y) / 2);

        shell.setLocation(x, y);
    }

    /**
     * Center a shell on the monitor containing the cursor
     *
     * @param shell
     *            the shell to be centered
     */
    public static void centerOnCurrentMonitor(Shell shell) {
        // center shell on monitor containing cursor
        Display display = shell.getDisplay();
        Monitor[] monitors = display.getMonitors();
        int monitor = 0;

        Point cursor = display.getCursorLocation();
        for (int i = 0; i < monitors.length; i++) {
            if (monitors[i].getBounds().contains(cursor)) {
                monitor = i;
                break;
            }
        }

        centerInBounds(shell, monitors[monitor].getBounds());
    }

    /**
     * Center a shell on it's parent
     *
     * @param parent
     *            the shell to be centered upon. This should be the parent shell
     *            or in the case of CAVE.INDEPENDENT_SHELLs the "pseudo parent"
     *            we want to use to position the dialog.
     * @param shell
     *            the shell to be centered
     */
    public static void centerOnParentShell(Shell parent, Shell shell) {
        centerInBounds(shell, parent.getBounds());
    }
}
