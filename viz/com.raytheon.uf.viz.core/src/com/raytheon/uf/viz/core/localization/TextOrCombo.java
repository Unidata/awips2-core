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

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

/**
 * A container that holds an SWT Text or Combo field. This enables GUI code to
 * show a Text field when there is only one option, or show an editable Combo
 * (dropdown) when there are multiple options.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 24, 2014 3236       njensen     Initial creation
 * 
 * </pre>
 * 
 * @author njensen
 * @version 1.0
 */

public class TextOrCombo {

    public final Control widget;

    protected final boolean isText;

    protected final boolean isCombo;

    /**
     * Constructor
     * 
     * @param parent
     *            the parent composite that the widget will be created in
     * @param style
     *            the style to apply to the widget
     * @param options
     *            the options for the dropdown, if applicable. If null or length
     *            < 2, then the widget will be a Text, otherwise the widget will
     *            be a Combo.
     */
    public TextOrCombo(Composite parent, int style, String[] options) {
        if (options != null && options.length > 1) {
            Combo inner = new Combo(parent, style);
            inner.setItems(options);
            this.widget = inner;
            isCombo = true;
            isText = false;
        } else {
            Text inner = new Text(parent, style);
            this.widget = inner;
            isText = true;
            isCombo = false;
        }
    }

    public String getText() {
        String text = null;
        if (isText) {
            text = ((Text) widget).getText();
        } else if (isCombo) {
            text = ((Combo) widget).getText();
        }
        return text;
    }

    public void setText(String text) {
        if (isText) {
            ((Text) widget).setText(text);
        } else if (isCombo) {
            ((Combo) widget).setText(text);
        }
    }

    public void addSelectionListener(SelectionListener listener) {
        if (isText) {
            ((Text) widget).addSelectionListener(listener);
        } else if (isCombo) {
            ((Combo) widget).addSelectionListener(listener);
        }
    }

    public void addModifyListener(ModifyListener listener) {
        if (isText) {
            ((Text) widget).addModifyListener(listener);
        } else if (isCombo) {
            ((Combo) widget).addModifyListener(listener);
        }
    }

}
