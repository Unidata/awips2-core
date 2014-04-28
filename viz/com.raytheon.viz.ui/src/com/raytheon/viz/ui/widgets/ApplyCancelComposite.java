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
package com.raytheon.viz.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;

/**
 * Composite holding the apply and cancel buttons.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 07, 2013   2180     mpduff      Initial creation.
 * Oct 03, 2013   2386     mpduff      Added apply failed message.
 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class ApplyCancelComposite extends Composite implements IEnableAction {
    /** Button Width. */
    private final int buttonWidth = 70;

    /** Apply button */
    private Button applyButton;

    /** Cancel button */
    private Button cancelButton;

    /** Action callback */
    private final IApplyCancelAction callback;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent Composite
     * @param style
     *            Style bits
     * @param callback
     *            Callback action
     */
    public ApplyCancelComposite(Composite parent, int style,
            IApplyCancelAction callback) {
        super(parent, style);
        this.callback = callback;
        init();
    }

    /**
     * Initialize the class
     */
    private void init() {
        GridLayout gl = new GridLayout(1, false);
        GridData gd = new GridData(SWT.RIGHT, SWT.FILL, true, true);
        this.setLayout(gl);
        this.setLayoutData(gd);

        GridData actionData = new GridData(SWT.DEFAULT, SWT.BOTTOM, false, true);
        GridLayout actionLayout = new GridLayout(2, false);
        Composite actionComp = new Composite(this, SWT.NONE);
        actionComp.setLayout(actionLayout);
        actionComp.setLayoutData(actionData);

        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);
        btnData.horizontalAlignment = SWT.RIGHT;

        applyButton = new Button(actionComp, SWT.PUSH);
        applyButton.setText("Apply");
        applyButton.setLayoutData(btnData);
        applyButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                if (callback.apply()) {
                    enableButtons(false);
                } else {
                    MessageBox messageDialog = new MessageBox(getParent()
                            .getShell(), SWT.ERROR);
                    messageDialog.setText("Apply Failed");
                    messageDialog
                            .setMessage("The apply action failed.  See server logs for details.");
                    messageDialog.open();
                }
            }
        });

        btnData = new GridData(buttonWidth, SWT.DEFAULT);
        btnData.horizontalAlignment = SWT.RIGHT;

        cancelButton = new Button(actionComp, SWT.PUSH);
        cancelButton.setText("Cancel");
        cancelButton.setLayoutData(btnData);
        cancelButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                callback.cancel();
                enableButtons(false);
            }
        });

        enableButtons(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enableButtons(boolean enabled) {
        applyButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}