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
package com.raytheon.uf.viz.personalities.cave.dialog;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IPerspectiveRegistry;
import org.eclipse.ui.internal.activities.ws.ActivityMessages;
import org.eclipse.ui.internal.dialogs.SelectPerspectiveDialog;

/**
 * The SelectPerspectiveDialog overridden to remove the "Show All" button so
 * users cannot inadvertently open the Java, Debug, etc perspectives.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 06, 2016  5192      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 * @version 1.0
 */

public class PerspectiveChooserDialog extends SelectPerspectiveDialog {

    @SuppressWarnings("restriction")
    public PerspectiveChooserDialog(Shell parentShell, IPerspectiveRegistry perspReg) {
        super(parentShell, perspReg);
    }

    @SuppressWarnings("restriction")
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);

        Control[] children = composite.getChildren();
        for (Control child : children) {
            if (child instanceof Button) {
                Button b = (Button) child;
                if (ActivityMessages.Perspective_showAll.equals(b.getText())) {
                    b.dispose();
                    break;
                }
            }
        }

        return composite;
    }

}
