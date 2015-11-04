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

import java.awt.image.BufferedImage;

import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Element;

import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.point.svg.SVGImageFactory;

/**
 * 
 * A class which can create a weather icon from it's numeric value using an svg.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------
 * Jan 26, 2010           bsteffen  Initial creation
 * May 14, 2015  4079     bsteffen  Move to core.point
 * Oct 27, 2015  4798     bsteffen  Extend SVGImageFactory
 * 
 * </pre>
 * 
 * @author bsteffen
 * @version 1.0
 */
public class PointIconFactory extends SVGImageFactory {

    private RGB color;

    private int size;

    public PointIconFactory(RGB color, int size) throws VizException {
        super(plotModelFile("Icon.svg"));
        this.color = color;
        this.size = size;
    }

    public BufferedImage getIcon(int i) {
        if (i < 0) {
            i = 0;
        }
        document.getElementsByTagName("svg").item(0).getAttributes()
                .getNamedItem("viewBox")
                .setNodeValue("0 0 " + size + " " + size);
        Element iconNode = document.getElementById("icon");
        iconNode.getFirstChild().setNodeValue(Integer.toString(i));
        iconNode.getAttributeNode("x").setNodeValue(String.valueOf(size / 2));
        iconNode.getAttributeNode("y").setNodeValue(String.valueOf(size / 2));
        return createSingleColorImage(color, size, size);
    }

}
