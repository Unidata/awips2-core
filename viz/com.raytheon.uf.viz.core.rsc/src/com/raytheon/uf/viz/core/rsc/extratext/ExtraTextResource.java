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
package com.raytheon.uf.viz.core.rsc.extratext;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.DrawableString;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.IGraphicsTarget.HorizontalAlignment;
import com.raytheon.uf.viz.core.IGraphicsTarget.TextStyle;
import com.raytheon.uf.viz.core.IGraphicsTarget.VerticalAlignment;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.uf.viz.core.drawables.PaintProperties;
import com.raytheon.uf.viz.core.drawables.ResourcePair;
import com.raytheon.uf.viz.core.drawables.ext.ICanvasRenderingExtension;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.IResourceGroup;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.uf.viz.core.rsc.ResourceList;
import com.raytheon.uf.viz.core.rsc.capabilities.ColorableCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.ImagingCapability;
import com.raytheon.uf.viz.core.rsc.capabilities.MagnificationCapability;

/**
 * Resource which is responsible for getting all the extra text from resources
 * on the display and rendering it non-overlapping and in the correct style for
 * each resource.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Jul 29, 2010  5991     mschenke  Initial creation
 * Aug 14, 2014  3523     mapeters  Updated deprecated {@link
 *                                  DrawableString#textStyle} assignments.
 * Nov 05, 2015  5070     randerso  Adjust font sizes for dpi scaling
 * May 18, 2016  3253     bsteffen  Move to core, rename, simplify removal.
 * 
 * </pre>
 * 
 * @author mschenke
 */
public class ExtraTextResource extends
        AbstractVizResource<ExtraTextResourceData, IDescriptor> {

    private class TextPair {

        String[] text;

        RGB color;

        float magnification = 1.0f;

        float alpha = 1.0f;

        TextPair(AbstractVizResource<?, ?> rsc, DataTime time) {
            if (rsc instanceof IExtraTextGeneratingResource) {
                text = ((IExtraTextGeneratingResource) rsc).getExtraText(time);
            }

            magnification = rsc.getCapability(MagnificationCapability.class)
                    .getMagnification().floatValue();
            if (rsc.hasCapability(ImagingCapability.class)) {
                alpha = rsc.getCapability(ImagingCapability.class).getAlpha();
            }
            color = rsc.getCapability(ColorableCapability.class).getColor();
        }
    }

    /** y offset from the top of the canvas */
    private static final int Y_OFFSET = 27;

    /**
     * The x offset from left of canvas and also the distance between vertical
     * text
     */
    private static final int X_OFFSET = 5;

    /** font for drawing the text */
    private IFont textFont;

    private final ResourceList.RemoveListener listListener = new ResourceList.RemoveListener() {
        @Override
        public void notifyRemove(ResourcePair rp) {
            removeIfUnnecessary();
        }
    };

    protected ExtraTextResource(ExtraTextResourceData resourceData,
            LoadProperties loadProperties) {
        super(resourceData, loadProperties);
    }

    @Override
    protected void disposeInternal() {
        if (textFont != null) {
            textFont.dispose();
        }
        descriptor.getResourceList().removePostRemoveListener(listListener);
    }

    @Override
    protected void initInternal(IGraphicsTarget target) throws VizException {
        textFont = target.initializeFont(java.awt.Font.DIALOG, 9,
                new IFont.Style[] {});
        descriptor.getResourceList().addPostRemoveListener(listListener);
    }

    @Override
    protected void paintInternal(IGraphicsTarget target,
            PaintProperties paintProps) throws VizException {
        List<DrawableString> stringsToRender = new ArrayList<>();

        int yOffset = Y_OFFSET;
        for (ResourcePair rp : descriptor.getResourceList()) {
            int height = 0;
            if (rp.getResource() != null && rp.getProperties().isVisible()) {
                AbstractVizResource<?, ?> rsc = rp.getResource();
                if (rsc instanceof IExtraTextGeneratingResource) {
                    DataTime time = descriptor.getTimeForResource(rsc);
                    TextPair pair = new TextPair(rsc, time);
                    if (rsc.hasCapability(ImagingCapability.class)) {
                        paintProps.setAlpha(rsc.getCapability(
                                ImagingCapability.class).getAlpha());
                        pair.magnification = rsc
                                .getCapability(MagnificationCapability.class)
                                .getMagnification().floatValue();
                        pair.alpha = rsc.getCapability(ImagingCapability.class)
                                .getAlpha();
                    }
                    height = paintSingleResource(target, pair,
                            HorizontalAlignment.LEFT, yOffset, X_OFFSET,
                            stringsToRender);
                } else if (rsc instanceof IResourceGroup) {
                    height = paintResourceList(target, paintProps,
                            ((IResourceGroup) rsc).getResourceList(), yOffset,
                            stringsToRender);
                }
            }
            if (height > 0) {
                yOffset += (height + X_OFFSET);
            }
        }

        target.getExtension(ICanvasRenderingExtension.class).drawStrings(
                paintProps, stringsToRender.toArray(new DrawableString[0]));
    }

    /**
     * Paints a single resource's text
     * 
     * @param target
     * @param paintProps
     * @param text
     *            text to draw
     * @param yOffset
     *            in pixel space
     * @param xOffset
     *            in pixel space
     * @return the height of the text drawn
     * @throws VizException
     */
    private int paintSingleResource(IGraphicsTarget target, TextPair pair,
            HorizontalAlignment hAlign, int yOffset, int xOffset,
            List<DrawableString> stringsToRender) throws VizException {
        if (pair.text != null && xOffset > 0 && yOffset > 0
                && pair.text.length > 0) {
            RGB[] colors = new RGB[pair.text.length];
            for (int i = 0; i < pair.text.length; ++i) {
                colors[i] = pair.color;
            }
            textFont.setMagnification(pair.magnification);
            DrawableString dString = new DrawableString(pair.text, colors);
            dString.font = textFont;
            dString.setCoordinates(xOffset, yOffset, 0);
            dString.addTextStyle(TextStyle.BLANKED);
            dString.horizontalAlignment = hAlign;
            dString.verticalAlignment = VerticalAlignment.TOP;
            dString.basics.alpha = pair.alpha;
            stringsToRender.add(dString);
            return (int) Math
                    .ceil(target.getStringsBounds(dString).getHeight());

        }
        return 0;
    }

    /**
     * 
     * @param target
     * @param paintProps
     * @param list
     *            resource list to paint
     * @param yOffset
     *            in pixel space
     * @return the height of the overall text drawn
     * @throws VizException
     */
    private int paintResourceList(IGraphicsTarget target,
            PaintProperties paintProps, ResourceList list, int yOffset,
            List<DrawableString> stringsToRender) throws VizException {
        List<TextPair> textArrays = new ArrayList<>();
        for (ResourcePair rp : list) {
            if (rp.getResource() != null) {
                AbstractVizResource<?, ?> rsc = rp.getResource();
                if (rsc instanceof IExtraTextGeneratingResource) {
                    if (rp.getProperties().isVisible()) {
                        TextPair pair = new TextPair(rsc,
                                descriptor.getTimeForResource(rsc));
                        pair.alpha *= 2;
                        textArrays.add(pair);

                    } else {
                        textArrays.add(null);
                    }
                }
            }
        }

        int maxHeight = 0;
        if (textArrays.size() > 0) {
            int drawingWidth = paintProps.getCanvasBounds().width
                    - (X_OFFSET * 2);
            int midRscsInc = drawingWidth / textArrays.size() - 1;
            for (int i = 0; i < textArrays.size(); ++i) {
                TextPair text = textArrays.get(i);
                if (text != null) {
                    int height = 0;
                    if (i == 0) {
                        height = paintSingleResource(target, text,
                                HorizontalAlignment.LEFT, yOffset, X_OFFSET,
                                stringsToRender);
                    } else if (i < textArrays.size() - 1) {
                        height = paintSingleResource(target, text,
                                HorizontalAlignment.CENTER, yOffset, midRscsInc
                                        * i, stringsToRender);
                    } else {
                        height = paintSingleResource(target, text,
                                HorizontalAlignment.RIGHT, yOffset,
                                paintProps.getCanvasBounds().width
                                        - (X_OFFSET * 2), stringsToRender);
                    }
                    maxHeight = Math.max(maxHeight, height);
                }
            }
        }
        return maxHeight;
    }

    protected void removeIfUnnecessary() {
        ResourceList resourceList = descriptor.getResourceList();
        List<AbstractVizResource<?, ?>> generators = resourceList
                .getResourcesByType(IExtraTextGeneratingResource.class);
        if (!generators.isEmpty()) {
            return;
        }
        List<IResourceGroup> groups = resourceList
                .getResourcesByTypeAsType(IResourceGroup.class);
        for (IResourceGroup group : groups) {
            ResourceList groupList = group.getResourceList();
            generators = groupList
                    .getResourcesByType(IExtraTextGeneratingResource.class);
            if (!generators.isEmpty()) {
                return;
            }
        }
        resourceList.removeRsc(this);
    }

}
