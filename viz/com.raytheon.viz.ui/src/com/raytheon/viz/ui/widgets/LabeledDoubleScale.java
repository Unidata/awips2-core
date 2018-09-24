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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Composite;

/**
 * This class displays a composite containing a scale control and a canvas that
 * displays the value above the scale control.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 27 Oct 2008             lvenable    Initial creation.
 * Nov 14, 2008            randerso    reworked as a subclass of new LabeledScale class
 * Aug 31, 2018  6588      dgilling    Support changes to LabeledScale.
 *
 * </pre>
 *
 * @author lvenable
 *
 */
public class LabeledDoubleScale extends LabeledScale {
    private double scaleFactor;

    private double offset;

    private double minimum = 0.0;

    private double maximum = 1.0;

    private DecimalFormat df;

    private double resolution = 0.01;

    public LabeledDoubleScale(Composite parent) {
        this(parent, false);
    }

    public LabeledDoubleScale(Composite parent, boolean drawScaleRangeValues) {
        super(parent, drawScaleRangeValues);

        df = new DecimalFormat();
        setFractionalDigits(1);

        updateScaleFactors();
    }

    private void updateScaleFactors() {
        setMinimum(0);
        setMaximum((int) Math.min(Integer.MAX_VALUE, Math
                .round((maximum - minimum) / resolution)));

        offset = minimum - getMinimum();
        scaleFactor = (maximum - minimum) / (getMaximum() - getMinimum());
    }

    public double getDoubleValue() {
        return getSelection() * scaleFactor + offset;
    }

    public void setDoubleValue(double doubleValue) {
        setSelection((int) Math.round((doubleValue - offset) / scaleFactor));
    }

    public void setFractionalDigits(int digits) {
        df.setMinimumFractionDigits(digits);
        df.setMaximumFractionDigits(digits);
    }

    @Override
    protected void paintScaleValueCanvas(GC gc) {
        drawScaleValue(gc, scaleValueCanvas, getSelection(),
                df.format(getDoubleValue()));
    }

    @Override
    protected void paintScaleRangeCanvas(GC gc) {
        double[] labelValues = calculateDoubleRangeLabels(gc);
        for (double value : labelValues) {
            int scaleValue = (int) Math.round((value - offset) / scaleFactor);
            drawScaleValue(gc, scaleRangeCanvas, scaleValue, df.format(value));
        }
    }

    public double getDoubleMinimum() {
        return minimum;
    }

    public void setDoubleMinimum(double minimum) {
        this.minimum = minimum;
        updateScaleFactors();
    }

    public double getDoubleMaximum() {
        return maximum;
    }

    public void setDoubleMaximum(double maximum) {
        this.maximum = maximum;
        updateScaleFactors();
    }

    public double getResolution() {
        return resolution;
    }

    public void setResolution(double resolution) {
        if (resolution <= 0.0) {
            throw new IllegalArgumentException(
                    "resolution must be greater than 0.0");
        }
        this.resolution = resolution;
        updateScaleFactors();
    }

    protected double[] calculateDoubleRangeLabels(final GC gc) {
        int labelSpacing = gc.textExtent(" ").x * 3 / 2;
        int labelWidth = Math.max(gc.textExtent(df.format(minimum)).x,
                gc.textExtent(df.format(maximum)).x);

        int barWidth = gc.getClipping().width;
        int totalRange = (int) (maximum - minimum);

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

        double[] labelValues = new double[numLabels];
        for (int i = 0; i < numLabels - 1; i++) {
            labelValues[i] = minimum + labelInc * i;
        }
        labelValues[numLabels - 1] = maximum;

        return labelValues;
    }
}
