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
package com.raytheon.viz.ui.widgets.duallist;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

/**
 * Images for the Add All, Add, Remove, Remove All, Move Up, and Move Down
 * buttons. Reused from Data Delivery
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * May 31, 2012           mpduff    Initial creation
 * Sep 22, 2020  7926     randerso  Fixed image transparency. Significant code
 *                                  cleanup
 *
 * </pre>
 *
 * @author mpduff
 */

public class ButtonImages {

    /*
     * Image size constants
     */
    private static final int imageWidthWide = 24;

    private static final int imageWidthNarrow = 12;

    private static final int imageHeight = 12;

    /**
     * Parent composite.
     */
    private Composite parent;

    /**
     * Array of button images.
     */
    private Image[] buttonImages;

    /**
     * ButtonImage enumeration that identifies which image to use.
     */
    public enum ButtonImage {
        AddAll(
                new int[][] { { 10, 6, 1, 1, 1, 10, 10, 6 },
                        { 22, 6, 13, 1, 13, 10, 22, 6 } },
                imageWidthWide),
        Add(new int[][] { { 17, 6, 8, 1, 8, 10, 17, 6 } }, imageWidthWide),
        Remove(new int[][] { { 8, 6, 17, 1, 17, 10, 8, 6 } }, imageWidthWide),
        RemoveAll(
                new int[][] { { 1, 6, 10, 1, 10, 10, 1, 6 },
                        { 13, 6, 22, 1, 22, 10, 13, 6 } },
                imageWidthWide),
        Up(new int[][] { { 1, 10, 6, 1, 11, 10, 1, 10 } }, imageWidthNarrow),
        Down(new int[][] { { 1, 1, 6, 10, 11, 1, 1, 1 } }, imageWidthNarrow);

        private final int[][] polygons;

        private final int width;

        private ButtonImage(int[][] polygons, int width) {
            this.polygons = polygons;
            this.width = width;
        }

        public int[][] getPolygons() {
            return polygons;
        }

        public int getWidth() {
            return width;
        }
    }

    /**
     * Constructor.
     *
     * @param parent
     *            Parent composite.
     */
    public ButtonImages(Composite parent) {
        this.parent = parent;

        // add listener to dispose images when parent is disposed
        parent.addDisposeListener(new DisposeListener() {
            @Override
            public void widgetDisposed(DisposeEvent e) {
                for (Image img : buttonImages) {
                    img.dispose();
                }
            }
        });

        // create the button images
        buttonImages = new Image[ButtonImage.values().length];
        for (ButtonImage buttonType : ButtonImage.values()) {
            buttonImages[buttonType.ordinal()] = createImage(buttonType);
        }
    }

    private Image createImage(ButtonImage buttonType) {
        Display display = parent.getDisplay();
        Image image = new Image(display, buttonType.getWidth(), imageHeight);
        Color black = display.getSystemColor(SWT.COLOR_BLACK);
        Color white = display.getSystemColor(SWT.COLOR_WHITE);

        GC gc = new GC(image);

        // fill the image with white
        gc.setBackground(white);
        gc.fillRectangle(0, 0, buttonType.getWidth(), imageHeight);

        gc.setAntialias(SWT.ON);

        // draw the polygons
        gc.setBackground(black);
        for (int[] polygon : buttonType.getPolygons()) {
            gc.fillPolygon(polygon);
        }

        gc.dispose();

        // convert white to transparent
        ImageData imageData = image.getImageData();
        imageData.transparentPixel = imageData.palette.getPixel(white.getRGB());
        Image transparentImage = new Image(display, imageData);
        image.dispose();

        return transparentImage;
    }

    /**
     * Get the image associated with the specified button type.
     *
     * @param buttonType
     *            Button image identifier.
     * @return The associated button image.
     */
    public Image getImage(ButtonImage buttonType) {
        return this.buttonImages[buttonType.ordinal()];
    }
}
