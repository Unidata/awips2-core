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

package com.raytheon.uf.common.json;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;

/**
 * Basic service for reading/writing JSON
 *
 * <pre>
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug 10, 2011            bclement    Initial creation
 * Aug 18, 2015  3806      njensen     Disable jackson closing Closeables
 * Oct 27, 2015  4767      bclement    upgraded jackson to 1.9
 * Jan 19, 2016  5067      bclement    upgrade jackson to 2.6
 * Jan 26, 2017  6092      randerso    Moved BasicJsonService to
 *                                     com.raytheon.uf.common.json
 *
 * </pre>
 *
 * @author bclement
 */
public class BasicJsonService implements JsonService {

    protected ObjectMapper mapper;

    /**
     * Default constructor
     */
    public BasicJsonService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.getFactory().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET,
                false);
    }

    @Override
    public String serialize(Object obj, boolean pretty) throws JsonException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(obj, baos, pretty);
        return baos.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> extract(Object obj) throws JsonException {
        try {
            return mapper.convertValue(obj, TreeMap.class);
        } catch (Exception e) {
            throw new JsonException("Problem extracting object to map", e);
        }
    }

    @Override
    public Object populate(Map<String, Object> map, Class<?> c)
            throws JsonException {
        try {
            return mapper.convertValue(map, c);
        } catch (Exception e) {
            throw new JsonException("Problem extracting object to map", e);
        }
    }

    @Override
    public void serialize(Object obj, OutputStream out, boolean pretty)
            throws JsonException {
        try {
            ObjectWriter writer = pretty
                    ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();
            writer.writeValue(out, obj);
        } catch (Exception e) {
            throw new JsonException("Problem serializing object to JSON", e);
        }
    }

    @Override
    public Object deserialize(String json, Class<?> c) throws JsonException {
        try {
            return mapper.readValue(json, c);
        } catch (Exception e) {
            throw new JsonException("Problem deserializing object to JSON", e);
        }
    }

    @Override
    public Object deserialize(InputStream in, Class<?> c) throws JsonException {
        try {
            return mapper.readValue(in, c);
        } catch (Exception e) {
            throw new JsonException("Problem deserializing object to JSON", e);
        }
    }

}
