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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.auth.util.PermissionDescriptionBuilder;
import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationContext;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationLevel;
import com.raytheon.uf.common.localization.LocalizationContext.LocalizationType;
import com.raytheon.uf.common.localization.LocalizationUtil;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.viz.ui.dialogs.CaveSWTDialog;

/**
 * Create a Localization permission string
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * May 01, 2017  6217     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */

public class DefineLocalizationPermissionDlg extends CaveSWTDialog {

    private static final String[] LOCALIZATION_OPERATIONS = new String[] { "*",
            "read", "write", "delete" };

    private static final String DEFAULT_LOCALIZATION_OPERATION = LOCALIZATION_OPERATIONS[0];

    /* valid characters for a path segment */
    private static final String PATH_CHARS = "a-zA-Z0-9._\\-";

    /*
     * valid characters to be typed in path text
     *
     * includes *, /, backspace, and del
     */
    private static final Pattern VALID_PATH_CHAR_PATTERN = Pattern
            .compile("[" + PATH_CHARS + "\\*" + IPathManager.SEPARATOR + "]*");

    /* regex for valid path string */
    private static final Pattern VALID_PATH_REGEX = Pattern.compile(
            "(\\*|[" + PATH_CHARS + "]+)(" + IPathManager.SEPARATOR + "(\\*|["
                    + PATH_CHARS + "]+))*" + IPathManager.SEPARATOR + "{0,1}");

    private String initialValue;

    private IPathManager pathMgr;

    private Combo operationCombo;

    private Combo typeCombo;

    private Combo levelCombo;

    private Label contextLabel;

    private Combo contextNameCombo;

    private Label errorLabel;

    private Text pathText;

    private Text permissionText;

    private Text permissionDescriptionText;

    private Button saveButton;

    private boolean dirty;

    /**
     * @param parent
     * @param initialValue
     *            value used to initialize GUI elements
     */
    public DefineLocalizationPermissionDlg(Shell parent, String initialValue) {
        super(parent, SWT.TITLE | SWT.BORDER | SWT.RESIZE | SWT.PRIMARY_MODAL,
                CAVE.PERSPECTIVE_INDEPENDENT | CAVE.MODE_INDEPENDENT);
        this.initialValue = initialValue;
        setText("Define Localization Permission");

        setReturnValue(false);
        pathMgr = PathManagerFactory.getPathManager();
        setReturnValue(null);
    }

    private boolean isValid() {
        boolean valid = true;
        errorLabel.setText("");

        Matcher matcher = VALID_PATH_CHAR_PATTERN.matcher(pathText.getText());
        valid = matcher.matches();
        if (!valid) {
            errorLabel.setText("Path may only contain "
                    + PATH_CHARS.replace("\\", "") + "*/");
            getDisplay().beep();

        } else {
            matcher = VALID_PATH_REGEX.matcher(pathText.getText());
            valid = matcher.matches();
            if (!valid) {
                errorLabel.setText(
                        "Path may not be empty or contain repeated *s or /s");
                getDisplay().beep();
            }
        }

        return valid;
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
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
        saveButton.setEnabled(dirty && isValid());
    }

    @Override
    protected void initializeComponents(Shell shell) {
        shell.setLayout(new GridLayout(2, false));

        Label label = new Label(shell, SWT.RIGHT);
        label.setText("Operation:");
        GridData gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        label.setLayoutData(gridData);

        operationCombo = new Combo(shell, SWT.READ_ONLY | SWT.DROP_DOWN);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        operationCombo.setLayoutData(gridData);
        operationCombo.setItems(LOCALIZATION_OPERATIONS);
        operationCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermission();
            }
        });

        /*
         * Hide the operationCombo for now
         *
         * If/when we want to implement read/delete permissions just remove the
         * following lines and change DEFAULT_LOCALIZATION_OPERATION as desired
         *
         */
        ((GridData) label.getLayoutData()).exclude = true;
        ((GridData) operationCombo.getLayoutData()).exclude = true;

        label = new Label(shell, SWT.RIGHT);
        label.setText("Type:");
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        label.setLayoutData(gridData);

        typeCombo = new Combo(shell, SWT.READ_ONLY | SWT.DROP_DOWN);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        typeCombo.setLayoutData(gridData);
        typeCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermission();
            }
        });

        typeCombo.add("*");
        for (LocalizationType type : LocalizationType.values()) {
            if (!LocalizationType.UNKNOWN.equals(type)) {
                String typeName = type.toString();
                typeCombo.add(typeName);
                typeCombo.setData(typeName, type);
            }
        }

        label = new Label(shell, SWT.RIGHT);
        label.setText("Level:");
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        label.setLayoutData(gridData);

        levelCombo = new Combo(shell, SWT.READ_ONLY | SWT.DROP_DOWN);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        levelCombo.setLayoutData(gridData);
        levelCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updateLevel();
            }
        });
        levelCombo.add("*");

        GC gc = new GC(shell);
        int contextLabelWidth = gc.textExtent("Context Name:").x;
        int avgCharWidth = gc.getFontMetrics().getAverageCharWidth();
        for (LocalizationLevel level : pathMgr.getAvailableLevels()) {
            if (!level.isSystemLevel()) {
                String levelName = level.toString();
                contextLabelWidth = Math.max(contextLabelWidth, gc.textExtent(
                        LocalizationUtil.getProperName(level) + " Name:").x);
                levelCombo.add(levelName);
                levelCombo.setData(levelName, level);
            }
        }
        gc.dispose();

        contextLabel = new Label(shell, SWT.RIGHT);
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        gridData.widthHint = contextLabelWidth;
        contextLabel.setLayoutData(gridData);

        contextNameCombo = new Combo(shell, SWT.DROP_DOWN);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        contextNameCombo.setLayoutData(gridData);
        contextNameCombo.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                updatePermission();
            }
        });
        contextNameCombo.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                updatePermission();
            }
        });

        label = new Label(shell, SWT.RIGHT);
        label.setText("Path:");
        gridData = new GridData(SWT.RIGHT, SWT.CENTER, false, true);
        label.setLayoutData(gridData);

        pathText = new Text(shell, SWT.BORDER | SWT.SINGLE);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        pathText.setLayoutData(gridData);
        pathText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                isValid();
                updatePermission();
            }
        });

        errorLabel = new Label(shell, SWT.LEFT);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        errorLabel.setLayoutData(gridData);
        errorLabel.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));

        Group group = new Group(shell, SWT.BORDER);
        group.setText("Permission:");
        gridData = new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        group.setLayout(layout);

        permissionText = new Text(group, SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.widthHint = avgCharWidth * 64;
        gridData.horizontalSpan = 2;
        gridData.heightHint = permissionText.getLineHeight() * 2;
        permissionText.setLayoutData(gridData);
        permissionText.setBackground(group.getBackground());

        group = new Group(shell, SWT.BORDER);
        group.setText("Permission Description:");
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        group.setLayoutData(gridData);
        layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        group.setLayout(layout);

        permissionDescriptionText = new Text(group,
                SWT.READ_ONLY | SWT.MULTI | SWT.WRAP);
        gridData = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
        gridData.heightHint = permissionDescriptionText.getLineHeight() * 2;
        permissionDescriptionText.setLayoutData(gridData);
        permissionDescriptionText.setBackground(group.getBackground());

        Composite buttonComp = new Composite(shell, SWT.NONE);
        gridData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gridData.horizontalSpan = 2;
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

        initDialog();
        updatePermission();
        setDirty(false);
    }

    private void initDialog() {
        operationCombo.setText(DEFAULT_LOCALIZATION_OPERATION);
        typeCombo.setText(LocalizationType.COMMON_STATIC.toString());
        levelCombo.setText(LocalizationLevel.SITE.toString());
        pathText.setText("*");
        updateLevel();

        if (initialValue != null) {
            String[] parts = initialValue.split(":");

            for (int i = 1; i < parts.length; i++) {
                switch (i) {
                case 1:
                    // read/write/delete
                    operationCombo.setText(parts[i]);
                    break;
                case 2:
                    // localization type
                    typeCombo.setText(parts[i].toUpperCase());
                    break;

                case 3:
                    // localization level
                    levelCombo.setText(parts[i].toUpperCase());
                    break;

                case 4:
                    // context name
                    contextNameCombo.setText(parts[i]);
                    break;

                case 5:
                    pathText.setText(parts[i]);
                    break;

                default:
                    pathText.setText(pathText.getText() + IPathManager.SEPARATOR
                            + parts[i]);
                    break;
                }
            }
        }
    }

    private void updateLevel() {
        contextNameCombo.removeAll();
        contextNameCombo.add("*");
        contextNameCombo.setText("*");

        LocalizationType type = (LocalizationType) typeCombo
                .getData(typeCombo.getText());
        String levelName = levelCombo.getText();
        LocalizationLevel level = (LocalizationLevel) levelCombo
                .getData(levelName);

        String contextName = "Context Name:";
        if (!"*".equals(levelName)) {
            LocalizationContext ctx = pathMgr.getContext(type, level);
            contextNameCombo.add(ctx.getContextName());
            contextName = LocalizationUtil.getProperName(level) + " Name:";
            contextNameCombo.setEnabled(true);
        } else {
            contextNameCombo.setEnabled(false);
        }
        contextLabel.setText(contextName);

        updatePermission();
    }

    private void updatePermission() {
        String[] pathParts = LocalizationUtil.splitUnique(pathText.getText());
        String permission = String.join(":", "localization",
                operationCombo.getText(), typeCombo.getText().toLowerCase(),
                levelCombo.getText().toLowerCase(), contextNameCombo.getText(),
                String.join(":", pathParts));

        permissionText.setText(permission);
        permissionDescriptionText.setText(
                PermissionDescriptionBuilder.buildDescription(permission));

        setDirty(true);
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

    private boolean saveChanges() {
        setReturnValue(permissionText.getText());
        setDirty(false);
        close();
        return true;
    }

}
