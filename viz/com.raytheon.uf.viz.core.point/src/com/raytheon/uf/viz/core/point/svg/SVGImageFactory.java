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
package com.raytheon.uf.viz.core.point.svg;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.util.Arrays;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;

import com.raytheon.uf.common.localization.IPathManager;
import com.raytheon.uf.common.localization.LocalizationFile;
import com.raytheon.uf.common.localization.PathManagerFactory;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * 
 * Base class for things that dynamically generate BufferedImages based off of
 * an SVG document.
 * 
 * The primary use case for this class is for cases where the SVG is loaded from
 * a localization file and then the same document is repeatedly manipulated and
 * used to generate BufferedImages of a single color. For this use case a
 * subclass should be created which performs the document manipulation, the
 * document field is deliberately exposed to subclasses.
 * 
 * This class was designed to be flexible, it can use Documents from other
 * sources and it is able to render the SVG to any {@link BufferedImage}. It is
 * mostly intended to be subclassed but for very simple renderings it can be
 * instantiated directly.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Oct 27, 2015  4798     bsteffen  Initial creation
 * Jan 26, 2018  6698     njensen   Added antialiasing support
 * 
 * </pre>
 * 
 * @author bsteffen
 */
public class SVGImageFactory {

    public static final String PLOT_MODEL_DIR = "plotModels";

    protected final Document document;

    private final GVTBuilder builder;

    private final BridgeContext bridgeContext;

    public SVGImageFactory(String localizationPath) throws VizException {
        this(loadLocalizationSVG(localizationPath));
    }

    public SVGImageFactory(Document document) {
        this.document = document;
        this.bridgeContext = new BridgeContext(new UserAgentAdapter());
        this.builder = new GVTBuilder();
    }

    public BufferedImage createSingleColorImage(RGB color, int width,
            int height) {
        return createSingleColorImage(color, width, height, false, false);
    }

    public BufferedImage createSingleColorImage(RGB color, int width,
            int height, boolean alpha, boolean antialias) {
        BufferedImage image = null;
        IndexColorModel colorModel = buildColorModel(color, alpha);
        if (alpha) {
            // first paint the image in TYPE_INT_ARGB
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_ARGB);
        } else {
            image = new BufferedImage(width, height,
                    BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        }
        image = paint(image, antialias);

        if (alpha) {
            // now convert the image to a TYPE_BYTE_INDEXED to save memory
            image = transformFromArgbToIndexed(image, colorModel);
        }

        return image;
    }

    private BufferedImage transformFromArgbToIndexed(BufferedImage image,
            IndexColorModel colorModel) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage newImage = new BufferedImage(width, height,
                BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                newImage.setRGB(x, y, image.getRGB(x, y));
            }
        }
        return newImage;
    }

    private IndexColorModel buildColorModel(RGB color, boolean alpha) {
        IndexColorModel colorModel = null;
        if (alpha) {
            int size = 32;
            byte[] red = new byte[size];
            byte[] green = new byte[size];
            byte[] blue = new byte[size];
            byte[] alphaArray = new byte[size];
            Arrays.fill(red, (byte) color.red);
            Arrays.fill(green, (byte) color.green);
            Arrays.fill(blue, (byte) color.blue);
            for (int i = 0; i < size; i++) {
                alphaArray[i] = (byte) (i * 8);
            }
            colorModel = new IndexColorModel(8, size, red, green, blue,
                    alphaArray);
        } else {
            byte[] red = { 0, (byte) color.red };
            byte[] green = { 0, (byte) color.green };
            byte[] blue = { 0, (byte) color.blue };
            colorModel = new IndexColorModel(8, red.length, red, green, blue,
                    0);
        }
        return colorModel;
    }

    protected BufferedImage paint(BufferedImage image, boolean antiAliased) {
        GraphicsNode gn = builder.build(bridgeContext, document);
        Graphics2D g2d = null;
        try {
            g2d = image.createGraphics();
            if (antiAliased) {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
            }
            gn.paint(g2d);
        } finally {
            if (g2d != null) {
                g2d.dispose();
            }
        }
        return image;
    }

    protected static Document loadLocalizationSVG(String localizationPath)
            throws VizException {
        LocalizationParsedURLHandler.register();
        IPathManager pathManager = PathManagerFactory.getPathManager();
        LocalizationFile file = pathManager
                .getStaticLocalizationFile(localizationPath);
        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        try {
            return f.createDocument(file.getFile().toURI().toString());
        } catch (IOException e) {
            throw new VizException(
                    "Error loading symbol file: " + localizationPath, e);
        }
    }

    protected static String plotModelFile(String fileName) {
        return "plotModels" + IPathManager.SEPARATOR + fileName;
    }

}
