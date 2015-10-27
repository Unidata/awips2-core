/**********************************************************************
 *
 * The following software products were developed by Raytheon:
 *
 * ADE (AWIPS Development Environment) software
 * CAVE (Common AWIPS Visualization Environment) software
 * EDEX (Environmental Data Exchange) software
 * uFrame™ (Universal Framework) software
 *
 * Copyright (c) 2010 Raytheon Co.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/org/documents/epl-v10.php
 *
 *
 * Contractor Name: Raytheon Company
 * Contractor Address:
 * 6825 Pine Street, Suite 340
 * Mail Stop B8
 * Omaha, NE 68106
 * 402.291.0100
 *
 **********************************************************************/
/**
 * 
 */
package com.raytheon.uf.common.json.jackson;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * A {@code KeyedPoolableObjectFactory} implementation for building ObjectMapper
 * instances for serializing and deserializing {@code Geometry} objects to and
 * from GeoJSON format.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 *                         bclement     Initial creation
 * Oct 23, 2015  #5004     dgilling     Update to use commons-pool2 API.
 * Oct 27, 2015  4767      bclement     upgraded jackson to 1.9
 * 
 * </pre>
 * 
 * @author bclement
 */
public class JacksonFactory extends
        BaseKeyedPooledObjectFactory<Long, ObjectMapper> {

    protected static final List<Class<? extends Geometry>> supportedGeoms = Collections
            .unmodifiableList(Arrays.asList(Geometry.class, Polygon.class,
                    MultiLineString.class, MultiPoint.class,
                    MultiPolygon.class, LinearRing.class));

    @Override
    public ObjectMapper create(Long arg0) throws Exception {
        FlexibleModule m = new FlexibleModule("FlexibleModule", new Version(0,
                0, 1, null));
        m.addSerializer(new GeometrySerializer());
        m.addDeserializers(supportedGeoms, new GeometryDeserializer());
        m.addSerializer(new EnvelopeSerializer());
        m.addDeserializer(Envelope.class, new EnvelopeDeserializer());
        m.addSerializer(new PointGeomSerialization.Serializer());
        m.addDeserializer(Point.class,
                new PointGeomSerialization.Deserializer());
        m.addSerializer(new CrsSerializer());
        m.addDeserializer(CoordinateReferenceSystem.class,
                new CrsDeserializer());
        m.addSerializer(new RefEnvelopeSerialization.Serializer());
        m.addDeserializer(ReferencedEnvelope.class,
                new RefEnvelopeSerialization.Deserializer());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();

        // make deserializer use JAXB annotations (only)
        DeserializationConfig deserializationConfig = mapper
                .getDeserializationConfig();
        deserializationConfig = deserializationConfig
                .withAnnotationIntrospector(introspector);
        mapper.setDeserializationConfig(deserializationConfig);
        // make serializer use JAXB annotations (only)
        SerializationConfig serializationConfig = mapper
                .getSerializationConfig();
        serializationConfig = serializationConfig
                .withAnnotationIntrospector(introspector);
        mapper.setSerializationConfig(serializationConfig);
        mapper.registerModule(m);
        return mapper;
    }

    @Override
    public PooledObject<ObjectMapper> wrap(ObjectMapper arg0) {
        return new DefaultPooledObject<ObjectMapper>(arg0);
    }

}
