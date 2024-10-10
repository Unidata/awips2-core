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
package com.raytheon.viz.core.contours;

import java.util.Objects;

import org.geotools.coverage.grid.GeneralGridGeometry;

import com.raytheon.uf.common.numeric.source.DataSource;
import com.raytheon.uf.common.style.contour.ContourPreferences;
import com.raytheon.uf.viz.core.IExtent;
import com.raytheon.uf.viz.core.IGraphicsTarget;
import com.raytheon.uf.viz.core.map.IMapDescriptor;

/**
 * ContourCreateRequest
 *
 * A request containing all the data needed for a ContourManagerJob to create
 * contours. The created contours are placed back into this object so keep a
 * reference around to check for finished contours.
 *
 * <pre>
 *
 *    SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- ------------------------------------------
 * Feb 25, 2011           ekladstrup  Initial creation
 * Feb 27, 2014  2791     bsteffen    Switch from IDataRecord to DataSource
 * Dec 06, 2021  8341     randerso    Added use of getResourceId for contour
 *                                    logging
 * Aug 20, 2024  2037631  mapeters    Cleanup contour disposal
 *
 * </pre>
 *
 * @author ekladstrup
 */
public class ContourCreateRequest {

    private String resourceId;

    private String identifier;

    private DataSource[] source;

    private float level;

    private IExtent pixelExtent;

    private double currentDensity;

    private double currentMagnification;

    private GeneralGridGeometry imageGridGeometry;

    private IGraphicsTarget target;

    private IMapDescriptor descriptor;

    private ContourPreferences prefs;

    private float zoom;

    private boolean canceled;

    private ContourGroup contourGroup;

    private boolean disposed = false;

    public ContourCreateRequest(String resourceId, String identifier,
            DataSource[] source, float level, IExtent pixelExtent,
            double currentDensity, double currentMagnification,
            GeneralGridGeometry imageGridGeometry, IGraphicsTarget target,
            IMapDescriptor descriptor, ContourPreferences prefs, float zoom) {
        super();
        this.resourceId = resourceId;
        this.identifier = identifier;
        this.source = source;
        this.level = level;
        this.pixelExtent = pixelExtent;
        this.imageGridGeometry = imageGridGeometry;
        this.target = target;
        this.descriptor = descriptor;
        this.prefs = prefs;
        this.currentDensity = currentDensity;
        this.currentMagnification = currentMagnification;
        this.zoom = zoom;
        this.canceled = false;
        this.contourGroup = null;
    }

    /**
     * @return the resourceId
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * @param resourceId
     *            the resourceId to be used for logging performance info
     */
    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public DataSource[] getSource() {
        return source;
    }

    public void setSource(DataSource[] source) {
        this.source = source;
    }

    public float getLevel() {
        return level;
    }

    public void setLevel(float level) {
        this.level = level;
    }

    public IExtent getPixelExtent() {
        return pixelExtent;
    }

    public void setPixelExtent(IExtent pixelExtent) {
        this.pixelExtent = pixelExtent;
    }

    public double getCurrentDensity() {
        return currentDensity;
    }

    public void setCurrentDensity(double currentDensity) {
        this.currentDensity = currentDensity;
    }

    public double getCurrentMagnification() {
        return currentMagnification;
    }

    public void setCurrentMagnification(double currentMagnification) {
        this.currentMagnification = currentMagnification;
    }

    public GeneralGridGeometry getImageGridGeometry() {
        return imageGridGeometry;
    }

    public void setImageGridGeometry(GeneralGridGeometry imageGridGeometry) {
        this.imageGridGeometry = imageGridGeometry;
    }

    public IGraphicsTarget getTarget() {
        return target;
    }

    public void setTarget(IGraphicsTarget target) {
        this.target = target;
    }

    public IMapDescriptor getDescriptor() {
        return descriptor;
    }

    public void setDescriptor(IMapDescriptor descriptor) {
        this.descriptor = descriptor;
    }

    public ContourPreferences getPrefs() {
        return prefs;
    }

    public void setPrefs(ContourPreferences prefs) {
        this.prefs = prefs;
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean cancel) {
        this.canceled = cancel;
    }

    public ContourGroup getContourGroup() {
        return contourGroup;
    }

    public synchronized void setContourGroup(ContourGroup contourGroup) {
        // synchronized so disposed can't get set after it is checked
        if (!this.disposed) {
            this.contourGroup = contourGroup;
        } else {
            contourGroup.dispose();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentDensity, currentMagnification, identifier,
                imageGridGeometry, level, pixelExtent, prefs, zoom);
    }

    @Override
    public boolean equals(Object obj) {
        // right now comparing:
        // identifier, level, pixelExtent, currentDensity, currentMagnification,
        // prefs, zoom, imageGridGeometry, mapGridGeometry
        //
        // NOT COMPARING:
        // record, worldGridToCRSTransform, target, descriptor
        //
        // Should never use canceled or contourGroup to check equality
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ContourCreateRequest other = (ContourCreateRequest) obj;
        return Double.doubleToLongBits(currentDensity) == Double
                .doubleToLongBits(other.currentDensity)
                && Double.doubleToLongBits(currentMagnification) == Double
                        .doubleToLongBits(other.currentMagnification)
                && Objects.equals(identifier, other.identifier)
                && Objects.equals(imageGridGeometry, other.imageGridGeometry)
                && Float.floatToIntBits(level) == Float
                        .floatToIntBits(other.level)
                && Objects.equals(pixelExtent, other.pixelExtent)
                && Objects.equals(prefs, other.prefs)
                && Float.floatToIntBits(zoom) == Float
                        .floatToIntBits(other.zoom);
    }

    public synchronized void dispose() {
        // synchronized to avoid leaking memory because of a missed dispose
        this.disposed = true;
        if (contourGroup != null) {
            this.contourGroup.posValueShape.dispose();
            this.contourGroup.negValueShape.dispose();
            this.contourGroup = null;
        }
    }

}
