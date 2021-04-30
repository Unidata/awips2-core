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
* Dec 7, 2011            bclement     Initial creation
*
*/
package com.raytheon.uf.common.json.jackson;

import java.io.IOException;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.raytheon.uf.common.json.jackson.util.ArrayDecoder;

/**
 * Serialization adapter for ReferencedEnvelope objects
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            bclement    Initial creation
 * Jan 19, 2016 5067       bclement    upgrade jackson to 2.6
 * Nov 29, 2017 6531       mapeters    upgrade jackson to 2.8
 *
 * </pre>
 *
 */
public class RefEnvelopeSerialization {

    public static class Deserializer
            extends JsonDeserializer<ReferencedEnvelope> {

        @Override
        public ReferencedEnvelope deserialize(JsonParser jp,
                DeserializationContext ctxt)
                throws IOException, JsonProcessingException {
            ArrayDecoder.checkArrayStart(jp, ctxt);
            double minx = getDouble(jp, ctxt);
            double miny = getDouble(jp, ctxt);
            double maxx = getDouble(jp, ctxt);
            double maxy = getDouble(jp, ctxt);
            JsonToken tok = jp.nextToken();
            if (tok != JsonToken.VALUE_STRING) {
                ctxt.handleUnexpectedToken(ReferencedEnvelope.class, jp);
            }
            String wkt = jp.getText();
            CoordinateReferenceSystem crs;
            if (wkt.isEmpty()) {
                crs = null;
            } else {
                try {
                    crs = CRS.parseWKT(wkt);
                } catch (FactoryException e) {
                    throw new IOException("Unable to parse CRS WKT", e);
                }
            }
            tok = jp.nextToken();
            ArrayDecoder.checkArrayEnd(jp, ctxt);
            return new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        }

        protected double getDouble(JsonParser jp, DeserializationContext ctxt)
                throws JsonParseException, IOException {
            JsonToken tok = jp.nextToken();
            if (tok != JsonToken.VALUE_NUMBER_FLOAT) {
                ctxt.handleUnexpectedToken(ReferencedEnvelope.class, jp);
            }
            return jp.getDoubleValue();
        }

    }

    public static class Serializer extends JsonSerializer<ReferencedEnvelope> {

        @Override
        public Class<ReferencedEnvelope> handledType() {
            return ReferencedEnvelope.class;
        }

        @Override
        public void serialize(ReferencedEnvelope value, JsonGenerator jgen,
                SerializerProvider provider)
                throws IOException, JsonProcessingException {
            jgen.writeStartArray();
            jgen.writeNumber(value.getMinX());
            jgen.writeNumber(value.getMinY());
            jgen.writeNumber(value.getMaxX());
            jgen.writeNumber(value.getMaxY());
            CoordinateReferenceSystem crs = value
                    .getCoordinateReferenceSystem();
            String wkt = (crs == null ? "" : crs.toWKT());
            jgen.writeString(wkt);
            jgen.writeEndArray();

        }

    }
}
