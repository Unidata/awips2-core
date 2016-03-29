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

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;

import com.raytheon.uf.common.time.SimulatedTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import com.raytheon.viz.ui.widgets.TimeEntry;

/**
 * Awips Calendar Date Selection Dialog.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 9, 2012             mpduff      Initial creation
 * Mar 24, 2014 #1426      lvenable    Removed unnecessary code, cleaned up code.
 * Jan 15, 2016 #5054      randerso    Change to subclass CaveSWTDialog
 * Mar 1, 2016  3989       tgurney     Rename AwipsCalendar to CalendarDialog
 * Mar 2, 2016  3989       tgurney     Add time and date rollover
 * Mar 17, 2016 #5483      randerso    Added timeEntry.setLayoutData()

 * 
 * </pre>
 * 
 * @author mpduff
 * @version 1.0
 */

public class CalendarDialog extends CaveSWTDialog implements
        TimeEntry.IDateChangeCallback {

    private Calendar cal;

    /** The date selection calendar widget */
    private DateTime calendarWidget;

    /** the time selection widget */
    private TimeEntry timeEntryWidget;

    private int timeFieldCount;

    /**
     * Constructor.
     * 
     * @param parentShell
     */
    public CalendarDialog(Shell parentShell) {
        this(parentShell, null, 0);
    }

    /**
     * Constructor.
     * 
     * @param parentShell
     * @param timeFieldCount
     *            number of time fields to display
     * 
     *            <pre>
     *   0 - do not display time field 
     *   1 - display hours 
     *   2 - display hours and minutes 
     *   3 - display hours, minutes, and seconds
     * </pre>
     */
    public CalendarDialog(Shell parentShell, int timeFieldCount) {
        this(parentShell, null, timeFieldCount);
    }

    /**
     * Constructor.
     * 
     * @param parentShell
     * @param initialDate
     *            Date/time to preset the calendar widget (null = current
     *            simulated time)
     * @param timeFieldCount
     *            number of time fields to display
     * 
     *            <pre>
     *   0 - do not display time field 
     *   1 - display hours 
     *   2 - display hours and minutes
     *   3 - display hours, minutes, and seconds
     * </pre>
     */
    public CalendarDialog(Shell parentShell, Date initialDate,
            int timeFieldCount) {
        super(parentShell, SWT.DIALOG_TRIM);
        setText("Calendar");
        if ((timeFieldCount < 0) || (timeFieldCount > 3)) {
            throw new IllegalArgumentException(
                    "timeFieldCount must be 0, 1, 2, or 3");
        }
        this.timeFieldCount = timeFieldCount;
        cal = TimeUtil.newCalendar(TimeZone.getTimeZone("GMT"));
        if (initialDate != null) {
            cal.setTime(initialDate);
        } else {
            cal.setTime(SimulatedTime.getSystemTime().getTime());
        }
    }

    /**
     * The time zone used for displaying the time field
     * 
     * @param timeZone
     */
    public void setTimeZone(TimeZone timeZone) {
        cal.setTimeZone(timeZone);
        if (timeEntryWidget != null) {
            timeEntryWidget.setTimeZone(timeZone);
        }
    }

    @Override
    protected Layout constructShellLayout() {
        return new GridLayout(2, false);
    }

    @Override
    protected Object constructShellLayoutData() {
        return new GridData(SWT.FILL, SWT.DEFAULT, true, false);
    }

    @Override
    protected void initializeComponents(Shell shell) {
        if (timeFieldCount > 0) {
            Label lbl = new Label(shell, SWT.NONE);
            GridData layoutData = new GridData(SWT.LEFT, SWT.CENTER, true,
                    false);
            lbl.setLayoutData(layoutData);

            TimeZone tz = cal.getTimeZone();
            String tzId = "Z";
            if (tz.getRawOffset() != 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("z");
                sdf.setTimeZone(tz);
                // date below is not used
                // we just want to get the tz abbreviation
                tzId = sdf.format(new Date());
            }

            if (timeFieldCount == 1) {
                lbl.setText("Select Hour (" + tzId + ") and Date: ");
            } else {
                lbl.setText("Select Time (" + tzId + ") and Date: ");
            }

            timeEntryWidget = new TimeEntry(shell, timeFieldCount, tz, this);
            timeEntryWidget.setTime(cal.getTime());
            timeEntryWidget.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, true,
                    false));
        }

        calendarWidget = new DateTime(shell, SWT.CALENDAR | SWT.BORDER);
        GridData layoutData = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        layoutData.horizontalSpan = 2;
        calendarWidget.setLayoutData(layoutData);
        calendarWidget.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        calendarWidget.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                // Update the time entry widget when new date is picked
                if (timeEntryWidget != null) {
                    cal.setTime(timeEntryWidget.getTime());
                }
                cal.set(calendarWidget.getYear(), calendarWidget.getMonth(),
                        calendarWidget.getDay());
                if (timeEntryWidget != null) {
                    timeEntryWidget.setTime(cal.getTime());
                }
            }
        });

        createButtons();
    }

    /**
     * Create the buttons
     */
    private void createButtons() {
        int buttonWidth = 75;
        GridData btnData = new GridData(buttonWidth, SWT.DEFAULT);

        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gd.horizontalSpan = 2;
        GridLayout gl = new GridLayout(2, false);
        Composite btnComp = new Composite(shell, SWT.NONE);
        btnComp.setLayout(gl);
        btnComp.setLayoutData(gd);

        Button okBtn = new Button(btnComp, SWT.PUSH);
        okBtn.setText("OK");
        okBtn.setLayoutData(btnData);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleOk();
                shell.dispose();
            }
        });

        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        cancelBtn.setLayoutData(btnData);
        cancelBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                shell.dispose();
            }
        });
    }

    /**
     * Event handler action for OK button
     */
    private void handleOk() {
        if (timeEntryWidget != null) {
            cal.setTime(timeEntryWidget.getTime());
        }
        if (timeFieldCount == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        }
        cal.set(Calendar.MILLISECOND, 0);
        this.setReturnValue(cal.getTime());
    }

    @Override
    public void dateChange() {
        if (timeEntryWidget != null) {
            cal.setTime(timeEntryWidget.getTime());
        }
        calendarWidget.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
    }

    /**
     * Main
     * 
     * @param args
     */
    public static void main(String[] args) {
        CalendarDialog ac = new CalendarDialog(new Shell(), 3);
        ac.setTimeZone(TimeZone.getDefault());
        Date date = (Date) ac.open();
        if (date == null) {
            System.out.println("null");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss'Z'");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            System.out.println(sdf.format(date));
        }
    }
}
