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

import java.text.DecimalFormat;

import javax.measure.UnitConverter;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;
import org.eclipse.swt.widgets.Text;

import com.raytheon.uf.common.colormap.image.Colormapper;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;
import com.raytheon.uf.common.colormap.prefs.DataMappingPreferences.DataMappingEntry;

import tec.uom.se.AbstractConverter;

/**
 * Composite for Max/Min slider bars and their corresponding Text widgets for
 * ColorMapParameters.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan  3, 2012            mschenke    Initial creation
 * Nov  8, 2013 2492       mschenke    Rewritten to work with colormap
 *                                     units different from data units
 * Aug 04, 2014 3394       rferrel     Added okAction and verify listener on Text widgets.
 * May 07, 2018 7176       bsteffen    Improve handling of edge cases using '>'
 * Apr 15, 2019  7596      lsingh      Updated units framework to JSR-363.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class ColorMapSliderComp extends Composite {

    private static final String NaN_STRING = "NO DATA";

    private static final int SLIDER_MIN = 0;

    private static final int SLIDER_MAX = 250;

    private static final int SLIDER_INC = 1;

    private ColorMapParameters cmap;

    private Scale minSlider;

    private Scale maxSlider;

    private Text minValueText;

    private Text maxValueText;

    private float cmapAbsoluteMin;

    private float cmapAbsoluteMax;

    private final float origCmapMin;

    private final float origCmapMax;

    private float currentCmapMin;

    private float currentCmapMax;

    private final DecimalFormat format;

    private UnitConverter displayToColorMap;

    private UnitConverter colorMapToDisplay;

    /**
     * @param parent
     * @param style
     */
    public ColorMapSliderComp(Composite parent, ColorMapParameters cmap) {
        super(parent, SWT.NONE);
        this.cmap = cmap;
        this.cmapAbsoluteMin = this.currentCmapMin = this.origCmapMin = cmap
                .getColorMapMin();
        this.cmapAbsoluteMax = this.currentCmapMax = this.origCmapMax = cmap
                .getColorMapMax();
        this.displayToColorMap = cmap.getDisplayToColorMapConverter();
        this.colorMapToDisplay = cmap.getColorMapToDisplayConverter();
        if (displayToColorMap == null) {
            displayToColorMap = AbstractConverter.IDENTITY;
        }
        if (colorMapToDisplay == null) {
            colorMapToDisplay = AbstractConverter.IDENTITY;
        }

        updateAbsolutes(cmapAbsoluteMin, cmapAbsoluteMax);

        this.format = getTextFormat();

        initializeComponents();
    }

    public void restore() {
        cmap.setColorMapMin(origCmapMin);
        cmap.setColorMapMax(origCmapMax);
    }

    /**
     * 
     */
    private void initializeComponents() {
        setLayout(new GridLayout(3, false));

        Label maxLabel = new Label(this, SWT.None);
        maxLabel.setText("Max:");

        maxSlider = new Scale(this, SWT.HORIZONTAL);
        maxSlider.setMaximum(SLIDER_MAX);
        maxSlider.setMinimum(SLIDER_MIN);
        maxSlider.setIncrement(SLIDER_INC);
        maxSlider.setSelection(maxSlider.getMaximum());
        GridData layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        layoutData.minimumWidth = 250;
        maxSlider.setLayoutData(layoutData);

        int width = 75;

        GridData labelLayoutData = new GridData(SWT.FILL, SWT.DEFAULT, false,
                false);
        labelLayoutData.widthHint = width;
        maxValueText = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);
        maxValueText.setLayoutData(labelLayoutData);

        Label minLabel = new Label(this, SWT.None);
        minLabel.setText("Min:");

        minSlider = new Scale(this, SWT.HORIZONTAL);
        minSlider.setMaximum(SLIDER_MAX);
        minSlider.setMinimum(SLIDER_MIN);
        minSlider.setIncrement(SLIDER_INC);
        minSlider.setSelection(minSlider.getMinimum());
        layoutData = new GridData(SWT.FILL, SWT.CENTER, true, false);
        layoutData.minimumWidth = 250;
        minSlider.setLayoutData(layoutData);

        labelLayoutData = new GridData(SWT.FILL, SWT.DEFAULT, false, false);
        labelLayoutData.widthHint = width;
        minValueText = new Text(this, SWT.SINGLE | SWT.BORDER | SWT.RIGHT);
        minValueText.setLayoutData(labelLayoutData);

        maxSlider.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setColorMapMax(
                        selectionToColorMapValue(maxSlider.getSelection()));
            }
        });

        minSlider.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                setColorMapMin(
                        selectionToColorMapValue(minSlider.getSelection()));
            }
        });

        // Listener for min/max value text.
        KeyListener keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.character == SWT.CR) {
                    updateMinMax();
                }
            }
        };

        maxValueText.addKeyListener(keyListener);
        minValueText.addKeyListener(keyListener);

        setColorMapMin(currentCmapMin);
        setColorMapMax(currentCmapMax);
    }

    /**
     * Check min/max text values and when valid updates Color Map and sliders to
     * new values.
     * 
     * @return true when update performed else false due to a bad value
     */
    public boolean updateMinMax() {
        boolean invalidMax = Float.isNaN(textToColorMapValue(maxValueText));
        boolean invalidMin = Float.isNaN(textToColorMapValue(minValueText));
        if (invalidMax || invalidMin) {
            StringBuilder sb = new StringBuilder();

            if (invalidMax) {
                sb.append("\"").append(maxValueText.getText().trim())
                        .append("\" invalid Maximum value.\nRevert will change value to: ")
                        .append(currentCmapMax);
            }

            if (invalidMin) {
                if (invalidMax) {
                    sb.append("\n\n");
                }
                sb.append("\"").append(minValueText.getText().trim())
                        .append("\" invalid Minimum value.\nRevert will change value to: ")
                        .append(currentCmapMin);
            }

            sb.append("\n\nCancel will allow editing of text.");
            MessageDialog dialog = new MessageDialog(getShell(),
                    "Invalid Value", null, sb.toString(), MessageDialog.ERROR,
                    new String[] { "Revert", "Cancel" }, 1);

            // User wants to edit.
            if (dialog.open() != 0) {
                if (invalidMax) {
                    maxValueText.forceFocus();
                } else {
                    minValueText.forceFocus();
                }
                return false;
            }
        }

        updateMinMaxFromText(maxValueText);
        updateMinMaxFromText(minValueText);
        return true;
    }

    /**
     * Update slider and the color map. When text contains invalid value revert
     * to the last valid value prior to performing the update.
     * 
     * @param text
     *            min or max text field with new value.
     */
    private void updateMinMaxFromText(Text text) {
        float newCmapValue = textToColorMapValue(text);
        if (Float.isNaN(newCmapValue)) {
            // Change nothing
            if (text == minValueText) {
                newCmapValue = currentCmapMin;
            } else {
                newCmapValue = currentCmapMax;
            }
        } else {
            // Update colormap range
            if (text == minValueText) {
                currentCmapMin = newCmapValue;
            } else {
                currentCmapMax = newCmapValue;
            }
        }

        updateAbsolutes(currentCmapMin, currentCmapMax);

        if (text == minValueText) {
            setColorMapMin(currentCmapMin);
        } else {
            setColorMapMax(currentCmapMax);
        }
    }

    /**
     * 
     */
    private void updateAbsolutes(float cmapAbsoluteMin, float cmapAbsoluteMax) {
        double displayAbsMax = colorMapToDisplay.convert(cmapAbsoluteMax);
        double displayAbsMin = colorMapToDisplay.convert(cmapAbsoluteMin);
        if (displayAbsMax < displayAbsMin) {
            float tmp = cmapAbsoluteMax;
            cmapAbsoluteMax = cmapAbsoluteMin;
            cmapAbsoluteMin = tmp;
        }

        // Add a 1/16 buffer on either side for fine tuning
        float buffer = (cmapAbsoluteMax - cmapAbsoluteMin) * .0625f;
        this.cmapAbsoluteMin = cmapAbsoluteMin - buffer;
        this.cmapAbsoluteMax = cmapAbsoluteMax + buffer;
    }

    /**
     * Converts a slider selection index to a colormap value
     * 
     * @param selection
     * @return
     */
    private float selectionToColorMapValue(int selection) {
        double indexValue = Colormapper.getLinearIndex(selection, SLIDER_MIN,
                SLIDER_MAX);
        double colorMapValue = cmapAbsoluteMin
                + (cmapAbsoluteMax - cmapAbsoluteMin) * indexValue;
        return (float) colorMapValue;
    }

    /**
     * Converts a colormap value to a slider selection index
     * 
     * @param colorMapValue
     * @return
     */
    private int colorMapValueToSelection(float colorMapValue) {
        double indexValue = Colormapper.getLinearIndex(colorMapValue,
                cmapAbsoluteMin, cmapAbsoluteMax);
        return (int) (SLIDER_MIN + (SLIDER_MAX - SLIDER_MIN) * indexValue);
    }

    /**
     * Converts a text string to a colormap float value. The text can either be
     * a float value or the label of data mapping entry.
     * 
     * @param textControl
     *            - contains text string to convert
     * @return Float.NaN when text is an invalid value
     */
    private float textToColorMapValue(Text textControl) {
        String text = textControl.getText().trim();
        if (cmap.getDataMapping() != null && !text.isEmpty()) {
            // First check for data mapping entries
            for (DataMappingEntry entry : cmap.getDataMapping().getEntries()) {
                if (entry.getLabel() != null && text.equals(entry.getLabel())) {
                    return entry.getPixelValue().floatValue();
                }
            }
        }
        if (NaN_STRING.equals(text) || text.startsWith("> ")) {
            // Attempt to parse and convert
            try {
                float currentColorMapValue = textControl == maxValueText
                        ? currentCmapMax : currentCmapMin;
                int currentSliderValue = colorMapValueToSelection(
                        currentColorMapValue);
                if (colorMapValueToText(currentColorMapValue).equals(text)) {
                    return currentColorMapValue;
                }
                int minDist = Integer.MAX_VALUE;
                float bestValue = currentColorMapValue;
                for (int i = SLIDER_MIN; i < SLIDER_MAX; i += SLIDER_INC) {
                    float colorMapValue = selectionToColorMapValue(i);
                    if (colorMapValueToText(colorMapValue).equals(text)) {
                        int dist = Math.abs(i - currentSliderValue);
                        if (dist < minDist) {
                            minDist = dist;
                            bestValue = colorMapValue;
                        }
                    }
                }
                return bestValue;
            } catch (Throwable t) {
                // Ignore, NaN will be returned and text will be reverted
            }
        } else {
            // Attempt to parse and convert
            try {
                float displayValue = Float.parseFloat(text);
                return (float) displayToColorMap.convert(displayValue);
            } catch (Throwable t) {
                // Ignore, NaN will be returned and text will be reverted
            }
        }
        return Float.NaN;
    }

    /**
     * Converts a colormap value into a text display string
     * 
     * @param colorMapValue
     * @return
     */
    private String colorMapValueToText(float colorMapValue) {
        String text = null;
        if (cmap.getDataMapping() != null) {
            text = cmap.getDataMapping()
                    .getLabelValueForDataValue(colorMapValue);
        }
        if (text == null || text.trim().isEmpty()) {
            float displayValue = (float) colorMapToDisplay
                    .convert(colorMapValue);
            if (!Float.isNaN(displayValue)) {
                text = format.format(displayValue);
            } else {
                text = NaN_STRING;
                int selection = colorMapValueToSelection(colorMapValue);
                for (int i = selection; i >= SLIDER_MIN; i -= SLIDER_INC) {
                    displayValue = (float) colorMapToDisplay
                            .convert(selectionToColorMapValue(i));
                    if (!Float.isNaN(displayValue)) {
                        text = "> " + format.format(displayValue);
                        break;
                    }
                }
            }
        }
        return text;
    }

    /**
     * Sets the colormap min value, updates the text and slider
     * 
     * @param colorMapMin
     */
    private void setColorMapMin(float colorMapMin) {
        if (!Float.isNaN(colorMapMin)) {
            currentCmapMin = colorMapMin;
        }
        minSlider.setSelection(colorMapValueToSelection(currentCmapMin));
        minValueText.setText(colorMapValueToText(currentCmapMin));

        cmap.setColorMapMin(currentCmapMin, true);
    }

    /**
     * Sets the colormap max value, updates the text and slider
     * 
     * @param colorMapMax
     */
    private void setColorMapMax(float colorMapMax) {
        if (!Float.isNaN(colorMapMax)) {
            currentCmapMax = colorMapMax;
        }
        maxSlider.setSelection(colorMapValueToSelection(currentCmapMax));
        maxValueText.setText(colorMapValueToText(currentCmapMax));

        cmap.setColorMapMax(currentCmapMax, true);
    }

    private DecimalFormat getTextFormat() {
        if (!cmap.isLogarithmic()) {
            for (int i = SLIDER_MIN; i < SLIDER_MAX; ++i) {
                double cmapValue = selectionToColorMapValue(i);
                double displayValue = colorMapToDisplay.convert(cmapValue);
                if (Double.isNaN(displayValue)) {
                    continue;
                }

                int zeros = 0;
                String val = Double.toString(displayValue);
                char[] vals = val.substring(val.indexOf('.') + 1).toCharArray();
                for (int j = 0; j < vals.length; ++j) {
                    if (vals[j] == '0') {
                        ++zeros;
                    } else {
                        ++zeros;
                        break;
                    }
                }
                zeros = Math.min(3, zeros);

                StringBuilder f = new StringBuilder("0.");
                for (int j = 0; j < zeros; ++j) {
                    f.append("0");
                }
                return new DecimalFormat(f.toString());
            }
        }
        return new DecimalFormat("0.000");
    }

}
