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
* Dec 1, 2011            bclement     Initial creation
*
*/ 
package com.raytheon.uf.common.json.jackson;

import java.io.IOException;

import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Deserialization adapter for CoordinateReferenceSystem objects
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
public class CrsDeserializer extends
        JsonDeserializer<CoordinateReferenceSystem> {

	@Override
	public CoordinateReferenceSystem deserialize(JsonParser jp,
			DeserializationContext ctxt) throws IOException,
			JsonProcessingException {
		JsonToken tok = jp.getCurrentToken();
		if (tok != JsonToken.VALUE_STRING) {
			throw ctxt.mappingException(CoordinateReferenceSystem.class);
		}
		try {
			return CRS.parseWKT(jp.getText());
		} catch (FactoryException e) {
			throw new IOException(e);
		}
	}

}
