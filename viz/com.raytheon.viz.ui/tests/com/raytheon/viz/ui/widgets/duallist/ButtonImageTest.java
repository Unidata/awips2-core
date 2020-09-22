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
package com.raytheon.viz.ui.widgets.duallist;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.viz.ui.widgets.duallist.ButtonImages.ButtonImage;

/**
 * Test main to validate ButtonImages appearance
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Sep 22, 2020  7926     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class ButtonImageTest {

    /**
     * @param args
     */
    public static void main(String[] args) {

        Display display = new Display();
        Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
        shell.setLayout(new GridLayout(1, true));
        shell.setText("Button Image Test");

        ButtonImages images = new ButtonImages(shell);
        for (ButtonImage buttonType : ButtonImage.values()) {
            Button button = new Button(shell, SWT.PUSH);
            button.setImage(images.getImage(buttonType));
//            button.setText(buttonType.name());
            button.setLayoutData(
                    new GridData(SWT.FILL, SWT.DEFAULT, true, false));
        }

        shell.pack();
        shell.setMinimumSize(shell.getSize());
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) {
                display.sleep();
            }
        }
    }

}
