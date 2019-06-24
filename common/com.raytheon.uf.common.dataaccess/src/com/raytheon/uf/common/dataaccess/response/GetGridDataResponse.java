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
package com.raytheon.uf.common.dataaccess.response;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.coverage.grid.GridEnvelope;

import com.raytheon.uf.common.dataaccess.grid.IGridData;
import com.raytheon.uf.common.geospatial.LatLonReprojection;
import com.raytheon.uf.common.geospatial.LatLonWrapper;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import org.locationtech.jts.geom.Envelope;

/**
 * Response for <code>GetGridDataRequest</code>.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jun 4, 2013            dgilling     Initial creation
 * Oct 18, 2016 5916       bsteffen    Allow lazy loading of lat/lon data
 * 
 * </pre>
 * 
 * @author dgilling
 */
@DynamicSerialize
public class GetGridDataResponse {

    @DynamicSerializeElement
    private List<GridResponseData> gridData;

    @DynamicSerializeElement
    private Map<String, Integer> siteNxValues;

    @DynamicSerializeElement
    private Map<String, Integer> siteNyValues;

    @DynamicSerializeElement
    private Map<String, float[]> siteLatGrids;

    @DynamicSerializeElement
    private Map<String, float[]> siteLonGrids;

    @DynamicSerializeElement
    private Map<String, Envelope> siteEnvelopes;

    @DynamicSerializeElement
    private Map<String, String> siteCrsWkt;

    public GetGridDataResponse() {
        // no-op, for serialization only
    }

    public GetGridDataResponse(final Collection<IGridData> gridData,
            final boolean includeLatLon) {
        this.gridData = new ArrayList<>(gridData.size());
        siteNxValues = new HashMap<>(gridData.size(), 1);
        siteNyValues = new HashMap<>(gridData.size(), 1);
        if (includeLatLon) {
            siteLatGrids = new HashMap<>(gridData.size(), 1);
            siteLonGrids = new HashMap<>(gridData.size(), 1);
        } else {
            siteEnvelopes = new HashMap<>(gridData.size(), 1);
            siteCrsWkt = new HashMap<>(gridData.size(), 1);
        }

        for (IGridData grid : gridData) {
            this.gridData.add(new GridResponseData(grid));

            String locationName = grid.getLocationName();
            if (!siteNxValues.containsKey(locationName)) {
                GridGeometry2D gridGeometry = grid.getGridGeometry();
                GridEnvelope gridShape = gridGeometry.getGridRange();
                siteNxValues.put(locationName, gridShape.getSpan(0));
                siteNyValues.put(locationName, gridShape.getSpan(1));
                if (includeLatLon) {
                    LatLonWrapper latLonData = LatLonReprojection
                            .getLatLons(gridGeometry);
                    siteLatGrids.put(locationName, latLonData.getLats());
                    siteLonGrids.put(locationName, latLonData.getLons());
                } else {
                    Envelope envelope = new Envelope(
                            new ReferencedEnvelope(gridGeometry.getEnvelope()));
                    siteEnvelopes.put(locationName, envelope);
                    siteCrsWkt.put(locationName, gridGeometry
                            .getCoordinateReferenceSystem().toWKT());
                }
            }
        }
    }

    public List<GridResponseData> getGridData() {
        return gridData;
    }

    public void setGridData(List<GridResponseData> gridData) {
        this.gridData = gridData;
    }

    public Map<String, Integer> getSiteNxValues() {
        return siteNxValues;
    }

    public void setSiteNxValues(Map<String, Integer> siteNxValues) {
        this.siteNxValues = siteNxValues;
    }

    public Map<String, Integer> getSiteNyValues() {
        return siteNyValues;
    }

    public void setSiteNyValues(Map<String, Integer> siteNyValues) {
        this.siteNyValues = siteNyValues;
    }

    public Map<String, float[]> getSiteLatGrids() {
        return siteLatGrids;
    }

    public void setSiteLatGrids(Map<String, float[]> siteLatGrids) {
        this.siteLatGrids = siteLatGrids;
    }

    public Map<String, float[]> getSiteLonGrids() {
        return siteLonGrids;
    }

    public void setSiteLonGrids(Map<String, float[]> siteLonGrids) {
        this.siteLonGrids = siteLonGrids;
    }

    public Map<String, Envelope> getSiteEnvelopes() {
        return siteEnvelopes;
    }

    public void setSiteEnvelopes(Map<String, Envelope> siteEnvelopes) {
        this.siteEnvelopes = siteEnvelopes;
    }

    public Map<String, String> getSiteCrsWkt() {
        return siteCrsWkt;
    }

    public void setSiteCrsWkt(Map<String, String> siteCrsWkt) {
        this.siteCrsWkt = siteCrsWkt;
    }

}
