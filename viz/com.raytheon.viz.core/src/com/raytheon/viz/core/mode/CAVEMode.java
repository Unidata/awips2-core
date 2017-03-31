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

package com.raytheon.viz.core.mode;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.viz.core.ProgramArguments;

/**
 * CAVEMode.
 * 
 * Holds the constants that define the CAVE mode.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date       	Ticket#		Engineer	Description
 * ----------	----------	-----------	--------------------------
 * 12/20/07     561         Dan Fitch    Initial Creation.
 *  5/31/16                 mjames@ucar  Always run in practice mode.
 * </pre>
 * 
 * @author Dan Fitch
 * @version 1
 */
public enum CAVEMode {

    OPERATIONAL("Operational"), PRACTICE("Practice"), TEST("Test");

    private String displayString;

    private CAVEMode(String displayString) {
        this.displayString = displayString;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Enum#toString()
     */
    @Override
    public String toString() {
        return this.displayString;
    }

    private static Color CAVE_BG_COLOR;

    private static Color CAVE_FG_COLOR;

    public static CAVEMode getMode() {
        return CAVEMode.PRACTICE;
    }

    public static Color getBackgroundColor() {
    	if (CAVE_BG_COLOR == null) {
            CAVE_BG_COLOR = Display.getDefault().getSystemColor(
                    SWT.COLOR_WIDGET_BACKGROUND);
        }
        return CAVE_BG_COLOR;
    }

    public static Color getForegroundColor() {
    	if (CAVE_FG_COLOR == null) {
            CAVE_FG_COLOR = Display.getDefault().getSystemColor(
                    SWT.COLOR_WIDGET_FOREGROUND);
        }
        return CAVE_FG_COLOR;
    }

}
