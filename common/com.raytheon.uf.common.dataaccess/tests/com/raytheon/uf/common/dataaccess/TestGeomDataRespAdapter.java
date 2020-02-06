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
package com.raytheon.uf.common.dataaccess;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import si.uom.SI;

import org.junit.Before;
import org.junit.Test;

import com.raytheon.uf.common.dataaccess.impl.DefaultGeometryData;
import com.raytheon.uf.common.dataaccess.response.GeomDataRespAdapter;
import com.raytheon.uf.common.dataaccess.response.GeometryResponseData;
import com.raytheon.uf.common.dataaccess.response.GetGeometryDataResponse;
import com.raytheon.uf.common.serialization.DynamicSerializationManager;
import com.raytheon.uf.common.serialization.DynamicSerializationManager.SerializationType;
import com.raytheon.uf.common.serialization.SerializationException;
import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.common.time.util.TimeUtil;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKBWriter;

/**
 * Tests that the {@link GeomDataRespAdapter} is symmetrical, i.e. the data that
 * is serialized and then deserialized is the same. Also verifies the adapter
 * can handle nulls.
 * 
 * This test makes the assumption that the DynamicSerializationManager and its
 * associated code are working correctly.
 * 
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Oct 19, 2016  5919      njensen     Initial creation
 *
 * </pre>
 *
 * @author njensen
 */

public class TestGeomDataRespAdapter {

    protected final GeometryFactory gf = new GeometryFactory();

    protected final WKBWriter wkbWriter = new WKBWriter();

    protected final DynamicSerializationManager dsm = DynamicSerializationManager
            .getManager(SerializationType.Thrift);

    protected GetGeometryDataResponse resp;

    @Before
    public void setUp() {
        // quiet the warning
        System.setProperty("thrift.stream.maxsize", "200");

        List<byte[]> wkbs = new ArrayList<>();
        Point p = gf.createPoint(new Coordinate(-97.31, 41.57));
        wkbs.add(wkbWriter.write(p));

        List<GeometryResponseData> dataList = new ArrayList<>();
        DefaultGeometryData geo = new DefaultGeometryData();
        geo.addData("temperature", 100.0, SI.CELSIUS);
        geo.addData("windSpd", 5);
        geo.addData("dewpoint", 33.3f);
        geo.addData("wx", "stormy");
        geo.addData("clouds", null);
        GeometryResponseData respData = new GeometryResponseData(geo, 0);
        dataList.add(respData);

        resp = new GetGeometryDataResponse();
        resp.setGeometryWKBs(wkbs);
        resp.setGeoData(dataList);
    }

    @Test
    public void testFields() throws SerializationException {
        GetGeometryDataResponse original = resp;
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void testLevelField() throws SerializationException {
        GetGeometryDataResponse original = resp;
        original.getGeoData().get(0).setLevel("Surface");
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(resp, deserialized);
    }

    @Test
    public void testLocNameField() throws SerializationException {
        GetGeometryDataResponse original = resp;
        original.getGeoData().get(0).setLocationName("KOMA");
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void testDataTimeField() throws SerializationException {
        GetGeometryDataResponse original = resp;
        resp.getGeoData().get(0)
                .setTime(new DataTime(TimeUtil.newGmtCalendar()));
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void testAttributesField() throws SerializationException {
        GetGeometryDataResponse original = resp;
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("extraName", "extraValue");
        attrs.put("fillValue", -1);
        attrs.put("missingValue", -9999.0);
        original.getGeoData().get(0).setAttributes(attrs);
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void testMultipleGeoms() throws SerializationException {
        GetGeometryDataResponse original = resp;
        Point p = gf.createPoint(new Coordinate(-96.88, 41.20));
        original.getGeometryWKBs().add(wkbWriter.write(p));

        DefaultGeometryData geo = new DefaultGeometryData();
        geo.addData("temperature", 96.8, SI.CELSIUS);
        geo.addData("windSpd", 10);
        geo.addData("dewpoint", 30.65f);
        geo.addData("wx", "rainy");
        geo.addData("clouds", "overcast");
        GeometryResponseData respData2 = new GeometryResponseData(geo, 1);
        original.getGeoData().add(respData2);

        // test reusing wkb index
        geo = new DefaultGeometryData();
        geo.addData("temperature", 47.5, SI.CELSIUS);
        geo.addData("windSpd", 15);
        geo.addData("dewpoint", 15.3f);
        geo.addData("wx", null);
        geo.addData("clouds", "fluffy");
        GeometryResponseData respData3 = new GeometryResponseData(geo, 0);
        original.getGeoData().add(respData3);

        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

    @Test
    public void testAllFieldsAndMultipleGeoms() throws SerializationException {
        /*
         * the tests are all modifying the original, combine them for a more
         * genuine response
         */
        testFields();
        testDataTimeField();
        testLevelField();
        testLocNameField();
        testAttributesField();
        testMultipleGeoms();

        GetGeometryDataResponse original = resp;
        byte[] serialized = dsm.serialize(original);
        GetGeometryDataResponse deserialized = (GetGeometryDataResponse) dsm
                .deserialize(serialized);
        assertEquals(original, deserialized);
    }

}
