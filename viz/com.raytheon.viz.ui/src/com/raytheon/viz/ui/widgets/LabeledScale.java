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

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Scale;

/**
 * This class implements a scale which displays its value above the thumb.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Nov 14, 2008            randerso     Initial creation
 * Aug 31, 2018            dgilling     Add ability to add lower canvas that
 *                                      displays a range bar.
 * </pre>
 *
 * @author randerso
 * @version 1.0
 */

public class LabeledScale extends Composite {

    /**
     * Canvas displaying the value of the scale control.
     */
    protected Canvas scaleValueCanvas;

    /**
     * The scale control.
     */
    protected Scale scale;

    protected Canvas scaleRangeCanvas;

    private NumberFormat formatter = new DecimalFormat("0");

    public LabeledScale(Composite parent) {
        this(parent, false);
    }

    /**
     * Constructor.
     *
     * @param parent
     *            Parent composite.
     */
    public LabeledScale(Composite parent, boolean drawScaleRangeValues) {
        super(parent, SWT.NONE);

        GridLayout layout = new GridLayout(1, false);
        layout.verticalSpacing = 0;
        layout.horizontalSpacing = 0;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        this.setLayout(layout);

        scaleValueCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GC gc = new GC(this.getDisplay());
        gc.setFont(this.getFont());
        int fontHeight = gc.getFontMetrics().getHeight();
        gd.heightHint = fontHeight;
        gc.dispose();
        scaleValueCanvas.setLayoutData(gd);
        scaleValueCanvas.addPaintListener((e) -> {
            e.gc.setFont(getFont());
            paintScaleValueCanvas(e.gc);
        });

        gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        scale = new Scale(this, SWT.HORIZONTAL);
        scale.setLayoutData(gd);
        scale.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                scaleValueCanvas.redraw();
            }
        });

        if (drawScaleRangeValues) {
            scaleRangeCanvas = new Canvas(this, SWT.DOUBLE_BUFFERED);
            scaleRangeCanvas.addPaintListener((e) -> {
                e.gc.setFont(getFont());
                paintScaleRangeCanvas(e.gc);
            });
            gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
            gd.heightHint = fontHeight;
            scaleRangeCanvas.setLayoutData(gd);
        }
    }

    protected void paintScaleValueCanvas(final GC gc) {
        int selectedValue = getSelection();
        drawScaleValue(gc, scaleValueCanvas, selectedValue,
                formatter.format(selectedValue));
    }

    protected void paintScaleRangeCanvas(final GC gc) {
        int[] labelValues = calculateRangeLabels(gc);
        for (int value : labelValues) {
            drawScaleValue(gc, scaleRangeCanvas, value,
                    formatter.format(value));
        }
    }

    /**
     * Draw the scale value on the canvas.
     *
     * @param gc
     *            Graphic context.
     */
    protected void drawScaleValue(GC gc, Canvas canvas, int scaleValue,
            String stringValue) {
        Point extent = gc.textExtent(stringValue);

        int x = (canvas.getBounds().width - extent.x) * scaleValue
                / (scale.getMaximum() - scale.getMinimum());

        gc.drawString(stringValue, x, 1, true);
    }

    public int getSelection() {
        return scale.getSelection();
    }

    public int getMinimum() {
        return scale.getMinimum();
    }

    public int getMaximum() {
        return scale.getMaximum();
    }

    public int getIncrement() {
        return scale.getIncrement();
    }

    public int getPageIncrement() {
        return scale.getPageIncrement();
    }

    public void setSelection(int value) {
        scale.setSelection(value);
    }

    public void setMinimum(int value) {
        scale.setMinimum(value);
    }

    public void setMaximum(int value) {
        scale.setMaximum(value);
    }

    public void setIncrement(int value) {
        scale.setIncrement(value);
    }

    public void setPageIncrement(int value) {
        scale.setPageIncrement(value);
    }

    public void addSelectionListener(SelectionListener listener) {
        scale.addSelectionListener(listener);
    }

    public void removeSelectionListener(SelectionListener listener) {
        scale.removeSelectionListener(listener);
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        GC gc = new GC(this.getDisplay());
        gc.setFont(font);
        int fontHeight = gc.getFontMetrics().getHeight();
        ((GridData) scaleValueCanvas.getLayoutData()).heightHint = fontHeight;
        if (scaleRangeCanvas != null) {
            ((GridData) scaleRangeCanvas
                    .getLayoutData()).heightHint = fontHeight;
        }
        gc.dispose();
    }

    public void setFormatter(NumberFormat formatter) {
        this.formatter = formatter;
    }

    protected int[] calculateRangeLabels(final GC gc) {
        int labelSpacing = gc.textExtent(" ").x * 3 / 2;
        int labelWidth = Math.max(
                gc.textExtent(formatter.format(getMinimum())).x,
                gc.textExtent(formatter.format(getMaximum())).x);

        int barWidth = scaleRangeCanvas.getBounds().width;
        int totalRange = getMaximum() - getMinimum();

        int labelInc = 1;
        int numLabels = totalRange + 1;
        while (numLabels * (labelWidth + labelSpacing) > barWidth) {
            labelInc++;
            if (totalRange % labelInc != 0) {
                continue;
            }

            numLabels = totalRange / labelInc + 1;

            if (numLabels == 2) {
                break;
            }
        }

        int[] labelValues = new int[numLabels];
        for (int i = 0; i < numLabels - 1; i++) {
            labelValues[i] = getMinimum() + labelInc * i;
        }
        labelValues[numLabels - 1] = getMaximum();

        return labelValues;
    }
}
