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
package com.raytheon.uf.viz.core.printing;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.PrintDialog;
import org.eclipse.swt.printing.Printer;
import org.eclipse.swt.printing.PrinterData;
import org.eclipse.swt.widgets.Display;

/**
 * Class encapsulating a printing job
 *
 * Sample usage:
 *
 * <pre>
 * <code>
        PrintDialog printDlg = new PrintDialog(shell);
        PrinterData printerData = printDlg.open();
        if (printerData == null) {
            return null;
        }

        AbstractEditor editor = (AbstractEditor) EditorUtil.getActiveEditor();
        BufferedImage bufferedImage = editor.screenshot();
        Display display = editor.getActiveDisplayPane().getDisplay();

        try (PrintJob printJob = new PrintJob(printerData)) {
            printJob.printImage(display, bufferedImage, true, true);
        } catch (PrintingException e) {
            statusHandler.error(e.getLocalizedMessage(), e);
        }
 * </code>
 * </pre>
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- -----------------
 * Apr 10, 2020  8120     randerso  Initial creation
 *
 * </pre>
 *
 * @author randerso
 */
public class PrintJob implements AutoCloseable {
    public static class PrintingException extends Exception {
        private static final long serialVersionUID = 458074223343942350L;

        public PrintingException(String message) {
            super(message);
        }
    }

    private Printer printer;

    private List<Image> images;

    /**
     * Creates a new print job using the printer specified by printerData. <br>
     * NOTE: the {@link#close()} method must be called on the PrintJob object to
     * release any resources created during the job.
     *
     * @param printerData
     *            a {@link PrinterData} object returned from {@link PrintDialog}
     *            specifying the printer and options to be used.
     * @throws PrintingException
     *             if the printer job could not be started
     */
    public PrintJob(PrinterData printerData) throws PrintingException {
        printer = new Printer(printerData);
        if (!printer.startJob("CAVE")) {
            printer.dispose();
            throw new PrintingException("Unable to start print job");
        }
        images = new ArrayList<>();
    }

    private Image addImage(ImageData imageData) {
        Image image = new Image(printer, imageData);
        images.add(image);

        return image;
    }

    @Override
    public void close() {
        if (printer != null && !printer.isDisposed()) {
            printer.endJob();
            printer.dispose();
        }

        /*
         * There is currently a bug in SWT/Cairo/GTK that causes the system to
         * crash if the images are disposed before the printer so it is
         * necessary to keep all the images until after the printer is disposed.
         */
        for (Image image : images) {
            image.dispose();
        }
    }

    /**
     * Print buffered image as returned by AbstractEditor.screenshot()
     *
     * @param display
     *            the SWT Display on which the image to printed is displayed.
     * @param bufferedImage
     *            the bufferedImage to be printed.
     * @param invertBlackWhite
     *            true if grayscale colors should be inverted. Useful for
     *            printing images with black backgrounds.
     * @param fitToPage
     *            true if image should be resized to fit a single page, if false
     *            the image may print over several pages if necessary
     */
    public void printImage(Display display, BufferedImage bufferedImage,
            boolean invertBlackWhite, boolean fitToPage) {

        if (invertBlackWhite) {
            /*
             * Only invert gray pixels, not colored pixels, awt doesn't not have
             * a good filter for this.
             */
            for (int x = 0; x < bufferedImage.getWidth(); x += 1) {
                for (int y = 0; y < bufferedImage.getHeight(); y += 1) {
                    Color color = new Color(bufferedImage.getRGB(x, y));
                    if (color.getRed() == color.getBlue()
                            && color.getBlue() == color.getGreen()) {
                        color = new Color(255 - color.getRed(),
                                255 - color.getGreen(), 255 - color.getBlue());
                        bufferedImage.setRGB(x, y, color.getRGB());
                    }

                }
            }
        }
        ImageData imageData = convertToSWT(bufferedImage);

        Rectangle printArea = printer.getClientArea();
        Point screenDPI = display.getDPI();
        Point printerDPI = printer.getDPI();

        double xScale = (double) printerDPI.x / screenDPI.x;
        double yScale = (double) printerDPI.y / screenDPI.y;

        // compute output image size in printer pixels
        double scaledImageWidth = Math.round(xScale * imageData.width);
        double scaledImageHeight = Math.round(yScale * imageData.height);

        if (fitToPage) {
            double pageScale = Math.min(printArea.width / scaledImageWidth,
                    printArea.height / scaledImageHeight);
            scaledImageWidth *= pageScale;
            scaledImageHeight *= pageScale;
        }

        Image image = addImage(imageData.scaledTo((int) scaledImageWidth,
                (int) scaledImageHeight));
        Rectangle imageBounds = image.getBounds();

        Point src = new Point(0, 0);
        Point dest = new Point(0, 0);
        if (printArea.width >= imageBounds.width) {
            dest.x = (printArea.width - imageBounds.width) / 2;
        }
        if (printArea.height >= imageBounds.height) {
            dest.y = (printArea.height - imageBounds.height) / 2;
        }

        Point remaining = new Point(imageBounds.width, imageBounds.height);
        while (remaining.x > 0 && remaining.y > 0) {
            if (printer.startPage()) {
                GC gc = new GC(printer);

                int width = Math.min(remaining.x, printArea.width);
                int height = Math.min(remaining.y, printArea.height);
                gc.drawImage(image, src.x, src.y, width, height, dest.x, dest.y,
                        width, height);

                gc.dispose();
                printer.endPage();
            }

            remaining.x -= printArea.width;
            src.x += printArea.width;
            if (remaining.x <= 0) {
                remaining.x = imageBounds.width;
                src.x = 0;
                remaining.y -= printArea.height;
                src.y += printArea.height;
            }
        }
    }

    private static ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof ComponentColorModel) {
            ComponentColorModel colorModel = (ComponentColorModel) bufferedImage
                    .getColorModel();

            PaletteData palette = new PaletteData(0x0000ff, 0x00ff00, 0xff0000);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0],
                            pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                }
            }
            return data;
        } else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel) bufferedImage
                    .getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                        blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        return null;
    }
}
