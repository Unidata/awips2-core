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
package com.raytheon.viz.core.gl.internal;

import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.graphics.Point;

import com.raytheon.uf.viz.core.drawables.IFont;
import com.raytheon.viz.core.gl.IGLFont;
import com.jogamp.opengl.util.awt.TextRenderer;

/**
 * GL font that is set by preferences page, listens for updates
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 19, 2010            mschenke    Initial creation
 * Nov 04, 2015   5070     randerso    Added DPI font scaling
 *
 * </pre>
 *
 * @author mschenke
 * @version 1.0
 */

public class PreferenceBasedGLFont implements IGLFont, IPropertyChangeListener {

    private IGLFont preferenceFont;

    private FontRegistry registry;

    private String propertyName;

    public PreferenceBasedGLFont(IGLFont font, String propName,
            FontRegistry registry) {
        this.preferenceFont = font;
        this.propertyName = propName;
        this.registry = registry;
        registry.addListener(this);
    }

    @Override
    public int hashCode() {
        return preferenceFont.hashCode();
    }

    @Override
    public void dispose() {
        preferenceFont.dispose();
    }

    @Override
    public boolean equals(Object obj) {
        return preferenceFont.equals(obj);
    }

    @Override
    public Point getDPI() {
        return preferenceFont.getDPI();
    }

    @Override
    public String getFontName() {
        return preferenceFont.getFontName();
    }

    @Override
    public float getFontSize() {
        return preferenceFont.getFontSize();
    }

    @Override
    public Style[] getStyle() {
        return preferenceFont.getStyle();
    }

    @Override
    public TextRenderer getTextRenderer() {
        return preferenceFont.getTextRenderer();
    }

    @Override
    public IFont deriveWithSize(float size) {
        return preferenceFont.deriveWithSize(size);
    }

    @Override
    public void setMagnification(float magnification) {
        preferenceFont.setMagnification(magnification);
    }

    @Override
    public void setMagnification(float magnification, boolean scaleFont) {
        preferenceFont.setMagnification(magnification, scaleFont);
    }

    @Override
    public float getMagnification() {
        return preferenceFont.getMagnification();
    }

    @Override
    public boolean getSmoothing() {
        return preferenceFont.getSmoothing();
    }

    @Override
    public void setSmoothing(boolean smoothing) {
        preferenceFont.setSmoothing(smoothing);
    }

    @Override
    public boolean isScaleFont() {
        return preferenceFont.isScaleFont();
    }

    @Override
    public void setScaleFont(boolean scaleFont) {
        preferenceFont.setScaleFont(scaleFont);
    }

    @Override
    public String toString() {
        return preferenceFont.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse
     * .jface.util.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (propertyName.equals(event.getProperty())) {
            boolean smoothing = preferenceFont.getSmoothing();
            boolean scaleFont = preferenceFont.isScaleFont();
            float magnification = preferenceFont.getMagnification();
            preferenceFont.disposeInternal();
            preferenceFont = FontFactory.getInstance().getFont(getDPI(),
                    propertyName);
            preferenceFont.setSmoothing(smoothing);
            preferenceFont.setScaleFont(scaleFont);
            preferenceFont.setMagnification(magnification);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see com.raytheon.viz.core.gl.IGLFont#disposeInternal()
     */
    @Override
    public void disposeInternal() {
        preferenceFont.disposeInternal();
        registry.removeListener(this);
    }
}
