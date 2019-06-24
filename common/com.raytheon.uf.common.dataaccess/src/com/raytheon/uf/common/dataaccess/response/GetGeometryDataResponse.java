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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.raytheon.uf.common.dataaccess.geom.IGeometryData;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeTypeAdapter;
import org.locationtech.jts.io.WKBWriter;

/**
 * Response for <code>GetGeometryDataRequest</code>.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer    Description
 * ------------- -------- ----------- --------------------------
 * Jun 3, 2013            dgilling    Initial creation
 * Jan 06, 2014  2537     bsteffen    Share geometry WKT.
 * Jun 30, 2015  4569     nabowle     Switch to WKB.
 * Oct 24, 2016  5919     njensen     Use custom serialization type adapter
 *                                     Added equals() and hashCode()
 *
 * </pre>
 *
 * @author dgilling
 */

@DynamicSerialize
@DynamicSerializeTypeAdapter(factory = GeomDataRespAdapter.class)
public class GetGeometryDataResponse {

    @DynamicSerializeElement
    private List<byte[]> geometryWKBs;

    @DynamicSerializeElement
    private List<GeometryResponseData> geoData;

    public GetGeometryDataResponse() {
        // no-op, for serialization only
    }

    public GetGeometryDataResponse(final Collection<IGeometryData> geoData) {
        Map<ByteArrayKey, Integer> indexMap = new HashMap<>();
        WKBWriter writer = new WKBWriter();
        this.geometryWKBs = new ArrayList<>();
        this.geoData = new ArrayList<>(geoData.size());
        byte[] wkb;
        Integer index;
        ByteArrayKey key;
        for (IGeometryData element : geoData) {
            wkb = writer.write(element.getGeometry());
            key = new ByteArrayKey(wkb);
            index = indexMap.get(key);
            if (index == null) {
                index = geometryWKBs.size();
                geometryWKBs.add(wkb);
                indexMap.put(key, index);
            }
            this.geoData.add(new GeometryResponseData(element, index));
        }
    }

    public List<GeometryResponseData> getGeoData() {
        return geoData;
    }

    public void setGeoData(List<GeometryResponseData> geoData) {
        this.geoData = geoData;
    }

    public List<byte[]> getGeometryWKBs() {
        return geometryWKBs;
    }

    public void setGeometryWKBs(List<byte[]> geometryWKBs) {
        this.geometryWKBs = geometryWKBs;
    }

    /**
     * Class to allow hashCode and equals for a byte array.
     */
    private static class ByteArrayKey {
        private byte[] data;

        public ByteArrayKey(byte[] ba) {
            this.data = ba;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(this.data);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            ByteArrayKey other = (ByteArrayKey) obj;
            return Arrays.equals(data, other.data);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((geoData == null) ? 0 : geoData.hashCode());
        // modified from auto-generated
        if (geometryWKBs == null) {
            result = prime * result + 0;
        } else {
            for (int i = 0; i < geometryWKBs.size(); i++) {
                result = prime * result + Arrays.hashCode(geometryWKBs.get(i));
            }
        }
        // end of modifications
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        GetGeometryDataResponse other = (GetGeometryDataResponse) obj;
        if (geoData == null) {
            if (other.geoData != null)
                return false;
        } else if (!geoData.equals(other.geoData))
            return false;
        if (geometryWKBs == null) {
            if (other.geometryWKBs != null)
                return false;
        }
        // modified from auto-generated
        else {
            if (geometryWKBs.size() != other.getGeometryWKBs().size()) {
                return false;
            }
            for (int i = 0; i < geometryWKBs.size(); i++) {
                if (!Arrays.equals(geometryWKBs.get(i),
                        other.geometryWKBs.get(i))) {
                    return false;
                }
            }
        }
        // end of modifications
        return true;
    }

}
