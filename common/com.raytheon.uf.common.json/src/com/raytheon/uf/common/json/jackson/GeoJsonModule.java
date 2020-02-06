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
package com.raytheon.uf.common.json.jackson;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Jackson module that handles serializing and deserializing JTS geometries
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jan 18, 2016 5067       bclement     Initial creation
 * 
 * </pre>
 * 
 */
public class GeoJsonModule extends SimpleModule {

    protected static final List<Class<? extends Geometry>> SUPPORTED_GEOMS = Collections
            .unmodifiableList(Arrays.asList(Geometry.class, Polygon.class,
                    MultiLineString.class, MultiPoint.class,
                    MultiPolygon.class, LinearRing.class));

    private static final long serialVersionUID = 5657172865122223403L;

    public static final String NAME = "GeoJsonModule";

    public GeoJsonModule() {
        super(NAME, new Version(0, 0, 1, null, null, null), getDeserializers(),
                getSerializers());
    }

    /**
     * Get list of GeoJson serializers
     * 
     * @return
     */
    private static final List<JsonSerializer<?>> getSerializers() {
        return Arrays.asList(new GeometrySerializer(),
                new EnvelopeSerializer(),
                new PointGeomSerialization.Serializer(), new CrsSerializer(),
                new RefEnvelopeSerialization.Serializer());
    }

    /**
     * Get Map of GeoJson deserializers
     * 
     * @return
     */
    private static final Map<Class<?>, JsonDeserializer<?>> getDeserializers() {
        Map<Class<?>, JsonDeserializer<?>> deserializers = new HashMap<>();
        addDeserializers(deserializers, SUPPORTED_GEOMS,
                new GeometryDeserializer());
        deserializers.put(Envelope.class, new EnvelopeDeserializer());
        deserializers.put(Point.class,
                new PointGeomSerialization.Deserializer());
        deserializers.put(CoordinateReferenceSystem.class,
                new CrsDeserializer());
        deserializers.put(ReferencedEnvelope.class,
                new RefEnvelopeSerialization.Deserializer());
        return deserializers;
    }

    /**
     * Bulk mapping function for a deserializer that handles multiple subtypes
     * 
     * @param target
     * @param classes
     * @param deserializer
     */
    private static final <T> void addDeserializers(
            Map<Class<?>, JsonDeserializer<?>> target,
            List<Class<? extends T>> classes, JsonDeserializer<T> deserializer) {
        for (Class<? extends T> c : classes) {
            target.put(c, deserializer);
        }
    }

}
