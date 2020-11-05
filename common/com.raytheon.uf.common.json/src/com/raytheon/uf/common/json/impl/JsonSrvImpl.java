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
package com.raytheon.uf.common.json.impl;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.raytheon.uf.common.json.JsonException;
import com.raytheon.uf.common.json.JsonService;
import com.raytheon.uf.common.json.jackson.JacksonPool;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;

/**
 * JsonService implementation with pooled ObjectMappers
 *
 * NOTE: This class, along with the com.raytheon.uf.common.json.jackson
 * packages, is not currently used, as everything uses BasicJsonService instead.
 * Consider removing this class in the future, especially if maintaining
 * compatibility with jackson upgrades becomes non-trivial.
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 *
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Aug  9, 2011            bclement    Initial creation
 * Oct 27, 2015  4767      bclement    upgraded jackson to 1.9
 * Nov 30, 2017  6531      mapeters    Added class javadoc note, cleanup
 *
 * </pre>
 *
 * @author bclement
 */
public class JsonSrvImpl implements JsonService {

    private JacksonPool pool;

    private IUFStatusHandler log = UFStatus.getHandler(this.getClass());

    public JsonSrvImpl() {
        pool = new JacksonPool();
    }

    public JsonSrvImpl(boolean poolClassloaders) {
        pool = new JacksonPool(poolClassloaders);
    }

    @Override
    public String serialize(Object obj, boolean pretty) throws JsonException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        serialize(obj, baos, pretty);
        return baos.toString();
    }

    @Override
    public void serialize(Object obj, OutputStream out, boolean pretty)
            throws JsonException {
        long id = Thread.currentThread().getId();
        ObjectMapper mapper = null;
        try {
            mapper = pool.borrowObject(id);
            getWriter(mapper, pretty).writeValue(out, obj);
        } catch (Exception e) {
            throw new JsonException("Problem serializing object to JSON", e);
        } finally {
            returnObject(id, mapper);
        }
    }

    protected ObjectWriter getWriter(ObjectMapper mapper, boolean pretty) {
        return pretty ? mapper.writerWithDefaultPrettyPrinter()
                : mapper.writer();
    }

    protected void returnObject(long key, ObjectMapper mapper) {
        if (mapper != null) {
            try {
                pool.returnObject(key, mapper);
            } catch (Exception e) {
                log.error("Unable to return mapper with key: " + key, e);
            }
        }
    }

    @Override
    public Object deserialize(String json, Class<?> c) throws JsonException {
        long id = Thread.currentThread().getId();
        Object rval;
        ObjectMapper mapper = null;
        try {
            mapper = pool.borrowObject(id);
            rval = mapper.readValue(json, c);
        } catch (Exception e) {
            throw new JsonException("Problem deserializing object to JSON", e);
        } finally {
            returnObject(id, mapper);
        }
        return rval;
    }

    @Override
    public Object deserialize(InputStream in, Class<?> c) throws JsonException {
        long id = Thread.currentThread().getId();
        Object rval;
        ObjectMapper mapper = null;
        try {
            mapper = pool.borrowObject(id);
            rval = mapper.readValue(in, c);
        } catch (Exception e) {
            throw new JsonException("Problem deserializing object to JSON", e);
        } finally {
            returnObject(id, mapper);
        }
        return rval;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> extract(Object obj) throws JsonException {
        long id = Thread.currentThread().getId();
        Map<String, Object> rval;
        ObjectMapper mapper = null;
        try {
            mapper = pool.borrowObject(id);
            rval = mapper.convertValue(obj, TreeMap.class);
        } catch (Exception e) {
            throw new JsonException("Problem extracting object to map", e);
        } finally {
            returnObject(id, mapper);
        }
        return rval;
    }

    @Override
    public Object populate(Map<String, Object> map, Class<?> c)
            throws JsonException {
        long id = Thread.currentThread().getId();
        Object rval;
        ObjectMapper mapper = null;
        try {
            mapper = pool.borrowObject(id);
            rval = mapper.convertValue(map, c);
        } catch (Exception e) {
            throw new JsonException("Problem extracting object to map", e);
        } finally {
            returnObject(id, mapper);
        }
        return rval;
    }

}
