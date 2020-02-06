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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;

import com.raytheon.uf.common.time.util.TimeUtil;

/**
 * Date/Time entry field with spinner controls
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Mar 17, 2016  5483     randerso  Initial creation
 * May 04, 2016  5602     bkowal    Added enable/disable, backing date get/set
 *                                  capabilities.
 * Dec 11, 2019  7994     randerso  Added constructor with format string to
 *                                  allow more flexibility. Deprecated
 *                                  constructors using field count.
 *
 * </pre>
 *
 * @author randerso
 */

public class DateTimeSpinner extends Canvas implements PaintListener {
    /**
     * Only characters in this string, punctuation, and whitespace are allowed
     * in the format string
     */
    public static final String supportedFormatChars = "yMdHms";

    /*
     * This array must match the format characters above
     */
    private static final int fieldType[] = new int[] { Calendar.YEAR,
            Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR_OF_DAY,
            Calendar.MINUTE, Calendar.SECOND };

    // TODO: remove this when the deprecated constructors are removed
    private static final String[] formatStrings = new String[] { "yyyy",
            "yyyy-MM", "yyyy-MM-dd", "yyyy-MM-dd HH", "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd HH:mm:ss" };

    private List<Point> fieldIndices = new ArrayList<>(
            supportedFormatChars.length());

    private final List<Integer> editableFields = new ArrayList<>(
            supportedFormatChars.length());;

    private Text text;

    private int characterCount = 0;

    private int currentField = 0;

    private Calendar calendar;

    private boolean ignoreVerify;

    private UIJob spinnerJob;

    /**
     * Set to -1 if down button pressed, 1 if up button pressed, 0 if no button
     * pressed
     */
    private int spinIncrement = 0;

    private static final int SPIN_START_DELAY_MS = 300;

    private static final int SPIN_INTERVAL_MS = 30;

    private DateFormat format;

    private boolean clearNonEditable;

    private static int validateFieldCount(int fieldCount) {
        if ((fieldCount < 1) || (fieldCount > formatStrings.length)) {
            throw new IllegalArgumentException(
                    "fieldCount must be in the range 1-"
                            + formatStrings.length);
        }
        return fieldCount - 1;
    }

    /**
     * Constructor
     *
     * @param parent
     *            parent composite
     * @param calendar
     *            Calendar instance containing initial date. This instance will
     *            be updated as the widget is updated
     * @param fieldCount
     *
     *            <pre>
     *            1 = yyyy
     *            2 = yyyy-MM
     *            3 = yyyy-MM-dd,
     *            4 = yyyy-MM-dd HH
     *            5 = yyyy-MM-dd HH:mm
     *            6 = yyyy-MM-dd HH:mm:ss
     *            </pre>
     *
     * @deprecated use
     *             {@link #DateTimeSpinner(Composite, Calendar, String, boolean)}
     */
    @Deprecated
    public DateTimeSpinner(Composite parent, Calendar calendar,
            int fieldCount) {
        this(parent, calendar, fieldCount, false);
    }

    /**
     * Constructor
     *
     * @param parent
     *            parent composite
     * @param calendar
     *            Calendar instance containing initial date. This instance will
     *            be updated as the widget is updated
     * @param fieldCount
     *
     *            <pre>
     *            1 = yyyy
     *            2 = yyyy-MM
     *            3 = yyyy-MM-dd,
     *            4 = yyyy-MM-dd HH
     *            5 = yyyy-MM-dd HH:mm
     *            6 = yyyy-MM-dd HH:mm:ss
     *            </pre>
     *
     * @param clearNonEditable
     *            triggers the execution of the {@link Calendar#clear(int)}
     *            method on non-editable fields when set.
     * @deprecated use
     *             {@link #DateTimeSpinner(Composite, Calendar, String, boolean)}
     */
    @Deprecated
    public DateTimeSpinner(Composite parent, Calendar calendar, int fieldCount,
            boolean clearNonEditable) {
        this(parent, calendar, formatStrings[validateFieldCount(fieldCount)],
                clearNonEditable);
    }

    /**
     * @param parent
     *            parent composite
     * @param calendar
     *            Calendar instance containing initial date. This instance will
     *            be updated as the widget is updated
     * @param formatString
     *            format string for SimpleDateFormat. Limited to only the those
     *            characters in {@link #supportedFormatChars}
     * @param clearNonEditable
     *            triggers the execution of the {@link Calendar#clear(int)}
     *            method on non-editable fields when set.
     */
    public DateTimeSpinner(Composite parent, Calendar calendar,
            String formatString, boolean clearNonEditable) {
        super(parent, SWT.NONE);
        this.clearNonEditable = clearNonEditable;

        parseFormatString(formatString);

        this.calendar = TimeUtil.newCalendar(calendar);
        if (clearNonEditable) {
            clearNonEditableFields();
        }

        format = new SimpleDateFormat(formatString);
        format.setTimeZone(calendar.getTimeZone());

        initializeControls();
    }

    private void clearNonEditableFields() {
        calendar.set(Calendar.MILLISECOND, 0);
        for (int field : fieldType) {
            if (!editableFields.contains(field)) {
                calendar.set(field, 0);
            }
        }
    }

    private void parseFormatString(String formatString) {
        int i = 0;
        while (i < formatString.length()) {
            char formatChar = formatString.charAt(i);
            int index = supportedFormatChars.indexOf(formatChar);

            if (index < 0) {
                // if it's a letter it may be an unsupported format character
                if (Character.isLetter(formatChar)) {
                    throw new IllegalArgumentException(
                            "formatString contains unsupported character: '"
                                    + formatChar + "'");
                }

                // must be punctuation or whitespace
                i++;
                continue;
            }

            // determine field type
            Integer type = fieldType[index];

            // check for redundant fields (e.g. two year fields)
            if (!editableFields.contains(type)) {
                editableFields.add(fieldType[index]);
            } else {
                throw new IllegalArgumentException(
                        "formatString contains redundant fields");
            }

            // determine extent of field
            int start = i;
            i++;
            while (i < formatString.length()
                    && formatString.charAt(i) == formatChar) {
                i++;
            }
            fieldIndices.add(new Point(start, i));

        }
    }

    private void initializeControls() {
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 1;
        gl.marginHeight = 1;
        gl.horizontalSpacing = 0;
        this.setLayout(gl);
        this.addPaintListener(this);

        text = new Text(this, SWT.SINGLE);
        GridData gd = new GridData(SWT.DEFAULT, SWT.CENTER, false, true);
        text.setLayoutData(gd);

        Listener textListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.KeyDown:
                    onKeyDown(event);
                    break;
                case SWT.FocusIn:
                    onFocusIn(event);
                    break;
                case SWT.MouseDown:
                    onMouseClick(event);
                    break;
                case SWT.MouseUp:
                    onMouseClick(event);
                    break;
                case SWT.Verify:
                    onVerify(event);
                    break;
                }
            }
        };

        text.addListener(SWT.KeyDown, textListener);
        text.addListener(SWT.FocusIn, textListener);
        text.addListener(SWT.MouseDown, textListener);
        text.addListener(SWT.MouseUp, textListener);
        text.addListener(SWT.Verify, textListener);

        Listener spinButtonListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.MouseDown:
                    spinIncrement = 1;
                    if (event.y > (((Canvas) event.widget).getSize().y / 2)) {
                        spinIncrement = -1;
                    }

                    incrementField(spinIncrement);
                    spinnerJob.schedule(SPIN_START_DELAY_MS);
                    break;

                case SWT.MouseUp:
                    spinIncrement = 0;
                    text.setFocus();
                    redraw();
                    break;
                }
            }
        };
        this.addListener(SWT.MouseDown, spinButtonListener);
        this.addListener(SWT.MouseUp, spinButtonListener);

        spinnerJob = new UIJob(Display.getDefault(),
                "Handle Spin Button Pressed") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if ((spinIncrement != 0) && !text.isDisposed()) {
                    incrementField(spinIncrement);
                    this.schedule(SPIN_INTERVAL_MS);
                }
                return Status.OK_STATUS;
            }
        };

        updateControl();
    }

    @Override
    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point size = text.computeSize(SWT.DEFAULT, SWT.DEFAULT);
        int y = size.y + 4;
        y += 1 - (y % 2);

        int x = size.x += (y * 0.65);
        x += x % 2;

        return new Point(x, y);
    }

    @Override
    public void paintControl(PaintEvent e) {
        Rectangle ca = getClientArea();
        e.gc.setClipping(ca);
        ca.width--;
        ca.height--;

        Rectangle tb = text.getBounds();

        // Fill text area
        e.gc.setBackground(
                getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
        e.gc.fillRectangle(tb.x, ca.y, tb.width, ca.height);

        // Draw border
        e.gc.setForeground(
                getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        e.gc.drawRoundRectangle(ca.x, ca.y, ca.width, ca.height, 5, 5);

        // Determine button dimensions
        int buttonLeft = tb.x + tb.width;
        int buttonRight = ca.x + ca.width;
        int buttonCenter = (buttonRight + buttonLeft) / 2;
        int buttonTop = ca.y;
        int buttonBottom = ca.y + ca.height;
        int buttonMiddle = (buttonTop + buttonBottom) / 2;

        // Fill button if pressed
        e.gc.setBackground(
                getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
        if (spinIncrement > 0) {
            e.gc.fillRectangle(buttonLeft, buttonTop, buttonRight - buttonLeft,
                    buttonMiddle - buttonTop);
        } else if (spinIncrement < 0) {
            e.gc.fillRectangle(buttonLeft, buttonMiddle,
                    buttonRight - buttonLeft, buttonBottom - buttonMiddle);
        }

        // Draw button borders
        e.gc.drawLine(buttonLeft, buttonTop, buttonLeft, buttonBottom);
        e.gc.drawLine(buttonLeft, buttonMiddle, buttonRight, buttonMiddle);

        // Draw upper arrow
        int arrowLeft = buttonLeft + 4;
        int arrowRight = buttonRight - 4;
        int arrowTip = buttonTop + 4;
        int arrowBase = buttonMiddle - 4;

        e.gc.setForeground(
                getDisplay().getSystemColor(SWT.COLOR_WIDGET_FOREGROUND));
        e.gc.drawLine(arrowLeft, arrowBase, buttonCenter, arrowTip);
        e.gc.drawLine(buttonCenter, arrowTip, arrowRight, arrowBase);

        // Draw lower arrow
        arrowTip = buttonBottom - 4;
        arrowBase = buttonMiddle + 4;

        e.gc.drawLine(arrowLeft, arrowBase, buttonCenter, arrowTip);
        e.gc.drawLine(buttonCenter, arrowTip, arrowRight, arrowBase);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        text.setEnabled(enabled);
        if (!enabled) {
            text.clearSelection();
        }
    }

    /**
     * Returns the selected date/time as a {@link Calendar}
     *
     * @return the selected date/time as a {@link Calendar}
     */
    public Calendar getSelection() {
        return TimeUtil.newCalendar(calendar);
    }

    /**
     * Sets the selected date/time based on the specified {@link Calendar}.
     * onlyEditable will determine whether all Calendar fields will be copied
     * from the specified Calendar or if only the editable fields will be
     * copied.
     *
     * @param calendar
     *            the specified {@link Calendar}
     */
    public void setSelection(Calendar calendar) {
        this.calendar = TimeUtil.newCalendar(calendar);
        if (clearNonEditable) {
            clearNonEditableFields();
        }
        updateControl();
    }

    private void incrementField(int amount) {
        int currentFieldName = editableFields.get(currentField);
        calendar.add(currentFieldName, amount);
        updateControl();
    }

    private void selectField(int index) {
        final int start = fieldIndices.get(index).x;
        final int end = fieldIndices.get(index).y;
        Point pt = text.getSelection();
        if ((index == currentField) && (start == pt.x) && (end == pt.y)) {
            return;
        }
        currentField = index;
        getDisplay().asyncExec(new Runnable() {
            @Override
            public void run() {
                if (!text.isDisposed()) {
                    String value = text.getText(start, end - 1);
                    int s = value.lastIndexOf(' ');
                    if (s == -1) {
                        s = start;
                    } else {
                        s = start + s + 1;
                    }
                    if (text.isEnabled()) {
                        text.setSelection(s, end);
                    }
                }
            }
        });
    }

    private void setField(int value) {
        calendar.set(editableFields.get(currentField), value);
        updateControl();
    }

    private void onKeyDown(Event event) {
        int fieldName;
        switch (event.keyCode) {
        case SWT.ARROW_RIGHT:
            // a right arrow or a valid separator navigates to the field on the
            // right, with wraping
            selectField((currentField + 1) % fieldIndices.size());
            break;
        case SWT.ARROW_LEFT:
            // navigate to the field on the left, with wrapping
            int index = currentField - 1;
            selectField(index < 0 ? fieldIndices.size() - 1 : index);
            break;
        case SWT.ARROW_UP:
        case SWT.KEYPAD_ADD:
            // set the value of the current field to value + 1, with wrapping
            incrementField(+1);
            break;
        case SWT.ARROW_DOWN:
        case SWT.KEYPAD_SUBTRACT:
            // set the value of the current field to value - 1, with wrapping
            incrementField(-1);
            break;
        case SWT.HOME:
            // set the value of the current field to its minimum
            fieldName = editableFields.get(currentField);
            setField(calendar.getActualMinimum(fieldName));
            selectField(currentField);
            break;
        case SWT.END:
            // set the value of the current field to its maximum
            fieldName = editableFields.get(currentField);
            setField(calendar.getActualMaximum(fieldName));
            selectField(currentField);
            break;
        default:
            switch (event.character) {
            case '/':
            case ':':
            case '-':
            case '.':
                // a valid separator navigates to the field on the right, with
                // wrapping
                selectField((currentField + 1) % fieldIndices.size());
                break;
            }
        }
    }

    private void onFocusIn(Event event) {
        selectField(currentField);
    }

    private void onMouseClick(Event event) {
        if (event.button != 1) {
            return;
        }
        Point sel = text.getSelection();
        for (int i = 0; i < fieldIndices.size(); i++) {
            Point p = fieldIndices.get(i);
            if ((p.x <= sel.x) && (sel.x <= p.y)) {
                selectField(i);
                break;
            }
        }
    }

    private void onVerify(Event event) {
        if (ignoreVerify) {
            return;
        }
        event.doit = false;
        int fieldName = editableFields.get(currentField);
        Point indices = fieldIndices.get(currentField);
        int start = indices.x;
        int end = indices.y;
        int length = end - start;
        String newText = event.text;
        if (characterCount > 0) {
            try {
                // parsing just to validate
                Integer.parseInt(newText);
            } catch (NumberFormatException ex) {
                return;
            }
            String value = text.getText(start, end - 1);
            int s = value.lastIndexOf(' ');
            if (s != -1) {
                value = value.substring(s + 1);
            }
            newText = "" + value + newText;
        }
        int newTextLength = newText.length();
        characterCount = (newTextLength < length) ? newTextLength : 0;
        int max = calendar.getActualMaximum(fieldName);
        int min = calendar.getActualMinimum(fieldName);
        int newValue;
        try {
            newValue = Integer.parseInt(newText);
        } catch (NumberFormatException ex) {
            characterCount = 0;
            return;
        }
        if ((min <= newValue) && (newValue <= max)) {
            setField(newValue);
        }
    }

    private String getFormattedString() {
        format.setTimeZone(calendar.getTimeZone());
        return format.format(calendar.getTime());
    }

    @Override
    public void redraw() {
        updateControl();
    }

    private void updateControl() {
        if (text != null) {
            String string = getFormattedString();
            ignoreVerify = true;
            text.setText(string);
            ignoreVerify = false;
        }
        if (text.getEnabled()) {
            selectField(currentField);
        }
        super.redraw();
    }
}
