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

package com.raytheon.viz.ui.dialogs.colordialog;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.measure.UnitConverter;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

import com.raytheon.uf.common.colormap.image.Colormapper;
import com.raytheon.uf.common.colormap.prefs.ColorMapParameters;

/**
 * A Composite that renders a colorbar and also arrows and buttons to
 * manipulate/edit it.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Apr 08, 2014  2950     bsteffen    Support dynamic color counts and resizing.
 *                                    for color editor's color bar for radar
 *                                    correlation coefficient.
 * Apr 11, 2014  15811    Qinglu Lin  Added decimalPlaceMap and logic to have 4
 *                                    decimal places
 * May 07, 2015  17219    jgerth      Allow user to interpolate alpha only
 * Feb 04, 2016  5301     tgurney     Fix undoColorBar and redoColorBar return
 *                                    values
 * Feb 27, 2017  6116     mapeters    Don't hardcode size of nav buttons
 * Feb 07, 2018  6816     randerso    Fix colorbar labeling for GFE logFactor
 *                                    color maps
 * Feb 22, 2018  6668     bsteffen    Move sliders to color edges.
 * Mar 19, 2018  6738     tgurney     Add getHistory, setHistory
 * Apr 17, 2018  6972     bsteffen    Correctly label log colormaps.
 * May 03, 2018  7285     randerso    Added contrastStretch. Changed setColors
 *                                    to use contrastStretch.
 * Jul 31, 2018  6668     bsteffen    Fix slider labels.
 * Jul 16, 2021  8591     randerso    Increased width of color bar for easier
 *                                    color selection. Changed bottom slider
 *                                    text to match the color of the arrow.
 *
 * </pre>
 *
 */
public class ColorBar extends Composite
        implements MouseListener, MouseMoveListener {

    /** Size of the slider arrows. */
    protected final Point ARROW_SIZE = new Point(10, 28);

    /** Size of the margin. */
    protected final Point MARGIN_SIZE = new Point(6, 6);

    /** Size of the bar. */
    protected final Point BAR_SIZE = new Point(686, 50);

    /** Callback used to update the color wheel. */
    protected IColorBarAction callBack;

    /** Canvas where the color bar is drawn. */
    protected Canvas colorBarCanvas;

    /** Font of arrow labels */
    protected Font font;

    /** Navigation Buttons */
    protected List<Button> navButtons = new ArrayList<>();

    /** Set on mouse down to move top slider when mouse is moved. */
    protected boolean moveTopSlider = false;

    /** Set on mouse down to move bottom slider when mouse is moved. */
    protected boolean moveBottomSlider = false;

    /**
     * Flag indicating if the color bar should be queried for the color where
     * the mouse is at (top of the color bar).
     */
    protected boolean queryTopColorBar = false;

    /**
     * Flag indicating if the color bar should be queried for the color where
     * the mouse is at (bottom of the color bar).
     */
    protected boolean queryBottomColorBar = false;

    /** Top slider index into current color list. */
    protected int topSliderIndex = 0;

    /** Bottom slider index into current color list. */
    protected int bottomSliderIndex = 0;

    /**
     * Index into the history list. This must be kept consistent with
     * colorHistory.
     */
    protected int currentColorIndex = 0;

    /**
     * List of color arrays to keep track of changes made to the color bar.
     * There must always be at least one item in this list representing the
     * current colors
     */
    protected List<List<ColorData>> colorHistory;

    /** The colormap parameters to use to initialize the bar */
    protected ColorMapParameters cmapParams;

    /**
     * When enabled, the canvas will look at the alphaMask in the
     * paintProperties
     */
    protected boolean enabledColorMask = false;

    /** The color bar image */
    protected Image colorBar;

    /** The color bar image with mask applied */
    protected Image colorBarWithMask;

    protected Point mouseDragStart = null;

    private final Map<String, String> decimalPlaceMap = new HashMap<String, String>() {
        private static final long serialVersionUID = 1L;

        {
            /*
             * keys are the last portion of the title in the color table editor
             * for a specific radar product, in lower case and value is the
             * decimals expected to be displayed in color bar.
             */
            put("correlation coeff", "0000");
        }
    };

    private NumberFormat numberFormat = null;

    public static class History {

        public int currentColorIndex = 0;

        public List<List<ColorData>> colorHistory = new ArrayList<>();

        public History(int currentColorIndex,
                List<List<ColorData>> colorHistory) {
            this.currentColorIndex = currentColorIndex;
            for (List<ColorData> colorDatas : colorHistory) {
                this.colorHistory.add(new ArrayList<>(colorDatas));
            }
        }

        public List<ColorData> getCurrentColors() {
            return this.colorHistory.get(currentColorIndex);
        }

    }

    /**
     * Constructor.
     *
     * @param parent
     *            Parent shell.
     * @param callback
     *            Callback to be called when color updates are needed.
     * @param cmapParams
     *            The colormap paramters to use to initalize the bar
     */
    public ColorBar(Composite parent, IColorBarAction callback,
            ColorMapParameters cmapParams) {
        this(parent, callback, cmapParams, false);
    }

    /**
     * Constructor.
     *
     * @param parent
     *            Parent shell.
     * @param callback
     *            Callback to be called when color updates are needed.
     * @param cmapParams
     *            The colormap paramters to use to initalize the bar
     * @param enableColorMask
     *            Use the color mask values when painting the color bar
     */
    public ColorBar(Composite parent, IColorBarAction callback,
            ColorMapParameters cmapParams, boolean enableColorMask) {
        super(parent, SWT.NONE);
        this.enabledColorMask = enableColorMask;
        this.cmapParams = cmapParams;

        for (Entry<String, String> entry : decimalPlaceMap.entrySet()) {
            if (parent.getShell().getText().toLowerCase()
                    .contains(entry.getKey())) {
                numberFormat = new DecimalFormat(
                        "###,###,##0." + entry.getValue());
                break;
            }
        }

        colorHistory = new ArrayList<>();
        colorHistory.add(ColorUtil.buildColorData(cmapParams.getColorMap()));

        topSliderIndex = 0;
        bottomSliderIndex = getColorCount() - 1;

        this.callBack = callback;

        GridData gd = new GridData(SWT.FILL, SWT.DEFAULT, true, false);
        GridLayout gl = new GridLayout(3, false);
        this.setLayout(gl);
        this.setLayoutData(gd);

        initComponents();
    }

    /** Initialize the components for the color bar. */
    private void initComponents() {
        // Create left buttons
        createNavButtons(true);

        createColorBarArea();

        // Create right buttons
        createNavButtons(false);

        initalizeColorbarImages();
    }

    /**
     * Create the slider navigation buttons on the given side of the color bar.
     *
     * @param left
     *            whether the buttons are on the left or right side
     */
    private void createNavButtons(boolean left) {
        // Create a composite to hold the navButtons
        Composite buttonsComp = new Composite(this, SWT.NONE);
        GridLayout gl = new GridLayout(1, true);
        buttonsComp.setLayout(gl);

        createNavButton(buttonsComp, true, left, false);
        createNavButton(buttonsComp, true, left, true);
        createNavButton(buttonsComp, false, left, true);
        createNavButton(buttonsComp, false, left, false);
    }

    /**
     * Create a single navigation button
     *
     * @param parent
     *            Button parent
     * @param top
     *            true for top slider, false for bottom slider
     * @param left
     *            true for left navigation, false for right navigation
     * @param skip
     *            true to skip to the next unique color, false to shift only one
     *            color even if it is identical.
     */
    protected void createNavButton(Composite parent, final boolean top,
            final boolean left, final boolean skip) {
        String label;
        if (left) {
            label = "<";
        } else {
            label = ">";
        }

        if (skip) {
            label = label + label;
        }

        Button button = new Button(parent, SWT.PUSH);
        button.setText(label);
        button.setLayoutData(new GridData(SWT.FILL, SWT.DEFAULT, false, false));
        button.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent event) {
                shiftSlider(top, left, skip);
            }
        });
        navButtons.add(button);
    }

    /**
     * Shift one of the sliders to a new index.
     *
     * @param top
     *            true for top slider, false for bottom slider
     * @param left
     *            true for left navigation, false for right navigation
     * @param skip
     *            true to skip to the next unique color, false to shift only one
     *            color even if it is identical.
     */
    protected void shiftSlider(boolean top, boolean left, boolean skip) {
        int startIndex;
        int direction;
        int endIndex;
        if (top) {
            startIndex = topSliderIndex;
        } else {
            startIndex = bottomSliderIndex;
        }
        if (left) {
            direction = -1;
            endIndex = -1;
        } else {
            direction = 1;
            endIndex = getColorCount();
        }
        RGB start = getCurrentColor(startIndex).rgbColor;
        for (int i = startIndex; i != endIndex; i += direction) {
            if (!getCurrentColor(i).rgbColor.equals(start)
                    || (!skip && i != startIndex)) {
                if (top) {
                    topSliderIndex = i;
                    bottomSliderIndex = Math.max(topSliderIndex,
                            bottomSliderIndex);
                } else {
                    bottomSliderIndex = i;
                    topSliderIndex = Math.min(topSliderIndex,
                            bottomSliderIndex);
                }
                repaint();
                break;
            }
        }
    }

    /**
     * Create the color bar canvas that will display the color bar and the top
     * and bottom sliders.
     */
    private void createColorBarArea() {
        /* Set the font used for the top and bottom sliders. */
        font = new Font(getDisplay(), "Courier", 10, SWT.NORMAL);

        /* Create the composite that will hold the color bar canvas. */
        Composite colorBarComp = new Composite(this, SWT.NONE);
        colorBarComp
                .setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
        colorBarComp.setLayout(new GridLayout(1, false));

        /*
         * Create a new color bar canvas and add listeners for the mouse, paint,
         * and resize events.
         */
        colorBarCanvas = new Canvas(colorBarComp, SWT.DOUBLE_BUFFERED);
        int width = 2 * (ARROW_SIZE.x + MARGIN_SIZE.x) + BAR_SIZE.x;
        int height = 2 * (ARROW_SIZE.y + MARGIN_SIZE.y) + BAR_SIZE.y;
        GridData gd = new GridData(width, height);
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalAlignment = SWT.FILL;
        colorBarCanvas.setLayoutData(gd);
        colorBarCanvas.addMouseListener(this);
        colorBarCanvas.addMouseMoveListener(this);
        colorBarCanvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent event) {
                paintCanvas(event.gc);
            }

        });
        colorBarCanvas.addListener(SWT.Resize, new Listener() {

            @Override
            public void handleEvent(Event event) {
                initalizeColorbarImages();
            }
        });
    }

    /** Get the area within the colorBarCanvas where the bar is painted. */
    protected Rectangle getBarBounds() {
        int totalMarginX = MARGIN_SIZE.x + ARROW_SIZE.x;
        int totalMarginY = MARGIN_SIZE.y + ARROW_SIZE.y;
        Point canvasSize = colorBarCanvas.getSize();
        return new Rectangle(totalMarginX, totalMarginY,
                canvasSize.x - 2 * totalMarginX,
                canvasSize.y - 2 * totalMarginY);
    }

    /**
     * Paint the colorbar, arrows, and decorations on the colorbar canvas.
     *
     * @param gc
     */
    public void paintCanvas(GC gc) {
        Point canvasSize = colorBarCanvas.getSize();
        Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
        gc.setBackground(black);
        gc.fillRectangle(0, 0, canvasSize.x, canvasSize.y);
        if (isEnabled()) {
            Rectangle bar = getBarBounds();

            /* Draw the white color bar outline rectangle. */
            Color white = getDisplay().getSystemColor(SWT.COLOR_WHITE);
            gc.setForeground(white);
            gc.drawRectangle(bar);

            /* Draw colorbar image */
            synchronized (this) {
                gc.drawImage(colorBar, bar.x + 1, bar.y + 1);
                if (enabledColorMask && cmapParams.isUseMask()) {
                    gc.drawImage(colorBarWithMask, bar.x + 1, bar.y + 1);
                }
            }

            /*
             * Draw a black line with a white dotted line on top down the middle
             * of the color bar.
             */
            int midY = bar.y + bar.height / 2;
            gc.setForeground(black);
            gc.drawLine(bar.x, midY, bar.x + bar.width, midY);
            gc.setForeground(white);
            gc.setLineStyle(SWT.LINE_DOT);
            gc.drawLine(bar.x, midY, bar.x + bar.width, midY);

            /*
             * Draw top slider arrow. The arrow will be filled with the color it
             * is pointing to on the color bar.
             */
            gc.setLineStyle(SWT.LINE_SOLID);
            ColorData colorData = getTopColor();
            Color currentColor = new Color(getDisplay(), colorData.rgbColor);
            gc.setBackground(currentColor);
            gc.setAlpha(colorData.alphaValue);
            int topSliderX = reIndex(getColorCount(), bar.width,
                    topSliderIndex);
            /*
             * reIndex positions it at the middle, additional math is needed to
             * shift it to the left edge of the color
             */
            topSliderX -= bar.width / getColorCount() / 2;
            int[] topArrowPolygon = calcTopArrow(topSliderX);
            gc.fillPolygon(topArrowPolygon);
            gc.setAlpha(255);
            gc.drawPolygon(topArrowPolygon);
            currentColor.dispose();

            /* Draw text that is displayed next to the top slider arrow. */
            gc.setForeground(white);
            gc.setFont(font);
            String text = getSliderText(topSliderIndex);
            int x = topSliderX + MARGIN_SIZE.x + ARROW_SIZE.x;
            if (topSliderIndex < getColorCount() / 2) {
                x += ARROW_SIZE.x + 5;
            } else {
                Point p = gc.textExtent(text);
                x -= ARROW_SIZE.x + 5 + p.x;
            }
            gc.drawString(text, x, MARGIN_SIZE.y, true);

            /*
             * Draw bottom slider arrow. The arrow will be filled with the color
             * it is pointing to on the color bar.
             */
            colorData = getBottomColor();
            currentColor = new Color(getDisplay(), colorData.rgbColor);
            gc.setBackground(currentColor);
            gc.setAlpha(colorData.alphaValue);
            int bottomSliderX = reIndex(getColorCount(), bar.width,
                    bottomSliderIndex);
            /*
             * reIndex positions it at the middle, additional math is needed to
             * shift it to the right edge of the color
             */
            bottomSliderX += bar.width / getColorCount() / 2;
            int[] bottomArrowPolygon = calcBottomArrow(bottomSliderX);
            gc.fillPolygon(bottomArrowPolygon);
            gc.setAlpha(255);
            gc.drawPolygon(bottomArrowPolygon);
            currentColor.dispose();

            /* Draw text that is displayed next to the bottom slider arrow. */
            gc.setForeground(white);
            text = getSliderText(bottomSliderIndex);
            x = bottomSliderX + MARGIN_SIZE.x + ARROW_SIZE.x;
            if (bottomSliderIndex < getColorCount() / 2) {
                x += ARROW_SIZE.x + 5;
            } else {
                Point p = gc.textExtent(text);
                x -= ARROW_SIZE.x + 5 + p.x;
            }
            gc.drawString(text, x, canvasSize.y - MARGIN_SIZE.y - 15, true);
        }
    }

    /**
     * Calculate the points of the top slider arrow.
     *
     * @param xCoord
     *            X coordinate to draw the top slider arrow.
     * @return Array of points outlining the top slider arrow.
     */
    private int[] calcTopArrow(int xCoord) {
        int left = xCoord + MARGIN_SIZE.x;
        int midX = left + ARROW_SIZE.x / 2;
        int right = left + ARROW_SIZE.x;

        int top = MARGIN_SIZE.y;
        int midY = top + ARROW_SIZE.y / 2;
        int bottom = top + ARROW_SIZE.y;

        return new int[] { right, bottom - 2, right, top, midX, top, midX, midY,
                left, midY };
    }

    /**
     * Calculate the points of the bottom slider arrow.
     *
     * @param xCoord
     *            X coordinate to draw the bottom slider arrow.
     * @return Array of points outlining the bottom slider arrow.
     */
    private int[] calcBottomArrow(int xCoord) {
        int left = xCoord + MARGIN_SIZE.x + ARROW_SIZE.x;
        int midX = left + ARROW_SIZE.x / 2;
        int right = left + ARROW_SIZE.x;

        Rectangle bar = getBarBounds();
        int top = bar.y + bar.height + 2;
        int midY = top + ARROW_SIZE.y / 2;
        int bottom = top + ARROW_SIZE.y;

        return new int[] { left, top + 2, left, bottom, midX, bottom, midX,
                midY, right, midY };
    }

    /**
     * @return The currently displayed list of colors.
     */
    public List<ColorData> getCurrentColors() {
        return colorHistory.get(currentColorIndex);
    }

    /**
     * @return A copy of the currently displayed list of colors(for
     *         modification).
     */
    protected List<ColorData> getCurrentColorsCopy() {
        return new ArrayList<>(getCurrentColors());
    }

    /**
     * Set the current colors as the last item on the color history.
     *
     * @param colors
     */
    protected void setCurrentColors(List<ColorData> colors) {
        if (!colors.equals(getCurrentColors())) {
            int oldCount = getColorCount();
            colorHistory.subList(currentColorIndex + 1, colorHistory.size())
                    .clear();
            colorHistory.add(colors);
            currentColorIndex += 1;
            int newCount = getColorCount();
            topSliderIndex = reIndex(oldCount, newCount, topSliderIndex);
            bottomSliderIndex = reIndex(oldCount, newCount, bottomSliderIndex);
            repaintColorbars();
        }
    }

    /**
     * @param index
     * @return the current {@link ColorData} at the specified index of the
     *         current color list.
     */
    protected ColorData getCurrentColor(int index) {
        return getCurrentColors().get(index);
    }

    /**
     * @return The colorData the top arrow is pointing to.
     */
    protected ColorData getTopColor() {
        return getCurrentColors().get(topSliderIndex);
    }

    /**
     * @return The colorData the bottom arrow is pointing to.
     */
    protected ColorData getBottomColor() {
        return getCurrentColors().get(bottomSliderIndex);
    }

    public void updateColorMap(ColorMapParameters newParams) {
        cmapParams = newParams;
        setCurrentColors(ColorUtil.buildColorData(cmapParams.getColorMap()));
    }

    /**
     * Method called when the mouse is moved.
     *
     * @param e
     *            Mouse event.
     */
    @Override
    public void mouseMove(MouseEvent e) {
        if (moveTopSlider || moveBottomSlider || queryTopColorBar
                || queryBottomColorBar) {
            Rectangle bar = getBarBounds();
            int index = reIndex(bar.width, getColorCount(), e.x - bar.x);
            if (moveTopSlider) {
                topSliderIndex = index;
                bottomSliderIndex = Math.max(topSliderIndex, bottomSliderIndex);
                repaint();
            } else if (moveBottomSlider) {
                bottomSliderIndex = index;
                topSliderIndex = Math.min(topSliderIndex, bottomSliderIndex);
                repaint();

            } else {
                callBack.updateColor(getCurrentColor(index), queryTopColorBar);

            }
        }
    }

    @Override
    public void mouseDoubleClick(MouseEvent e) {
        /* NOT USED */
    }

    @Override
    public void mouseDown(MouseEvent e) {
        Rectangle bar = getBarBounds();

        if (e.button == 1) {
            /* MB1 was pressed */

            /* Determine action to take based off location. */
            if (e.y < bar.y) {
                moveTopSlider = true;
            } else if (e.y > bar.y + bar.height) {
                moveBottomSlider = true;
            } else if (bar.contains(e.x, e.y)) {
                if (e.y < bar.y + bar.height / 2) {
                    queryTopColorBar = true;
                } else {
                    queryBottomColorBar = true;

                }
            }

            /* Use mouse move to perform action, avoid duplicate code. */
            mouseMove(e);
        } else if (e.button == 2) {
            /* MB2 was pressed */
            if (bar.contains(e.x, e.y)) {
                mouseDragStart = new Point(e.x, e.y);
            }
        }
    }

    @Override
    public void mouseUp(MouseEvent e) {
        if (e.button == 1) {
            /* Reset any action flags that might have been set on mouseDown */
            moveBottomSlider = false;
            moveTopSlider = false;
            queryTopColorBar = false;
            queryBottomColorBar = false;

        } else if (e.button == 2) {
            Rectangle bar = getBarBounds();

            int dragStartIndex = reIndex(bar.width, getColorCount(),
                    mouseDragStart.x - bar.x);
            int dragEndIndex = reIndex(bar.width, getColorCount(), e.x - bar.x);

            List<ColorData> currentColors = getCurrentColors();
            List<ColorData> newColors = contrastStretch(dragStartIndex,
                    dragEndIndex, topSliderIndex, bottomSliderIndex,
                    currentColors);

            if (newColors != null) {
                setCurrentColors(newColors);
                callBack.updateColorMap();
            }
        }
    }

    /**
     * Does a contrast stretch operation between startIndex and endIndex. You
     * end up with colors sampled on one side of endDrag and filled in by
     * interpolation on the other side of endDrag. Will not change colors
     * <=startIndex or >=endIndex. Color at beginDrag ends up being copied to
     * endDrag. Values of beginDrag or endDrag outside the range of startIndex
     * to endIndex will be truncated to lie within that range before performing
     * the operation.
     *
     * @param beginDrag
     *            Initial pivot point for the contrast stretch operation.
     * @param endDrag
     *            Final pivot point for the contrast stretch operation.
     * @param startIndex
     *            The index of the first color that will be involved in the
     *            contrast stretch.
     * @param endIndex
     *            The index of the last color that will be involved in the
     *            contrast stretch.
     * @param currentColors
     *            the current colors
     * @return the new colors or null if problem or no change
     */
    private List<ColorData> contrastStretch(int beginDrag, int endDrag,
            int startIndex, int endIndex, List<ColorData> currentColors) {
        // Do integrity checking on the range limits.
        if (endIndex < startIndex) {
            // logBug << "Start index for EditableColorTable::contrastStretch
            // has "
            // << "to be greater than the end index." << std::endl;
            return null;
        }

        // Get the current colors
        int cmapSize = currentColors.size();

        // Make sure the range is between 1 and cmapSize-2. We do not want to
        // set the first or last entry in the color map.
        if (startIndex < 1) {
            startIndex = 1;
        }

        if (endIndex >= cmapSize - 2) {
            endIndex = cmapSize - 2;
        }

        // Clip beginDrag and endDrag to be within range startIndex to
        // endIndex
        if (beginDrag < startIndex) {
            beginDrag = startIndex;
        } else if (beginDrag > endIndex) {
            beginDrag = endIndex;
        }
        if (endDrag < startIndex) {
            endDrag = startIndex;
        } else if (endDrag > endIndex) {
            endDrag = endIndex;
        }
        if (beginDrag == endDrag) {
            return null;
        }

        // Copy current color table to a work copy
        List<ColorData> newColors = new ArrayList<>(currentColors);

        // Initialize loop for the sample side.
        int lastFrom, lastTo;
        int from, to;
        if (beginDrag < endDrag) {
            from = beginDrag;
            to = endDrag;
            lastFrom = endIndex;
            lastTo = endIndex;
        } else {
            from = startIndex;
            to = startIndex;
            lastFrom = beginDrag;
            lastTo = endDrag;
        }
        int nFrom = lastFrom - from;
        int nTo = lastTo - to;

        // Do the sampling.
        int f = nTo / 2;
        int t;
        if (nTo > 0) {
            for (t = to; t <= lastTo; t++) {
                newColors.set(t, currentColors.get(from + f / nTo));
                f += nFrom;
            }
        }

        // Initialize loop for the interpolate side.
        if (beginDrag > endDrag) {
            from = beginDrag;
            to = endDrag;
            lastFrom = endIndex;
            lastTo = endIndex;
        } else {
            from = startIndex;
            to = startIndex;
            lastFrom = beginDrag;
            lastTo = endDrag;
        }
        nFrom = lastFrom - from;
        nTo = lastTo - to;

        // Special case of no from range
        if (nFrom <= 0) {
            for (t = to; t <= lastTo; t++) {
                newColors.set(t, currentColors.get(from));
            }
        } else {

            // Do the interpolation.
            int prevTo = to;
            int newTo;
            t = nFrom / 2;
            for (f = from; f < lastFrom; f++) {
                t += nTo;
                newTo = to + t / nFrom;
                double nNew = newTo - prevTo;
                if (nNew <= 0) {
                    continue;
                }
                newColors.set(prevTo, currentColors.get(f));
                prevTo++;

                if (nNew == 1) {
                    continue;
                }

                int alpha = currentColors.get(f).alphaValue;
                double red = currentColors.get(f).rgbColor.red;
                double green = currentColors.get(f).rgbColor.green;
                double blue = currentColors.get(f).rgbColor.blue;
                double deltaRed = (currentColors.get(f + 1).rgbColor.red - red)
                        / nNew;
                double deltaGreen = (currentColors.get(f + 1).rgbColor.green
                        - green) / nNew;
                double deltaBlue = (currentColors.get(f + 1).rgbColor.blue
                        - blue) / nNew;
                for (; prevTo < newTo; prevTo++) {
                    red += deltaRed;
                    green += deltaGreen;
                    blue += deltaBlue;
                    newColors.set(prevTo,
                            new ColorData(
                                    new RGB((int) red, (int) green, (int) blue),
                                    alpha));
                }
            }
        }

        return newColors;
    }

    /**
     * Set the current cell (top or bottom depending on the flag passed in) to
     * the specified color in the color data object.
     *
     * @param colorData
     *            Color data object containing the RGB and alpha values.
     * @param upperFlag
     *            If true the cell where the top slider is located will be
     *            updated, if false the cell where the bottom slider is located
     *            will be updated.
     */
    public void setColorBarColor(ColorData colorData, boolean upperFlag) {
        List<ColorData> newColors = getCurrentColorsCopy();
        if (upperFlag) {
            newColors.set(topSliderIndex, colorData);
        } else {
            newColors.set(bottomSliderIndex, colorData);
        }
        setCurrentColors(newColors);
    }

    /**
     * Fill the area of the color bar with a solid color from the color wheel.
     * The area between the top and bottom sliders will be fill with the
     * specified color.
     *
     * @param colorData
     *            The color to fill the color bar area with.
     */
    public void fillColorBarColor(ColorData colorData) {
        List<ColorData> newColors = getCurrentColorsCopy();

        /*
         * Fill the cells between the top slider and the bottom slider with the
         * fill color.
         */
        for (int i = topSliderIndex; i <= bottomSliderIndex; ++i) {
            newColors.set(i, colorData);
        }

        setCurrentColors(newColors);
    }

    /**
     * Undo the last change to the color bar.
     *
     * @return True if there are more "undos" that can take place, otherwise
     *         return false.
     */
    public boolean undoColorBar() {
        if (currentColorIndex <= 0) {
            return false;
        } else {
            int oldCount = getColorCount();
            currentColorIndex -= 1;
            int newCount = getColorCount();
            topSliderIndex = reIndex(oldCount, newCount, topSliderIndex);
            bottomSliderIndex = reIndex(oldCount, newCount, bottomSliderIndex);
            repaintColorbars();
            return canUndo();
        }
    }

    /**
     * Redo the last change to the color bar.
     *
     * @return True if there are more "redos" that can take place, otherwise
     *         return false.
     */
    public boolean redoColorBar() {
        if (currentColorIndex >= colorHistory.size()) {
            return false;
        } else {
            int oldCount = getColorCount();
            currentColorIndex += 1;
            int newCount = getColorCount();
            topSliderIndex = reIndex(oldCount, newCount, topSliderIndex);
            bottomSliderIndex = reIndex(oldCount, newCount, bottomSliderIndex);
            repaintColorbars();
            return canRedo();
        }
    }

    /**
     * Reverting will clear the undo/redo array and display the original colors
     * in the color bar.
     */
    public void revertColorBar() {
        int oldCount = getColorCount();
        currentColorIndex = 0;
        colorHistory.subList(1, colorHistory.size()).clear();
        int newCount = getColorCount();
        topSliderIndex = reIndex(oldCount, newCount, topSliderIndex);
        bottomSliderIndex = reIndex(oldCount, newCount, bottomSliderIndex);
        repaintColorbars();
    }

    /**
     * Interpolate between the start and end colors. If the colors are HSB: ---
     * Fills the specified range with an interpolation or blending of the lower
     * color with the upper color. This interpolation along the hue, saturation,
     * brightness color model is linear.
     *
     * If the colors are RGB: --- Fills the specified range with an
     * interpolation or blending of the lower color with the upper color. This
     * interpolation along the red, green, blue color model is linear.
     *
     * The alpha value is also interpolated.
     *
     * @param startColorData
     *            Starting color.
     * @param endColorData
     *            Ending color.
     * @param isRGB
     *            If true the colors are interpolated as RGB, if false the
     *            colors are interpolated as HSB.
     */
    public void interpolate(ColorData startColorData, ColorData endColorData,
            boolean isRGB) {
        float numOfCells = bottomSliderIndex - topSliderIndex;
        // Create a new color array using the current set of colors.
        List<ColorData> newColors = getCurrentColorsCopy();
        if (topSliderIndex == bottomSliderIndex) {
            newColors.set(topSliderIndex, startColorData);
        } else if (isRGB) {
            RGB startRGB = startColorData.rgbColor;
            RGB endRGB = endColorData.rgbColor;
            /*
             * Get the delta between the starting and ending colors for red,
             * green, blue, as well as the alpha.
             */
            float deltaRed = (endRGB.red - startRGB.red) / numOfCells;
            float deltaGreen = (endRGB.green - startRGB.green) / numOfCells;
            float deltaBlue = (endRGB.blue - startRGB.blue) / numOfCells;
            float deltaAlpha = (endColorData.alphaValue
                    - startColorData.alphaValue) / numOfCells;

            /*
             * Loop through all of the cells and fill them with the interpolated
             * colors.
             */
            for (int i = 0; i < numOfCells; i += 1) {
                float newRed = (deltaRed * i) + startRGB.red;
                float newGreen = (deltaGreen * i) + startRGB.green;
                float newBlue = (deltaBlue * i) + startRGB.blue;
                float newAlpha = (deltaAlpha * i) + startColorData.alphaValue;

                RGB tmpRGB = new RGB(checkRgbColor(newRed),
                        checkRgbColor(newGreen), checkRgbColor(newBlue));
                ColorData colorData = new ColorData(tmpRGB,
                        checkRgbColor(newAlpha));

                newColors.set(topSliderIndex + i, colorData);

            }
            newColors.set(bottomSliderIndex, endColorData);

        } else {
            // Get the starting and ending HSB colors.
            float[] startHSB = startColorData.rgbColor.getHSB();
            float[] endHSB = endColorData.rgbColor.getHSB();

            // Get the deltas for the hue, saturation, brightness, and alpha.
            float deltaHue = getMinHueDiff(startHSB[0], endHSB[0]) / numOfCells;
            float deltaSaturation = (endHSB[1] - startHSB[1]) / numOfCells;
            float deltaBrightness = (endHSB[2] - startHSB[2]) / numOfCells;

            float deltaAlpha = (endColorData.alphaValue
                    - startColorData.alphaValue) / numOfCells;
            /*
             * Loop through all of the cells and fill them with the interpolated
             * colors.
             */
            for (int i = 0; i < numOfCells; i += 1) {
                float newHue = (deltaHue * i) + startHSB[0];
                float newSaturation = (deltaSaturation * i) + startHSB[1];
                float newBrightness = (deltaBrightness * i) + startHSB[2];
                int newAlpha = Math
                        .round((deltaAlpha * i) + startColorData.alphaValue);

                RGB tmpRGB = new RGB(checkHue(newHue), newSaturation,
                        newBrightness);
                ColorData colorData = new ColorData(tmpRGB, newAlpha);

                newColors.set(topSliderIndex + i, colorData);
            }
            newColors.set(bottomSliderIndex, endColorData);
        }

        setCurrentColors(newColors);
    }

    /**
     * Interpolate between the start and end alpha values only.
     *
     * @param startColorData
     *            Starting color.
     * @param endColorData
     *            Ending color.
     */
    public void interpolateAlphaOnly(ColorData startColorData,
            ColorData endColorData) {
        float numOfCells = bottomSliderIndex - topSliderIndex;
        // Create a new color array using the current set of colors.
        List<ColorData> newColors = getCurrentColorsCopy();
        if (topSliderIndex == bottomSliderIndex) {
            newColors.set(topSliderIndex, startColorData);
        } else {
            float deltaAlpha = (endColorData.alphaValue
                    - startColorData.alphaValue) / numOfCells;

            /*
             * Loop through all of the cells and fill them with the interpolated
             * colors.
             */
            for (int i = 0; i < numOfCells; i += 1) {
                float newAlpha = (deltaAlpha * i) + startColorData.alphaValue;
                ColorData colorData = new ColorData(newColors.get(i).rgbColor,
                        checkRgbColor(newAlpha));
                newColors.set(topSliderIndex + i, colorData);
            }
            newColors.set(bottomSliderIndex, endColorData);
        }

        setCurrentColors(newColors);
    }

    /**
     * Clear the color history so that a revert operation in the future will
     * revert to the current colors.
     */
    public void updateRevertToCurrent() {
        colorHistory.subList(0, colorHistory.size() - 1).clear();
        currentColorIndex = 0;
    }

    /**
     * Get the text to display for a slider arrow at the specified index.
     *
     * @param index
     * @return
     */
    protected String getSliderText(double index) {
        int size = getColorCount();
        UnitConverter unitConv = cmapParams.getColorMapToDisplayConverter();

        float max = cmapParams.getColorMapMax();
        float min = cmapParams.getColorMapMin();
        double lastVal = Double.NaN;
        double value = Double.NaN;
        if (cmapParams.isLogarithmic()) {
            double idx = index / (size - 1);
            value = Colormapper.getLogValue(idx, min, max,
                    cmapParams.isMirror());
            if (index > 0) {
                idx = (index - 1) / (size - 1);
                lastVal = Colormapper.getLogValue(idx, min, max,
                        cmapParams.isMirror());
            }
        } else if (cmapParams.getLogFactor() > 0.0) {
            double idx = index / size;
            value = Colormapper.getLogFactorValue(idx, min, max,
                    cmapParams.getLogFactor());
            if (index > 0) {
                idx = (index - 1) / size;
                lastVal = Colormapper.getLogFactorValue(idx, min, max,
                        cmapParams.getLogFactor());
            }
        } else {
            // linear color mapping
            double idx = index / size;
            value = Colormapper.getLinearValue(idx, min, max);
            if (index > 0) {
                idx = (index - 1) / size;
                lastVal = Colormapper.getLinearValue(idx, min, max);
            }
        }

        if (unitConv != null) {
            value = unitConv.convert(value);
            lastVal = unitConv.convert(lastVal);

            /* Check if the last value is non a number. */
            if (Double.isNaN(value)) {
                if (Double.isNaN(lastVal)) {
                    return "NO DATA";
                } else if (((Double) value).isNaN()) {
                    if (numberFormat != null) {
                        return "> " + numberFormat.format(lastVal);
                    } else {
                        return "> " + lastVal;
                    }
                }
            }

        }
        if (cmapParams.getDataMapping() != null) {
            String dmLabel = cmapParams.getDataMapping()
                    .getLabelValueForDataValue(value);
            if (dmLabel != null && !"".equals(dmLabel.trim())) {
                return dmLabel;
            }
        }
        NumberFormat format = numberFormat != null ? numberFormat
                : new DecimalFormat("0.0##");
        return format.format(value);
    }

    public Point getSliderRange() {
        return new Point(topSliderIndex, bottomSliderIndex);
    }

    /** Repaint the colobar's canvas because if alpha mask state has changed */
    public void repaint() {
        colorBarCanvas.redraw();
    }

    /** Paint the color bar images */
    public void repaintColorbars() {
        // Don't do this if I am disposed;
        if (isDisposed()) {
            return;
        }
        // initialize the color bars
        initalizeColorbarImages();

        // repaint the canvas
        repaint();
    }

    private void initalizeColorbarImages() {
        Rectangle bar = getBarBounds();
        if (bar.isEmpty()) {
            return;
        }
        List<ColorData> currentColors = getCurrentColors();
        Image colorBar = paintColorBar(currentColors, false);
        Image colorBarWithMask = null;
        if (enabledColorMask) {
            colorBarWithMask = paintColorBar(currentColors, true);
        }
        synchronized (this) {
            if (this.colorBar != null) {
                this.colorBar.dispose();
            }
            this.colorBar = colorBar;

            if (colorBarWithMask != null) {
                if (this.colorBarWithMask != null) {
                    this.colorBarWithMask.dispose();
                }
                this.colorBarWithMask = colorBarWithMask;
            }
        }
    }

    private Image paintColorBar(List<ColorData> colors, boolean useMask) {
        Rectangle bar = getBarBounds();
        int colorCount = getColorCount();
        Color black = getDisplay().getSystemColor(SWT.COLOR_BLACK);
        Color white = getDisplay().getSystemColor(SWT.COLOR_WHITE);

        Image colorBar = new Image(getDisplay(), bar.width - 1, bar.height - 1);
        GC gc = new GC(colorBar);

        gc.setBackground(black);
        gc.fillRectangle(0, 0, bar.width - 1, bar.height - 1);
        /*
         * Draw 2 filled rectangles. One for the top and one for the bottom of
         * the color bar. These rectangles will show up when the alpha channel
         * is set.
         */
        gc.setBackground(white);
        gc.fillRectangle(0, 10, bar.width, Math.round(bar.height / 6));
        gc.fillRectangle(0, (bar.height / 2) + 10, bar.width,
                Math.round(bar.height / 6));

        byte[] mask = cmapParams.getAlphaMask();

        for (int x = 0; x < bar.width - 1; ++x) {
            int idx = reIndex(bar.width, colorCount, x);

            if (useMask && mask[idx] != 0) {
                gc.setAlpha(255);
                gc.setForeground(black);
                gc.drawLine(x, 0, x, bar.height);
            } else {
                ColorData colorData = colors.get(idx);
                Color color = new Color(getDisplay(), colorData.rgbColor);
                gc.setAlpha(colorData.alphaValue);
                gc.setForeground(color);
                gc.drawLine(x, 0, x, bar.height);
                color.dispose();
            }
        }
        gc.dispose();
        return colorBar;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            revertColorBar();
        }
        for (Button button : navButtons) {
            button.setEnabled(enabled);
        }
        colorBarCanvas.setEnabled(enabled);
        colorBarCanvas.redraw();
    }

    public boolean canUndo() {
        return currentColorIndex > 0;
    }

    public boolean canRedo() {
        return currentColorIndex < (colorHistory.size() - 1);
    }

    /**
     * Adjust the number of colors in the current color list. If more colors
     * than the current list then interpolated colors will be added.
     *
     * @param newCount
     */
    public void setColorCount(int newCount) {
        List<ColorData> currentColors = getCurrentColors();
        int oldCount = currentColors.size();
        if (newCount == oldCount) {
            return;
        }

        List<ColorData> newColors = null;
        if (newCount > oldCount) {
            List<ColorData> tempColors = new ArrayList<>(newCount);
            tempColors.addAll(currentColors);
            ColorData lastColor = currentColors.get(oldCount - 1);
            for (int i = oldCount; i < newCount; i++) {
                tempColors.add(lastColor);
            }
            newColors = contrastStretch(oldCount - 1, newCount - 1, 0,
                    newCount - 1, tempColors);
        } else {
            List<ColorData> tempColors = contrastStretch(oldCount - 1,
                    newCount - 1, 0, oldCount - 1, currentColors);
            if (tempColors != null) {
                newColors = new ArrayList<>(tempColors.subList(0, newCount));
            }
        }

        if (newColors != null) {
            setCurrentColors(newColors);
            callBack.updateColorMap();
        }
    }

    public int getColorCount() {
        return getCurrentColors().size();
    }

    @Override
    public void dispose() {
        super.dispose();
        font.dispose();
    }

    /**
     * Get the minimum difference between hue values. The hue values range from
     * 0 to 359.99 so we need to figure out the shortest "distance" between the
     * two colors.
     *
     * Example: If the first color is 0 and the second is 170, the colors will
     * go "clockwise" from 0 to 170. If the second color is 190 the the colors
     * will go from 0 to 190 "counterclockwise" (0, 359, 358, ..., 191, 190)
     *
     *
     * @param startHue
     *            Starting hue value.
     * @param endHue
     *            Ending hue value.
     * @return The minimum difference between the hue values.
     */
    private static float getMinHueDiff(float startHue, float endHue) {
        float diff = endHue - startHue;

        if (Math.abs(diff) > 180) {
            diff = (diff > 0) ? (diff - 360) : (diff + 360);
        }

        return diff;
    }

    /**
     * Valid that the hue value is within the 0 to 359.99 range.
     *
     * @param hue
     *            Hue value.
     * @return Either the original hue value or the corrected hue value.
     */
    private static float checkHue(float hue) {
        if (hue > 360f) {
            hue = hue - 360f;
        } else if (hue < 0f) {
            hue = hue + 360f;
        }

        return hue;
    }

    /**
     * Validate that the R/G/B color is within the 0 to 255 range.
     *
     * @param color
     *            R/G/B color to validate.
     * @return Existing R/G/B color value or the adjusted color value.
     */
    private static int checkRgbColor(float color) {
        int icolol = Math.round(color);
        if (icolol > 255) {
            return 255;
        } else if (icolol < 0) {
            return 0;
        }

        return icolol;
    }

    private static int reIndex(int currentWidth, int newWidth,
            int currentIndex) {
        if (newWidth == currentWidth) {
            return currentIndex;
        }
        float pct = (currentIndex + 0.5f) / currentWidth;
        int index = (int) (newWidth * pct);
        index = Math.max(index, 0);
        index = Math.min(newWidth - 1, index);
        return index;
    }

    public History getHistory() {
        return new History(currentColorIndex, colorHistory);
    }

    public void setHistory(History h) {
        this.currentColorIndex = h.currentColorIndex;
        this.colorHistory = h.colorHistory;
        repaintColorbars();
    }

}
