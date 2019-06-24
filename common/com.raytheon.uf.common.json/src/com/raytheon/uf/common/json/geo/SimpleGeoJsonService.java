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
package com.raytheon.uf.common.json.geo;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.raytheon.uf.common.json.BasicJsonService;
import com.raytheon.uf.common.json.JsonException;
import com.raytheon.uf.common.json.JsonService;
import org.locationtech.jts.geom.Geometry;

/**
 * Simple implementation of {@code IGeoJsonService}. Uses {@code Map}s as
 * intermediate representations.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date          Ticket#  Engineer  Description
 * ------------- -------- --------- --------------------------------------------
 * Aug 10, 2011           bclement  Initial creation
 * Apr 27, 2015  4354     dgilling  Renamed to SimpleGeoJsonService.
 * Jan 26, 2017  6092     randerso  Moved BasicJsonService to
 *                                  com.raytheon.uf.common.json
 *
 * </pre>
 *
 * @author bclement
 */
public class SimpleGeoJsonService implements IGeoJsonService {

    private static JsonService _jservice;

    protected GeoJsonMapUtil mapUtil = new GeoJsonMapUtil();

    protected boolean pretty = true;

    private static Object _mutex = new Object();

    protected static JsonService getJsonService() {
        if (_jservice == null) {
            synchronized (_mutex) {
                if (_jservice == null) {
                    // we need a json service that does not enable default
                    // typing
                    _jservice = new BasicJsonService();
                }
            }
        }
        return _jservice;
    }

    @Override
    public void serialize(Geometry geom, OutputStream out)
            throws JsonException {
        JsonService service = getJsonService();
        Map<String, Object> map = mapUtil.extract(geom);
        service.serialize(map, out, pretty);
    }

    @Override
    public String serialize(Geometry geom) throws JsonException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(geom, baos);
        return baos.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Geometry deserializeGeom(InputStream in) throws JsonException {
        JsonService service = getJsonService();
        Map<String, Object> map = (Map<String, Object>) service.deserialize(in,
                LinkedHashMap.class);
        return mapUtil.populateGeometry(map);
    }

    @Override
    public void serialize(SimpleFeature feature, OutputStream out)
            throws JsonException {
        Map<String, Object> map = mapUtil.extract(feature);
        getJsonService().serialize(map, out, pretty);
    }

    @Override
    public String serialize(SimpleFeature feature) throws JsonException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(feature, baos);
        return baos.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public SimpleFeature deserializeFeature(InputStream in)
            throws JsonException {
        Map<String, Object> map = (Map<String, Object>) getJsonService()
                .deserialize(in, LinkedHashMap.class);
        return mapUtil.populateFeature(map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SimpleFeature deserializeFeature(InputStream in,
            SimpleFeatureType type) throws JsonException {
        Map<String, Object> map = (Map<String, Object>) getJsonService()
                .deserialize(in, LinkedHashMap.class);
        return mapUtil.populateFeature(map, type);
    }

    @Override
    public void serialize(
            FeatureCollection<SimpleFeatureType, SimpleFeature> coll,
            OutputStream out) throws JsonException {
        Map<String, Object> map = mapUtil.extract(coll);
        getJsonService().serialize(map, out, pretty);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> deserializeFeatureCollection(
            InputStream in) throws JsonException {
        Map<String, Object> map = (Map<String, Object>) getJsonService()
                .deserialize(in, LinkedHashMap.class);
        return mapUtil.populateFeatureCollection(map);
    }

    @SuppressWarnings("unchecked")
    @Override
    public FeatureCollection<SimpleFeatureType, SimpleFeature> deserializeFeatureCollection(
            InputStream in, SimpleFeatureType type) throws JsonException {
        Map<String, Object> map = (Map<String, Object>) getJsonService()
                .deserialize(in, LinkedHashMap.class);
        return mapUtil.populateFeatureCollection(map, type);
    }

    /**
     * @return the pretty
     */
    public boolean isPretty() {
        return pretty;
    }

    /**
     * @param pretty
     *            the pretty to set
     */
    public void setPretty(boolean pretty) {
        this.pretty = pretty;
    }

}
