/**
 * This software was developed and / or modified by Raytheon Company,
 * pursuant to Contract EA133W-17-CQ-0082 with the US Government.
 *
 * U.S. EXPORT CONTROLLED TECHNICAL DATA
 * This software product contains export-restricted data whose
 * export/transfer/disclosure is restricted by U.S. law. Dissemination
 * to non-U.S. persons whether in the United States or abroad requires
 * an export license or other authorization.
 *
 * Contractor Name:        Raytheon Company
 * Contractor Address:     2120 South 72nd Street, Suite 900
 *                         Omaha, NE 68124
 *                         402.291.0100
 *
 * See the AWIPS II Master Rights File ("Master Rights File.pdf") for
 * further licensing information.
 **/
package com.raytheon.viz.core.contours;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.graphics.RGB;
import org.locationtech.jts.geom.Geometry;

import com.raytheon.uf.common.style.BaseLabelingPreferences;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.LineStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.PixelExtent;
import com.raytheon.uf.viz.core.RGBColors;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.exception.VizException;

/**
 * Data structure for contouring
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 15, 2024 2037631    mapeters    Extracted from ContourSupport, added dispose()
 *
 * </pre>
 *
 * @author mapeters
 */
public class ContourGroup {

    public double zoomLevel;

    public IWireframeShape posValueShape;

    public IWireframeShape negValueShape;

    // holds map of unique labeling preferences to frame shape
    public Map<BaseLabelingPreferences, IWireframeShape> labeledValuesMap = new HashMap<>();

    public ContourGroup parent;

    public PixelExtent lastUsedPixelExtent;

    public double lastDensity;

    public double lastPixelDensity;

    public Geometry contourGeometry;

    public String minMark;

    public String[] minVals;

    public double[][] minLabelPoints;

    public String maxMark;

    public String[] maxVals;

    public double[][] maxLabelPoints;

    public void drawContours(IGraphicsTarget target, RGB color, float lineWidth,
            LineStyle posLineStyle, LineStyle negLineStyle,
            IFont contourLabelFont, IFont minMaxLabelFont) throws VizException {

        target.drawWireframeShape(posValueShape, color, lineWidth, posLineStyle,
                contourLabelFont);
        target.drawWireframeShape(negValueShape, color, lineWidth, negLineStyle,
                contourLabelFont);

        for (Map.Entry<BaseLabelingPreferences, IWireframeShape> entry : labeledValuesMap
                .entrySet()) {
            RGB col = color;
            Float thickness = lineWidth;
            LineStyle lStyle;
            // Items with no specific styles are bucketed in the
            // posValueShape/negValueShape
            BaseLabelingPreferences label = entry.getKey();

            try {
                lStyle = LineStyle.valueOf(label.getLinePattern());
            } catch (@SuppressWarnings("squid:S1166")
            Exception e) {
                lStyle = LineStyle.DEFAULT;
            }

            if (label.getColor() != null) {
                // if incorrect color is specified, "white" is returned.
                col = RGBColors.getRGBColor(label.getColor());
            }
            if (label.getThickness() > 0) {
                thickness = (float) label.getThickness();
            }
            target.drawWireframeShape(entry.getValue(), col, thickness, lStyle,
                    contourLabelFont);
        }

        drawLabels(target, minMaxLabelFont, color, maxLabelPoints, maxVals,
                maxMark);
        drawLabels(target, minMaxLabelFont, color, minLabelPoints, minVals,
                minMark);
    }

    private void drawLabels(IGraphicsTarget target, IFont labelFont, RGB color,
            double[][] labelPoints, String[] vals, String mark)
            throws VizException {
        if (labelPoints != null) {
            int size = labelPoints.length;
            boolean isMark = mark != null;
            boolean isVal = vals != null;
            VerticalAlignment markVert = VerticalAlignment.MIDDLE;
            VerticalAlignment valVert = VerticalAlignment.MIDDLE;
            if (isMark && isVal) {
                markVert = VerticalAlignment.BOTTOM;
                valVert = VerticalAlignment.TOP;
            }
            List<DrawableString> strings = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                double point[] = labelPoints[i];
                if (isMark) {
                    DrawableString string = new DrawableString(mark, color);
                    string.font = labelFont;
                    string.setCoordinates(point[0], point[1], 1.0);
                    string.horizontalAlignment = HorizontalAlignment.CENTER;
                    string.verticallAlignment = markVert;
                    strings.add(string);
                }
                if (isVal) {
                    DrawableString string = new DrawableString(vals[i], color);
                    string.font = labelFont;
                    string.setCoordinates(point[0], point[1], 1.0);
                    string.horizontalAlignment = HorizontalAlignment.CENTER;
                    string.verticallAlignment = valVert;
                    strings.add(string);
                }
            }
            // draw all strings in a bulk operation for better performance
            target.drawStrings(strings);
        }
    }

    /**
     * Dispose this group's resources.
     */
    public void dispose() {
        if (posValueShape != null) {
            posValueShape.dispose();
        }

        if (negValueShape != null) {
            negValueShape.dispose();
        }
    }
}