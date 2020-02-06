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
package com.raytheon.viz.core.rsc.jts;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.geospatial.ReferencedGeometry;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.drawables.IDescriptor;
import com.raytheon.uf.viz.core.drawables.IShadedShape;
import com.raytheon.uf.viz.core.drawables.IWireframeShape;
import com.raytheon.uf.viz.core.exception.VizException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

/**
 * @deprecated use com.raytheon.uf.viz.core.drawables.JTSCompiler
 * 
 *             <pre>
 * 
 *  SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Oct 24, 2006           chammack  Initial Creation.
 * Feb 14, 2014  2804     mschenke  Rewrote to move clipping from
 *                                  GLWireframeShape2D to here
 * Apr 21, 2014  2997     randerso  Improved error handling in
 *                                  handle(ReferencedGeometry, JTSGeometryData)
 * Jul 16, 2014  3366     bclement  don't reuse arrays for line segments in
 *                                  handlePoint()
 * Jan 29, 2015  4062     randerso  Don't throw errors for ProjectionExceptions
 * Sep 13, 2016  3241     bsteffen  Move real implementation to uf.viz.core
 *                                  plugin and deprecate
 * 
 *             </pre>
 * 
 * @author chammack
 */
@Deprecated
public class JTSCompiler {

    public static enum PointStyle {

        SQUARE(com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle.SQUARE), CROSS(
                com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle.CROSS);

        private final com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle style;

        private PointStyle(
                com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle style) {
            this.style = style;
        }

        public com.raytheon.uf.viz.core.drawables.JTSCompiler.PointStyle getStyle() {
            return style;
        }
    };

    public static class JTSGeometryData {

        private com.raytheon.uf.viz.core.drawables.JTSCompiler.JTSGeometryData data;

        protected JTSGeometryData(
                com.raytheon.uf.viz.core.drawables.JTSCompiler.JTSGeometryData data) {
            this.data = data;
        }

        public void setGeometryColor(RGB geometryColor) {
            this.data.setGeometryColor(geometryColor);
        }

        public void setWorldWrapCorrect(boolean worldWrapCorrect) {
            this.data.setWorldWrapCorrect(worldWrapCorrect);
        }

        public void setClippingArea(Polygon clippingArea) {
            this.data.setClippingArea(clippingArea);
        }

        public void setClippingExtent(IExtent clippingExtent) {
            this.data.setClippingExtent(clippingExtent);
        }

        public void setPointStyle(PointStyle pointStyle) {
            this.data.setPointStyle(pointStyle.getStyle());
        }

        protected com.raytheon.uf.viz.core.drawables.JTSCompiler.JTSGeometryData getData() {
            return data;
        }

    }

    private final com.raytheon.uf.viz.core.drawables.JTSCompiler delegate;

    private final PointStyle defaultPointStyle;

    public JTSCompiler(IShadedShape shadedShp, IWireframeShape wireShp,
            IDescriptor descriptor) {
        delegate = new com.raytheon.uf.viz.core.drawables.JTSCompiler(shadedShp,
                wireShp, descriptor);
        defaultPointStyle = null;
    }

    @Deprecated
    public JTSCompiler(IShadedShape shadedShp, IWireframeShape wireShp,
            IDescriptor descriptor, PointStyle pointStyle) {
        delegate = new com.raytheon.uf.viz.core.drawables.JTSCompiler(shadedShp,
                wireShp, descriptor);
        defaultPointStyle = pointStyle;
    }

    public JTSGeometryData createGeometryData() {
        JTSGeometryData data = new JTSGeometryData(
                delegate.createGeometryData());
        if (defaultPointStyle != null) {
            data.setPointStyle(defaultPointStyle);
        }
        return data;
    }

    @Deprecated
    public void handle(ReferencedGeometry referencedGeom, RGB color)
            throws VizException {
        JTSGeometryData data = createGeometryData();
        data.setGeometryColor(color);
        handle(referencedGeom, data);
    }

    @Deprecated
    public void handle(Geometry geom, boolean wrapCheck) throws VizException {
        JTSGeometryData data = createGeometryData();
        data.setWorldWrapCorrect(wrapCheck);
        handle(geom, data);
    }

    @Deprecated
    public void handle(Geometry geom, RGB color) throws VizException {
        JTSGeometryData data = createGeometryData();
        data.setGeometryColor(color);
        handle(geom, data);
    }

    @Deprecated
    public void handle(Geometry geom, RGB color, boolean wrapCorrect)
            throws VizException {
        JTSGeometryData data = createGeometryData();
        data.setGeometryColor(color);
        data.setWorldWrapCorrect(wrapCorrect);
        handle(geom, data);
    }

    public void handle(Geometry geom) throws VizException {
        delegate.handle(geom);
    }

    public void handle(Geometry geom, JTSGeometryData data)
            throws VizException {
        delegate.handle(geom, data.getData());
    }

    public void handle(ReferencedGeometry geom) throws VizException {
        delegate.handle(geom);
    }

    public void handle(ReferencedGeometry geom, JTSGeometryData data)
            throws VizException {
        delegate.handle(geom, data.getData());
    }

}
