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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.raytheon.uf.common.serialization.IDeserializationContext;
import com.raytheon.uf.common.serialization.ISerializationContext;
import com.raytheon.uf.common.serialization.ISerializationTypeAdapter;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.DataTime;

/**
 * A custom serialization adapter for {@link GetGeometryDataResponse}. This
 * adapter drops some of the self-describing nature of DynamicSerialize to
 * achieve faster performance and smaller messages. It assumes that the
 * deserialization code is familiar with this order of the data. The primary
 * efficiency gain is that the List<GeometryResponseData> will not have each
 * item's type name encoded repeatedly, which can add up if the list size is
 * large. There are smaller efficiency gains in enforcing that the fields are in
 * a set order, skipping encoding field names.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 17, 2016  5919      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class GeomDataRespAdapter
        implements ISerializationTypeAdapter<GetGeometryDataResponse> {

    @Override
    public void serialize(ISerializationContext serializer,
            GetGeometryDataResponse object) throws SerializationException {
        List<byte[]> wkbs = object.getGeometryWKBs();
        // write list size
        serializer.writeI32(wkbs.size());
        // write byte arrays
        for (byte[] wkb : wkbs) {
            serializer.writeBinary(wkb);
        }

        List<GeometryResponseData> geoList = object.getGeoData();
        // write list size
        serializer.writeI32(geoList.size());

        // write objects
        for (GeometryResponseData geo : geoList) {
            serializer.writeI32(geo.getGeometryWKBindex());
            serializer.writeObject(geo.getTime());
            serializer.writeObject(geo.getLevel());
            serializer.writeObject(geo.getLocationName());
            serializer.writeObject(geo.getAttributes());

            // write data map
            Map<String, Object[]> params = geo.getDataMap();
            serializer.writeI32(params.size());
            Set<Entry<String, Object[]>> entries = params.entrySet();
            for (Entry<String, Object[]> entry : entries) {
                // write parameter name
                serializer.writeString(entry.getKey());

                // write parameter values
                Object[] value = entry.getValue();
                // actual value
                serializer.writeObject(value[0]);
                // value type as string
                serializer.writeString(value[1].toString());
                // unit
                serializer.writeObject(value[2]);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public GetGeometryDataResponse deserialize(
            IDeserializationContext deserializer)
                    throws SerializationException {
        GetGeometryDataResponse resp = new GetGeometryDataResponse();

        // read list size
        int size = deserializer.readI32();
        // read wkbs
        List<byte[]> wkbs = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            wkbs.add(deserializer.readBinary());
        }
        resp.setGeometryWKBs(wkbs);

        // read list size
        size = deserializer.readI32();
        List<GeometryResponseData> geoList = new ArrayList<>(size);
        // read objects
        for (int i = 0; i < size; i++) {
            int wkbIndex = deserializer.readI32();
            DataTime time = (DataTime) deserializer.readObject();
            String level = (String) deserializer.readObject();
            String locName = (String) deserializer.readObject();
            Map<String, Object> attrs = (Map<String, Object>) deserializer
                    .readObject();

            int paramSize = deserializer.readI32();
            Map<String, Object[]> dataMap = new HashMap<>(paramSize);
            for (int k = 0; k < paramSize; k++) {
                String paramName = deserializer.readString();
                Object[] data = new Object[3];
                // actual value
                data[0] = deserializer.readObject();
                // value type as string
                data[1] = deserializer.readString();
                // unit
                data[2] = deserializer.readObject();
                dataMap.put(paramName, data);
            }

            GeometryResponseData respData = new GeometryResponseData();
            respData.setAttributes(attrs);
            respData.setGeometryWKBindex(wkbIndex);
            respData.setLevel(level);
            respData.setLocationName(locName);
            respData.setTime(time);
            respData.setDataMap(dataMap);

            geoList.add(respData);
        }

        resp.setGeoData(geoList);
        return resp;
    }

}
