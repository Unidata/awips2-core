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
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.progress.UIJob;

/**
 * Time of day entry field. Heavily borrowed from {@link DateTime}.
 * 
 * Used because DateTime lacks several features including 24-hour time format
 * and continuous adjustment via spin buttons.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 10, 2012            randerso    Initial creation
 * Mar 24, 2014  #1426     lvenable    Fixed arrow buttons so the arrows show up, cleaned up code.
 * Mar 02, 2016  3989      tgurney     Add time and date rollover
 * Mar 02, 2016  3989      tgurney     Make up/down buttons emulate spinner
 *                                     click-and-hold behavior
 * Mar 17, 2016  #5483     randerso    Removed this.setLayoutData(). 
 *                                     This should be done from outside the control
 *                                     since the parent may not have a GridLayout
 * Apr 08, 2016  3989      tgurney     Make text field behavior more intuitive
 * 
 * </pre>
 * 
 * @author randerso
 * @version 1.0
 */

public class TimeEntry extends Composite {

    public static interface IDateChangeCallback {
        /** Called when time rolls over past midnight into previous or next day. */
        public void dateChange();
    }

    private static Point[] fieldIndices = new Point[] { new Point(0, 2),
            new Point(3, 5), new Point(6, 8) };

    private static int[] fieldNames = new int[] { Calendar.HOUR_OF_DAY,
            Calendar.MINUTE, Calendar.SECOND };

    private static String[] formatStrings = new String[] { "HH", "HH:mm",
            "HH:mm:ss" };

    private Text text;

    private Button up;

    private Button down;

    private int fieldCount;

    private int characterCount = 0;

    private int currentField = 0;

    private Calendar calendar;

    private boolean ignoreVerify;

    private IDateChangeCallback callback;

    private UIJob spinnerJob;

    /** Set to true if up or down spin button is currently being pressed. */
    private boolean spinButtonPressed = false;

    private final int SPIN_START_DELAY_MS = 300;

    private final int SPIN_INTERVAL_MS = 30;

    private DateFormat format;

    /**
     * Constructor
     * 
     * @param parent
     * @param fieldCount
     *            1 = HH, 2 = HH:MM 3 = HH:MM:SS
     * @param timeZone
     * @param callback
     *            Object that receives dateChanged() call when time rolls over
     *            past midnight into previous/next day.
     * 
     */
    public TimeEntry(Composite parent, int fieldCount, TimeZone timeZone,
            IDateChangeCallback callback) {
        super(parent, SWT.NONE);
        if ((fieldCount < 1) || (fieldCount > 3)) {
            throw new IllegalArgumentException("fieldCount must be 1, 2, or 3");
        }
        this.fieldCount = fieldCount;
        calendar = Calendar.getInstance(timeZone);
        format = new SimpleDateFormat(formatStrings[fieldCount - 1]);
        format.setTimeZone(timeZone);
        this.callback = callback;
        initializeControls();
    }

    /**
     * Constructor
     * 
     * @param parent
     * @param fieldCount
     *            1 = HH, 2 = HH:MM 3 = HH:MM:SS
     * @param timeZone
     */
    public TimeEntry(Composite parent, int fieldCount, TimeZone timeZone) {
        this(parent, fieldCount, timeZone, null);
    }

    private void initializeControls() {
        GridLayout gl = new GridLayout(2, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.horizontalSpacing = 0;
        this.setLayout(gl);

        text = new Text(this, SWT.SINGLE | SWT.BORDER);
        Listener listener = new Listener() {
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

        text.addListener(SWT.KeyDown, listener);
        text.addListener(SWT.FocusIn, listener);
        text.addListener(SWT.FocusOut, listener);
        text.addListener(SWT.MouseDown, listener);
        text.addListener(SWT.MouseUp, listener);
        text.addListener(SWT.Verify, listener);

        /* Create the up/down buttons and put them in their own composite. */
        Composite buttonComp = new Composite(this, SWT.NONE);
        gl = new GridLayout(1, false);
        gl.marginWidth = 0;
        gl.marginHeight = 0;
        gl.verticalSpacing = 0;
        buttonComp.setLayout(gl);

        int buttonWidth = 22;
        int buttonHeight = 20;

        Listener spinButtonListener = new Listener() {
            @Override
            public void handleEvent(Event event) {
                switch (event.type) {
                case SWT.MouseDown:
                    handleSpinButtonPressed((int) event.widget.getData());
                    break;
                case SWT.MouseUp:
                    spinButtonPressed = false;
                    if (spinnerJob != null) {
                        spinnerJob.cancel();
                        spinnerJob = null;
                    }
                    text.setFocus();
                    break;
                }
            }
        };

        GridData gd = new GridData(buttonWidth, buttonHeight);
        up = new Button(buttonComp, SWT.ARROW | SWT.UP);
        up.setLayoutData(gd);
        up.setData(1);
        up.addListener(SWT.MouseDown, spinButtonListener);
        up.addListener(SWT.MouseUp, spinButtonListener);

        gd = new GridData(buttonWidth, buttonHeight);
        down = new Button(buttonComp, SWT.ARROW | SWT.DOWN);
        down.setLayoutData(gd);
        down.setData(-1);
        down.addListener(SWT.MouseDown, spinButtonListener);
        down.addListener(SWT.MouseUp, spinButtonListener);

        updateControl();
    }

    /**
     * Called when spin button is pressed. Queues up a timer task that will
     * continuously adjust the spinner value up or down until the button is
     * released.
     * 
     * @param incrementAmount
     *            How much to increment the spinner field by on each iteration.
     *            (Typically 1 or -1)
     */
    private void handleSpinButtonPressed(final int incrementAmount) {
        spinButtonPressed = true;
        incrementField(incrementAmount);
        spinnerJob = new UIJob(Display.getDefault(),
                "Handle Spin Button Pressed") {
            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                if (spinButtonPressed && !text.isDisposed()) {
                    incrementField(incrementAmount);
                    this.schedule(SPIN_INTERVAL_MS);
                }
                return Status.OK_STATUS;
            }
        };
        spinnerJob.schedule(SPIN_START_DELAY_MS);
    }

    private void incrementField(int amount) {
        int currentFieldName = fieldNames[currentField];
        int oldDate = calendar.get(Calendar.DAY_OF_MONTH);
        calendar.add(currentFieldName, amount);
        updateControl();
        int newDate = calendar.get(Calendar.DAY_OF_MONTH);
        if (callback != null && newDate != oldDate) {
            callback.dateChange();
        }
    }

    private void selectField(int index) {
        final int start = fieldIndices[index].x;
        final int end = fieldIndices[index].y;
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
                    text.setSelection(s, end);
                }
            }
        });
    }

    private void setField(int value) {
        calendar.set(fieldNames[currentField], value);
        updateControl();
    }

    private void onKeyDown(Event event) {
        int fieldName;
        switch (event.keyCode) {
        case SWT.ARROW_RIGHT:
            // a right arrow or a valid separator navigates to the field on the
            // right, with wrapping
            selectField((currentField + 1) % fieldCount);
            break;
        case SWT.ARROW_LEFT:
            // navigate to the field on the left, with wrapping
            int index = currentField - 1;
            selectField(index < 0 ? fieldCount - 1 : index);
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
            fieldName = fieldNames[currentField];
            setField(calendar.getActualMinimum(fieldName));
            selectField(currentField);
            break;
        case SWT.END:
            // set the value of the current field to its maximum
            fieldName = fieldNames[currentField];
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
                selectField((currentField + 1) % fieldCount);
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
        for (int i = 0; i < fieldCount; i++) {
            if ((fieldIndices[i].x <= sel.x) && (sel.x <= fieldIndices[i].y)) {
                selectField(i);
                break;
            }
        }
    }

    private void handleTextInput(String input) {
        int currentFieldName = fieldNames[currentField];
        String newText = input;
        try {
            Integer.parseInt(newText);
        } catch (NumberFormatException ex) {
            if (!input.equals("")) {
                characterCount = 0;
            }
            setField(calendar.get(currentFieldName));
            return;
        }
        if (!newText.equals("0") && characterCount == 0) {
            characterCount += 1;
        } else if (characterCount != 0) {
            String oldText = String.valueOf(calendar.get(currentFieldName));
            newText = String.valueOf(oldText + newText);
            characterCount = 0;
        }
        int newValue;
        try {
            newValue = Integer.parseInt(newText);
        } catch (NumberFormatException ex) {
            characterCount = 0;
            return;
        }
        int max = calendar.getActualMaximum(currentFieldName);
        int min = calendar.getActualMinimum(currentFieldName);
        if (min <= newValue && newValue <= max) {
            setField(newValue);
        } else if (newText.length() > 1) {
            String lastInputDigit = newText.substring(newText.length() - 1);
            handleTextInput(lastInputDigit);
        } else {
            setField(calendar.get(fieldNames[currentField]));
        }
    }

    private void onVerify(Event event) {
        if (ignoreVerify) {
            return;
        }
        event.doit = false;
        handleTextInput(event.text);
    }

    public String getFormattedString() {
        format.setTimeZone(calendar.getTimeZone());
        return format.format(calendar.getTime());
    }

    private void updateControl() {
        if (text != null) {
            String string = getFormattedString();
            ignoreVerify = true;
            text.setText(string);
            ignoreVerify = false;
        }
        selectField(currentField);
        redraw();
    }

    public void setTime(Date time) {
        checkWidget();
        calendar.setTime(time);
        updateControl();
    }

    public Date getTime() {
        checkWidget();
        return calendar.getTime();
    }

    public void setTimeZone(TimeZone timeZone) {
        calendar.setTimeZone(timeZone);
        format.setTimeZone(timeZone);
    }

}
