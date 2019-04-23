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
package com.raytheon.uf.common.json.jackson.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import org.locationtech.jts.geom.Envelope;

/**
 * JSON parsing utility for populating java arrays from JSON arrays
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
public class ArrayDecoder {

    /**
     * Decode a two dimensional double array
     *
     * @param jp
     * @param ctxt
     * @return
     * @throws JsonParseException
     * @throws IOException
     */
    public static double[][] decodeDbl2D(JsonParser jp,
            DeserializationContext ctxt)
            throws JsonParseException, IOException {
        ArrayList<double[]> rval = new ArrayList<>();
        checkArrayStart(jp, ctxt);
        JsonToken tok = jp.nextToken();
        while (tok == JsonToken.START_ARRAY) {
            rval.add(decodeDbl(jp, ctxt));
            tok = jp.nextToken();
        }
        checkArrayEnd(jp, ctxt);
        return toPrimitive2D(rval);
    }

    /**
     * Decode a one dimensional double array
     *
     * @param jp
     * @param ctxt
     * @return
     * @throws JsonParseException
     * @throws IOException
     */
    public static double[] decodeDbl(JsonParser jp, DeserializationContext ctxt)
            throws JsonParseException, IOException {
        ArrayList<Double> rval = new ArrayList<>();
        checkArrayStart(jp, ctxt);
        JsonToken tok = jp.nextToken();
        while (tok == JsonToken.VALUE_NUMBER_FLOAT) {
            rval.add(jp.getDoubleValue());
            tok = jp.nextToken();
        }
        checkArrayEnd(jp, ctxt);
        return toPrimitive(rval);
    }

    /**
     * Convert list of Double objects to array of double primatives
     *
     * @param list
     * @return
     */
    protected static double[] toPrimitive(List<Double> list) {
        double[] rval = new double[list.size()];
        for (int i = 0; i < rval.length; ++i) {
            rval[i] = list.get(i);
        }
        return rval;
    }

    /**
     * Convert list of double arrays objects to two dimensional array of double
     * primatives
     *
     * @param list
     * @return
     */
    protected static double[][] toPrimitive2D(List<double[]> list) {
        double[][] rval = new double[list.size()][];
        for (int i = 0; i < rval.length; ++i) {
            rval[i] = list.get(i);
        }
        return rval;
    }

    /**
     * Ensure that context is at the start of an array
     *
     * @param jp
     * @param ctxt
     * @throws IOException
     */
    public static void checkArrayStart(JsonParser jp,
            DeserializationContext ctxt) throws IOException {
        if (jp.getCurrentToken() != JsonToken.START_ARRAY) {
            ctxt.handleUnexpectedToken(Envelope.class, jp);
        }
    }

    /**
     * Ensure that context is at the end of an array
     *
     * @param jp
     * @param ctxt
     * @throws IOException
     */
    public static void checkArrayEnd(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        if (jp.getCurrentToken() != JsonToken.END_ARRAY) {
            ctxt.handleUnexpectedToken(Envelope.class, jp);
        }
    }
}
