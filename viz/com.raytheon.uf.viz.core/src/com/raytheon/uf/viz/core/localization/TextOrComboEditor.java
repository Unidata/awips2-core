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
package com.raytheon.uf.viz.core.localization;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

/**
 * A preference page field editor that can be a text field or an editable combo
 * (dropdown) field.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 26, 2014 3236       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class TextOrComboEditor extends FieldEditor {

    private TextOrCombo field;

    private String prefOptionsName;

    private String errorMsg;

    public TextOrComboEditor(Composite parent, IPreferenceStore prefs,
            String prefName, String prefOptionsName, String labelName) {
        init(prefName, labelName);
        this.setPreferenceStore(prefs);
        this.prefOptionsName = prefOptionsName;
        createControl(parent);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#adjustForNumColumns(int)
     */
    @Override
    protected void adjustForNumColumns(int numColumns) {
        // borrowed from StringFieldEditor
        GridData gd = (GridData) field.widget.getLayoutData();
        gd.horizontalSpan = numColumns - 1;
        gd.grabExcessHorizontalSpace = gd.horizontalSpan == 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.jface.preference.FieldEditor#doFillIntoGrid(org.eclipse.swt
     * .widgets.Composite, int)
     */
    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        int comboC = 1;
        if (numColumns > 1) {
            comboC = numColumns - 1;
        }
        Control label = getLabelControl(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = 1;
        label.setLayoutData(gd);
        field = getTextOrComboControl(parent);
        gd = new GridData();
        gd.horizontalSpan = comboC;
        gd.horizontalAlignment = GridData.FILL;
        field.widget.setLayoutData(gd);
        field.widget.setFont(parent.getFont());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#doLoad()
     */
    @Override
    protected void doLoad() {
        if (field != null) {
            String value = getPreferenceStore().getString(getPreferenceName());
            field.setText(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#doLoadDefault()
     */
    @Override
    protected void doLoadDefault() {
        if (field != null) {
            String value = getPreferenceStore().getDefaultString(
                    getPreferenceName());
            field.setText(value);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#doStore()
     */
    @Override
    protected void doStore() {
        getPreferenceStore().setValue(getPreferenceName(), field.getText());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.preference.FieldEditor#getNumberOfControls()
     */
    @Override
    public int getNumberOfControls() {
        return 2;
    }

    public TextOrCombo getTextOrComboControl(Composite parent) {
        if (field == null) {
            field = new TextOrCombo(parent, SWT.BORDER,
                    ServerRemembrance.getServerOptions(getPreferenceStore(),
                            prefOptionsName));
            field.widget.setFont(parent.getFont());
            field.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    fireValueChanged("field text", getPreferenceStore()
                            .getString(getPreferenceName()), field.getText());
                }
            });
        }
        return field;
    }

    public void setErrorMessage(String msg) {
        this.errorMsg = msg;
    }

    public void showErrorMessage() {
        super.showErrorMessage(errorMsg);
    }

    /**
     * Clears the error message from the message line.
     */
    @Override
    public void clearErrorMessage() {
        super.clearErrorMessage();
    }

    public void addSelectionListener(SelectionListener listener) {
        this.field.addSelectionListener(listener);
    }

}
