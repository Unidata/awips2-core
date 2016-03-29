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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * This is a message box that will only block the parent shell and not the
 * entire application.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Apr 7, 2011             lvenable    Initial creation
 * Oct 18, 2012 1229       rferrel     Made dialog non-blocking.
 * Nov 13, 2015 4946       mapeters    Use scrollable StyledText for long messages.
 * Jan 15, 2016 5054       randerso    Change to subclass CaveSWTDialog
 * Jan 27, 2016 4946       randerso    Make text scrollable for long messages
 * Mar 28, 2016 5482       randerso    Fix fixed size buttons
 * 
 * </pre>
 * 
 * @author lvenable
 * @version 1.0
 */
public class SWTMessageBox extends CaveSWTDialog {

    /**
     * The SWT icon to be displayed.
     */
    private int swtIcon = 0;

    /**
     * The SWT styles to determine the icon and buttons to use.
     */
    private int swtMessageBoxStyle = 0;

    /**
     * Message to be displayed.
     */
    private String message = "";

    private Text messageText;

    /**
     * Constructor.
     * 
     * @param parent
     *            Parent shell.
     * @param title
     *            Dialog title.
     * @param message
     *            Message.
     * @param swtMessageBoxStyle
     *            Style for icon and buttons.
     * 
     */
    public SWTMessageBox(Shell parent, String title, String message,
            int swtMessageBoxStyle) {
        super(parent, SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL, CAVE.DO_NOT_BLOCK);
        this.swtMessageBoxStyle = swtMessageBoxStyle;
        this.message = message;
        setText(title);
    }

    /**
     * Constructor.
     * 
     * @param display
     * 
     * @param title
     *            Dialog title.
     * @param message
     *            Message.
     * @param swtMessageBoxStyle
     *            Style for icon and buttons.
     */
    public SWTMessageBox(Display display, String title, String message,
            int swtMessageBoxStyle) {
        super(display, SWT.DIALOG_TRIM | SWT.MODELESS, CAVE.DO_NOT_BLOCK);
        this.swtMessageBoxStyle = swtMessageBoxStyle;
        this.message = message;
        setText(title);
    }

    @Override
    protected Layout constructShellLayout() {
        // Create the main layout for the shell.
        GridLayout mainLayout = new GridLayout(1, false);
        mainLayout.verticalSpacing = 15;
        return mainLayout;
    }

    @Override
    protected Object constructShellLayoutData() {
        return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
    }

    @Override
    protected void initializeComponents(Shell shell) {
        determineLabelIcon();
        createIconAndMessage();
        createBottomButtons();

        shell.layout(true, true);
        shell.pack(true);

        int desiredHeight = messageText.getLineHeight() * 10;
        int height = messageText.getClientArea().height;
        if (height <= desiredHeight) {
            messageText.getVerticalBar().setVisible(false);
        } else {
            GridData gd = (GridData) messageText.getLayoutData();
            int heightHint = messageText.computeTrim(0, 0, gd.widthHint,
                    desiredHeight).height;
            gd.heightHint = heightHint;
            messageText.setBackground(getDisplay().getSystemColor(
                    SWT.COLOR_WHITE));
        }

    }

    /**
     * Create the icon label (if needed) and the message field.
     */
    private void createIconAndMessage() {
        int numberOfGridCells = 1;
        if (swtIcon != 0) {
            numberOfGridCells = 2;
        }
        Composite iconMessageComp = new Composite(shell, SWT.NONE);
        GridLayout gl = new GridLayout(numberOfGridCells, false);
        gl.horizontalSpacing = 20;
        iconMessageComp.setLayout(gl);
        if (swtIcon != 0) {
            Label iconLbl = new Label(iconMessageComp, SWT.NONE);
            iconLbl.setImage(getDisplay().getSystemImage(swtIcon));
        }
        GridData gd = new GridData(SWT.FILL, SWT.CENTER, false, true);
        gd.widthHint = 300;

        messageText = new Text(iconMessageComp, SWT.READ_ONLY | SWT.WRAP
                | SWT.MULTI | SWT.V_SCROLL);
        messageText.setText(message);
        messageText.setLayoutData(gd);
        messageText.setBackground(shell.getDisplay().getSystemColor(
                SWT.COLOR_WIDGET_BACKGROUND));
    }

    /**
     * Create the bottom buttons.
     */
    private void createBottomButtons() {
        int buttonWidth = shell.getDisplay().getDPI().x;
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        Composite mainButtonComp = new Composite(shell, SWT.NONE);
        mainButtonComp.setLayout(new GridLayout(getButtonCount(), false));
        mainButtonComp.setLayoutData(gd);
        if (hasStyleAttributes(SWT.OK)) {
            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            gd.minimumWidth = buttonWidth;
            Button okBtn = new Button(mainButtonComp, SWT.PUSH);
            okBtn.setText("OK");
            okBtn.setLayoutData(gd);
            okBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setReturnValue(new Integer(SWT.OK));
                    close();
                }
            });

            if (hasStyleAttributes(SWT.CANCEL)) {
                setReturnValue(new Integer(SWT.CANCEL));
                gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
                gd.minimumWidth = buttonWidth;
                Button cancelBtn = new Button(mainButtonComp, SWT.PUSH);
                cancelBtn.setText("Cancel");
                cancelBtn.setLayoutData(gd);
                cancelBtn.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        setReturnValue(new Integer(SWT.CANCEL));
                        close();
                    }
                });
            }
        } else if (hasStyleAttributes(SWT.YES | SWT.NO)) {
            setReturnValue(new Integer(SWT.NO));
            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            gd.minimumWidth = buttonWidth;
            Button yesBtn = new Button(mainButtonComp, SWT.PUSH);
            yesBtn.setText("Yes");
            yesBtn.setLayoutData(gd);
            yesBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setReturnValue(new Integer(SWT.YES));
                    close();
                }
            });

            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            gd.minimumWidth = buttonWidth;
            Button noBtn = new Button(mainButtonComp, SWT.PUSH);
            noBtn.setText("No");
            noBtn.setLayoutData(gd);
            noBtn.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    setReturnValue(new Integer(SWT.NO));
                    close();
                }
            });
        }
    }

    /**
     * Determine the icon to be displayed in the icon label.
     */
    private void determineLabelIcon() {
        /*
         * Find the SWT icon to use
         */
        if (hasStyleAttributes(SWT.ICON_ERROR)) {
            swtIcon = SWT.ICON_ERROR;
        } else if (hasStyleAttributes(SWT.ICON_INFORMATION)) {
            swtIcon = SWT.ICON_INFORMATION;
        } else if (hasStyleAttributes(SWT.ICON_QUESTION)) {
            swtIcon = SWT.ICON_QUESTION;
        } else if (hasStyleAttributes(SWT.ICON_WARNING)) {
            swtIcon = SWT.ICON_WARNING;
        }
    }

    /**
     * Get the number of buttons to be displayed.
     * 
     * @return
     */
    private int getButtonCount() {
        int btnCount;
        if (hasStyleAttributes(SWT.OK | SWT.CANCEL)) {
            btnCount = 2;
        } else if (hasStyleAttributes(SWT.YES | SWT.NO)) {
            btnCount = 2;
        } else {
            btnCount = 1;
        }
        return btnCount;
    }

    /**
     * Determine if the style contains a certain attribute.
     * 
     * @param attribute
     *            Attribute.
     * @return True if the style contains an attribute.
     */
    private boolean hasStyleAttributes(int attribute) {
        return (swtMessageBoxStyle & attribute) == attribute;
    }
}
