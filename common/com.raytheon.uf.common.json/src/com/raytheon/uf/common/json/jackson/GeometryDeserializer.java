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

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

/**
 * Deserialization adapter for JTS Geometry objects
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
public class GeometryDeserializer extends JsonDeserializer<Geometry> {

    @Override
    public Geometry deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {
        if (jp.getCurrentToken() != JsonToken.VALUE_STRING) {
            ctxt.handleUnexpectedToken(Geometry.class, jp);
        }
        WKTReader reader = new WKTReader();
        try {
            return reader.read(jp.getText());
        } catch (ParseException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Object deserializeWithType(JsonParser jp,
            DeserializationContext ctxt, TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException {
        return super.deserializeWithType(jp, ctxt, typeDeserializer);
    }

}
