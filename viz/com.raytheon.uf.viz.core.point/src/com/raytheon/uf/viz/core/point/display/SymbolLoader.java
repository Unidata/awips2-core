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
package com.raytheon.uf.viz.core.point.display;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.data.IRenderedImageCallback;
import com.raytheon.uf.viz.core.drawables.IImage;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.point.svg.SVGImageFactory;

/**
 * Loads symbols from svg into IImage
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Sep 25, 2009  3099     bsteffen  Initial creation
 * Oct 20, 2010  6853     bgonzale  Migrated common symbol loading code.
 * Aug 09, 2013  2033     mschenke  Switched File.separator to IPathManager.SEPARATOR
 * Aug 11, 2014  3504     mapeters  Replaced deprecated IODataPreparer
 *                                  instances with IRenderedImageCallback.
 * May 14, 2015  4079     bsteffen  Move to core.point
 * Oct 27, 2015  4798     bsteffen  Extend SVGImageFactory
 * 
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class SymbolLoader extends SVGImageFactory {

    private IGraphicsTarget target;

    private RGB color;

    private HashMap<Character, IImage> images = new HashMap<Character, IImage>();

    public SymbolLoader() throws VizException {
        super(plotModelFile("WxSymbolText.svg"));
    }

    public IImage getImage(IGraphicsTarget target, RGB color, char c) {

        if (!target.equals(this.target) || !color.equals(this.color)) {
            images.clear();
            this.target = target;
            this.color = color;
        }

        IImage image = images.get(c);

        if (image == null) {
            image = target.initializeRaster(new ImageCallback(color, c));
            images.put(c, image);
        }
        return images.get(c);
    }

    private final class ImageCallback implements IRenderedImageCallback {

        private final RGB color;

        private final char c;

        public ImageCallback(RGB color, char c) {
            this.color = color;
            this.c = c;
        }

        @Override
        public RenderedImage getImage() throws VizException {
            synchronized (document) {
                Element textNode = document.getElementById("theText");
                Text textText = (Text) textNode.getFirstChild();
                textText.setNodeValue(Integer.toString(c));

                return createSingleColorImage(color, 12, 12);
            }
        }

    }
}
