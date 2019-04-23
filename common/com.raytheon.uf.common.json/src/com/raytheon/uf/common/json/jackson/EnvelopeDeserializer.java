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
 *
 */
package com.raytheon.uf.common.json.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.raytheon.uf.common.json.jackson.util.ArrayDecoder;
import org.locationtech.jts.geom.Envelope;

/**
 * Deserialization adapter for JTS Envelope objects
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            bclement    Initial creation
 * Jan 19, 2016 5067       bclement    upgrade jackson to 2.6
 * Nov 29, 2017 6531       mapeters    Remove unused checkArrayStart()
 *
 * </pre>
 *
 */
public class EnvelopeDeserializer extends JsonDeserializer<Envelope> {

    @Override
    public Envelope deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        // this deserializes as a 2D array, the xml deserializer can only do 1d
        // arrays.
        // double[][] obj = ArrayDecoder.decodeDbl2D(jp, ctxt);
        // double[] min = obj[0];
        // double[] max = obj[1];
        // return new Envelope(min[0], max[0], min[1], max[1]);

        // xml deserializer compatible. Downside is that mongo spatial index
        // works on 2d arrays
        // comes back as MinX, MinY, MaxX, MaxY
        double[] obj = ArrayDecoder.decodeDbl(jp, ctxt);
        return new Envelope(obj[0], obj[2], obj[1], obj[3]);
    }

}
