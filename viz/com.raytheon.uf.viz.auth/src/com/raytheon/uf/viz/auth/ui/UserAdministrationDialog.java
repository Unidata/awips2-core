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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.RolesAndPermissions.Role;
import com.raytheon.uf.common.auth.req.GetRolesAndPermissionsRequest;
import com.raytheon.uf.common.auth.req.SaveRolesAndPermissionsRequest;
import com.raytheon.uf.common.auth.util.PermissionDescriptionBuilder;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.requests.ThriftClient;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * User Administration Dialog
 *
 * This dialog allows a system administrator to edit user roles and permissions
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 24, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class UserAdministrationDialog extends CaveSWTDialog {
    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private static final Pattern VALID_ROLENAME_REGEX = Pattern
            .compile("[a-zA-Z_][a-zA-Z0-9_\\-\\.]{0,63}");

    private RolesAndPermissions rolesAndPermissions;

    private boolean dirty = false;

    private List userList;

    private List userRolesList;

    private Button saveButton;

    private List rolesList;

    private Text userRoleDescriptionText;

    private List rolePermissionsList;

    private Button editRoleButton;

    private Button copyRoleButton;

    private Button deleteRoleButton;

    private Group roleDescGroup;

    private Text roleDescriptionText;

    private Text permissionDescriptionText;

    /**
     * Constructor
     *
     * @param parent
     * @throws VizException
     */
    public UserAdministrationDialog(Shell parent) throws VizException {
        super(parent, SWT.TITLE | SWT.BORDER | SWT.RESIZE, CAVE.DO_NOT_BLOCK
                | CAVE.PERSPECTIVE_INDEPENDENT | CAVE.MODE_INDEPENDENT);

        setText("User Administration");

        GetRolesAndPermissionsRequest request = new GetRolesAndPermissionsRequest();
        try {
            rolesAndPermissions = (RolesAndPermissions) ThriftClient
                    .sendRequest(request);
        } catch (Throwable e) {
            throw new VizException("Unable to retrieve roles and permissions: "
                    + e.getLocalizedMessage(), e);
        }
    }

    @Override
    protected void initializeComponents(Shell shell) {
        shell.setLayout(new GridLayout(1, false));

        TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tabFolder.setLayoutData(gridData);

        createUserTab(tabFolder);
        createRolesTab(tabFolder);

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int i = tabFolder.getSelectionIndex();
                List list = (List) tabFolder.getItem(i).getData();
                list.showSelection();
            }
        });

        /* Create bottom buttons */
        Composite bottomComp = new Composite(shell, SWT.NONE);
        bottomComp.setLayout(new GridLayout(2, true));
        gridData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 3;
        bottomComp.setLayoutData(gridData);

        saveButton = new Button(bottomComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        saveButton.setLayoutData(gridData);
        saveButton.setText("Save");
        saveButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                saveChanges();
            }
        });

        Button closeButton = new Button(bottomComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        closeButton.setLayoutData(gridData);
        closeButton.setText("Close");
        closeButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                close();
            }
        });

        initDialog();
    }

    private void createUserTab(TabFolder tabFolder) {
        TabItem userTab = new TabItem(tabFolder, SWT.NONE);
        userTab.setText("Assign User Roles");

        Composite userTabComp = new Composite(tabFolder, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        userTabComp.setLayoutData(gridData);
        GridLayout layout = new GridLayout(2, false);
        userTabComp.setLayout(layout);
        userTab.setControl(userTabComp);

        /* Create user list */
        Group userGroup = new Group(userTabComp, SWT.BORDER);
        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        userGroup.setLayout(layout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        userGroup.setLayoutData(gridData);
        userGroup.setText("Select User:");

        userList = new List(userGroup,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
        userTab.setData(userList);

        GC gc = new GC(userList);
        int avgCharWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();

        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = avgCharWidth * 32;
        gridData.heightHint = userList.getItemHeight() * 12;
        userList.setLayoutData(gridData);
        userList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateSelectedUser();
            }
        });

        /* Create buttons */
        Label label = new Label(userGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        label.setLayoutData(gridData);

        Composite buttonComp = new Composite(userGroup, SWT.NONE);
        gridData = new GridData(SWT.CENTER, SWT.CENTER, true, false);
        buttonComp.setLayoutData(gridData);
        buttonComp.setLayout(new GridLayout(2, true));

        Button editUserButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.minimumWidth = getDisplay().getDPI().x * 7 / 8;
        editUserButton.setLayoutData(gridData);
        editUserButton.setText("Edit...");
        editUserButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editUser();
            }
        });

        Button deleteUserButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        deleteUserButton.setLayoutData(gridData);
        deleteUserButton.setText("Delete");
        deleteUserButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteUser();
            }
        });

        /* Create roles list */
        Group rolesGroup = new Group(userTabComp, SWT.BORDER);
        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        rolesGroup.setLayout(layout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        rolesGroup.setLayoutData(gridData);
        rolesGroup.setText("Assigned Roles:");

        userRolesList = new List(rolesGroup,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = avgCharWidth * 32;
        gridData.heightHint = userRolesList.getItemHeight() * 12;
        userRolesList.setLayoutData(gridData);
        userRolesList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateUserRoleDescription();
            }
        });

        Group group = new Group(userTabComp, SWT.BORDER);
        group.setText("Role Description:");
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        group.setLayout(new GridLayout(1, false));

        userRoleDescriptionText = new Text(group, SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.heightHint = userRoleDescriptionText.getLineHeight() * 2;
        userRoleDescriptionText.setLayoutData(gridData);
        userRoleDescriptionText.setBackground(group.getBackground());
    }

    void createRolesTab(TabFolder tabFolder) {
        TabItem rolesTab = new TabItem(tabFolder, SWT.NONE);
        rolesTab.setText("Define Roles");
        Composite rolesTabComp = new Composite(tabFolder, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        rolesTabComp.setLayoutData(gridData);
        GridLayout layout = new GridLayout(2, true);
        rolesTabComp.setLayout(layout);
        rolesTab.setControl(rolesTabComp);

        /* Create roles list */
        Group rolesGroup = new Group(rolesTabComp, SWT.BORDER);
        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        rolesGroup.setLayout(layout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        rolesGroup.setLayoutData(gridData);
        rolesGroup.setText("Select Role:");

        rolesList = new List(rolesGroup, SWT.V_SCROLL | SWT.H_SCROLL);
        rolesTab.setData(rolesList);

        GC gc = new GC(userList);
        int avgCharWidth = gc.getFontMetrics().getAverageCharWidth();
        gc.dispose();

        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = avgCharWidth * 56;
        gridData.heightHint = rolesList.getItemHeight() * 12;
        rolesList.setLayoutData(gridData);
        rolesList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateSelectedRole();
            }
        });

        /* Create buttons */
        Label label = new Label(rolesGroup, SWT.SEPARATOR | SWT.HORIZONTAL);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        label.setLayoutData(gridData);

        Composite buttonComp = new Composite(rolesGroup, SWT.NONE);
        gridData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        buttonComp.setLayoutData(gridData);
        buttonComp.setLayout(new GridLayout(4, true));

        Button newRoleButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        newRoleButton.setLayoutData(gridData);
        newRoleButton.setText("New...");
        newRoleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                addRole();
            }
        });

        editRoleButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        editRoleButton.setLayoutData(gridData);
        editRoleButton.setText("Edit...");
        editRoleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                editRole();
            }
        });

        copyRoleButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        copyRoleButton.setLayoutData(gridData);
        copyRoleButton.setText("Copy...");
        copyRoleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                copyRole();
            }
        });

        deleteRoleButton = new Button(buttonComp, SWT.PUSH);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        deleteRoleButton.setLayoutData(gridData);
        deleteRoleButton.setText("Delete");
        deleteRoleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteRole();
            }
        });

        /* Create permissions list */
        Group permissionsGroup = new Group(rolesTabComp, SWT.BORDER);
        layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        permissionsGroup.setLayout(layout);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        permissionsGroup.setLayoutData(gridData);
        permissionsGroup.setText("Permissions:");

        rolePermissionsList = new List(permissionsGroup,
                SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        gridData.widthHint = avgCharWidth * 56;
        gridData.heightHint = rolePermissionsList.getItemHeight() * 12;
        rolePermissionsList.setLayoutData(gridData);

        roleDescGroup = new Group(rolesTabComp, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        roleDescGroup.setLayoutData(gridData);
        layout = new GridLayout(1, false);
        roleDescGroup.setLayout(layout);
        roleDescGroup.setText("Role Description:");

        roleDescriptionText = new Text(roleDescGroup,
                SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.heightHint = roleDescriptionText.getLineHeight() * 2;
        roleDescriptionText.setLayoutData(gridData);
        roleDescriptionText.setBackground(roleDescGroup.getBackground());

        Group permDescGroup = new Group(rolesTabComp, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        permDescGroup.setLayoutData(gridData);
        layout = new GridLayout(1, false);
        permDescGroup.setLayout(layout);
        permDescGroup.setText("Permission Description:");

        permissionDescriptionText = new Text(permDescGroup,
                SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.heightHint = permissionDescriptionText.getLineHeight() * 2;
        permissionDescriptionText.setLayoutData(gridData);
        permissionDescriptionText.setBackground(permDescGroup.getBackground());

        rolePermissionsList.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermissionDescription();
            }
        });
    }

    private void initDialog() {
        /*
         * Filter out protected users as they are not editable
         */
        Set<String> keyset = rolesAndPermissions.getUsers().keySet();
        java.util.List<String> filteredUsers = new ArrayList<>(keyset);
        filteredUsers.removeAll(rolesAndPermissions.getProtectedUsers());
        String[] users = filteredUsers
                .toArray(new String[filteredUsers.size()]);
        Arrays.sort(users, String.CASE_INSENSITIVE_ORDER);
        userList.setItems(users);
        if (userList.getItemCount() > 0) {
            userList.setSelection(0);
            updateSelectedUser();
        }

        keyset = rolesAndPermissions.getRoles().keySet();
        String[] roles = keyset.toArray(new String[keyset.size()]);
        Arrays.sort(roles, String.CASE_INSENSITIVE_ORDER);
        rolesList.setItems(roles);
        if (rolesList.getItemCount() > 0) {
            rolesList.setSelection(0);
            updateSelectedRole();
        }

        setDirty(false);
    }

    @Override
    protected void preOpened() {
        shell.setMinimumSize(shell.getSize());
    }

    /**
     * @return the dirty
     */
    private boolean isDirty() {
        return dirty;
    }

    /**
     * @param dirty
     *            the dirty to set
     */
    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        saveButton.setEnabled(dirty);
    }

    @Override
    public boolean shouldClose() {
        if (isDirty()) {
        	String[] labels = {"No", "Cancel", "Yes"};
            MessageDialog dlg = new MessageDialog(shell, "Save Changes?", null, 
            		"Role permissions have been modified. Save changes?", 
            		MessageDialog.QUESTION_WITH_CANCEL, labels, 2);

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

    private void updateSelectedUser() {
        userRolesList.removeAll();
        int index = userList.getSelectionIndex();
        if (index > -1) {
            String user = userList.getItem(index);

            Set<String> roles = rolesAndPermissions.getUsers().get(user);
            String[] rolesArray = roles.toArray(new String[roles.size()]);
            Arrays.sort(rolesArray, String.CASE_INSENSITIVE_ORDER);
            userRolesList.setItems(rolesArray);
            userRoleDescriptionText.setText("");
        }
    }

    private void editUser() {
        int index = userList.getSelectionIndex();
        if (index > -1) {
            String user = userList.getItem(index);

            AssignUserRolesDialog dlg = new AssignUserRolesDialog(shell, user,
                    rolesAndPermissions);
            if ((Boolean) dlg.open()) {
                setDirty(true);
                updateSelectedUser();
            }
        }
    }

    private void deleteUser() {
        int index = userList.getSelectionIndex();
        if (index > -1) {
            String user = userList.getItem(index);
            if (MessageDialog.openQuestion(shell, "Confirm Delete",
                    "Are you sure you wish to delete user: " + user + "?")) {

                rolesAndPermissions.updateUser(user, null);
                setDirty(true);

                userList.remove(index);
                if (index >= userList.getItemCount()) {
                    index -= 1;
                }
                userList.setSelection(index);
                updateSelectedUser();
            }
        }
    }

    private void updateUserRoleDescription() {
        userRoleDescriptionText.setText("");
        int index = userRolesList.getSelectionIndex();
        if (index > -1) {
            String rolename = userRolesList.getItem(index);

            Role role = rolesAndPermissions.getRoles().get(rolename);
            userRoleDescriptionText.setText(role.getDescription());
        }
    }

    private void updateSelectedRole() {
        String roleType = "";
        roleDescriptionText.setText("");
        rolePermissionsList.removeAll();
        int index = rolesList.getSelectionIndex();
        if (index > -1) {
            String rolename = rolesList.getItem(index);

            Role role = rolesAndPermissions.getRoles().get(rolename);
            roleDescriptionText.setText(role.getDescription());

            Set<String> permissions = role.getPermissions();
            String[] permissionsArray = permissions
                    .toArray(new String[permissions.size()]);
            Arrays.sort(permissionsArray, String.CASE_INSENSITIVE_ORDER);
            rolePermissionsList.setItems(permissionsArray);
            permissionDescriptionText.setText("");

            boolean isProtected = rolesAndPermissions.getProtectedRoles()
                    .contains(rolename);
            roleType = isProtected ? "predefined" : "custom";
            editRoleButton.setEnabled(!isProtected);
            deleteRoleButton.setEnabled(!isProtected);
            copyRoleButton.setEnabled(true);
        } else {
            copyRoleButton.setEnabled(false);
        }

        roleDescGroup.setText("Role Description: (" + roleType + ")");
    }

    private void updatePermissionDescription() {
        int index = rolePermissionsList.getSelectionIndex();
        if (index > -1) {
            String permission = rolePermissionsList.getItem(index);
            String description = rolesAndPermissions.getPermissions()
                    .get(permission);

            if (description == null) {
                description = PermissionDescriptionBuilder
                        .buildDescription(permission);
            }

            permissionDescriptionText.setText(description);
        }
    }

    private String getNewRoleName() {
        String roleName = null;
        InputDialog dlg = new InputDialog(shell, "Add Role", "Role name:", "",
                new IInputValidator() {

                    @Override
                    public String isValid(String newText) {
                        Matcher matcher = VALID_ROLENAME_REGEX
                                .matcher(newText.trim());
                        if (!matcher.matches()) {
                            return "Role names must begin with a letter and contain only\n"
                                    + "letters, digits, underscores, dashes, or periods!";
                        }

                        if (rolesAndPermissions.getRoles().keySet()
                                .contains(newText)) {
                            return "Role name already exists!";
                        }
                        return null;
                    }
                });
        dlg.setBlockOnOpen(true);
        if (dlg.open() == Window.OK) {
            roleName = dlg.getValue();
        }
        return roleName;
    }

    private void addRole() {
        String roleName = getNewRoleName();
        if (roleName != null) {
            if (editRole(roleName)) {
                addRole(roleName);
                updateSelectedRole();
            }
        }
    }

    private void addRole(String roleName) {
        String[] items = Arrays.copyOfRange(rolesList.getItems(), 0,
                rolesList.getItemCount() + 1);
        items[items.length - 1] = roleName;
        Arrays.sort(items, String.CASE_INSENSITIVE_ORDER);
        rolesList.setItems(items);
        rolesList.setSelection(rolesList.indexOf(roleName));
    }

    private void editRole() {
        int index = rolesList.getSelectionIndex();
        if (index > -1) {
            String role = rolesList.getItem(index);
            editRole(role);
            updateSelectedRole();
        }
    }

    private void copyRole() {
        int index = rolesList.getSelectionIndex();
        if (index > -1) {
            String existingRoleName = rolesList.getItem(index);
            String roleName = getNewRoleName();
            if (roleName != null) {
                Role existingRole = rolesAndPermissions.getRoles()
                        .get(existingRoleName);
                rolesAndPermissions.updateRole(roleName,
                        new Role(existingRole));
                setDirty(true);
                addRole(roleName);
                updateSelectedRole();
            }
        }
    }

    private boolean editRole(String role) {
        AssignRolePermissionsDialog dlg = new AssignRolePermissionsDialog(shell,
                role, rolesAndPermissions);
        if ((Boolean) dlg.open()) {
            setDirty(true);
            return true;
        }
        return false;
    }

    private void deleteRole() {
        int index = rolesList.getSelectionIndex();
        if (index > -1) {
            String role = rolesList.getItem(index);
            if (MessageDialog.openQuestion(shell, "Confirm Delete",
                    "Are you sure you wish to delete role: " + role + "?")) {

                rolesAndPermissions.updateRole(role, null);
                setDirty(true);

                rolesList.remove(index);
                if (index >= rolesList.getItemCount()) {
                    index -= 1;
                }
                rolesList.setSelection(index);
                updateSelectedRole();
            }
        }
    }

    private boolean saveChanges() {
        try {
            SaveRolesAndPermissionsRequest request = new SaveRolesAndPermissionsRequest(
                    rolesAndPermissions);

            this.rolesAndPermissions = (RolesAndPermissions) ThriftClient
                    .sendRequest(request);
            initDialog();
            return true;

        } catch (Throwable e) {
            statusHandler.error(
                    "Error saving changes: " + e.getLocalizedMessage(), e);
        }
        return false;
    }
}
