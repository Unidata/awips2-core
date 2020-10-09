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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;

import com.raytheon.viz.ui.UiPlugin;
import com.raytheon.viz.ui.dialogs.CalendarDialog;

/**
 * Date/Time entry widget with read-only text and button to pop up and editable
 * dialog to enter the date and time.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Dec 6,  2012             randerso    Initial creation
 * Apr 9,  2013  1860       randerso    Fix image disposed issued on Windows
 * Mar 1,  2016  3989       tgurney     Rename AwipsCalendar to CalendarDialog
 * Sep 29, 2017  6461       tgurney     Add dateFormat to constructor
 * Dec 31, 2019  7996       skabasele   Add ability to make textbox editable &
 *                                      updated getDate()
 * Jun 16, 2020  7996       skabasele   setLeniency to false in getDate()
 *
 * </pre>
 *
 * @author randerso
 */

public class DateTimeEntry extends Composite {
    /**
     * Interface for a listener to receive notification when a DateTimeEntry
     * widget is updated
     */
    public static interface IUpdateListener {
        /**
         * Called when the associated DateTimeEntry widget is updated.
         *
         * @param date
         *            the newly updated date
         */
        public void dateTimeUpdated(Date date);
    }

    private static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private SimpleDateFormat sdf;

    private String dateFormat;

    private Date date;

    private Text text;

    private ListenerList listeners;

    /**
     * Construct a DateTimeEnty widget
     *
     * @param parent
     */
    public DateTimeEntry(Composite parent, String dateFormat) {
        super(parent, SWT.NONE);
        this.dateFormat = dateFormat;
        sdf = new SimpleDateFormat(dateFormat);
        date = new Date();
        listeners = new ListenerList();

        GridLayout layout = new GridLayout(2, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 0;
        setLayout(layout);

        FocusAdapter myFocusAdapter = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent focusEvent) {
              getDate();
            }
          
        };

        KeyAdapter keyAdapter = new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent arg0) {
                if (arg0.keyCode == SWT.CR || arg0.keyCode == SWT.KEYPAD_CR) {
                    getDate();
                }
            }

           
        };
        text = new Text(this, SWT.BORDER | SWT.READ_ONLY);
        GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, true);
        text.setLayoutData(layoutData);
        text.addFocusListener(myFocusAdapter);
        text.addKeyListener(keyAdapter);
        Button button = new Button(this, SWT.PUSH);
        ImageDescriptor imageDesc = UiPlugin
                .getImageDescriptor("icons/calendar.gif");
        final Image image = imageDesc.createImage();
        button.setImage(image);
        button.addDisposeListener(new DisposeListener() {

            @Override
            public void widgetDisposed(DisposeEvent e) {
                image.dispose();
            }
        });

        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                int fieldCount = getFieldCount();
                CalendarDialog dlg;
                Date dateEntered = getDate();
                // if the user has entered a valid date in the text then reflect
                // it in the
                // calendar
                if (dateEntered != null) {
                    dlg = new CalendarDialog(getShell(), dateEntered,
                            fieldCount);
                } else {
                    // Otherwise default to current date

                    dlg = new CalendarDialog(getShell(), date, fieldCount);
                }
                dlg.setTimeZone(sdf.getTimeZone());
                Date selectedDate = (Date) dlg.open();
                if (selectedDate != null) {
                    updateDate(selectedDate);
                }
            }
        });

        updateDate(this.date);
    }

    public DateTimeEntry(Composite parent) {
        this(parent, DEFAULT_DATE_FORMAT);
    }

    private int getFieldCount() {
        int fieldCount = 0;
        if (dateFormat.contains("s")) {
            fieldCount = 3;
        } else if (dateFormat.contains("m")) {
            fieldCount = 2;
        } else if (dateFormat.contains("h") || dateFormat.contains("H")) {
            fieldCount = 1;
        }
        return fieldCount;
    }

    private void updateDate(Date date) {
        text.setText(sdf.format(date));
        try {
            this.date = sdf.parse(text.getText());
        } catch (ParseException e) {
            // should never happen since we just formatted the string
            // using the same SimpleDateFormat
        }

        fireUpdateListeners(this.date);
    }

    /**
     * Sets the date format string used to format the date/time string for
     * display
     *
     * @param dateFormat
     *            the date format string see {@link SimpleDateFormat} for more
     *            information.
     */
    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
        this.sdf.applyPattern(dateFormat);
        updateDate(date);
    }

    /**
     * Sets the time zone used to format the date/time string for display
     *
     * @param tz
     *            the time zone
     */
    public void setTimeZone(TimeZone tz) {
        sdf.setTimeZone(tz);
        updateDate(date);
    }

    /**
     * Sets the contents of the receiver to the given date
     *
     * @param date
     *            the date
     */
    public void setDate(Date date) {
        updateDate(date);
    }

    /**
     * Retrieve the date from the widget. When The Text is editable, this method
     * will return null if the user hasn't entered a valid date.
     *
     * @return the date
     */
    public Date getDate() {
        if (text.getEditable()) {
            try {
                // only for this particular parse
                sdf.setLenient(false);
                this.date = sdf.parse(text.getText());
                fireUpdateListeners(date);
            } catch (ParseException e) {
                return null;
            }finally{
               // reset to default
               sdf.setLenient(true);
            }

            
        }
        return this.date;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (Control c : getChildren()) {
            c.setEnabled(enabled);
        }
    }

    /**
     * Add a listener to be called when this widget is updated.
     *
     * @param listener
     */
    public void addUpdateListener(IUpdateListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove a listener
     *
     * @param listener
     */
    public void removeUpdateListener(IUpdateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notify listeners of the updated date
     *
     * @param date
     *            the updated date
     */
    private void fireUpdateListeners(Date date) {
        for (Object listener : listeners.getListeners()) {
            ((IUpdateListener) listener).dateTimeUpdated(date);
        }
    }

    /**
     * Method used to allow the text box to be editable
     * 
     * @param value
     */
    public void setTextBoxEditable(boolean value) {
        text.setEditable(value);

    }

}
