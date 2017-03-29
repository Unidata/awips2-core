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
package com.raytheon.viz.core.preferences;

import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Spinner;

/**
 * FieldEditor for numeric preferences that can be displayed in a
 * {@link Spinner}
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------
 * Mar 29, 2017  6202     bsteffen  Initial Creation
 * 
 * </pre>
 * 
 * @author bsteffen
 * 
 */
public class SpinnerFieldEditor extends FieldEditor {

    private Spinner spinner;

    public SpinnerFieldEditor(String name, String labelText, Composite parent) {
        init(name, labelText);
        createControl(parent);
    }

    public SpinnerFieldEditor(String name, String labelText, Composite parent,
            int minimum, int maximum) {
        this(name, labelText, parent);
        setMinimum(minimum);
        setMaximum(maximum);
    }

    public void setIncrement(int value) {
        spinner.setIncrement(value);
    }

    public void setMaximum(int value) {
        spinner.setMaximum(value);
    }

    public void setMinimum(int value) {
        spinner.setMinimum(value);
    }

    public void setPageIncrement(int value) {
        spinner.setPageIncrement(value);
    }

    public void setTextLimit(int limit) {
        spinner.setTextLimit(limit);
    }

    public void setToolTipText(String string) {
        spinner.setToolTipText(string);
    }

    public void setDigits(int value) {
        spinner.setDigits(value);
    }

    @Override
    protected void adjustForNumColumns(int numColumns) {
        GridData gd = (GridData) spinner.getLayoutData();
        gd.horizontalSpan = numColumns - 1;
    }

    @Override
    protected void doFillIntoGrid(Composite parent, int numColumns) {
        getLabelControl(parent);

        spinner = getSpinner(parent);
        GridData gd = new GridData();
        gd.horizontalSpan = numColumns - 1;

        gd.horizontalAlignment = GridData.FILL;
        gd.grabExcessHorizontalSpace = true;
        spinner.setLayoutData(gd);
    }

    @Override
    protected void doLoad() {
        if (spinner != null) {
            int digits = spinner.getDigits();
            if (digits == 0) {
                int value = getPreferenceStore().getInt(getPreferenceName());
                spinner.setSelection(value);
            } else {
                float value = getPreferenceStore()
                        .getFloat(getPreferenceName());
                int adjustedValue = (int) Math
                        .round((Math.pow(10, digits) * value));
                spinner.setSelection(adjustedValue);
            }
        }
    }

    @Override
    protected void doLoadDefault() {
        if (spinner != null) {
            int digits = spinner.getDigits();
            if (digits == 0) {
                int value = getPreferenceStore()
                        .getDefaultInt(getPreferenceName());
                spinner.setSelection(value);
            } else {
                float value = getPreferenceStore()
                        .getDefaultFloat(getPreferenceName());
                int adjustedValue = (int) Math
                        .round((Math.pow(10, digits) * value));
                spinner.setSelection(adjustedValue);
            }
        }
    }

    @Override
    protected void doStore() {
        int digits = spinner.getDigits();
        int value = spinner.getSelection();
        if (digits == 0) {
            getPreferenceStore().setValue(getPreferenceName(), value);
        } else {
            float adjustedValue = (float) (value / Math.pow(10, digits));
            getPreferenceStore().setValue(getPreferenceName(), adjustedValue);

        }
    }

    @Override
    public int getNumberOfControls() {
        return 2;
    }

    protected Spinner getTextControl() {
        return spinner;
    }

    protected Spinner getSpinner(Composite parent) {
        if (spinner == null) {
            spinner = new Spinner(parent, SWT.NONE);
        } else {
            checkParent(spinner, parent);
        }
        return spinner;
    }

    @Override
    public void setFocus() {
        if (spinner != null) {
            spinner.setFocus();
        }
    }

    @Override
    public void setEnabled(boolean enabled, Composite parent) {
        super.setEnabled(enabled, parent);
        getSpinner(parent).setEnabled(enabled);
    }
}
