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
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;

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
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class SVGImageFactory {

    public static final String PLOT_MODEL_DIR = "plotModels";

    protected final Document document;

    private final GVTBuilder builder;

    private final BridgeContext bridgeContext;

    public SVGImageFactory(String localizationPath)
            throws VizException {
        this(loadLocalizationSVG(localizationPath));
    }

    public SVGImageFactory(Document document) {
        this.document = document;
        this.bridgeContext = new BridgeContext(new UserAgentAdapter());
        this.builder = new GVTBuilder();
    }

    public BufferedImage createSingleColorImage(RGB color, int width, int height) {
        byte[] red = { 0, (byte) color.red };
        byte[] green = { 0, (byte) color.green };
        byte[] blue = { 0, (byte) color.blue };
        IndexColorModel colorModel = new IndexColorModel(8, 2, red, green,
                blue, 0);
        BufferedImage image = new BufferedImage(width, height,
                BufferedImage.TYPE_BYTE_INDEXED, colorModel);
        return paint(image);
    }

    public BufferedImage paint(BufferedImage image) {
        GraphicsNode gn = builder.build(bridgeContext, document);
        Graphics2D g2d = null;
        try {
            g2d = image.createGraphics();
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
            throw new VizException("Error loading symbol file: "
                    + localizationPath, e);
        }
    }

    protected static String plotModelFile(String fileName) {
        return "plotModels" + IPathManager.SEPARATOR + fileName;
    }

}
