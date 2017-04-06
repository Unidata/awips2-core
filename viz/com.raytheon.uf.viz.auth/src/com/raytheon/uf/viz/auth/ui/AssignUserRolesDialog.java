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
package com.raytheon.uf.viz.auth.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;
import com.raytheon.viz.ui.widgets.duallist.IUpdate;

/**
 * Assign User Roles Dialog
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 26, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class AssignUserRolesDialog extends CaveSWTDialog {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private String user;

    private RolesAndPermissions rolesAndPermissions;

    private DualList rolesDL;

    private Label errorLabel;

    private Button saveButton;

    private boolean dirty;

    /**
     * Constructor
     *
     * @param parent
     * @param user
     *            name of user to be edited
     * @param rolesAndPermissions
     *            roles and permissions data
     */
    public AssignUserRolesDialog(Shell parent, String user,
            RolesAndPermissions rolesAndPermissions) {
        super(parent, SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.PRIMARY_MODAL,
                CAVE.PERSPECTIVE_INDEPENDENT | CAVE.MODE_INDEPENDENT);
        this.user = user;
        this.rolesAndPermissions = rolesAndPermissions;

        setText("Assign User Roles");

        setReturnValue(false);
    }

    /**
     * @return the dirty
     */
    private boolean isDirty() {
        return dirty;
    }

    private boolean isValid() {
        boolean valid = rolesDL.getSelectedListItems().length > 0;

        if (valid) {
            errorLabel.setText("");
        } else {
            errorLabel.setText("At least one role must be assigned!");
        }

        return valid;
    }

    /**
     * @param dirty
     *            the dirty to set
     */
    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        boolean valid = isValid();
        saveButton.setEnabled(valid && dirty);
    }

    @Override
    protected void initializeComponents(Shell shell) {
        GridLayout layout = new GridLayout(1, false);
        shell.setLayout(layout);

        Label label = new Label(shell, SWT.CENTER);
        GridData gridData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        label.setLayoutData(gridData);
        label.setText("Assign roles for user: " + user);

        FontData fd = label.getFont().getFontData()[0];
        Font font = new Font(getDisplay(), new FontData(fd.getName(),
                fd.getHeight(), fd.getStyle() | SWT.BOLD));
        label.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                font.dispose();
            }
        });
        label.setFont(font);

        DualListConfig config = new DualListConfig();
        config.setAvailableListLabel("Available Roles:");
        config.setSelectedListLabel("Selected Roles:");
        config.setListWidthInChars(32);
        config.setVisibleItems(12);
        config.setSortList(true);

        config.setFullList(
                new ArrayList<String>(rolesAndPermissions.getRoles().keySet()));
        config.setSelectedList(new ArrayList<String>(
                rolesAndPermissions.getUsers().get(user)));

        rolesDL = new DualList(shell, SWT.NONE, config, new IUpdate() {
            @Override
            public void selectionChanged() {
                setDirty(true);
            }

            @Override
            public void hasEntries(boolean entries) {
                // do nothing
            }
        });
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        rolesDL.setLayoutData(gridData);

        errorLabel = new Label(shell, SWT.LEFT);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        errorLabel.setLayoutData(gridData);
        errorLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));

        Composite buttonComp = new Composite(shell, SWT.NONE);
        gridData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        buttonComp.setLayoutData(gridData);
        buttonComp.setLayout(new GridLayout(2, true));

        saveButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        saveButton.setLayoutData(gridData);
        saveButton.setText("Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveChanges();
            }
        });

        Button closeButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        closeButton.setLayoutData(gridData);
        closeButton.setText("Close");
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });

        setDirty(false);
    }

    @Override
    public boolean shouldClose() {
        if (isDirty() && isValid()) {
            MessageDialog dlg = new MessageDialog(shell, "Save Changes?", null,
                    "User roles have been modified. Save changes?",
                    MessageDialog.QUESTION_WITH_CANCEL, 2, "No", "Cancel",
                    "Yes");

            switch (dlg.open()) {
            case 0:
                // No, discard changes
                return true;

            case 2:
                // Yes, save changes
                return saveChanges();

            default:
                // Do not close
                return false;
            }
        } else {
            return true;
        }
    }

    private boolean saveChanges() {
        try {
            setReturnValue(true);
            Set<String> roles = new HashSet<>(
                    Arrays.asList(rolesDL.getSelectedListItems()));
            rolesAndPermissions.updateUser(user, roles);

            setDirty(false);
            return true;

        } catch (Throwable e) {
            statusHandler.error(
                    "Error saving changes: " + e.getLocalizedMessage(), e);
        }
        return false;
    }
}
