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
import com.vividsolutions.jts.io.WKBWriter;

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
 *
 * </pre>
 *
 * @author dgilling
 * @version 1.0
 */

@DynamicSerialize
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
        this.geoData = new ArrayList<GeometryResponseData>(geoData.size());
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
}
