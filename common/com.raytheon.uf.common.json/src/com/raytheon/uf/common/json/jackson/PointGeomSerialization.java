/*
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
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Jul 21, 2011            bclement     Initial creation
 *
 */
package com.raytheon.uf.common.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.raytheon.uf.common.json.jackson.util.ArrayDecoder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * Serialization adapter for JTS Point objects
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            bclement    Initial creation
 * Jan 19, 2016  5067      bclement    upgrade jackson to 2.6
 * 
 * </pre>
 * 
 */
public class PointGeomSerialization {

	public static class Deserializer extends JsonDeserializer<Point> {

		@Override
		public Point deserialize(JsonParser jp, DeserializationContext ctxt)
				throws IOException, JsonProcessingException {
			double[] v = ArrayDecoder.decodeDbl(jp, ctxt);
			return new GeometryFactory()
					.createPoint(new Coordinate(v[0], v[1]));
		}

	}

	public static class Serializer extends JsonSerializer<Point> {

		@Override
		public void serialize(Point value, JsonGenerator jgen,
				SerializerProvider provider) throws IOException,
				JsonProcessingException {
			jgen.writeStartArray();
			jgen.writeNumber(value.getX());
			jgen.writeNumber(value.getY());
			jgen.writeEndArray();

		}

		@Override
		public Class<Point> handledType() {
			return Point.class;
		}

	}
}
