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
import com.raytheon.viz.ui.widgets.DateTimeSpinner;

/**
 * AWIPS Calendar Date Selection Dialog.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -------------------------------------------
 * Jan 09, 2012           mpduff    Initial creation
 * Mar 24, 2014  1426     lvenable  Removed unnecessary code, cleaned up code.
 * Jan 15, 2016  5054     randerso  Change to subclass CaveSWTDialog
 * Mar 01, 2016  3989     tgurney   Rename AwipsCalendar to CalendarDialog
 * Mar 02, 2016  3989     tgurney   Add time and date rollover
 * Mar 17, 2016  5483     randerso  Added timeEntry.setLayoutData()
 * Dec 11, 2019  7994     randerso  Replace TimeEntry with DateTimeSpinner. Code cleanup.
 *
 * </pre>
 *
 * @author mpduff
 */

public class CalendarDialog extends CaveSWTDialog {

    private static final String[] formatStrings = new String[] { "HH", "HH:mm",
            "HH:mm:ss" };

    private static final int[] timeFields = new int[] { Calendar.HOUR_OF_DAY,
            Calendar.MINUTE, Calendar.SECOND };

    private Calendar cal;

    /** The date selection calendar widget */
    private DateTime calendarWidget;

    /** the time selection widget */
    private DateTimeSpinner timeEntryWidget = null;

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
     *            </pre>
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
     *            </pre>
     */
    public CalendarDialog(Shell parentShell, Date initialDate,
            int timeFieldCount) {
        super(parentShell, SWT.DIALOG_TRIM | SWT.RESIZE);
        setText("Calendar");
        if ((timeFieldCount < 0) || (timeFieldCount > formatStrings.length)) {
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
        cal.set(Calendar.MILLISECOND, 0);
    }

    /**
     * The time zone used for displaying the time field
     *
     * @param timeZone
     */
    public void setTimeZone(TimeZone timeZone) {
        cal.setTimeZone(timeZone);
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

            timeEntryWidget = new DateTimeSpinner(shell, cal,
                    formatStrings[timeFieldCount - 1], false);
            timeEntryWidget.setLayoutData(
                    new GridData(SWT.DEFAULT, SWT.DEFAULT, false, false));
        }

        calendarWidget = new DateTime(shell, SWT.CALENDAR | SWT.BORDER);
        GridData layoutData = new GridData(SWT.DEFAULT, SWT.DEFAULT, false,
                false);
        layoutData.horizontalSpan = 2;
        calendarWidget.setLayoutData(layoutData);
        calendarWidget.setDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        calendarWidget.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                cal.set(calendarWidget.getYear(), calendarWidget.getMonth(),
                        calendarWidget.getDay());
            }
        });

        createButtons();
    }

    /**
     * Create the buttons
     */
    private void createButtons() {
        GridData gd = new GridData(SWT.CENTER, SWT.DEFAULT, true, false);
        gd.horizontalSpan = 2;
        GridLayout gl = new GridLayout(2, true);
        Composite btnComp = new Composite(shell, SWT.NONE);
        btnComp.setLayout(gl);
        btnComp.setLayoutData(gd);

        Button okBtn = new Button(btnComp, SWT.PUSH);
        okBtn.setText("OK");
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        okBtn.setLayoutData(gd);
        okBtn.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                handleOk();
                shell.dispose();
            }
        });

        Button cancelBtn = new Button(btnComp, SWT.PUSH);
        cancelBtn.setText("Cancel");
        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        cancelBtn.setLayoutData(gd);
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
        cal.set(Calendar.MILLISECOND, 0);

        Calendar timeCal = null;
        if (timeFieldCount > 0) {
            timeCal = timeEntryWidget.getSelection();
        }

        for (int i = 0; i < timeFields.length; i++) {
            int field = timeFields[i];

            if (i < timeFieldCount) {
                cal.set(field, timeCal.get(field));
            } else {
                cal.set(field, 0);
            }
        }

        this.setReturnValue(cal.getTime());
    }
}
