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
import java.util.Collections;
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
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.auth.RolesAndPermissions;
import com.raytheon.uf.common.auth.util.PermissionDescriptionBuilder;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;
import com.raytheon.viz.ui.widgets.duallist.DualList;
import com.raytheon.viz.ui.widgets.duallist.DualListConfig;
import com.raytheon.viz.ui.widgets.duallist.IUpdate;

/**
 * Assign Role Permissions Dialog
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

public class AssignRolePermissionsDialog extends CaveSWTDialog {
    private static final String LOCALIZATION_PREFIX = "localization:";

    private final IUFStatusHandler statusHandler = UFStatus
            .getHandler(this.getClass());

    private String role;

    private RolesAndPermissions rolesAndPermissions;

    private DualList applicationDL;

    private DualList localizationDL;

    private Label errorLabel;

    private Text permissionDescriptionText;

    private Button saveButton;

    private boolean dirty;

    /**
     * Constructor
     *
     * @param parent
     * @param role
     *            name of role to be edited
     * @param rolesAndPermissions
     *            roles and permissions data
     */
    public AssignRolePermissionsDialog(Shell parent, String role,
            RolesAndPermissions rolesAndPermissions) {
        super(parent, SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.PRIMARY_MODAL,
                CAVE.PERSPECTIVE_INDEPENDENT | CAVE.MODE_INDEPENDENT);
        this.role = role;
        this.rolesAndPermissions = rolesAndPermissions;

        setText("Assign Role Permissions");

        setReturnValue(false);
    }

    /**
     * @return the dirty
     */
    private boolean isDirty() {
        return dirty;
    }

    private boolean isValid() {
        boolean valid = applicationDL.getSelectedListItems().length > 0
                || localizationDL.getSelectedListItems().length > 0;

        if (valid) {
            errorLabel.setText("");
        } else {
            errorLabel.setText("At least one permission must be assigned!");
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
        shell.setLayout(new GridLayout(1, false));

        Label label = new Label(shell, SWT.CENTER);
        GridData gridData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        label.setLayoutData(gridData);
        label.setText("Assign permissions for role: " + role);

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

        TabFolder tabFolder = new TabFolder(shell, SWT.BORDER);
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        tabFolder.setLayoutData(gridData);

        createApplicationTab(tabFolder);
        createLocalizationTab(tabFolder);

        tabFolder.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                int i = tabFolder.getSelectionIndex();
                DualList dl = (DualList) tabFolder.getItem(i).getData();
                updatePermissionDescription(dl);
            }
        });

        errorLabel = new Label(shell, SWT.LEFT);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        errorLabel.setLayoutData(gridData);
        errorLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));

        Group group = new Group(shell, SWT.BORDER);
        group.setText("Permission Description:");
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        group.setLayoutData(gridData);
        group.setLayout(new GridLayout(1, false));

        permissionDescriptionText = new Text(group,
                SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.heightHint = permissionDescriptionText.getLineHeight() * 2;
        permissionDescriptionText.setLayoutData(gridData);
        permissionDescriptionText.setBackground(group.getBackground());

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

    private void createApplicationTab(TabFolder tabFolder) {
        TabItem applicationTab = new TabItem(tabFolder, SWT.NONE);
        applicationTab.setText("Application Permissions");
        Composite applicationTabComp = new Composite(tabFolder, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        applicationTabComp.setLayoutData(gridData);
        GridLayout layout = new GridLayout(1, false);
        applicationTabComp.setLayout(layout);
        applicationTab.setControl(applicationTabComp);

        DualListConfig config = new DualListConfig();
        config.setAvailableListLabel("Application Permissions:");
        config.setSelectedListLabel("Selected Permissions:");
        config.setListWidthInChars(32);
        config.setVisibleItems(12);
        config.setSortList(true);

        /* filter out only application permissions */
        java.util.List<String> available = new ArrayList<>(
                rolesAndPermissions.getPermissions().size());
        for (String permission : rolesAndPermissions.getPermissions()
                .keySet()) {
            if (!permission.startsWith(LOCALIZATION_PREFIX)) {
                available.add(permission);
            }
        }
        config.setFullList(available);

        Set<String> permissions = rolesAndPermissions.getRoles().get(role);
        if (permissions != null) {
            java.util.List<String> selected = new ArrayList<>(
                    permissions.size());
            for (String permission : permissions) {
                if (!permission.startsWith(LOCALIZATION_PREFIX)) {
                    selected.add(permission);
                }
            }
            config.setSelectedList(new ArrayList<String>(selected));
        }

        applicationDL = new DualList(applicationTabComp, SWT.NONE, config,
                new IUpdate() {
                    @Override
                    public void selectionChanged() {
                        setDirty(true);
                        updatePermissionDescription(applicationDL);
                    }

                    @Override
                    public void hasEntries(boolean entries) {
                        // do nothing
                    }
                });
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        applicationDL.setLayoutData(gridData);

        SelectionAdapter adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermissionDescription(applicationDL);
            }
        };
        applicationDL.getAvailableList().addSelectionListener(adapter);
        applicationDL.getSelectedList().addSelectionListener(adapter);

        applicationTab.setData(applicationDL);
    }

    private void createLocalizationTab(TabFolder tabFolder) {
        TabItem localizationTab = new TabItem(tabFolder, SWT.NONE);
        localizationTab.setText("Localization Permissions");
        Composite localizationTabComp = new Composite(tabFolder, SWT.NONE);
        GridData gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        localizationTabComp.setLayoutData(gridData);
        GridLayout layout = new GridLayout(1, false);
        localizationTabComp.setLayout(layout);
        localizationTab.setControl(localizationTabComp);

        DualListConfig config = new DualListConfig();
        config.setAvailableListLabel("Localization Permissions:");
        config.setSelectedListLabel("Selected Permissions:");
        config.setListWidthInChars(56);
        config.setVisibleItems(12);
        config.setSortList(true);

        /* filter out only localization permissions */
        java.util.List<String> available = new ArrayList<>(
                rolesAndPermissions.getPermissions().size());
        for (String permission : rolesAndPermissions.getPermissions()
                .keySet()) {
            if (permission.startsWith(LOCALIZATION_PREFIX)) {
                available.add(permission);
            }
        }
        config.setFullList(available);

        Set<String> permissions = rolesAndPermissions.getRoles().get(role);
        if (permissions != null) {
            java.util.List<String> selected = new ArrayList<>(
                    permissions.size());
            for (String permission : permissions) {
                if (permission.startsWith(LOCALIZATION_PREFIX)) {
                    selected.add(permission);
                }
            }
            config.setSelectedList(new ArrayList<String>(selected));
        }

        localizationDL = new DualList(localizationTabComp, SWT.NONE, config,
                new IUpdate() {
                    @Override
                    public void selectionChanged() {
                        setDirty(true);
                        updatePermissionDescription(localizationDL);
                    }

                    @Override
                    public void hasEntries(boolean entries) {
                        // do nothing
                    }
                });
        gridData = new GridData(SWT.FILL, SWT.FILL, true, true);
        localizationDL.setLayoutData(gridData);

        SelectionAdapter adapter = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermissionDescription(localizationDL);
            }
        };
        localizationDL.getAvailableList().addSelectionListener(adapter);
        localizationDL.getSelectedList().addSelectionListener(adapter);

        localizationTab.setData(localizationDL);

        Button addLocalPermButton = new Button(localizationTabComp, SWT.PUSH);
        gridData = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        addLocalPermButton.setLayoutData(gridData);
        addLocalPermButton.setText("Define New Localization Permission...");
        addLocalPermButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                defineLocalizationPermission();
            }
        });
    }

    private void defineLocalizationPermission() {
        DefineLocalizationPermissionDlg dlg = new DefineLocalizationPermissionDlg(
                getShell());
        String permission = (String) dlg.open();
        List availableList = localizationDL.getAvailableList();
        if (permission != null) {
            if (availableList.indexOf(permission) < 0) {
                String[] items = availableList.getItems();
                java.util.List<String> newItems = new ArrayList<>(
                        items.length + 1);
                newItems.addAll(Arrays.asList(items));
                newItems.add(permission);
                Collections.sort(newItems);
                localizationDL.setAvailableItems(newItems);
            }
            availableList.setSelection(availableList.indexOf(permission));

            if (!rolesAndPermissions.getPermissions().keySet()
                    .contains(permission)) {
                rolesAndPermissions.getPermissions().put(permission,
                        PermissionDescriptionBuilder
                                .buildDescription(permission));
            }

            localizationDL.getSelectedList().deselectAll();
            updatePermissionDescription(localizationDL);
        }
    }

    private void updatePermissionDescription(DualList dl) {

        List list = dl.getAvailableList();
        if (list.getSelectionIndex() == -1) {
            list = dl.getSelectedList();
        }

        int index = list.getSelectionIndex();
        if (index > -1) {
            String permission = list.getItem(index);
            String description = rolesAndPermissions.getPermissions()
                    .get(permission);

            if (description == null) {
                description = PermissionDescriptionBuilder
                        .buildDescription(permission);
            }

            permissionDescriptionText.setText(description);
        }
    }

    @Override
    public boolean shouldClose() {
        if (isDirty() && isValid()) {
            MessageDialog dlg = new MessageDialog(shell, "Save Changes?", null,
                    "Role permissions have been modified. Save changes?",
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
            Set<String> permissions = new HashSet<>(
                    Arrays.asList(applicationDL.getSelectedListItems()));
            permissions.addAll(
                    Arrays.asList(localizationDL.getSelectedListItems()));
            rolesAndPermissions.updateRole(role, permissions);

            setDirty(false);
            return true;

        } catch (Throwable e) {
            statusHandler.error(
                    "Error saving changes: " + e.getLocalizedMessage(), e);
        }
        return false;
    }
}
